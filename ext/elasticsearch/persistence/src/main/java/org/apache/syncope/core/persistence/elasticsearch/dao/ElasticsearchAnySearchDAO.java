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
package org.apache.syncope.core.persistence.elasticsearch.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AbstractAnySearchDAO;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

/**
 * Search engine implementation for users, groups and any objects, based on Elasticsearch.
 */
public class ElasticsearchAnySearchDAO extends AbstractAnySearchDAO {

    protected final ElasticsearchClient client;

    protected final int indexMaxResultWindow;

    public ElasticsearchAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final ElasticsearchClient client,
            final int indexMaxResultWindow) {

        super(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator);

        this.client = client;
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    protected Triple<Optional<Query>, Set<String>, Set<String>> getAdminRealmsFilter(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final AnyTypeKind kind) {

        Set<String> dynRealmKeys = new HashSet<>();
        Set<String> groupOwners = new HashSet<>();
        List<Query> queries = new ArrayList<>();

        if (recursive) {
            adminRealms.forEach(realmPath -> {
                Optional<Pair<String, String>> goRealm = RealmUtils.parseGroupOwnerRealm(realmPath);
                if (goRealm.isPresent()) {
                    groupOwners.add(goRealm.get().getRight());
                } else if (realmPath.startsWith("/")) {
                    Realm realm = realmSearchDAO.findByFullPath(realmPath).
                            orElseThrow(() -> new IllegalArgumentException("Invalid Realm full path: " + realmPath));

                    realmSearchDAO.findDescendants(realm.getFullPath(), base.getFullPath()).
                            forEach(descendant -> queries.add(
                            new Query.Builder().term(QueryBuilders.term().
                                    field("realm").value(descendant).build()).
                                    build()));
                } else {
                    dynRealmDAO.findById(realmPath).ifPresentOrElse(
                            dynRealm -> {
                                dynRealmKeys.add(dynRealm.getKey());
                                queries.add(new Query.Builder().term(QueryBuilders.term().
                                        field("dynRealm").value(dynRealm.getKey()).build()).
                                        build());
                            },
                            () -> LOG.warn("Ignoring invalid dynamic realm {}", realmPath));
                }
            });
        } else {
            if (adminRealms.stream().anyMatch(r -> r.startsWith(base.getFullPath()))) {
                queries.add(new Query.Builder().term(QueryBuilders.term().
                        field("realm").value(base.getKey()).build()).
                        build());
            }
        }

        return Triple.of(
                dynRealmKeys.isEmpty() && groupOwners.isEmpty()
                ? Optional.of(new Query.Builder().disMax(QueryBuilders.disMax().queries(queries).build()).build())
                : Optional.empty(),
                dynRealmKeys,
                groupOwners);
    }

    protected Query getQuery(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        Query query;
        if (SyncopeConstants.FULL_ADMIN_REALMS.equals(adminRealms)) {
            query = getQuery(cond, kind);

            if (!recursive) {
                query = new Query.Builder().bool(
                        QueryBuilders.bool().
                                must(new Query.Builder().term(QueryBuilders.term().
                                        field("realm").value(base.getKey()).build()).
                                        build()).
                                must(query).build()).
                        build();
            }
        } else {
            Triple<Optional<Query>, Set<String>, Set<String>> filter =
                    getAdminRealmsFilter(base, recursive, adminRealms, kind);
            query = getQuery(buildEffectiveCond(cond, filter.getMiddle(), filter.getRight(), kind), kind);

            if (filter.getLeft().isPresent()) {
                query = new Query.Builder().bool(
                        QueryBuilders.bool().
                                must(filter.getLeft().get()).
                                must(query).build()).
                        build();
            }
        }

        return query;
    }

    @Override
    protected long doCount(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        CountRequest request = new CountRequest.Builder().
                index(ElasticsearchUtils.getAnyIndex(AuthContextUtils.getDomain(), kind)).
                query(getQuery(base, recursive, adminRealms, cond, kind)).
                build();
        LOG.debug("Count JSON request: {}", request);

        try {
            return client.count(request).count();
        } catch (Exception e) {
            LOG.error("While counting in Elasticsearch", e);
            return 0;
        }
    }

    protected List<SortOptions> sortBuilders(final AnyTypeKind kind, final Stream<Sort.Order> orderBy) {
        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);

        List<SortOptions> options = new ArrayList<>();
        orderBy.forEach(clause -> {
            String sortName = null;

            // Manage difference among external key attribute and internal JPA @Id
            String fieldName = "key".equals(clause.getProperty()) ? "id" : clause.getProperty();

            Field anyField = anyUtils.getField(fieldName).orElse(null);
            if (anyField == null) {
                PlainSchema schema = plainSchemaDAO.findById(fieldName).orElse(null);
                if (schema != null) {
                    sortName = fieldName;
                }
            } else {
                sortName = fieldName;
            }

            if (sortName == null) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                options.add(new SortOptions.Builder().field(
                        new FieldSort.Builder().
                                field(sortName).
                                order(clause.getDirection() == Sort.Direction.ASC
                                        ? SortOrder.Asc : SortOrder.Desc).
                                build()).
                        build());
            }
        });
        return options;
    }

    @Override
    protected <T extends Any> List<T> doSearch(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind) {

        SearchRequest request = new SearchRequest.Builder().
                index(ElasticsearchUtils.getAnyIndex(AuthContextUtils.getDomain(), kind)).
                searchType(SearchType.QueryThenFetch).
                query(getQuery(base, recursive, adminRealms, cond, kind)).
                from(pageable.isUnpaged() ? 0 : pageable.getPageSize() * pageable.getPageNumber()).
                size(pageable.isUnpaged() ? indexMaxResultWindow : pageable.getPageSize()).
                sort(sortBuilders(kind, pageable.getSort().get())).
                build();
        LOG.debug("Search JSON request: {}", request);

        List<Hit<Void>> esResult = null;
        try {
            esResult = client.search(request, Void.class).hits().hits();
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch", e);
        }

        return CollectionUtils.isEmpty(esResult)
                ? List.of()
                : buildResult(esResult.stream().map(Hit::id).collect(Collectors.toList()), kind);
    }

    protected Query getQuery(final SearchCond cond, final AnyTypeKind kind) {
        Query query = null;

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                query = cond.asLeaf(AnyTypeCond.class).
                        filter(leaf -> AnyTypeKind.ANY_OBJECT == kind).
                        map(this::getQuery).
                        orElse(null);

                if (query == null) {
                    query = cond.asLeaf(RelationshipTypeCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(RelationshipCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(MembershipCond.class).
                            filter(leaf -> AnyTypeKind.GROUP != kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(MemberCond.class).
                            filter(leaf -> AnyTypeKind.GROUP == kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(RoleCond.class).
                            filter(leaf -> AnyTypeKind.USER == kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(DynRealmCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(AuxClassCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(ResourceCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.asLeaf(AnyCond.class).map(ac -> getQuery(ac, kind)).
                            or(() -> cond.asLeaf(AttrCond.class).map(this::getQuery)).
                            orElse(null);
                }

                // allow for additional search conditions
                if (query == null) {
                    query = getQueryForCustomConds(cond, kind);
                }

                if (query == null) {
                    throw new IllegalArgumentException("Cannot construct QueryBuilder");
                }

                if (cond.getType() == SearchCond.Type.NOT_LEAF) {
                    query = new Query.Builder().bool(QueryBuilders.bool().mustNot(query).build()).build();
                }
                break;

            case AND:
                List<Query> andCompound = new ArrayList<>();

                Query andLeft = getQuery(cond.getLeft(), kind);
                if (andLeft._kind() == Query.Kind.Bool && !((BoolQuery) andLeft._get()).must().isEmpty()) {
                    andCompound.addAll(((BoolQuery) andLeft._get()).must());
                } else {
                    andCompound.add(andLeft);
                }

                Query andRight = getQuery(cond.getRight(), kind);
                if (andRight._kind() == Query.Kind.Bool && !((BoolQuery) andRight._get()).must().isEmpty()) {
                    andCompound.addAll(((BoolQuery) andRight._get()).must());
                } else {
                    andCompound.add(andRight);
                }

                query = new Query.Builder().bool(QueryBuilders.bool().must(andCompound).build()).build();
                break;

            case OR:
                List<Query> orCompound = new ArrayList<>();

                Query orLeft = getQuery(cond.getLeft(), kind);
                if (orLeft._kind() == Query.Kind.DisMax) {
                    orCompound.addAll(((DisMaxQuery) orLeft._get()).queries());
                } else {
                    orCompound.add(orLeft);
                }

                Query orRight = getQuery(cond.getRight(), kind);
                if (orRight._kind() == Query.Kind.DisMax) {
                    orCompound.addAll(((DisMaxQuery) orRight._get()).queries());
                } else {
                    orCompound.add(orRight);
                }

                query = new Query.Builder().disMax(QueryBuilders.disMax().queries(orCompound).build()).build();
                break;

            default:
        }

        return query;
    }

    protected Query getQuery(final AnyTypeCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("anyType").value(cond.getAnyTypeKey()).build()).
                build();
    }

    protected Query getQuery(final RelationshipTypeCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("relationshipTypes").value(cond.getRelationshipTypeKey()).build()).
                build();
    }

    protected Query getQuery(final RelationshipCond cond) {
        List<Query> queries = check(cond).stream().
                map(key -> new Query.Builder().term(QueryBuilders.term().
                field("relationships").value(key).build()).
                build()).toList();

        return queries.size() == 1
                ? queries.getFirst()
                : new Query.Builder().disMax(QueryBuilders.disMax().queries(queries).build()).build();
    }

    protected Query getQuery(final MembershipCond cond) {
        List<Query> queries = check(cond).stream().
                map(key -> new Query.Builder().term(QueryBuilders.term().
                field("memberships").value(key).build()).
                build()).toList();

        return queries.size() == 1
                ? queries.getFirst()
                : new Query.Builder().disMax(QueryBuilders.disMax().queries(queries).build()).build();
    }

    protected Query getQuery(final RoleCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("roles").value(cond.getRole()).build()).
                build();
    }

    protected Query getQuery(final DynRealmCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("dynRealms").value(cond.getDynRealm()).build()).
                build();
    }

    protected Query getQuery(final MemberCond cond) {
        List<Query> queries = check(cond).stream().
                map(key -> new Query.Builder().term(QueryBuilders.term().
                field("members").value(key).build()).
                build()).toList();

        return queries.size() == 1
                ? queries.getFirst()
                : new Query.Builder().disMax(QueryBuilders.disMax().queries(queries).build()).build();
    }

    protected Query getQuery(final AuxClassCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("auxClasses").value(cond.getAuxClass()).build()).
                build();
    }

    protected Query getQuery(final ResourceCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("resources").value(cond.getResource()).build()).
                build();
    }

    protected Query fillAttrQuery(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final AttrCond cond) {

        Object value = schema.getType() == AttrSchemaType.Date && attrValue.getDateValue() != null
                ? FormatUtils.format(attrValue.getDateValue())
                : attrValue.getValue();

        Query query = null;

        switch (cond.getType()) {
            case ISNOTNULL:
                query = new Query.Builder().exists(QueryBuilders.exists().field(schema.getKey()).build()).build();
                break;

            case ISNULL:
                query = new Query.Builder().bool(QueryBuilders.bool().mustNot(
                        new Query.Builder().exists(QueryBuilders.exists().field(schema.getKey()).build()).build()).
                        build()).build();
                break;

            case ILIKE:
                StringBuilder output = new StringBuilder();
                for (char c : cond.getExpression().toLowerCase().replace("\\_", "_").toCharArray()) {
                    if (c == '%') {
                        output.append(".*");
                    } else if (Character.isLetter(c)) {
                        output.append('[').
                                append(c).
                                append(Character.toUpperCase(c)).
                                append(']');
                    } else {
                        output.append(ElasticsearchUtils.escapeForLikeRegex(c));
                    }
                }
                query = new Query.Builder().regexp(QueryBuilders.regexp().
                        field(schema.getKey()).value(output.toString()).build()).build();
                break;

            case LIKE:
                query = new Query.Builder().wildcard(QueryBuilders.wildcard().
                        field(schema.getKey()).value(cond.getExpression().replace('%', '*').replace("\\_", "_")).
                        build()).build();
                break;

            case IEQ:
                query = new Query.Builder().match(QueryBuilders.match().
                        field(schema.getKey()).query(cond.getExpression().toLowerCase()).build()).
                        build();
                break;

            case EQ:
                FieldValue fieldValue;
                if (value instanceof Double aDouble) {
                    fieldValue = FieldValue.of(aDouble);
                } else if (value instanceof Long aLong) {
                    fieldValue = FieldValue.of(aLong);
                } else if (value instanceof Boolean aBoolean) {
                    fieldValue = FieldValue.of(aBoolean);
                } else {
                    fieldValue = FieldValue.of(value.toString());
                }
                query = new Query.Builder().term(QueryBuilders.term().
                        field(schema.getKey()).value(fieldValue).build()).
                        build();
                break;

            case GE:
                query = new Query.Builder().range(RangeQuery.of(r -> r.untyped(n -> n.
                        field(schema.getKey()).
                        gte(JsonData.of(value))))).
                        build();
                break;

            case GT:
                query = new Query.Builder().range(RangeQuery.of(r -> r.untyped(n -> n.
                        field(schema.getKey()).
                        gt(JsonData.of(value))))).
                        build();
                break;

            case LE:
                query = new Query.Builder().range(RangeQuery.of(r -> r.untyped(n -> n.
                        field(schema.getKey()).
                        lte(JsonData.of(value))))).
                        build();
                break;

            case LT:
                query = new Query.Builder().range(RangeQuery.of(r -> r.untyped(n -> n.
                        field(schema.getKey()).
                        lt(JsonData.of(value))))).
                        build();
                break;

            default:
        }

        return query;
    }

    protected Query getQuery(final AttrCond cond) {
        Pair<PlainSchema, PlainAttrValue> checked = check(cond);

        return fillAttrQuery(checked.getLeft(), checked.getRight(), cond);
    }

    protected Query getQuery(final AnyCond cond, final AnyTypeKind kind) {
        if (JAXRSService.PARAM_REALM.equals(cond.getSchema()) && cond.getExpression().startsWith("/")) {
            Realm realm = realmSearchDAO.findByFullPath(cond.getExpression()).
                    orElseThrow(() -> new IllegalArgumentException("Invalid Realm full path: " + cond.getExpression()));
            cond.setExpression(realm.getKey());
        }

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked = check(cond, kind);

        return fillAttrQuery(checked.getLeft(), checked.getMiddle(), checked.getRight());
    }

    protected Query getQueryForCustomConds(final SearchCond cond, final AnyTypeKind kind) {
        return null;
    }
}
