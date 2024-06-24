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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.AuxClassCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.PrivilegeCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PGJPAJSONAnySearchDAO extends JPAAnySearchDAO {

    protected static final String ALWAYS_FALSE_ASSERTION = "1=2";

    protected static final String REGEX_CHARS = "!$()*+.:<=>?[\\]^{|}-";

    protected static String escapeForLikeRegex(final String input) {
        String output = input;
        for (char toEscape : REGEX_CHARS.toCharArray()) {
            output = output.replace(String.valueOf(toEscape), "\\" + toEscape);
        }
        return output.replace("'", "''");
    }

    protected static String escapeIfString(final String value, final boolean isStr) {
        return isStr
                ? new StringBuilder().append('"').append(value.replace("'", "''")).append('"').toString()
                : value;
    }

    public PGJPAJSONAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final EntityManagerFactory entityManagerFactory,
            final EntityManager entityManager) {

        super(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator,
                entityManagerFactory,
                entityManager);
    }

    @Override
    protected void parseOrderByForPlainSchema(
            final SearchSupport svs,
            final OrderBySupport obs,
            final OrderBySupport.Item item,
            final Sort.Order clause,
            final PlainSchema schema,
            final String fieldName) {

        // keep track of involvement of non-mandatory schemas in the order by clauses
        obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        obs.views.add(svs.table());

        item.select = fieldName + " -> 0 AS " + fieldName;
        item.where = StringUtils.EMPTY;
        item.orderBy = fieldName + ' ' + clause.getDirection().name();
    }

    @Override
    protected void parseOrderByForField(
            final SearchSupport svs,
            final OrderBySupport.Item item,
            final String fieldName,
            final Sort.Order clause) {

        item.select = svs.table().alias() + '.' + fieldName;
        item.where = StringUtils.EMPTY;
        item.orderBy = svs.table().alias() + '.' + fieldName + ' ' + clause.getDirection().name();
    }

    protected void fillAttrQuery(
            final AnyUtils anyUtils,
            final StringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final SearchSupport svs) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(anyUtils, query, attrValue, schema, cond, true, svs);
        } else if (not) {
            query.append("NOT (");
            fillAttrQuery(anyUtils, query, attrValue, schema, cond, false, svs);
            query.append(')');
        } else {
            String key = key(schema.getType());

            String value = Optional.ofNullable(attrValue.getDateValue()).
                    map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                    orElse(cond.getExpression());

            boolean isStr = true;
            boolean lower = false;
            if (schema.getType().isStringClass()) {
                lower = (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);
            } else if (schema.getType() != AttrSchemaType.Date) {
                lower = false;
                try {
                    switch (schema.getType()) {
                        case Long ->
                            Long.valueOf(value);

                        case Double ->
                            Double.valueOf(value);

                        case Boolean -> {
                            if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
                                throw new IllegalArgumentException();
                            }
                        }

                        default -> {
                        }
                    }

                    isStr = false;
                } catch (Exception nfe) {
                    // ignore
                }
            }

            switch (cond.getType()) {
                case ISNULL -> {
                }

                case ISNOTNULL ->
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*]')");

                case ILIKE, LIKE -> {
                    // jsonb_path_exists(Nome, '$[*] ? (@.stringValue like_regex "EL.*" flag "i")')
                    if (schema.getType().isStringClass()) {
                        query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                                append("(@.").append(key).append(" like_regex \"").
                                append(escapeForLikeRegex(value).replace("%", ".*")).
                                append("\"").
                                append(lower ? " flag \"i\"" : "").append(")')");
                    } else {
                        query.append(' ').append(ALWAYS_FALSE_ASSERTION);
                        LOG.error("LIKE is only compatible with string or enum schemas");
                    }
                }

                case IEQ, EQ -> {
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key);

                    if (StringUtils.containsAny(value, REGEX_CHARS) || lower) {
                        query.append(" like_regex \"^").
                                append(escapeForLikeRegex(value).replace("'", "''")).
                                append("$\"");
                    } else {
                        query.append(" == ").append(escapeIfString(value, isStr));
                    }

                    query.append(lower ? " flag \"i\"" : "").append(")')");
                }

                case GE ->
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" >= ").
                            append(escapeIfString(value, isStr)).append(")')");

                case GT ->
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" > ").
                            append(escapeIfString(value, isStr)).append(")')");

                case LE ->
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" <= ").
                            append(escapeIfString(value, isStr)).append(")')");

                case LT ->
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" < ").
                            append(escapeIfString(value, isStr)).append(")')");

                default -> {
                }
            }
            // shouldn't occour: processed before
        }
    }

    @Override
    protected String getQuery(
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Pair<PlainSchema, PlainAttrValue> checked = check(cond, svs.anyTypeKind);

        StringBuilder query = new StringBuilder();

        switch (cond.getType()) {
            case ISNOTNULL ->
                query.append(not ? " NOT " : ' ').
                        append("jsonb_path_exists(").append(checked.getLeft().getKey()).append(",'$[*]')");

            case ISNULL ->
                query.append(not ? ' ' : " NOT ").
                        append("jsonb_path_exists(").append(checked.getLeft().getKey()).append(",'$[*]')");

            default ->
                fillAttrQuery(
                        anyUtilsFactory.getInstance(svs.anyTypeKind),
                        query, checked.getRight(), checked.getLeft(), cond, not, svs);
        }

        return query.toString();
    }

    @Override
    protected String getQuery(
            final AnyTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("type_id");

        if (not) {
            query.append("<>");
        } else {
            query.append('=');
        }

        query.append('?').append(setParameter(parameters, cond.getAnyTypeKey()));

        return query.toString();
    }

    @Override
    protected String getQuery(
            final AuxClassCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder();

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.auxClass().name()).
                append(" WHERE anyTypeClass_id=?").
                append(setParameter(parameters, cond.getAuxClass())).
                append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final RoleCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder().append('(');

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.role().name()).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(SearchSupport.dynrolemembership().name()).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append(')');

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final PrivilegeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder().append('(');

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.priv().name()).append(" WHERE ").
                append("privilege_id=?").append(setParameter(parameters, cond.getPrivilege())).
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynpriv().name()).append(" WHERE ").
                append("privilege_id=?").append(setParameter(parameters, cond.getPrivilege())).
                append(')');

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final DynRealmCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder();

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(SearchSupport.dynrealmmembership().name()).append(" WHERE ").
                append("dynRealm_id=?").append(setParameter(parameters, cond.getDynRealm())).
                append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final ResourceCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder();

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.resource().name()).
                append(" WHERE resource_id=?").
                append(setParameter(parameters, cond.getResource()));

        if (svs.anyTypeKind == AnyTypeKind.USER || svs.anyTypeKind == AnyTypeKind.ANY_OBJECT) {
            query.append(" UNION SELECT DISTINCT any_id FROM ").
                    append(svs.groupResource().name()).
                    append(" WHERE resource_id=?").
                    append(setParameter(parameters, cond.getResource()));
        }

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final MemberCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Set<String> members = check(cond);

        StringBuilder query = new StringBuilder().append('(');

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.USER).membership().name()).append(" WHERE ").
                append(members.stream().
                        map(key -> "any_id=?" + setParameter(parameters, key)).
                        collect(Collectors.joining(" OR "))).
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.ANY_OBJECT).membership().name()).append(" WHERE ").
                append(members.stream().
                        map(key -> "any_id=?" + setParameter(parameters, key)).
                        collect(Collectors.joining(" OR "))).
                append(')');

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final RelationshipTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder().append('(');

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT any_id ").append("FROM ").
                append(svs.relationship().name()).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(" UNION SELECT right_any_id AS any_id FROM ").
                append(svs.relationship().name()).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(')');

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final RelationshipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Set<String> rightAnyObjectKeys = check(cond);

        StringBuilder query = new StringBuilder().append('(');

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.relationship().name()).append(" WHERE ").
                append(rightAnyObjectKeys.stream().
                        map(key -> "right_any_id=?" + setParameter(parameters, key)).
                        collect(Collectors.joining(" OR "))).
                append(')');

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final MembershipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        List<String> groupKeys = check(cond);

        String where = groupKeys.stream().
                map(key -> "group_id=?" + setParameter(parameters, key)).
                collect(Collectors.joining(" OR "));

        StringBuilder query = new StringBuilder().append('(');

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.membership().name()).append(" WHERE ").
                append('(').append(where).append(')').
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dyngroupmembership().name()).append(" WHERE ").
                append('(').append(where).append(')').
                append(')');

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final AnyCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        if (JAXRSService.PARAM_REALM.equals(cond.getSchema())
                && !SyncopeConstants.UUID_PATTERN.matcher(cond.getExpression()).matches()) {

            Realm realm = realmSearchDAO.findByFullPath(cond.getExpression()).
                    orElseThrow(() -> new IllegalArgumentException("Invalid Realm full path: " + cond.getExpression()));
            cond.setExpression(realm.getKey());
        }

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked = check(cond, svs.anyTypeKind);

        StringBuilder query = new StringBuilder();

        plainSchemaDAO.findById(cond.getSchema()).ifPresentOrElse(
                schema -> fillAttrQuery(
                        anyUtilsFactory.getInstance(svs.anyTypeKind),
                        query, checked.getMiddle(), checked.getLeft(), checked.getRight(), not, svs),
                () -> fillAttrQuery(
                        query, checked.getMiddle(), checked.getLeft(), checked.getRight(), not, parameters, svs));

        return query.toString();
    }

    @Override
    protected String buildAdminRealmsFilter(
            final Set<String> realmKeys,
            final SearchSupport svs,
            final List<Object> parameters) {

        if (realmKeys.isEmpty()) {
            return "realm_id IS NOT NULL";
        }

        String realmKeysArg = realmKeys.stream().
                map(realmKey -> "?" + setParameter(parameters, realmKey)).
                collect(Collectors.joining(","));
        return "realm_id IN (" + realmKeysArg + ')';
    }

    @Override
    protected long doCount(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        List<Object> parameters = new ArrayList<>();

        SearchSupport svs = buildSearchSupport(kind);

        Triple<String, Set<String>, Set<String>> filter =
                getAdminRealmsFilter(base, recursive, adminRealms, svs, parameters);

        Pair<StringBuilder, Set<String>> queryInfo =
                getQuery(buildEffectiveCond(cond, filter.getMiddle(), filter.getRight(), kind), parameters, svs);

        StringBuilder queryString =
                new StringBuilder("SELECT count(").append(svs.table().alias()).append(".id").append(')');

        buildFromAndWhere(queryString, queryInfo, filter.getLeft(), svs, null);

        Query countQuery = entityManager.createNativeQuery(queryString.toString());
        fillWithParameters(countQuery, parameters);

        return ((Number) countQuery.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Any<?>> List<T> doSearch(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind) {

        try {
            List<Object> parameters = new ArrayList<>();

            SearchSupport svs = buildSearchSupport(kind);

            Triple<String, Set<String>, Set<String>> filter =
                    getAdminRealmsFilter(base, recursive, adminRealms, svs, parameters);

            SearchCond effectiveCond = buildEffectiveCond(cond, filter.getMiddle(), filter.getRight(), kind);

            // 1. get the query string from the search condition
            Pair<StringBuilder, Set<String>> queryInfo = getQuery(effectiveCond, parameters, svs);

            // 2. take into account realms and ordering
            OrderBySupport obs = parseOrderBy(svs, pageable.getSort().get());

            StringBuilder queryString = new StringBuilder("SELECT ").append(svs.table().alias()).append(".id");
            obs.items.forEach(item -> queryString.append(',').append(item.select));

            buildFromAndWhere(queryString, queryInfo, filter.getLeft(), svs, obs);

            LOG.debug("Query: {}, parameters: {}", queryString, parameters);

            queryString.append(buildOrderBy(obs));

            LOG.debug("Query with auth and order by statements: {}, parameters: {}", queryString, parameters);

            // 3. prepare the search query
            Query query = entityManager.createNativeQuery(queryString.toString());

            if (pageable.isPaged()) {
                query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
                query.setMaxResults(pageable.getPageSize());
            }

            // 5. populate the search query with parameter values
            fillWithParameters(query, parameters);

            // 6. Prepare the result (avoiding duplicates)
            return buildResult(query.getResultList(), kind);
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("While searching for {}", kind, e);
        }

        return List.of();
    }

    @Override
    protected void queryOp(
            final StringBuilder query,
            final String op,
            final Pair<StringBuilder, Set<String>> leftInfo,
            final Pair<StringBuilder, Set<String>> rightInfo) {

        query.append('(').
                append(leftInfo.getKey()).
                append(' ').append(op).append(' ').
                append(rightInfo.getKey()).
                append(')');
    }

    @Override
    protected void fillAttrQuery(
            final StringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters, svs);
        } else if (not) {
            query.append("NOT (");
            fillAttrQuery(query, attrValue, schema, cond, false, parameters, svs);
            query.append(')');
        } else if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters, svs);
        } else {
            boolean lower = schema.getType().isStringClass()
                    && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

            String column = cond.getSchema();
            if (lower) {
                column = "LOWER (" + column + ')';
            }

            switch (cond.getType()) {

                case ISNULL ->
                    query.append(column).append(" IS NULL");

                case ISNOTNULL ->
                    query.append(column).append(" IS NOT NULL");

                case ILIKE, LIKE -> {
                    if (schema.getType().isStringClass()) {
                        query.append(column).append(" LIKE ");
                        if (lower) {
                            query.append("LOWER(?").append(setParameter(parameters, cond.getExpression())).append(')');
                        } else {
                            query.append('?').append(setParameter(parameters, cond.getExpression()));
                        }
                    } else {
                        query.append(' ').append(ALWAYS_FALSE_ASSERTION);
                        LOG.error("LIKE is only compatible with string or enum schemas");
                    }
                }

                case IEQ, EQ -> {
                    query.append(column).append('=');

                    if (lower) {
                        query.append("LOWER(?").append(setParameter(parameters, attrValue.getValue())).append(')');
                    } else {
                        query.append('?').append(setParameter(parameters, attrValue.getValue()));
                    }
                }

                case GE -> {
                    query.append(column);
                    if (not) {
                        query.append('<');
                    } else {
                        query.append(">=");
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                }

                case GT -> {
                    query.append(column);
                    if (not) {
                        query.append("<=");
                    } else {
                        query.append('>');
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                }

                case LE -> {
                    query.append(column);
                    if (not) {
                        query.append('>');
                    } else {
                        query.append("<=");
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                }

                case LT -> {
                    query.append(column);
                    if (not) {
                        query.append(">=");
                    } else {
                        query.append('<');
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                }

                default -> {
                }
            }
        }
    }

    protected void buildFromAndWhere(
            final StringBuilder queryString,
            final Pair<StringBuilder, Set<String>> queryInfo,
            final String realmsFilter,
            final SearchSupport svs,
            final OrderBySupport obs) {

        queryString.append(" FROM ").append(svs.table().name()).append(' ').append(svs.table().alias());

        Set<String> schemas = queryInfo.getRight();

        if (obs != null) {
            obs.views.stream().
                    filter(view -> !svs.field().name().equals(view.name()) && !svs.table().name().equals(view.name())).
                    map(view -> view.name() + ' ' + view.alias()).
                    forEach(view -> queryString.append(',').append(view));

            obs.items.forEach(item -> {
                String schema = StringUtils.substringBefore(item.orderBy, ' ');
                if (StringUtils.isNotBlank(schema)) {
                    schemas.add(schema);
                }
            });
        }

        // i.e jsonb_path_query(plainattrs, '$[*] ? (@.schema=="Nome")."values"') AS Nome
        schemas.forEach(schema -> plainSchemaDAO.findById(schema).ifPresentOrElse(
                pschema -> queryString.append(',').
                        append("jsonb_path_query_array(plainattrs, '$[*] ? (@.schema==\"").
                        append(schema).append("\").").
                        append("\"").append(pschema.isUniqueConstraint() ? "uniqueValue" : "values").append("\"')").
                        append(" AS ").append(schema),
                () -> LOG.warn("Ignoring invalid schema '{}'", schema)));

        StringBuilder where = new StringBuilder();

        if (queryInfo.getLeft().length() > 0) {
            where.append(" WHERE ").append(queryInfo.getLeft());
        }

        if (queryInfo.getLeft().length() == 0) {
            where.append(" WHERE ");
        } else {
            where.append(" AND ");
        }
        where.append(realmsFilter);

        if (obs != null) {
            String obsWhere = obs.views.stream().
                    filter(view -> !svs.field().name().equals(view.name()) && !svs.table().name().equals(view.name())).
                    map(view -> "t.id=" + view.alias() + ".any_id").
                    collect(Collectors.joining(" AND "));
            if (!obsWhere.isEmpty()) {
                if (where.length() == 0) {
                    where.append(" WHERE ");
                } else {
                    where.append(" AND ");
                }
                where.append(obsWhere);
            }
        }

        queryString.append(where);
    }
}
