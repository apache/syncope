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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.syncope.client.to.VirtualSchemaTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class VirtualSchemaTestITCase extends AbstractTest {

    private static final String ROLE = "role";
    private static final String USER = "user";
    private static final String MEMBERSHIP = "membership";

    @Test
    public void list() {
        List<VirtualSchemaTO> VirtualSchemas = schemaService.list(USER, VirtualSchemaTO[].class);
        assertFalse(VirtualSchemas.isEmpty());
        for (VirtualSchemaTO VirtualSchemaTO : VirtualSchemas) {
            assertNotNull(VirtualSchemaTO);
        }
    }

    @Test
    public void read() {
        VirtualSchemaTO VirtualSchemaTO = schemaService.read(MEMBERSHIP, "mvirtualdata",
                VirtualSchemaTO.class);
        assertNotNull(VirtualSchemaTO);
    }

    @Test
    public void create() {
        VirtualSchemaTO schema = new VirtualSchemaTO();
        schema.setName("virtual");

        VirtualSchemaTO actual = schemaService.create(USER, schema);
        assertNotNull(actual);

        actual = schemaService.read(USER, actual.getName(), VirtualSchemaTO.class);
        assertNotNull(actual);
    }

    @Test
    public void delete() {
        VirtualSchemaTO schema = schemaService.read(ROLE, "rvirtualdata", VirtualSchemaTO.class);
        assertNotNull(schema);

        VirtualSchemaTO deletedSchema = schemaService.delete(ROLE, schema.getName(), VirtualSchemaTO.class);
        assertNotNull(deletedSchema);

        Throwable t = null;
        try {
            schema = schemaService.read(ROLE, "rvirtualdata", VirtualSchemaTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            t = e;
            assertNotNull(e.getException(SyncopeClientExceptionType.NotFound));
        }
        assertNotNull(t);
    }
}
