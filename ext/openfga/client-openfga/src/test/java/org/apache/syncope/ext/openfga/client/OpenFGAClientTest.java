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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.ext.openfga.client.model.AuthorizationModel;
import org.apache.syncope.ext.openfga.client.model.CheckRequest;
import org.apache.syncope.ext.openfga.client.model.CheckRequestTupleKey;
import org.apache.syncope.ext.openfga.client.model.ReadRequest;
import org.apache.syncope.ext.openfga.client.model.ReadRequestTupleKey;
import org.apache.syncope.ext.openfga.client.model.Tuple;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.openfga.OpenFGAContainer;

@SpringJUnitConfig(classes = OpenFGAClientTestContext.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpenFGAClientTest {

    static final OpenFGAContainer OPENFGA_CONTAINER = new OpenFGAContainer("openfga/openfga:v1");

    static {
        OPENFGA_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("OPENFGA_API_URL", () -> OPENFGA_CONTAINER.getHttpEndpoint());
    }

    protected static AnyType mockAnyType() {
        AnyType anyType = mock(AnyType.class);
        when(anyType.getKind()).thenReturn(AnyTypeKind.ANY_OBJECT);
        when(anyType.getKey()).thenReturn(OpenFGAClientTestContext.NEW_TYPE_KEY);
        return anyType;
    }

    protected static EntityLifecycleEvent<Entity> mockNewAnyTypeEvent() {
        @SuppressWarnings("unchecked")
        EntityLifecycleEvent<Entity> event = mock(EntityLifecycleEvent.class);
        when(event.getEntity()).thenAnswer(ic -> mockAnyType());
        when(event.getDomain()).thenReturn(SyncopeConstants.MASTER_DOMAIN);
        when(event.getType()).thenReturn(SyncDeltaType.CREATE);
        return event;
    }

    protected static RelationshipType mockRelationshipType() {
        RelationshipType relType = mock(RelationshipType.class);
        when(relType.getKey()).thenReturn("newrelationship");

        when(relType.getLeftEndAnyType()).thenAnswer(ic -> {
            AnyType anyType = mock(AnyType.class);
            when(anyType.getKind()).thenReturn(AnyTypeKind.GROUP);
            when(anyType.getKey()).thenReturn(AnyTypeKind.GROUP.name());
            return anyType;
        });

        when(relType.getRightEndAnyType()).thenAnswer(ic -> mockAnyType());

        return relType;
    }

    protected static EntityLifecycleEvent<Entity> mockNewRelationshipTypeEvent() {
        @SuppressWarnings("unchecked")
        EntityLifecycleEvent<Entity> event = mock(EntityLifecycleEvent.class);
        when(event.getEntity()).thenAnswer(ic -> mockRelationshipType());
        when(event.getDomain()).thenReturn(SyncopeConstants.MASTER_DOMAIN);
        when(event.getType()).thenReturn(SyncDeltaType.CREATE);
        return event;
    }

    @Autowired
    private OpenFGAClientFactory clientFactory;

    @Autowired
    private OpenFGAStoreManager storeManager;

    @Test
    @Order(1)
    void predefined() throws Exception {
        OpenFGAClient client = clientFactory.get(SyncopeConstants.MASTER_DOMAIN);

        String store = client.getStore().getName();
        assertEquals(SyncopeConstants.MASTER_DOMAIN, store);

        AuthorizationModel authorizationModel = client.readLatestAuthorizationModel();
        assertNotNull(authorizationModel.getId());
        assertEquals(2, authorizationModel.getTypeDefinitions().size());
    }

    @Test
    @Order(2)
    void anyTypeCRUD() throws Exception {
        EntityLifecycleEvent<Entity> event = mockNewAnyTypeEvent();

        storeManager.entity(event);

        await().atMost(Duration.ofSeconds(5L)).pollInterval(Duration.ofSeconds(1L)).until(() -> {
            AuthorizationModel authorizationModel = clientFactory.get(SyncopeConstants.MASTER_DOMAIN).
                    readLatestAuthorizationModel();
            return authorizationModel.getTypeDefinitions().size() == 3;
        });

        when(event.getType()).thenReturn(SyncDeltaType.DELETE);

        storeManager.entity(event);

        await().atMost(Duration.ofSeconds(5L)).pollInterval(Duration.ofSeconds(1L)).until(() -> {
            AuthorizationModel authorizationModel = clientFactory.get(SyncopeConstants.MASTER_DOMAIN).
                    readLatestAuthorizationModel();
            return authorizationModel.getTypeDefinitions().size() == 2;
        });
    }

    @Test
    @Order(3)
    void relationshipTypeCRUD() throws IOException {
        storeManager.entity(mockNewAnyTypeEvent());

        EntityLifecycleEvent<Entity> event = mockNewRelationshipTypeEvent();

        storeManager.entity(event);

        await().atMost(Duration.ofSeconds(5L)).pollInterval(Duration.ofSeconds(1L)).until(() -> {
            AuthorizationModel authorizationModel = clientFactory.get(SyncopeConstants.MASTER_DOMAIN).
                    readLatestAuthorizationModel();

            return authorizationModel.getTypeDefinitions().stream().
                    anyMatch(type -> OpenFGAClientTestContext.NEW_TYPE_KEY.equals(type.getType())
                    && !type.getRelations().isEmpty());
        });

        when(event.getType()).thenReturn(SyncDeltaType.DELETE);

        storeManager.entity(event);

        await().atMost(Duration.ofSeconds(5L)).pollInterval(Duration.ofSeconds(1L)).until(() -> {
            AuthorizationModel authorizationModel = clientFactory.get(SyncopeConstants.MASTER_DOMAIN).
                    readLatestAuthorizationModel();

            return authorizationModel.getTypeDefinitions().stream().
                    anyMatch(type -> OpenFGAClientTestContext.NEW_TYPE_KEY.equals(type.getType())
                    && type.getRelations().isEmpty());
        });
    }

    @Test
    @Order(4)
    void relatable() throws Exception {
        storeManager.entity(mockNewAnyTypeEvent());

        storeManager.entity(mockNewRelationshipTypeEvent());

        @SuppressWarnings("unchecked")
        EntityLifecycleEvent<Entity> event = mock(EntityLifecycleEvent.class);
        when(event.getEntity()).thenAnswer(ic -> {
            AnyObject anyObject = mock(AnyObject.class);
            when(anyObject.getType()).thenAnswer(ic2 -> {
                AnyType anyType = mock(AnyType.class);
                when(anyType.getKey()).thenReturn(OpenFGAClientTestContext.NEW_TYPE_KEY);
                return anyType;
            });
            when(anyObject.getKey()).thenReturn("anyObjectKey");

            Group group = mock(Group.class);
            when(group.getType()).thenAnswer(ic2 -> {
                AnyType anyType = mock(AnyType.class);
                when(anyType.getKey()).thenReturn(AnyTypeKind.GROUP.name());
                return anyType;
            });
            when(group.getName()).thenReturn("groupKey");
            when(group.getRelationships()).thenAnswer(ic2 -> {
                GRelationship relationship = mock(GRelationship.class);
                when(relationship.getKey()).thenReturn("newrelationship");
                when(relationship.getRightEnd()).thenReturn(anyObject);
                when(relationship.getType()).thenAnswer(ic3 -> mockRelationshipType());

                return List.of(relationship);
            });

            return group;
        });
        when(event.getDomain()).thenReturn(SyncopeConstants.MASTER_DOMAIN);
        when(event.getType()).thenReturn(SyncDeltaType.CREATE);

        storeManager.entity(event);

        OpenFGAClient client = clientFactory.get(SyncopeConstants.MASTER_DOMAIN);

        // the tuple was created
        List<Tuple> tuples = await().atMost(Duration.ofSeconds(5L)).pollInterval(Duration.ofSeconds(1L)).
                until(() -> {
                    ReadRequest request = new ReadRequest();
                    request.setTupleKey(new ReadRequestTupleKey().
                            user("GROUP:groupKey").
                            _object(OpenFGAClientTestContext.NEW_TYPE_KEY + ":"));
                    try {
                        return client.read(request).getTuples();
                    } catch (Exception e) {
                        return List.of();
                    }
                }, list -> !list.isEmpty());

        assertEquals(1, tuples.size());
        assertEquals("GROUP:groupKey", tuples.get(0).getKey().getUser());
        assertEquals("newrelationship", tuples.get(0).getKey().getRelation());
        assertEquals("NEWTYPE:anyObjectKey", tuples.get(0).getKey().getObject());
        assertNull(tuples.get(0).getKey().getCondition());

        // check is succssful
        assertTrue(client.check(new CheckRequest().tupleKey(new CheckRequestTupleKey().
                user(tuples.get(0).getKey().getUser()).
                relation(tuples.get(0).getKey().getRelation()).
                _object(tuples.get(0).getKey().getObject()))).getAllowed());
    }
}
