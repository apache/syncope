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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

public class Neo4jAnyChecker extends AnyChecker {

    protected final AnyTypeDAO anyTypeDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final DerSchemaDAO derSchemaDAO;

    public Neo4jAnyChecker(
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO) {

        super(plainSchemaDAO);
        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.derSchemaDAO = derSchemaDAO;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Override
    public <S extends Schema> AllowedSchemas<S> findAllowedSchemas(final Any any, final Class<S> reference) {
        AllowedSchemas<S> result = new AllowedSchemas<>();

        // schemas given by type and aux classes
        Set<AnyTypeClass> anyTypeOwnClasses = new HashSet<>();

        anyTypeDAO.findById(any.getType().getKey()).ifPresent(anyType -> {
            anyTypeOwnClasses.addAll(anyType.getClasses());
            any.getAuxClasses().forEach(atc -> anyTypeClassDAO.findById(atc.getKey()).
                    ifPresent(anyTypeOwnClasses::add));
        });

        anyTypeOwnClasses.forEach(atc -> {
            if (reference.equals(PlainSchema.class)) {
                atc.getPlainSchemas().stream().
                        map(schema -> plainSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.self().add((S) schema));
            } else if (reference.equals(DerSchema.class)) {
                atc.getDerSchemas().stream().
                        map(schema -> derSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.self().add((S) schema));
            }
        });

        // schemas given by group type extensions
        Map<Group, Set<AnyTypeClass>> gTypeExtensionClasses = new HashMap<>();

        switch (any) {
            case User user ->
                user.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().forEach(typeExt -> {
                    Set<AnyTypeClass> classes = new HashSet<>();
                    typeExt.getAuxClasses().forEach(atc -> anyTypeClassDAO.findById(atc.getKey()).
                            ifPresent(classes::add));

                    gTypeExtensionClasses.put(memb.getRightEnd(), classes);
                }));

            case AnyObject anyObject ->
                anyObject.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().stream().
                        filter(typeExt -> any.getType().equals(typeExt.getAnyType())).forEach(typeExt -> {

                    Set<AnyTypeClass> classes = new HashSet<>();
                    typeExt.getAuxClasses().forEach(atc -> anyTypeClassDAO.findById(atc.getKey()).
                            ifPresent(classes::add));

                    gTypeExtensionClasses.put(memb.getRightEnd(), classes);
                }));

            default -> {
            }
        }

        gTypeExtensionClasses.entrySet().stream().peek(
                entry -> result.memberships().put(entry.getKey(), new HashSet<>())).
                forEach(entry -> entry.getValue().forEach(atc -> {

            if (reference.equals(PlainSchema.class)) {
                atc.getPlainSchemas().stream().
                        map(schema -> plainSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.memberships().get(entry.getKey()).add((S) schema));
            } else if (reference.equals(DerSchema.class)) {
                atc.getDerSchemas().stream().
                        map(schema -> derSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.memberships().get(entry.getKey()).add((S) schema));
            }
        }));

        // schemas given by relationship type extensions
        Map<RelationshipType, Set<AnyTypeClass>> rTypeExtensionClasses = new HashMap<>();

        switch (any) {
            case User user ->
                user.getRelationships().forEach(rel -> rel.getType().getTypeExtensions().forEach(typeExt -> {
                    Set<AnyTypeClass> classes = new HashSet<>();
                    typeExt.getAuxClasses().forEach(atc -> anyTypeClassDAO.findById(atc.getKey()).
                            ifPresent(classes::add));

                    rTypeExtensionClasses.put(rel.getType(), classes);
                }));

            case AnyObject anyObject ->
                anyObject.getRelationships().forEach(rel -> rel.getType().getTypeExtensions().stream().
                        filter(typeExt -> any.getType().equals(typeExt.getAnyType())).forEach(typeExt -> {

                    Set<AnyTypeClass> classes = new HashSet<>();
                    typeExt.getAuxClasses().forEach(atc -> anyTypeClassDAO.findById(atc.getKey()).
                            ifPresent(classes::add));

                    rTypeExtensionClasses.put(rel.getType(), classes);
                }));

            default -> {
            }
        }

        rTypeExtensionClasses.entrySet().stream().peek(
                entry -> result.relationshipTypes().put(entry.getKey(), new HashSet<>())).
                forEach(entry -> entry.getValue().forEach(atc -> {

            if (reference.equals(PlainSchema.class)) {
                atc.getPlainSchemas().stream().
                        map(schema -> plainSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.relationshipTypes().get(entry.getKey()).add((S) schema));
            } else if (reference.equals(DerSchema.class)) {
                atc.getDerSchemas().stream().
                        map(schema -> derSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.relationshipTypes().get(entry.getKey()).add((S) schema));
            }
        }));

        return result;
    }
}
