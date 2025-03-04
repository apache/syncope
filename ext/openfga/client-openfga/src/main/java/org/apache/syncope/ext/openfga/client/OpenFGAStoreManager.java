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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.ext.openfga.client.model.AuthorizationModel;
import org.apache.syncope.ext.openfga.client.model.Metadata;
import org.apache.syncope.ext.openfga.client.model.ReadRequest;
import org.apache.syncope.ext.openfga.client.model.ReadRequestTupleKey;
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
import org.apache.syncope.ext.openfga.client.model.WriteRequestDeletes;
import org.apache.syncope.ext.openfga.client.model.WriteRequestWrites;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

public class OpenFGAStoreManager {

    protected static final Logger LOG = LoggerFactory.getLogger(OpenFGAStoreManager.class);

    protected static String anyMembership(final AnyType anyType) {
        return anyType.getKey() + "_" + OpenFGAClientFactory.MEMBERSHIP_RELATION;
    }

    protected static String type(final String anyType) {
        return anyType + ":";
    }

    protected static String type(final AnyType anyType) {
        return type(anyType.getKey());
    }

    protected static String id(final Any any) {
        return type(any.getType())
                + (any instanceof User user
                        ? user.getUsername()
                        : any instanceof Group group
                                ? group.getName()
                                : any.getKey());
    }

    protected final OpenFGAClientFactory clientFactory;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    public OpenFGAStoreManager(
            final OpenFGAClientFactory clientFactory,
            final RelationshipTypeDAO relationshipTypeDAO) {

        this.clientFactory = clientFactory;
        this.relationshipTypeDAO = relationshipTypeDAO;
    }

    public void handle(
            final OpenFGAClient openfgaClient,
            final SyncDeltaType eventType,
            final AnyType anyType) throws ApiException {

        AuthorizationModel authModel = openfgaClient.readLatestAuthorizationModel();
        openfgaClient.setAuthorizationModelId(authModel.getId());

        Optional<TypeDefinition> group = authModel.getTypeDefinitions().stream().
                filter(type -> AnyTypeKind.GROUP.name().equals(type.getType())).
                findFirst();

        boolean changed = false;
        switch (eventType) {
            case CREATE:
                changed = authModel.getTypeDefinitions().
                        add(new TypeDefinition().type(anyType.getKey()).relations(Map.of()));
                if (changed) {
                    group.ifPresent(g -> {
                        g.putRelationsItem(anyMembership(anyType), new Userset()._this(Map.of()));
                        g.getMetadata().putRelationsItem(
                                anyMembership(anyType),
                                new RelationMetadata().addDirectlyRelatedUserTypesItem(
                                        new RelationReference().type(anyType.getKey())));
                    });
                }
                break;

            case DELETE:
                changed = authModel.getTypeDefinitions().
                        removeIf(typeDefinition -> anyType.getKey().equals(typeDefinition.getType()));
                if (changed) {
                    group.ifPresent(g -> {
                        g.getRelations().remove(anyMembership(anyType));
                        g.getMetadata().getRelations().remove(anyMembership(anyType));
                    });
                }
                break;

            default:
        }

        if (changed) {
            LOG.debug("Authorization Model is to update on {} {}", eventType, anyType);

            WriteAuthorizationModelResponse response = openfgaClient.writeAuthorizationModel(
                    new WriteAuthorizationModelRequest().
                            schemaVersion(authModel.getSchemaVersion()).
                            typeDefinitions(authModel.getTypeDefinitions()).
                            conditions(authModel.getConditions()));
            openfgaClient.setAuthorizationModelId(response.getAuthorizationModelId());
        } else {
            LOG.debug("Authorization Model not to update on {} {}", eventType, anyType);
        }
    }

    public void handle(
            final OpenFGAClient openfgaClient,
            final SyncDeltaType eventType,
            final RelationshipType relationshipType) throws ApiException {

        // skip self relationships, not supported by OpenFGA
        if (relationshipType.getLeftEndAnyType().equals(relationshipType.getRightEndAnyType())) {
            return;
        }

        AuthorizationModel authModel = openfgaClient.readLatestAuthorizationModel();
        openfgaClient.setAuthorizationModelId(authModel.getId());

        TypeDefinition left = authModel.getTypeDefinitions().stream().
                filter(type -> relationshipType.getLeftEndAnyType().getKey().equals(type.getType())).
                findFirst().
                orElseGet(() -> new TypeDefinition().type(relationshipType.getLeftEndAnyType().getKey()));
        TypeDefinition right = authModel.getTypeDefinitions().stream().
                filter(type -> relationshipType.getRightEndAnyType().getKey().equals(type.getType())).
                findFirst().
                orElseGet(() -> new TypeDefinition().type(relationshipType.getRightEndAnyType().getKey()));

        authModel.getTypeDefinitions().removeIf(
                type -> left.getType().equals(type.getType()) || right.getType().equals(type.getType()));

        authModel.addTypeDefinitionsItem(left);
        authModel.addTypeDefinitionsItem(right);

        boolean changed = false;
        switch (eventType) {
            case CREATE:
                right.putRelationsItem(relationshipType.getKey(), new Userset()._this(Map.of()));
                right.metadata(new Metadata().putRelationsItem(
                        relationshipType.getKey(),
                        new RelationMetadata().addDirectlyRelatedUserTypesItem(
                                new RelationReference().type(left.getType()))));
                changed = true;
                break;

            case DELETE:
                changed = right.getRelations().remove(relationshipType.getKey()) != null
                        && right.getMetadata().getRelations().remove(relationshipType.getKey()) != null;
                break;

            default:
        }

        if (changed) {
            LOG.debug("Authorization Model is to update on {} {}", eventType, relationshipType);

            WriteAuthorizationModelResponse response = openfgaClient.writeAuthorizationModel(
                    new WriteAuthorizationModelRequest().
                            schemaVersion(authModel.getSchemaVersion()).
                            typeDefinitions(authModel.getTypeDefinitions()).
                            conditions(authModel.getConditions()));
            openfgaClient.setAuthorizationModelId(response.getAuthorizationModelId());
        } else {
            LOG.debug("Authorization Model not to update on {} {}", eventType, relationshipType);
        }
    }

    public void handle(
            final OpenFGAClient openfgaClient,
            final SyncDeltaType eventType,
            final Any any) throws ApiException {

        List<ReadRequest> readRequests = new ArrayList<>();
        if (any instanceof Groupable) {
            ReadRequest request = new ReadRequest();
            request.setTupleKey(new ReadRequestTupleKey().
                    user(id(any))._object(type(AnyTypeKind.GROUP.name())));
            readRequests.add(request);
        }
        for (RelationshipType relationshipType : relationshipTypeDAO.findByLeftEndAnyType(any.getType())) {
            ReadRequest request = new ReadRequest();
            request.setTupleKey(new ReadRequestTupleKey().
                    user(id(any))._object(type(relationshipType.getRightEndAnyType())));
            readRequests.add(request);
        }

        List<TupleKey> current = new ArrayList<>();
        for (ReadRequest request : readRequests) {
            current.addAll(openfgaClient.read(request).getTuples().stream().map(Tuple::getKey).toList());
        }

        List<TupleKey> next;
        if (eventType == SyncDeltaType.DELETE) {
            next = List.of();
        } else {
            next = new ArrayList<>();
            if (any instanceof Groupable<?, ?, ?, ?> groupable) {
                next.addAll(groupable.getMemberships().stream().map(m -> new TupleKey().
                        user(id(groupable)).
                        relation(OpenFGAClientFactory.MEMBERSHIP_RELATION).
                        _object(id(m.getRightEnd()))).
                        toList());
            }
            if (any instanceof Relatable<?, ?, ?> relatable) {
                next.addAll(relatable.getRelationships().stream().
                        filter(r -> !r.getType().getRightEndAnyType().equals(any.getType())).
                        map(r -> new TupleKey().
                        user(id(relatable)).
                        relation(r.getType().getKey()).
                        _object(id(r.getRightEnd()))).
                        toList());
            }
        }

        List<TupleKey> toWrite = next.stream().
                filter(tk -> !current.contains(tk)).
                map(tk -> new TupleKey().
                user(tk.getUser()).
                relation(tk.getRelation())
                ._object(tk.getObject())).
                toList();
        List<TupleKeyWithoutCondition> toDelete = current.stream().
                filter(tk -> !next.contains(tk)).
                map(tk -> new TupleKeyWithoutCondition().
                user(tk.getUser()).
                relation(tk.getRelation())
                ._object(tk.getObject())).
                toList();

        if (!toWrite.isEmpty() || !toDelete.isEmpty()) {
            LOG.debug("Tuples are to update on {} {}", eventType, any);

            AuthorizationModel authModel = openfgaClient.readLatestAuthorizationModel();
            openfgaClient.setAuthorizationModelId(authModel.getId());

            WriteRequest request = new WriteRequest();
            if (!toWrite.isEmpty()) {
                request.writes(new WriteRequestWrites().tupleKeys(toWrite));
            }
            if (!toDelete.isEmpty()) {
                request.deletes(new WriteRequestDeletes().tupleKeys(toDelete));
            }

            openfgaClient.write(request);
        } else {
            LOG.debug("Tuples not to update on {} {}", eventType, any);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener
    public void entity(final EntityLifecycleEvent<Entity> event) throws IOException {
        LOG.debug("About to handle {}", event);

        OpenFGAClient openfgaClient = clientFactory.get(event.getDomain());

        try {
            if (event.getEntity() instanceof final AnyType anyType && anyType.getKind() == AnyTypeKind.ANY_OBJECT) {
                handle(openfgaClient, event.getType(), anyType);
            } else if (event.getEntity() instanceof final RelationshipType relationshipType) {
                handle(openfgaClient, event.getType(), relationshipType);
            } else if (event.getEntity() instanceof final Any any) {
                handle(openfgaClient, event.getType(), any);
            }
        } catch (Exception e) {
            LOG.error("While handling event {}", event, e);
        }
    }
}
