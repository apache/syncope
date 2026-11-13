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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.junit.jupiter.api.Test;

class RequestBuildersTest {

    @Test
    void userCR() {
        UserCR user = new UserCR.Builder("/", "rossini").
                password("password").
                storePassword(false).
                securityQuestion("question").
                securityAnswer("answer").
                mustChangePassword(true).
                membership(new MembershipTO.Builder("group").build()).
                memberships(new MembershipTO.Builder("group2").build()).
                role("role").
                roles("role2", "role3").
                linkedAccount(new LinkedAccountTO.Builder("resource", "key").build()).
                uManager("uManager").
                gManager("gManager").
                auxClass("aux").
                auxClasses("aux2", "aux3").
                plainAttr(new Attr.Builder("plain").value("value").build()).
                resource("resource").
                relationship(new RelationshipTO.Builder("type").otherEnd("other").build()).
                build();

        assertEquals("/", user.getRealm());
        assertEquals("rossini", user.getUsername());
        assertEquals("password", user.getPassword());
        assertFalse(user.isStorePassword());
        assertEquals("question", user.getSecurityQuestion());
        assertEquals("answer", user.getSecurityAnswer());
        assertTrue(user.isMustChangePassword());
        assertEquals(2, user.getMemberships().size());
        assertEquals(Set.of("role", "role2", "role3"), user.getRoles());
        assertEquals(1, user.getLinkedAccounts().size());
        assertEquals("uManager", user.getuManager());
        assertEquals("gManager", user.getgManager());
        assertTrue(user.getAuxClasses().containsAll(Set.of("aux", "aux2", "aux3")));
        assertTrue(user.getPlainAttr("plain").isPresent());
        assertEquals(1, user.getResources().size());
        assertEquals(1, user.getRelationships().size());
        assertEquals(UserCR.class.getName(), user.getDiscriminator());
    }

    @Test
    void groupCR() {
        GroupCR group = new GroupCR.Builder("/", "name").
                typeExtension(new TypeExtensionTO()).
                typeExtensions(new TypeExtensionTO(), new TypeExtensionTO()).
                auxClasses(List.of("aux")).
                plainAttrs(List.of(new Attr.Builder("plain").value("value").build())).
                resources(List.of("resource")).
                relationships(List.of(new RelationshipTO.Builder("type").otherEnd("other").build())).
                build();

        assertEquals("/", group.getRealm());
        assertEquals("name", group.getName());
        assertEquals(3, group.getTypeExtensions().size());
        assertEquals(1, group.getAuxClasses().size());
        assertEquals(1, group.getPlainAttrs().size());
        assertEquals(1, group.getResources().size());
        assertEquals(1, group.getRelationships().size());
        assertEquals(GroupCR.class.getName(), group.getDiscriminator());
    }

    @Test
    void anyObjectCR() {
        AnyObjectCR anyObject = new AnyObjectCR.Builder("/", "PRINTER", "name").
                membership(new MembershipTO.Builder("group").build()).
                memberships(List.of(new MembershipTO.Builder("group2").build())).
                build();

        assertEquals("/", anyObject.getRealm());
        assertEquals("PRINTER", anyObject.getType());
        assertEquals("name", anyObject.getName());
        assertEquals(2, anyObject.getMemberships().size());
        assertEquals(AnyObjectCR.class.getName(), anyObject.getDiscriminator());
    }

    @Test
    void anyCREmpty() {
        AnyObjectCR anyObject = new AnyObjectCR();

        assertTrue(anyObject.getPlainAttr("plain").isEmpty());
        assertTrue(anyObject.getDerAttr("derived").isEmpty());
        assertTrue(anyObject.getRelationship("type", "other").isEmpty());
        assertTrue(anyObject.getDerAttrs().isEmpty());
    }

    @Test
    void userUR() {
        UserUR user = new UserUR.Builder("key").
                username(new StringReplacePatchItem.Builder().value("username").build()).
                password(new PasswordPatch.Builder().value("password").build()).
                securityQuestion(new StringReplacePatchItem.Builder().value("question").build()).
                securityAnswer(new StringReplacePatchItem.Builder().value("answer").build()).
                mustChangePassword(new BooleanReplacePatchItem.Builder().value(true).build()).
                membership(new MembershipUR.Builder("group").build()).
                memberships(List.of(new MembershipUR.Builder("group2").build())).
                role(new StringPatchItem.Builder().value("role").build()).
                roles(List.of(new StringPatchItem.Builder().value("role2").build())).
                auxClasses(List.of(new StringPatchItem.Builder().value("aux").build())).
                plainAttrs(List.of(new AttrPatch.Builder(new Attr.Builder("plain").value("value").build()).build())).
                resources(List.of(new StringPatchItem.Builder().value("resource").build())).
                relationships(List.of(new RelationshipUR.Builder("type").otherEnd("other").build())).
                build();

        assertEquals("key", user.getKey());
        assertEquals("username", user.getUsername().getValue());
        assertEquals("password", user.getPassword().getValue());
        assertEquals("question", user.getSecurityQuestion().getValue());
        assertEquals("answer", user.getSecurityAnswer().getValue());
        assertTrue(user.getMustChangePassword().getValue());
        assertEquals(2, user.getMemberships().size());
        assertEquals(2, user.getRoles().size());
        assertEquals(1, user.getAuxClasses().size());
        assertEquals(1, user.getPlainAttrs().size());
        assertEquals(1, user.getResources().size());
        assertEquals(1, user.getRelationships().size());
        assertEquals(UserUR.class.getName(), user.getDiscriminator());
    }

    @Test
    void userUREmpty() {
        UserUR user = new UserUR();
        assertTrue(user.isEmpty());
        assertFalse(user.isEmptyButPassword());
        assertFalse(user.requiresApproval());

        user.setPassword(new PasswordPatch.Builder().value("password").build());
        assertTrue(user.isEmptyButPassword());
        assertFalse(user.isEmpty());
    }

    @Test
    void userURRequiresApproval() {
        UserUR user = new UserUR();
        user.setgManager(new StringReplacePatchItem.Builder().value("gManager").build());
        assertTrue(user.requiresApproval());

        user = new UserUR();
        user.getRoles().add(new StringPatchItem.Builder().value("role").build());
        assertTrue(user.requiresApproval());
    }

    @Test
    void groupUR() {
        TypeExtensionTO named = new TypeExtensionTO();
        named.setAnyType("USER");

        GroupUR group = new GroupUR.Builder("key").
                name(new StringReplacePatchItem.Builder().value("name").build()).
                typeExtension(named).
                typeExtensions(List.of(new TypeExtensionTO())).
                build();

        assertEquals("key", group.getKey());
        assertEquals("name", group.getName().getValue());
        assertEquals(2, group.getTypeExtensions().size());
        assertTrue(group.getTypeExtension("USER").isPresent());
        assertFalse(group.getTypeExtension(null).isPresent());
        assertFalse(group.isEmpty());
    }

    @Test
    void anyObjectUR() {
        AnyObjectUR anyObject = new AnyObjectUR.Builder("key").
                name(new StringReplacePatchItem.Builder().value("name").build()).
                membership(new MembershipUR.Builder("group").build()).
                memberships(List.of(new MembershipUR.Builder("group2").build())).
                build();

        assertEquals("key", anyObject.getKey());
        assertEquals("name", anyObject.getName().getValue());
        assertEquals(2, anyObject.getMemberships().size());
        assertFalse(anyObject.isEmpty());
    }
}
