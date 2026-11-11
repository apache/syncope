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
package org.apache.syncope.core.logic.saml2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.saml2.SAML2LoginResponse;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIUserTemplate;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.api.rules.InboundMatch;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SAML2SP4UIUserManagerTest {

    private SAML2SP4UIIdPDAO idpDAO;

    private InboundMatcher inboundMatcher;

    private UserDAO userDAO;

    private ImplementationDAO implementationDAO;

    private IntAttrNameParser intAttrNameParser;

    private TemplateUtils templateUtils;

    private UserProvisioningManager provisioningManager;

    private UserDataBinder binder;

    private SAML2SP4UIUserManager userManager;

    @BeforeEach
    void setUp() {
        idpDAO = mock(SAML2SP4UIIdPDAO.class);
        inboundMatcher = mock(InboundMatcher.class);
        userDAO = mock(UserDAO.class);
        implementationDAO = mock(ImplementationDAO.class);
        intAttrNameParser = mock(IntAttrNameParser.class);
        templateUtils = mock(TemplateUtils.class);
        provisioningManager = mock(UserProvisioningManager.class);
        binder = mock(UserDataBinder.class);

        userManager = new SAML2SP4UIUserManager(
                idpDAO,
                inboundMatcher,
                userDAO,
                implementationDAO,
                intAttrNameParser,
                templateUtils,
                provisioningManager,
                binder);
    }

    private static SAML2SP4UIIdP idp() {
        SAML2SP4UIIdP idp = mock(SAML2SP4UIIdP.class);
        when(idp.getKey()).thenReturn("idpKey");
        when(idp.getItems()).thenReturn(List.of());
        when(idp.getActions()).thenReturn(List.of());
        when(idp.getConnObjectKeyItem()).thenReturn(Optional.empty());
        return idp;
    }

    private static Item item(final String intAttrName, final String extAttrName) {
        Item item = new Item();
        item.setIntAttrName(intAttrName);
        item.setExtAttrName(extAttrName);
        return item;
    }

    private static IntAttrName field(final String name) {
        IntAttrName intAttrName = mock(IntAttrName.class);
        when(intAttrName.getField()).thenReturn(name);
        return intAttrName;
    }

    private static IntAttrName plainSchema(final String schemaKey) {
        Schema schema = mock(Schema.class);
        when(schema.getKey()).thenReturn(schemaKey);

        IntAttrName intAttrName = mock(IntAttrName.class);
        when(intAttrName.getSchemaInfo()).thenReturn(new IntAttrName.SchemaInfo(schema, SchemaType.PLAIN));
        return intAttrName;
    }

    @Test
    void findMatchingUserReturnsMatchingUsernames() {
        User john = mock(User.class);
        when(john.getUsername()).thenReturn("john");
        User jane = mock(User.class);
        when(jane.getUsername()).thenReturn("jane");

        Item connObjectKeyItem = item("username", "NameID");
        SAML2SP4UIIdP idp = idp();
        when(idp.getConnObjectKeyItem()).thenReturn(Optional.of(connObjectKeyItem));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");

        when(inboundMatcher.matchByConnObjectKeyValue(
                connObjectKeyItem, "value", AnyTypeKind.USER, null, false)).
                thenReturn(List.of(new InboundMatch(MatchType.ANY, john), new InboundMatch(MatchType.ANY, jane)));

        assertEquals(List.of("john", "jane"), userManager.findMatchingUser("value", "idpKey"));
    }

    @Test
    void findMatchingUserSkipsMatchesWithoutAny() {
        User john = mock(User.class);
        when(john.getUsername()).thenReturn("john");

        Item connObjectKeyItem = item("username", "NameID");
        SAML2SP4UIIdP idp = idp();
        when(idp.getConnObjectKeyItem()).thenReturn(Optional.of(connObjectKeyItem));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");

        when(inboundMatcher.matchByConnObjectKeyValue(
                connObjectKeyItem, "value", AnyTypeKind.USER, null, false)).
                thenReturn(List.of(
                        new InboundMatch(MatchType.ANY, mock(LinkedAccount.class)),
                        new InboundMatch(MatchType.ANY, john)));

        assertEquals(List.of("john"), userManager.findMatchingUser("value", "idpKey"));
    }

    @Test
    void findMatchingUserWithoutIdPOrConnObjectKey() {
        assertTrue(userManager.findMatchingUser("value", "missing").isEmpty());

        doReturn(Optional.of(idp())).when(idpDAO).findById("idpKey");
        assertTrue(userManager.findMatchingUser("value", "idpKey").isEmpty());
    }

    @Test
    void fillSetsUsername() throws Exception {
        SAML2SP4UIIdP idp = idp();
        when(idp.getItems()).thenReturn(List.of(item("username", "email")));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");
        doReturn(field("username")).when(intAttrNameParser).parse("username", AnyTypeKind.USER);

        SAML2LoginResponse response = new SAML2LoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john@example.com").build());

        UserTO userTO = new UserTO();
        userManager.fill("idpKey", response, userTO);

        assertEquals("john@example.com", userTO.getUsername());
    }

    @Test
    void fillAddsPlainAttribute() throws Exception {
        SAML2SP4UIIdP idp = idp();
        when(idp.getItems()).thenReturn(List.of(item("firstname", "given_name")));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");
        doReturn(plainSchema("firstname")).when(intAttrNameParser).parse("firstname", AnyTypeKind.USER);

        SAML2LoginResponse response = new SAML2LoginResponse();
        response.getAttrs().add(new Attr.Builder("given_name").values("John", "Johnny").build());

        UserTO userTO = new UserTO();
        userManager.fill("idpKey", response, userTO);

        Attr firstname = userTO.getPlainAttr("firstname").orElseThrow();
        assertEquals(List.of("John", "Johnny"), firstname.getValues());
    }

    @Test
    void fillReplacesExistingPlainAttribute() throws Exception {
        SAML2SP4UIIdP idp = idp();
        when(idp.getItems()).thenReturn(List.of(item("firstname", "given_name")));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");
        doReturn(plainSchema("firstname")).when(intAttrNameParser).parse("firstname", AnyTypeKind.USER);

        SAML2LoginResponse response = new SAML2LoginResponse();
        response.getAttrs().add(new Attr.Builder("given_name").value("John").build());

        UserTO userTO = new UserTO();
        userTO.getPlainAttrs().add(new Attr.Builder("firstname").value("Old").build());

        userManager.fill("idpKey", response, userTO);

        assertEquals(List.of("John"), userTO.getPlainAttr("firstname").orElseThrow().getValues());
    }

    @Test
    void fillIgnoresInvalidIntAttrName() throws Exception {
        SAML2SP4UIIdP idp = idp();
        when(idp.getItems()).thenReturn(List.of(item("invalid", "email")));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");
        when(intAttrNameParser.parse("invalid", AnyTypeKind.USER)).thenThrow(new ParseException("invalid", 0));

        SAML2LoginResponse response = new SAML2LoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john@example.com").build());

        UserTO userTO = new UserTO();
        userManager.fill("idpKey", response, userTO);

        assertEquals(null, userTO.getUsername());
        assertTrue(userTO.getPlainAttrs().isEmpty());
    }

    @Test
    void fillIgnoresUnknownIdP() {
        SAML2LoginResponse response = new SAML2LoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john@example.com").build());

        UserTO userTO = new UserTO();
        userManager.fill("missing", response, userTO);

        assertEquals(null, userTO.getUsername());
        assertTrue(userTO.getPlainAttrs().isEmpty());
    }

    @Test
    void createUsesNameIDAndRootRealm() {
        SAML2SP4UIIdP idp = idp();
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");

        when(provisioningManager.create(
                any(UserCR.class), eq(false), eq("nameID"), eq(SAML2SP4UIUserManager.SAML2SP_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("created", List.of()));

        UserTO created = new UserTO();
        created.setUsername("nameID");
        when(binder.getUserTO("created")).thenReturn(created);

        assertEquals("nameID", userManager.create(idp, new SAML2LoginResponse(), "nameID"));

        ArgumentCaptor<UserCR> userCR = ArgumentCaptor.forClass(UserCR.class);
        verify(provisioningManager).create(
                userCR.capture(), eq(false), eq("nameID"), eq(SAML2SP4UIUserManager.SAML2SP_CONTEXT));
        assertEquals("nameID", userCR.getValue().getUsername());
        assertEquals(SyncopeConstants.ROOT_REALM, userCR.getValue().getRealm());
    }

    @Test
    void createUsesUsernameFromResponse() throws Exception {
        SAML2SP4UIIdP idp = idp();
        when(idp.getItems()).thenReturn(List.of(item("username", "email")));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");
        doReturn(field("username")).when(intAttrNameParser).parse("username", AnyTypeKind.USER);

        SAML2LoginResponse response = new SAML2LoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john").build());

        when(provisioningManager.create(
                any(UserCR.class), eq(false), eq("john"), eq(SAML2SP4UIUserManager.SAML2SP_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("created", List.of()));

        UserTO created = new UserTO();
        created.setUsername("john");
        when(binder.getUserTO("created")).thenReturn(created);

        assertEquals("john", userManager.create(idp, response, "nameID"));
    }

    @Test
    void createAppliesUserTemplate() {
        SAML2SP4UIIdP idp = idp();
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");

        SAML2SP4UIUserTemplate template = mock(SAML2SP4UIUserTemplate.class);
        when(template.get()).thenReturn(new UserTO());
        when(idp.getUserTemplate()).thenReturn(template);

        when(provisioningManager.create(
                any(UserCR.class), eq(false), eq("nameID"), eq(SAML2SP4UIUserManager.SAML2SP_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("created", List.of()));

        UserTO created = new UserTO();
        created.setUsername("created");
        when(binder.getUserTO("created")).thenReturn(created);

        userManager.create(idp, new SAML2LoginResponse(), "nameID");

        verify(templateUtils).apply(any(UserCR.class), any());
    }

    @Test
    void updateResolvesUserAndReturnsUpdatedUsername() throws Exception {
        SAML2SP4UIIdP idp = idp();
        when(idp.getItems()).thenReturn(List.of(item("username", "email")));
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");
        doReturn(field("username")).when(intAttrNameParser).parse("username", AnyTypeKind.USER);

        UserTO current = new UserTO();
        current.setKey("key");
        current.setUsername("old");
        when(userDAO.findKey("old")).thenReturn(Optional.of("key"));
        when(binder.getUserTO("key")).thenReturn(current);

        UserUR userUR = new UserUR();
        userUR.setKey("key");
        when(provisioningManager.update(
                any(UserUR.class), eq(false), eq("new"), eq(SAML2SP4UIUserManager.SAML2SP_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(userUR, List.of()));

        UserTO updated = new UserTO();
        updated.setUsername("new");
        when(binder.getUserTO("key")).thenReturn(current, updated);

        SAML2LoginResponse response = new SAML2LoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("new").build());

        assertEquals("new", userManager.update("old", idp, response));
    }

    @Test
    void updateWithoutChangesDoesNotProvision() throws Exception {
        SAML2SP4UIIdP idp = idp();
        doReturn(Optional.of(idp)).when(idpDAO).findById("idpKey");

        UserTO current = new UserTO();
        current.setKey("key");
        current.setUsername("john");
        when(userDAO.findKey("john")).thenReturn(Optional.of("key"));
        when(binder.getUserTO("key")).thenReturn(current);

        assertEquals("john", userManager.update("john", idp, new SAML2LoginResponse()));

        verify(provisioningManager, never()).
                update(any(UserUR.class), eq(false), any(String.class), any(String.class));
    }

    @Test
    void updateMissingUser() {
        when(userDAO.findKey("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userManager.update("missing", idp(), new SAML2LoginResponse()));
        verify(provisioningManager, never()).
                update(any(UserUR.class), eq(false), any(String.class), any(String.class));
    }
}
