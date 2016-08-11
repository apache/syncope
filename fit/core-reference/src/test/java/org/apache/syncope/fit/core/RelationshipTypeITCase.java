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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class RelationshipTypeITCase extends AbstractITCase {

    @Test
    public void read() {
        RelationshipTypeTO otherType = relationshipTypeService.read("inclusion");
        assertNotNull(otherType);
        assertEquals("inclusion", otherType.getKey());
    }

    @Test
    public void list() {
        List<RelationshipTypeTO> list = relationshipTypeService.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void crud() {
        RelationshipTypeTO newType = new RelationshipTypeTO();
        newType.setKey("new type");
        newType.setDescription("description");

        Response response = relationshipTypeService.create(newType);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newType = getObject(response.getLocation(), RelationshipTypeService.class, RelationshipTypeTO.class);
        assertNotNull(newType);
        assertEquals("description", newType.getDescription());

        newType.setDescription("new description");
        relationshipTypeService.update(newType);

        newType = relationshipTypeService.read(newType.getKey());
        assertNotNull(newType);
        assertEquals("new description", newType.getDescription());

        relationshipTypeService.delete(newType.getKey());

        try {
            relationshipTypeService.read(newType.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void createInvalidName() {
        RelationshipTypeTO newType = new RelationshipTypeTO();
        newType.setKey("membership");
        try {
            relationshipTypeService.create(newType);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRelationshipType, e.getType());
        }
    }
}
