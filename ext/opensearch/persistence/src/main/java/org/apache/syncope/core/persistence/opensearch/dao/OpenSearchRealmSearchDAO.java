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
package org.apache.syncope.core.persistence.opensearch.dao;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AbstractRealmSearchDAO;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.opensearch.client.OpenSearchUtils;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.BuiltinScriptLanguage;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.ScriptLanguage;
import org.opensearch.client.opensearch._types.ScriptSortType;
import org.opensearch.client.opensearch._types.SearchType;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public class OpenSearchRealmSearchDAO extends AbstractRealmSearchDAO implements RealmSearchDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(RealmSearchDAO.class);

    protected static final Set<String> ID_PROPS = Set.of("key", "id", "_id");

    protected static final List<SortOptions> FULLPATH_SORT_OPTIONS = List.of(
            new SortOptions.Builder().
                    script(s -> s.type(ScriptSortType.Number).
                    script(t -> t.inline(i -> i.lang(ScriptLanguage.builder().
                    builtin(BuiltinScriptLanguage.Painless).build()).
                    source("doc['fullPath'].value.chars().filter(ch -> ch == '/').count()"))).
                    order(SortOrder.Asc)).
                    build());

    protected final RealmDAO realmDAO;

    protected final RealmUtils realmUtils;

    protected final OpenSearchClient client;

    protected final int indexMaxResultWindow;

    public OpenSearchRealmSearchDAO(
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator,
            final OpenSearchClient client,
            final int indexMaxResultWindow) {

        super(plainSchemaDAO, entityFactory, validator);
        this.realmDAO = realmDAO;
        this.realmUtils = new RealmUtils(entityFactory);
        this.client = client;
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Realm> findByFullPath(final String fullPath) {
        if (SyncopeConstants.ROOT_REALM.equals(fullPath)) {
            return Optional.of(realmDAO.getRoot());
        }

        if (StringUtils.isBlank(fullPath) || !RealmDAO.PATH_PATTERN.matcher(fullPath).matches()) {
            throw new MalformedPathException(fullPath);
        }

        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(new Query.Builder().term(QueryBuilders.term().
                        field("fullPath").value(FieldValue.of(fullPath)).caseInsensitive(false).build()).build()).
                size(1).
                fields(List.of()).source(new SourceConfig.Builder().fetch(false).build()).
                build();
        LOG.debug("Search request: {}", request);

        try {
            String result = client.search(request, Void.class).hits().hits().stream().findFirst().
                    map(Hit::id).
                    orElse(null);
            return realmDAO.findById(result).map(Realm.class::cast);
        } catch (Exception e) {
            LOG.error("While searching Elasticsearch for Realm path {} with request {}", fullPath, request, e);
        }

        return Optional.empty();
    }

    protected List<String> search(final Query query) {
        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(query).
                sort(FULLPATH_SORT_OPTIONS).
                fields(List.of()).source(new SourceConfig.Builder().fetch(false).build()).
                build();
        LOG.debug("Search request: {}", request);

        try {
            return client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    toList();
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch with request {}", request, e);
            return List.of();
        }
    }

    @Override
    public List<Realm> findByName(final String name) {
        List<String> result = search(
                new Query.Builder().term(QueryBuilders.term().
                        field("name").value(FieldValue.of(name)).caseInsensitive(false).build()).build());
        return result.stream().map(realmDAO::findById).
                flatMap(Optional::stream).map(Realm.class::cast).toList();
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        List<String> result = search(
                new Query.Builder().term(QueryBuilders.term().
                        field("parent_id").value(FieldValue.of(realm.getKey())).caseInsensitive(false).
                        build()).build());
        return result.stream().map(realmDAO::findById).
                flatMap(Optional::stream).map(Realm.class::cast).toList();
    }

    protected Query buildDescendantsQuery(final Set<String> bases, final SearchCond searchCond) {
        List<Query> basesQueries = new ArrayList<>();
        bases.forEach(base -> {
            basesQueries.add(new Query.Builder().term(QueryBuilders.term().
                    field("fullPath").value(FieldValue.of(base)).caseInsensitive(false).build()).build());
            basesQueries.add(new Query.Builder().regexp(QueryBuilders.regexp().
                    field("fullPath").value(SyncopeConstants.ROOT_REALM.equals(base) ? "/.*" : base + "/.*").
                    build()).build());
        });
        Query prefix = new Query.Builder().disMax(QueryBuilders.disMax().queries(basesQueries).build()).build();

        BoolQuery.Builder boolBuilder = QueryBuilders.bool().filter(prefix);
        if (searchCond != null) {
            boolBuilder.filter(getQuery(searchCond));
        }
        return new Query.Builder().bool(boolBuilder.build()).build();
    }

    protected Query getQuery(final SearchCond cond) {
        Query query = null;

        switch (cond.getType()) {
            case LEAF, NOT_LEAF -> {
                query = cond.asLeaf(AnyCond.class).map(this::getQuery).orElse(null);
                if (query == null) {
                    query = cond.asLeaf(AttrCond.class).map(this::getQuery).orElse(null);
                }
                if (query == null) {
                    query = getQueryForCustomConds(cond);
                }
                if (query == null) {
                    throw new IllegalArgumentException("Cannot construct QueryBuilder");
                }
                if (cond.getType() == SearchCond.Type.NOT_LEAF) {
                    query = new Query.Builder().bool(QueryBuilders.bool().mustNot(query).build()).build();
                }
            }
            case AND -> {
                List<Query> andCompound = new ArrayList<>();
                Query andLeft = getQuery(cond.getLeft());
                if (andLeft.isBool() && !andLeft.bool().filter().isEmpty()) {
                    andCompound.addAll(andLeft.bool().filter());
                } else {
                    andCompound.add(andLeft);
                }
                Query andRight = getQuery(cond.getRight());
                if (andRight.isBool() && !andRight.bool().filter().isEmpty()) {
                    andCompound.addAll(andRight.bool().filter());
                } else {
                    andCompound.add(andRight);
                }
                query = new Query.Builder().bool(QueryBuilders.bool().filter(andCompound).build()).build();
            }
            case OR -> {
                List<Query> orCompound = new ArrayList<>();
                Query orLeft = getQuery(cond.getLeft());
                if (orLeft.isDisMax()) {
                    orCompound.addAll(orLeft.disMax().queries());
                } else {
                    orCompound.add(orLeft);
                }
                Query orRight = getQuery(cond.getRight());
                if (orRight.isDisMax()) {
                    orCompound.addAll(orRight.disMax().queries());
                } else {
                    orCompound.add(orRight);
                }
                query = new Query.Builder().disMax(QueryBuilders.disMax().queries(orCompound).build()).build();
            }
            default -> {
            }
        }

        return query;
    }

    @Override
    protected CheckResult<AnyCond> check(final AnyCond cond, final Field field, final Set<String> relationshipsFields) {
        CheckResult<AnyCond> checked = super.check(cond, field, relationshipsFields);

        // Manage difference between external id attribute and internal _id
        if ("id".equals(checked.cond().getSchema())) {
            checked.cond().setSchema("_id");
        }
        if ("id".equals(checked.schema().getKey())) {
            checked.schema().setKey("_id");
        }

        return checked;
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
                        new Query.Builder().exists(QueryBuilders.exists().field(schema.getKey()).build())
                                .build()).build()).build();
                break;

            case ILIKE:
                query = new Query.Builder().wildcard(QueryBuilders.wildcard().
                        field(schema.getKey()).value(cond.getExpression().replace('%', '*').replace("\\_", "_")).
                        caseInsensitive(true).build()).build();
                break;

            case LIKE:
                query = new Query.Builder().wildcard(QueryBuilders.wildcard().
                        field(schema.getKey()).value(cond.getExpression().replace('%', '*').replace("\\_", "_")).
                        caseInsensitive(false).build()).build();
                break;

            case IEQ:
                query = new Query.Builder().term(QueryBuilders.term().
                        field(schema.getKey()).value(FieldValue.of(cond.getExpression())).caseInsensitive(true).
                        build()).build();
                break;

            case EQ:
                FieldValue fieldValue = switch (value) {
                    case Double aDouble ->
                        FieldValue.of(aDouble);
                    case Long aLong ->
                        FieldValue.of(aLong);
                    case Boolean aBoolean ->
                        FieldValue.of(aBoolean);
                    default ->
                        FieldValue.of(value.toString());
                };
                query = new Query.Builder().term(QueryBuilders.term().
                        field(schema.getKey()).value(fieldValue).caseInsensitive(false).build()).
                        build();
                break;

            case GE:
                query = new Query.Builder().range(QueryBuilders.range().
                        field(schema.getKey()).gte(JsonData.of(value)).build()).
                        build();
                break;

            case GT:
                query = new Query.Builder().range(QueryBuilders.range().
                        field(schema.getKey()).gt(JsonData.of(value)).build()).
                        build();
                break;

            case LE:
                query = new Query.Builder().range(QueryBuilders.range().
                        field(schema.getKey()).lte(JsonData.of(value)).build()).
                        build();
                break;

            case LT:
                query = new Query.Builder().range(QueryBuilders.range().
                        field(schema.getKey()).lt(JsonData.of(value)).build()).
                        build();
                break;

            default:
                break;
        }

        return query;
    }

    protected Query getQuery(final AttrCond cond) {
        CheckResult<AttrCond> checked = check(cond);
        return fillAttrQuery(checked.schema(), checked.value(), cond);
    }

    protected Query getQuery(final AnyCond cond) {
        CheckResult<AnyCond> checked = check(
                cond,
                realmUtils.getField(cond.getSchema()).
                        orElseThrow(() -> new IllegalArgumentException("Invalid schema " + cond.getSchema())),
                RELATIONSHIP_FIELDS);
        return fillAttrQuery(checked.schema(), checked.value(), checked.cond());
    }

    protected Query getQueryForCustomConds(final SearchCond cond) {
        return null;
    }

    @Override
    protected long doCount(final Set<String> bases, final SearchCond searchCond) {
        CountRequest request = new CountRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                query(buildDescendantsQuery(bases, searchCond)).
                build();
        LOG.debug("Count request: {}", request);

        try {
            return client.count(request).count();
        } catch (Exception e) {
            LOG.error("While counting in Elasticsearch with request {}", request, e);
            return 0;
        }
    }

    protected List<SortOptions> sortBuilders(final Stream<Sort.Order> orderBy) {
        List<SortOptions> options = new ArrayList<>();
        orderBy.forEach(clause -> {
            String sortName = null;

            String fieldName = clause.getProperty();
            // Cannot sort by internal _id
            if (!ID_PROPS.contains(fieldName)) {
                Field anyField = realmUtils.getField(fieldName).orElse(null);
                if (anyField == null) {
                    PlainSchema schema = plainSchemaDAO.findById(fieldName).orElse(null);
                    if (schema != null) {
                        sortName = fieldName;
                    }
                } else {
                    sortName = fieldName;
                }
            }

            if (sortName == null) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                if ("fullPath".equals(sortName)) {
                    options.addAll(FULLPATH_SORT_OPTIONS);
                } else {
                    options.add(new SortOptions.Builder().field(
                            new FieldSort.Builder().
                                    field(sortName).
                                    order(clause.getDirection() == Sort.Direction.ASC ? SortOrder.Asc : SortOrder.Desc).
                                    build()).
                            build());
                }
            }
        });
        return options.isEmpty() ? FULLPATH_SORT_OPTIONS : options;
    }

    @Override
    protected List<Realm> doSearch(final Set<String> bases, final SearchCond searchCond, final Pageable pageable) {
        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(buildDescendantsQuery(bases, searchCond)).
                from(pageable.isUnpaged() ? 0 : pageable.getPageSize() * pageable.getPageNumber()).
                size(pageable.isUnpaged() ? indexMaxResultWindow : pageable.getPageSize()).
                sort(sortBuilders(pageable.getSort().get())).
                fields(List.of()).source(new SourceConfig.Builder().fetch(false).build()).
                build();
        LOG.debug("Search request: {}", request);

        List<String> result = List.of();
        try {
            result = client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    toList();
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch with request {}", request, e);
        }

        return result.stream().map(realmDAO::findById).
                flatMap(Optional::stream).map(Realm.class::cast).toList();
    }

    @Override
    public List<Realm> findDescendants(final String base, final String prefix) {
        Query descendantsQuery = buildDescendantsQuery(Set.of(base), null);
        Query query;
        if (prefix == null) {
            query = descendantsQuery;
        } else {
            Query prefixQuery = new Query.Builder().disMax(QueryBuilders.disMax().queries(
                    new Query.Builder().term(QueryBuilders.term().
                            field("fullPath").value(FieldValue.of(prefix)).caseInsensitive(false).build()).build(),
                    new Query.Builder().prefix(QueryBuilders.prefix().
                            field("fullPath").value(SyncopeConstants.ROOT_REALM.equals(prefix) ? "/" : prefix + "/").
                            build()).build()).build()).build();
            query = new Query.Builder().bool(QueryBuilders.bool().filter(
                    descendantsQuery, prefixQuery).build()).
                    build();
        }

        SearchRequest request = new SearchRequest.Builder().
                index(OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain())).
                searchType(SearchType.QueryThenFetch).
                query(query).
                from(0).
                size(indexMaxResultWindow).
                sort(FULLPATH_SORT_OPTIONS).
                fields(List.of()).source(new SourceConfig.Builder().fetch(false).build()).
                build();
        LOG.debug("Search request: {}", request);

        List<String> result = List.of();
        try {
            result = client.search(request, Void.class).hits().hits().stream().
                    map(Hit::id).
                    toList();
        } catch (Exception e) {
            LOG.error("While searching in Elasticsearch with request {}", request, e);
        }
        return result.stream().map(realmDAO::findById).flatMap(Optional::stream).map(Realm.class::cast).toList();
    }
}
