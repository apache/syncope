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
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;

public class RelationshipTypeDataBinderImpl implements RelationshipTypeDataBinder {

    protected final AnyTypeDAO anyTypeDAO;

    protected final EntityFactory entityFactory;

    public RelationshipTypeDataBinderImpl(final AnyTypeDAO anyTypeDAO, final EntityFactory entityFactory) {
        this.anyTypeDAO = anyTypeDAO;
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
    }

    @Override
    public RelationshipTypeTO getRelationshipTypeTO(final RelationshipType relationshipType) {
        RelationshipTypeTO relationshipTypeTO = new RelationshipTypeTO();

        relationshipTypeTO.setKey(relationshipType.getKey());
        relationshipTypeTO.setDescription(relationshipType.getDescription());
        relationshipTypeTO.setLeftEndAnyType(relationshipType.getLeftEndAnyType().getKey());
        relationshipTypeTO.setRightEndAnyType(relationshipType.getRightEndAnyType().getKey());

        return relationshipTypeTO;
    }
}
