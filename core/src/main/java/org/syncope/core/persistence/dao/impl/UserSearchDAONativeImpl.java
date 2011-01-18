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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.PaginatedResult;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.types.SchemaType;

@Repository
public class UserSearchDAONativeImpl extends AbstractUserSearchDAOImpl
        implements UserSearchDAO {

    static final private String EMPTY_ATTR_QUERY =
            "SELECT user_id FROM user_search_attr WHERE 1=2";

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    private final Random random;

    public UserSearchDAONativeImpl() {
        super();

        random = new Random(Calendar.getInstance().getTimeInMillis());
    }

    private Integer setParameter(final Random random,
            final Map<Integer, Object> parameters,
            final Object parameter) {

        Integer key;
        synchronized (parameters) {
            do {
                key = random.nextInt(Integer.MAX_VALUE);
            } while (parameters.containsKey(key));

            parameters.put(key, parameter);
        }

        return key;
    }

    private void fillWithParameters(final Query query,
            final Map<Integer, Object> parameters) {

        for (Integer key : parameters.keySet()) {
            if (parameters.get(key) instanceof Date) {
                query.setParameter("param" + key,
                        (Date) parameters.get(key), TemporalType.TIMESTAMP);
            } else {
                query.setParameter("param" + key, parameters.get(key));
            }
        }
    }

    @Override
    protected List<SyncopeUser> doSearch(final NodeCond nodeCond,
            final int page,
            final int itemsPerPage,
            final PaginatedResult paginatedResult) {

        Map<Integer, Object> parameters = Collections.synchronizedMap(
                new HashMap<Integer, Object>());

        StringBuilder queryString = getQuery(nodeCond, parameters);

        Query query = entityManager.createNativeQuery(queryString.toString());
        // @page starts from 1, while setFirtResult() starts from 0
        if (page >= 1) {
            query.setFirstResult(page - 1);
        }
        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }
        fillWithParameters(query, parameters);

        LOG.debug("Native query\n{}\nwith parameters\n{}",
                query, parameters);

        // Avoiding duplicates (set)
        Set<Number> userIds = new HashSet<Number>();
        userIds.addAll(query.getResultList());

        List<SyncopeUser> result =
                new ArrayList<SyncopeUser>(userIds.size());

        SyncopeUser user;
        for (Number userId : userIds) {
            user = userDAO.find(userId.longValue());
            if (user == null) {
                LOG.error("Could not find user with id {}, "
                        + "even though returned by the native query", userId);
            } else {
                result.add(user);
            }
        }

        if (paginatedResult != null) {
            queryString.insert(0, "SELECT COUNT(user_id) WHERE user_id IN (");
            queryString.append(")");

            Query countQuery =
                    entityManager.createNativeQuery(queryString.toString());
            fillWithParameters(countQuery, parameters);

            LOG.debug("Native count query\n{}\nwith parameters\n{}",
                    countQuery, parameters);

            paginatedResult.setTotalRecords(
                    ((Long) countQuery.getSingleResult()).intValue());

            LOG.debug("Native count query result: {}",
                    paginatedResult.getTotalRecords());
        }

        return result;
    }

    private StringBuilder getQuery(final NodeCond nodeCond,
            final Map<Integer, Object> parameters) {

        StringBuilder query = new StringBuilder();

        switch (nodeCond.getType()) {

            case LEAF:
            case NOT_LEAF:
                if (nodeCond.getMembershipCond() != null) {
                    query.append(getQuery(nodeCond.getMembershipCond(),
                            nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters));
                }
                if (nodeCond.getAttributeCond() != null) {
                    query.append(getQuery(nodeCond.getAttributeCond(),
                            nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters));
                }
                break;

            case AND:
                query.append(getQuery(nodeCond.getLeftNodeCond(),
                        parameters)).
                        append(" INTERSECT ").
                        append(getQuery(nodeCond.getRightNodeCond(),
                        parameters));
                break;

            case OR:
                query.append(getQuery(nodeCond.getLeftNodeCond(),
                        parameters)).
                        append(" UNION ").
                        append(getQuery(nodeCond.getRightNodeCond(),
                        parameters));
                break;

            default:
        }

        return query;
    }

    private String getQuery(final MembershipCond cond,
            final boolean not,
            final Map<Integer, Object> parameters) {

        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT user_id FROM user_search_membership WHERE ");

        if (not) {
            query.append("user_id NOT IN (").
                    append("SELECT DISTINCT user_id ").
                    append("FROM user_search_membership WHERE ");
        }

        Integer paramKey;
        if (cond.getRoleId() != null) {
            paramKey = setParameter(random, parameters, cond.getRoleId());
            query.append("role_id=:param").append(paramKey);
        } else if (cond.getRoleName() != null) {
            paramKey = setParameter(random, parameters, cond.getRoleName());
            query.append("role_name=:param").append(paramKey);
        }

        if (not) {
            query.append(")");
        }


        return query.toString();
    }

    private String getFieldName(final SchemaType type) {
        String result;

        switch (type) {
            case Boolean:
                result = "booleanvalue";
                break;

            case Date:
                result = "datevalue";
                break;

            case Double:
                result = "doublevalue";
                break;

            case Long:
                result = "longvalue";
                break;

            case String:
                result = "stringvalue";
                break;

            default:
                result = null;
        }

        return result;
    }

    private String getQuery(final AttributeCond cond,
            final boolean not,
            final Map<Integer, Object> parameters) {

        USchema schema = schemaDAO.find(cond.getSchema(), USchema.class);
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        UAttrValue attrValue = new UAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE) {
                attrValue = schema.getValidator().
                        getValue(cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '"
                    + cond.getExpression() + "'", e);
            return EMPTY_ATTR_QUERY;
        }

        if (not) {
            switch (cond.getType()) {

                case ISNULL:
                    cond.setType(AttributeCond.Type.ISNOTNULL);
                    break;

                case ISNOTNULL:
                    cond.setType(AttributeCond.Type.ISNULL);
                    break;

                case GE:
                    cond.setType(AttributeCond.Type.LT);
                    break;

                case GT:
                    cond.setType(AttributeCond.Type.LE);
                    break;

                case LE:
                    cond.setType(AttributeCond.Type.GT);
                    break;

                case LT:
                    cond.setType(AttributeCond.Type.GE);
                    break;

                default:
            }
        }

        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT user_id FROM user_search_attr WHERE ").append(
                "schema_name='").append(schema.getName());

        Integer paramKey;
        switch (cond.getType()) {

            case ISNULL:
                query.append("' AND ").append(getFieldName(schema.getType())).
                        append(" IS NULL");
                break;

            case ISNOTNULL:
                query.append("' AND ").append(getFieldName(schema.getType())).
                        append(" IS NOT NULL");
                break;

            case LIKE:
                query.append("' AND ").append(getFieldName(schema.getType()));
                if (not) {
                    query.append(" NOT ");
                }
                query.append(" LIKE '").append(cond.getExpression()).
                        append("'");
                break;

            case EQ:
                paramKey = setParameter(random, parameters,
                        attrValue.getValue());
                query.append("' AND ").append(getFieldName(schema.getType()));
                if (not) {
                    query.append("<>");
                } else {
                    query.append("=");
                }
                query.append(":param").append(paramKey);
                break;

            case GE:
                paramKey = setParameter(random, parameters,
                        attrValue.getValue());
                query.append("' AND ").append(getFieldName(schema.getType())).
                        append(">=:param").append(paramKey);
                break;

            case GT:
                paramKey = setParameter(random, parameters,
                        attrValue.getValue());
                query.append("' AND ").append(getFieldName(schema.getType())).
                        append(">:param").append(paramKey);
                break;

            case LE:
                paramKey = setParameter(random, parameters,
                        attrValue.getValue());
                query.append("' AND ").append(getFieldName(schema.getType())).
                        append("<=:param").append(paramKey);
                break;

            case LT:
                paramKey = setParameter(random, parameters,
                        attrValue.getValue());
                query.append("' AND ").append(getFieldName(schema.getType())).
                        append("<:param").append(paramKey);
                break;

            default:
        }

        return query.toString();
    }
}
