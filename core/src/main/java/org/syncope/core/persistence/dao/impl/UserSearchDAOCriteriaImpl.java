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
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserSearchDAO;

@Repository
public class UserSearchDAOCriteriaImpl extends AbstractUserSearchDAOImpl
        implements UserSearchDAO {

    @Autowired
    private SchemaDAO schemaDAO;

    @Override
    protected List<SyncopeUser> doSearch(final NodeCond nodeCond,
            final int page,
            final int itemsPerPage,
            final PaginatedResult paginatedResult) {

        List<SyncopeUser> result = doSearch(nodeCond);

        if (paginatedResult != null) {
            paginatedResult.setTotalRecords(result.size());
        }

        // TODO: temporary solution to the paginated search
        int from = itemsPerPage * (page <= 0 ? 0 : page - 1);

        int to = itemsPerPage <= 0 || from + itemsPerPage > result.size()
                ? result.size() : from + itemsPerPage;

        return from > to ? Collections.EMPTY_LIST : result.subList(from, to);
    }

    private List<SyncopeUser> doSearch(final NodeCond nodeCond) {
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
