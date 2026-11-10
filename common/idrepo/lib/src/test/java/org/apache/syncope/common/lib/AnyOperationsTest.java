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
package org.apache.syncope.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.junit.jupiter.api.Test;

class AnyOperationsTest {

    @Test
    void diffUser() {
        UserTO original = new UserTO();
        original.setKey("key");
        original.setUsername("oldUsername");
        original.setPassword("oldPassword");
        original.setSecurityQuestion("oldQuestion");
        original.setMustChangePassword(true);
        original.getRoles().add("roleToRemove");
        original.getRoles().add("roleToKeep");
        original.getAuxClasses().add("auxToRemove");
        original.getResources().add("resourceToRemove");
        original.getPlainAttrs().add(new Attr.Builder("plain").value("oldValue").build());
        original.getPlainAttrs().add(new Attr.Builder("toRemove").value("value").build());
        original.getPlainAttrs().add(new Attr.Builder("onlyOriginal").value("value").build());
        original.getMemberships().add(new MembershipTO.Builder("groupToRemove").build());
        original.getLinkedAccounts().add(new LinkedAccountTO.Builder("resourceToRemove", "key").build());
        original.getRelationships().add(new RelationshipTO.Builder("typeToRemove").otherEnd("other").build());

        UserTO updated = new UserTO();
        updated.setKey("key");
        updated.setUsername("newUsername");
        updated.setPassword("newPassword");
        updated.setSecurityQuestion("newQuestion");
        updated.setMustChangePassword(false);
        updated.getRoles().add("roleToKeep");
        updated.getRoles().add("roleToAdd");
        updated.getAuxClasses().add("auxToAdd");
        updated.getResources().add("resourceToAdd");
        updated.getPlainAttrs().add(new Attr.Builder("plain").value("newValue").build());
        updated.getPlainAttrs().add(new Attr.Builder("toRemove").build());
        updated.getPlainAttrs().add(new Attr.Builder("onlyUpdated").value("value").build());
        updated.getMemberships().add(new MembershipTO.Builder("groupToAdd").build());
        updated.getLinkedAccounts().add(new LinkedAccountTO.Builder("resourceToAdd", "key").build());
        updated.getRelationships().add(new RelationshipTO.Builder("typeToAdd").otherEnd("other").build());

        UserUR diff = AnyOperations.diff(updated, original, false);

        assertEquals("key", diff.getKey());
        assertEquals("newUsername", diff.getUsername().getValue());
        assertEquals("newPassword", diff.getPassword().getValue());
        assertTrue(diff.getPassword().getResources().contains("resourceToAdd"));
        assertEquals("newQuestion", diff.getSecurityQuestion().getValue());
        assertNull(diff.getSecurityAnswer());
        assertFalse(diff.getMustChangePassword().getValue());

        assertTrue(diff.getRoles().contains(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("roleToRemove").build()));
        assertTrue(diff.getRoles().contains(
                new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value("roleToAdd").build()));

        assertTrue(diff.getAuxClasses().contains(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("auxToRemove").build()));
        assertTrue(diff.getAuxClasses().contains(
                new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value("auxToAdd").build()));

        assertTrue(diff.getResources().contains(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("resourceToRemove").build()));
        assertTrue(diff.getResources().contains(
                new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value("resourceToAdd").build()));

        assertEquals(PatchOperation.DELETE, plainAttrPatch(diff, "onlyOriginal", PatchOperation.DELETE).getOperation());
        assertEquals(PatchOperation.DELETE, plainAttrPatch(diff, "toRemove", PatchOperation.DELETE).getOperation());
        assertEquals("newValue",
                plainAttrPatch(diff, "plain", PatchOperation.ADD_REPLACE).getAttr().getValues().get(0));
        assertTrue(diff.getPlainAttrs().contains(plainAttrPatch(diff, "onlyUpdated", PatchOperation.ADD_REPLACE)));

        assertEquals(PatchOperation.DELETE, membership(diff.getMemberships(), "groupToRemove",
                PatchOperation.DELETE).getOperation());
        assertEquals(PatchOperation.ADD_REPLACE, membership(diff.getMemberships(), "groupToAdd",
                PatchOperation.ADD_REPLACE).getOperation());

        assertEquals(PatchOperation.DELETE, relationship(diff.getRelationships(), "typeToRemove",
                PatchOperation.DELETE).getOperation());
        assertEquals(PatchOperation.ADD_REPLACE, relationship(diff.getRelationships(), "typeToAdd",
                PatchOperation.ADD_REPLACE).getOperation());

        assertEquals(2, diff.getLinkedAccounts().size());
        assertEquals(PatchOperation.DELETE, linkedAccount(diff.getLinkedAccounts(), "resourceToRemove").getOperation());
        assertEquals(PatchOperation.ADD_REPLACE,
                linkedAccount(diff.getLinkedAccounts(), "resourceToAdd").getOperation());
    }

    @Test
    void diffUserIncremental() {
        UserTO original = new UserTO();
        original.setKey("key");
        original.setuManager("uManager");
        original.getRoles().add("roleToRemove");
        original.getAuxClasses().add("auxToRemove");
        original.getResources().add("resourceToRemove");
        original.getPlainAttrs().add(new Attr.Builder("toRemove").value("value").build());
        original.getMemberships().add(new MembershipTO.Builder("groupToRemove").build());
        original.getLinkedAccounts().add(new LinkedAccountTO.Builder("resourceToRemove", "key").build());
        original.getRelationships().add(new RelationshipTO.Builder("typeToRemove").otherEnd("other").build());

        UserTO updated = new UserTO();
        updated.setKey("key");
        updated.getPlainAttrs().add(new Attr.Builder("toRemove").build());

        UserUR diff = AnyOperations.diff(updated, original, true);

        assertNull(diff.getuManager());
        assertTrue(diff.getRoles().isEmpty());
        assertTrue(diff.getAuxClasses().isEmpty());
        assertTrue(diff.getResources().isEmpty());
        assertTrue(diff.getPlainAttrs().isEmpty());
        assertTrue(diff.getMemberships().isEmpty());
        assertTrue(diff.getLinkedAccounts().isEmpty());
        assertTrue(diff.getRelationships().isEmpty());
    }

    @Test
    void diffGroup() {
        GroupTO original = new GroupTO();
        original.setKey("key");
        original.setName("oldName");

        GroupTO updated = new GroupTO();
        updated.setKey("key");
        updated.setName("newName");
        updated.getTypeExtensions().add(new TypeExtensionTO());

        GroupUR diff = AnyOperations.diff(updated, original, false);

        assertEquals("newName", diff.getName().getValue());
        assertEquals(1, diff.getTypeExtensions().size());
    }

    @Test
    void diffAnyObject() {
        AnyObjectTO original = new AnyObjectTO();
        original.setKey("key");
        original.setName("oldName");
        original.getMemberships().add(new MembershipTO.Builder("groupToRemove").build());

        AnyObjectTO updated = new AnyObjectTO();
        updated.setKey("key");
        updated.setName("newName");
        updated.getMemberships().add(new MembershipTO.Builder("groupToAdd").build());

        AnyObjectUR diff = AnyOperations.diff(updated, original, false);

        assertEquals("newName", diff.getName().getValue());
        assertEquals(2, diff.getMemberships().size());
    }

    @Test
    void diffWithDifferentKeys() {
        UserTO updated = new UserTO();
        updated.setKey("first");

        UserTO original = new UserTO();
        original.setKey("second");

        assertThrows(IllegalArgumentException.class, () -> AnyOperations.diff(updated, original, false));
    }

    @Test
    void diffWithMissingKey() {
        UserTO updated = new UserTO();

        UserTO original = new UserTO();
        original.setKey("second");

        assertThrows(IllegalArgumentException.class, () -> AnyOperations.diff(updated, original, false));
    }

    @Test
    void genericDiff() {
        AnyUR user = AnyOperations.diff(new UserTO(), new UserTO(), false);
        AnyUR group = AnyOperations.diff(new GroupTO(), new GroupTO(), false);
        AnyUR anyObject = AnyOperations.diff(new AnyObjectTO(), new AnyObjectTO(), false);

        assertTrue(user instanceof UserUR);
        assertTrue(group instanceof GroupUR);
        assertTrue(anyObject instanceof AnyObjectUR);
    }

    @Test
    void genericDiffUnsupported() {
        AnyTO custom = new AnyTO() {

            @Override
            public String getDiscriminator() {
                return "custom";
            }
        };

        assertThrows(IllegalArgumentException.class,
                () -> AnyOperations.<AnyTO, AnyUR>diff(custom, custom, false));
    }

    @Test
    void patchUser() {
        Attr oldMembershipAttr = new Attr.Builder("memb").value("old").build();
        Attr newMembershipAttr = new Attr.Builder("memb").value("new").build();
        RelationshipTO oldRelationship = new RelationshipTO.Builder("typeToRemove").otherEnd("other").build();
        RelationshipTO newRelationship = new RelationshipTO.Builder("typeToAdd").otherEnd("other").build();
        LinkedAccountTO oldAccount = new LinkedAccountTO.Builder("resourceToRemove", "key").build();
        LinkedAccountTO newAccount = new LinkedAccountTO.Builder("resourceToAdd", "key").build();

        UserTO user = new UserTO();
        user.setKey("key");
        user.setRealm("/");
        user.setuManager("uManager");
        user.getAuxClasses().add("auxToRemove");
        user.getPlainAttrs().add(new Attr.Builder("plain").value("old").build());
        user.getPlainAttrs().add(new Attr.Builder("toRemove").value("value").build());
        user.getResources().add("resourceToRemove");
        user.getRoles().add("roleToRemove");
        user.getMemberships().add(new MembershipTO.Builder("groupToRemove").plainAttr(oldMembershipAttr).build());
        user.getRelationships().add(oldRelationship);
        user.getLinkedAccounts().add(oldAccount);

        UserUR req = new UserUR();
        req.setKey("key");
        req.setRealm(new StringReplacePatchItem.Builder().value("/new").build());
        req.setuManager(new StringReplacePatchItem.Builder().value("newUManager").build());
        req.setgManager(new StringReplacePatchItem.Builder().value("gManager").build());
        req.getAuxClasses().add(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("auxToRemove").build());
        req.getAuxClasses().add(
                new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value("auxToAdd").build());
        req.getPlainAttrs().add(new AttrPatch.Builder(
                new Attr.Builder("plain").value("new").build()).build());
        req.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder("toRemove").build()).
                operation(PatchOperation.DELETE).build());
        req.getResources().add(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("resourceToRemove").build());
        req.getResources().add(
                new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value("resourceToAdd").build());
        req.setPassword(new PasswordPatch.Builder().value("password").build());
        req.setUsername(new StringReplacePatchItem.Builder().value("username").build());
        req.getRoles().add(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value("roleToRemove").build());
        req.getRoles().add(
                new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value("roleToAdd").build());
        req.getMemberships().add(
                new MembershipUR.Builder("groupToRemove").operation(PatchOperation.DELETE).build());
        req.getMemberships().add(new MembershipUR.Builder("groupToAdd").
                operation(PatchOperation.ADD_REPLACE).plainAttr(newMembershipAttr).build());
        req.getRelationships().add(new RelationshipUR.Builder("typeToRemove").otherEnd("other").
                operation(PatchOperation.DELETE).build());
        req.getRelationships().add(new RelationshipUR.Builder("typeToAdd").otherEnd("other").
                operation(PatchOperation.ADD_REPLACE).
                plainAttr(new Attr.Builder("rel").value("value").build()).build());
        req.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                operation(PatchOperation.DELETE).linkedAccountTO(oldAccount).build());
        req.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                operation(PatchOperation.ADD_REPLACE).linkedAccountTO(newAccount).build());

        UserTO patched = AnyOperations.patch(user, req);

        assertNotSame(user, patched);
        assertEquals("key", patched.getKey());
        assertEquals("/new", patched.getRealm());
        assertEquals("newUManager", patched.getuManager());
        assertEquals("gManager", patched.getgManager());
        assertEquals(Set.of("auxToAdd"), patched.getAuxClasses());
        assertEquals(Set.of("resourceToAdd"), patched.getResources());
        assertEquals("password", patched.getPassword());
        assertEquals("username", patched.getUsername());
        assertEquals(Set.of("roleToAdd"), Set.copyOf(patched.getRoles()));
        assertEquals(1, patched.getPlainAttrs().size());
        assertTrue(patched.getPlainAttrs().contains(new Attr.Builder("plain").value("new").build()));
        assertEquals(1, patched.getMemberships().size());
        assertEquals("groupToAdd", patched.getMemberships().get(0).getGroupKey());
        assertTrue(patched.getMemberships().get(0).getPlainAttrs().contains(newMembershipAttr));
        assertEquals(1, patched.getRelationships().size());
        assertEquals("typeToAdd", patched.getRelationships().get(0).getType());
        assertTrue(patched.getRelationships().get(0).getPlainAttrs().contains(
                new Attr.Builder("rel").value("value").build()));
        assertEquals(1, patched.getLinkedAccounts().size());
        assertEquals("resourceToAdd", patched.getLinkedAccounts().get(0).getResource());

        assertEquals("old", user.getPlainAttrs().stream().
                filter(attr -> "plain".equals(attr.getSchema())).findFirst().orElseThrow().getValues().get(0));
    }

    @Test
    void patchGroup() {
        GroupTO group = new GroupTO();
        group.setKey("key");
        group.setName("oldName");
        group.getRelationships().add(new RelationshipTO.Builder("typeToRemove").otherEnd("other").build());

        GroupUR req = new GroupUR();
        req.setKey("key");
        req.setName(new StringReplacePatchItem.Builder().value("newName").build());
        req.getRelationships().add(new RelationshipUR.Builder("typeToRemove").otherEnd("other").
                operation(PatchOperation.DELETE).build());
        req.getRelationships().add(new RelationshipUR.Builder("typeToAdd").otherEnd("other").
                operation(PatchOperation.ADD_REPLACE).build());

        GroupTO patched = AnyOperations.patch(group, req);

        assertNotSame(group, patched);
        assertEquals("newName", patched.getName());
        assertEquals(1, patched.getRelationships().size());
        assertEquals("typeToAdd", patched.getRelationships().get(0).getType());
        assertEquals(1, group.getRelationships().size());
    }

    @Test
    void patchAnyObject() {
        AnyObjectTO anyObject = new AnyObjectTO();
        anyObject.setKey("key");
        anyObject.setName("oldName");
        anyObject.getMemberships().add(new MembershipTO.Builder("groupToRemove").build());
        anyObject.getRelationships().add(new RelationshipTO.Builder("typeToRemove").otherEnd("other").build());

        AnyObjectUR req = new AnyObjectUR();
        req.setKey("key");
        req.setName(new StringReplacePatchItem.Builder().value("newName").build());
        req.getMemberships().add(
                new MembershipUR.Builder("groupToRemove").operation(PatchOperation.DELETE).build());
        req.getMemberships().add(
                new MembershipUR.Builder("groupToAdd").operation(PatchOperation.ADD_REPLACE).build());
        req.getRelationships().add(new RelationshipUR.Builder("typeToRemove").otherEnd("other").
                operation(PatchOperation.DELETE).build());
        req.getRelationships().add(new RelationshipUR.Builder("typeToAdd").otherEnd("other").
                operation(PatchOperation.ADD_REPLACE).build());

        AnyObjectTO patched = AnyOperations.patch(anyObject, req);

        assertNotSame(anyObject, patched);
        assertEquals("newName", patched.getName());
        assertEquals(1, patched.getMemberships().size());
        assertEquals("groupToAdd", patched.getMemberships().get(0).getGroupKey());
        assertEquals(1, patched.getRelationships().size());
        assertEquals("typeToAdd", patched.getRelationships().get(0).getType());
    }

    @Test
    void patchGeneric() {
        UserTO user = new UserTO();
        user.setKey("key");
        UserUR userUR = new UserUR();
        userUR.setKey("key");
        userUR.setUsername(new StringReplacePatchItem.Builder().value("username").build());

        AnyTO patched = AnyOperations.patch(user, userUR);

        assertTrue(patched instanceof UserTO);
        assertEquals("username", ((UserTO) patched).getUsername());

        AnyTO custom = new AnyTO() {

            @Override
            public String getDiscriminator() {
                return "custom";
            }
        };
        AnyUR customUR = new AnyUR() {

            @Override
            public String getDiscriminator() {
                return "custom";
            }
        };

        assertNull(AnyOperations.patch(custom, customUR));
    }

    @Test
    void patchWithDifferentKeys() {
        UserTO user = new UserTO();
        user.setKey("first");

        UserUR req = new UserUR();
        req.setKey("second");

        assertThrows(IllegalArgumentException.class, () -> AnyOperations.patch(user, req));
    }

    @Test
    void patchWithMissingKey() {
        UserTO user = new UserTO();

        UserUR req = new UserUR();
        req.setKey("key");

        assertThrows(IllegalArgumentException.class, () -> AnyOperations.patch(user, req));
    }

    @Test
    void patchInvalidRelationshipsAndMemberships() {
        AnyObjectTO anyObject = new AnyObjectTO();
        anyObject.setKey("key");
        anyObject.getMemberships().add(new MembershipTO.Builder("group").build());
        anyObject.getRelationships().add(new RelationshipTO.Builder("type").otherEnd("other").build());

        AnyObjectUR req = new AnyObjectUR();
        req.setKey("key");
        req.getMemberships().add(new MembershipUR.Builder((String) null).
                operation(PatchOperation.ADD_REPLACE).build());
        req.getRelationships().add(new RelationshipUR.Builder((String) null).
                operation(PatchOperation.ADD_REPLACE).build());

        AnyObjectTO patched = AnyOperations.patch(anyObject, req);

        assertEquals(1, patched.getMemberships().size());
        assertEquals(1, patched.getRelationships().size());
    }

    @Test
    void patchInvalidLinkedAccount() {
        UserTO user = new UserTO();
        user.setKey("key");

        UserUR req = new UserUR();
        req.setKey("key");
        req.getLinkedAccounts().add(new LinkedAccountUR.Builder().operation(PatchOperation.ADD_REPLACE).build());

        assertTrue(AnyOperations.patch(user, req).getLinkedAccounts().isEmpty());
    }

    @Test
    void cleanEmptyAttrs() {
        UserTO user = new UserTO();
        user.getPlainAttrs().add(new Attr.Builder("empty").build());
        user.getPlainAttrs().add(new Attr.Builder("null").value((String) null).build());
        user.getPlainAttrs().add(new Attr.Builder("notEmpty").value("value").build());

        UserUR req = new UserUR();
        AnyOperations.cleanEmptyAttrs(user, req);

        assertEquals(2, req.getPlainAttrs().size());
        assertTrue(req.getPlainAttrs().stream().
                allMatch(patch -> patch.getOperation() == PatchOperation.DELETE));
        assertTrue(req.getPlainAttrs().stream().
                anyMatch(patch -> "empty".equals(patch.getAttr().getSchema())));
        assertTrue(req.getPlainAttrs().stream().
                anyMatch(patch -> "null".equals(patch.getAttr().getSchema())));
    }

    private static AttrPatch plainAttrPatch(
            final AnyUR anyUR, final String schema, final PatchOperation operation) {

        return anyUR.getPlainAttrs().stream().
                filter(patch -> patch.getAttr() != null
                && schema.equals(patch.getAttr().getSchema())
                && patch.getOperation() == operation).
                findFirst().orElseThrow();
    }

    private static MembershipUR membership(
            final Set<MembershipUR> memberships, final String group, final PatchOperation operation) {

        return memberships.stream().
                filter(membership -> group.equals(membership.getGroup())
                && membership.getOperation() == operation).
                findFirst().orElseThrow();
    }

    private static RelationshipUR relationship(
            final Set<RelationshipUR> relationships, final String type, final PatchOperation operation) {

        return relationships.stream().
                filter(relationship -> type.equals(relationship.getType())
                && relationship.getOperation() == operation).
                findFirst().orElseThrow();
    }

    private static LinkedAccountUR linkedAccount(
            final List<LinkedAccountUR> accounts, final String resource) {

        return accounts.stream().
                filter(account -> account.getLinkedAccountTO() != null
                && resource.equals(account.getLinkedAccountTO().getResource())).
                findFirst().orElseThrow();
    }
}
