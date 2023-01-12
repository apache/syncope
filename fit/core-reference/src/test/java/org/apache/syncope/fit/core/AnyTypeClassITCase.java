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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class AnyTypeClassITCase extends AbstractITCase {

    @Test
    public void read() {
        AnyTypeClassTO minimalGroup = ANY_TYPE_CLASS_SERVICE.read("minimal group");
        assertNotNull(minimalGroup);

        assertFalse(minimalGroup.getPlainSchemas().isEmpty());
        assertFalse(minimalGroup.getDerSchemas().isEmpty());
        assertFalse(minimalGroup.getVirSchemas().isEmpty());
    }

    @Test
    public void list() {
        List<AnyTypeClassTO> list = ANY_TYPE_CLASS_SERVICE.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void crud() {
        // 1. create sample schemas
        PlainSchemaTO plainSchema = new PlainSchemaTO();
        plainSchema.setKey("new_plain_schema" + getUUIDString());
        plainSchema.setType(AttrSchemaType.String);
        plainSchema = createSchema(SchemaType.PLAIN, plainSchema);

        DerSchemaTO derSchema = new DerSchemaTO();
        derSchema.setKey("new_der_schema" + getUUIDString());
        derSchema.setExpression(plainSchema.getKey() + " + '_' + derived_dx");
        derSchema = createSchema(SchemaType.DERIVED, derSchema);

        // 2. actual CRUD
        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("new class" + getUUIDString());
        newClass.getPlainSchemas().add(plainSchema.getKey());

        Response response = ANY_TYPE_CLASS_SERVICE.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().isEmpty());
        assertTrue(newClass.getDerSchemas().isEmpty());
        assertTrue(newClass.getVirSchemas().isEmpty());

        newClass.getDerSchemas().add(derSchema.getKey());
        ANY_TYPE_CLASS_SERVICE.update(newClass);

        newClass = ANY_TYPE_CLASS_SERVICE.read(newClass.getKey());
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().isEmpty());
        assertFalse(newClass.getDerSchemas().isEmpty());
        assertTrue(newClass.getVirSchemas().isEmpty());

        assertEquals(newClass.getKey(), SCHEMA_SERVICE.read(SchemaType.PLAIN, plainSchema.getKey()).getAnyTypeClass());
        assertEquals(newClass.getKey(), SCHEMA_SERVICE.read(SchemaType.DERIVED, derSchema.getKey()).getAnyTypeClass());

        ANY_TYPE_CLASS_SERVICE.delete(newClass.getKey());

        try {
            ANY_TYPE_CLASS_SERVICE.read(newClass.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        assertNull(SCHEMA_SERVICE.read(SchemaType.PLAIN, plainSchema.getKey()).getAnyTypeClass());
        assertNull(SCHEMA_SERVICE.read(SchemaType.DERIVED, derSchema.getKey()).getAnyTypeClass());
    }

    @Test
    public void deleteSchema() {
        PlainSchemaTO newSchema = new PlainSchemaTO();
        newSchema.setKey("newSchema" + getUUIDString());
        newSchema.setType(AttrSchemaType.Date);
        createSchema(SchemaType.PLAIN, newSchema);

        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("new class" + getUUIDString());
        newClass.getPlainSchemas().add(newSchema.getKey());

        Response response = ANY_TYPE_CLASS_SERVICE.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);
        assertNotNull(newClass);
        assertTrue(newClass.getPlainSchemas().contains(newSchema.getKey()));

        SCHEMA_SERVICE.delete(SchemaType.PLAIN, newSchema.getKey());

        newClass = ANY_TYPE_CLASS_SERVICE.read(newClass.getKey());
        assertNotNull(newClass);
        assertFalse(newClass.getPlainSchemas().contains(newSchema.getKey()));
    }

    @Test
    public void issueSYNCOPE759() {
        AnyTypeClassTO minimalGroup = ANY_TYPE_CLASS_SERVICE.read("minimal group");
        assertNotNull(minimalGroup);

        AnyTypeClassTO newAnyTypeClass = new AnyTypeClassTO();
        newAnyTypeClass.setKey(minimalGroup.getKey());

        try {
            ANY_TYPE_CLASS_SERVICE.create(newAnyTypeClass);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }
}
