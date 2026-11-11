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
package org.apache.syncope.ext.openfga.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.ext.openfga.client.model.AuthorizationModel;
import org.apache.syncope.ext.openfga.client.model.CreateStoreRequest;
import org.apache.syncope.ext.openfga.client.model.CreateStoreResponse;
import org.apache.syncope.ext.openfga.client.model.ListStoresResponse;
import org.apache.syncope.ext.openfga.client.model.ReadAuthorizationModelsResponse;
import org.apache.syncope.ext.openfga.client.model.RelationReference;
import org.apache.syncope.ext.openfga.client.model.Store;
import org.apache.syncope.ext.openfga.client.model.TypeDefinition;
import org.apache.syncope.ext.openfga.client.model.WriteAuthorizationModelRequest;
import org.apache.syncope.ext.openfga.client.model.WriteAuthorizationModelResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenFGAClientFactoryTest {

    @Mock
    private ApiClient apiClient;

    private OpenFGAClientFactory newFactory() {
        return new OpenFGAClientFactory(apiClient, new OpenFGAProperties());
    }

    private static TypeDefinition typeDefinition(final WriteAuthorizationModelRequest request, final String type) {
        return request.getTypeDefinitions().stream().
                filter(definition -> type.equals(definition.getType())).
                findFirst().
                orElseThrow();
    }

    @Test
    void storeId() {
        assertThat(OpenFGAClientFactory.storeId("master")).
                isEqualTo("MASTER" + "0".repeat(20)).
                hasSize(26);
    }

    @Test
    void storeIdOfLongDomainIsNotTruncated() {
        String domain = "a".repeat(30);
        assertThat(OpenFGAClientFactory.storeId(domain)).isEqualTo(domain.toUpperCase());
    }

    @Test
    void getBaseUri() {
        when(apiClient.getBaseUri()).thenReturn("http://openfga:8080");

        assertThat(newFactory().getBaseUri()).isEqualTo("http://openfga:8080");
    }

    @Test
    void initAuthorizationModelReusingExistingStoreAndModel() throws Exception {
        OpenFGAClient client = mock(OpenFGAClient.class);
        when(client.listStores()).thenReturn(new ListStoresResponse().stores(List.of(
                new Store().id("otherStore").name("Other"),
                new Store().id("masterStore").name(SyncopeConstants.MASTER_DOMAIN))));
        when(client.readAuthorizationModels()).thenReturn(new ReadAuthorizationModelsResponse().
                authorizationModels(List.of(
                        new AuthorizationModel().id("latest"),
                        new AuthorizationModel().id("older"))));

        newFactory().initAuthorizationModel(client, SyncopeConstants.MASTER_DOMAIN, false);

        verify(client).setStoreId("masterStore");
        verify(client).setAuthorizationModelId("latest");
        verify(client, never()).createStore(any(CreateStoreRequest.class));
        verify(client, never()).writeAuthorizationModel(any(WriteAuthorizationModelRequest.class));
    }

    @Test
    void initAuthorizationModelCreatingStoreAndModel() throws Exception {
        OpenFGAClient client = mock(OpenFGAClient.class);
        when(client.listStores()).thenReturn(new ListStoresResponse().
                stores(List.of(new Store().id("otherStore").name("Other"))));
        when(client.createStore(any(CreateStoreRequest.class))).thenReturn(new CreateStoreResponse().id("newStore"));
        when(client.readAuthorizationModels()).thenReturn(new ReadAuthorizationModelsResponse().
                authorizationModels(List.of()));
        when(client.writeAuthorizationModel(any(WriteAuthorizationModelRequest.class))).
                thenReturn(new WriteAuthorizationModelResponse().authorizationModelId("newModel"));

        newFactory().initAuthorizationModel(client, SyncopeConstants.MASTER_DOMAIN, false);

        verify(client).createStore(any(CreateStoreRequest.class));
        verify(client).setStoreId("newStore");
        verify(client).setAuthorizationModelId("newModel");

        ArgumentCaptor<WriteAuthorizationModelRequest> requestCaptor =
                ArgumentCaptor.forClass(WriteAuthorizationModelRequest.class);
        verify(client).writeAuthorizationModel(requestCaptor.capture());

        WriteAuthorizationModelRequest request = requestCaptor.getValue();
        assertThat(request.getSchemaVersion()).isEqualTo(OpenFGAClientFactory.SCHEMA_VERSION);
        assertThat(request.getTypeDefinitions()).hasSize(2);

        TypeDefinition user = typeDefinition(request, AnyTypeKind.USER.name());
        assertThat(user.getRelations()).isEmpty();

        TypeDefinition group = typeDefinition(request, AnyTypeKind.GROUP.name());
        assertThat(group.getRelations()).containsOnlyKeys(OpenFGAClientFactory.MEMBERSHIP_RELATION);
        assertThat(group.getMetadata().getRelations()).containsOnlyKeys(OpenFGAClientFactory.MEMBERSHIP_RELATION);
        assertThat(group.getMetadata().getRelations().
                get(OpenFGAClientFactory.MEMBERSHIP_RELATION).getDirectlyRelatedUserTypes()).
                extracting(RelationReference::getType).
                containsExactly(AnyTypeKind.USER.name());
    }

    @Test
    void initAuthorizationModelForced() throws Exception {
        OpenFGAClient client = mock(OpenFGAClient.class);
        when(client.listStores()).thenReturn(new ListStoresResponse().stores(List.of(
                new Store().id("masterStore").name(SyncopeConstants.MASTER_DOMAIN))));
        when(client.writeAuthorizationModel(any(WriteAuthorizationModelRequest.class))).
                thenReturn(new WriteAuthorizationModelResponse().authorizationModelId("forcedModel"));

        newFactory().initAuthorizationModel(client, SyncopeConstants.MASTER_DOMAIN, true);

        verify(client).setStoreId("masterStore");
        verify(client).setAuthorizationModelId("forcedModel");
        verify(client, never()).readAuthorizationModels();
    }

    @Test
    void getIsCachedPerDomain() throws Exception {
        try (MockedConstruction<OpenFGAClient> mocked = mockConstruction(OpenFGAClient.class, (client, context) -> {
            when(client.listStores()).thenReturn(new ListStoresResponse().
                    stores(List.of(new Store().id("masterStore").name(SyncopeConstants.MASTER_DOMAIN))));
            when(client.readAuthorizationModels()).thenReturn(new ReadAuthorizationModelsResponse().
                    authorizationModels(List.of(new AuthorizationModel().id("model"))));
        })) {

            OpenFGAClientFactory factory = newFactory();

            OpenFGAClient first = factory.get(SyncopeConstants.MASTER_DOMAIN);
            OpenFGAClient second = factory.get(SyncopeConstants.MASTER_DOMAIN);

            assertThat(second).isSameAs(first);
            assertThat(mocked.constructed()).hasSize(1);
            verify(mocked.constructed().getFirst()).setStoreId("masterStore");
            verify(mocked.constructed().getFirst()).setAuthorizationModelId("model");
        }
    }

    @Test
    void removeEvictsCachedClient() throws Exception {
        try (MockedConstruction<OpenFGAClient> mocked = mockConstruction(OpenFGAClient.class, (client, context) -> {
            when(client.listStores()).thenReturn(new ListStoresResponse().
                    stores(List.of(new Store().id("masterStore").name(SyncopeConstants.MASTER_DOMAIN))));
            when(client.readAuthorizationModels()).thenReturn(new ReadAuthorizationModelsResponse().
                    authorizationModels(List.of(new AuthorizationModel().id("model"))));
        })) {

            OpenFGAClientFactory factory = newFactory();

            OpenFGAClient first = factory.get(SyncopeConstants.MASTER_DOMAIN);
            factory.remove(SyncopeConstants.MASTER_DOMAIN);
            OpenFGAClient second = factory.get(SyncopeConstants.MASTER_DOMAIN);

            assertThat(second).isNotSameAs(first);
            assertThat(mocked.constructed()).hasSize(2);
        }
    }

    @Test
    void getWrapsFailures() throws Exception {
        try (MockedConstruction<OpenFGAClient> mocked = mockConstruction(OpenFGAClient.class, (client, context) -> {
            when(client.listStores()).thenThrow(new ApiException("boom"));
        })) {

            OpenFGAClientFactory factory = newFactory();

            assertThatThrownBy(() -> factory.get(SyncopeConstants.MASTER_DOMAIN)).
                    isInstanceOf(IllegalStateException.class).
                    hasMessage("Could not init OpenFga client for domain " + SyncopeConstants.MASTER_DOMAIN).
                    hasCauseInstanceOf(ApiException.class);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }
}
