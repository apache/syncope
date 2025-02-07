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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class RelationshipTypeITCase extends AbstractITCase {

    @Test
    public void read() {
        RelationshipTypeTO relType = RELATIONSHIP_TYPE_SERVICE.read("inclusion");
        assertNotNull(relType);
        assertEquals("inclusion", relType.getKey());
        assertEquals(PRINTER, relType.getLeftEndAnyType());
        assertEquals(PRINTER, relType.getRightEndAnyType());
    }

    @Test
    public void list() {
        List<RelationshipTypeTO> list = RELATIONSHIP_TYPE_SERVICE.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void crud() {
        RelationshipTypeTO newType = new RelationshipTypeTO();
        newType.setKey("new type");
        newType.setDescription("description");
        newType.setLeftEndAnyType(AnyTypeKind.GROUP.name());
        newType.setRightEndAnyType(PRINTER);

        Response response = RELATIONSHIP_TYPE_SERVICE.create(newType);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newType = getObject(response.getLocation(), RelationshipTypeService.class, RelationshipTypeTO.class);
        assertNotNull(newType);
        assertEquals("description", newType.getDescription());
        assertEquals(AnyTypeKind.GROUP.name(), newType.getLeftEndAnyType());
        assertEquals(PRINTER, newType.getRightEndAnyType());

        newType.setDescription("new description");
        RELATIONSHIP_TYPE_SERVICE.update(newType);

        newType = RELATIONSHIP_TYPE_SERVICE.read(newType.getKey());
        assertNotNull(newType);
        assertEquals("new description", newType.getDescription());
        assertEquals(AnyTypeKind.GROUP.name(), newType.getLeftEndAnyType());
        assertEquals(PRINTER, newType.getRightEndAnyType());

        RELATIONSHIP_TYPE_SERVICE.delete(newType.getKey());

        try {
            RELATIONSHIP_TYPE_SERVICE.read(newType.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void createInvalidName() {
        RelationshipTypeTO newType = new RelationshipTypeTO();
        newType.setKey("umembership");
        newType.setLeftEndAnyType(AnyTypeKind.GROUP.name());
        newType.setRightEndAnyType(PRINTER);
        try {
            RELATIONSHIP_TYPE_SERVICE.create(newType);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRelationshipType, e.getType());
        }
    }
}
