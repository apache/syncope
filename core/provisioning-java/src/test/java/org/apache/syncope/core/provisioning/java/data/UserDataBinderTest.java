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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserDataBinderTest extends AbstractTest {

    private static final String ROSSINI_KEY = "1417acbe-cbf6-4277-9372-e75e04f97000";

    private static final String VERDI_KEY = "74cd8ece-715a-44a4-a736-e17b46c4e7e6";

    private static final String PRINTER_KEY = "8559d14d-58c2-46eb-a2d4-a7d35161e8f8";

    private static final String CITIZEN_KEY = "29f96485-729e-4d31-88a1-6fc60e4677f3";

    private static final String ART_DIRECTOR_KEY = "ece66293-8f31-4a84-8e8d-23da36e70846";

    private static final String ADDITIONAL_KEY = "034740a9-fa10-453b-af37-dc7897e98fb1";

    private static final String SECURITY_QUESTION_KEY = "887028ea-66fc-41e7-b397-620d7ea6dfbb";

    @BeforeAll
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = Stream.concat(
                IdRepoEntitlement.values().stream(),
                Stream.of(AnyEntitlement.READ.getFor("PRINTER"))).
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterAll
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Autowired
    private UserDataBinder binder;

    @Autowired
    private RelationshipTypeDataBinder relationshipTypeDataBinder;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private EncryptorManager encryptorManager;

    @Autowired
    private EntityFactory entityFactory;

    @Test
    void createFull() {
        UserCR userCR = new UserCR.Builder("/even", "fullUser").
                password("Password123!").
                storePassword(true).
                securityQuestion(SECURITY_QUESTION_KEY).
                securityAnswer("secret answer").
                mustChangePassword(true).
                uManager(VERDI_KEY).
                auxClasses("other", "csv", "not-existing").
                plainAttr(new Attr.Builder("fullname").value("Full User").build()).
                plainAttr(new Attr.Builder("userId").value("full.user@example.com").build()).
                plainAttr(new Attr.Builder("surname").value(AnyTypeKind.USER.name()).build()).
                plainAttr(new Attr.Builder("firstname").value("Full").build()).
                plainAttr(new Attr.Builder("email").value("full.user@example.com").build()).
                plainAttr(new Attr.Builder("cool").value("true").build()).
                plainAttr(new Attr.Builder("gender").value("M").build()).
                plainAttr(new Attr.Builder("aLong").value("42").build()).
                plainAttr(new Attr.Builder("loginDate").values(List.of("2020-01-01", "2021-02-02")).build()).
                plainAttr(new Attr.Builder("uselessReadonly").value("ignored").build()).
                plainAttr(new Attr.Builder("not-existing").value("ignored").build()).
                resource("ws-target-resource-1").
                resource("not-existing").
                relationship(new RelationshipTO.Builder("neighborhood").
                        otherEnd(PRINTER_KEY).
                        build()).
                membership(new MembershipTO.Builder(ADDITIONAL_KEY).
                        plainAttr(new Attr.Builder("obscure").value("membership secret").build()).
                        build()).
                membership(new MembershipTO.Builder(ART_DIRECTOR_KEY).build()).
                role("Other").
                role("not-existing").
                build();
        LinkedAccountTO linkedAccount = new LinkedAccountTO.Builder("ws-target-resource-1", "full-user").
                username("fullUser").
                password("linkedPassword").
                suspended(true).
                build();
        linkedAccount.getPlainAttrs().add(new Attr.Builder("email").value("linked@example.com").build());
        userCR.getLinkedAccounts().add(linkedAccount);

        User user = entityFactory.newEntity(User.class);
        binder.create(user, userCR);
        user = userDAO.save(user);
        entityManager.flush();

        User actual = userDAO.findById(user.getKey()).orElseThrow();
        assertEquals("fullUser", actual.getUsername());
        assertTrue(actual.isMustChangePassword());
        assertEquals("/even", actual.getRealm().getFullPath());
        assertEquals(VERDI_KEY, actual.getuManager().getKey());
        assertNull(actual.getgManager());
        assertEquals(SECURITY_QUESTION_KEY, actual.getSecurityQuestion().getKey());
        assertTrue(encryptorManager.getInstance().verify(
                "Password123!", actual.getCipherAlgorithm(), actual.getPassword()));

        assertTrue(actual.getRoles().stream().anyMatch(role -> "Other".equals(role.getKey())));

        assertEquals(2, actual.getAuxClasses().size());
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "other".equals(auxClass.getKey())));
        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "csv".equals(auxClass.getKey())));

        assertEquals("Full", actual.getPlainAttr("firstname").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("Full User", actual.getPlainAttr("fullname").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals(2, actual.getPlainAttr("loginDate").orElseThrow().getValuesAsStrings().size());
        assertTrue(actual.getPlainAttr("uselessReadonly").isEmpty());
        assertTrue(actual.getPlainAttr("not-existing").isEmpty());

        assertEquals(1, actual.getResources().size());
        assertEquals("ws-target-resource-1", actual.getResources().getFirst().getKey());

        URelationship relationship = actual.getRelationship(
                relationshipTypeDAO.findById("neighborhood").orElseThrow(), PRINTER_KEY).orElseThrow();
        assertNotNull(relationship);

        UMembership additional = actual.getMembership(ADDITIONAL_KEY).orElseThrow();
        assertTrue(encryptorManager.getInstance().verify(
                "membership secret",
                plainSchemaDAO.findById("obscure").orElseThrow().getCipherAlgorithm(),
                actual.getPlainAttr("obscure", additional).orElseThrow().getValuesAsStrings().getFirst()));
        assertNotNull(actual.getMembership(ART_DIRECTOR_KEY).orElseThrow());

        assertEquals(1, actual.getLinkedAccounts().size());
        assertEquals("fullUser", actual.getLinkedAccounts().getFirst().getUsername());
        assertTrue(actual.getLinkedAccounts().getFirst().isSuspended());
    }

    @Test
    void updateFull() {
        UserUR userUR = new UserUR.Builder(ROSSINI_KEY).
                username(new StringReplacePatchItem.Builder().value("rossini2").build()).
                password(new PasswordPatch.Builder().
                        value("NewPassword123!").
                        onSyncope(true).
                        resource("ws-target-resource-1").
                        build()).
                securityQuestion(new StringReplacePatchItem.Builder().value(SECURITY_QUESTION_KEY).build()).
                securityAnswer(new StringReplacePatchItem.Builder().value("new answer").build()).
                mustChangePassword(new BooleanReplacePatchItem.Builder().value(true).build()).
                uManager(new StringReplacePatchItem.Builder().value(VERDI_KEY).build()).
                realm(new StringReplacePatchItem.Builder().value("/even").build()).
                auxClass(new StringPatchItem.Builder().value("csv").operation(PatchOperation.ADD_REPLACE).build()).
                auxClass(new StringPatchItem.Builder().value("other").operation(PatchOperation.DELETE).build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("firstname").value("Gioacchino2").build()).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("aLong").value("42").build()).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("cool").value("true").build()).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("gender").value("F").build()).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("loginDate").build()).
                        operation(PatchOperation.DELETE).
                        build()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("not-existing").value("ignored").build()).
                        build()).
                resource(new StringPatchItem.Builder().
                        value("ws-target-resource-1").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                resource(new StringPatchItem.Builder().
                        value("not-existing").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                relationship(new RelationshipUR.Builder("neighborhood").
                        otherEnd(PRINTER_KEY).
                        build()).
                membership(new MembershipUR.Builder(ART_DIRECTOR_KEY).build()).
                membership(new MembershipUR.Builder("37d15e4c-cdc1-460b-a591-8505c8133806").
                        operation(PatchOperation.DELETE).
                        build()).
                membership(new MembershipUR.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").
                        operation(PatchOperation.DELETE).
                        build()).
                role(new StringPatchItem.Builder().
                        value("User reviewer").
                        operation(PatchOperation.ADD_REPLACE).
                        build()).
                role(new StringPatchItem.Builder().
                        value("Other").
                        operation(PatchOperation.DELETE).
                        build()).
                build();
        LinkedAccountTO updatedLinkedAccount = new LinkedAccountTO.Builder("ws-target-resource-1", "rossini2").
                username("rossini2").
                password("linkedPassword").
                suspended(false).
                build();
        updatedLinkedAccount.getPlainAttrs().
                add(new Attr.Builder("email").value("linked.rossini@example.com").build());
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                linkedAccountTO(updatedLinkedAccount).
                build());

        binder.update(userDAO.findById(ROSSINI_KEY).orElseThrow(), userUR);
        entityManager.flush();

        User actual = userDAO.findById(ROSSINI_KEY).orElseThrow();
        assertEquals("rossini2", actual.getUsername());
        assertEquals("/even", actual.getRealm().getFullPath());
        assertTrue(actual.isMustChangePassword());
        assertEquals(VERDI_KEY, actual.getuManager().getKey());
        assertNull(actual.getgManager());
        assertEquals(SECURITY_QUESTION_KEY, actual.getSecurityQuestion().getKey());
        assertTrue(encryptorManager.getInstance().verify(
                "NewPassword123!", actual.getCipherAlgorithm(), actual.getPassword()));

        assertTrue(actual.getRoles().stream().anyMatch(role -> "User reviewer".equals(role.getKey())));
        assertFalse(actual.getRoles().stream().anyMatch(role -> "Other".equals(role.getKey())));

        assertTrue(actual.getAuxClasses().stream().anyMatch(auxClass -> "csv".equals(auxClass.getKey())));
        assertFalse(actual.getAuxClasses().stream().anyMatch(auxClass -> "other".equals(auxClass.getKey())));

        assertEquals("Gioacchino2", actual.getPlainAttr("firstname").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("42", actual.getPlainAttr("aLong").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("true", actual.getPlainAttr("cool").orElseThrow().getValuesAsStrings().getFirst());
        assertEquals("F", actual.getPlainAttr("gender").orElseThrow().getValuesAsStrings().getFirst());
        assertTrue(actual.getPlainAttr("loginDate").isEmpty());

        assertEquals(2, actual.getResources().size());
        assertTrue(actual.getResources().stream().
                anyMatch(resource -> "ws-target-resource-1".equals(resource.getKey())));

        assertNotNull(actual.getRelationship(
                relationshipTypeDAO.findById("neighborhood").orElseThrow(), PRINTER_KEY).orElseThrow());
        assertNotNull(actual.getMembership(ART_DIRECTOR_KEY).orElseThrow());
        assertTrue(actual.getMembership("37d15e4c-cdc1-460b-a591-8505c8133806").isEmpty());
        assertTrue(actual.getMembership("f779c0d4-633b-4be5-8f57-32eb478a3ca5").isEmpty());

        assertEquals(1, actual.getLinkedAccounts().size());
        assertEquals("rossini2", actual.getLinkedAccounts().getFirst().getUsername());
        assertFalse(actual.getLinkedAccounts().getFirst().isSuspended());
    }

    @Test
    void updateManagersAndLinkedAccounts() {
        UserUR add = new UserUR.Builder(ROSSINI_KEY).
                uManager(new StringReplacePatchItem.Builder().value(VERDI_KEY).build()).
                build();
        add.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                linkedAccountTO(new LinkedAccountTO.Builder("ws-target-resource-1", "rossini-linked").
                        username("rossini-linked").
                        build()).
                build());

        binder.update(userDAO.findById(ROSSINI_KEY).orElseThrow(), add);
        entityManager.flush();

        User updated = userDAO.findById(ROSSINI_KEY).orElseThrow();
        assertEquals(VERDI_KEY, updated.getuManager().getKey());
        assertNull(updated.getgManager());
        assertEquals(1, updated.getLinkedAccounts().size());

        UserUR clearUserManager = new UserUR.Builder(ROSSINI_KEY).
                uManager(new StringReplacePatchItem.Builder().value(null).build()).
                build();
        binder.update(userDAO.findById(ROSSINI_KEY).orElseThrow(), clearUserManager);
        entityManager.flush();

        User withoutManager = userDAO.findById(ROSSINI_KEY).orElseThrow();
        assertNull(withoutManager.getuManager());
        assertNull(withoutManager.getgManager());

        UserUR addGroupManager = new UserUR.Builder(ROSSINI_KEY).
                gManager(new StringReplacePatchItem.Builder().value(CITIZEN_KEY).build()).
                build();
        binder.update(userDAO.findById(ROSSINI_KEY).orElseThrow(), addGroupManager);
        entityManager.flush();

        User groupManaged = userDAO.findById(ROSSINI_KEY).orElseThrow();
        assertNull(groupManaged.getuManager());
        assertEquals(CITIZEN_KEY, groupManaged.getgManager().getKey());

        UserUR remove = new UserUR.Builder(ROSSINI_KEY).
                gManager(new StringReplacePatchItem.Builder().value(null).build()).
                build();
        remove.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                operation(PatchOperation.DELETE).
                linkedAccountTO(new LinkedAccountTO.Builder("ws-target-resource-1", "rossini-linked").
                        username("rossini-linked").
                        build()).
                build());

        binder.update(userDAO.findById(ROSSINI_KEY).orElseThrow(), remove);
        entityManager.flush();

        User cleared = userDAO.findById(ROSSINI_KEY).orElseThrow();
        assertNull(cleared.getuManager());
        assertNull(cleared.getgManager());
        assertTrue(cleared.getLinkedAccounts().isEmpty());
    }

    @Test
    void updatePasswordNotOnSyncope() {
        UserUR userUR = new UserUR.Builder(ROSSINI_KEY).
                password(new PasswordPatch.Builder().
                        value("NotStoredPassword123!").
                        onSyncope(false).
                        resources("ws-target-resource-1", "ws-target-resource-2").
                        build()).
                build();

        binder.update(userDAO.findById(ROSSINI_KEY).orElseThrow(), userUR);
        entityManager.flush();

        User actual = userDAO.findById(ROSSINI_KEY).orElseThrow();
        assertTrue(encryptorManager.getInstance().verify(
                "password", actual.getCipherAlgorithm(), actual.getPassword()));
    }

    @Test
    void membershipWithAttrNotAllowed() {
        UserUR userUR = new UserUR.Builder("1417acbe-cbf6-4277-9372-e75e04f97000").build();

        // add 'obscure' to user (no membership): works because 'obscure' is from 'other', default class for USER
        userUR.getPlainAttrs().
                add(new AttrPatch.Builder(new Attr.Builder("obscure").value("testvalue").build()).build());

        // add 'obscure' to user (via 'artDirector' membership): does not work because 'obscure' is from 'other'
        // but 'artDirector' defines no type extension
        userUR.getMemberships().add(new MembershipUR.Builder("ece66293-8f31-4a84-8e8d-23da36e70846").
                plainAttr(new Attr.Builder("obscure").value("testvalue2").build()).build());

        assertThrows(
                InvalidEntityException.class,
                () -> binder.update(userDAO.findById(userUR.getKey()).orElseThrow(), userUR));
    }

    @Test
    void membershipWithAttr() {
        UserUR userUR = new UserUR.Builder("1417acbe-cbf6-4277-9372-e75e04f97000").build();

        // add 'obscure' (no membership): works because 'obscure' is from 'other', default class for USER
        userUR.getPlainAttrs().
                add(new AttrPatch.Builder(new Attr.Builder("obscure").value("testvalue").build()).build());

        // add 'obscure' (via 'additional' membership): that group defines type extension with classes 'other' and 'csv'
        userUR.getMemberships().add(new MembershipUR.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").
                plainAttr(new Attr.Builder("obscure").value("testvalue2").build()).build());

        binder.update(userDAO.findById(userUR.getKey()).orElseThrow(), userUR);

        User user = userDAO.findById(userUR.getKey()).orElseThrow();
        UMembership newM = user.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1").orElseThrow();
        assertEquals(1, user.getPlainAttrs(newM).size());

        assertNull(user.getPlainAttr("obscure").orElseThrow().getMembership());
        assertTrue(encryptorManager.getInstance().verify(
                "testvalue2",
                user.getCipherAlgorithm(),
                user.getPlainAttr("obscure", newM).orElseThrow().getValuesAsStrings().getFirst()));
    }

    @Test
    void relationshipWithAttr() {
        // first add type extension to neighborhood
        RelationshipType relType = relationshipTypeDAO.findById("neighborhood").orElseThrow();
        RelationshipTypeTO relTypeTO = relationshipTypeDataBinder.getRelationshipTypeTO(relType);

        TypeExtensionTO typeExt = new TypeExtensionTO();
        typeExt.setAnyType(AnyTypeKind.USER.name());
        typeExt.getAuxClasses().add("other");
        relTypeTO.getTypeExtensions().add(typeExt);

        relType = relationshipTypeDAO.save(relationshipTypeDataBinder.update(relType, relTypeTO));
        relTypeTO = relationshipTypeDataBinder.getRelationshipTypeTO(relType);
        assertEquals("other", relTypeTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().getFirst());

        // then add relationship with attribute
        UserUR userUR = new UserUR.Builder("1417acbe-cbf6-4277-9372-e75e04f97000").
                relationship(new RelationshipUR.Builder("neighborhood").
                        otherEnd("8559d14d-58c2-46eb-a2d4-a7d35161e8f8").
                        plainAttr(new Attr.Builder("obscure").value("testvalue3").build()).
                        build()).
                build();

        binder.update(userDAO.findById(userUR.getKey()).orElseThrow(), userUR);

        User user = userDAO.findById(userUR.getKey()).orElseThrow();
        URelationship newR = user.getRelationship(relType, "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").orElseThrow();
        assertEquals(1, user.getPlainAttrs(newR).size());

        assertTrue(encryptorManager.getInstance().verify(
                "testvalue3",
                user.getCipherAlgorithm(),
                user.getPlainAttr("obscure", newR).orElseThrow().getValuesAsStrings().getFirst()));
    }
}
