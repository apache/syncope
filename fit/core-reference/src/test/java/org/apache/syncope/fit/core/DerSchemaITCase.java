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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.SchemaQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class DerSchemaITCase extends AbstractITCase {

    @Test
    public void list() {
        List<DerSchemaTO> derSchemas = schemaService.list(new SchemaQuery.Builder().type(SchemaType.DERIVED).build());
        assertFalse(derSchemas.isEmpty());
        for (DerSchemaTO derivedSchemaTO : derSchemas) {
            assertNotNull(derivedSchemaTO);
        }
    }

    @Test
    public void read() {
        DerSchemaTO derivedSchemaTO = schemaService.read(SchemaType.DERIVED, "cn");
        assertNotNull(derivedSchemaTO);
    }

    @Test
    public void create() {
        DerSchemaTO schema = new DerSchemaTO();
        schema.setKey("derived");
        schema.setExpression("derived_sx + '_' + derived_dx");

        DerSchemaTO actual = createSchema(SchemaType.DERIVED, schema);
        assertNotNull(actual);

        actual = schemaService.read(SchemaType.DERIVED, actual.getKey());
        assertNotNull(actual);
        assertEquals(actual.getExpression(), "derived_sx + '_' + derived_dx");
    }

    @Test
    public void delete() {
        DerSchemaTO schema = schemaService.read(SchemaType.DERIVED, "rderiveddata");
        assertNotNull(schema);

        schemaService.delete(SchemaType.DERIVED, schema.getKey());

        try {
            schemaService.read(SchemaType.DERIVED, "rderiveddata");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        } finally {
            // Recreate schema to make test re-runnable
            schema = createSchema(SchemaType.DERIVED, schema);
            assertNotNull(schema);
        }
    }

    @Test
    public void update() {
        DerSchemaTO schema = schemaService.read(SchemaType.DERIVED, "mderiveddata");
        assertNotNull(schema);
        assertEquals("mderived_sx + '-' + mderived_dx", schema.getExpression());
        try {
            schema.setExpression("mderived_sx + '.' + mderived_dx");

            schemaService.update(SchemaType.DERIVED, schema);

            schema = schemaService.read(SchemaType.DERIVED, "mderiveddata");
            assertNotNull(schema);
            assertEquals("mderived_sx + '.' + mderived_dx", schema.getExpression());
        } finally {
            // Set updated back to make test re-runnable
            schema.setExpression("mderived_sx + '-' + mderived_dx");
            schemaService.update(SchemaType.DERIVED, schema);
        }
    }

    @Test
    public void issueSYNCOPE323() {
        DerSchemaTO actual = schemaService.read(SchemaType.DERIVED, "rderiveddata");
        assertNotNull(actual);

        try {
            createSchema(SchemaType.DERIVED, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.CONFLICT, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }

        actual.setKey(null);
        try {
            createSchema(SchemaType.DERIVED, actual);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.BAD_REQUEST, e.getType().getResponseStatus());
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE418() {
        DerSchemaTO schema = new DerSchemaTO();
        schema.setKey("http://schemas.examples.org/security/authorization/organizationUnit");
        schema.setExpression("derived_sx + '_' + derived_dx");

        try {
            createSchema(SchemaType.DERIVED, schema);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidDerSchema, e.getType());
            assertTrue(e.getElements().iterator().next().contains(EntityViolationType.InvalidKey.name()));
        }
    }
}
