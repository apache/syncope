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

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class AnyTypeITCase extends AbstractITCase {

    @Test
    public void read() {
        AnyTypeTO userType = ANY_TYPE_SERVICE.read(AnyTypeKind.USER.name());
        assertNotNull(userType);
        assertEquals(AnyTypeKind.USER, userType.getKind());
        assertEquals(AnyTypeKind.USER.name(), userType.getKey());
        assertFalse(userType.getClasses().isEmpty());

        AnyTypeTO groupType = ANY_TYPE_SERVICE.read(AnyTypeKind.GROUP.name());
        assertNotNull(groupType);
        assertEquals(AnyTypeKind.GROUP, groupType.getKind());
        assertEquals(AnyTypeKind.GROUP.name(), groupType.getKey());
        assertFalse(groupType.getClasses().isEmpty());

        AnyTypeTO otherType = ANY_TYPE_SERVICE.read(PRINTER);
        assertNotNull(otherType);
        assertEquals(AnyTypeKind.ANY_OBJECT, otherType.getKind());
        assertEquals(PRINTER, otherType.getKey());
    }

    @Test
    public void list() {
        List<AnyTypeTO> list = ANY_TYPE_SERVICE.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void crud() {
        AnyTypeTO newType = new AnyTypeTO();
        newType.setKey("new type");
        newType.setKind(AnyTypeKind.ANY_OBJECT);
        newType.getClasses().add("generic membership");
        newType.getClasses().add("csv");

        Response response = ANY_TYPE_SERVICE.create(newType);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newType = getObject(response.getLocation(), AnyTypeService.class, AnyTypeTO.class);
        assertNotNull(newType);
        assertEquals(2, newType.getClasses().size());
        assertTrue(newType.getClasses().contains("generic membership"));
        assertTrue(newType.getClasses().contains("csv"));

        newType.getClasses().remove("generic membership");
        ANY_TYPE_SERVICE.update(newType);

        newType = ANY_TYPE_SERVICE.read(newType.getKey());
        assertNotNull(newType);
        assertEquals(1, newType.getClasses().size());
        assertTrue(newType.getClasses().contains("csv"));

        ANY_TYPE_SERVICE.delete(newType.getKey());

        try {
            ANY_TYPE_SERVICE.read(newType.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void createInvalidKind() {
        AnyTypeTO newType = new AnyTypeTO();
        newType.setKey("new type");
        newType.setKind(AnyTypeKind.USER);
        try {
            ANY_TYPE_SERVICE.create(newType);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidAnyType, e.getType());
        }
    }

    @Test
    public void createInvalidName() {
        AnyTypeTO newType = new AnyTypeTO();
        newType.setKey("GROUP");
        newType.setKind(AnyTypeKind.GROUP);
        try {
            ANY_TYPE_SERVICE.create(newType);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void deleteTypeClass() {
        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("new class" + getUUIDString());
        newClass.getDerSchemas().add("cn");

        Response response = ANY_TYPE_CLASS_SERVICE.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);
        assertNotNull(newClass);

        AnyTypeTO other = ANY_TYPE_SERVICE.read(PRINTER);
        assertNotNull(other);

        other.getClasses().add(newClass.getKey());
        ANY_TYPE_SERVICE.update(other);

        other = ANY_TYPE_SERVICE.read(other.getKey());
        assertNotNull(other);
        assertTrue(other.getClasses().contains(newClass.getKey()));

        ANY_TYPE_CLASS_SERVICE.delete(newClass.getKey());

        other = ANY_TYPE_SERVICE.read(other.getKey());
        assertNotNull(other);
        assertFalse(other.getClasses().contains(newClass.getKey()));
    }

    @Test
    public void issueSYNCOPE754() {
        AnyTypeClassTO other = ANY_TYPE_CLASS_SERVICE.read("other");
        assertNotNull(other);

        AnyTypeTO group = ANY_TYPE_SERVICE.read(AnyTypeKind.GROUP.name());
        try {
            assertFalse(group.getClasses().contains("other"));
            group.getClasses().add("other");

            ANY_TYPE_SERVICE.update(group);

            group = ANY_TYPE_SERVICE.read(AnyTypeKind.GROUP.name());
            assertTrue(group.getClasses().contains("other"));

            other = ANY_TYPE_CLASS_SERVICE.read("other");
            assertEquals(2, other.getInUseByTypes().size());
            assertTrue(other.getInUseByTypes().contains(AnyTypeKind.USER.name()));
            assertTrue(other.getInUseByTypes().contains(AnyTypeKind.GROUP.name()));
        } finally {
            group.getClasses().remove("other");
            ANY_TYPE_SERVICE.update(group);
        }
    }

    @Test
    public void issueSYNCOPE1472() {
        // 1. add any type class csv twice to PRINTER any type
        AnyTypeTO anyTypeTO = ANY_TYPE_SERVICE.read(PRINTER);
        anyTypeTO.getClasses().clear();
        anyTypeTO.getClasses().add("minimal printer");
        anyTypeTO.getClasses().add("csv");
        anyTypeTO.getClasses().add("csv");
        ANY_TYPE_SERVICE.update(anyTypeTO);

        // 2. read again and remove any type class
        anyTypeTO = ANY_TYPE_SERVICE.read(PRINTER);
        anyTypeTO.getClasses().remove("csv");
        ANY_TYPE_SERVICE.update(anyTypeTO);

        assertFalse(ANY_TYPE_SERVICE.read(PRINTER).getClasses().contains("csv"), 
                "Should not contain removed any type classes");
    }
}
