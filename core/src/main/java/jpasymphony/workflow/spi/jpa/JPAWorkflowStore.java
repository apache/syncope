/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package jpasymphony.workflow.spi.jpa;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.QueryNotSupportedException;
import com.opensymphony.workflow.StoreException;
import com.opensymphony.workflow.query.Expression;
import com.opensymphony.workflow.query.FieldExpression;
import com.opensymphony.workflow.query.NestedExpression;
import com.opensymphony.workflow.query.WorkflowExpressionQuery;
import com.opensymphony.workflow.query.WorkflowQuery;
import com.opensymphony.workflow.spi.Step;
import com.opensymphony.workflow.spi.WorkflowEntry;
import com.opensymphony.workflow.spi.WorkflowStore;
import com.opensymphony.workflow.util.PropertySetDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import jpasymphony.beans.AbstractJPAStep;
import jpasymphony.beans.JPACurrentStep;
import jpasymphony.beans.JPAHistoryStep;
import jpasymphony.beans.JPAWorkflowEntry;
import jpasymphony.dao.JPAWorkflowEntryDAO;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of OSWorkflow's WorkflowStore.
 * Still using Hibernate's criteria API (available since JPA 2.0).
 */
@Transactional(rollbackFor = {
    Throwable.class
})
public class JPAWorkflowStore implements WorkflowStore {

    @PersistenceContext(type = PersistenceContextType.TRANSACTION)
    private EntityManager entityManager;

    @Autowired
    private JPAWorkflowEntryDAO workflowEntryDAO;

    @Autowired
    private PropertySetDelegate propertySetDelegate;

    @Override
    public void setEntryState(final long entryId, final int state)
            throws StoreException {

        JPAWorkflowEntry entry = workflowEntryDAO.find(entryId);
        if (entry == null) {
            throw new StoreException(
                    "Could not find workflow entry " + entryId);
        }

        entry.setWorkflowState(state);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertySet getPropertySet(final long entryId)
            throws StoreException {

        if (propertySetDelegate == null) {
            throw new StoreException(
                    "PropertySetDelegate is not properly configured");
        }

        return propertySetDelegate.getPropertySet(entryId);
    }

    @Override
    public Step createCurrentStep(final long entryId,
            final int stepId,
            final String owner,
            final Date startDate,
            final Date dueDate,
            final String status,
            final long[] previousIds)
            throws StoreException {

        JPAWorkflowEntry entry = workflowEntryDAO.find(entryId);
        if (entry == null) {
            throw new StoreException(
                    "Could not find workflow entry " + entryId);
        }

        JPACurrentStep step = new JPACurrentStep();
        step.setWorkflowEntry(entry);
        step.setStepId(stepId);
        step.setOwner(owner);
        step.setStartDate(startDate);
        step.setDueDate(dueDate);
        step.setStatus(status);

        entry.addCurrentStep(step);
        entry = workflowEntryDAO.save(entry);

        List<JPACurrentStep> currentSteps = entry.getCurrentSteps();
        return currentSteps.get(currentSteps.size() - 1);
    }

    @Override
    public WorkflowEntry createEntry(final String workflowName)
            throws StoreException {

        JPAWorkflowEntry entry = new JPAWorkflowEntry();
        entry.setWorkflowState(WorkflowEntry.CREATED);
        entry.setWorkflowName(workflowName);

        return workflowEntryDAO.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List findCurrentSteps(final long entryId)
            throws StoreException {

        JPAWorkflowEntry entry = workflowEntryDAO.find(entryId);
        if (entry == null) {
            throw new StoreException(
                    "Could not find workflow entry " + entryId);
        }

        return entry.getCurrentSteps();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowEntry findEntry(final long entryId)
            throws StoreException {

        JPAWorkflowEntry entry = workflowEntryDAO.find(entryId);
        if (entry == null) {
            throw new StoreException(
                    "Could not find workflow entry " + entryId);
        }

        return entry;
    }

    @Override
    @Transactional(readOnly = true)
    public List findHistorySteps(final long entryId)
            throws StoreException {

        JPAWorkflowEntry entry = workflowEntryDAO.find(entryId);
        if (entry == null) {
            throw new StoreException(
                    "Could not find workflow entry " + entryId);
        }

        return entry.getHistorySteps();
    }

    @Override
    public void init(final Map props)
            throws StoreException {
    }

    @Override
    public Step markFinished(final Step step,
            final int actionId,
            final Date finishDate,
            final String status,
            final String caller)
            throws StoreException {

        JPACurrentStep currentStep = (JPACurrentStep) step;

        currentStep.setActionId(actionId);
        currentStep.setFinishDate(finishDate);
        currentStep.setStatus(status);
        currentStep.setCaller(caller);

        workflowEntryDAO.save(currentStep.getWorkflowEntry());

        return currentStep;
    }

    @Override
    public void moveToHistory(final Step step)
            throws StoreException {

        JPACurrentStep currentStep = (JPACurrentStep) step;
        JPAWorkflowEntry entry = currentStep.getWorkflowEntry();

        JPAHistoryStep historyStep = new JPAHistoryStep();
        historyStep.setActionId(currentStep.getActionId());
        historyStep.setCaller(currentStep.getCaller());
        historyStep.setDueDate(currentStep.getDueDate());
        historyStep.setFinishDate(currentStep.getFinishDate());
        historyStep.setOwner(currentStep.getOwner());
        historyStep.setStartDate(currentStep.getStartDate());
        historyStep.setStatus(currentStep.getStatus());
        historyStep.setStepId(currentStep.getStepId());
        historyStep.setWorkflowEntry(entry);

        entry.removeCurrentStep(currentStep);
        workflowEntryDAO.deleteCurrentStep(currentStep.getId());

        entry.addHistoryStep(historyStep);
        workflowEntryDAO.save(entry);
    }

    @Override
    @Deprecated
    public List query(final WorkflowQuery query)
            throws StoreException {

        throw new UnsupportedOperationException("Deprecated");
    }

    /**
     * @see com.opensymphony.workflow.spi.WorkflowStore
     * #query(com.opensymphony.workflow.query.WorkflowExpressionQuery)
     */
    @Override
    @Transactional(readOnly = true)
    public List query(final WorkflowExpressionQuery query)
            throws StoreException {

        Class entityClass = getQueryClass(query.getExpression(), null);

        Criterion expr;
        if (query.getExpression().isNested()) {
            expr = buildNested((NestedExpression) query.getExpression());
        } else {
            expr = queryComparison((FieldExpression) query.getExpression());
        }

        Session hibernateSess = (Session) entityManager.getDelegate();
        Criteria criteria = hibernateSess.createCriteria(entityClass);
        criteria.add(expr);

        List<Long> results = new ArrayList<Long>();
        Object next;
        Long item;
        for (Iterator iter = criteria.list().iterator(); iter.hasNext();) {
            next = iter.next();

            if (next instanceof AbstractJPAStep) {
                AbstractJPAStep step = (AbstractJPAStep) next;
                item = new Long(step.getEntryId());
            } else {
                WorkflowEntry entry = (WorkflowEntry) next;
                item = new Long(entry.getId());
            }

            results.add(item);
        }

        return results;
    }

    private Class getQueryClass(Expression expr, Collection classesCache) {
        if (classesCache == null) {
            classesCache = new HashSet();
        }

        if (expr instanceof FieldExpression) {
            FieldExpression fieldExpression = (FieldExpression) expr;

            switch (fieldExpression.getContext()) {
                case FieldExpression.CURRENT_STEPS:
                    classesCache.add(JPACurrentStep.class);

                    break;

                case FieldExpression.HISTORY_STEPS:
                    classesCache.add(JPAHistoryStep.class);

                    break;

                case FieldExpression.ENTRY:
                    classesCache.add(JPAWorkflowEntry.class);

                    break;

                default:
                    throw new QueryNotSupportedException(
                            "Query for unsupported context " + fieldExpression.
                            getContext());
            }
        } else {
            NestedExpression nestedExpression = (NestedExpression) expr;

            for (int i = 0; i < nestedExpression.getExpressionCount(); i++) {
                Expression expression = nestedExpression.getExpression(i);

                if (expression.isNested()) {
                    classesCache.add(getQueryClass(nestedExpression.
                            getExpression(i), classesCache));
                } else {
                    classesCache.add(getQueryClass(expression, classesCache));
                }
            }
        }

        if (classesCache.size() > 1) {
            throw new QueryNotSupportedException(
                    "Store does not support nested queries of different types "
                    + "(types found:" + classesCache + ")");
        }

        return (Class) classesCache.iterator().next();
    }

    private Criterion buildNested(NestedExpression nestedExpression) {
        Criterion full = null;

        for (int i = 0; i < nestedExpression.getExpressionCount(); i++) {
            Criterion expr;
            Expression expression = nestedExpression.getExpression(i);

            if (expression.isNested()) {
                expr = buildNested((NestedExpression) nestedExpression.
                        getExpression(i));
            } else {
                FieldExpression sub = (FieldExpression) nestedExpression.
                        getExpression(i);
                expr = queryComparison(sub);

                if (sub.isNegate()) {
                    expr = Restrictions.not(expr);
                }
            }

            if (full == null) {
                full = expr;
            } else {
                switch (nestedExpression.getExpressionOperator()) {
                    case NestedExpression.AND:
                        full = Restrictions.and(full, expr);
                        break;

                    case NestedExpression.OR:
                        full = Restrictions.or(full, expr);
                        break;

                    default:
                }
            }
        }

        return full;
    }

    private Criterion queryComparison(FieldExpression expression) {
        int operator = expression.getOperator();

        switch (operator) {
            case FieldExpression.EQUALS:
                return Restrictions.eq(getFieldName(expression.getField()),
                        expression.getValue());

            case FieldExpression.NOT_EQUALS:
                return Restrictions.not(
                        Restrictions.like(getFieldName(expression.getField()),
                        expression.getValue()));

            case FieldExpression.GT:
                return Restrictions.gt(getFieldName(expression.getField()),
                        expression.getValue());

            case FieldExpression.LT:
                return Restrictions.lt(getFieldName(expression.getField()),
                        expression.getValue());

            default:
                return Restrictions.eq(getFieldName(expression.getField()),
                        expression.getValue());
        }
    }

    private String getFieldName(int field) {
        switch (field) {
            case FieldExpression.ACTION: // actionId
                return "actionId";

            case FieldExpression.CALLER:
                return "caller";

            case FieldExpression.FINISH_DATE:
                return "finishDate";

            case FieldExpression.OWNER:
                return "owner";

            case FieldExpression.START_DATE:
                return "startDate";

            case FieldExpression.STEP: // stepId
                return "stepId";

            case FieldExpression.STATUS:
                return "status";

            case FieldExpression.STATE:
                return "workflowState";

            case FieldExpression.NAME:
                return "workflowName";

            case FieldExpression.DUE_DATE:
                return "dueDate";

            default:
                return "1";
        }
    }
}
