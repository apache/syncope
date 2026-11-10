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
package org.apache.syncope.common.lib.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.StatusRType;
import org.junit.jupiter.api.Test;

class PatchTest {

    @Test
    void stringPatchItem() {
        StringPatchItem item = new StringPatchItem.Builder().value("value").build();

        assertEquals("value", item.getValue());
        assertEquals(PatchOperation.ADD_REPLACE, item.getOperation());

        item = new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("value").build();
        assertEquals(PatchOperation.DELETE, item.getOperation());
    }

    @Test
    void stringReplacePatchItem() {
        StringReplacePatchItem item = new StringReplacePatchItem.Builder().value("value").build();

        assertEquals("value", item.getValue());
        assertEquals(PatchOperation.ADD_REPLACE, item.getOperation());

        item.setOperation(PatchOperation.DELETE);
        assertEquals(PatchOperation.ADD_REPLACE, item.getOperation());
    }

    @Test
    void booleanReplacePatchItem() {
        BooleanReplacePatchItem item = new BooleanReplacePatchItem.Builder().value(true).build();

        assertTrue(item.getValue());
        assertEquals(PatchOperation.ADD_REPLACE, item.getOperation());
    }

    @Test
    void longPatchItem() {
        LongPatchItem item = new LongPatchItem.Builder().value(10L).operation(PatchOperation.DELETE).build();

        assertEquals(10L, item.getValue());
        assertEquals(PatchOperation.DELETE, item.getOperation());
    }

    @Test
    void passwordPatch() {
        PasswordPatch patch = new PasswordPatch.Builder().
                value("password").
                onSyncope(false).
                resource("one").
                resources("two", "three").
                build();

        assertEquals("password", patch.getValue());
        assertFalse(patch.isOnSyncope());
        assertEquals(3, patch.getResources().size());
        assertTrue(patch.getResources().containsAll(Set.of("one", "two", "three")));

        PasswordPatch same = new PasswordPatch.Builder().
                value("password").
                onSyncope(false).
                resources("one", "two", "three").
                build();
        PasswordPatch other = new PasswordPatch.Builder().value("other").build();

        assertEquals(patch, same);
        assertEquals(patch.hashCode(), same.hashCode());
        assertNotEquals(patch, other);

        patch.setOperation(PatchOperation.DELETE);
        assertEquals(PatchOperation.ADD_REPLACE, patch.getOperation());
    }

    @Test
    void attrPatch() {
        AttrPatch empty = new AttrPatch.Builder(new Attr.Builder("schema").build()).build();
        assertTrue(empty.isEmpty());

        AttrPatch notEmpty = new AttrPatch.Builder(new Attr.Builder("schema").value("value").build()).build();
        assertFalse(notEmpty.isEmpty());

        AttrPatch noAttr = new AttrPatch.Builder(null).build();
        assertTrue(noAttr.isEmpty());
        assertNull(noAttr.getAttr());

        notEmpty.setOperation(PatchOperation.DELETE);
        assertEquals(PatchOperation.DELETE, notEmpty.getOperation());
    }

    @Test
    void statusR() {
        StatusR status = new StatusR.Builder("key", StatusRType.SUSPEND).
                token("token").
                onSyncope(false).
                resource("resource").
                build();

        assertEquals("key", status.getKey());
        assertEquals(StatusRType.SUSPEND, status.getType());
        assertEquals("token", status.getToken());
        assertFalse(status.isOnSyncope());
        assertEquals(1, status.getResources().size());
    }

    @Test
    void resourceAR() {
        ResourceAR request = new ResourceAR.Builder().
                key("key").
                action(ResourceAssociationAction.ASSIGN).
                onSyncope(false).
                resource("resource").
                value("password").
                build();

        assertEquals("key", request.getKey());
        assertEquals(ResourceAssociationAction.ASSIGN, request.getAction());
        assertFalse(request.isOnSyncope());
        assertEquals(1, request.getResources().size());
        assertEquals("password", request.getValue());
    }

    @Test
    void resourceDR() {
        ResourceDR request = new ResourceDR.Builder().
                key("key").
                action(ResourceDeassociationAction.UNASSIGN).
                resource("resource").
                build();

        assertEquals("key", request.getKey());
        assertEquals(ResourceDeassociationAction.UNASSIGN, request.getAction());
        assertEquals(1, request.getResources().size());
    }
}
