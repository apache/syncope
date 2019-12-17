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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
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
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.MatchNoneQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Search engine implementation for users, groups and any objects, based on Elasticsearch.
 */
public class ElasticsearchAnySearchDAO extends AbstractAnySearchDAO {

    private static final QueryBuilder EMPTY_QUERY_BUILDER = new MatchNoneQueryBuilder();

    @Autowired
    private RestHighLevelClient client;

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
                    realmDAO.findDescendants(realm).forEach(
                            descendant -> builder.add(QueryBuilders.termQuery("realm", descendant.getFullPath())));
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
            realmDAO.findAll().forEach(
                    descendant -> builder.add(QueryBuilders.termQuery("realm", descendant.getFullPath())));
        }

        return Pair.of(builder, dynRealmKeys);
    }

    private SearchRequest searchRequest(
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind,
            final int from,
            final int size,
            final List<SortBuilder<?>> sortBuilders) {

        Pair<DisMaxQueryBuilder, Set<String>> filter = adminRealmsFilter(adminRealms);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().
                query(SyncopeConstants.FULL_ADMIN_REALMS.equals(adminRealms)
                        ? getQueryBuilder(cond, kind)
                        : QueryBuilders.boolQuery().
                                must(filter.getLeft()).
                                must(getQueryBuilder(buildEffectiveCond(cond, filter.getRight()), kind))).
                from(from).
                size(size);
        sortBuilders.forEach(sourceBuilder::sort);

        return new SearchRequest(ElasticsearchUtils.getContextDomainName(AuthContextUtils.getDomain(), kind)).
                searchType(SearchType.QUERY_THEN_FETCH).
                source(sourceBuilder);
    }

    @Override
    protected int doCount(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        SearchRequest request = searchRequest(adminRealms, cond, kind, 0, 0, List.of());
        try {
            return (int) client.search(request, RequestOptions.DEFAULT).getHits().getTotalHits().value;
        } catch (IOException e) {
            LOG.error("Search error", e);
            return 0;
        }
    }

    private List<SortBuilder<?>> sortBuilders(
            final AnyTypeKind kind,
            final List<OrderByClause> orderBy) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);

        List<SortBuilder<?>> builders = new ArrayList<>();
        orderBy.forEach(clause -> {
            String sortName = null;

            // Manage difference among external key attribute and internal JPA @Id
            String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

            Field anyField = anyUtils.getField(fieldName);
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
                builders.add(new FieldSortBuilder(sortName).order(SortOrder.valueOf(clause.getDirection().name())));
            }
        });
        return builders;
    }

    @Override
    protected <T extends Any<?>> List<T> doSearch(
            final Set<String> adminRealms,
            final SearchCond cond,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderBy,
            final AnyTypeKind kind) {

        SearchRequest request = searchRequest(
                adminRealms,
                cond,
                kind,
                (itemsPerPage * (page <= 0 ? 0 : page - 1)),
                (itemsPerPage < 0 ? elasticsearchUtils.getIndexMaxResultWindow() : itemsPerPage),
                sortBuilders(kind, orderBy));

        SearchHit[] esResult = null;
        try {
            esResult = client.search(request, RequestOptions.DEFAULT).getHits().getHits();
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch", e);
        }

        return ArrayUtils.isEmpty(esResult)
                ? List.of()
                : buildResult(Stream.of(Objects.requireNonNull(esResult))
                        .map(SearchHit::getId).collect(Collectors.toList()), kind);
    }

    private QueryBuilder getQueryBuilder(final SearchCond cond, final AnyTypeKind kind) {
        QueryBuilder builder = null;

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                builder = cond.getLeaf(AnyTypeCond.class).
                        filter(leaf -> AnyTypeKind.ANY_OBJECT == kind).
                        map(leaf -> getQueryBuilder(leaf)).
                        orElse(null);

                if (builder == null) {
                    builder = cond.getLeaf(RelationshipTypeCond.class).
                            filter(leaf -> AnyTypeKind.GROUP != kind).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(RelationshipCond.class).
                            filter(leaf -> AnyTypeKind.GROUP != kind).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(MembershipCond.class).
                            filter(leaf -> AnyTypeKind.GROUP != kind).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(MemberCond.class).
                            filter(leaf -> AnyTypeKind.GROUP == kind).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(AssignableCond.class).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(RoleCond.class).
                            filter(leaf -> AnyTypeKind.USER == kind).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(PrivilegeCond.class).
                            filter(leaf -> AnyTypeKind.USER == kind).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(DynRealmCond.class).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    builder = cond.getLeaf(ResourceCond.class).
                            map(leaf -> getQueryBuilder(leaf)).
                            orElse(null);
                }

                if (builder == null) {
                    Optional<AnyCond> anyCond = cond.getLeaf(AnyCond.class);
                    if (anyCond.isPresent()) {
                        builder = getQueryBuilder(anyCond.get(), kind);
                    } else {
                        builder = cond.getLeaf(AttrCond.class).
                                map(leaf -> getQueryBuilder(leaf, kind)).
                                orElse(null);
                    }
                }

                if (builder == null) {
                    builder = EMPTY_QUERY_BUILDER;
                }
                builder = checkNot(builder, cond.getType() == SearchCond.Type.NOT_LEAF);
                break;

            case AND:
                builder = QueryBuilders.boolQuery().
                        must(getQueryBuilder(cond.getLeft(), kind)).
                        must(getQueryBuilder(cond.getRight(), kind));
                break;

            case OR:
                builder = QueryBuilders.disMaxQuery().
                        add(getQueryBuilder(cond.getLeft(), kind)).
                        add(getQueryBuilder(cond.getRight(), kind));
                break;

            default:
        }

        return builder;
    }

    private static QueryBuilder checkNot(final QueryBuilder builder, final boolean not) {
        return not
                ? QueryBuilders.boolQuery().mustNot(builder)
                : builder;
    }

    private static QueryBuilder getQueryBuilder(final AnyTypeCond cond) {
        return QueryBuilders.termQuery("anyType", cond.getAnyTypeKey());
    }

    private static QueryBuilder getQueryBuilder(final RelationshipTypeCond cond) {
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
            realmDAO.findDescendants(realm).forEach(
                    current -> builder.add(QueryBuilders.termQuery("realm", current.getFullPath())));
        } else {
            for (Realm current = realm; current.getParent() != null; current = current.getParent()) {
                builder.add(QueryBuilders.termQuery("realm", current.getFullPath()));
            }
            builder.add(QueryBuilders.termQuery("realm", realmDAO.getRoot().getFullPath()));
        }

        return builder;
    }

    private static QueryBuilder getQueryBuilder(final RoleCond cond) {
        return QueryBuilders.termQuery("roles", cond.getRole());
    }

    private static QueryBuilder getQueryBuilder(final PrivilegeCond cond) {
        return QueryBuilders.termQuery("privileges", cond.getPrivilege());
    }

    private static QueryBuilder getQueryBuilder(final DynRealmCond cond) {
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

    private static QueryBuilder getQueryBuilder(final ResourceCond cond) {
        return QueryBuilders.termQuery("resources", cond.getResourceKey());
    }

    private static QueryBuilder fillAttrQuery(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final AttrCond cond) {

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
                StringBuilder output = new StringBuilder();
                for (char c : cond.getExpression().toLowerCase().toCharArray()) {
                    if (c == '%') {
                        output.append(".*");
                    } else if (Character.isLetter(c)) {
                        output.append('[').
                                append(c).
                                append(Character.toUpperCase(c)).
                                append(']');
                    } else {
                        output.append(c);
                    }
                }
                builder = QueryBuilders.regexpQuery(schema.getKey(), output.toString());
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

    private QueryBuilder getQueryBuilder(final AttrCond cond, final AnyTypeKind kind) {
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
