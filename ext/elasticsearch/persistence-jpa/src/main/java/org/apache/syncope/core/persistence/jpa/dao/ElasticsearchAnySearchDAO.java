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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Search engine implementation for users, groups and any objects, based on Elasticsearch.
 */
public class ElasticsearchAnySearchDAO extends AbstractAnySearchDAO {

    private static final QueryBuilder EMPTY_QUERY_BUILDER = new MatchNoneQueryBuilder();

    @Autowired
    private Client client;

    @Autowired
    private ElasticsearchUtils elasticsearchUtils;

    private DisMaxQueryBuilder adminRealmsFilter(final Set<String> adminRealms) {
        DisMaxQueryBuilder builder = QueryBuilders.disMaxQuery();

        for (String realmPath : RealmUtils.normalize(adminRealms)) {
            Realm realm = realmDAO.findByFullPath(realmPath);
            if (realm == null) {
                LOG.warn("Ignoring invalid realm {}", realmPath);
            } else {
                for (Realm descendant : realmDAO.findDescendants(realm)) {
                    builder.add(QueryBuilders.termQuery("realm.keyword", descendant.getFullPath()));
                }
            }
        }

        return builder;
    }

    private SearchRequestBuilder searchRequestBuilder(
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        return client.prepareSearch(AuthContextUtils.getDomain().toLowerCase()).
                setTypes(kind.name()).
                setSearchType(SearchType.QUERY_THEN_FETCH).
                setQuery(SyncopeConstants.FULL_ADMIN_REALMS.equals(adminRealms)
                        ? getQueryBuilder(cond, kind)
                        : QueryBuilders.boolQuery().
                                must(adminRealmsFilter(adminRealms)).
                                must(getQueryBuilder(cond, kind)));
    }

    @Override
    protected int doCount(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        SearchRequestBuilder builder = searchRequestBuilder(adminRealms, cond, kind).
                setFrom(0).setSize(0);

        return (int) builder.get().getHits().getTotalHits();
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
        for (OrderByClause clause : orderBy) {
            builder.addSort(clause.getField(), SortOrder.valueOf(clause.getDirection().name()));
        }

        return buildResult(
                CollectionUtils.collect(Arrays.asList(builder.get().getHits().getHits()),
                        new Transformer<SearchHit, Object>() {

                    @Override
                    public Object transform(final SearchHit input) {
                        return input.getId();
                    }
                }, new ArrayList<>()),
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
        return QueryBuilders.termQuery("anyType.keyword", cond.getAnyTypeKey());
    }

    private QueryBuilder getQueryBuilder(final RelationshipTypeCond cond) {
        return QueryBuilders.termQuery("relationshipTypes.keyword", cond.getRelationshipTypeKey());
    }

    private QueryBuilder getQueryBuilder(final RelationshipCond cond) {
        String rightAnyObjectKey;
        try {
            rightAnyObjectKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return QueryBuilders.termQuery("relationships.keyword", rightAnyObjectKey);
    }

    private QueryBuilder getQueryBuilder(final MembershipCond cond) {
        String groupKey;
        try {
            groupKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return QueryBuilders.termQuery("memberships.keyword", groupKey);
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
            for (Realm current = realm; current.getParent() != null; current = current.getParent()) {
                builder.add(QueryBuilders.termQuery("realm.keyword", current.getFullPath()));
            }
            builder.add(QueryBuilders.termQuery("realm.keyword", realmDAO.getRoot().getFullPath()));
        } else {
            for (Realm current : realmDAO.findDescendants(realm)) {
                builder.add(QueryBuilders.termQuery("realm.keyword", current.getFullPath()));
            }
        }

        return builder;
    }

    private QueryBuilder getQueryBuilder(final RoleCond cond) {
        return QueryBuilders.termQuery("roles.keyword", cond.getRoleKey());
    }

    private QueryBuilder getQueryBuilder(final MemberCond cond) {
        String memberKey;
        try {
            memberKey = check(cond);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY_BUILDER;
        }

        return QueryBuilders.termQuery("members.keyword", memberKey);
    }

    private QueryBuilder getQueryBuilder(final ResourceCond cond) {
        return QueryBuilders.termQuery("resources.keyword", cond.getResourceKey());
    }

    private QueryBuilder fillAttrQuery(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final AttributeCond cond) {

        String name = schema.getType() == AttrSchemaType.String
                || schema.getType() == AttrSchemaType.Enum
                ? schema.getKey() + ".keyword"
                : schema.getKey();
        Object value = schema.getType() == AttrSchemaType.Date && attrValue.getDateValue() != null
                ? attrValue.getDateValue().getTime()
                : attrValue.getValue();

        QueryBuilder builder = EMPTY_QUERY_BUILDER;

        switch (cond.getType()) {
            case ISNOTNULL:
                builder = QueryBuilders.existsQuery(name);
                break;

            case ISNULL:
                builder = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(name));
                break;

            case ILIKE:
                builder = QueryBuilders.queryStringQuery(
                        schema.getKey() + ":" + cond.getExpression().replace('%', '*'));
                break;

            case LIKE:
                builder = QueryBuilders.wildcardQuery(name, cond.getExpression().replace('%', '*'));
                break;

            case IEQ:
                builder = QueryBuilders.matchQuery(schema.getKey(), value);
                break;

            case EQ:
                builder = QueryBuilders.termQuery(name, value);
                break;

            case GE:
                builder = QueryBuilders.rangeQuery(name).gte(value);
                break;

            case GT:
                builder = QueryBuilders.rangeQuery(name).gt(value);
                break;

            case LE:
                builder = QueryBuilders.rangeQuery(name).lte(value);
                break;

            case LT:
                builder = QueryBuilders.rangeQuery(name).lt(value);
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
