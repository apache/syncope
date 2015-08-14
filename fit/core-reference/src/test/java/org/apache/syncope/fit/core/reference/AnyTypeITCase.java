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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AnyTypeITCase extends AbstractITCase {

    @Test
    public void read() {
        AnyTypeTO userType = anyTypeService.read(AnyTypeKind.USER.name());
        assertNotNull(userType);
        assertEquals(AnyTypeKind.USER, userType.getKind());
        assertEquals(AnyTypeKind.USER.name(), userType.getKey());
        assertFalse(userType.getClasses().isEmpty());

        AnyTypeTO groupType = anyTypeService.read(AnyTypeKind.GROUP.name());
        assertNotNull(groupType);
        assertEquals(AnyTypeKind.GROUP, groupType.getKind());
        assertEquals(AnyTypeKind.GROUP.name(), groupType.getKey());
        assertFalse(groupType.getClasses().isEmpty());

        AnyTypeTO otherType = anyTypeService.read("PRINTER");
        assertNotNull(otherType);
        assertEquals(AnyTypeKind.ANY_OBJECT, otherType.getKind());
        assertEquals("PRINTER", otherType.getKey());
    }

    @Test
    public void list() {
        List<AnyTypeTO> list = anyTypeService.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void crud() {
        AnyTypeTO newType = new AnyTypeTO();
        newType.setKey("new type");
        newType.setKind(AnyTypeKind.ANY_OBJECT);
        newType.getClasses().add("generic membership");
        newType.getClasses().add("csv");

        Response response = anyTypeService.create(newType);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newType = getObject(response.getLocation(), AnyTypeService.class, AnyTypeTO.class);
        assertNotNull(newType);
        assertEquals(2, newType.getClasses().size());
        assertTrue(newType.getClasses().contains("generic membership"));
        assertTrue(newType.getClasses().contains("csv"));

        newType.getClasses().remove("generic membership");
        anyTypeService.update(newType);

        newType = anyTypeService.read(newType.getKey());
        assertNotNull(newType);
        assertEquals(1, newType.getClasses().size());
        assertTrue(newType.getClasses().contains("csv"));

        anyTypeService.delete(newType.getKey());

        try {
            anyTypeService.read(newType.getKey());
            fail();
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
            anyTypeService.create(newType);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidAnyType, e.getType());
        }
    }

    @Test
    public void createInvalidName() {
        AnyTypeTO newType = new AnyTypeTO();
        newType.setKey("group");
        newType.setKind(AnyTypeKind.ANY_OBJECT);
        try {
            anyTypeService.create(newType);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidAnyType, e.getType());
        }
    }

    @Test
    public void deleteInvalid() {
        try {
            anyTypeService.delete(AnyTypeKind.USER.name());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidAnyType, e.getType());
        }
    }

    @Test
    public void deleteTypeClass() {
        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("new class" + getUUIDString());
        newClass.getDerSchemas().add("cn");

        Response response = anyTypeClassService.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);
        assertNotNull(newClass);

        AnyTypeTO other = anyTypeService.read("PRINTER");
        assertNotNull(other);

        other.getClasses().add(newClass.getKey());
        anyTypeService.update(other);

        other = anyTypeService.read(other.getKey());
        assertNotNull(other);
        assertTrue(other.getClasses().contains(newClass.getKey()));

        anyTypeClassService.delete(newClass.getKey());

        other = anyTypeService.read(other.getKey());
        assertNotNull(other);
        assertFalse(other.getClasses().contains(newClass.getKey()));
    }
}
