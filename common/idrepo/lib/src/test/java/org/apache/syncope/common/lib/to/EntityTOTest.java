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
package org.apache.syncope.common.lib.to;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.junit.jupiter.api.Test;

class EntityTOTest {

    private static final OffsetDateTime START = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime END = OffsetDateTime.of(2026, 12, 31, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void roleTO() {
        RoleTO role = new RoleTO();
        role.setKey("key");
        role.getEntitlements().add("entitlement");
        role.getRealms().add("/");

        assertEquals("key", role.getKey());
        assertEquals(1, role.getEntitlements().size());
        assertEquals(1, role.getRealms().size());

        RoleTO same = new RoleTO();
        same.setKey("key");
        same.getEntitlements().add("entitlement");
        same.getRealms().add("/");

        assertEquals(role, role);
        assertNotEquals(role, null);
        assertNotEquals(role, "string");
        assertEquals(role, same);
        assertEquals(role.hashCode(), same.hashCode());

        same.getRealms().add("/other");
        assertNotEquals(role, same);
    }

    @Test
    void anyTypeTO() {
        AnyTypeTO anyType = new AnyTypeTO();
        anyType.setKey("PRINTER");
        anyType.setKind(AnyTypeKind.ANY_OBJECT);
        anyType.getClasses().add("class");

        assertEquals("PRINTER", anyType.getKey());
        assertEquals(AnyTypeKind.ANY_OBJECT, anyType.getKind());
        assertEquals(1, anyType.getClasses().size());

        AnyTypeTO same = new AnyTypeTO();
        same.setKey("PRINTER");
        same.setKind(AnyTypeKind.ANY_OBJECT);
        same.getClasses().add("class");

        assertEquals(anyType, same);
        assertEquals(anyType.hashCode(), same.hashCode());
        assertNotEquals(anyType, new AnyTypeTO());
    }

    @Test
    void startEndBean() {
        DelegationTO delegation = new DelegationTO();
        delegation.setKey("key");
        delegation.setDelegating("delegating");
        delegation.setDelegated("delegated");
        delegation.getRoles().add("role");
        delegation.setStart(START);
        delegation.setEnd(END);

        assertEquals("key", delegation.getKey());
        assertEquals("delegating", delegation.getDelegating());
        assertEquals("delegated", delegation.getDelegated());
        assertEquals(1, delegation.getRoles().size());
        assertEquals(START, delegation.getStart());
        assertEquals(END, delegation.getEnd());
        assertNotNull(delegation.toString());

        DelegationTO same = new DelegationTO();
        same.setKey("key");
        same.setDelegating("delegating");
        same.setDelegated("delegated");
        same.getRoles().add("role");
        same.setStart(START);
        same.setEnd(END);

        assertEquals(delegation, same);
        assertEquals(delegation.hashCode(), same.hashCode());

        same.setEnd(null);
        assertNotEquals(delegation, same);
    }
}
