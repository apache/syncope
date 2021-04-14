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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.AccessControlException;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VirSchemaITCase extends AbstractITCase {

    @Test
    public void search() {
        List<VirSchemaTO> schemas = schemaService.search(new SchemaQuery.Builder().type(SchemaType.VIRTUAL).build());
        assertFalse(schemas.isEmpty());
        schemas.forEach(Assertions::assertNotNull);

        schemas = schemaService.search(new SchemaQuery.Builder().type(SchemaType.VIRTUAL).keyword("rvirtual*").build());
        assertFalse(schemas.isEmpty());
        schemas.forEach(Assertions::assertNotNull);
    }

    @Test
    public void crud() {
        ResourceTO csv = resourceService.read(RESOURCE_NAME_CSV);
        assertNotNull(csv);
        assertEquals(1, csv.getProvisions().size());
        assertTrue(csv.getProvisions().get(0).getVirSchemas().isEmpty());

        VirSchemaTO schema = new VirSchemaTO();
        schema.setKey("virtualTest" + getUUIDString());
        schema.setExtAttrName("name");
        schema.setResource(RESOURCE_NAME_CSV);
        schema.setAnyType(csv.getProvisions().get(0).getAnyType());
        schema.getLabels().put(Locale.ENGLISH, "Virtual");

        schema = createSchema(SchemaType.VIRTUAL, schema);
        assertNotNull(schema);
        assertEquals(csv.getProvisions().get(0).getAnyType(), schema.getAnyType());
        assertEquals(1, schema.getLabels().size());
        assertEquals("Virtual", schema.getLabel(Locale.ENGLISH));
        assertEquals(schema.getKey(), schema.getLabel(Locale.CHINESE));

        csv = resourceService.read(RESOURCE_NAME_CSV);
        assertNotNull(csv);
        assertEquals(1, csv.getProvisions().size());
        assertFalse(csv.getProvisions().get(0).getVirSchemas().isEmpty());

        schema = schemaService.read(SchemaType.VIRTUAL, schema.getKey());
        assertNotNull(schema);

        schemaService.delete(SchemaType.VIRTUAL, schema.getKey());

        try {
            schemaService.read(SchemaType.VIRTUAL, schema.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        csv = resourceService.read(RESOURCE_NAME_CSV);
        assertNotNull(csv);
        assertEquals(1, csv.getProvisions().size());
        assertTrue(csv.getProvisions().get(0).getVirSchemas().isEmpty());
    }

    @Test
    public void anonymous() {
        SchemaService unauthenticated = clientFactory.create().getService(SchemaService.class);
        try {
            unauthenticated.search(new SchemaQuery.Builder().type(SchemaType.VIRTUAL).build());
            fail("This should not happen");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        SchemaService anonymous = anonymusClient.getService(SchemaService.class);
        assertFalse(anonymous.search(new SchemaQuery.Builder().type(SchemaType.VIRTUAL).build()).isEmpty());
    }

    @Test
    public void issueSYNCOPE323() {
        VirSchemaTO actual = schemaService.read(SchemaType.VIRTUAL, "virtualdata");
        assertNotNull(actual);

        try {
            createSchema(SchemaType.VIRTUAL, actual);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setKey(null);
        try {
            createSchema(SchemaType.VIRTUAL, actual);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.BAD_REQUEST, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE418() {
        ResourceTO ws1 = resourceService.read(RESOURCE_NAME_WS1);
        assertNotNull(ws1);
        assertEquals(1, ws1.getProvisions().size());
        assertTrue(ws1.getProvisions().get(0).getVirSchemas().isEmpty());

        VirSchemaTO schema = new VirSchemaTO();
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");
        schema.setExtAttrName("name");
        schema.setResource(RESOURCE_NAME_WS1);
        schema.setAnyType(ws1.getProvisions().get(0).getAnyType());

        try {
            createSchema(SchemaType.VIRTUAL, schema);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidVirSchema, e.getType());

            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidKey.name()));
        }
    }
}
