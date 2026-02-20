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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DisMaxQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchRealmSearchDAOTest {

    @Mock
    private RealmDAO realmDAO;

    @Mock
    private PlainSchemaDAO plainSchemaDAO;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private PlainAttrValidationManager validator;

    @Mock
    private ElasticsearchClient client;

    private ElasticsearchRealmSearchDAO searchDAO;

    @BeforeEach
    protected void setupSearchDAO() {
        doReturn(JPARealm.class).when(entityFactory).realmClass();
        searchDAO = new ElasticsearchRealmSearchDAO(
                realmDAO,
                plainSchemaDAO,
                entityFactory,
                validator,
                client,
                10000);
    }

    @Test
    public void query4anyCond() {
        when(entityFactory.newEntity(PlainSchema.class)).thenReturn(new JPAPlainSchema());
        doAnswer(ic -> {
            PlainAttrValue value = ic.getArgument(2);
            value.setStringValue(ic.getArgument(1));
            return null;
        }).when(validator).validate(any(PlainSchema.class), anyString(), any(PlainAttrValue.class));

        AnyCond name = new AnyCond(AttrCond.Type.EQ);
        name.setSchema("name");
        name.setExpression("two");

        Query query = searchDAO.getQuery(SearchCond.of(name));
        assertThat(
                new Query.Builder().term(QueryBuilders.term().
                        field("name").value(FieldValue.of("two")).caseInsensitive(false).build()).build()).
                usingRecursiveComparison().isEqualTo(query);
        verifyNoInteractions(plainSchemaDAO);
    }

    @Test
    public void query4attrCond() {
        PlainSchema aLong = new JPAPlainSchema();
        aLong.setKey("aLong");
        aLong.setType(AttrSchemaType.Long);
        doReturn(Optional.of(aLong)).when(plainSchemaDAO).findById("aLong");
        doAnswer(ic -> {
            PlainAttrValue value = ic.getArgument(2);
            value.setLongValue(Long.valueOf(ic.getArgument(1)));
            return null;
        }).when(validator).validate(any(PlainSchema.class), anyString(), any(PlainAttrValue.class));

        AttrCond attrEq = new AttrCond(AttrCond.Type.EQ);
        attrEq.setSchema("aLong");
        attrEq.setExpression("42");

        Query query = searchDAO.getQuery(SearchCond.of(attrEq));
        assertEquals(Query.Kind.Term, query._kind());
        assertEquals("aLong", query.term().field());
        assertEquals(Boolean.FALSE, query.term().caseInsensitive());
        assertEquals("42", query.term().value().anyValue().toString());
    }

    @Test
    public void query4attrCondNullChecks() {
        PlainSchema aLong = new JPAPlainSchema();
        aLong.setKey("aLong");
        aLong.setType(AttrSchemaType.Long);
        doReturn(Optional.of(aLong)).when(plainSchemaDAO).findById("aLong");

        AttrCond isNull = new AttrCond(AttrCond.Type.ISNULL);
        isNull.setSchema("aLong");

        Query query = searchDAO.getQuery(SearchCond.of(isNull));
        assertThat(
                new Query.Builder().bool(QueryBuilders.bool().mustNot(
                        new Query.Builder().exists(QueryBuilders.exists().field("aLong").build())
                                .build()).build()).build()).
                usingRecursiveComparison().isEqualTo(query);

        AttrCond isNotNull = new AttrCond(AttrCond.Type.ISNOTNULL);
        isNotNull.setSchema("aLong");

        query = searchDAO.getQuery(SearchCond.of(isNotNull));
        assertThat(
                new Query.Builder().exists(QueryBuilders.exists().field("aLong").build()).build()).
                usingRecursiveComparison().isEqualTo(query);
    }

    @Test
    public void descendantsQueryWithAndOrFilters() {
        when(entityFactory.newEntity(PlainSchema.class)).thenReturn(new JPAPlainSchema());
        doAnswer(ic -> {
            PlainAttrValue value = ic.getArgument(2);
            value.setStringValue(ic.getArgument(1));
            return null;
        }).when(validator).validate(any(PlainSchema.class), anyString(), any(PlainAttrValue.class));

        AnyCond nameTwo = new AnyCond(AttrCond.Type.EQ);
        nameTwo.setSchema("name");
        nameTwo.setExpression("two");

        AnyCond nameOdd = new AnyCond(AttrCond.Type.EQ);
        nameOdd.setSchema("name");
        nameOdd.setExpression("odd");

        Query query = searchDAO.buildDescendantsQuery(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.or(SearchCond.of(nameTwo), SearchCond.of(nameOdd)));

        assertEquals(Query.Kind.Bool, query._kind());
        assertEquals(2, ((BoolQuery) query._get()).filter().size());
        Query right = ((BoolQuery) query._get()).filter().get(1);
        assertEquals(Query.Kind.DisMax, right._kind());
        assertEquals(2, ((DisMaxQuery) right._get()).queries().size());

        assertThat(
                new Query.Builder().bool(QueryBuilders.bool().
                        filter(new Query.Builder().disMax(QueryBuilders.disMax().
                                queries(new Query.Builder().term(QueryBuilders.term().
                                        field("fullPath").value(FieldValue.of(SyncopeConstants.ROOT_REALM)).
                                        caseInsensitive(false).build()).build()).
                                queries(new Query.Builder().regexp(QueryBuilders.regexp().
                                        field("fullPath").value("/.*").build()).build()).
                                build()).build()).
                        filter(new Query.Builder().disMax(QueryBuilders.disMax().
                                queries(new Query.Builder().term(QueryBuilders.term().
                                        field("name").value(FieldValue.of("two")).caseInsensitive(false).
                                        build()).build()).
                                queries(new Query.Builder().term(QueryBuilders.term().
                                        field("name").value(FieldValue.of("odd")).caseInsensitive(false).
                                        build()).build()).
                                build()).build()).
                        build()).build()).
                usingRecursiveComparison().isEqualTo(query);
    }
}
