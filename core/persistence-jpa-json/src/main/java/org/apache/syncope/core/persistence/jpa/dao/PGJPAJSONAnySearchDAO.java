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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.PrivilegeCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;

public class PGJPAJSONAnySearchDAO extends AbstractJPAJSONAnySearchDAO {

    protected static final String ALWAYS_FALSE_ASSERTION = "1=2";

    @Override
    protected void parseOrderByForPlainSchema(
            final SearchSupport svs,
            final OrderBySupport obs,
            final OrderBySupport.Item item,
            final OrderByClause clause,
            final PlainSchema schema,
            final String fieldName) {

        // keep track of involvement of non-mandatory schemas in the order by clauses
        obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        obs.views.add(svs.table());

        item.select = fieldName + " -> 0 AS " + fieldName;
        item.where = StringUtils.EMPTY;
        item.orderBy = fieldName + " " + clause.getDirection().name();
    }

    protected void fillAttrQuery(
            final AnyUtils anyUtils,
            final StringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        // This first branch is required for handling with not conditions given on multivalue fields (SYNCOPE-1419)
        if (not && !(cond instanceof AnyCond)) {
            query.append("NOT (");
            fillAttrQuery(anyUtils, query, attrValue, schema, cond, false, parameters, svs);
            query.append(")");
        } else if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(anyUtils, query, attrValue, schema, cond, true, parameters, svs);
        } else {
            String key = key(schema.getType());

            String value = cond.getExpression();
            if (schema.getType() == AttrSchemaType.Date) {
                try {
                    value = String.valueOf(FormatUtils.parseDate(value).getTime());
                } catch (ParseException e) {
                    LOG.error("Could not parse {} as date", value, e);
                }
            }

            boolean isStr = true;
            boolean lower;
            if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                lower = (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);
            } else {
                lower = false;
                try {
                    switch (schema.getType()) {
                        case Date:
                        case Long:
                            Long.parseLong(value);
                            break;
                        case Double:
                            Double.parseDouble(value);
                            break;
                        case Boolean:
                            if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
                                throw new IllegalArgumentException();
                            }
                            break;
                        default:
                    }

                    isStr = false;
                } catch (Exception nfe) {
                    // ignore}
                }
            }

            switch (cond.getType()) {

                case ISNULL:
                    // shouldn't occour: processed before
                    break;

                case ISNOTNULL:
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*]')");
                    break;

                case ILIKE:
                case LIKE:
                    // jsonb_path_exists(Nome, '$[*] ? (@.stringValue like_regex "EL.*" flag "i")')
                    if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                        query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                                append("(@.").append(key).append(" like_regex \"").
                                append(value.replaceAll("%", ".*")).
                                append("\"").
                                append(lower ? " flag \"i\"" : "").append(")')");
                    } else {
                        query.append(" 1=2");
                        LOG.error("LIKE is only compatible with string or enum schemas");
                    }
                    break;

                case IEQ:
                case EQ:
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key);
                    if (isStr) {
                        query.append(" like_regex \"").append(value.replace("'", "''")).append("\"");
                    } else {
                        query.append(" == ").append(value);
                    }

                    query.append(lower ? " flag \"i\"" : "").append(")')");
                    break;

                case GE:
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" >= ").
                            append(value).append(")')");
                    break;

                case GT:
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" > ").
                            append(value).append(")')");
                    break;

                case LE:
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" <= ").
                            append(value).append(")')");
                    break;

                case LT:
                    query.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" < ").
                            append(value).append(")')");
                    break;

                default:
            }
        }
    }

    @Override
    protected String getQuery(
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Pair<PlainSchema, PlainAttrValue> checked;
        try {
            checked = check(cond, svs.anyTypeKind);
        } catch (IllegalArgumentException e) {
            return ALWAYS_FALSE_ASSERTION;
        }

        // normalize NULL / NOT NULL checks
        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        StringBuilder query = new StringBuilder();

        switch (cond.getType()) {
            case ISNOTNULL:
                query.append(not ? " NOT " : " ").
                        append("jsonb_path_exists(").append(checked.getLeft().getKey()).append(",'$[*]')");
                break;

            case ISNULL:
                query.append(not ? " " : " NOT ").
                        append("jsonb_path_exists(").append(checked.getLeft().getKey()).append(",'$[*]')");
                break;

            default:
                fillAttrQuery(anyUtilsFactory.getInstance(svs.anyTypeKind),
                        query, checked.getRight(), checked.getLeft(), cond, not, parameters, svs);
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
            final RoleCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("(");

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.role().name).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynrolemembership().name).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append(")");

        query.append(")");

        return query.toString();
    }

    @Override
    protected String getQuery(
            final PrivilegeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("(");

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.priv().name).append(" WHERE ").
                append("privilege_id=?").append(setParameter(parameters, cond.getPrivilege())).
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynpriv().name).append(" WHERE ").
                append("privilege_id=?").append(setParameter(parameters, cond.getPrivilege())).
                append(")");

        query.append(")");

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
                append(svs.dynrealmmembership().name).append(" WHERE ").
                append("dynRealm_id=?").append(setParameter(parameters, cond.getDynRealm())).
                append(")");

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
                append(svs.resource().name).
                append(" WHERE resource_id=?").
                append(setParameter(parameters, cond.getResourceKey()));

        if (svs.anyTypeKind == AnyTypeKind.USER || svs.anyTypeKind == AnyTypeKind.ANY_OBJECT) {
            query.append(" UNION SELECT DISTINCT any_id FROM ").
                    append(svs.groupResource().name).
                    append(" WHERE resource_id=?").
                    append(setParameter(parameters, cond.getResourceKey()));
        }

        query.append(')');

        return query.toString();
    }

    @Override
    protected String getQuery(
            final AssignableCond cond,
            final List<Object> parameters,
            final SearchSupport svs) {

        Realm realm;
        try {
            realm = check(cond);
        } catch (IllegalArgumentException e) {
            return ALWAYS_FALSE_ASSERTION;
        }

        StringBuilder query = new StringBuilder("(");
        if (cond.isFromGroup()) {
            realmDAO.findDescendants(realm).forEach(current -> {
                query.append("realm_id=?").append(setParameter(parameters, current.getKey())).append(" OR ");
            });
            query.setLength(query.length() - 4);
        } else {
            for (Realm current = realm; current.getParent() != null; current = current.getParent()) {
                query.append("realm_id=?").append(setParameter(parameters, current.getKey())).append(" OR ");
            }
            query.append("realm_id=?").append(setParameter(parameters, realmDAO.getRoot().getKey()));
        }

        query.append(")");

        return query.toString();
    }

    @Override
    protected String getQuery(
            final MemberCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        String memberKey;
        try {
            memberKey = check(cond);
        } catch (IllegalArgumentException e) {
            return ALWAYS_FALSE_ASSERTION;
        }

        StringBuilder query = new StringBuilder("(");

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.USER).membership().name).append(" WHERE ").
                append("any_id=?").append(setParameter(parameters, memberKey)).
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.ANY_OBJECT).membership().name).append(" WHERE ").
                append("any_id=?").append(setParameter(parameters, memberKey)).
                append(")");

        query.append(")");

        return query.toString();
    }

    @Override
    protected String getQuery(
            final RelationshipTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("(");

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT any_id ").append("FROM ").
                append(svs.relationship().name).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(" UNION SELECT right_any_id AS any_id FROM ").
                append(svs.relationship().name).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(')');

        query.append(")");

        return query.toString();
    }

    @Override
    protected String getQuery(
            final RelationshipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        String rightAnyObjectKey;
        try {
            rightAnyObjectKey = check(cond);
        } catch (IllegalArgumentException e) {
            return ALWAYS_FALSE_ASSERTION;
        }

        StringBuilder query = new StringBuilder("(");

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.relationship().name).append(" WHERE ").
                append("right_any_id=?").append(setParameter(parameters, rightAnyObjectKey)).
                append(')');

        query.append(")");

        return query.toString();
    }

    @Override
    protected String getQuery(
            final MembershipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        List<String> groupKeys;
        try {
            groupKeys = check(cond);
        } catch (IllegalArgumentException e) {
            return ALWAYS_FALSE_ASSERTION;
        }

        String where = groupKeys.stream().
                map(key -> "group_id=?" + setParameter(parameters, key)).
                collect(Collectors.joining(" OR "));

        StringBuilder query = new StringBuilder("(");

        if (not) {
            query.append("id NOT IN (");
        } else {
            query.append("id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.membership().name).append(" WHERE ").
                append("(").append(where).append(")").
                append(") ");

        if (not) {
            query.append("AND id NOT IN (");
        } else {
            query.append("OR id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dyngroupmembership().name).append(" WHERE ").
                append("(").append(where).append(")").
                append(")");

        query.append(")");

        return query.toString();
    }

    @Override
    protected String getQuery(
            final AnyCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked;
        try {
            checked = check(cond, svs.anyTypeKind);
        } catch (IllegalArgumentException e) {
            return ALWAYS_FALSE_ASSERTION;
        }

        StringBuilder query = new StringBuilder();

        PlainSchema schema = schemaDAO.find(cond.getSchema());
        if (schema == null) {
            fillAttrQuery(query, checked.getMiddle(), checked.getLeft(), checked.getRight(), not, parameters, svs);
        } else {
            fillAttrQuery(anyUtilsFactory.getInstance(svs.anyTypeKind),
                    query, checked.getMiddle(), checked.getLeft(), checked.getRight(), not, parameters, svs);
        }

        return query.toString();
    }

    @Override
    protected String buildAdminRealmsFilter(
            final Set<String> realmKeys,
            final SearchSupport svs,
            final List<Object> parameters) {

        List<String> realmKeyArgs = realmKeys.stream().
                map(realmKey -> "?" + setParameter(parameters, realmKey)).
                collect(Collectors.toList());
        return "realm_id IN (" + StringUtils.join(realmKeyArgs, ", ") + ")";
    }

    @Override
    protected int doCount(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        List<Object> parameters = new ArrayList<>();

        SearchSupport svs = buildSearchSupport(kind);

        Pair<String, Set<String>> filter = getAdminRealmsFilter(adminRealms, svs, parameters);

        Pair<StringBuilder, Set<String>> queryInfo =
                getQuery(buildEffectiveCond(cond, filter.getRight()), parameters, svs);

        StringBuilder queryString =
                new StringBuilder("SELECT count(").append(svs.table().alias).append(".id").append(")");

        buildFrom(queryString, queryInfo, svs, null);

        buildWhere(queryString, queryInfo, filter);

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
            List<Object> parameters = new ArrayList<>();

            SearchSupport svs = buildSearchSupport(kind);

            Pair<String, Set<String>> filter = getAdminRealmsFilter(adminRealms, svs, parameters);

            SearchCond effectiveCond = buildEffectiveCond(cond, filter.getRight());

            // 1. get the query string from the search condition
            Pair<StringBuilder, Set<String>> queryInfo = getQuery(effectiveCond, parameters, svs);

            // 2. take into account realms and ordering
            OrderBySupport obs = parseOrderBy(svs, orderBy);

            StringBuilder queryString = new StringBuilder("SELECT ").append(svs.table().alias).append(".id");
            obs.items.forEach(item -> {
                queryString.append(",").append(item.select);
            });

            buildFrom(queryString, queryInfo, svs, obs);

            buildWhere(queryString, queryInfo, filter);

            LOG.debug("Query: {}, parameters: {}", queryString, parameters);

            queryString.append(buildOrderBy(obs));

            LOG.debug("Query with auth and order by statements: {}, parameters: {}", queryString, parameters);

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
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("While searching for {}", kind, e);
        }

        return List.of();
    }

    protected StringBuilder buildOrderBy(final OrderBySupport obs) {
        StringBuilder orderBy = new StringBuilder();

        obs.items.forEach(item -> {
            orderBy.append(item.orderBy).append(',');
        });
        if (!obs.items.isEmpty()) {
            orderBy.insert(0, " ORDER BY ");
            orderBy.deleteCharAt(orderBy.length() - 1);
        }

        return orderBy;
    }

    protected Pair<StringBuilder, Set<String>> getQuery(
            final SearchCond cond, final List<Object> parameters, final SearchSupport svs) {

        boolean not = cond.getType() == SearchCond.Type.NOT_LEAF;

        StringBuilder query = new StringBuilder();
        Set<String> involvedPlainAttrs = new HashSet<>();

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                cond.getLeaf(AnyTypeCond.class).
                        filter(leaf -> AnyTypeKind.ANY_OBJECT == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(RelationshipTypeCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(RelationshipCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(MembershipCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(MemberCond.class).
                        filter(leaf -> AnyTypeKind.GROUP == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(AssignableCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, parameters, svs)));

                cond.getLeaf(RoleCond.class).
                        filter(leaf -> AnyTypeKind.USER == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(PrivilegeCond.class).
                        filter(leaf -> AnyTypeKind.USER == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(DynRealmCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(ResourceCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                Optional<AnyCond> anyCond = cond.getLeaf(AnyCond.class);
                if (anyCond.isPresent()) {
                    query.append(getQuery(anyCond.get(), not, parameters, svs));
                } else {
                    cond.getLeaf(AttrCond.class).ifPresent(leaf -> {
                        query.append(getQuery(leaf, not, parameters, svs));
                        try {
                            involvedPlainAttrs.add(check(leaf, svs.anyTypeKind).getLeft().getKey());
                        } catch (IllegalArgumentException e) {
                            // ignore
                        }
                    });
                }

                // allow for additional search conditions
                getQueryForCustomConds(cond, parameters, svs, not, query);
                break;

            case AND:
                Pair<StringBuilder, Set<String>> leftAndInfo = getQuery(cond.getLeft(), parameters, svs);
                involvedPlainAttrs.addAll(leftAndInfo.getRight());

                Pair<StringBuilder, Set<String>> rigthAndInfo = getQuery(cond.getRight(), parameters, svs);
                involvedPlainAttrs.addAll(rigthAndInfo.getRight());

                query.append("(").
                        append(leftAndInfo.getKey()).
                        append(" AND ").
                        append(rigthAndInfo.getKey()).
                        append(")");
                break;

            case OR:
                Pair<StringBuilder, Set<String>> leftOrInfo = getQuery(cond.getLeft(), parameters, svs);
                involvedPlainAttrs.addAll(leftOrInfo.getRight());

                Pair<StringBuilder, Set<String>> rigthOrInfo = getQuery(cond.getRight(), parameters, svs);
                involvedPlainAttrs.addAll(rigthOrInfo.getRight());

                query.append("(").
                        append(leftOrInfo.getKey()).
                        append(" OR ").
                        append(rigthOrInfo.getKey()).
                        append(")");
                break;

            default:
        }

        return Pair.of(query, involvedPlainAttrs);
    }

    protected void fillAttrQuery(
            final StringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        // This first branch is required for handling with not conditions given on multivalue fields (SYNCOPE-1419)
        if (not && !(cond instanceof AnyCond)) {

            query.append("NOT (");
            fillAttrQuery(query, attrValue, schema, cond, false, parameters, svs);
            query.append(")");
        } else if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters, svs);
        } else {
            boolean lower = (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)
                    && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

            String column = cond.getSchema();
            if ((schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) && lower) {
                column = "LOWER (" + column + ")";
            }

            switch (cond.getType()) {

                case ISNULL:
                    // shouldn't occour: processed before
                    break;

                case ISNOTNULL:
                    query.append(column).append(" IS NOT NULL");
                    break;

                case ILIKE:
                case LIKE:
                    if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                        query.append(column);
                        query.append(" LIKE ");
                        if (lower) {
                            query.append("LOWER(?").append(setParameter(parameters, cond.getExpression())).append(')');
                        } else {
                            query.append('?').append(setParameter(parameters, cond.getExpression()));
                        }
                    } else {
                        query.append(" 1=2");
                        LOG.error("LIKE is only compatible with string or enum schemas");
                    }
                    break;

                case IEQ:
                case EQ:
                    query.append(column);
                    query.append('=');

                    if ((schema.getType() == AttrSchemaType.String
                            || schema.getType() == AttrSchemaType.Enum) && lower) {
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
    }

    protected void fillWithParameters(final Query query, final List<Object> parameters) {
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

    protected OrderBySupport parseOrderBy(
            final SearchSupport svs,
            final List<OrderByClause> orderBy) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(svs.anyTypeKind);

        OrderBySupport obs = new OrderBySupport();

        Set<String> orderByUniquePlainSchemas = new HashSet<>();
        Set<String> orderByNonUniquePlainSchemas = new HashSet<>();
        orderBy.forEach(clause -> {
            OrderBySupport.Item item = new OrderBySupport.Item();

            parseOrderByForCustom(svs, clause, item, obs);

            if (item.isEmpty()) {
                if (anyUtils.getField(clause.getField()) == null) {
                    PlainSchema schema = schemaDAO.find(clause.getField());
                    if (schema != null) {
                        if (schema.isUniqueConstraint()) {
                            orderByUniquePlainSchemas.add(schema.getKey());
                        } else {
                            orderByNonUniquePlainSchemas.add(schema.getKey());
                        }
                        if (orderByUniquePlainSchemas.size() > 1 || orderByNonUniquePlainSchemas.size() > 1) {
                            SyncopeClientException invalidSearch =
                                    SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
                            invalidSearch.getElements().add("Order by more than one attribute is not allowed; "
                                    + "remove one from " + (orderByUniquePlainSchemas.size() > 1
                                    ? orderByUniquePlainSchemas : orderByNonUniquePlainSchemas));
                            throw invalidSearch;
                        }
                        parseOrderByForPlainSchema(svs, obs, item, clause, schema, clause.getField());
                    }
                } else {
                    // Manage difference among external key attribute and internal JPA @Id
                    String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

                    // Adjust field name to column name
                    if (ArrayUtils.contains(RELATIONSHIP_FIELDS, fieldName)) {
                        fieldName += "_id";
                    }

                    obs.views.add(svs.field());

                    item.select = svs.table().alias + "." + fieldName;
                    item.where = StringUtils.EMPTY;
                    item.orderBy = svs.table().alias + "." + fieldName + " " + clause.getDirection().name();
                }
            }

            if (item.isEmpty()) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                obs.items.add(item);
            }
        });

        return obs;
    }

    protected Pair<String, Set<String>> getAdminRealmsFilter(
            final Set<String> adminRealms,
            final SearchSupport svs,
            final List<Object> parameters) {

        Set<String> realmKeys = new HashSet<>();
        Set<String> dynRealmKeys = new HashSet<>();
        RealmUtils.normalize(adminRealms).forEach(realmPath -> {
            if (realmPath.startsWith("/")) {
                Realm realm = realmDAO.findByFullPath(realmPath);
                if (realm == null) {
                    SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                    noRealm.getElements().add("Invalid realm specified: " + realmPath);
                    throw noRealm;
                } else {
                    realmKeys.addAll(realmDAO.findDescendants(realm).stream().
                            map(Entity::getKey).collect(Collectors.toSet()));
                }
            } else {
                DynRealm dynRealm = dynRealmDAO.find(realmPath);
                if (dynRealm == null) {
                    LOG.warn("Ignoring invalid dynamic realm {}", realmPath);
                } else {
                    dynRealmKeys.add(dynRealm.getKey());
                }
            }
        });
        if (!dynRealmKeys.isEmpty()) {
            realmKeys.addAll(realmDAO.findAll().stream().
                    map(Entity::getKey).collect(Collectors.toSet()));
        }

        return Pair.of(buildAdminRealmsFilter(realmKeys, svs, parameters), dynRealmKeys);
    }

    protected void buildFrom(
            final StringBuilder query,
            final Pair<StringBuilder, Set<String>> queryInfo,
            final SearchSupport svs,
            final OrderBySupport obs) {

        query.append(" FROM ").append(svs.table().name).append(" ").append(svs.table().alias);

        Set<String> schemas = queryInfo.getRight();

        if (obs != null) {
            Pattern pattern = Pattern.compile("(.*) -> 0 AS .*");
            obs.items.forEach(item -> {
                Matcher matcher = pattern.matcher(item.select);
                if (matcher.find()) {
                    schemas.add(matcher.group(1));
                }
            });
        }

        schemas.forEach(schema -> {
            // i.e jsonb_path_query(plainattrs, '$[*] ? (@.schema=="Nome")."values"') AS Nome
            PlainSchema pschema = schemaDAO.find(schema);
            if (pschema == null) {
                // just to be sure
                LOG.warn("Ignoring invalid schema '{}'", schema);
            } else {
                query.append(",").
                        append("jsonb_path_query_array(plainattrs, '$[*] ? (@.schema==\"").
                        append(schema).append("\").").
                        append("\"").append(pschema.isUniqueConstraint() ? "uniqueValue" : "values").append("\"')").
                        append(" AS ").append(schema);
            }
        });
    }

    protected void buildWhere(
            final StringBuilder query,
            final Pair<StringBuilder, Set<String>> queryInfo,
            final Pair<String, Set<String>> realms) {
        if (queryInfo.getLeft().length() > 0) {
            query.append(" WHERE ").append(queryInfo.getLeft());
        }

        if (realms.getLeft().length() > 0) {
            if (queryInfo.getLeft().length() > 0) {
                query.append(" AND ").append(realms.getLeft());
            } else {
                query.append(" WHERE ").append(realms.getLeft());
            }
        }
    }
}
