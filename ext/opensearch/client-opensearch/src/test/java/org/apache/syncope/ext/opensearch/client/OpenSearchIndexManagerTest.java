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
package org.apache.syncope.ext.opensearch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.mapping.DynamicTemplate;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;

@ExtendWith(MockitoExtension.class)
public class OpenSearchIndexManagerTest {

    @Mock
    private OpenSearchClient client;

    @Mock
    private OpenSearchIndicesClient indices;

    @Mock
    private OpenSearchUtils openSearchUtils;

    private OpenSearchIndexManager indexManager;

    @BeforeEach
    public void setupIndexManager() {
        indexManager = new OpenSearchIndexManager(client, openSearchUtils, 3, 2);
    }

    private static OpenSearchException indexAlreadyExists() {
        return new OpenSearchException(new ErrorResponse.Builder().
                status(400).
                error(new ErrorCause.Builder().
                        type("resource_already_exists_exception").
                        reason("already exists").
                        build()).
                build());
    }

    @Test
    public void existsIndexes() throws Exception {
        when(client.indices()).thenReturn(indices);
        when(indices.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(true));

        assertThat(indexManager.existsAnyIndex("Master", AnyTypeKind.USER)).isTrue();
        assertThat(indexManager.existsRealmIndex("Master")).isTrue();
        assertThat(indexManager.existsAuditIndex("Master")).isTrue();

        ArgumentCaptor<ExistsRequest> captor = ArgumentCaptor.forClass(ExistsRequest.class);
        verify(indices, times(3)).exists(captor.capture());
        assertThat(captor.getAllValues().get(0).index()).containsExactly("master_user");
        assertThat(captor.getAllValues().get(1).index()).containsExactly("master_realm");
        assertThat(captor.getAllValues().get(2).index()).containsExactly("master_audit");
    }

    @Test
    public void defaultSettings() {
        IndexSettings settings = indexManager.defaultSettings();

        assertThat(settings.numberOfShards()).isEqualTo(3);
        assertThat(settings.numberOfReplicas()).isEqualTo(2);
        assertThat(settings.analysis()).isNotNull();
    }

    @Test
    public void defaultMappings() {
        TypeMapping anyMapping = indexManager.defaultAnyMapping();
        assertThat(anyMapping.properties()).isEmpty();
        assertThat(anyMapping.dynamicTemplates()).hasSize(1);
        DynamicTemplate anyTemplate = anyMapping.dynamicTemplates().getFirst().get("strings");
        assertThat(anyTemplate).isNotNull();
        assertThat(anyTemplate.matchMappingType()).isEqualTo("string");
        assertThat(anyTemplate.mapping()._kind()).isEqualTo(Property.Kind.Keyword);

        assertThat(indexManager.defaultRealmMapping().properties()).isEmpty();

        TypeMapping auditMapping = indexManager.defaultAuditMapping();
        assertThat(auditMapping.properties()).containsOnlyKeys("before", "inputs", "output", "throwable");
        assertThat(auditMapping.properties().get("before").text().analyzer()).isEqualTo("standard");
        assertThat(auditMapping.properties().get("inputs").text().analyzer()).isEqualTo("standard");
        assertThat(auditMapping.properties().get("output").text().analyzer()).isEqualTo("standard");
        assertThat(auditMapping.properties().get("throwable").text().analyzer()).isEqualTo("standard");
    }

    @Test
    public void createAnyIndex() throws Exception {
        when(client.indices()).thenReturn(indices);

        indexManager.createAnyIndex(
                "Master", AnyTypeKind.GROUP, indexManager.defaultSettings(), indexManager.defaultAnyMapping());

        ArgumentCaptor<CreateIndexRequest> captor = ArgumentCaptor.forClass(CreateIndexRequest.class);
        verify(indices).create(captor.capture());
        assertThat(captor.getValue().index()).isEqualTo("master_group");
        assertThat(captor.getValue().settings().numberOfShards()).isEqualTo(3);
        assertThat(captor.getValue().mappings().dynamicTemplates()).hasSize(1);
        verify(indices, never()).delete(any(DeleteIndexRequest.class));
    }

    @Test
    public void createAnyIndexAfterFailure() throws Exception {
        when(client.indices()).thenReturn(indices);
        when(indices.create(any(CreateIndexRequest.class))).
                thenThrow(indexAlreadyExists()).
                thenReturn(null);

        indexManager.createAnyIndex(
                "Master", AnyTypeKind.USER, indexManager.defaultSettings(), indexManager.defaultAnyMapping());

        verify(indices, times(2)).create(any(CreateIndexRequest.class));
        ArgumentCaptor<DeleteIndexRequest> captor = ArgumentCaptor.forClass(DeleteIndexRequest.class);
        verify(indices).delete(captor.capture());
        assertThat(captor.getValue().index()).containsExactly("master_user");
    }

    @Test
    public void createRealmIndex() throws Exception {
        when(client.indices()).thenReturn(indices);

        indexManager.createRealmIndex("Master", indexManager.defaultSettings(), indexManager.defaultRealmMapping());

        ArgumentCaptor<CreateIndexRequest> captor = ArgumentCaptor.forClass(CreateIndexRequest.class);
        verify(indices).create(captor.capture());
        assertThat(captor.getValue().index()).isEqualTo("master_realm");
        verify(indices, never()).delete(any(DeleteIndexRequest.class));
    }

    @Test
    public void createAuditIndexAfterFailure() throws Exception {
        when(client.indices()).thenReturn(indices);
        when(indices.create(any(CreateIndexRequest.class))).
                thenThrow(indexAlreadyExists()).
                thenReturn(null);

        indexManager.createAuditIndex("Master", indexManager.defaultSettings(), indexManager.defaultAuditMapping());

        verify(indices, times(2)).create(any(CreateIndexRequest.class));
        ArgumentCaptor<DeleteIndexRequest> captor = ArgumentCaptor.forClass(DeleteIndexRequest.class);
        verify(indices).delete(captor.capture());
        assertThat(captor.getValue().index()).containsExactly("master_audit");
    }

    @Test
    public void removeIndexes() throws Exception {
        when(client.indices()).thenReturn(indices);

        indexManager.removeAnyIndex("Master", AnyTypeKind.ANY_OBJECT);
        indexManager.removeRealmIndex("Master");
        indexManager.removeAuditIndex("Master");

        ArgumentCaptor<DeleteIndexRequest> captor = ArgumentCaptor.forClass(DeleteIndexRequest.class);
        verify(indices, times(3)).delete(captor.capture());
        assertThat(captor.getAllValues().get(0).index()).containsExactly("master_any_object");
        assertThat(captor.getAllValues().get(1).index()).containsExactly("master_realm");
        assertThat(captor.getAllValues().get(2).index()).containsExactly("master_audit");
    }

    @Test
    public void entityAnyDelete() throws Exception {
        AnyType anyType = mock(AnyType.class);
        when(anyType.getKind()).thenReturn(AnyTypeKind.USER);
        User user = mock(User.class);
        when(user.getKey()).thenReturn("user-key");
        when(user.getType()).thenReturn(anyType);

        indexManager.entity(new EntityLifecycleEvent<>(this, SyncDeltaType.DELETE, user, "Master"));

        ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(client).delete(captor.capture());
        assertThat(captor.getValue().index()).isEqualTo("master_user");
        assertThat(captor.getValue().id()).isEqualTo("user-key");
        verifyNoInteractions(openSearchUtils);
    }

    @Test
    public void entityAnyUpdate() throws Exception {
        AnyType anyType = mock(AnyType.class);
        when(anyType.getKind()).thenReturn(AnyTypeKind.GROUP);
        Group group = mock(Group.class);
        when(group.getKey()).thenReturn("group-key");
        when(group.getType()).thenReturn(anyType);
        when(openSearchUtils.document(group)).thenReturn(Map.of("name", "citizens"));

        indexManager.entity(new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, group, "Master"));

        verify(client).index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>argThat(
                request -> "master_group".equals(request.index())
                && "group-key".equals(request.id())
                && Map.of("name", "citizens").equals(request.document())));
        verify(client, never()).delete(any(DeleteRequest.class));
    }

    @Test
    public void entityRealmDelete() throws Exception {
        Realm realm = mock(Realm.class);
        when(realm.getKey()).thenReturn("realm-key");

        indexManager.entity(new EntityLifecycleEvent<>(this, SyncDeltaType.DELETE, realm, "Master"));

        ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(client).delete(captor.capture());
        assertThat(captor.getValue().index()).isEqualTo("master_realm");
        assertThat(captor.getValue().id()).isEqualTo("realm-key");
        assertThat(captor.getValue().refresh()).isEqualTo(Refresh.True);
        verifyNoInteractions(openSearchUtils);
    }

    @Test
    public void entityRealmUpdate() throws Exception {
        Realm realm = mock(Realm.class);
        when(realm.getKey()).thenReturn("realm-key");
        when(openSearchUtils.document(realm)).thenReturn(Map.of("name", "citizens"));

        indexManager.entity(new EntityLifecycleEvent<>(this, SyncDeltaType.CREATE, realm, "Master"));

        verify(client).index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>argThat(
                request -> "master_realm".equals(request.index())
                && "realm-key".equals(request.id())
                && Refresh.True.equals(request.refresh())
                && Map.of("name", "citizens").equals(request.document())));
    }

    @Test
    public void entityNotIndexable() throws Exception {
        Entity entity = mock(Entity.class);

        indexManager.entity(new EntityLifecycleEvent<>(this, SyncDeltaType.UPDATE, entity, "Master"));

        verifyNoInteractions(client, openSearchUtils);
    }

    @Test
    public void audit() throws Exception {
        AuditEvent auditEvent = mock(AuditEvent.class);
        when(openSearchUtils.document(auditEvent)).thenReturn(Map.of("who", "admin"));

        indexManager.audit("Master", auditEvent);

        verify(client).index(ArgumentMatchers.<IndexRequest<Map<String, Object>>>argThat(
                request -> "master_audit".equals(request.index())
                && request.id() == null
                && Map.of("who", "admin").equals(request.document())));
    }
}
