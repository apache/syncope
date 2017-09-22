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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.MatchNoneQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

/**
 * Search engine implementation for users, groups and any objects, based on Elasticsearch.
 */
public class ElasticsearchAnySearchDAO extends AbstractAnySearchDAO {

    private static final QueryBuilder EMPTY_QUERY_BUILDER = new MatchNoneQueryBuilder();

    @Autowired
    private Client client;

    @Autowired
    private ElasticsearchUtils elasticsearchUtils;

    private Pair<DisMaxQueryBuilder, Set<String>> adminRealmsFilter(final Set<String> adminRealms) {
        DisMaxQueryBuilder builder = QueryBuilders.disMaxQuery();

        Set<String> dynRealmKeys = new HashSet<>();
        RealmUtils.normalize(adminRealms).forEach(realmPath -> {
            if (realmPath.startsWith("/")) {
                Realm realm = realmDAO.findByFullPath(realmPath);
                if (realm == null) {
                    SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                    noRealm.getElements().add("Invalid realm specified: " + realmPath);
                    throw noRealm;
                } else {
                    realmDAO.findDescendants(realm).forEach(descendant -> {
                        builder.add(QueryBuilders.termQuery("realm", descendant.getFullPath()));
                    });
                }
            } else {
                DynRealm dynRealm = dynRealmDAO.find(realmPath);
                if (dynRealm == null) {
                    LOG.warn("Ignoring invalid dynamic realm {}", realmPath);
                } else {
                    dynRealmKeys.add(dynRealm.getKey());
                    builder.add(QueryBuilders.termQuery("dynRealm", dynRealm.getKey()));
                }
            }
        });
        if (!dynRealmKeys.isEmpty()) {
            realmDAO.findAll().forEach(descendant -> {
                builder.add(QueryBuilders.termQuery("realm", descendant.getFullPath()));
            });
        }

        return Pair.of(builder, dynRealmKeys);
    }

    private SearchRequestBuilder searchRequestBuilder(
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        Pair<DisMaxQueryBuilder, Set<String>> filter = adminRealmsFilter(adminRealms);

        return client.prepareSearch(AuthContextUtils.getDomain().toLowerCase()).
                setTypes(kind.name()).
                setSearchType(SearchType.QUERY_THEN_FETCH).
                setQuery(SyncopeConstants.FULL_ADMIN_REALMS.equals(adminRealms)
                        ? getQueryBuilder(cond, kind)
                        : QueryBuilders.boolQuery().
                                must(filter.getLeft()).
                                must(getQueryBuilder(buildEffectiveCond(cond, filter.getRight()), kind)));
    }

    @Override
    protected int doCount(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        SearchRequestBuilder builder = searchRequestBuilder(adminRealms, cond, kind).
                setFrom(0).setSize(0);

        return (int) builder.get().getHits().getTotalHits();
    }

    private void addSort(
            final SearchRequestBuilder builder,
            final AnyTypeKind kind,
            final List<OrderByClause> orderBy) {

        AnyUtils attrUtils = anyUtilsFactory.getInstance(kind);

        orderBy.forEach(clause -> {
            String sortName = null;

            // Manage difference among external key attribute and internal JPA @Id
            String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

            Field anyField = ReflectionUtils.findField(attrUtils.anyClass(), fieldName);
            if (anyField == null) {
                PlainSchema schema = schemaDAO.find(fieldName);
                if (schema != null) {
                    sortName = fieldName;
                }
            } else {
                sortName = fieldName;
            }

            if (sortName == null) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                builder.addSort(sortName, SortOrder.valueOf(clause.getDirection().name()));
            }
        });
    }

    @Override
    protected <T extends Any<?>> List<T> doSearch(
            final Set<String> adminRealms,
            final SearchCond cond,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderBy,
            final AnyTypeKind kind) {

        SearchRequestBuilder builder = searchRequestBuilder(adminRealms, cond, kind).
                setFrom(page <= 0 ? 0 : page - 1).
                setSize(itemsPerPage < 0 ? elasticsearchUtils.getIndexMaxResultWindow() : itemsPerPage);
        addSort(builder, kind, orderBy);

        return buildResult(Stream.of(builder.get().getHits().getHits()).
                map(hit -> hit.getId()).collect(Collectors.toList()),
                kind);
    }

    private QueryBuilder getQueryBuilder(final SearchCond cond, final AnyTypeKind kind) {
        QueryBuilder builder = EMPTY_QUERY_BUILDER;

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                if (cond.getAnyTypeCond() != null && AnyTypeKind.ANY_OBJECT == kind) {
                    builder = getQueryBuilder(cond.getAnyTypeCond());
                } else if (cond.getRelationshipTypeCond() != null
                        && (AnyTypeKind.USER == kind || AnyTypeKind.ANY_OBJECT == kind)) {

                    builder = getQueryBuilder(cond.getRelationshipTypeCond());
                } else if (cond.getRelationshipCond() != null
                        && (AnyTypeKind.USER == kind || AnyTypeKind.ANY_OBJECT == kind)) {

                    builder = getQueryBuilder(cond.getRelationshipCond());
                } else if (cond.getMembershipCond() != null
                        && (AnyTypeKind.USER == kind || AnyTypeKind.ANY_OBJECT == kind)) {

                    builder = getQueryBuilder(cond.getMembershipCond());
                } else if (cond.getAssignableCond() != null) {
                    builder = getQueryBuilder(cond.getAssignableCond());
                } else if (cond.getRoleCond() != null && AnyTypeKind.USER == kind) {
                    builder = getQueryBuilder(cond.getRoleCond());
                } else if (cond.getDynRealmCond() != null) {
                    builder = getQueryBuilder(cond.getDynRealmCond());
                } else if (cond.getMemberCond() != null && AnyTypeKind.GROUP == kind) {
                    builder = getQueryBuilder(cond.getMemberCond());
                } else if (cond.getResourceCond() != null) {
                    builder = getQueryBuilder(cond.getResourceCond());
                } else if (cond.getAttributeCond() != null) {
                    builder = getQueryBuilder(cond.getAttributeCond(), kind);
                } else if (cond.getAnyCond() != null) {
                    builder = getQueryBuilder(cond.getAnyCond(), kind);
                }
                builder = checkNot(builder, cond.getType() == SearchCond.Type.NOT_LEAF);
                break;

            case AND:
                builder = QueryBuilders.boolQuery().
                        must(getQueryBuilder(cond.getLeftSearchCond(), kind)).
                        must(getQueryBuilder(cond.getRightSearchCond(), kind));
                break;

            case OR:
                builder = QueryBuilders.disMaxQuery().
                        add(getQueryBuilder(cond.getLeftSearchCond(), kind)).
                        add(getQueryBuilder(cond.getRightSearchCond(), kind));
                break;

            default:
        }

        return builder;
    }

    private QueryBuilder checkNot(final QueryBuilder builder, final boolean not) {
        return not
                ? QueryBuilders.boolQuery().mustNot(builder)
                : builder;
    }

    private QueryBuilder getQueryBuilder(final AnyTypeCond cond) {
        return QueryBuilders.termQuery("anyType", cond.getAnyTypeKey());
    }

    private QueryBuilder getQueryBuilder(final RelationshipTypeCond cond) {
        return QueryBuilders.termQuery("relationshipTypes", cond.getRelationshipTypeKey());
    }

    private QueryBuilder getQueryBuilder(final RelationshipCond cond) {
        String rightAnyObjectKey;
        try {
            rightAnyObjectKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return QueryBuilders.termQuery("relationships", rightAnyObjectKey);
    }

    private QueryBuilder getQueryBuilder(final MembershipCond cond) {
        String groupKey;
        try {
            groupKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return QueryBuilders.termQuery("memberships", groupKey);
    }

    private QueryBuilder getQueryBuilder(final AssignableCond cond) {
        Realm realm;
        try {
            realm = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        DisMaxQueryBuilder builder = QueryBuilders.disMaxQuery();
        if (cond.isFromGroup()) {
            realmDAO.findDescendants(realm).forEach(current -> {
                builder.add(QueryBuilders.termQuery("realm", current.getFullPath()));
            });
        } else {
            for (Realm current = realm; current.getParent() != null; current = current.getParent()) {
                builder.add(QueryBuilders.termQuery("realm", current.getFullPath()));
            }
            builder.add(QueryBuilders.termQuery("realm", realmDAO.getRoot().getFullPath()));
        }

        return builder;
    }

    private QueryBuilder getQueryBuilder(final RoleCond cond) {
        return QueryBuilders.termQuery("roles", cond.getRole());
    }

    private QueryBuilder getQueryBuilder(final DynRealmCond cond) {
        return QueryBuilders.termQuery("dynRealms", cond.getDynRealm());
    }

    private QueryBuilder getQueryBuilder(final MemberCond cond) {
        String memberKey;
        try {
            memberKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return QueryBuilders.termQuery("members", memberKey);
    }

    private QueryBuilder getQueryBuilder(final ResourceCond cond) {
        return QueryBuilders.termQuery("resources", cond.getResourceKey());
    }

    private QueryBuilder fillAttrQuery(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final AttributeCond cond) {

        Object value = schema.getType() == AttrSchemaType.Date && attrValue.getDateValue() != null
                ? attrValue.getDateValue().getTime()
                : attrValue.getValue();

        QueryBuilder builder = EMPTY_QUERY_BUILDER;

        switch (cond.getType()) {
            case ISNOTNULL:
                builder = QueryBuilders.existsQuery(schema.getKey());
                break;

            case ISNULL:
                builder = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(schema.getKey()));
                break;

            case ILIKE:
                builder = QueryBuilders.queryStringQuery(
                        schema.getKey() + ":" + cond.getExpression().replace('%', '*').toLowerCase());
                break;

            case LIKE:
                builder = QueryBuilders.wildcardQuery(schema.getKey(), cond.getExpression().replace('%', '*'));
                break;

            case IEQ:
                builder = QueryBuilders.matchQuery(schema.getKey(), cond.getExpression().toLowerCase());
                break;

            case EQ:
                builder = QueryBuilders.termQuery(schema.getKey(), value);
                break;

            case GE:
                builder = QueryBuilders.rangeQuery(schema.getKey()).gte(value);
                break;

            case GT:
                builder = QueryBuilders.rangeQuery(schema.getKey()).gt(value);
                break;

            case LE:
                builder = QueryBuilders.rangeQuery(schema.getKey()).lte(value);
                break;

            case LT:
                builder = QueryBuilders.rangeQuery(schema.getKey()).lt(value);
                break;

            default:
        }

        return builder;
    }

    private QueryBuilder getQueryBuilder(final AttributeCond cond, final AnyTypeKind kind) {
        Pair<PlainSchema, PlainAttrValue> checked;
        try {
            checked = check(cond, kind);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return fillAttrQuery(checked.getLeft(), checked.getRight(), cond);
    }

    private QueryBuilder getQueryBuilder(final AnyCond cond, final AnyTypeKind kind) {
        Triple<PlainSchema, PlainAttrValue, AnyCond> checked;
        try {
            checked = check(cond, kind);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return fillAttrQuery(checked.getLeft(), checked.getMiddle(), checked.getRight());
    }
}
