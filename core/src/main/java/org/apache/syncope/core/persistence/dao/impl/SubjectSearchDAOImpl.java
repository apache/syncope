/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.dao.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.dao.search.EntitlementCond;
import org.apache.syncope.core.persistence.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

@Repository
public class SubjectSearchDAOImpl extends AbstractDAOImpl implements SubjectSearchDAO {

    private static final String EMPTY_ATTR_QUERY = "SELECT subject_id FROM user_search_attr WHERE 1=2";

    private static final String[] SUBJECT_FIELDS = new String[] { "parent", "userOwner", "roleOwner" };

    private static final String[] ORDER_BY_NOT_ALLOWED = {
        "serialVersionUID", "password", "securityQuestion", "securityAnswer", "token", "tokenExpireTime"
    };

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired(required = false)
    private String adminUser;

    private String getAdminRolesFilter(final Set<Long> adminRoles, final SubjectType type) {
        StringBuilder adminRolesFilter = new StringBuilder("u.subject_id NOT IN (");

        if (type == SubjectType.USER) {
            adminRolesFilter.append("SELECT syncopeUser_id AS subject_id FROM Membership WHERE syncopeRole_id NOT IN (");
        }

        adminRolesFilter.append("SELECT id AS ").
                append(type == SubjectType.USER ? "syncopeRole" : "subject").
                append("_id FROM SyncopeRole");

        boolean firstRole = true;

        for (Long adminRoleId : adminRoles) {
            if (firstRole) {
                adminRolesFilter.append(" WHERE");
                firstRole = false;
            } else {
                adminRolesFilter.append(type == SubjectType.USER ? " OR" : " AND");
            }
            adminRolesFilter.append(type == SubjectType.USER ? " id=" : " id <>").append(adminRoleId);
        }

        if (type == SubjectType.USER) {
            adminRolesFilter.append(")");
        }

        return adminRolesFilter.append(')').toString();
    }

    @Override
    public int count(final Set<Long> adminRoles, final SearchCond searchCondition, final SubjectType type) {
        List<Object> parameters = Collections.synchronizedList(new ArrayList<Object>());

        // 1. get the query string from the search condition
        SearchSupport svs = new SearchSupport(type);
        StringBuilder queryString = getQuery(searchCondition, parameters, svs);

        // 2. take into account administrative roles (skips checks for admin)
        queryString.insert(0, "SELECT u.subject_id FROM (");
        queryString.append(") u ");
        if (!adminUser.equals(EntitlementUtil.getAuthenticatedUsername())) {
            queryString.append("WHERE ").append(getAdminRolesFilter(adminRoles, type));
        }

        // 3. prepare the COUNT query
        queryString.insert(0, "SELECT COUNT(subject_id) FROM (");
        queryString.append(") count_subject_id");

        Query countQuery = entityManager.createNativeQuery(queryString.toString());
        fillWithParameters(countQuery, parameters);

        LOG.debug("Native count query\n{}\nwith parameters\n{}", queryString.toString(), parameters);

        int result = ((Number) countQuery.getSingleResult()).intValue();
        LOG.debug("Native count query result: {}", result);

        return result;
    }

    @Override
    public <T extends AbstractSubject> List<T> search(final Set<Long> adminRoles, final SearchCond searchCondition,
            final SubjectType type) {

        return search(adminRoles, searchCondition, Collections.<OrderByClause>emptyList(), type);
    }

    @Override
    public <T extends AbstractSubject> List<T> search(final Set<Long> adminRoles, final SearchCond searchCondition,
            final List<OrderByClause> orderBy, final SubjectType type) {

        return search(adminRoles, searchCondition, -1, -1, orderBy, type);
    }

    @Override
    public <T extends AbstractSubject> List<T> search(final Set<Long> adminRoles, final SearchCond searchCondition,
            final int page, final int itemsPerPage, final List<OrderByClause> orderBy,
            final SubjectType type) {

        List<T> result = Collections.<T>emptyList();

        if (adminRoles != null && (!adminRoles.isEmpty() || roleDAO.findAll().isEmpty())) {
            LOG.debug("Search condition:\n{}", searchCondition);

            if (searchCondition != null && searchCondition.isValid()) {
                try {
                    result = doSearch(adminRoles, searchCondition, page, itemsPerPage, orderBy, type);
                } catch (Exception e) {
                    LOG.error("While searching for {}", type, e);
                }
            } else {
                LOG.error("Invalid search condition:\n{}", searchCondition);
            }
        }

        return result;
    }

    @Override
    public <T extends AbstractSubject> boolean matches(
            final T subject, final SearchCond searchCondition, final SubjectType type) {

        List<Object> parameters = Collections.synchronizedList(new ArrayList<Object>());

        // 1. get the query string from the search condition
        SearchSupport svs = new SearchSupport(type);
        StringBuilder queryString = getQuery(searchCondition, parameters, svs);

        boolean matches;
        if (queryString.length() == 0) {
            // Could be empty: got into a role search with a single membership condition ...
            matches = false;
        } else {
            // 2. take into account the passed user
            queryString.insert(0, "SELECT u.subject_id FROM (");
            queryString.append(") u WHERE subject_id=?").append(setParameter(parameters, subject.getId()));

            // 3. prepare the search query
            Query query = entityManager.createNativeQuery(queryString.toString());

            // 4. populate the search query with parameter values
            fillWithParameters(query, parameters);

            // 5. executes query
            matches = !query.getResultList().isEmpty();
        }

        return matches;
    }

    private int setParameter(final List<Object> parameters, final Object parameter) {
        int key;
        synchronized (parameters) {
            parameters.add(parameter);
            key = parameters.size();
        }

        return key;
    }

    private void fillWithParameters(final Query query, final List<Object> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof Date) {
                query.setParameter(i + 1, (Date) parameters.get(i), TemporalType.TIMESTAMP);
            } else if (parameters.get(i) instanceof Boolean) {
                query.setParameter(i + 1, ((Boolean) parameters.get(i))
                        ? 1
                        : 0);
            } else {
                query.setParameter(i + 1, parameters.get(i));
            }
        }
    }

    private StringBuilder buildSelect(final OrderBySupport obs) {
        final StringBuilder select = new StringBuilder("SELECT u.subject_id");

        for (OrderBySupport.Item item : obs.items) {
            select.append(',').append(item.select);
        }
        select.append(" FROM ");

        return select;
    }

    private StringBuilder buildWhere(final SearchSupport svs, final OrderBySupport obs) {
        final StringBuilder where = new StringBuilder(" u");
        for (SearchSupport.SearchView searchView : obs.views) {
            where.append(',');
            if (searchView.name.equals(svs.attr().name)) {
                where.append(" (SELECT * FROM ").append(searchView.name);

                if (svs.nonMandatorySchemas || obs.nonMandatorySchemas) {
                    where.append(" UNION SELECT * FROM ").append(svs.nullAttr().name);
                }

                where.append(')');
            } else {
                where.append(searchView.name);
            }
            where.append(' ').append(searchView.alias);
        }
        where.append(" WHERE ");
        for (SearchSupport.SearchView searchView : obs.views) {
            where.append("u.subject_id=").append(searchView.alias).append(".subject_id AND ");
        }

        for (OrderBySupport.Item item : obs.items) {
            if (StringUtils.isNotBlank(item.where)) {
                where.append(item.where).append(" AND ");
            }
        }

        return where;
    }

    private StringBuilder buildOrderBy(final OrderBySupport obs) {
        final StringBuilder orderBy = new StringBuilder();

        for (OrderBySupport.Item item : obs.items) {
            orderBy.append(item.orderBy).append(',');
        }
        if (!obs.items.isEmpty()) {
            orderBy.insert(0, " ORDER BY ");
            orderBy.deleteCharAt(orderBy.length() - 1);
        }

        return orderBy;
    }

    protected List<OrderByClause> filterOrderBy(final List<OrderByClause> orderBy) {
        List<OrderByClause> result = new ArrayList<OrderByClause>();

        for (OrderByClause clause : orderBy) {
            if (!ArrayUtils.contains(ORDER_BY_NOT_ALLOWED, clause.getField())) {
                result.add(clause);
            }
        }

        return result;
    }

    private OrderBySupport parseOrderBy(final SearchSupport svs, final List<OrderByClause> orderBy) {
        final AttributableUtil attrUtil = AttributableUtil.getInstance(svs.type.asAttributableType());

        OrderBySupport obs = new OrderBySupport();

        for (OrderByClause clause : filterOrderBy(orderBy)) {
            OrderBySupport.Item item = new OrderBySupport.Item();

            Field subjectField = ReflectionUtils.findField(attrUtil.attributableClass(), clause.getField());
            if (subjectField == null) {
                AbstractNormalSchema schema = schemaDAO.find(clause.getField(), attrUtil.schemaClass());
                if (schema != null) {
                    // keep track of involvement of non-mandatory schemas in the order by clauses
                    obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

                    if (schema.isUniqueConstraint()) {
                        obs.views.add(svs.uniqueAttr());

                        item.select = new StringBuilder().
                                append(svs.uniqueAttr().alias).append('.').append(svs.fieldName(schema.getType())).
                                append(" AS ").append(clause.getField()).toString();
                        item.where = new StringBuilder().
                                append(svs.uniqueAttr().alias).
                                append(".schema_name='").append(clause.getField()).append("'").toString();
                        item.orderBy = clause.getField() + " " + clause.getDirection().name();
                    } else {
                        obs.views.add(svs.attr());

                        item.select = new StringBuilder().
                                append(svs.attr().alias).append('.').append(svs.fieldName(schema.getType())).
                                append(" AS ").append(clause.getField()).toString();
                        item.where = new StringBuilder().
                                append(svs.attr().alias).
                                append(".schema_name='").append(clause.getField()).append("'").toString();
                        item.orderBy = clause.getField() + " " + clause.getDirection().name();
                    }
                }
            } else {
                obs.views.add(svs.field());

                item.select = svs.field().alias + "." + clause.getField();
                item.where = StringUtils.EMPTY;
                item.orderBy = svs.field().alias + "." + clause.getField() + " " + clause.getDirection().name();
            }

            if (item.isEmpty()) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                obs.items.add(item);
            }
        }

        return obs;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractSubject> List<T> doSearch(
            final Set<Long> adminRoles,
            final SearchCond cond,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderBy,
            final SubjectType type) {

        List<Object> parameters = Collections.synchronizedList(new ArrayList<Object>());

        // 1. get the query string from the search condition
        SearchSupport svs = new SearchSupport(type);
        StringBuilder queryString = getQuery(cond, parameters, svs);

        // 2. take into account administrative roles and ordering
        OrderBySupport obs = parseOrderBy(svs, orderBy);
        if (queryString.charAt(0) == '(') {
            queryString.insert(0, buildSelect(obs));
            queryString.append(buildWhere(svs, obs));
        } else {
            queryString.insert(0, buildSelect(obs).append('('));
            queryString.append(')').append(buildWhere(svs, obs));
        }
        // skips checks for admin
        if (adminUser.equals(EntitlementUtil.getAuthenticatedUsername())) {
            String qss = queryString.toString();
            if (qss.endsWith(" WHERE ")) {
                queryString.setLength(queryString.length() - 6);
            } else if (qss.endsWith(" AND ")) {
                queryString.setLength(queryString.length() - 4);
            }
        } else {
            queryString.append(getAdminRolesFilter(adminRoles, type));
        }
        queryString.append(buildOrderBy(obs));

        // 3. prepare the search query
        Query query = entityManager.createNativeQuery(queryString.toString());

        // 4. page starts from 1, while setFirtResult() starts from 0
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        // 5. populate the search query with parameter values
        fillWithParameters(query, parameters);

        LOG.debug("Native query\n{}\nwith parameters\n{}", queryString.toString(), parameters);

        // 6. Prepare the result (avoiding duplicates)
        List<T> result = new ArrayList<T>();

        for (Object subjectId : query.getResultList()) {
            long actualId;
            if (subjectId instanceof Object[]) {
                actualId = ((Number) ((Object[]) subjectId)[0]).longValue();
            } else {
                actualId = ((Number) subjectId).longValue();
            }

            T subject = type == SubjectType.USER
                    ? (T) userDAO.find(actualId)
                    : (T) roleDAO.find(actualId);
            if (subject == null) {
                LOG.error("Could not find {} with id {}, even though returned by the native query",
                        type, subjectId);
            } else if (!result.contains(subject)) {
                result.add(subject);
            }
        }

        return result;
    }

    private StringBuilder getQuery(final SearchCond cond, final List<Object> parameters, final SearchSupport svs) {
        StringBuilder query = new StringBuilder();

        switch (cond.getType()) {

            case LEAF:
            case NOT_LEAF:
                if (cond.getMembershipCond() != null && SubjectType.USER == svs.type) {
                    query.append(getQuery(cond.getMembershipCond(), cond.getType() == SearchCond.Type.NOT_LEAF,
                            parameters, svs));
                }
                if (cond.getResourceCond() != null) {
                    query.append(getQuery(cond.getResourceCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                }
                if (cond.getEntitlementCond() != null) {
                    query.append(getQuery(cond.getEntitlementCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                }
                if (cond.getAttributeCond() != null) {
                    query.append(getQuery(cond.getAttributeCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                }
                if (cond.getSubjectCond() != null) {
                    query.append(getQuery(cond.getSubjectCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                }
                break;

            case AND:
                String andSubQuery = getQuery(cond.getLeftNodeCond(), parameters, svs).toString();
                // Add extra parentheses
                andSubQuery = andSubQuery.replaceFirst("WHERE ", "WHERE (");
                query.append(andSubQuery).
                        append(" AND subject_id IN ( ").
                        append(getQuery(cond.getRightNodeCond(), parameters, svs)).
                        append("))");
                break;

            case OR:
                String orSubQuery = getQuery(cond.getLeftNodeCond(), parameters, svs).toString();
                // Add extra parentheses
                orSubQuery = orSubQuery.replaceFirst("WHERE ", "WHERE (");
                query.append(orSubQuery).
                        append(" OR subject_id IN ( ").
                        append(getQuery(cond.getRightNodeCond(), parameters, svs)).
                        append("))");
                break;

            default:
        }

        return query;
    }

    private String getQuery(final MembershipCond cond, final boolean not, final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("subject_id NOT IN (");
        } else {
            query.append("subject_id IN (");
        }

        query.append("SELECT DISTINCT subject_id ").append("FROM ").
                append(svs.membership().name).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRoleId())).
                append(')');

        return query.toString();
    }

    private String getQuery(
            final ResourceCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        final StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("subject_id NOT IN (");
        } else {
            query.append("subject_id IN (");
        }

        query.append("SELECT DISTINCT subject_id FROM ").
                append(svs.resource().name).
                append(" WHERE resource_name=?").
                append(setParameter(parameters, cond.getResourceName()));

        if (svs.type == SubjectType.USER) {
            query.append(" UNION SELECT DISTINCT subject_id FROM ").
                    append(svs.roleResource().name).
                    append(" WHERE resource_name=?").
                    append(setParameter(parameters, cond.getResourceName()));
        }

        query.append(')');

        return query.toString();
    }

    private String getQuery(final EntitlementCond cond, final boolean not, final List<Object> parameters,
            final SearchSupport svs) {

        final StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(svs.entitlements().name).
                append(" WHERE entitlement_name ");
        if (not) {
            query.append(" NOT ");
        }
        query.append(" LIKE ?").append(setParameter(parameters, cond.getExpression()));

        return query.toString();
    }

    private void fillAttributeQuery(final StringBuilder query, final AbstractAttrValue attrValue,
            final AbstractNormalSchema schema, final AttributeCond cond, final boolean not,
            final List<Object> parameters, final SearchSupport svs) {

        // activate ignoreCase only for EQ and LIKE operators
        boolean ignoreCase = AttributeCond.Type.ILIKE == cond.getType() || AttributeCond.Type.IEQ == cond.getType();

        String column = (cond instanceof SubjectCond) ? cond.getSchema() : svs.fieldName(schema.getType());
        if (ignoreCase) {
            column = "LOWER (" + column + ")";
        }
        if (!(cond instanceof SubjectCond)) {
            column = "' AND " + column;
        }

        switch (cond.getType()) {

            case ISNULL:
                query.append(column).append(not
                        ? " IS NOT NULL"
                        : " IS NULL");
                break;

            case ISNOTNULL:
                query.append(column).append(not
                        ? " IS NULL"
                        : " IS NOT NULL");
                break;

            case ILIKE:
            case LIKE:
                if (schema.getType() == AttributeSchemaType.String || schema.getType() == AttributeSchemaType.Enum) {
                    query.append(column);
                    if (not) {
                        query.append(" NOT ");
                    }
                    query.append(" LIKE ");
                    if (ignoreCase) {
                        query.append("LOWER(?").append(setParameter(parameters, cond.getExpression())).append(')');
                    } else {
                        query.append('?').append(setParameter(parameters, cond.getExpression()));
                    }
                } else {
                    if (!(cond instanceof SubjectCond)) {
                        query.append("' AND");
                    }
                    query.append(" 1=2");
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
                break;

            case IEQ:
            case EQ:
                query.append(column);
                if (not) {
                    query.append("<>");
                } else {
                    query.append('=');
                }
                if (ignoreCase) {
                    query.append("LOWER(?").append(setParameter(parameters, attrValue.getValue())).append(')');
                } else {
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                }
                break;

            case GE:
                query.append(column);
                if (not) {
                    query.append('<');
                } else {
                    query.append(">=");
                }
                query.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case GT:
                query.append(column);
                if (not) {
                    query.append("<=");
                } else {
                    query.append('>');
                }
                query.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case LE:
                query.append(column);
                if (not) {
                    query.append('>');
                } else {
                    query.append("<=");
                }
                query.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case LT:
                query.append(column);
                if (not) {
                    query.append(">=");
                } else {
                    query.append('<');
                }
                query.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            default:
        }
    }

    private String getQuery(
            final AttributeCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        final AttributableUtil attrUtil = AttributableUtil.getInstance(svs.type.asAttributableType());

        AbstractNormalSchema schema = schemaDAO.find(cond.getSchema(), attrUtil.schemaClass());
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        // keep track of involvement of non-mandatory schemas in the search condition
        svs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        AbstractAttrValue attrValue = attrUtil.newAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE
                    && cond.getType() != AttributeCond.Type.ILIKE
                    && cond.getType() != AttributeCond.Type.ISNULL
                    && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                schema.getValidator().validate(cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '" + cond.getExpression() + "'", e);
            return EMPTY_ATTR_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ");
        if (cond.getType() == AttributeCond.Type.ISNOTNULL) {
            query.append(svs.field().name).
                    append(" WHERE subject_id NOT IN (SELECT subject_id FROM ").
                    append(svs.nullAttr().name).
                    append(" WHERE schema_name='").append(schema.getName()).append("')");
        } else if (cond.getType() == AttributeCond.Type.ISNULL) {
            query.append(svs.nullAttr().name).
                    append(" WHERE schema_name='").append(schema.getName()).append("'");
        } else {
            if (schema.isUniqueConstraint()) {
                query.append(svs.uniqueAttr().name);
            } else {
                query.append(svs.attr().name);
            }
            query.append(" WHERE schema_name='").append(schema.getName());

            fillAttributeQuery(query, attrValue, schema, cond, not, parameters, svs);
        }

        return query.toString();
    }

    @SuppressWarnings("rawtypes")
    private String getQuery(
            final SubjectCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        final AttributableUtil attrUtil = AttributableUtil.getInstance(svs.type.asAttributableType());

        int subjFieldIdx = ArrayUtils.indexOf(SUBJECT_FIELDS, StringUtils.substringBeforeLast(cond.getSchema(), "_"));
        Field subjectField = ReflectionUtils.findField(
                attrUtil.attributableClass(),
                subjFieldIdx == -1 ? cond.getSchema() : SUBJECT_FIELDS[subjFieldIdx]);
        if (subjectField == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        AbstractNormalSchema schema = attrUtil.newSchema();
        schema.setName(subjectField.getName());
        for (AttributeSchemaType attrSchemaType : AttributeSchemaType.values()) {
            if (subjectField.getType().isAssignableFrom(attrSchemaType.getType())) {
                schema.setType(attrSchemaType);
            }
        }

        // Deal with subject Integer fields logically mapping to boolean values
        // (SyncopeRole.inheritAttrs, for example)
        boolean foundBooleanMin = false;
        boolean foundBooleanMax = false;
        if (Integer.class.equals(subjectField.getType())) {
            for (Annotation annotation : subjectField.getAnnotations()) {
                if (Min.class.equals(annotation.annotationType())) {
                    foundBooleanMin = ((Min) annotation).value() == 0;
                } else if (Max.class.equals(annotation.annotationType())) {
                    foundBooleanMax = ((Max) annotation).value() == 1;
                }
            }
        }
        if (foundBooleanMin && foundBooleanMax) {
            schema.setType(AttributeSchemaType.Boolean);
        }

        // Deal with subject fields representing relationships to other entities
        // Only _id and _name are suppored
        if (subjectField.getType().getAnnotation(Entity.class) != null) {
            if (BeanUtils.findDeclaredMethodWithMinimalParameters(subjectField.getType(), "getId") != null) {
                cond.setSchema(cond.getSchema() + "_id");
                schema.setType(AttributeSchemaType.Long);
            } else if (BeanUtils.findDeclaredMethodWithMinimalParameters(subjectField.getType(), "getName") != null) {
                cond.setSchema(cond.getSchema() + "_name");
                schema.setType(AttributeSchemaType.String);
            }
        }

        AbstractAttrValue attrValue = attrUtil.newAttrValue();
        if (cond.getType() != AttributeCond.Type.LIKE
                && cond.getType() != AttributeCond.Type.ILIKE
                && cond.getType() != AttributeCond.Type.ISNULL
                && cond.getType() != AttributeCond.Type.ISNOTNULL) {

            try {
                schema.getValidator().validate(cond.getExpression(), attrValue);
            } catch (ValidationException e) {
                LOG.error("Could not validate expression '" + cond.getExpression() + "'", e);
                return EMPTY_ATTR_QUERY;
            }
        }

        final StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(svs.field().name).append(" WHERE ");

        fillAttributeQuery(query, attrValue, schema, cond, not, parameters, svs);

        return query.toString();
    }
}
