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
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
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
        Query query = entityManager.createNamedQuery("SyncopeUser.find");
        query.setParameter("id", id);

        try {
            return (SyncopeUser) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public SyncopeUser findByWorkflowId(final Long workflowId) {
        Query query = entityManager.createNamedQuery(
                "SyncopeUser.findByWorkflowId");
        query.setParameter("workflowId", workflowId);

        return (SyncopeUser) query.getSingleResult();
    }

    @Override
    public List<SyncopeUser> findByAttributeValue(
            final UAttrValue attrValue) {

        StringBuilder queryHead1 = new StringBuilder("SELECT u").append(
                " FROM SyncopeUser u, UAttr ua, UAttrValue e");
        StringBuilder queryHead2 = new StringBuilder(" SELECT u").append(
                " FROM SyncopeUser u, UAttr ua, UAttrUniqueValue e");

        StringBuilder whereCondition = new StringBuilder().append(
                " WHERE e.attribute = ua AND ua.owner = u").
                append(" AND ((e.stringValue IS NOT NULL").
                append(" AND e.stringValue = :stringValue)").
                append(" OR (e.booleanValue IS NOT NULL").
                append(" AND e.booleanValue = :booleanValue)").
                append(" OR (e.dateValue IS NOT NULL").
                append(" AND e.dateValue = :dateValue)").
                append(" OR (e.longValue IS NOT NULL").
                append(" AND e.longValue = :longValue)").
                append(" OR (e.doubleValue IS NOT NULL").
                append(" AND e.doubleValue = :doubleValue))").
                append(" ORDER BY u.id");

        Query query = entityManager.createQuery(
                queryHead1.append(whereCondition).toString());

        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue", attrValue.getBooleanValue() == null
                ? null
                : attrValue.getBooleanAsInteger(attrValue.getBooleanValue()));
        query.setParameter("dateValue", attrValue.getDateValue());
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<SyncopeUser> result1 = query.getResultList();

        query = entityManager.createQuery(
                queryHead2.append(whereCondition).toString());

        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue", attrValue.getBooleanValue() == null
                ? null
                : attrValue.getBooleanAsInteger(attrValue.getBooleanValue()));
        query.setParameter("dateValue", attrValue.getDateValue());
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<SyncopeUser> result2 = query.getResultList();
        result2.addAll(result1);

        return result2;
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
    public SyncopeUser save(final SyncopeUser syncopeUser) {
        return entityManager.merge(syncopeUser);
    }

    @Override
    public void delete(final Long id) {
        SyncopeUser user = find(id);
        if (user == null) {
            return;
        }

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
        return search(searchCondition, -1, -1);
    }

    @Override
    public List<SyncopeUser> search(final NodeCond searchCondition,
            final int page, final int itemsPerPage) {

        LOG.debug("Search condition:\n{}", searchCondition);

        List<SyncopeUser> result;
        try {
            result = doSearch(searchCondition);
        } catch (Throwable t) {
            LOG.error("While searching users", t);

            result = Collections.EMPTY_LIST;
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
        Criteria baseCriteria = hibernateSess.createCriteria(SyncopeUser.class);

        if (leafCond.getMembershipCond() != null) {
            baseCriteria = baseCriteria.createAlias("memberships", "m").
                    createAlias("m.syncopeRole", "r");
        }
        USchema schema = null;
        if (leafCond.getAttributeCond() != null) {
            schema = schemaDAO.find(
                    leafCond.getAttributeCond().getSchema(),
                    USchema.class);
            if (schema == null) {
                LOG.warn("Ignoring invalid schema '{}'",
                        leafCond.getAttributeCond().getSchema());
            } else {
                baseCriteria = baseCriteria.createAlias("attributes", "a");
                if (schema.isUniqueConstraint()) {
                    baseCriteria =
                            baseCriteria.createAlias("a.uniqueValue", "av");
                } else {
                    baseCriteria =
                            baseCriteria.createAlias("a.values", "av");
                }
            }
        }

        baseCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return baseCriteria.add(getCriterion(schema, leafCond));
    }

    private Criterion getCriterion(final USchema schema,
            final NodeCond leafCond) {

        Criterion criterion = null;

        switch (leafCond.getType()) {
            case LEAF:
                if (leafCond.getMembershipCond() != null) {
                    if (leafCond.getMembershipCond().getRoleId() != null) {
                        criterion = Restrictions.eq("r.id",
                                leafCond.getMembershipCond().getRoleId());
                    }
                    if (leafCond.getMembershipCond().getRoleName() != null) {
                        criterion = Restrictions.eq("r.name",
                                leafCond.getMembershipCond().getRoleName());
                    }
                }
                if (leafCond.getAttributeCond() != null && schema != null) {
                    UAttrValue attrValue = new UAttrValue();
                    try {
                        if (leafCond.getAttributeCond().getType()
                                == AttributeCond.Type.LIKE) {

                            attrValue.setStringValue(
                                    leafCond.getAttributeCond().
                                    getExpression());
                        } else {
                            attrValue =
                                    schema.getValidator().
                                    getValue(
                                    leafCond.getAttributeCond().
                                    getExpression(),
                                    attrValue);
                        }

                        criterion = Restrictions.and(
                                Restrictions.eq("a.schema.name",
                                schema.getName()),
                                getCriterion(
                                leafCond.getAttributeCond().getType(),
                                attrValue));
                    } catch (ValidationException e) {
                        LOG.error("Could not validate expression '"
                                + leafCond.getAttributeCond().
                                getExpression() + "'", e);
                    }
                }

                break;

            case NOT_LEAF:
                
                final AttributeCond attributeCondition =
                        leafCond.getAttributeCond();

                if (attributeCondition != null) {
                    if (schema == null) {
                        LOG.warn("Ignoring invalid schema '"
                                + leafCond.getAttributeCond().getSchema()
                                + "'");
                    } else {
                        UAttrValue attributeValue = new UAttrValue();
                        try {
                            if (leafCond.getAttributeCond().getType()
                                    == AttributeCond.Type.LIKE) {

                                attributeValue.setStringValue(
                                        leafCond.getAttributeCond().
                                        getExpression());
                            } else {
                                attributeValue =
                                        schema.getValidator().
                                        getValue(
                                        leafCond.getAttributeCond().
                                        getExpression(),
                                        attributeValue);
                            }

                            criterion = Restrictions.and(
                                    Restrictions.eq("a.schema.name",
                                    leafCond.getAttributeCond().getSchema()),
                                    Restrictions.not(getCriterion(
                                    leafCond.getAttributeCond().getType(),
                                    attributeValue)));

                            // if user doesn't have the attribute it won't be returned

                        } catch (ValidationException e) {
                            LOG.error("Could not validate expression '"
                                    + leafCond.getAttributeCond().
                                    getExpression() + "'", e);
                        }
                    }
                } else {
                    leafCond.setType(NodeCond.Type.LEAF);
                    criterion = Restrictions.not(
                            getCriterion(schema, leafCond));
                }
                break;

            default:
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
