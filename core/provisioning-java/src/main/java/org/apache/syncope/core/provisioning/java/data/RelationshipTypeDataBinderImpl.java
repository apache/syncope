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
package org.apache.syncope.core.provisioning.java.data;

import java.util.Optional;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.RelationshipTypeExtension;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipTypeDataBinderImpl implements RelationshipTypeDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(RelationshipTypeDataBinder.class);

    protected final AnyTypeDAO anyTypeDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final EntityFactory entityFactory;

    public RelationshipTypeDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final EntityFactory entityFactory) {

        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public RelationshipType create(final RelationshipTypeTO relationshipTypeTO) {
        RelationshipType relationshipType = entityFactory.newEntity(RelationshipType.class);

        relationshipType.setLeftEndAnyType(Optional.ofNullable(relationshipTypeTO.getLeftEndAnyType()).
                flatMap(anyTypeDAO::findById).
                orElseThrow(() -> new NotFoundException("AnyType " + relationshipTypeTO.getLeftEndAnyType())));
        relationshipType.setRightEndAnyType(Optional.ofNullable(relationshipTypeTO.getRightEndAnyType()).
                flatMap(anyTypeDAO::findById).
                orElseThrow(() -> new NotFoundException("AnyType " + relationshipTypeTO.getRightEndAnyType())));

        update(relationshipType, relationshipTypeTO);

        return relationshipType;
    }

    @Override
    public void update(final RelationshipType relationshipType, final RelationshipTypeTO relationshipTypeTO) {
        if (relationshipType.getKey() == null) {
            relationshipType.setKey(relationshipTypeTO.getKey());
        }

        relationshipType.setDescription(relationshipTypeTO.getDescription());

        // type extensions
        relationshipTypeTO.getTypeExtensions().
                forEach(typeExtTO -> anyTypeDAO.findById(typeExtTO.getAnyType()).ifPresentOrElse(anyType -> {

            RelationshipTypeExtension typeExt = relationshipType.getTypeExtension(anyType).orElse(null);
            if (typeExt == null) {
                typeExt = entityFactory.newEntity(RelationshipTypeExtension.class);
                typeExt.setAnyType(anyType);
                typeExt.setRelationshipType(relationshipType);
                relationshipType.add(typeExt);
            }

            // add all classes contained in the TO
            for (String key : typeExtTO.getAuxClasses()) {
                AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(key).orElse(null);
                if (anyTypeClass == null) {
                    LOG.warn("Ignoring invalid {}: {}", AnyTypeClass.class.getSimpleName(), key);
                } else {
                    typeExt.add(anyTypeClass);
                }
            }
            // remove all classes not contained in the TO
            typeExt.getAuxClasses().
                    removeIf(anyTypeClass -> !typeExtTO.getAuxClasses().contains(anyTypeClass.getKey()));

            // only consider non-empty type extensions
            if (typeExt.getAuxClasses().isEmpty()) {
                relationshipType.getTypeExtensions().remove(typeExt);
                typeExt.setRelationshipType(null);
            }

        }, () -> LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), typeExtTO.getAnyType())));

        // remove all type extensions not contained in the TO
        relationshipType.getTypeExtensions().
                removeIf(typeExt -> relationshipTypeTO.getTypeExtension(typeExt.getAnyType().getKey()).isEmpty());
    }

    protected TypeExtensionTO getTypeExtensionTO(final RelationshipTypeExtension typeExt) {
        TypeExtensionTO typeExtTO = new TypeExtensionTO();
        typeExtTO.setAnyType(typeExt.getAnyType().getKey());
        typeExtTO.getAuxClasses().addAll(typeExt.getAuxClasses().stream().map(AnyTypeClass::getKey).toList());
        return typeExtTO;
    }

    @Override
    public RelationshipTypeTO getRelationshipTypeTO(final RelationshipType relationshipType) {
        RelationshipTypeTO relationshipTypeTO = new RelationshipTypeTO();

        relationshipTypeTO.setKey(relationshipType.getKey());
        relationshipTypeTO.setDescription(relationshipType.getDescription());
        relationshipTypeTO.setLeftEndAnyType(relationshipType.getLeftEndAnyType().getKey());
        relationshipTypeTO.setRightEndAnyType(relationshipType.getRightEndAnyType().getKey());

        relationshipType.getTypeExtensions().
                forEach(typeExt -> relationshipTypeTO.getTypeExtensions().add(getTypeExtensionTO(typeExt)));

        return relationshipTypeTO;
    }
}
