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
package org.apache.syncope.core.persistence.api.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AnyChecker {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyChecker.class);

    protected final PlainSchemaDAO plainSchemaDAO;

    public AnyChecker(final PlainSchemaDAO plainSchemaDAO) {
        this.plainSchemaDAO = plainSchemaDAO;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @SuppressWarnings("unchecked")
    public <S extends Schema> AllowedSchemas<S> findAllowedSchemas(final Any any, final Class<S> reference) {
        AllowedSchemas<S> result = new AllowedSchemas<>();

        // schemas given by type and aux classes
        Set<AnyTypeClass> typeOwnClasses = new HashSet<>();
        typeOwnClasses.addAll(any.getType().getClasses());
        typeOwnClasses.addAll(any.getAuxClasses());

        typeOwnClasses.forEach(typeClass -> {
            if (reference.equals(PlainSchema.class)) {
                result.self().addAll((Collection<? extends S>) typeClass.getPlainSchemas());
            } else if (reference.equals(DerSchema.class)) {
                result.self().addAll((Collection<? extends S>) typeClass.getDerSchemas());
            }
        });

        // schemas given by group type extensions
        Map<Group, List<? extends AnyTypeClass>> gTypeExtensionClasses = new HashMap<>();
        switch (any) {
            case User user ->
                user.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().
                        forEach(typeExt -> gTypeExtensionClasses.put(memb.getRightEnd(), typeExt.getAuxClasses())));
            case AnyObject anyObject ->
                anyObject.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().stream().
                        filter(typeExt -> any.getType().equals(typeExt.getAnyType())).
                        forEach(typeExt -> gTypeExtensionClasses.put(memb.getRightEnd(), typeExt.getAuxClasses())));
            default -> {
            }
        }
        gTypeExtensionClasses.entrySet().stream().peek(
                entry -> result.memberships().put(entry.getKey(), new HashSet<>())).
                forEach(entry -> entry.getValue().forEach(typeClass -> {
            if (reference.equals(PlainSchema.class)) {
                result.memberships().get(entry.getKey()).
                        addAll((Collection<? extends S>) typeClass.getPlainSchemas());
            } else if (reference.equals(DerSchema.class)) {
                result.memberships().get(entry.getKey()).
                        addAll((Collection<? extends S>) typeClass.getDerSchemas());
            }
        }));

        // schemas given by relationship type extensions
        Map<RelationshipType, List<? extends AnyTypeClass>> rTypeExtensionClasses = new HashMap<>();
        switch (any) {
            case User user ->
                user.getRelationships().stream().map(Relationship::getType).distinct().
                        forEach(rt -> rt.getTypeExtensions().
                        forEach(typeExt -> rTypeExtensionClasses.put(rt, typeExt.getAuxClasses())));
            case AnyObject anyObject ->
                anyObject.getRelationships().stream().map(Relationship::getType).distinct().
                        forEach(rt -> rt.getTypeExtensions().
                        forEach(typeExt -> rTypeExtensionClasses.put(rt, typeExt.getAuxClasses())));
            default -> {
            }
        }
        rTypeExtensionClasses.entrySet().stream().peek(
                entry -> result.relationshipTypes().put(entry.getKey(), new HashSet<>())).
                forEach(entry -> entry.getValue().forEach(typeClass -> {
            if (reference.equals(PlainSchema.class)) {
                result.relationshipTypes().get(entry.getKey()).
                        addAll((Collection<? extends S>) typeClass.getPlainSchemas());
            } else if (reference.equals(DerSchema.class)) {
                result.relationshipTypes().get(entry.getKey()).
                        addAll((Collection<? extends S>) typeClass.getDerSchemas());
            }
        }));

        return result;
    }

    @Transactional(readOnly = true)
    public <T extends Attributable> void checkBeforeSave(final T attributable, final AnyUtils anyUtils) {
        if (attributable instanceof Any any) {
            AllowedSchemas<PlainSchema> allowed = findAllowedSchemas(any, PlainSchema.class);

            for (PlainAttr attr : any.getPlainAttrs()) {
                String schema = Optional.ofNullable(attr).map(PlainAttr::getSchema).orElse(null);
                if (schema != null && !allowed.selfContains(schema)) {
                    throw new InvalidEntityException(
                            anyUtils.anyClass(),
                            EntityViolationType.InvalidPlainAttr.propertyPath("plainAttrs"),
                            schema + " not allowed for this instance");
                }
            }
            if (any instanceof Groupable<?, ?, ?> groupable) {
                for (Membership<?> membership : groupable.getMemberships()) {
                    for (PlainAttr attr : groupable.getPlainAttrs(membership)) {
                        String schema = Optional.ofNullable(attr).map(PlainAttr::getSchema).orElse(null);
                        if (schema != null && !allowed.membershipsContains(membership.getRightEnd(), schema)) {
                            throw new InvalidEntityException(
                                    anyUtils.anyClass(),
                                    EntityViolationType.InvalidPlainAttr.propertyPath("plainAttrs"),
                                    schema + " not allowed for membership of group "
                                    + membership.getRightEnd().getName());
                        }
                    }
                }
            }
            if (any instanceof Relatable<?, ?> relatable) {
                for (Relationship<?, ?> relationship : relatable.getRelationships()) {
                    for (PlainAttr attr : relatable.getPlainAttrs(relationship)) {
                        String schema = Optional.ofNullable(attr).map(PlainAttr::getSchema).orElse(null);
                        if (schema != null && !allowed.relationshipTypesContains(relationship.getType(), schema)) {
                            throw new InvalidEntityException(
                                    anyUtils.anyClass(),
                                    EntityViolationType.InvalidPlainAttr.propertyPath("plainAttrs"),
                                    schema + " not allowed for relationships of type "
                                    + relationship.getType().getKey());
                        }
                    }
                }
            }
        }

        // check UNIQUE constraints
        attributable.getPlainAttrs().stream().
                filter(attr -> attr.getUniqueValue() != null).
                forEach(attr -> {
                    if (plainSchemaDAO.existsPlainAttrUniqueValue(
                            anyUtils,
                            attributable.getKey(),
                            plainSchemaDAO.findById(attr.getSchema()).
                                    orElseThrow(() -> new NotFoundException("PlainSchema " + attr.getSchema())),
                            attr.getUniqueValue())) {

                        throw new DuplicateException("Duplicate value found for "
                                + attr.getSchema() + "=" + attr.getUniqueValue().getValueAsString());
                    } else {
                        LOG.debug("No duplicate value found for {}={}",
                                attr.getSchema(), attr.getUniqueValue().getValueAsString());
                    }
                });
    }
}
