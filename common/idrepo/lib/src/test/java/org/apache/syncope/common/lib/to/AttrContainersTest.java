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

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.junit.jupiter.api.Test;

class AttrContainersTest {

    @Test
    void relationship() {
        RelationshipTO relationship = new RelationshipTO.Builder("type", RelationshipTO.End.RIGHT).
                otherEnd("otherType", "otherKey", "otherName").
                plainAttr(new Attr.Builder("plain").value("value").build()).
                plainAttrs(List.of(new Attr.Builder("plain2").value("value").build())).
                build();

        assertEquals("type", relationship.getType());
        assertEquals(RelationshipTO.End.RIGHT, relationship.getEnd());
        assertEquals("otherType", relationship.getOtherEndType());
        assertEquals("otherKey", relationship.getOtherEndKey());
        assertEquals("otherName", relationship.getOtherEndName());
        assertEquals(2, relationship.getPlainAttrs().size());
        assertTrue(relationship.getPlainAttr("plain").isPresent());
        assertFalse(relationship.getPlainAttr("other").isPresent());
        assertFalse(relationship.getDerAttr("other").isPresent());
        assertTrue(relationship.toString().contains("type"));
    }

    @Test
    void relationshipDefaults() {
        RelationshipTO relationship = new RelationshipTO.Builder("type").build();

        assertEquals(RelationshipTO.End.LEFT, relationship.getEnd());
    }

    @Test
    void membership() {
        MembershipTO membership = new MembershipTO.Builder("group").
                groupName("groupName").
                plainAttr(new Attr.Builder("plain").value("value").build()).
                plainAttrs(List.of(new Attr.Builder("plain2").value("value").build())).
                build();

        assertEquals("group", membership.getGroupKey());
        assertEquals("groupName", membership.getGroupName());
        assertEquals(2, membership.getPlainAttrs().size());
        assertTrue(membership.getPlainAttr("plain").isPresent());
        assertFalse(membership.getPlainAttr("other").isPresent());
        assertFalse(membership.getDerAttr("other").isPresent());
        assertTrue(membership.toString().contains("group"));
    }

    @Test
    void relationshipsEqualsAndHashCode() {
        RelationshipTO first = new RelationshipTO.Builder("type").otherEnd("other").build();
        RelationshipTO second = new RelationshipTO.Builder("type").otherEnd("other").build();
        RelationshipTO other = new RelationshipTO.Builder("type").otherEnd("different").build();

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

    @Test
    void membershipsEqualsAndHashCode() {
        MembershipTO first = new MembershipTO.Builder("group").build();
        MembershipTO second = new MembershipTO.Builder("group").build();
        MembershipTO other = new MembershipTO.Builder("other").build();

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }

    @Test
    void linkedAccount() {
        LinkedAccountTO account = new LinkedAccountTO.Builder("key", "resource", "connObjectKeyValue").
                username("username").
                password("password").
                suspended(true).
                build();
        account.getPlainAttrs().add(new Attr.Builder("plain").value("value").build());

        assertEquals("key", account.getKey());
        assertEquals("resource", account.getResource());
        assertEquals("connObjectKeyValue", account.getConnObjectKeyValue());
        assertEquals("username", account.getUsername());
        assertEquals("password", account.getPassword());
        assertTrue(account.isSuspended());
        assertTrue(account.getPlainAttr("plain").isPresent());
        assertFalse(account.getPlainAttr("other").isPresent());
    }
}
