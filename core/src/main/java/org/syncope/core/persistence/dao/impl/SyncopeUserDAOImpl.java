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
package org.syncope.core.persistence.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.validation.ValidationException;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.PaginatedResult;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrUniqueValue;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Repository
public class SyncopeUserDAOImpl extends AbstractDAOImpl
        implements SyncopeUserDAO {

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private SyncopeRoleDAO syncopeRoleDAO;

    @Override
    public SyncopeUser find(final Long id) {
        Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeUser e WHERE e.id = :id");
        query.setHint("org.hibernate.cacheable", true);
        query.setParameter("id", id);

        try {
            return (SyncopeUser) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public SyncopeUser findByWorkflowId(final Long workflowId) {
        Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeUser e WHERE e.workflowId = :workflowId");
        query.setHint("org.hibernate.cacheable", true);
        query.setParameter("workflowId", workflowId);

        return (SyncopeUser) query.getSingleResult();
    }

    @Override
    public List<SyncopeUser> findByAttrValue(final String schemaName,
            final UAttrValue attrValue) {

        StringBuilder queryHead1 = new StringBuilder(
                "SELECT e FROM " + UAttrValue.class.getName() + " e");
        StringBuilder queryHead2 = new StringBuilder(
                "SELECT e FROM " + UAttrUniqueValue.class.getName() + " e");

        StringBuilder whereCondition = new StringBuilder().append(
                " WHERE e.attribute.schema.name = :schemaName ").
                append(" AND (e.stringValue IS NOT NULL").
                append(" AND e.stringValue = :stringValue)").
                append(" OR (e.booleanValue IS NOT NULL").
                append(" AND e.booleanValue = :booleanValue)").
                append(" OR (e.dateValue IS NOT NULL").
                append(" AND e.dateValue = :dateValue)").
                append(" OR (e.longValue IS NOT NULL").
                append(" AND e.longValue = :longValue)").
                append(" OR (e.doubleValue IS NOT NULL").
                append(" AND e.doubleValue = :doubleValue)");

        Query query = entityManager.createQuery(
                queryHead1.append(whereCondition).toString());

        query.setParameter("schemaName", schemaName);
        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue", attrValue.getBooleanValue() == null
                ? null
                : attrValue.getBooleanAsInteger(attrValue.getBooleanValue()));
        query.setParameter("dateValue", attrValue.getDateValue());
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<UAttrValue> result1 = query.getResultList();

        query = entityManager.createQuery(
                queryHead2.append(whereCondition).toString());

        query.setParameter("schemaName", schemaName);
        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue", attrValue.getBooleanValue() == null
                ? null
                : attrValue.getBooleanAsInteger(attrValue.getBooleanValue()));
        query.setParameter("dateValue", attrValue.getDateValue());
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<UAttrUniqueValue> result2 = query.getResultList();

        List<SyncopeUser> result = new ArrayList<SyncopeUser>();
        for (UAttrValue value : (List<UAttrValue>) result1) {

            result.add((SyncopeUser) value.getAttribute().getOwner());
        }
        for (UAttrUniqueValue value : (List<UAttrUniqueValue>) result2) {

            result.add((SyncopeUser) value.getAttribute().getOwner());
        }

        return result;
    }

    @Override
    public SyncopeUser findByAttrUniqueValue(final String schemaName,
            final UAttrValue attrUniqueValue) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + UAttrUniqueValue.class.getSimpleName() + " e"
                + " WHERE e.attribute.schema.name = :schemaName "
                + " AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL"
                + " AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL"
                + " AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL"
                + " AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL"
                + " AND e.doubleValue = :doubleValue)");

        query.setParameter("schemaName", schemaName);
        query.setParameter("stringValue", attrUniqueValue.getStringValue());
        query.setParameter("booleanValue",
                attrUniqueValue.getBooleanValue() == null
                ? null
                : attrUniqueValue.getBooleanAsInteger(attrUniqueValue.
                getBooleanValue()));
        query.setParameter("dateValue", attrUniqueValue.getDateValue());
        query.setParameter("longValue", attrUniqueValue.getLongValue());
        query.setParameter("doubleValue", attrUniqueValue.getDoubleValue());

        UAttrUniqueValue result = null;
        try {
            result = (UAttrUniqueValue) query.getSingleResult();
        } catch (Exception e) {
            LOG.debug("While finding by attribute unique value: " + schemaName
                    + " {}", attrUniqueValue, e);
        }

        return result == null
                ? null : (SyncopeUser) result.getAttribute().getOwner();
    }

    @Override
    public final List<SyncopeUser> findAll() {
        return findAll(-1, -1);
    }

    @Override
    public final List<SyncopeUser> findAll(
            final int page, final int itemsPerPage) {

        final Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeUser e ORDER BY e.id");

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public final Long count() {

        final Query query = entityManager.createQuery(
                "SELECT count(e.id) FROM SyncopeUser e");

        return (Long) query.getSingleResult();
    }

    @Override
    public SyncopeUser save(final SyncopeUser syncopeUser) {
        return entityManager.merge(syncopeUser);
    }

    @Override
    public void delete(final Long id) {
        SyncopeUser user = find(id);
        if (user == null) {
            return;
        }

        delete(user);
    }

    @Override
    public void delete(final SyncopeUser user) {
        // Not calling membershipDAO.delete() here because it would try
        // to save this user as well, thus going into
        // ConcurrentModificationException
        for (Membership membership : user.getMemberships()) {
            membership.setSyncopeUser(null);

            membership.getSyncopeRole().removeMembership(membership);
            syncopeRoleDAO.save(membership.getSyncopeRole());
            membership.setSyncopeRole(null);

            entityManager.remove(membership);
        }
        user.getMemberships().clear();

        entityManager.remove(user);
    }

    @Override
    public List<SyncopeUser> search(final NodeCond searchCondition) {
        return search(searchCondition, -1, -1, null);
    }

    @Override
    public List<SyncopeUser> search(final NodeCond searchCondition,
            final int page,
            final int itemsPerPage,
            final PaginatedResult paginatedResult) {

        LOG.debug("Search condition:\n{}", searchCondition);

        List<SyncopeUser> result;
        try {
            result = doSearch(searchCondition);
        } catch (Throwable t) {
            LOG.error("While searching users", t);

            result = Collections.EMPTY_LIST;
        }

        if (paginatedResult != null) {
            paginatedResult.setTotalRecords(
                    new Long((long) result.size()));
        }

        // TODO: temporary solution to the paginated search
        int from = itemsPerPage * (page <= 0 ? 0 : page - 1);

        int to = itemsPerPage <= 0 || from + itemsPerPage > result.size()
                ? result.size() : from + itemsPerPage;

        return from > to ? Collections.EMPTY_LIST : result.subList(from, to);
    }

    private List<SyncopeUser> doSearch(
            final NodeCond nodeCond) {
        List<SyncopeUser> result;
        List<SyncopeUser> rightResult;

        switch (nodeCond.getType()) {
            case LEAF:
            case NOT_LEAF:
                if (nodeCond.getAttributeCond() != null
                        && nodeCond.getAttributeCond().getType()
                        == AttributeCond.Type.ISNULL) {

                    if (nodeCond.getType() == NodeCond.Type.NOT_LEAF) {
                        nodeCond.setType(NodeCond.Type.LEAF);
                        nodeCond.getAttributeCond().setType(
                                AttributeCond.Type.ISNOTNULL);

                        result = doSearch(nodeCond);
                    } else {
                        Query query = entityManager.createQuery(
                                "SELECT e FROM SyncopeUser e WHERE e NOT IN ("
                                + "SELECT u FROM SyncopeUser u "
                                + "LEFT OUTER JOIN u.attributes ua "
                                + "WHERE ua.schema.name = :schemaName)");
                        query.setParameter("schemaName",
                                nodeCond.getAttributeCond().getSchema());
                        LOG.debug("[ISNOTULL] "
                                + "Search query to be performed: {}", query);

                        result = query.getResultList();
                    }
                } else {
                    Criteria criteria = getCriteria(nodeCond);
                    LOG.debug("Criteria to be performed: {}", criteria);

                    result = criteria.list();
                }

                LOG.debug("Leaf result: {}", result);
                break;

            case AND:
                // TODO: not paginable
                result = doSearch(nodeCond.getLeftNodeCond());
                rightResult = doSearch(nodeCond.getRightNodeCond());
                result.retainAll(rightResult);
                break;

            case OR:
                // TODO: not paginable
                result = doSearch(nodeCond.getLeftNodeCond());
                rightResult = doSearch(nodeCond.getRightNodeCond());

                List<SyncopeUser> from = null;
                List<SyncopeUser> to = null;

                if (rightResult.size() > result.size()) {
                    from = result;
                    to = rightResult;
                } else {
                    from = rightResult;
                    to = result;
                }

                for (SyncopeUser user : from) {
                    if (!to.contains(user)) {
                        to.add(user);
                    }
                }

                result = to;
                break;

            default:
                result = Collections.EMPTY_LIST;
        }

        return result;
    }

    private Criteria getCriteria(final NodeCond leafCond) {
        Session hibernateSess = (Session) entityManager.getDelegate();
        Criteria criteria = hibernateSess.createCriteria(SyncopeUser.class);
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        if (leafCond.getMembershipCond() != null) {
            criteria = criteria.createAlias("memberships", "m").
                    createAlias("m.syncopeRole", "r");

            criteria.add(getCriterion(leafCond.getMembershipCond(),
                    leafCond.getType()));
        }

        USchema schema = null;
        if (leafCond.getAttributeCond() != null) {
            schema = schemaDAO.find(leafCond.getAttributeCond().getSchema(),
                    USchema.class);
            if (schema == null) {
                LOG.warn("Ignoring invalid schema '{}'",
                        leafCond.getAttributeCond().getSchema());
            } else {
                criteria = criteria.createAlias("attributes", "a");
                if (schema.isUniqueConstraint()) {
                    criteria = criteria.createAlias("a.uniqueValue", "av");
                } else {
                    criteria = criteria.createAlias("a.values", "av");
                }

                criteria.add(getCriterion(leafCond.getAttributeCond(),
                        leafCond.getType(), schema));
            }
        }

        return criteria;
    }

    private Criterion getCriterion(final MembershipCond cond,
            final NodeCond.Type nodeCondType) {

        Criterion criterion = null;

        if (cond.getRoleId() != null) {
            criterion = Restrictions.eq("r.id", cond.getRoleId());
        }
        if (cond.getRoleName() != null) {
            criterion = Restrictions.eq("r.name", cond.getRoleName());
        }

        if (nodeCondType == NodeCond.Type.NOT_LEAF) {
            criterion = Restrictions.not(criterion);
        }

        return criterion;
    }

    private Criterion getCriterion(final AttributeCond cond,
            final NodeCond.Type nodeCondType, final USchema schema) {

        Criterion criterion = null;

        UAttrValue attrValue = new UAttrValue();
        try {
            if (cond.getType() == AttributeCond.Type.LIKE) {
                attrValue.setStringValue(cond.getExpression());
            } else {
                attrValue = schema.getValidator().
                        getValue(cond.getExpression(), attrValue);
            }

            criterion = Restrictions.and(
                    Restrictions.eq("a.schema.name", schema.getName()),
                    nodeCondType == NodeCond.Type.LEAF
                    ? getCriterion(cond.getType(), attrValue)
                    : Restrictions.not(
                    getCriterion(cond.getType(), attrValue)));
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '"
                    + cond.getExpression() + "'", e);
        }

        return criterion;
    }

    private Criterion getCriterion(final AttributeCond.Type type,
            final AbstractAttrValue attrValue) {

        Criterion result = null;
        Conjunction conjunction;
        switch (type) {
            case EQ:
                conjunction = Restrictions.conjunction();

                if (attrValue.getStringValue() != null) {
                    conjunction.add(Restrictions.eq("av.stringValue",
                            attrValue.getStringValue()));
                }
                if (attrValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.eq("av.booleanValue",
                            attrValue.getBooleanAsInteger(
                            attrValue.getBooleanValue())));
                }
                if (attrValue.getLongValue() != null) {
                    conjunction.add(Restrictions.eq("av.longValue",
                            attrValue.getLongValue()));
                }
                if (attrValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.eq("av.doubleValue",
                            attrValue.getDoubleValue()));
                }
                if (attrValue.getDateValue() != null) {
                    conjunction.add(Restrictions.eq("av.dateValue",
                            attrValue.getDateValue()));
                }

                result = conjunction;
                break;

            case GE:
                conjunction = Restrictions.conjunction();

                if (attrValue.getStringValue() != null) {
                    conjunction.add(Restrictions.ge("av.stringValue",
                            attrValue.getStringValue()));
                }
                if (attrValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.ge("av.booleanValue",
                            attrValue.getBooleanAsInteger(
                            attrValue.getBooleanValue())));
                }
                if (attrValue.getLongValue() != null) {
                    conjunction.add(Restrictions.ge("av.longValue",
                            attrValue.getLongValue()));
                }
                if (attrValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.ge("av.doubleValue",
                            attrValue.getDoubleValue()));
                }
                if (attrValue.getDateValue() != null) {
                    conjunction.add(Restrictions.ge("av.dateValue",
                            attrValue.getDateValue()));
                }

                result = conjunction;
                break;

            case GT:
                conjunction = Restrictions.conjunction();

                if (attrValue.getStringValue() != null) {
                    conjunction.add(Restrictions.gt("av.stringValue",
                            attrValue.getStringValue()));
                }
                if (attrValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.gt("av.booleanValue",
                            attrValue.getBooleanAsInteger(
                            attrValue.getBooleanValue())));
                }
                if (attrValue.getLongValue() != null) {
                    conjunction.add(Restrictions.gt("av.longValue",
                            attrValue.getLongValue()));
                }
                if (attrValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.gt("av.doubleValue",
                            attrValue.getDoubleValue()));
                }
                if (attrValue.getDateValue() != null) {
                    conjunction.add(Restrictions.gt("av.dateValue",
                            attrValue.getDateValue()));
                }

                result = conjunction;
                break;

            case ISNOTNULL:
                result = Restrictions.disjunction().
                        add(Restrictions.isNotNull("av.stringValue")).
                        add(Restrictions.isNotNull("av.booleanValue")).
                        add(Restrictions.isNotNull("av.longValue")).
                        add(Restrictions.isNotNull("av.doubleValue")).
                        add(Restrictions.isNotNull("av.dateValue"));
                break;

            case LE:
                conjunction = Restrictions.conjunction();

                if (attrValue.getStringValue() != null) {
                    conjunction.add(Restrictions.le("av.stringValue",
                            attrValue.getStringValue()));
                }
                if (attrValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.le("av.booleanValue",
                            attrValue.getBooleanAsInteger(
                            attrValue.getBooleanValue())));
                }
                if (attrValue.getLongValue() != null) {
                    conjunction.add(Restrictions.le("av.longValue",
                            attrValue.getLongValue()));
                }
                if (attrValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.le("av.doubleValue",
                            attrValue.getDoubleValue()));
                }
                if (attrValue.getDateValue() != null) {
                    conjunction.add(Restrictions.le("av.dateValue",
                            attrValue.getDateValue()));
                }

                result = conjunction;
                break;

            case LT:
                conjunction = Restrictions.conjunction();

                if (attrValue.getStringValue() != null) {
                    conjunction.add(Restrictions.lt("av.stringValue",
                            attrValue.getStringValue()));
                }
                if (attrValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.lt("av.booleanValue",
                            attrValue.getBooleanAsInteger(
                            attrValue.getBooleanValue())));
                }
                if (attrValue.getLongValue() != null) {
                    conjunction.add(Restrictions.lt("av.longValue",
                            attrValue.getLongValue()));
                }
                if (attrValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.lt("av.doubleValue",
                            attrValue.getDoubleValue()));
                }
                if (attrValue.getDateValue() != null) {
                    conjunction.add(Restrictions.lt("av.dateValue",
                            attrValue.getDateValue()));
                }

                result = conjunction;
                break;

            case LIKE:
                // LIKE operator is meaningful for strings only
                result = Restrictions.like("av.stringValue",
                        attrValue.getStringValue());
                break;

            default:
        }

        return result;
    }
}
