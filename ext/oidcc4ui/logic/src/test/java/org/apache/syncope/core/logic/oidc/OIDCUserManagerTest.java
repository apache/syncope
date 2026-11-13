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
package org.apache.syncope.core.logic.oidc;

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
import org.apache.syncope.common.lib.oidc.OIDCLoginResponse;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIUserTemplate;
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

class OIDCUserManagerTest {

    private InboundMatcher inboundMatcher;

    private UserDAO userDAO;

    private ImplementationDAO implementationDAO;

    private IntAttrNameParser intAttrNameParser;

    private TemplateUtils templateUtils;

    private UserProvisioningManager provisioningManager;

    private UserDataBinder binder;

    private OIDCUserManager userManager;

    @BeforeEach
    void setUp() {
        inboundMatcher = mock(InboundMatcher.class);
        userDAO = mock(UserDAO.class);
        implementationDAO = mock(ImplementationDAO.class);
        intAttrNameParser = mock(IntAttrNameParser.class);
        templateUtils = mock(TemplateUtils.class);
        provisioningManager = mock(UserProvisioningManager.class);
        binder = mock(UserDataBinder.class);

        userManager = new OIDCUserManager(
                inboundMatcher,
                userDAO,
                implementationDAO,
                intAttrNameParser,
                templateUtils,
                provisioningManager,
                binder);
    }

    private static OIDCC4UIProvider provider() {
        OIDCC4UIProvider op = mock(OIDCC4UIProvider.class);
        when(op.getName()).thenReturn("op");
        when(op.getItems()).thenReturn(List.of());
        when(op.getActions()).thenReturn(List.of());
        return op;
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

        Item connObjectKeyItem = item("username", "email");
        when(inboundMatcher.matchByConnObjectKeyValue(
                connObjectKeyItem, "value", AnyTypeKind.USER, null, false)).
                thenReturn(List.of(new InboundMatch(MatchType.ANY, john), new InboundMatch(MatchType.ANY, jane)));

        assertEquals(List.of("john", "jane"), userManager.findMatchingUser("value", connObjectKeyItem));
    }

    @Test
    void findMatchingUserSkipsMatchesWithoutAny() {
        User john = mock(User.class);
        when(john.getUsername()).thenReturn("john");

        Item connObjectKeyItem = item("username", "email");
        when(inboundMatcher.matchByConnObjectKeyValue(
                connObjectKeyItem, "value", AnyTypeKind.USER, null, false)).
                thenReturn(List.of(
                        new InboundMatch(MatchType.ANY, mock(LinkedAccount.class)),
                        new InboundMatch(MatchType.ANY, john)));

        assertEquals(List.of("john"), userManager.findMatchingUser("value", connObjectKeyItem));
    }

    @Test
    void fillSetsUsername() throws Exception {
        OIDCC4UIProvider op = provider();
        when(op.getItems()).thenReturn(List.of(item("username", "email")));
        doReturn(field("username")).when(intAttrNameParser).parse("username", AnyTypeKind.USER);

        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john@example.com").build());

        UserTO userTO = new UserTO();
        userManager.fill(op, response, userTO);

        assertEquals("john@example.com", userTO.getUsername());
    }

    @Test
    void fillAddsPlainAttribute() throws Exception {
        OIDCC4UIProvider op = provider();
        when(op.getItems()).thenReturn(List.of(item("firstname", "given_name")));
        doReturn(plainSchema("firstname")).when(intAttrNameParser).parse("firstname", AnyTypeKind.USER);

        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("given_name").values("John", "Johnny").build());

        UserTO userTO = new UserTO();
        userManager.fill(op, response, userTO);

        Attr firstname = userTO.getPlainAttr("firstname").orElseThrow();
        assertEquals(List.of("John", "Johnny"), firstname.getValues());
    }

    @Test
    void fillReplacesExistingPlainAttribute() throws Exception {
        OIDCC4UIProvider op = provider();
        when(op.getItems()).thenReturn(List.of(item("firstname", "given_name")));
        doReturn(plainSchema("firstname")).when(intAttrNameParser).parse("firstname", AnyTypeKind.USER);

        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("given_name").value("John").build());

        UserTO userTO = new UserTO();
        userTO.getPlainAttrs().add(new Attr.Builder("firstname").value("Old").build());

        userManager.fill(op, response, userTO);

        assertEquals(List.of("John"), userTO.getPlainAttr("firstname").orElseThrow().getValues());
    }

    @Test
    void fillWithoutAttributeLeavesUsernameUnset() throws Exception {
        OIDCC4UIProvider op = provider();
        when(op.getItems()).thenReturn(List.of(item("username", "email")));
        doReturn(field("username")).when(intAttrNameParser).parse("username", AnyTypeKind.USER);

        UserTO userTO = new UserTO();
        userManager.fill(op, new OIDCLoginResponse(), userTO);

        assertEquals(null, userTO.getUsername());
    }

    @Test
    void fillIgnoresInvalidIntAttrName() throws Exception {
        OIDCC4UIProvider op = provider();
        when(op.getItems()).thenReturn(List.of(item("invalid", "email")));
        when(intAttrNameParser.parse("invalid", AnyTypeKind.USER)).thenThrow(new ParseException("invalid", 0));

        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john@example.com").build());

        UserTO userTO = new UserTO();
        userManager.fill(op, response, userTO);

        assertEquals(null, userTO.getUsername());
        assertTrue(userTO.getPlainAttrs().isEmpty());
    }

    @Test
    void fillIgnoresUnsupportedField() throws Exception {
        OIDCC4UIProvider op = provider();
        when(op.getItems()).thenReturn(List.of(item("surname", "family_name")));
        doReturn(field("surname")).when(intAttrNameParser).parse("surname", AnyTypeKind.USER);

        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("family_name").value("Doe").build());

        UserTO userTO = new UserTO();
        userManager.fill(op, response, userTO);

        assertEquals(null, userTO.getUsername());
        assertTrue(userTO.getPlainAttrs().isEmpty());
    }

    @Test
    void createUsesDefaultUsernameAndRootRealm() throws Exception {
        OIDCC4UIProvider op = provider();
        when(provisioningManager.create(
                any(UserCR.class), eq(false), eq("default"), eq(OIDCUserManager.OIDC_CLIENT_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("created", List.of()));

        UserTO created = new UserTO();
        created.setUsername("created");
        when(binder.getUserTO("created")).thenReturn(created);

        assertEquals("created", userManager.create(op, new OIDCLoginResponse(), "default"));

        ArgumentCaptor<UserCR> userCR = ArgumentCaptor.forClass(UserCR.class);
        verify(provisioningManager).create(
                userCR.capture(), eq(false), eq("default"), eq(OIDCUserManager.OIDC_CLIENT_CONTEXT));
        assertEquals("default", userCR.getValue().getUsername());
        assertEquals(SyncopeConstants.ROOT_REALM, userCR.getValue().getRealm());
    }

    @Test
    void createUsesUsernameFromResponse() throws Exception {
        OIDCC4UIProvider op = provider();

        IntAttrName intAttrName = field("username");
        Item item = item("username", "email");
        when(op.getItems()).thenReturn(List.of(item));
        when(intAttrNameParser.parse("username", AnyTypeKind.USER)).thenReturn(intAttrName);

        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("john").build());

        when(provisioningManager.create(
                any(UserCR.class), eq(false), eq("john"), eq(OIDCUserManager.OIDC_CLIENT_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("created", List.of()));

        UserTO created = new UserTO();
        created.setUsername("john");
        when(binder.getUserTO("created")).thenReturn(created);

        assertEquals("john", userManager.create(op, response, "default"));
    }

    @Test
    void createAppliesUserTemplate() throws Exception {
        OIDCC4UIProvider op = provider();

        OIDCC4UIUserTemplate template = mock(OIDCC4UIUserTemplate.class);
        when(template.get()).thenReturn(new UserTO());
        when(op.getUserTemplate()).thenReturn(template);

        when(provisioningManager.create(
                any(UserCR.class), eq(false), eq("default"), eq(OIDCUserManager.OIDC_CLIENT_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("created", List.of()));

        UserTO created = new UserTO();
        created.setUsername("created");
        when(binder.getUserTO("created")).thenReturn(created);

        userManager.create(op, new OIDCLoginResponse(), "default");

        verify(templateUtils).apply(any(UserCR.class), any());
    }

    @Test
    void updateResolvesUserAndReturnsUpdatedUsername() throws Exception {
        OIDCC4UIProvider op = provider();

        Item item = item("username", "email");
        when(op.getItems()).thenReturn(List.of(item));
        doReturn(field("username")).when(intAttrNameParser).parse("username", AnyTypeKind.USER);

        UserTO current = new UserTO();
        current.setKey("key");
        current.setUsername("old");
        when(userDAO.findKey("old")).thenReturn(Optional.of("key"));
        when(binder.getUserTO("key")).thenReturn(current);

        UserUR userUR = new UserUR();
        userUR.setKey("key");
        when(provisioningManager.update(
                any(UserUR.class), eq(false), eq("new"), eq(OIDCUserManager.OIDC_CLIENT_CONTEXT))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(userUR, List.of()));

        UserTO updated = new UserTO();
        updated.setUsername("new");
        when(binder.getUserTO("key")).thenReturn(current, updated);

        OIDCLoginResponse response = new OIDCLoginResponse();
        response.getAttrs().add(new Attr.Builder("email").value("new").build());

        assertEquals("new", userManager.update("old", op, response));
    }

    @Test
    void updateMissingUser() {
        when(userDAO.findKey("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userManager.update("missing", provider(), new OIDCLoginResponse()));
        verify(provisioningManager, never()).
                update(any(UserUR.class), eq(false), any(String.class), any(String.class));
    }
}
