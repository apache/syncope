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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import jpasymphony.beans.AbstractJPAStep;
import jpasymphony.beans.JPACurrentStep;
import jpasymphony.beans.JPAHistoryStep;
import jpasymphony.beans.JPAWorkflowEntry;
import jpasymphony.dao.JPAWorkflowEntryDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * JPA implementation of OSWorkflow's WorkflowStore.
 * Still using Hibernate's criteria API (available since JPA 2.0).
 */
public class JPAWorkflowStore implements WorkflowStore {

    @PersistenceContext(type = PersistenceContextType.TRANSACTION)
    private EntityManager entityManager;

    @Autowired
    private JPAWorkflowEntryDAO workflowEntryDAO;

    @Autowired
    private PropertySetDelegate propertySetDelegate;

    private JPAWorkflowEntry getEntry(final long entryId)
            throws StoreException {

        JPAWorkflowEntry entry = workflowEntryDAO.find(entryId);
        if (entry == null) {
            throw new StoreException(
                    "Could not find workflow entry " + entryId);
        }

        return entry;
    }

    @Override
    public void setEntryState(final long entryId, final int state)
            throws StoreException {

        JPAWorkflowEntry entry = getEntry(entryId);

        entry.setWorkflowState(state);
    }

    @Override
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

        JPAWorkflowEntry entry = getEntry(entryId);

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
    public List findCurrentSteps(final long entryId)
            throws StoreException {

        JPAWorkflowEntry entry = getEntry(entryId);

        return entry.getCurrentSteps();
    }

    @Override
    public WorkflowEntry findEntry(final long entryId)
            throws StoreException {

        return getEntry(entryId);
    }

    @Override
    public List findHistorySteps(final long entryId)
            throws StoreException {

        JPAWorkflowEntry entry = getEntry(entryId);

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

        final JPACurrentStep currentStep = (JPACurrentStep) step;

        currentStep.setActionId(actionId);
        currentStep.setFinishDate(finishDate);
        currentStep.setCaller(caller);

        workflowEntryDAO.save(currentStep.getWorkflowEntry());

        return currentStep;
    }

    @Override
    public void moveToHistory(final Step step)
            throws StoreException {

        final JPACurrentStep currentStep = (JPACurrentStep) step;
        final JPAWorkflowEntry entry = currentStep.getWorkflowEntry();

        final JPAHistoryStep historyStep = new JPAHistoryStep();
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
    public List query(final WorkflowExpressionQuery query)
            throws StoreException {

        Class entityClass = getQueryClass(query.getExpression(), null);

        CriteriaQuery criteria =
                entityManager.getCriteriaBuilder().createQuery(
                getQueryClass(query.getExpression(), null));
        Root from = criteria.from(entityClass);

        Predicate expr = query.getExpression().isNested()
                ? buildNested((NestedExpression) query.getExpression(), from)
                : queryComp((FieldExpression) query.getExpression(), from);

        criteria.where(expr);

        TypedQuery criteriaQuery = entityManager.createQuery(criteria);

        List<Long> results = new ArrayList<Long>();
        Object next;
        Long item;
        for (Iterator iter = criteriaQuery.getResultList().iterator();
                iter.hasNext();) {

            next = iter.next();

            if (next instanceof AbstractJPAStep) {
                AbstractJPAStep step = (AbstractJPAStep) next;
                item = Long.valueOf(step.getEntryId());
            } else {
                WorkflowEntry entry = (WorkflowEntry) next;
                item = Long.valueOf(entry.getId());
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

    private Predicate buildNested(NestedExpression nestedExpression,
            Root from) {

        Predicate full = null;

        for (int i = 0; i < nestedExpression.getExpressionCount(); i++) {
            Predicate expr;
            Expression expression = nestedExpression.getExpression(i);

            if (expression.isNested()) {
                expr = buildNested((NestedExpression) nestedExpression.
                        getExpression(i), from);
            } else {
                FieldExpression sub = (FieldExpression) nestedExpression.
                        getExpression(i);
                expr = queryComp(sub, from);

                if (sub.isNegate()) {
                    expr = entityManager.getCriteriaBuilder().not(expr);
                }
            }

            if (full == null) {
                full = expr;
            } else {
                switch (nestedExpression.getExpressionOperator()) {
                    case NestedExpression.AND:
                        full = entityManager.getCriteriaBuilder().
                                and(full, expr);
                        break;

                    case NestedExpression.OR:
                        full = entityManager.getCriteriaBuilder().
                                or(full, expr);
                        break;

                    default:
                }
            }
        }

        return full;
    }

    private Predicate queryComp(FieldExpression expression, Root from) {
        switch (expression.getOperator()) {
            default:
            case FieldExpression.EQUALS:
                return entityManager.getCriteriaBuilder().equal(
                        from.get(getFieldName(expression.getField())),
                        expression.getValue());

            case FieldExpression.NOT_EQUALS:
                return entityManager.getCriteriaBuilder().notEqual(
                        from.get(getFieldName(expression.getField())),
                        expression.getValue());

            case FieldExpression.GT:
                return entityManager.getCriteriaBuilder().gt(
                        from.get(getFieldName(expression.getField())),
                        (Number) (expression.getValue()));

            case FieldExpression.LT:
                return entityManager.getCriteriaBuilder().lt(
                        from.get(getFieldName(expression.getField())),
                        (Number) (expression.getValue()));
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
