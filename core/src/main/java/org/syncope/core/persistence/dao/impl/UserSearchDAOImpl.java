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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.SyncopeUserCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.ResourceCond;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.persistence.dao.UserSearchDAO;
import org.syncope.types.SchemaType;

@Repository
public class UserSearchDAOImpl extends AbstractDAOImpl
        implements UserSearchDAO {

    static final private String EMPTY_ATTR_QUERY =
            "SELECT user_id FROM user_search_attr WHERE 1=2";

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    private final Random random;

    public UserSearchDAOImpl() {
        super();

        random = new Random(Calendar.getInstance().getTimeInMillis());
    }

    private String getAdminRolesFilter(final Set<Long> adminRoles) {
        final StringBuilder adminRolesFilter = new StringBuilder();
        if (adminRoles == null || adminRoles.isEmpty()) {
            adminRolesFilter.append("SELECT syncopeUser_id AS user_id ").
                    append("FROM Membership");
        } else {
            adminRolesFilter.append("SELECT syncopeUser_id AS user_id ").
                    append("FROM Membership M1 ").
                    append("WHERE syncopeRole_id IN (");
            adminRolesFilter.append("SELECT syncopeRole_id ").
                    append("FROM Membership M2 ").
                    append("WHERE M2.syncopeUser_id=M1.syncopeUser_id ").
                    append("AND syncopeRole_id NOT IN (");
            adminRolesFilter.append(
                    "SELECT id AS syncopeRole_id FROM SyncopeRole");
            boolean firstRole = true;
            for (Long adminRoleId : adminRoles) {
                if (firstRole) {
                    adminRolesFilter.append(" WHERE");
                    firstRole = false;
                } else {
                    adminRolesFilter.append(" OR");
                }

                adminRolesFilter.append(" id=").append(adminRoleId);
            }
            adminRolesFilter.append("))");
        }

        return adminRolesFilter.toString();
    }

    @Override
    public Integer count(final Set<Long> adminRoles,
            final NodeCond searchCondition) {

        Map<Integer, Object> parameters = Collections.synchronizedMap(
                new HashMap<Integer, Object>());

        // 1. get the query string from the search condition
        StringBuilder queryString = getQuery(searchCondition, parameters);

        // 2. take into account administrative roles
        queryString.insert(0, "SELECT u.user_id FROM (");
        queryString.append(") u WHERE user_id NOT IN (");
        queryString.append(getAdminRolesFilter(adminRoles)).append(")");

        // 3. prepare the COUNT query
        queryString.insert(0, "SELECT COUNT(user_id) FROM (");
        queryString.append(") count_user_id");

        Query countQuery =
                entityManager.createNativeQuery(queryString.toString());
        fillWithParameters(countQuery, parameters);

        LOG.debug("Native count query\n{}\nwith parameters\n{}",
                queryString.toString(), parameters);

        Integer result = ((Number) countQuery.getSingleResult()).intValue();
        LOG.debug("Native count query result: {}", result);

        return result;
    }

    @Override
    public List<SyncopeUser> search(final NodeCond searchCondition) {
        return search(null, searchCondition, -1, -1);
    }

    @Override
    public List<SyncopeUser> search(final Set<Long> adminRoles,
            final NodeCond searchCondition) {

        return search(adminRoles, searchCondition, -1, -1);
    }

    @Override
    public List<SyncopeUser> search(final Set<Long> adminRoles,
            final NodeCond searchCondition,
            final int page,
            final int itemsPerPage) {

        List<SyncopeUser> result = Collections.EMPTY_LIST;

        LOG.debug("Search condition:\n{}", searchCondition);
        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition:\n{}", searchCondition);
        } else {
            try {
                result = doSearch(adminRoles, searchCondition, page,
                        itemsPerPage);
            } catch (Throwable t) {
                LOG.error("While searching users", t);
            }
        }

        return result;
    }

    @Override
    public boolean matches(final SyncopeUser user,
            final NodeCond searchCondition) {

        Map<Integer, Object> parameters = Collections.synchronizedMap(
                new HashMap<Integer, Object>());

        // 1. get the query string from the search condition
        StringBuilder queryString = getQuery(searchCondition, parameters);

        // 2. take into account the passed user
        queryString.insert(0, "SELECT u.user_id FROM (");
        queryString.append(") u WHERE user_id=:matchesUserId");

        // 3. prepare the search query
        Query query = entityManager.createNativeQuery(queryString.toString());

        // 4. populate the search query with parameter values
        fillWithParameters(query, parameters);
        query.setParameter("matchesUserId", user.getId());

        // 5. executes query
        List<SyncopeUser> result = query.getResultList();

        return !result.isEmpty();
    }

    private Integer setParameter(final Map<Integer, Object> parameters,
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

        for (Entry<Integer, Object> entry : parameters.entrySet()) {
            if (entry.getValue() instanceof Date) {
                query.setParameter("param" + entry.getKey(),
                        (Date) entry.getValue(), TemporalType.TIMESTAMP);
            } else if (entry.getValue() instanceof Boolean) {
                query.setParameter("param" + entry.getKey(),
                        ((Boolean) entry.getValue()) ? 1 : 0);
            } else {
                query.setParameter("param" + entry.getKey(), entry.getValue());
            }
        }
    }

    private List<SyncopeUser> doSearch(final Set<Long> adminRoles,
            final NodeCond nodeCond,
            final int page, final int itemsPerPage) {

        Map<Integer, Object> parameters = Collections.synchronizedMap(
                new HashMap<Integer, Object>());

        // 1. get the query string from the search condition
        final StringBuilder queryString = getQuery(nodeCond, parameters);

        // 2. take into account administrative roles
        if (queryString.charAt(0) == '(') {
            queryString.insert(0, "SELECT u.user_id FROM ");
            queryString.append(" u WHERE user_id NOT IN (");
        } else {
            queryString.insert(0, "SELECT u.user_id FROM (");
            queryString.append(") u WHERE user_id NOT IN (");
        }
        queryString.append(getAdminRolesFilter(adminRoles)).append(")");

        // 3. prepare the search query
        final Query query =
                entityManager.createNativeQuery(queryString.toString());

        // page starts from 1, while setFirtResult() starts from 0
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        // 4. populate the search query with parameter values
        fillWithParameters(query, parameters);

        LOG.debug("Native query\n{}\nwith parameters\n{}",
                queryString.toString(), parameters);

        // 5. Prepare the result (avoiding duplicates - set)
        final Set<Number> userIds = new HashSet<Number>();
        final List resultList = query.getResultList();

        //fix for HHH-5902 - bug hibernate
        if (resultList != null) {
            for (Object userId : resultList) {
                if (userId instanceof Object[]) {
                    userIds.add((Number) ((Object[]) userId)[0]);
                } else {
                    userIds.add((Number) userId);
                }
            }
        }

        final List<SyncopeUser> result =
                new ArrayList<SyncopeUser>(userIds.size());

        SyncopeUser user;
        for (Object userId : userIds) {
            user = userDAO.find(((Number) userId).longValue());
            if (user == null) {
                LOG.error("Could not find user with id {}, "
                        + "even though returned by the native query", userId);
            } else {
                result.add(user);
            }
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
                if (nodeCond.getResourceCond() != null) {
                    query.append(getQuery(nodeCond.getResourceCond(),
                            nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters));
                }
                if (nodeCond.getAttributeCond() != null) {
                    query.append(getQuery(nodeCond.getAttributeCond(),
                            nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters));
                }
                if (nodeCond.getSyncopeUserCond() != null) {
                    query.append(getQuery(nodeCond.getSyncopeUserCond(),
                            nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters));
                }
                break;

            case AND:
                query.append(getQuery(nodeCond.getLeftNodeCond(),
                        parameters)).
                        append(" AND user_id IN ( ").
                        append(getQuery(nodeCond.getRightNodeCond(),
                        parameters).
                        append(")"));
                break;

            case OR:
                query.append("(").
                        append(getQuery(nodeCond.getLeftNodeCond(),
                        parameters)).
                        append(" UNION ").
                        append(getQuery(nodeCond.getRightNodeCond(),
                        parameters).
                        append(")"));
                break;

            default:
        }

        return query;
    }

    private String getQuery(final MembershipCond cond,
            final boolean not, final Map<Integer, Object> parameters) {

        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT user_id FROM user_search WHERE ");

        if (not) {
            query.append("user_id NOT IN (");
        } else {
            query.append("user_id IN (");
        }

        query.append("SELECT DISTINCT user_id ").
                append("FROM user_search_membership WHERE ");

        if (cond.getRoleId() != null) {
            query.append("role_id=:param").append(
                    setParameter(parameters, cond.getRoleId()));
        } else if (cond.getRoleName() != null) {
            query.append("role_name=:param").append(
                    setParameter(parameters, cond.getRoleName()));
        }

        query.append(")");

        return query.toString();
    }

    private String getQuery(final ResourceCond cond,
            final boolean not, final Map<Integer, Object> parameters) {

        final StringBuilder query = new StringBuilder(
                "SELECT DISTINCT user_id FROM user_search WHERE ");

        if (not) {
            query.append("user_id NOT IN (");
        } else {
            query.append("user_id IN (");
        }

        query.append("SELECT DISTINCT user_id ").
                append("FROM user_search_resource WHERE ");

        query.append("resource_name=:param").append(
                setParameter(parameters, cond.getResourceName()));

        query.append(")");

        return query.toString();
    }

    private void fillAttributeQuery(final StringBuilder query,
            final UAttrValue attrValue, final USchema schema,
            final AttributeCond cond, final boolean not,
            final Map<Integer, Object> parameters) {

        String column = (cond instanceof SyncopeUserCond)
                ? cond.getSchema()
                : "' AND " + getFieldName(schema.getType());

        switch (cond.getType()) {

            case ISNULL:
                query.append(column).append(not ? " IS NOT NULL" : " IS NULL");
                break;

            case ISNOTNULL:
                query.append(column).append(not ? " IS NULL" : " IS NOT NULL");
                break;

            case LIKE:
                if (schema.getType() == SchemaType.String
                        || schema.getType() == SchemaType.Enum) {

                    query.append(column);
                    if (not) {
                        query.append(" NOT ");
                    }
                    query.append(" LIKE ");
                    if (!(cond instanceof SyncopeUserCond)) {
                        query.append('\'');
                    }
                    query.append(cond.getExpression()).append("'");
                } else {
                    if (!(cond instanceof SyncopeUserCond)) {
                        query.append("' AND");
                    }
                    query.append(" 1=2");
                    LOG.error("LIKE is only compatible with string schemas");
                }
                break;

            case EQ:
                query.append(column);
                if (not) {
                    query.append("<>");
                } else {
                    query.append("=");
                }
                query.append(":param").append(
                        setParameter(parameters, attrValue.getValue()));
                break;

            case GE:
                query.append(column);
                if (not) {
                    query.append("<");
                } else {
                    query.append(">=");
                }
                query.append(":param").append(
                        setParameter(parameters, attrValue.getValue()));
                break;

            case GT:
                query.append(column);
                if (not) {
                    query.append("<=");
                } else {
                    query.append(">");
                }
                query.append(":param").append(
                        setParameter(parameters, attrValue.getValue()));
                break;

            case LE:
                query.append(column);
                if (not) {
                    query.append(">");
                } else {
                    query.append("<=");
                }
                query.append(":param").append(
                        setParameter(parameters, attrValue.getValue()));
                break;

            case LT:
                query.append(column);
                if (not) {
                    query.append(">=");
                } else {
                    query.append("<");
                }
                query.append(":param").append(
                        setParameter(parameters, attrValue.getValue()));
                break;

            default:
        }
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
            case Enum:
                result = "stringvalue";
                break;

            default:
                result = null;
        }

        return result;
    }

    private String getQuery(final AttributeCond cond,
            final boolean not, final Map<Integer, Object> parameters) {

        USchema schema = schemaDAO.find(cond.getSchema(), USchema.class);
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        UAttrValue attrValue = new UAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE
                    && cond.getType() != AttributeCond.Type.ISNULL
                    && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                attrValue = schema.getValidator().
                        getValue(cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '"
                    + cond.getExpression() + "'", e);
            return EMPTY_ATTR_QUERY;
        }

        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT user_id FROM user_search_attr WHERE ").append(
                "schema_name='").append(schema.getName());
        fillAttributeQuery(query, attrValue, schema, cond, not, parameters);

        return query.toString();
    }

    private String getQuery(final SyncopeUserCond cond,
            final boolean not, final Map<Integer, Object> parameters) {

        Field syncopeUserClassField = null;
        // loop over SyncopeUser class and all superclasses searching for field
        for (Class<?> i = SyncopeUser.class;
                syncopeUserClassField == null && i != Object.class;) {

            try {
                syncopeUserClassField = i.getDeclaredField(cond.getSchema());
            } catch (Exception ignore) {
                // ignore exception
                LOG.debug("Field '{}' not found on class '{}'",
                        new String[]{cond.getSchema(), i.getSimpleName()},
                        ignore);
            } finally {
                i = i.getSuperclass();
            }
        }
        if (syncopeUserClassField == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        USchema schema = new USchema();
        schema.setName(syncopeUserClassField.getName());
        for (SchemaType type : SchemaType.values()) {
            if (syncopeUserClassField.getType().
                    getName().equals(type.getClassName())) {

                schema.setType(type);
            }
        }

        UAttrValue attrValue = new UAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE
                    && cond.getType() != AttributeCond.Type.ISNULL
                    && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                attrValue = schema.getValidator().
                        getValue(cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '"
                    + cond.getExpression() + "'", e);
            return EMPTY_ATTR_QUERY;
        }

        final StringBuilder query = new StringBuilder(
                "SELECT DISTINCT user_id FROM user_search WHERE ");

        fillAttributeQuery(query, attrValue, schema, cond, not, parameters);

        return query.toString();
    }
}
