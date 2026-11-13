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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.apache.syncope.common.lib.Attr;
import org.junit.jupiter.api.Test;

class AnyTOTest {

    private static final OffsetDateTime CREATION = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime LAST_CHANGE = OffsetDateTime.of(2026, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void etag() {
        UserTO user = new UserTO();
        assertEquals("", user.getETagValue());

        user.setCreationDate(CREATION);
        assertEquals(String.valueOf(CREATION.toInstant().toEpochMilli()), user.getETagValue());

        user.setLastChangeDate(LAST_CHANGE);
        assertEquals(String.valueOf(LAST_CHANGE.toInstant().toEpochMilli()), user.getETagValue());
    }

    @Test
    void plainAndDerivedAttrs() {
        UserTO user = new UserTO();
        user.getPlainAttrs().add(new Attr.Builder("username").value("rossini").build());
        user.getDerAttrs().add(new Attr.Builder("derived").value("value").build());

        assertTrue(user.getPlainAttr("username").isPresent());
        assertFalse(user.getPlainAttr("other").isPresent());
        assertTrue(user.getDerAttr("derived").isPresent());
        assertFalse(user.getDerAttr("other").isPresent());

        assertEquals(1, user.getPlainAttrs().size());
        assertEquals(1, user.getDerAttrs().size());
    }

    @Test
    void groupsAndRelationships() {
        UserTO user = new UserTO();
        user.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());
        user.getAuxClasses().add("aux");
        user.getResources().add("resource");

        assertEquals(1, user.getRelationships().size());
        assertEquals(1, user.getAuxClasses().size());
        assertEquals(1, user.getResources().size());
    }

    @Test
    void equalsAndHashCode() {
        UserTO first = new UserTO();
        first.setKey("key");
        first.setUsername("username");
        UserTO second = new UserTO();
        second.setKey("key");
        second.setUsername("username");
        UserTO other = new UserTO();
        other.setKey("key");
        other.setUsername("other");

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertNotEquals(first, other);
    }

    @Test
    void discriminator() {
        assertEquals(UserTO.class.getName(), new UserTO().getDiscriminator());
        assertEquals(GroupTO.class.getName(), new GroupTO().getDiscriminator());
        assertEquals(AnyObjectTO.class.getName(), new AnyObjectTO().getDiscriminator());

        UserTO user = new UserTO();
        user.setDiscriminator("ignored");
        assertEquals(UserTO.class.getName(), user.getDiscriminator());
    }

    @Test
    void statusAndManagers() {
        AnyObjectTO anyObject = new AnyObjectTO();
        anyObject.setStatus("active");
        anyObject.setuManager("uManager");
        anyObject.setgManager("gManager");
        anyObject.setCreator("creator");
        anyObject.setCreationContext("creationContext");
        anyObject.setLastModifier("lastModifier");
        anyObject.setLastChangeContext("lastChangeContext");
        anyObject.setType("PRINTER");
        anyObject.setRealm("/");

        assertEquals("active", anyObject.getStatus());
        assertEquals("uManager", anyObject.getuManager());
        assertEquals("gManager", anyObject.getgManager());
        assertEquals("creator", anyObject.getCreator());
        assertEquals("creationContext", anyObject.getCreationContext());
        assertEquals("lastModifier", anyObject.getLastModifier());
        assertEquals("lastChangeContext", anyObject.getLastChangeContext());
        assertEquals("PRINTER", anyObject.getType());
        assertEquals("/", anyObject.getRealm());
    }
}
