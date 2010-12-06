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
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;
import org.syncope.core.persistence.validation.ValidationException;

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
            final UserAttributeValue attributeValue) {

        return findByAttributeValue(attributeValue, -1, -1);
    }

    @Override
    public final List<SyncopeUser> findByAttributeValue(
            final UserAttributeValue attributeValue,
            final int page, final int itemsPerPage) {

        final Query query = entityManager.createQuery(
                "SELECT u"
                + " FROM SyncopeUser u, UserAttribute ua, UserAttributeValue e "
                + " WHERE e.attribute = ua AND ua.owner = u"
                + " AND ((e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL"
                + " AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL"
                + " AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL"
                + " AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL"
                + " AND e.doubleValue = :doubleValue)) ORDER BY u.id");
        query.setParameter("stringValue", attributeValue.getStringValue());
        query.setParameter("booleanValue", attributeValue.getBooleanValue());
        query.setParameter("dateValue", attributeValue.getDateValue());
        query.setParameter("longValue", attributeValue.getLongValue());
        query.setParameter("doubleValue", attributeValue.getDoubleValue());

        query.setFirstResult(
                itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
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

        query.setFirstResult(
                itemsPerPage * (page <= 0 ? 0 : page - 1));

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
                        LOG.debug("Search query to be performed: {}", query);

                        result = query.getResultList();
                    }
                } else {
                    Criteria criteria = getBaseCriteria().
                            add(getCriterion(nodeCond));
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
                    if (!to.contains(user)) to.add(user);
                }

                result = to;
                break;

            default:
                result = Collections.EMPTY_LIST;
        }

        return result;
    }

    private Criteria getBaseCriteria() {
        Session hibernateSess = (Session) entityManager.getDelegate();
        Criteria baseCriteria = hibernateSess.createCriteria(SyncopeUser.class).
                createAlias("memberships", "m").
                createAlias("m.syncopeRole", "r").
                createAlias("attributes", "a").
                createAlias("a.values", "av");

        baseCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return baseCriteria;
    }

    private Criterion getCriterion(final NodeCond leafCond) {
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
                } else if (leafCond.getAttributeCond() != null) {
                        UserSchema userSchema = schemaDAO.find(
                                leafCond.getAttributeCond().getSchema(),
                                UserSchema.class);
                        if (userSchema == null) {
                            LOG.warn("Ignoring invalid schema '"
                                    + leafCond.getAttributeCond().getSchema()
                                    + "'");
                        } else {
                            UserAttributeValue attributeValue =
                                    new UserAttributeValue();
                            try {
                                if (leafCond.getAttributeCond().getType()
                                        == AttributeCond.Type.LIKE) {

                                    attributeValue.setStringValue(
                                            leafCond.getAttributeCond().
                                            getExpression());
                                } else {
                                    attributeValue =
                                            userSchema.getValidator().
                                            getValue(
                                            leafCond.getAttributeCond().
                                            getExpression(),
                                            attributeValue);
                                }

                                criterion = Restrictions.and(
                                        Restrictions.eq("a.schema.name",
                                        leafCond.getAttributeCond().getSchema()),
                                        getCriterion(
                                        leafCond.getAttributeCond().getType(),
                                        attributeValue));
                            } catch (ValidationException e) {
                                LOG.error("Could not validate expression '"
                                        + leafCond.getAttributeCond().
                                        getExpression() + "'", e);
                            }
                        }
                    }

                break;

            case NOT_LEAF:
                leafCond.setType(NodeCond.Type.LEAF);

                final AttributeCond attributeCondition =
                        leafCond.getAttributeCond();

                if (attributeCondition != null) {
                    UserSchema userSchema = schemaDAO.find(
                            leafCond.getAttributeCond().getSchema(),
                            UserSchema.class);
                    if (userSchema == null) {
                        LOG.warn("Ignoring invalid schema '"
                                + leafCond.getAttributeCond().getSchema()
                                + "'");
                    } else {
                        UserAttributeValue attributeValue =
                                new UserAttributeValue();
                        try {
                            if (leafCond.getAttributeCond().getType()
                                    == AttributeCond.Type.LIKE) {

                                attributeValue.setStringValue(
                                        leafCond.getAttributeCond().
                                        getExpression());
                            } else {
                                attributeValue =
                                        userSchema.getValidator().
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
                    criterion = Restrictions.not(getCriterion(leafCond));
                }
                break;

            default:
        }

        return criterion;
    }

    private Criterion getCriterion(final AttributeCond.Type type,
            final AbstractAttributeValue attributeValue) {

        Criterion result = null;
        switch (type) {
            case EQ:
                Conjunction conjunction = Restrictions.conjunction();

                if (attributeValue.getStringValue() != null) {
                    conjunction.add(Restrictions.eq("av.stringValue",
                            attributeValue.getStringValue()));
                }

                if (attributeValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.eq("av.booleanValue",
                            attributeValue.getBooleanValue()));
                }

                if (attributeValue.getLongValue() != null) {
                    conjunction.add(Restrictions.eq("av.longValue",
                            attributeValue.getLongValue()));
                }

                if (attributeValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.eq("av.doubleValue",
                            attributeValue.getDoubleValue()));
                }

                if (attributeValue.getDateValue() != null) {
                    conjunction.add(Restrictions.eq("av.dateValue",
                            attributeValue.getDateValue()));
                }

                result = conjunction;

                break;

            case GE:
                conjunction = Restrictions.conjunction();

                if (attributeValue.getStringValue() != null) {
                    conjunction.add(Restrictions.ge("av.stringValue",
                            attributeValue.getStringValue()));
                }

                if (attributeValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.ge("av.booleanValue",
                            attributeValue.getBooleanValue()));
                }

                if (attributeValue.getLongValue() != null) {
                    conjunction.add(Restrictions.ge("av.longValue",
                            attributeValue.getLongValue()));
                }

                if (attributeValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.ge("av.doubleValue",
                            attributeValue.getDoubleValue()));
                }

                if (attributeValue.getDateValue() != null) {
                    conjunction.add(Restrictions.ge("av.dateValue",
                            attributeValue.getDateValue()));
                }

                result = conjunction;

                break;

            case GT:
                conjunction = Restrictions.conjunction();

                if (attributeValue.getStringValue() != null) {
                    conjunction.add(Restrictions.gt("av.stringValue",
                            attributeValue.getStringValue()));
                }

                if (attributeValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.gt("av.booleanValue",
                            attributeValue.getBooleanValue()));
                }

                if (attributeValue.getLongValue() != null) {
                    conjunction.add(Restrictions.gt("av.longValue",
                            attributeValue.getLongValue()));
                }

                if (attributeValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.gt("av.doubleValue",
                            attributeValue.getDoubleValue()));
                }

                if (attributeValue.getDateValue() != null) {
                    conjunction.add(Restrictions.gt("av.dateValue",
                            attributeValue.getDateValue()));
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

                if (attributeValue.getStringValue() != null) {
                    conjunction.add(Restrictions.le("av.stringValue",
                            attributeValue.getStringValue()));
                }

                if (attributeValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.le("av.booleanValue",
                            attributeValue.getBooleanValue()));
                }

                if (attributeValue.getLongValue() != null) {
                    conjunction.add(Restrictions.le("av.longValue",
                            attributeValue.getLongValue()));
                }

                if (attributeValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.le("av.doubleValue",
                            attributeValue.getDoubleValue()));
                }

                if (attributeValue.getDateValue() != null) {
                    conjunction.add(Restrictions.le("av.dateValue",
                            attributeValue.getDateValue()));
                }

                result = conjunction;

                break;

            case LIKE:
                // LIKE operator is meaningful for strings only
                result = Restrictions.like("av.stringValue",
                        attributeValue.getStringValue());
                break;

            case LT:
                conjunction = Restrictions.conjunction();

                if (attributeValue.getStringValue() != null) {
                    conjunction.add(Restrictions.lt("av.stringValue",
                            attributeValue.getStringValue()));
                }

                if (attributeValue.getBooleanValue() != null) {
                    conjunction.add(Restrictions.lt("av.booleanValue",
                            attributeValue.getBooleanValue()));
                }

                if (attributeValue.getLongValue() != null) {
                    conjunction.add(Restrictions.lt("av.longValue",
                            attributeValue.getLongValue()));
                }

                if (attributeValue.getDoubleValue() != null) {
                    conjunction.add(Restrictions.lt("av.doubleValue",
                            attributeValue.getDoubleValue()));
                }

                if (attributeValue.getDateValue() != null) {
                    conjunction.add(Restrictions.lt("av.dateValue",
                            attributeValue.getDateValue()));
                }

                result = conjunction;

                break;

            default:
        }
        return result;
    }
}
