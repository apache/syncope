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
package org.apache.syncope.core.rest;

import static org.junit.Assert.*;

import java.util.List;
import org.apache.syncope.client.to.DerivedSchemaTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.services.SchemaService;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class DerivedSchemaTestITCase extends AbstractTest {

    private static final String ROLE = "role";

    private static final String USER = "user";

    private static final String MEMBERSHIP = "membership";

    @Test
    public void list() {
        List<DerivedSchemaTO> derivedSchemas = schemaService.list(USER, SchemaService.SchemaKind.DERIVED);
        assertFalse(derivedSchemas.isEmpty());
        for (DerivedSchemaTO derivedSchemaTO : derivedSchemas) {
            assertNotNull(derivedSchemaTO);
        }
    }

    @Test
    public void read() {
        DerivedSchemaTO derivedSchemaTO = schemaService.read(USER, SchemaService.SchemaKind.DERIVED, "cn");
        assertNotNull(derivedSchemaTO);
    }

    @Test
    public void create() {
        DerivedSchemaTO schema = new DerivedSchemaTO();
        schema.setName("derived");
        schema.setExpression("derived_sx + '_' + derived_dx");

        DerivedSchemaTO actual = schemaService.create(USER, SchemaService.SchemaKind.DERIVED, schema);
        assertNotNull(actual);

        actual = schemaService.read(USER, SchemaService.SchemaKind.DERIVED, actual.getName());
        assertNotNull(actual);
        assertEquals(actual.getExpression(), "derived_sx + '_' + derived_dx");
    }

    @Test
    public void delete() {
        DerivedSchemaTO schema = schemaService.read(ROLE, SchemaService.SchemaKind.DERIVED, "rderiveddata");
        assertNotNull(schema);

        DerivedSchemaTO schemaToDelete = schemaService.delete(ROLE, SchemaService.SchemaKind.DERIVED, schema.getName());
        assertNotNull(schemaToDelete);

        Throwable t = null;
        try {
            schemaService.read(ROLE, SchemaService.SchemaKind.DERIVED, "rderiveddata");
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
            assertNotNull(e.getException(SyncopeClientExceptionType.NotFound));
        } finally {
            // Recreate schema to make test re-runnable
            schema = schemaService.create(ROLE, SchemaService.SchemaKind.DERIVED, schemaToDelete);
            assertNotNull(schema);
        }
        assertNotNull(t);
    }

    @Test
    public void update() {
        DerivedSchemaTO schema = schemaService.read(MEMBERSHIP, SchemaService.SchemaKind.DERIVED, "mderiveddata");
        assertNotNull(schema);
        assertEquals("mderived_sx + '-' + mderived_dx", schema.getExpression());
        try {
            schema.setExpression("mderived_sx + '.' + mderived_dx");

            schema = schemaService.update(MEMBERSHIP, SchemaService.SchemaKind.DERIVED, schema.getName(), schema);
            assertNotNull(schema);

            schema = schemaService.read(MEMBERSHIP, SchemaService.SchemaKind.DERIVED, "mderiveddata");
            assertNotNull(schema);
            assertEquals("mderived_sx + '.' + mderived_dx", schema.getExpression());
        } finally {
            // Set updated back to make test re-runnable
            schema.setExpression("mderived_sx + '-' + mderived_dx");
            schema = schemaService.update(MEMBERSHIP, SchemaService.SchemaKind.DERIVED, schema.getName(), schema);
            assertNotNull(schema);
        }
    }
}
