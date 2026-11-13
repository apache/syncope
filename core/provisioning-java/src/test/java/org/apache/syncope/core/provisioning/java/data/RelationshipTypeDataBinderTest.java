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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationshipTypeDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private RelationshipTypeDataBinder binder;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Test
    void get() {
        RelationshipTypeTO relationshipType = binder.getRelationshipTypeTO(
                relationshipTypeDAO.findById("neighborhood").orElseThrow());
        assertEquals("neighborhood", relationshipType.getKey());
        assertEquals("Models the act that an object is near another", relationshipType.getDescription());
        assertEquals(AnyTypeKind.USER.name(), relationshipType.getLeftEndAnyType());
        assertEquals("PRINTER", relationshipType.getRightEndAnyType());
    }

    @Test
    void createUpdate() {
        RelationshipTypeTO relationshipTypeTO = new RelationshipTypeTO();
        relationshipTypeTO.setKey("testRelationship");
        relationshipTypeTO.setDescription("a test relationship");
        relationshipTypeTO.setLeftEndAnyType(AnyTypeKind.USER.name());
        relationshipTypeTO.setRightEndAnyType("PRINTER");

        TypeExtensionTO typeExtension = new TypeExtensionTO();
        typeExtension.setAnyType(AnyTypeKind.USER.name());
        typeExtension.getAuxClasses().addAll(List.of("other", "not-existing"));
        relationshipTypeTO.getTypeExtensions().add(typeExtension);
        TypeExtensionTO invalid = new TypeExtensionTO();
        invalid.setAnyType("not-existing");
        relationshipTypeTO.getTypeExtensions().add(invalid);

        RelationshipType relationshipType = relationshipTypeDAO.save(binder.create(relationshipTypeTO));

        RelationshipTypeTO created = binder.getRelationshipTypeTO(relationshipType);
        assertEquals("testRelationship", created.getKey());
        assertEquals("a test relationship", created.getDescription());
        assertEquals(1, created.getTypeExtensions().size());
        assertEquals(List.of("other"), created.getTypeExtension(AnyTypeKind.USER.name()).orElseThrow().getAuxClasses());

        created.setDescription("an updated test relationship");
        created.getTypeExtensions().clear();
        binder.update(relationshipType, created);

        RelationshipTypeTO updated = binder.getRelationshipTypeTO(relationshipType);
        assertEquals("an updated test relationship", updated.getDescription());
        assertTrue(updated.getTypeExtensions().isEmpty());
    }

    @Test
    void invalidAnyTypes() {
        RelationshipTypeTO relationshipTypeTO = new RelationshipTypeTO();
        relationshipTypeTO.setKey("testRelationship");
        relationshipTypeTO.setLeftEndAnyType("not-existing");
        relationshipTypeTO.setRightEndAnyType("PRINTER");
        assertThrows(NotFoundException.class, () -> binder.create(relationshipTypeTO));

        relationshipTypeTO.setLeftEndAnyType(AnyTypeKind.USER.name());
        relationshipTypeTO.setRightEndAnyType("not-existing");
        assertThrows(NotFoundException.class, () -> binder.create(relationshipTypeTO));
    }
}
