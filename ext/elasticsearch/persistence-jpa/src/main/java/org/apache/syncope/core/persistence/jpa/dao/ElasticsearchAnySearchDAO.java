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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
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
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.springframework.util.CollectionUtils;

/**
 * Search engine implementation for users, groups and any objects, based on Elasticsearch.
 */
public class ElasticsearchAnySearchDAO extends AbstractAnySearchDAO {

    protected static final char[] ELASTICSEARCH_REGEX_CHARS = new char[] {
        '.', '?', '+', '*', '|', '{', '}', '[', ']', '(', ')', '"', '\\', '&' };

    protected static String escapeForLikeRegex(final char c) {
        StringBuilder output = new StringBuilder();

        if (ArrayUtils.contains(ELASTICSEARCH_REGEX_CHARS, c)) {
            output.append('\\');
        }

        output.append(c);

        return output.toString();
    }

    protected final ElasticsearchClient client;

    protected final ElasticsearchUtils elasticsearchUtils;

    public ElasticsearchAnySearchDAO(
            final RealmDAO realmDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final ElasticsearchClient client,
            final ElasticsearchUtils elasticsearchUtils) {

        super(realmDAO, dynRealmDAO, userDAO, groupDAO, anyObjectDAO, schemaDAO, entityFactory, anyUtilsFactory);
        this.client = client;
        this.elasticsearchUtils = elasticsearchUtils;
    }

    protected Triple<Optional<Query>, Set<String>, Set<String>> getAdminRealmsFilter(
            final AnyTypeKind kind, final Set<String> adminRealms) {

        Set<String> dynRealmKeys = new HashSet<>();
        Set<String> groupOwners = new HashSet<>();
        List<Query> queries = new ArrayList<>();

        adminRealms.forEach(realmPath -> {
            Optional<Pair<String, String>> goRealm = RealmUtils.parseGroupOwnerRealm(realmPath);
            if (goRealm.isPresent()) {
                groupOwners.add(goRealm.get().getRight());
            } else if (realmPath.startsWith("/")) {
                Realm realm = realmDAO.findByFullPath(realmPath);
                if (realm == null) {
                    SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                    noRealm.getElements().add("Invalid realm specified: " + realmPath);
                    throw noRealm;
                } else {
                    realmDAO.findDescendants(realm).forEach(descendant -> queries.add(
                            new Query.Builder().term(QueryBuilders.term().
                                    field("realm").value(FieldValue.of(descendant.getFullPath())).build()).
                                    build()));
                }
            } else {
                DynRealm dynRealm = dynRealmDAO.find(realmPath);
                if (dynRealm == null) {
                    LOG.warn("Ignoring invalid dynamic realm {}", realmPath);
                } else {
                    dynRealmKeys.add(dynRealm.getKey());
                    queries.add(new Query.Builder().term(QueryBuilders.term().
                            field("dynRealm").value(FieldValue.of(dynRealm.getKey())).build()).
                            build());
                }
            }
        });

        return Triple.of(
                dynRealmKeys.isEmpty() && groupOwners.isEmpty()
                ? Optional.of(new Query.Builder().disMax(QueryBuilders.disMax().queries(queries).build()).build())
                : Optional.empty(),
                dynRealmKeys,
                groupOwners);
    }

    protected Query getQuery(
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        Triple<Optional<Query>, Set<String>, Set<String>> filter = getAdminRealmsFilter(kind, adminRealms);
        Query query;
        if (SyncopeConstants.FULL_ADMIN_REALMS.equals(adminRealms)) {
            query = getQuery(cond, kind);
        } else {
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
    protected int doCount(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        CountRequest request = new CountRequest.Builder().
                index(ElasticsearchUtils.getContextDomainName(AuthContextUtils.getDomain(), kind)).
                query(getQuery(adminRealms, cond, kind)).
                build();
        try {
            return (int) client.count(request).count();
        } catch (IOException e) {
            LOG.error("Search error", e);
            return 0;
        }
    }

    protected List<SortOptions> sortBuilders(
            final AnyTypeKind kind,
            final List<OrderByClause> orderBy) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(kind);

        List<SortOptions> options = new ArrayList<>();
        orderBy.forEach(clause -> {
            String sortName = null;

            // Manage difference among external key attribute and internal JPA @Id
            String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

            Field anyField = anyUtils.getField(fieldName);
            if (anyField == null) {
                PlainSchema schema = plainSchemaDAO.find(fieldName);
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
                                order(clause.getDirection() == OrderByClause.Direction.ASC
                                        ? SortOrder.Asc : SortOrder.Desc).
                                build()).
                        build());
            }
        });
        return options;
    }

    @Override
    protected <T extends Any<?>> List<T> doSearch(
            final Set<String> adminRealms,
            final SearchCond cond,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderBy,
            final AnyTypeKind kind) {

        SearchRequest request = new SearchRequest.Builder().
                index(ElasticsearchUtils.getContextDomainName(AuthContextUtils.getDomain(), kind)).
                searchType(SearchType.QueryThenFetch).
                query(getQuery(adminRealms, cond, kind)).
                from(itemsPerPage * (page <= 0 ? 0 : page - 1)).
                size(itemsPerPage < 0 ? elasticsearchUtils.getIndexMaxResultWindow() : itemsPerPage).
                sort(sortBuilders(kind, orderBy)).
                build();

        @SuppressWarnings("rawtypes")
        List<Hit<Map>> esResult = null;
        try {
            esResult = client.search(request, Map.class).hits().hits();
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
                query = cond.getLeaf(AnyTypeCond.class).
                        filter(leaf -> AnyTypeKind.ANY_OBJECT == kind).
                        map(this::getQuery).
                        orElse(null);

                if (query == null) {
                    query = cond.getLeaf(RelationshipTypeCond.class).
                            filter(leaf -> AnyTypeKind.GROUP != kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(RelationshipCond.class).
                            filter(leaf -> AnyTypeKind.GROUP != kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(MembershipCond.class).
                            filter(leaf -> AnyTypeKind.GROUP != kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(MemberCond.class).
                            filter(leaf -> AnyTypeKind.GROUP == kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(AssignableCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(RoleCond.class).
                            filter(leaf -> AnyTypeKind.USER == kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(PrivilegeCond.class).
                            filter(leaf -> AnyTypeKind.USER == kind).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(DynRealmCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    query = cond.getLeaf(ResourceCond.class).
                            map(this::getQuery).
                            orElse(null);
                }

                if (query == null) {
                    Optional<AnyCond> anyCond = cond.getLeaf(AnyCond.class);
                    if (anyCond.isPresent()) {
                        query = getQuery(anyCond.get(), kind);
                    } else {
                        query = cond.getLeaf(AttrCond.class).
                                map(leaf -> getQuery(leaf, kind)).
                                orElse(null);
                    }
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
                query = new Query.Builder().bool(QueryBuilders.bool().
                        must(getQuery(cond.getLeft(), kind)).must(getQuery(cond.getRight(), kind)).build()).
                        build();
                break;

            case OR:
                query = new Query.Builder().disMax(QueryBuilders.disMax().
                        queries(getQuery(cond.getLeft(), kind), getQuery(cond.getRight(), kind)).build()).
                        build();
                break;

            default:
        }

        return query;
    }

    protected Query getQuery(final AnyTypeCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("anyType").value(FieldValue.of(cond.getAnyTypeKey())).build()).
                build();
    }

    protected Query getQuery(final RelationshipTypeCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("relationshipTypes").value(FieldValue.of(cond.getRelationshipTypeKey())).build()).
                build();
    }

    protected Query getQuery(final RelationshipCond cond) {
        String rightAnyObjectKey = check(cond);

        return new Query.Builder().term(QueryBuilders.term().
                field("relationships").value(FieldValue.of(rightAnyObjectKey)).build()).
                build();
    }

    protected Query getQuery(final MembershipCond cond) {
        List<String> groupKeys = check(cond);

        List<Query> membershipQueries = groupKeys.stream().
                map(key -> new Query.Builder().term(QueryBuilders.term().
                field("memberships").value(FieldValue.of(key)).build()).
                build()).collect(Collectors.toList());
        if (membershipQueries.size() == 1) {
            return membershipQueries.get(0);
        }

        return new Query.Builder().disMax(QueryBuilders.disMax().queries(membershipQueries).build()).build();
    }

    protected Query getQuery(final AssignableCond cond) {
        Realm realm = check(cond);

        List<Query> queries = new ArrayList<>();
        if (cond.isFromGroup()) {
            realmDAO.findDescendants(realm).forEach(
                    current -> queries.add(new Query.Builder().term(QueryBuilders.term().
                            field("realm").value(FieldValue.of(current.getFullPath())).build()).
                            build()));
        } else {
            for (Realm current = realm; current.getParent() != null; current = current.getParent()) {
                queries.add(new Query.Builder().term(QueryBuilders.term().
                        field("realm").value(FieldValue.of(current.getFullPath())).build()).
                        build());
            }
            queries.add(new Query.Builder().term(QueryBuilders.term().
                    field("realm").value(FieldValue.of(realmDAO.getRoot().getFullPath())).build()).
                    build());
        }

        return new Query.Builder().disMax(QueryBuilders.disMax().queries(queries).build()).build();
    }

    protected Query getQuery(final RoleCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("roles").value(FieldValue.of(cond.getRole())).build()).
                build();
    }

    protected Query getQuery(final PrivilegeCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("privileges").value(FieldValue.of(cond.getPrivilege())).build()).
                build();
    }

    protected Query getQuery(final DynRealmCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("dynRealms").value(FieldValue.of(cond.getDynRealm())).build()).
                build();
    }

    protected Query getQuery(final MemberCond cond) {
        String memberKey = check(cond);

        return new Query.Builder().term(QueryBuilders.term().
                field("members").value(FieldValue.of(memberKey)).build()).
                build();
    }

    protected Query getQuery(final ResourceCond cond) {
        return new Query.Builder().term(QueryBuilders.term().
                field("resources").value(FieldValue.of(cond.getResourceKey())).build()).
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
                for (char c : cond.getExpression().toLowerCase().toCharArray()) {
                    if (c == '%') {
                        output.append(".*");
                    } else if (Character.isLetter(c)) {
                        output.append('[').
                                append(c).
                                append(Character.toUpperCase(c)).
                                append(']');
                    } else {
                        output.append(escapeForLikeRegex(c));
                    }
                }
                query = new Query.Builder().regexp(QueryBuilders.regexp().
                        field(schema.getKey()).value(output.toString()).build()).build();
                break;

            case LIKE:
                query = new Query.Builder().wildcard(QueryBuilders.wildcard().
                        field(schema.getKey()).value(cond.getExpression().replace('%', '*')).build()).build();
                break;

            case IEQ:
                query = new Query.Builder().match(QueryBuilders.match().
                        field(schema.getKey()).query(FieldValue.of(cond.getExpression().toLowerCase())).build()).
                        build();
                break;

            case EQ:
                FieldValue fieldValue;
                if (value instanceof Double) {
                    fieldValue = FieldValue.of((Double) value);
                } else if (value instanceof Long) {
                    fieldValue = FieldValue.of((Long) value);
                } else if (value instanceof Boolean) {
                    fieldValue = FieldValue.of((Boolean) value);
                } else {
                    fieldValue = FieldValue.of(value.toString());
                }
                query = new Query.Builder().term(QueryBuilders.term().
                        field(schema.getKey()).value(fieldValue).build()).
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
        }

        return query;
    }

    protected Query getQuery(final AttrCond cond, final AnyTypeKind kind) {
        Pair<PlainSchema, PlainAttrValue> checked = check(cond, kind);

        return fillAttrQuery(checked.getLeft(), checked.getRight(), cond);
    }

    protected Query getQuery(final AnyCond cond, final AnyTypeKind kind) {
        if (JAXRSService.PARAM_REALM.equals(cond.getSchema())
                && SyncopeConstants.UUID_PATTERN.matcher(cond.getExpression()).matches()) {

            Realm realm = realmDAO.find(cond.getExpression());
            if (realm == null) {
                throw new IllegalArgumentException("Invalid Realm key: " + cond.getExpression());
            }
            cond.setExpression(realm.getFullPath());
        }

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked = check(cond, kind);

        return fillAttrQuery(checked.getLeft(), checked.getMiddle(), checked.getRight());
    }

    protected Query getQueryForCustomConds(final SearchCond cond, final AnyTypeKind kind) {
        return null;
    }
}
