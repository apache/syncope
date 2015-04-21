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
package org.apache.syncope.core.persistence.jpa.dao;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.core.misc.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

@Repository
public class JPASubjectSearchDAO extends AbstractDAO<Subject<?, ?, ?>, Long> implements SubjectSearchDAO {

    private static final String EMPTY_ATTR_QUERY = "SELECT subject_id FROM user_search_attr WHERE 1=2";

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private PlainSchemaDAO schemaDAO;

    @Autowired
    private AttributableUtilsFactory attrUtilsFactory;

    private String getAdminRealmsFilter(final Set<String> adminRealms, final SearchSupport svs) {
        Set<Long> realmKeys = new HashSet<>();
        for (String realmPath : RealmUtils.normalize(adminRealms)) {
            Realm realm = realmDAO.find(realmPath);
            if (realm == null) {
                LOG.warn("Ignoring invalid realm {}", realmPath);
            } else {
                CollectionUtils.collect(realmDAO.findDescendants(realm), new Transformer<Realm, Long>() {

                    @Override
                    public Long transform(final Realm descendant) {
                        return descendant.getKey();
                    }
                }, realmKeys);
            }
        }

        StringBuilder adminRealmFilter = new StringBuilder().
                append("SELECT subject_id FROM ").append(svs.field().name).
                append(" WHERE realm_id IN (SELECT id AS realm_id FROM Realm");

        boolean firstRealm = true;
        for (Long realmKey : realmKeys) {
            if (firstRealm) {
                adminRealmFilter.append(" WHERE");
                firstRealm = false;
            } else {
                adminRealmFilter.append(" OR");
            }
            adminRealmFilter.append(" id = ").append(realmKey);
        }

        adminRealmFilter.append(')');

        return adminRealmFilter.toString();
    }

    @Override
    public int count(final Set<String> adminRealms, final SearchCond searchCondition, final SubjectType type) {
        List<Object> parameters = Collections.synchronizedList(new ArrayList<>());

        // 1. get the query string from the search condition
        SearchSupport svs = new SearchSupport(type);
        StringBuilder queryString = getQuery(searchCondition, parameters, type, svs);

        // 2. take into account administrative realms
        queryString.insert(0, "SELECT u.subject_id FROM (");
        queryString.append(") u WHERE subject_id IN (");
        queryString.append(getAdminRealmsFilter(adminRealms, svs)).append(')');

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
    public <T extends Subject<?, ?, ?>> List<T> search(
            final Set<String> adminRealms, final SearchCond searchCondition, final SubjectType type) {

        return search(adminRealms, searchCondition, Collections.<OrderByClause>emptyList(), type);
    }

    @Override
    public <T extends Subject<?, ?, ?>> List<T> search(
            final Set<String> adminRealms, final SearchCond searchCondition, final List<OrderByClause> orderBy,
            final SubjectType type) {

        return search(adminRealms, searchCondition, -1, -1, orderBy, type);
    }

    @Override
    public <T extends Subject<?, ?, ?>> List<T> search(
            final Set<String> adminRealms, final SearchCond searchCondition, final int page, final int itemsPerPage,
            final List<OrderByClause> orderBy, final SubjectType type) {

        List<T> result = Collections.<T>emptyList();

        if (adminRealms != null && !adminRealms.isEmpty()) {
            LOG.debug("Search condition:\n{}", searchCondition);

            if (searchCondition != null && searchCondition.isValid()) {
                try {
                    result = doSearch(adminRealms, searchCondition, page, itemsPerPage, orderBy, type);
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
    public <T extends Subject<?, ?, ?>> boolean matches(
            final T subject, final SearchCond searchCondition, final SubjectType type) {

        List<Object> parameters = Collections.synchronizedList(new ArrayList<>());

        // 1. get the query string from the search condition
        SearchSupport svs = new SearchSupport(type);
        StringBuilder queryString = getQuery(searchCondition, parameters, type, svs);

        boolean matches;
        if (queryString.length() == 0) {
            // Could be empty: got into a group search with a single membership condition ...
            matches = false;
        } else {
            // 2. take into account the passed user
            queryString.insert(0, "SELECT u.subject_id FROM (");
            queryString.append(") u WHERE subject_id=?").append(setParameter(parameters, subject.getKey()));

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

    private StringBuilder buildSelect(final OrderBySupport orderBySupport) {
        final StringBuilder select = new StringBuilder("SELECT u.subject_id");

        for (OrderBySupport.Item obs : orderBySupport.items) {
            select.append(',').append(obs.select);
        }
        select.append(" FROM ");

        return select;
    }

    private StringBuilder buildWhere(final OrderBySupport orderBySupport) {
        final StringBuilder where = new StringBuilder(" u");
        for (SearchSupport.SearchView searchView : orderBySupport.views) {
            where.append(',').append(searchView.name).append(' ').append(searchView.alias);
        }
        where.append(" WHERE ");
        for (SearchSupport.SearchView searchView : orderBySupport.views) {
            where.append("u.subject_id=").append(searchView.alias).append(".subject_id AND ");
        }

        for (OrderBySupport.Item obs : orderBySupport.items) {
            if (StringUtils.isNotBlank(obs.where)) {
                where.append(obs.where).append(" AND ");
            }
        }
        where.append("u.subject_id IN (");

        return where;
    }

    private StringBuilder buildOrderBy(final OrderBySupport orderBySupport) {
        final StringBuilder orderBy = new StringBuilder();

        for (OrderBySupport.Item obs : orderBySupport.items) {
            orderBy.append(obs.orderBy).append(',');
        }
        if (!orderBySupport.items.isEmpty()) {
            orderBy.insert(0, " ORDER BY ");
            orderBy.deleteCharAt(orderBy.length() - 1);
        }

        return orderBy;
    }

    private OrderBySupport parseOrderBy(final SubjectType type, final SearchSupport svs,
            final List<OrderByClause> orderByClauses) {

        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(type.asAttributableType());

        OrderBySupport orderBySupport = new OrderBySupport();

        for (OrderByClause clause : orderByClauses) {
            OrderBySupport.Item obs = new OrderBySupport.Item();

            // Manage difference among external key attribute and internal JPA @Id
            String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

            Field subjectField = ReflectionUtils.findField(attrUtils.attributableClass(), fieldName);
            if (subjectField == null) {
                PlainSchema schema = schemaDAO.find(fieldName, attrUtils.plainSchemaClass());
                if (schema != null) {
                    if (schema.isUniqueConstraint()) {
                        orderBySupport.views.add(svs.uniqueAttr());

                        obs.select = new StringBuilder().
                                append(svs.uniqueAttr().alias).append('.').append(svs.fieldName(schema.getType())).
                                append(" AS ").append(fieldName).toString();
                        obs.where = new StringBuilder().
                                append(svs.uniqueAttr().alias).
                                append(".schema_name='").append(fieldName).append("'").toString();
                        obs.orderBy = fieldName + " " + clause.getDirection().name();
                    } else {
                        orderBySupport.views.add(svs.attr());

                        obs.select = new StringBuilder().
                                append(svs.attr().alias).append('.').append(svs.fieldName(schema.getType())).
                                append(" AS ").append(fieldName).toString();
                        obs.where = new StringBuilder().
                                append(svs.attr().alias).
                                append(".schema_name='").append(fieldName).append("'").toString();
                        obs.orderBy = fieldName + " " + clause.getDirection().name();
                    }
                }
            } else {
                orderBySupport.views.add(svs.field());

                obs.select = svs.field().alias + "." + fieldName;
                obs.where = StringUtils.EMPTY;
                obs.orderBy = svs.field().alias + "." + fieldName + " " + clause.getDirection().name();
            }

            if (obs.isEmpty()) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                orderBySupport.items.add(obs);
            }
        }

        return orderBySupport;
    }

    @SuppressWarnings("unchecked")
    private <T extends Subject<?, ?, ?>> List<T> doSearch(final Set<String> adminRealms,
            final SearchCond nodeCond, final int page, final int itemsPerPage, final List<OrderByClause> orderBy,
            final SubjectType type) {

        List<Object> parameters = Collections.synchronizedList(new ArrayList<>());

        // 1. get the query string from the search condition
        SearchSupport svs = new SearchSupport(type);
        StringBuilder queryString = getQuery(nodeCond, parameters, type, svs);

        // 2. take into account administrative groups and ordering
        OrderBySupport orderBySupport = parseOrderBy(type, svs, orderBy);
        if (queryString.charAt(0) == '(') {
            queryString.insert(0, buildSelect(orderBySupport));
            queryString.append(buildWhere(orderBySupport));
        } else {
            queryString.insert(0, buildSelect(orderBySupport).append('('));
            queryString.append(')').append(buildWhere(orderBySupport));
        }
        queryString.
                append(getAdminRealmsFilter(adminRealms, svs)).append(')').
                append(buildOrderBy(orderBySupport));

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
        List<T> result = new ArrayList<>();

        for (Object subjectId : query.getResultList()) {
            long actualId;
            if (subjectId instanceof Object[]) {
                actualId = ((Number) ((Object[]) subjectId)[0]).longValue();
            } else {
                actualId = ((Number) subjectId).longValue();
            }

            T subject = type == SubjectType.USER
                    ? (T) userDAO.find(actualId)
                    : (T) groupDAO.find(actualId);
            if (subject == null) {
                LOG.error("Could not find {} with id {}, even though returned by the native query",
                        type, actualId);
            } else {
                if (!result.contains(subject)) {
                    result.add(subject);
                }
            }
        }

        return result;
    }

    private StringBuilder getQuery(final SearchCond nodeCond, final List<Object> parameters,
            final SubjectType type, final SearchSupport svs) {

        StringBuilder query = new StringBuilder();

        switch (nodeCond.getType()) {

            case LEAF:
            case NOT_LEAF:
                if (nodeCond.getMembershipCond() != null && SubjectType.USER == type) {
                    query.append(getQuery(nodeCond.getMembershipCond(),
                            nodeCond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                }
                if (nodeCond.getResourceCond() != null) {
                    query.append(getQuery(nodeCond.getResourceCond(),
                            nodeCond.getType() == SearchCond.Type.NOT_LEAF, parameters, type, svs));
                }
                if (nodeCond.getAttributeCond() != null) {
                    query.append(getQuery(nodeCond.getAttributeCond(),
                            nodeCond.getType() == SearchCond.Type.NOT_LEAF, parameters, type, svs));
                }
                if (nodeCond.getSubjectCond() != null) {
                    query.append(getQuery(nodeCond.getSubjectCond(),
                            nodeCond.getType() == SearchCond.Type.NOT_LEAF, parameters, type, svs));
                }
                break;

            case AND:
                query.append(getQuery(nodeCond.getLeftNodeCond(), parameters, type, svs)).
                        append(" AND subject_id IN ( ").
                        append(getQuery(nodeCond.getRightNodeCond(), parameters, type, svs)).
                        append(")");
                break;

            case OR:
                query.append(getQuery(nodeCond.getLeftNodeCond(), parameters, type, svs)).
                        append(" OR subject_id IN ( ").
                        append(getQuery(nodeCond.getRightNodeCond(), parameters, type, svs)).
                        append(")");
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
                append("group_id=?").append(setParameter(parameters, cond.getGroupId())).
                append(')');

        return query.toString();
    }

    private String getQuery(final ResourceCond cond, final boolean not, final List<Object> parameters,
            final SubjectType type, final SearchSupport svs) {

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

        if (type == SubjectType.USER) {
            query.append(" UNION SELECT DISTINCT subject_id FROM ").
                    append(svs.groupResource().name).
                    append(" WHERE resource_name=?").
                    append(setParameter(parameters, cond.getResourceName()));
        }

        query.append(')');

        return query.toString();
    }

    private void fillAttributeQuery(final StringBuilder query, final PlainAttrValue attrValue,
            final PlainSchema schema, final AttributeCond cond, final boolean not,
            final List<Object> parameters, final SearchSupport svs) {

        String column = (cond instanceof SubjectCond)
                ? cond.getSchema()
                : "' AND " + svs.fieldName(schema.getType());

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
                if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                    query.append(column);
                    if (not) {
                        query.append(" NOT ");
                    }
                    query.append(" LIKE ?").append(setParameter(parameters, cond.getExpression()));
                } else {
                    if (!(cond instanceof SubjectCond)) {
                        query.append("' AND");
                    }
                    query.append(" 1=2");
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
                break;

            case EQ:
                query.append(column);
                if (not) {
                    query.append("<>");
                } else {
                    query.append('=');
                }
                query.append('?').append(setParameter(parameters, attrValue.getValue()));
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

    private String getQuery(final AttributeCond cond, final boolean not, final List<Object> parameters,
            final SubjectType type, final SearchSupport svs) {

        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(type.asAttributableType());

        PlainSchema schema = schemaDAO.find(cond.getSchema(), attrUtils.plainSchemaClass());
        if (schema == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        PlainAttrValue attrValue = attrUtils.newPlainAttrValue();
        try {
            if (cond.getType() != AttributeCond.Type.LIKE && cond.getType() != AttributeCond.Type.ISNULL
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
                    append(" WHERE schema_name='").append(schema.getKey()).append("')");
        } else {
            if (cond.getType() == AttributeCond.Type.ISNULL) {
                query.append(svs.nullAttr().name).
                        append(" WHERE schema_name='").append(schema.getKey()).append("'");
            } else {
                if (schema.isUniqueConstraint()) {
                    query.append(svs.uniqueAttr().name);
                } else {
                    query.append(svs.attr().name);
                }
                query.append(" WHERE schema_name='").append(schema.getKey());

                fillAttributeQuery(query, attrValue, schema, cond, not, parameters, svs);
            }
        }

        return query.toString();
    }

    @SuppressWarnings("rawtypes")
    private String getQuery(final SubjectCond cond, final boolean not, final List<Object> parameters,
            final SubjectType type, final SearchSupport svs) {

        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(type.asAttributableType());

        // Keeps track of difference between entity's getKey() and JPA @Id fields
        if ("key".equals(cond.getSchema())) {
            cond.setSchema("id");
        }

        Field subjectField = ReflectionUtils.findField(attrUtils.attributableClass(), cond.getSchema());
        if (subjectField == null) {
            LOG.warn("Ignoring invalid schema '{}'", cond.getSchema());
            return EMPTY_ATTR_QUERY;
        }

        PlainSchema schema = attrUtils.newPlainSchema();
        schema.setKey(subjectField.getName());
        for (AttrSchemaType attrSchemaType : AttrSchemaType.values()) {
            if (subjectField.getType().isAssignableFrom(attrSchemaType.getType())) {
                schema.setType(attrSchemaType);
            }
        }

        // Deal with subject Integer fields logically mapping to boolean values
        // (JPAGroup.inheritPlainAttrs, for example)
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
            schema.setType(AttrSchemaType.Boolean);
        }

        // Deal with subject fields representing relationships to other entities
        if (subjectField.getType().getAnnotation(Entity.class) != null) {
            Method relMethod = null;
            try {
                relMethod = ClassUtils.getPublicMethod(subjectField.getType(), "getKey", new Class[0]);
            } catch (Exception e) {
                LOG.error("Could not find {}#getKey", subjectField.getType(), e);
            }

            if (relMethod != null) {
                if (Long.class.isAssignableFrom(relMethod.getReturnType())) {
                    cond.setSchema(cond.getSchema() + "_id");
                    schema.setType(AttrSchemaType.Long);
                }
                if (String.class.isAssignableFrom(relMethod.getReturnType())) {
                    cond.setSchema(cond.getSchema() + "_name");
                    schema.setType(AttrSchemaType.String);
                }
            }
        }

        PlainAttrValue attrValue = attrUtils.newPlainAttrValue();
        if (cond.getType() != AttributeCond.Type.LIKE
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
