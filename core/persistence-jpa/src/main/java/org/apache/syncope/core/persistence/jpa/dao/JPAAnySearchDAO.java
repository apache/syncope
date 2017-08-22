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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.springframework.util.ReflectionUtils;

/**
 * Search engine implementation for users, groups and any objects, based on self-updating SQL views.
 */
public class JPAAnySearchDAO extends AbstractAnySearchDAO {

    private static final String EMPTY_QUERY = "SELECT any_id FROM user_search_attr WHERE 1=2";

    private Pair<String, Set<String>> getAdminRealmsFilter(
            final Set<String> adminRealms,
            final SearchSupport svs,
            final List<Object> parameters) {

        Set<String> realmKeys = new HashSet<>();
        Set<String> dynRealmKeys = new HashSet<>();
        for (String realmPath : RealmUtils.normalize(adminRealms)) {
            if (realmPath.startsWith("/")) {
                Realm realm = realmDAO.findByFullPath(realmPath);
                if (realm == null) {
                    SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                    noRealm.getElements().add("Invalid realm specified: " + realmPath);
                    throw noRealm;
                } else {
                    realmKeys.addAll(realmDAO.findDescendants(realm).stream().
                            map(r -> r.getKey()).collect(Collectors.toSet()));
                }
            } else {
                DynRealm dynRealm = dynRealmDAO.find(realmPath);
                if (dynRealm == null) {
                    LOG.warn("Ignoring invalid dynamic realm {}", realmPath);
                } else {
                    dynRealmKeys.add(dynRealm.getKey());
                }
            }
        }
        if (!dynRealmKeys.isEmpty()) {
            realmKeys.addAll(realmDAO.findAll().stream().
                    map(r -> r.getKey()).collect(Collectors.toSet()));
        }

        StringBuilder adminRealmFilter = new StringBuilder("u.any_id IN (").
                append("SELECT any_id FROM ").append(svs.field().name).
                append(" WHERE realm_id IN (SELECT id AS realm_id FROM Realm");

        boolean firstRealm = true;
        for (String realmKey : realmKeys) {
            if (firstRealm) {
                adminRealmFilter.append(" WHERE");
                firstRealm = false;
            } else {
                adminRealmFilter.append(" OR");
            }
            adminRealmFilter.append(" id=?").append(setParameter(parameters, realmKey));
        }

        adminRealmFilter.append("))");

        return Pair.of(adminRealmFilter.toString(), dynRealmKeys);
    }

    @Override
    protected int doCount(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        List<Object> parameters = Collections.synchronizedList(new ArrayList<>());

        SearchSupport svs = new SearchSupport(kind);

        Pair<String, Set<String>> filter = getAdminRealmsFilter(adminRealms, svs, parameters);

        // 1. get the query string from the search condition
        StringBuilder queryString = getQuery(buildEffectiveCond(cond, filter.getRight()), parameters, svs);

        // 2. take into account administrative realms
        queryString.insert(0, "SELECT u.any_id FROM (");
        queryString.append(") u WHERE ").append(filter.getLeft());

        // 3. prepare the COUNT query
        queryString.insert(0, "SELECT COUNT(any_id) FROM (");
        queryString.append(") count_any_id");

        Query countQuery = entityManager().createNativeQuery(queryString.toString());
        fillWithParameters(countQuery, parameters);

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Any<?>> List<T> doSearch(
            final Set<String> adminRealms,
            final SearchCond cond,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderBy,
            final AnyTypeKind kind) {

        try {
            List<Object> parameters = Collections.synchronizedList(new ArrayList<>());

            SearchSupport svs = new SearchSupport(kind);

            Pair<String, Set<String>> filter = getAdminRealmsFilter(adminRealms, svs, parameters);

            // 1. get the query string from the search condition
            StringBuilder queryString = getQuery(buildEffectiveCond(cond, filter.getRight()), parameters, svs);

            // 2. take into account realms and ordering
            OrderBySupport obs = parseOrderBy(kind, svs, orderBy);
            if (queryString.charAt(0) == '(') {
                queryString.insert(0, buildSelect(obs));
                queryString.append(buildWhere(svs, obs));
            } else {
                queryString.insert(0, buildSelect(obs).append('('));
                queryString.append(')').append(buildWhere(svs, obs));
            }
            queryString.
                    append(filter.getLeft()).
                    append(buildOrderBy(obs));

            // 3. prepare the search query
            Query query = entityManager().createNativeQuery(queryString.toString());

            // 4. page starts from 1, while setFirtResult() starts from 0
            query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

            if (itemsPerPage >= 0) {
                query.setMaxResults(itemsPerPage);
            }

            // 5. populate the search query with parameter values
            fillWithParameters(query, parameters);

            // 6. Prepare the result (avoiding duplicates)
            return buildResult(query.getResultList(), kind);
        } catch (Exception e) {
            LOG.error("While searching for {}", kind, e);
        }

        return Collections.emptyList();
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
        final StringBuilder select = new StringBuilder("SELECT u.any_id");

        for (OrderBySupport.Item item : obs.items) {
            select.append(',').append(item.select);
        }
        select.append(" FROM ");

        return select;
    }

    private StringBuilder buildWhere(final SearchSupport svs, final OrderBySupport obs) {
        StringBuilder where = new StringBuilder(" u");
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
            where.append("u.any_id=").append(searchView.alias).append(".any_id AND ");
        }

        for (OrderBySupport.Item item : obs.items) {
            if (StringUtils.isNotBlank(item.where)) {
                where.append(item.where).append(" AND ");
            }
        }

        return where;
    }

    private StringBuilder buildOrderBy(final OrderBySupport obs) {
        StringBuilder orderBy = new StringBuilder();

        for (OrderBySupport.Item item : obs.items) {
            orderBy.append(item.orderBy).append(',');
        }
        if (!obs.items.isEmpty()) {
            orderBy.insert(0, " ORDER BY ");
            orderBy.deleteCharAt(orderBy.length() - 1);
        }

        return orderBy;
    }

    private OrderBySupport parseOrderBy(final AnyTypeKind kind, final SearchSupport svs,
            final List<OrderByClause> orderByClauses) {

        AnyUtils attrUtils = anyUtilsFactory.getInstance(kind);

        OrderBySupport obs = new OrderBySupport();

        for (OrderByClause clause : orderByClauses) {
            OrderBySupport.Item item = new OrderBySupport.Item();

            // Manage difference among external key attribute and internal JPA @Id
            String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

            Field anyField = ReflectionUtils.findField(attrUtils.anyClass(), fieldName);
            if (anyField == null) {
                PlainSchema schema = schemaDAO.find(fieldName);
                if (schema != null) {
                    // keep track of involvement of non-mandatory schemas in the order by clauses
                    obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

                    if (schema.isUniqueConstraint()) {
                        obs.views.add(svs.uniqueAttr());

                        item.select = new StringBuilder().
                                append(svs.uniqueAttr().alias).append('.').append(svs.fieldName(schema.getType())).
                                append(" AS ").append(fieldName).toString();
                        item.where = new StringBuilder().
                                append(svs.uniqueAttr().alias).
                                append(".schema_id='").append(fieldName).append("'").toString();
                        item.orderBy = fieldName + " " + clause.getDirection().name();
                    } else {
                        obs.views.add(svs.attr());

                        item.select = new StringBuilder().
                                append(svs.attr().alias).append('.').append(svs.fieldName(schema.getType())).
                                append(" AS ").append(fieldName).toString();
                        item.where = new StringBuilder().
                                append(svs.attr().alias).
                                append(".schema_id='").append(fieldName).append("'").toString();
                        item.orderBy = fieldName + " " + clause.getDirection().name();
                    }
                }
            } else {
                obs.views.add(svs.field());

                item.select = svs.field().alias + "." + fieldName;
                item.where = StringUtils.EMPTY;
                item.orderBy = svs.field().alias + "." + fieldName + " " + clause.getDirection().name();
            }

            if (item.isEmpty()) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                obs.items.add(item);
            }
        }

        return obs;
    }

    private StringBuilder getQuery(final SearchCond cond, final List<Object> parameters, final SearchSupport svs) {
        StringBuilder query = new StringBuilder();

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                if (cond.getAnyTypeCond() != null && AnyTypeKind.ANY_OBJECT == svs.anyTypeKind) {
                    query.append(getQuery(cond.getAnyTypeCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getRelationshipTypeCond() != null
                        && (AnyTypeKind.USER == svs.anyTypeKind || AnyTypeKind.ANY_OBJECT == svs.anyTypeKind)) {

                    query.append(getQuery(cond.getRelationshipTypeCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getRelationshipCond() != null
                        && (AnyTypeKind.USER == svs.anyTypeKind || AnyTypeKind.ANY_OBJECT == svs.anyTypeKind)) {

                    query.append(getQuery(cond.getRelationshipCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getMembershipCond() != null
                        && (AnyTypeKind.USER == svs.anyTypeKind || AnyTypeKind.ANY_OBJECT == svs.anyTypeKind)) {

                    query.append(getQuery(cond.getMembershipCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getAssignableCond() != null) {
                    query.append(getQuery(cond.getAssignableCond(), parameters, svs));
                } else if (cond.getRoleCond() != null && AnyTypeKind.USER == svs.anyTypeKind) {
                    query.append(getQuery(cond.getRoleCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getDynRealmCond() != null) {
                    query.append(getQuery(cond.getDynRealmCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getMemberCond() != null && AnyTypeKind.GROUP == svs.anyTypeKind) {
                    query.append(getQuery(cond.getMemberCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getResourceCond() != null) {
                    query.append(getQuery(cond.getResourceCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getAttributeCond() != null) {
                    query.append(getQuery(cond.getAttributeCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                } else if (cond.getAnyCond() != null) {
                    query.append(getQuery(cond.getAnyCond(),
                            cond.getType() == SearchCond.Type.NOT_LEAF, parameters, svs));
                }
                break;

            case AND:
                String andSubQuery = getQuery(cond.getLeftSearchCond(), parameters, svs).toString();
                // Add extra parentheses
                andSubQuery = andSubQuery.replaceFirst("WHERE ", "WHERE (");
                query.append(andSubQuery).
                        append(" AND any_id IN ( ").
                        append(getQuery(cond.getRightSearchCond(), parameters, svs)).
                        append("))");
                break;

            case OR:
                String orSubQuery = getQuery(cond.getLeftSearchCond(), parameters, svs).toString();
                // Add extra parentheses
                orSubQuery = orSubQuery.replaceFirst("WHERE ", "WHERE (");
                query.append(orSubQuery).
                        append(" OR any_id IN ( ").
                        append(getQuery(cond.getRightSearchCond(), parameters, svs)).
                        append("))");
                break;

            default:
        }

        return query;
    }

    private String getQuery(
            final AnyTypeCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE type_id");

        if (not) {
            query.append("<>");
        } else {
            query.append('=');
        }

        query.append('?').append(setParameter(parameters, cond.getAnyTypeKey()));

        return query.toString();
    }

    private String getQuery(
            final RelationshipTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT any_id ").append("FROM ").
                append(svs.relationship().name).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(" UNION SELECT right_any_id AS any_id FROM ").
                append(svs.relationship().name).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(')');

        return query.toString();
    }

    private String getQuery(
            final RelationshipCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        String rightAnyObjectKey;
        try {
            rightAnyObjectKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.relationship().name).append(" WHERE ").
                append("right_any_id=?").append(setParameter(parameters, rightAnyObjectKey)).
                append(')');

        return query.toString();
    }

    private String getQuery(
            final MembershipCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        String groupKey;
        try {
            groupKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.membership().name).append(" WHERE ").
                append("group_id=?").append(setParameter(parameters, groupKey)).
                append(") ");

        if (not) {
            query.append("AND any_id NOT IN (");
        } else {
            query.append("OR any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dyngroupmembership().name).append(" WHERE ").
                append("group_id=?").append(setParameter(parameters, groupKey)).
                append("))");

        return query.toString();
    }

    private String getQuery(
            final RoleCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.role().name).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append(") ");

        if (not) {
            query.append("AND any_id NOT IN (");
        } else {
            query.append("OR any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynrolemembership().name).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append("))");

        return query.toString();
    }

    private String getQuery(
            final DynRealmCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynrealmmembership().name).append(" WHERE ").
                append("dynRealm_id=?").append(setParameter(parameters, cond.getDynRealm())).
                append("))");

        return query.toString();
    }

    private String getQuery(
            final ResourceCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.resource().name).
                append(" WHERE resource_id=?").
                append(setParameter(parameters, cond.getResourceKey()));

        if (svs.anyTypeKind == AnyTypeKind.USER) {
            query.append(" UNION SELECT DISTINCT any_id FROM ").
                    append(svs.groupResource().name).
                    append(" WHERE resource_id=?").
                    append(setParameter(parameters, cond.getResourceKey()));
        }

        query.append(')');

        return query.toString();
    }

    private String getQuery(final AssignableCond cond, final List<Object> parameters, final SearchSupport svs) {
        Realm realm;
        try {
            realm = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");
        if (cond.isFromGroup()) {
            for (Realm current = realm; current.getParent() != null; current = current.getParent()) {
                query.append("realm_id=?").append(setParameter(parameters, current.getKey())).append(" OR ");
            }
            query.append("realm_id=?").append(setParameter(parameters, realmDAO.getRoot().getKey()));
        } else {
            for (Realm current : realmDAO.findDescendants(realm)) {
                query.append("realm_id=?").append(setParameter(parameters, current.getKey())).append(" OR ");
            }
            query.setLength(query.length() - 4);
        }
        query.append(')');

        return query.toString();
    }

    private String getQuery(
            final MemberCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        String memberKey;
        try {
            memberKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.USER).membership().name).append(" WHERE (").
                append("any_id=?").append(setParameter(parameters, memberKey)).
                append(") ");

        if (not) {
            query.append("AND any_id NOT IN (");
        } else {
            query.append("OR any_id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.ANY_OBJECT).membership().name).append(" WHERE ").
                append("any_id=?").append(setParameter(parameters, memberKey)).
                append("))");

        return query.toString();
    }

    private void fillAttrQuery(
            final StringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttributeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        // activate ignoreCase only for EQ and LIKE operators
        boolean ignoreCase = AttributeCond.Type.ILIKE == cond.getType() || AttributeCond.Type.IEQ == cond.getType();

        String column = (cond instanceof AnyCond) ? cond.getSchema() : svs.fieldName(schema.getType());
        if (ignoreCase) {
            column = "LOWER (" + column + ")";
        }
        if (!(cond instanceof AnyCond)) {
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
                if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
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
                    if (!(cond instanceof AnyCond)) {
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

        Pair<PlainSchema, PlainAttrValue> checked;
        try {
            checked = check(cond, svs.anyTypeKind);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ");
        switch (cond.getType()) {
            case ISNOTNULL:
                query.append(svs.field().name).
                        append(" WHERE any_id NOT IN (SELECT any_id FROM ").
                        append(svs.nullAttr().name).
                        append(" WHERE schema_id='").append(checked.getLeft().getKey()).append("')");
                break;

            case ISNULL:
                query.append(svs.nullAttr().name).
                        append(" WHERE schema_id='").append(checked.getLeft().getKey()).append("'");
                break;

            default:
                if (checked.getLeft().isUniqueConstraint()) {
                    query.append(svs.uniqueAttr().name);
                } else {
                    query.append(svs.attr().name);
                }
                query.append(" WHERE schema_id='").append(checked.getLeft().getKey());
                fillAttrQuery(query, checked.getRight(), checked.getLeft(), cond, not, parameters, svs);
        }

        return query.toString();
    }

    private String getQuery(
            final AnyCond cond, final boolean not, final List<Object> parameters, final SearchSupport svs) {

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked;
        try {
            checked = check(cond, svs.anyTypeKind);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        fillAttrQuery(query, checked.getMiddle(), checked.getLeft(), checked.getRight(), not, parameters, svs);

        return query.toString();
    }
}
