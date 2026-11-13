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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.ext.openfga.client.model.AuthorizationModel;
import org.apache.syncope.ext.openfga.client.model.Metadata;
import org.apache.syncope.ext.openfga.client.model.ReadRequest;
import org.apache.syncope.ext.openfga.client.model.ReadResponse;
import org.apache.syncope.ext.openfga.client.model.RelationMetadata;
import org.apache.syncope.ext.openfga.client.model.RelationReference;
import org.apache.syncope.ext.openfga.client.model.Tuple;
import org.apache.syncope.ext.openfga.client.model.TupleKey;
import org.apache.syncope.ext.openfga.client.model.TupleKeyWithoutCondition;
import org.apache.syncope.ext.openfga.client.model.TypeDefinition;
import org.apache.syncope.ext.openfga.client.model.Userset;
import org.apache.syncope.ext.openfga.client.model.WriteAuthorizationModelRequest;
import org.apache.syncope.ext.openfga.client.model.WriteAuthorizationModelResponse;
import org.apache.syncope.ext.openfga.client.model.WriteRequest;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenFGAStoreManagerTest {

    private static AnyType anyType(final String key) {
        AnyType anyType = mock(AnyType.class);
        lenient().when(anyType.getKey()).thenReturn(key);
        return anyType;
    }

    private static RelationshipType relationshipType(
            final String key, final AnyType left, final AnyType right) {

        RelationshipType relationshipType = mock(RelationshipType.class);
        lenient().when(relationshipType.getKey()).thenReturn(key);
        lenient().when(relationshipType.getLeftEndAnyType()).thenReturn(left);
        lenient().when(relationshipType.getRightEndAnyType()).thenReturn(right);
        return relationshipType;
    }

    private static AuthorizationModel authorizationModel(final TypeDefinition... typeDefinitions) {
        return new AuthorizationModel().
                id("model1").
                schemaVersion(OpenFGAClientFactory.SCHEMA_VERSION).
                typeDefinitions(new ArrayList<>(List.of(typeDefinitions)));
    }

    private static TypeDefinition typeDefinition(final AuthorizationModel model, final String type) {
        return model.getTypeDefinitions().stream().
                filter(definition -> type.equals(definition.getType())).
                findFirst().
                orElseThrow();
    }

    private static Tuple tuple(final String user, final String relation, final String object) {
        return new Tuple().key(new TupleKey().user(user).relation(relation)._object(object));
    }

    @Mock
    private OpenFGAClientFactory clientFactory;

    @Mock
    private RelationshipTypeDAO relationshipTypeDAO;

    @Mock
    private OpenFGAClient openfgaClient;

    private OpenFGAStoreManager storeManager;

    @BeforeEach
    void setUp() {
        storeManager = new OpenFGAStoreManager(clientFactory, relationshipTypeDAO);
    }

    @Test
    void staticHelpers() {
        AnyType userType = anyType(AnyTypeKind.USER.name());
        AnyType groupType = anyType(AnyTypeKind.GROUP.name());
        AnyType anyObjectType = anyType("NEWTYPE");

        User user = mock(User.class);
        when(user.getType()).thenReturn(userType);
        when(user.getUsername()).thenReturn("rossi");

        Group group = mock(Group.class);
        when(group.getType()).thenReturn(groupType);
        when(group.getName()).thenReturn("admin");

        AnyObject anyObject = mock(AnyObject.class);
        when(anyObject.getType()).thenReturn(anyObjectType);
        when(anyObject.getKey()).thenReturn("anyObjectKey");

        assertThat(OpenFGAStoreManager.type(anyObjectType)).isEqualTo("NEWTYPE:");
        assertThat(OpenFGAStoreManager.anyMembership(anyObjectType)).isEqualTo("NEWTYPE_member");
        assertThat(OpenFGAStoreManager.id(user)).isEqualTo("USER:rossi");
        assertThat(OpenFGAStoreManager.id(group)).isEqualTo("GROUP:admin");
        assertThat(OpenFGAStoreManager.id(anyObject)).isEqualTo("NEWTYPE:anyObjectKey");
    }

    @Test
    void anyTypeCreated() throws Exception {
        AnyType anyObjectType = anyType("NEWTYPE");
        AuthorizationModel authModel = authorizationModel(
                new TypeDefinition().type(AnyTypeKind.USER.name()).relations(new HashMap<>()),
                new TypeDefinition().type(AnyTypeKind.GROUP.name()).relations(new HashMap<>()).
                        metadata(new Metadata()));
        when(openfgaClient.readLatestAuthorizationModel()).thenReturn(authModel);
        when(openfgaClient.writeAuthorizationModel(any(WriteAuthorizationModelRequest.class))).
                thenReturn(new WriteAuthorizationModelResponse().authorizationModelId("model2"));

        storeManager.handle(openfgaClient, SyncDeltaType.CREATE, anyObjectType);

        assertThat(authModel.getTypeDefinitions()).
                extracting(TypeDefinition::getType).
                containsExactly(AnyTypeKind.USER.name(), AnyTypeKind.GROUP.name(), "NEWTYPE");

        TypeDefinition group = typeDefinition(authModel, AnyTypeKind.GROUP.name());
        assertThat(group.getRelations()).containsKey("NEWTYPE_member");
        assertThat(group.getMetadata().getRelations()).containsKey("NEWTYPE_member");
        assertThat(group.getMetadata().getRelations().get("NEWTYPE_member").getDirectlyRelatedUserTypes()).
                extracting(RelationReference::getType).
                containsExactly("NEWTYPE");

        ArgumentCaptor<WriteAuthorizationModelRequest> requestCaptor =
                ArgumentCaptor.forClass(WriteAuthorizationModelRequest.class);
        verify(openfgaClient).writeAuthorizationModel(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getSchemaVersion()).isEqualTo(OpenFGAClientFactory.SCHEMA_VERSION);
        assertThat(requestCaptor.getValue().getTypeDefinitions()).hasSize(3);
        verify(openfgaClient).setAuthorizationModelId("model2");
    }

    @Test
    void anyTypeDeleted() throws Exception {
        AnyType anyObjectType = anyType("NEWTYPE");
        AuthorizationModel authModel = authorizationModel(
                new TypeDefinition().type(AnyTypeKind.USER.name()).relations(new HashMap<>()),
                new TypeDefinition().type(AnyTypeKind.GROUP.name()).relations(new HashMap<>()).
                        putRelationsItem(OpenFGAClientFactory.MEMBERSHIP_RELATION, new Userset()._this(Map.of())).
                        metadata(new Metadata().
                                putRelationsItem(OpenFGAClientFactory.MEMBERSHIP_RELATION, new RelationMetadata()).
                                putRelationsItem("NEWTYPE_member", new RelationMetadata())),
                new TypeDefinition().type("NEWTYPE").relations(new HashMap<>()));
        when(openfgaClient.readLatestAuthorizationModel()).thenReturn(authModel);
        when(openfgaClient.writeAuthorizationModel(any(WriteAuthorizationModelRequest.class))).
                thenReturn(new WriteAuthorizationModelResponse().authorizationModelId("model2"));

        storeManager.handle(openfgaClient, SyncDeltaType.DELETE, anyObjectType);

        assertThat(authModel.getTypeDefinitions()).
                extracting(TypeDefinition::getType).
                containsExactly(AnyTypeKind.USER.name(), AnyTypeKind.GROUP.name());

        TypeDefinition group = typeDefinition(authModel, AnyTypeKind.GROUP.name());
        assertThat(group.getRelations()).doesNotContainKey("NEWTYPE_member");
        assertThat(group.getMetadata().getRelations()).doesNotContainKey("NEWTYPE_member");
        verify(openfgaClient).setAuthorizationModelId("model2");
    }

    @Test
    void anyTypeUpdated() throws Exception {
        AnyType anyObjectType = mock(AnyType.class);
        AuthorizationModel authModel = authorizationModel(
                new TypeDefinition().type(AnyTypeKind.USER.name()).relations(new HashMap<>()),
                new TypeDefinition().type(AnyTypeKind.GROUP.name()).relations(new HashMap<>()).
                        metadata(new Metadata()));
        when(openfgaClient.readLatestAuthorizationModel()).thenReturn(authModel);

        storeManager.handle(openfgaClient, SyncDeltaType.UPDATE, anyObjectType);

        assertThat(authModel.getTypeDefinitions()).hasSize(2);
        verify(openfgaClient, never()).writeAuthorizationModel(any(WriteAuthorizationModelRequest.class));
    }

    @Test
    void selfRelationshipTypeSkipped() throws Exception {
        AnyType groupType = anyType(AnyTypeKind.GROUP.name());
        RelationshipType relationshipType = relationshipType(
                "membership", groupType, groupType);

        storeManager.handle(openfgaClient, SyncDeltaType.CREATE, relationshipType);

        verifyNoInteractions(openfgaClient);
    }

    @Test
    void relationshipTypeCreated() throws Exception {
        AnyType groupType = anyType(AnyTypeKind.GROUP.name());
        AnyType anyObjectType = anyType("NEWTYPE");
        RelationshipType relationshipType = relationshipType("rel", groupType, anyObjectType);

        AuthorizationModel authModel = authorizationModel(
                new TypeDefinition().type(AnyTypeKind.GROUP.name()).relations(new HashMap<>()).
                        metadata(new Metadata()),
                new TypeDefinition().type("NEWTYPE").relations(new HashMap<>()).
                        metadata(new Metadata()));
        when(openfgaClient.readLatestAuthorizationModel()).thenReturn(authModel);
        when(openfgaClient.writeAuthorizationModel(any(WriteAuthorizationModelRequest.class))).
                thenReturn(new WriteAuthorizationModelResponse().authorizationModelId("model2"));

        storeManager.handle(openfgaClient, SyncDeltaType.CREATE, relationshipType);

        TypeDefinition right = typeDefinition(authModel, "NEWTYPE");
        assertThat(right.getRelations()).containsKey("rel");
        assertThat(right.getMetadata().getRelations().get("rel").getDirectlyRelatedUserTypes()).
                extracting(RelationReference::getType).
                containsExactly(AnyTypeKind.GROUP.name());

        assertThat(authModel.getTypeDefinitions()).
                extracting(TypeDefinition::getType).
                containsExactlyInAnyOrder(AnyTypeKind.GROUP.name(), "NEWTYPE");
        verify(openfgaClient).setAuthorizationModelId("model2");
    }

    @Test
    void relationshipTypeDeleted() throws Exception {
        AnyType groupType = anyType(AnyTypeKind.GROUP.name());
        AnyType anyObjectType = anyType("NEWTYPE");
        RelationshipType relationshipType = relationshipType("rel", groupType, anyObjectType);

        AuthorizationModel authModel = authorizationModel(
                new TypeDefinition().type(AnyTypeKind.GROUP.name()).relations(new HashMap<>()).
                        metadata(new Metadata()),
                new TypeDefinition().type("NEWTYPE").
                        putRelationsItem("rel", new Userset()._this(Map.of())).
                        metadata(new Metadata().putRelationsItem("rel", new RelationMetadata())));
        when(openfgaClient.readLatestAuthorizationModel()).thenReturn(authModel);
        when(openfgaClient.writeAuthorizationModel(any(WriteAuthorizationModelRequest.class))).
                thenReturn(new WriteAuthorizationModelResponse().authorizationModelId("model2"));

        storeManager.handle(openfgaClient, SyncDeltaType.DELETE, relationshipType);

        TypeDefinition right = typeDefinition(authModel, "NEWTYPE");
        assertThat(right.getRelations()).doesNotContainKey("rel");
        assertThat(right.getMetadata().getRelations()).doesNotContainKey("rel");
        verify(openfgaClient).setAuthorizationModelId("model2");
    }

    @Test
    void userTuplesWritten() throws Exception {
        AnyType userType = anyType(AnyTypeKind.USER.name());
        AnyType groupType = anyType(AnyTypeKind.GROUP.name());
        AnyType anyObjectType = anyType("NEWTYPE");

        User user = mock(User.class);
        when(user.getType()).thenReturn(userType);
        when(user.getUsername()).thenReturn("rossi");

        Group group = mock(Group.class);
        when(group.getType()).thenReturn(groupType);
        when(group.getName()).thenReturn("admin");
        UMembership membership = mock(UMembership.class);
        when(membership.getRightEnd()).thenReturn(group);
        doReturn(List.of(membership)).when(user).getMemberships();

        AnyObject anyObject = mock(AnyObject.class);
        when(anyObject.getType()).thenReturn(anyObjectType);
        when(anyObject.getKey()).thenReturn("anyObjectKey");

        RelationshipType relationshipType = relationshipType("rel", userType, anyObjectType);
        URelationship relationship = mock(URelationship.class);
        when(relationship.getType()).thenReturn(relationshipType);
        when(relationship.getRightEnd()).thenReturn(anyObject);
        doReturn(List.of(relationship)).when(user).getRelationships();

        doReturn(List.of(relationshipType)).when(relationshipTypeDAO).findByLeftEndAnyType(userType);
        when(openfgaClient.read(any(ReadRequest.class))).thenReturn(new ReadResponse().tuples(List.of()));
        when(openfgaClient.readLatestAuthorizationModel()).thenReturn(new AuthorizationModel().id("model1"));

        storeManager.handle(openfgaClient, SyncDeltaType.CREATE, user);

        ArgumentCaptor<WriteRequest> requestCaptor = ArgumentCaptor.forClass(WriteRequest.class);
        verify(openfgaClient).write(requestCaptor.capture());

        WriteRequest request = requestCaptor.getValue();
        assertThat(request.getDeletes()).isNull();

        List<TupleKey> written = request.getWrites().getTupleKeys();
        assertThat(written).hasSize(2);
        assertThat(written.get(0).getUser()).isEqualTo("USER:rossi");
        assertThat(written.get(0).getRelation()).isEqualTo(OpenFGAClientFactory.MEMBERSHIP_RELATION);
        assertThat(written.get(0).getObject()).isEqualTo("GROUP:admin");
        assertThat(written.get(1).getUser()).isEqualTo("USER:rossi");
        assertThat(written.get(1).getRelation()).isEqualTo("rel");
        assertThat(written.get(1).getObject()).isEqualTo("NEWTYPE:anyObjectKey");

        verify(openfgaClient).setAuthorizationModelId("model1");
    }

    @Test
    void userTuplesDeleted() throws Exception {
        AnyType userType = anyType(AnyTypeKind.USER.name());
        AnyType anyObjectType = anyType("NEWTYPE");

        User user = mock(User.class);
        when(user.getType()).thenReturn(userType);
        when(user.getUsername()).thenReturn("rossi");

        RelationshipType relationshipType = relationshipType("rel", userType, anyObjectType);
        doReturn(List.of(relationshipType)).when(relationshipTypeDAO).findByLeftEndAnyType(userType);
        when(openfgaClient.read(any(ReadRequest.class))).thenAnswer(invocation -> {
            ReadRequest request = invocation.getArgument(0);
            return new ReadResponse().tuples(List.of(request.getTupleKey().getObject().startsWith("GROUP:")
                    ? tuple("USER:rossi", OpenFGAClientFactory.MEMBERSHIP_RELATION, "GROUP:admin")
                    : tuple("USER:rossi", "rel", "NEWTYPE:anyObjectKey")));
        });
        when(openfgaClient.readLatestAuthorizationModel()).thenReturn(new AuthorizationModel().id("model1"));

        storeManager.handle(openfgaClient, SyncDeltaType.DELETE, user);

        ArgumentCaptor<WriteRequest> requestCaptor = ArgumentCaptor.forClass(WriteRequest.class);
        verify(openfgaClient).write(requestCaptor.capture());

        WriteRequest request = requestCaptor.getValue();
        assertThat(request.getWrites()).isNull();

        List<TupleKeyWithoutCondition> deleted = request.getDeletes().getTupleKeys();
        assertThat(deleted).
                extracting(key -> key.getUser() + "|" + key.getRelation() + "|" + key.getObject()).
                containsExactlyInAnyOrder(
                        "USER:rossi|" + OpenFGAClientFactory.MEMBERSHIP_RELATION + "|GROUP:admin",
                        "USER:rossi|rel|NEWTYPE:anyObjectKey");

        verify(openfgaClient).setAuthorizationModelId("model1");
    }

    @Test
    void userTuplesUnchanged() throws Exception {
        AnyType userType = anyType(AnyTypeKind.USER.name());
        AnyType groupType = anyType(AnyTypeKind.GROUP.name());
        AnyType anyObjectType = anyType("NEWTYPE");

        User user = mock(User.class);
        when(user.getType()).thenReturn(userType);
        when(user.getUsername()).thenReturn("rossi");

        Group group = mock(Group.class);
        when(group.getType()).thenReturn(groupType);
        when(group.getName()).thenReturn("admin");
        UMembership membership = mock(UMembership.class);
        when(membership.getRightEnd()).thenReturn(group);
        doReturn(List.of(membership)).when(user).getMemberships();

        AnyObject anyObject = mock(AnyObject.class);
        when(anyObject.getType()).thenReturn(anyObjectType);
        when(anyObject.getKey()).thenReturn("anyObjectKey");

        RelationshipType relationshipType = relationshipType("rel", userType, anyObjectType);
        URelationship relationship = mock(URelationship.class);
        when(relationship.getType()).thenReturn(relationshipType);
        when(relationship.getRightEnd()).thenReturn(anyObject);
        doReturn(List.of(relationship)).when(user).getRelationships();

        doReturn(List.of(relationshipType)).when(relationshipTypeDAO).findByLeftEndAnyType(userType);
        when(openfgaClient.read(any(ReadRequest.class))).thenReturn(new ReadResponse().tuples(List.of(
                tuple("USER:rossi", OpenFGAClientFactory.MEMBERSHIP_RELATION, "GROUP:admin"),
                tuple("USER:rossi", "rel", "NEWTYPE:anyObjectKey"))));

        storeManager.handle(openfgaClient, SyncDeltaType.CREATE, user);

        verify(openfgaClient, never()).write(any(WriteRequest.class));
        verify(openfgaClient, never()).readLatestAuthorizationModel();
    }
}
