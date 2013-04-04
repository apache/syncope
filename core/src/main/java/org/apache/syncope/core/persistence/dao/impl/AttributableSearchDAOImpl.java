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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.search.AttributableCond;
import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.common.search.EntitlementCond;
import org.apache.syncope.common.search.MembershipCond;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.search.ResourceCond;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

@Repository
public class AttributableSearchDAOImpl extends AbstractDAOImpl implements AttributableSearchDAO {

    static final private String EMPTY_ATTR_QUERY = "SELECT subject_id FROM user_search_attr WHERE 1=2";

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    private String getAdminRolesFilter(final Set<Long> adminRoles, final AttributableUtil attrUtil) {
        final StringBuilder adminRolesFilter = new StringBuilder();

        if (attrUtil.getType() == AttributableType.USER) {
            adminRolesFilter.append("SELECT syncopeUser_id AS subject_id FROM Membership M1 WHERE syncopeRole_id IN (").
                    append("SELECT syncopeRole_id FROM Membership M2 WHERE M2.syncopeUser_id=M1.syncopeUser_id ").
                    append("AND syncopeRole_id NOT IN (");
        }

        adminRolesFilter.append("SELECT id AS ").
                append(attrUtil.getType() == AttributableType.USER ? "syncopeRole" : "subject").
                append("_id FROM SyncopeRole");

        boolean firstRole = true;

        for (Long adminRoleId : adminRoles) {
            if (firstRole) {
                adminRolesFilter.append(" WHERE");
                firstRole = false;
            } else {
                adminRolesFilter.append(attrUtil.getType() == AttributableType.USER ? " OR" : " AND");
            }
            adminRolesFilter.append(attrUtil.getType() == AttributableType.USER
                    ? " id=" : " id <>").append(adminRoleId);
        }

        if (attrUtil.getType() == AttributableType.USER) {
            adminRolesFilter.append("))");
        }

        return adminRolesFilter.toString();
    }

    @Override
    public int count(final Set<Long> adminRoles, final NodeCond searchCondition, final AttributableUtil attrUtil) {
        List<Object> parameters = Collections.synchronizedList(new ArrayList<Object>());

        // 1. get the query string from the search condition
        StringBuilder queryString = getQuery(searchCondition, parameters, attrUtil);

        // 2. take into account administrative roles
        queryString.insert(0, "SELECT u.subject_id FROM (");
        queryString.append(") u WHERE subject_id NOT IN (");
        queryString.append(getAdminRolesFilter(adminRoles, attrUtil)).append(")");

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
    public <T extends AbstractAttributable> List<T> search(final Set<Long> adminRoles, final NodeCond searchCondition,
            final AttributableUtil attrUtil) {

        return search(adminRoles, searchCondition, -1, -1, attrUtil);
    }

    @Override
    public <T extends AbstractAttributable> List<T> search(final Set<Long> adminRoles, final NodeCond searchCondition,
            final int page, final int itemsPerPage, final AttributableUtil attrUtil) {

        List<T> result = Collections.<T>emptyList();

        if (adminRoles != null && (!adminRoles.isEmpty() || roleDAO.findAll().isEmpty())) {
            LOG.debug("Search condition:\n{}", searchCondition);

            if (searchCondition.isValid()) {
                try {
                    result = doSearch(adminRoles, searchCondition, page, itemsPerPage, attrUtil);
                } catch (Exception e) {
                    LOG.error("While searching for {}", attrUtil.getType(), e);
                }
            } else {
                LOG.error("Invalid search condition:\n{}", searchCondition);
            }
        }

        return result;
    }

    @Override
    public <T extends AbstractAttributable> boolean matches(final T user, final NodeCond searchCondition,
            final AttributableUtil attrUtil) {

        List<Object> parameters = Collections.synchronizedList(new ArrayList<Object>());

        // 1. get the query string from the search condition
        StringBuilder queryString = getQuery(searchCondition, parameters, attrUtil);

        // 2. take into account the passed user
        queryString.insert(0, "SELECT u.subject_id FROM (");
        queryString.append(") u WHERE subject_id=?").append(setParameter(parameters, user.getId()));

        // 3. prepare the search query
        Query query = entityManager.createNativeQuery(queryString.toString());

        // 4. populate the search query with parameter values
        fillWithParameters(query, parameters);

        // 5. executes query
        return !query.getResultList().isEmpty();
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

    @SuppressWarnings("unchecked")
    private <T extends AbstractAttributable> List<T> doSearch(final Set<Long> adminRoles, final NodeCond nodeCond,
            final int page, final int itemsPerPage, final AttributableUtil attrUtil) {

        List<Object> parameters = Collections.synchronizedList(new ArrayList<Object>());

        // 1. get the query string from the search condition
        final StringBuilder queryString = getQuery(nodeCond, parameters, attrUtil);

        // 2. take into account administrative roles
        if (queryString.charAt(0) == '(') {
            queryString.insert(0, "SELECT u.subject_id FROM ");
            queryString.append(" u WHERE subject_id NOT IN (");
        } else {
            queryString.insert(0, "SELECT u.subject_id FROM (");
            queryString.append(") u WHERE subject_id NOT IN (");
        }
        queryString.append(getAdminRolesFilter(adminRoles, attrUtil)).append(")");

        // 3. prepare the search query
        final Query query = entityManager.createNativeQuery(queryString.toString());

        // page starts from 1, while setFirtResult() starts from 0
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage >= 0) {
            query.setMaxResults(itemsPerPage);
        }

        // 4. populate the search query with parameter values
        fillWithParameters(query, parameters);

        LOG.debug("Native query\n{}\nwith parameters\n{}", queryString.toString(), parameters);

        // 5. Prepare the result (avoiding duplicates - set)
        final Set<Number> subjectIds = new HashSet<Number>();
        final List resultList = query.getResultList();

        //fix for HHH-5902 - bug hibernate
        if (resultList != null) {
            for (Object userId : resultList) {
                if (userId instanceof Object[]) {
                    subjectIds.add((Number) ((Object[]) userId)[0]);
                } else {
                    subjectIds.add((Number) userId);
                }
            }
        }

        final List<T> result = new ArrayList<T>(subjectIds.size());

        for (Object subjectId : subjectIds) {
            T subject = attrUtil.getType() == AttributableType.USER
                    ? (T) userDAO.find(((Number) subjectId).longValue())
                    : (T) roleDAO.find(((Number) subjectId).longValue());
            if (subject == null) {
                LOG.error("Could not find {} with id {}, even though returned by the native query",
                        attrUtil.getType(), subjectId);
            } else {
                result.add(subject);
            }
        }

        return result;
    }

    private StringBuilder getQuery(final NodeCond nodeCond, final List<Object> parameters,
            final AttributableUtil attrUtil) {

        StringBuilder query = new StringBuilder();

        switch (nodeCond.getType()) {

            case LEAF:
            case NOT_LEAF:
                if (nodeCond.getMembershipCond() != null && AttributableType.USER == attrUtil.getType()) {
                    query.append(getQuery(nodeCond.getMembershipCond(), nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters, attrUtil));
                }
                if (nodeCond.getResourceCond() != null) {
                    query.append(getQuery(nodeCond.getResourceCond(), nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters, attrUtil));
                }
                if (nodeCond.getEntitlementCond() != null) {
                    query.append(getQuery(nodeCond.getEntitlementCond(), nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters));
                }
                if (nodeCond.getAttributeCond() != null) {
                    query.append(getQuery(nodeCond.getAttributeCond(), nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters, attrUtil));
                }
                if (nodeCond.getAttributableCond() != null) {
                    query.append(getQuery(nodeCond.getAttributableCond(), nodeCond.getType() == NodeCond.Type.NOT_LEAF,
                            parameters, attrUtil));
                }
                break;

            case AND:
                query.append(getQuery(nodeCond.getLeftNodeCond(), parameters, attrUtil)).
                        append(" AND subject_id IN ( ").
                        append(getQuery(nodeCond.getRightNodeCond(), parameters, attrUtil).
                        append(")"));
                break;

            case OR:
                query.append("(").
                        append(getQuery(nodeCond.getLeftNodeCond(), parameters, attrUtil)).
                        append(" UNION ").
                        append(getQuery(nodeCond.getRightNodeCond(), parameters, attrUtil).
                        append(")"));
                break;

            default:
        }

        return query;
    }

    private String getQuery(final MembershipCond cond, final boolean not, final List<Object> parameters,
            final AttributableUtil attrUtil) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(attrUtil.searchView()).append(" WHERE ");

        if (not) {
            query.append("subject_id NOT IN (");
        } else {
            query.append("subject_id IN (");
        }

        query.append("SELECT DISTINCT subject_id ").append("FROM ").
                append(attrUtil.searchView()).append("_membership WHERE ");

        if (cond.getRoleId() != null) {
            query.append("role_id=?").append(setParameter(parameters, cond.getRoleId()));
        } else if (cond.getRoleName() != null) {
            query.append("role_name=?").append(setParameter(parameters, cond.getRoleName()));
        }

        query.append(')');

        return query.toString();
    }

    private String getQuery(final ResourceCond cond, final boolean not, final List<Object> parameters,
            final AttributableUtil attrUtil) {

        final StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(attrUtil.searchView()).append(" WHERE ");

        if (not) {
            query.append("subject_id NOT IN (");
        } else {
            query.append("subject_id IN (");
        }

        query.append("SELECT DISTINCT subject_id ").append("FROM ").
                append(attrUtil.searchView()).append("_resource WHERE ");

        query.append("resource_name=?").append(setParameter(parameters, cond.getResourceName()));

        query.append(')');

        return query.toString();
    }

    private String getQuery(final EntitlementCond cond, final boolean not, final List<Object> parameters) {
        final StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append("role_search_entitlements WHERE entitlement_name ");
        if (not) {
            query.append(" NOT ");
        }
        query.append(" LIKE ?").append(setParameter(parameters, cond.getExpression()));

        return query.toString();
    }

    private void fillAttributeQuery(final StringBuilder query, final AbstractAttrValue attrValue,
            final AbstractSchema schema, final AttributeCond cond, final boolean not, final List<Object> parameters) {

        String column = (cond instanceof AttributableCond)
                ? cond.getSchema()
                : "' AND " + getFieldName(schema.getType());

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

            case LIKE:
                if (schema.getType() == AttributeSchemaType.String || schema.getType() == AttributeSchemaType.Enum) {
                    query.append(column);
                    if (not) {
                        query.append(" NOT ");
                    }
                    query.append(" LIKE '").append(cond.getExpression()).append("'");
                } else {
                    if (!(cond instanceof AttributableCond)) {
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
                query.append("?").append(setParameter(parameters, attrValue.getValue()));
                break;

            case GE:
                query.append(column);
                if (not) {
                    query.append("<");
                } else {
                    query.append(">=");
                }
                query.append("?").append(setParameter(parameters, attrValue.getValue()));
                break;

            case GT:
                query.append(column);
                if (not) {
                    query.append("<=");
                } else {
                    query.append(">");
                }
                query.append("?").append(setParameter(parameters, attrValue.getValue()));
                break;

            case LE:
                query.append(column);
                if (not) {
                    query.append(">");
                } else {
                    query.append("<=");
                }
                query.append("?").append(setParameter(parameters, attrValue.getValue()));
                break;

            case LT:
                query.append(column);
                if (not) {
                    query.append(">=");
                } else {
                    query.append("<");
                }
                query.append("?").append(setParameter(parameters, attrValue.getValue()));
                break;

            default:
        }
    }

    private String getFieldName(final AttributeSchemaType type) {
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

    private String getQuery(final AttributeCond cond, final boolean not, final List<Object> parameters,
            final AttributableUtil attrUtil) {

        AbstractSchema schema = schemaDAO.find(cond.getSchema(), attrUtil.schemaClass());
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        AbstractAttrValue attrValue = attrUtil.newAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE && cond.getType() != AttributeCond.Type.ISNULL
                    && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                schema.getValidator().validate(cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '" + cond.getExpression() + "'", e);
            return EMPTY_ATTR_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(attrUtil.searchView()).append("_attr WHERE ").append("schema_name='").append(schema.getName());
        fillAttributeQuery(query, attrValue, schema, cond, not, parameters);

        return query.toString();
    }

    @SuppressWarnings("rawtypes")
    private String getQuery(final AttributableCond cond, final boolean not, final List<Object> parameters,
            final AttributableUtil attrUtil) {

        Field attributableField = ReflectionUtils.findField(attrUtil.attributableClass(), cond.getSchema());
        if (attributableField == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        AbstractSchema schema = attrUtil.newSchema();
        schema.setName(attributableField.getName());
        for (AttributeSchemaType type : AttributeSchemaType.values()) {
            if (attributableField.getType().isAssignableFrom(type.getType())) {
                schema.setType(type);
            }
        }

        // Deal with Attributable Integer fields logically mapping to boolean values
        // (SyncopeRole.inheritAttributes, for example)
        boolean foundBooleanMin = false;
        boolean foundBooleanMax = false;
        if (Integer.class.equals(attributableField.getType())) {
            for (Annotation annotation : attributableField.getAnnotations()) {
                if (Min.class.equals(annotation.annotationType())) {
                    foundBooleanMin = ((Min) annotation).value() == 0;
                } else if (Max.class.equals(annotation.annotationType())) {
                    foundBooleanMax = ((Max) annotation).value() == 1;
                }
            }
        }
        if (foundBooleanMin && foundBooleanMax) {
            if ("true".equalsIgnoreCase(cond.getExpression())) {
                cond.setExpression("1");
                schema.setType(AttributeSchemaType.Long);
            } else if ("false".equalsIgnoreCase(cond.getExpression())) {
                cond.setExpression("0");
                schema.setType(AttributeSchemaType.Long);
            }
        }

        // Deal with Attributable fields representing relationships to other entities
        // Only _id and _name are suppored
        if (attributableField.getType().getAnnotation(Entity.class) != null) {
            if (BeanUtils.findDeclaredMethodWithMinimalParameters(attributableField.getType(), "getId") != null) {
                cond.setSchema(cond.getSchema() + "_id");
                schema.setType(AttributeSchemaType.Long);
            }
            if (BeanUtils.findDeclaredMethodWithMinimalParameters(attributableField.getType(), "getName") != null) {
                cond.setSchema(cond.getSchema() + "_name");
                schema.setType(AttributeSchemaType.String);
            }
        }

        AbstractAttrValue attrValue = attrUtil.newAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE && cond.getType() != AttributeCond.Type.ISNULL
                    && cond.getType() != AttributeCond.Type.ISNOTNULL) {

                schema.getValidator().validate(cond.getExpression(), attrValue);
            }
        } catch (ValidationException e) {
            LOG.error("Could not validate expression '" + cond.getExpression() + "'", e);
            return EMPTY_ATTR_QUERY;
        }

        final StringBuilder query = new StringBuilder("SELECT DISTINCT subject_id FROM ").
                append(attrUtil.searchView()).append(" WHERE ");

        fillAttributeQuery(query, attrValue, schema, cond, not, parameters);

        return query.toString();
    }
}
