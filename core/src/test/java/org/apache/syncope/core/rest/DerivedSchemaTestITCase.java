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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.syncope.client.to.DerivedSchemaTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
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
        List<DerivedSchemaTO> derivedSchemas = schemaService.list(USER, DerivedSchemaTO[].class);
        assertFalse(derivedSchemas.isEmpty());
        for (DerivedSchemaTO derivedSchemaTO : derivedSchemas) {
            assertNotNull(derivedSchemaTO);
        }
    }

    @Test
    public void read() {
        DerivedSchemaTO derivedSchemaTO = schemaService.read(USER, "cn", DerivedSchemaTO.class);
        assertNotNull(derivedSchemaTO);
    }

    @Test
    public void create() {
        DerivedSchemaTO schema = new DerivedSchemaTO();
        schema.setName("derived");
        schema.setExpression("derived_sx + '_' + derived_dx");

        DerivedSchemaTO actual = schemaService.create(USER, schema);
        assertNotNull(actual);

        actual = schemaService.read(USER, actual.getName(), DerivedSchemaTO.class);
        assertNotNull(actual);
        assertEquals(actual.getExpression(), "derived_sx + '_' + derived_dx");
    }

    @Test
    public void delete() {
        DerivedSchemaTO schema = schemaService.read(ROLE, "rderiveddata", DerivedSchemaTO.class);
        assertNotNull(schema);

        DerivedSchemaTO schemaToDelete = schemaService.delete(ROLE, schema.getName(), DerivedSchemaTO.class);
        assertNotNull(schemaToDelete);

        Throwable t = null;
        try {
            schemaService.read(ROLE, "rderiveddata", DerivedSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
            assertNotNull(e.getException(SyncopeClientExceptionType.NotFound));
        }
        assertNotNull(t);
    }

    @Test
    public void update() {
        DerivedSchemaTO schema = schemaService.read(MEMBERSHIP, "mderiveddata", DerivedSchemaTO.class);
        assertNotNull(schema);
        assertEquals("mderived_sx + '-' + mderived_dx", schema.getExpression());

        schema.setExpression("mderived_sx + '.' + mderived_dx");

        schema = schemaService.update(MEMBERSHIP, schema.getName(), schema);
        assertNotNull(schema);

        schema = schemaService.read(MEMBERSHIP, "mderiveddata", DerivedSchemaTO.class);
        assertNotNull(schema);
        assertEquals("mderived_sx + '.' + mderived_dx", schema.getExpression());
    }
}
