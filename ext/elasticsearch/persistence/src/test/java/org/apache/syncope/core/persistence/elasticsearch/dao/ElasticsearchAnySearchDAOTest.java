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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.SearchType;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchAnySearchDAOTest {

    @Mock
    private RealmDAO realmDAO;

    @Mock
    private RealmSearchDAO realmSearchDAO;

    @Mock
    private DynRealmDAO dynRealmDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private AnyUtilsFactory anyUtilsFactory;

    @Mock
    private PlainAttrValidationManager validator;

    private ElasticsearchAnySearchDAO searchDAO;

    @BeforeEach
    protected void setupSearchDAO() {
        searchDAO = new ElasticsearchAnySearchDAO(
                realmSearchDAO,
                dynRealmDAO,
                null,
                groupDAO,
                null,
                null,
                entityFactory,
                anyUtilsFactory,
                validator,
                null,
                10000);
    }

    @Test
    public void getAdminRealmsFilter4realm() throws IOException {
        // 1. mock
        Realm root = mock(Realm.class);
        when(root.getFullPath()).thenReturn(SyncopeConstants.ROOT_REALM);

        when(realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM)).thenAnswer(ic -> Optional.of(root));
        when(realmSearchDAO.findDescendants(eq(SyncopeConstants.ROOT_REALM), anyString())).
                thenReturn(List.of("rootKey"));

        // 2. test
        Set<String> adminRealms = Set.of(SyncopeConstants.ROOT_REALM);
        Triple<Optional<Query>, Set<String>, Set<String>> filter =
                searchDAO.getAdminRealmsFilter(root, true, adminRealms, AnyTypeKind.USER);

        assertThat(new Query.Builder().disMax(QueryBuilders.disMax().queries(
                new Query.Builder().term(QueryBuilders.term().field("realm").value("rootKey").build()).
                        build()).build()).build()).
                usingRecursiveComparison().isEqualTo(filter.getLeft().get());
        assertEquals(Set.of(), filter.getMiddle());
        assertEquals(Set.of(), filter.getRight());
    }

    @Test
    public void getAdminRealmsFilter4dynRealm() {
        // 1. mock
        DynRealm dyn = mock(DynRealm.class);
        when(dyn.getKey()).thenReturn("dyn");

        when(dynRealmDAO.findById("dyn")).thenAnswer(ic -> Optional.of(dyn));

        // 2. test
        Set<String> adminRealms = Set.of("dyn");
        Triple<Optional<Query>, Set<String>, Set<String>> filter =
                searchDAO.getAdminRealmsFilter(realmDAO.getRoot(), true, adminRealms, AnyTypeKind.USER);
        assertFalse(filter.getLeft().isPresent());
        assertEquals(Set.of("dyn"), filter.getMiddle());
        assertEquals(Set.of(), filter.getRight());
    }

    @Test
    public void getAdminRealmsFilter4groupOwner() {
        Set<String> adminRealms = Set.of(RealmUtils.getGroupOwnerRealm("/any", "groupKey"));
        Triple<Optional<Query>, Set<String>, Set<String>> filter =
                searchDAO.getAdminRealmsFilter(realmDAO.getRoot(), true, adminRealms, AnyTypeKind.USER);
        assertFalse(filter.getLeft().isPresent());
        assertEquals(Set.of(), filter.getMiddle());
        assertEquals(Set.of("groupKey"), filter.getRight());
    }

    @Test
    public void searchRequest4groupOwner() throws IOException {
        // 1. mock
        AnyUtils anyUtils = mock(AnyUtils.class);
        when(anyUtils.getField("key")).thenReturn(Optional.of(ReflectionUtils.findField(JPAUser.class, "id")));

        when(anyUtilsFactory.getInstance(AnyTypeKind.USER)).thenReturn(anyUtils);

        when(entityFactory.newEntity(PlainSchema.class)).thenReturn(new JPAPlainSchema());

        when(groupDAO.findKey("groupKey")).thenReturn(Optional.of("groupKey"));

        try (MockedStatic<ElasticsearchUtils> utils = Mockito.mockStatic(ElasticsearchUtils.class)) {
            utils.when(() -> ElasticsearchUtils.getAnyIndex(
                    SyncopeConstants.MASTER_DOMAIN, AnyTypeKind.USER)).thenReturn("master_user");

            // 2. test
            Set<String> adminRealms = Set.of(RealmUtils.getGroupOwnerRealm("/any", "groupKey"));

            AnyCond anyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
            anyCond.setSchema("key");

            SearchRequest request = new SearchRequest.Builder().
                    index(ElasticsearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER)).
                    searchType(SearchType.QueryThenFetch).
                    query(searchDAO.getQuery(mock(Realm.class), true,
                            adminRealms, SearchCond.of(anyCond), AnyTypeKind.USER)).
                    from(1).
                    size(10).
                    build();

            assertThat(
                    new Query.Builder().bool(QueryBuilders.bool().
                            must(new Query.Builder().exists(QueryBuilders.exists().field("id").build()).build()).
                            must(new Query.Builder().term(QueryBuilders.term().field("memberships").value("groupKey").
                                    build()).build()).build()).build()).
                    usingRecursiveComparison().
                    isEqualTo(request.query());
        }
    }

    @Test
    public void issueSYNCOPE1725() throws IOException {
        // 1. mock
        AnyUtils anyUtils = mock(AnyUtils.class);
        when(anyUtils.getField("key")).thenReturn(Optional.of(ReflectionUtils.findField(JPAUser.class, "id")));

        when(anyUtilsFactory.getInstance(AnyTypeKind.USER)).thenReturn(anyUtils);

        when(entityFactory.newEntity(PlainSchema.class)).thenReturn(new JPAPlainSchema());

        doAnswer(ic -> {
            PlainAttrValue value = ic.getArgument(2);
            value.setStringValue(ic.getArgument(1));
            return null;
        }).when(validator).validate(any(PlainSchema.class), anyString(), any(PlainAttrValue.class));

        AnyCond cond1 = new AnyCond(AttrCond.Type.EQ);
        cond1.setSchema("key");
        cond1.setExpression("1");

        AnyCond cond2 = new AnyCond(AttrCond.Type.EQ);
        cond2.setSchema("key");
        cond2.setExpression("2");

        AnyCond cond3 = new AnyCond(AttrCond.Type.EQ);
        cond3.setSchema("key");
        cond3.setExpression("3");

        AnyCond cond4 = new AnyCond(AttrCond.Type.EQ);
        cond4.setSchema("key");
        cond4.setExpression("4");

        AnyCond cond5 = new AnyCond(AttrCond.Type.EQ);
        cond5.setSchema("key");
        cond5.setExpression("5");

        AnyCond cond6 = new AnyCond(AttrCond.Type.EQ);
        cond6.setSchema("key");
        cond6.setExpression("6");

        try (MockedStatic<ElasticsearchUtils> utils = Mockito.mockStatic(ElasticsearchUtils.class)) {
            utils.when(() -> ElasticsearchUtils.getAnyIndex(
                    SyncopeConstants.MASTER_DOMAIN, AnyTypeKind.USER)).thenReturn("master_user");

            Query query = searchDAO.getQuery(SearchCond.and(
                    SearchCond.of(cond1),
                    SearchCond.of(cond2),
                    SearchCond.of(cond3),
                    SearchCond.of(cond4),
                    SearchCond.of(cond5),
                    SearchCond.of(cond6)),
                    AnyTypeKind.USER);
            assertEquals(Query.Kind.Bool, query._kind());
            assertEquals(6, ((BoolQuery) query._get()).must().size());
            assertThat(
                    new Query.Builder().bool(QueryBuilders.bool().
                            must(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("1").build()).build()).
                            must(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("2").build()).build()).
                            must(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("3").build()).build()).
                            must(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("4").build()).build()).
                            must(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("5").build()).build()).
                            must(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("6").build()).build()).
                            build()).build()).
                    usingRecursiveComparison().isEqualTo(query);

            query = searchDAO.getQuery(SearchCond.or(
                    SearchCond.of(cond1),
                    SearchCond.of(cond2),
                    SearchCond.of(cond3),
                    SearchCond.of(cond4),
                    SearchCond.of(cond5),
                    SearchCond.of(cond6)),
                    AnyTypeKind.USER);
            assertEquals(Query.Kind.DisMax, query._kind());
            assertEquals(6, ((DisMaxQuery) query._get()).queries().size());
            assertThat(
                    new Query.Builder().disMax(QueryBuilders.disMax().
                            queries(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("1").build()).build()).
                            queries(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("2").build()).build()).
                            queries(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("3").build()).build()).
                            queries(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("4").build()).build()).
                            queries(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("5").build()).build()).
                            queries(new Query.Builder().term(
                                    QueryBuilders.term().field("id").value("6").build()).build()).
                            build()).build()).
                    usingRecursiveComparison().isEqualTo(query);

            query = searchDAO.getQuery(SearchCond.and(
                    SearchCond.or(
                            SearchCond.of(cond1),
                            SearchCond.of(cond2),
                            SearchCond.of(cond3)),
                    SearchCond.or(
                            SearchCond.of(cond4),
                            SearchCond.of(cond5),
                            SearchCond.of(cond6))),
                    AnyTypeKind.USER);
            assertEquals(Query.Kind.Bool, query._kind());
            assertEquals(2, ((BoolQuery) query._get()).must().size());
            Query left = ((BoolQuery) query._get()).must().getFirst();
            assertEquals(Query.Kind.DisMax, left._kind());
            assertEquals(3, ((DisMaxQuery) left._get()).queries().size());
            Query right = ((BoolQuery) query._get()).must().get(1);
            assertEquals(Query.Kind.DisMax, right._kind());
            assertEquals(3, ((DisMaxQuery) right._get()).queries().size());
            assertThat(
                    new Query.Builder().bool(QueryBuilders.bool().
                            must(new Query.Builder().disMax(QueryBuilders.disMax().
                                    queries(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("1").build()).build()).
                                    queries(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("2").build()).build()).
                                    queries(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("3").build()).build()).build()).
                                    build()).
                            must(new Query.Builder().disMax(QueryBuilders.disMax().
                                    queries(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("4").build()).build()).
                                    queries(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("5").build()).build()).
                                    queries(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("6").build()).build()).build()).
                                    build()).
                            build()).build()).
                    usingRecursiveComparison().isEqualTo(query);

            query = searchDAO.getQuery(SearchCond.or(
                    SearchCond.and(
                            SearchCond.of(cond1),
                            SearchCond.of(cond2),
                            SearchCond.of(cond3)),
                    SearchCond.and(
                            SearchCond.of(cond4),
                            SearchCond.of(cond5),
                            SearchCond.of(cond6))),
                    AnyTypeKind.USER);
            assertEquals(Query.Kind.DisMax, query._kind());
            assertEquals(2, ((DisMaxQuery) query._get()).queries().size());
            left = ((DisMaxQuery) query._get()).queries().getFirst();
            assertEquals(Query.Kind.Bool, left._kind());
            assertEquals(3, ((BoolQuery) left._get()).must().size());
            right = ((DisMaxQuery) query._get()).queries().get(1);
            assertEquals(Query.Kind.Bool, right._kind());
            assertEquals(3, ((BoolQuery) right._get()).must().size());
            assertThat(
                    new Query.Builder().disMax(QueryBuilders.disMax().
                            queries(new Query.Builder().bool(QueryBuilders.bool().
                                    must(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("1").build()).build()).
                                    must(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("2").build()).build()).
                                    must(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("3").build()).build()).build()).
                                    build()).
                            queries(new Query.Builder().bool(QueryBuilders.bool().
                                    must(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("4").build()).build()).
                                    must(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("5").build()).build()).
                                    must(new Query.Builder().term(
                                            QueryBuilders.term().field("id").value("6").build()).build()).build()).
                                    build()).
                            build()).build()).
                    usingRecursiveComparison().isEqualTo(query);
        }
    }
}
