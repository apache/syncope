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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private DynRealmDAO dynRealmDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private EntityFactory entityFactory;

    @Mock
    private AnyUtilsFactory anyUtilsFactory;

    @Mock
    private ElasticsearchUtils elasticsearchUtils;

    @InjectMocks
    private ElasticsearchAnySearchDAO searchDAO;

    @Test
    public void getAdminRealmsFilter_realm() {
        // 1. mock
        Realm root = mock(Realm.class);
        when(root.getFullPath()).thenReturn(SyncopeConstants.ROOT_REALM);

        when(realmDAO.findByFullPath(SyncopeConstants.ROOT_REALM)).thenReturn(root);
        when(realmDAO.findDescendants(root)).thenReturn(List.of(root));

        // 2. test
        Set<String> adminRealms = Set.of(SyncopeConstants.ROOT_REALM);
        Triple<Optional<QueryBuilder>, Set<String>, Set<String>> filter =
                searchDAO.getAdminRealmsFilter(AnyTypeKind.USER, adminRealms);
        assertEquals(
                QueryBuilders.disMaxQuery().add(QueryBuilders.termQuery("realm", SyncopeConstants.ROOT_REALM)),
                filter.getLeft().get());
        assertEquals(Set.of(), filter.getMiddle());
        assertEquals(Set.of(), filter.getRight());
    }

    @Test
    public void getAdminRealmsFilter_dynRealm() {
        // 1. mock
        DynRealm dyn = mock(DynRealm.class);
        when(dyn.getKey()).thenReturn("dyn");

        when(dynRealmDAO.find("dyn")).thenReturn(dyn);

        // 2. test
        Set<String> adminRealms = Set.of("dyn");
        Triple<Optional<QueryBuilder>, Set<String>, Set<String>> filter =
                searchDAO.getAdminRealmsFilter(AnyTypeKind.USER, adminRealms);
        assertFalse(filter.getLeft().isPresent());
        assertEquals(Set.of("dyn"), filter.getMiddle());
        assertEquals(Set.of(), filter.getRight());
    }

    @Test
    public void getAdminRealmsFilter_groupOwner() {
        Set<String> adminRealms = Set.of(RealmUtils.getGroupOwnerRealm("/any", "groupKey"));
        Triple<Optional<QueryBuilder>, Set<String>, Set<String>> filter =
                searchDAO.getAdminRealmsFilter(AnyTypeKind.USER, adminRealms);
        assertFalse(filter.getLeft().isPresent());
        assertEquals(Set.of(), filter.getMiddle());
        assertEquals(Set.of("groupKey"), filter.getRight());
    }

    @Test
    public void searchRequest_groupOwner() throws IOException {
        // 1. mock
        AnyUtils anyUtils = mock(AnyUtils.class);
        when(anyUtils.getField("id")).thenReturn(ReflectionUtils.findField(JPAUser.class, "id"));
        when(anyUtils.newPlainAttrValue()).thenReturn(new JPAUPlainAttrValue());

        when(anyUtilsFactory.getInstance(AnyTypeKind.USER)).thenReturn(anyUtils);

        when(entityFactory.newEntity(PlainSchema.class)).thenReturn(new JPAPlainSchema());

        when(groupDAO.findKey("groupKey")).thenReturn("groupKey");

        try (MockedStatic<ElasticsearchUtils> utils = Mockito.mockStatic(ElasticsearchUtils.class)) {
            utils.when(() -> ElasticsearchUtils.getContextDomainName(
                    SyncopeConstants.MASTER_DOMAIN, AnyTypeKind.USER)).thenReturn("master_user");

            // 2. test
            Set<String> adminRealms = Set.of(RealmUtils.getGroupOwnerRealm("/any", "groupKey"));

            AnyCond anyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
            anyCond.setSchema("id");

            SearchRequest searchRequest = searchDAO.searchRequest(
                    adminRealms, SearchCond.getLeaf(anyCond), AnyTypeKind.USER, 1, 10, List.of());

            assertEquals(
                    QueryBuilders.boolQuery().
                            must(QueryBuilders.existsQuery("id")).
                            must(QueryBuilders.termQuery("memberships", "groupKey")),
                    searchRequest.source().query());
        }
    }
}
