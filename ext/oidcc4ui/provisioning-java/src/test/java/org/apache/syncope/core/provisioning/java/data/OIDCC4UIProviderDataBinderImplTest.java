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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIEntityFactory;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIUserTemplate;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OIDCC4UIProviderDataBinderImplTest {

    private AnyTypeDAO anyTypeDAO;

    private OIDCC4UIProviderDAO oidcOPDAO;

    private ImplementationDAO implementationDAO;

    private OIDCC4UIEntityFactory entityFactory;

    private IntAttrNameParser intAttrNameParser;

    private JexlTools jexlTools;

    private OIDCC4UIProviderDataBinderImpl binder;

    @BeforeEach
    void setUp() {
        anyTypeDAO = mock(AnyTypeDAO.class);
        oidcOPDAO = mock(OIDCC4UIProviderDAO.class);
        implementationDAO = mock(ImplementationDAO.class);
        entityFactory = mock(OIDCC4UIEntityFactory.class);
        intAttrNameParser = mock(IntAttrNameParser.class);
        jexlTools = mock(JexlTools.class);

        binder = new OIDCC4UIProviderDataBinderImpl(
                anyTypeDAO, oidcOPDAO, implementationDAO, entityFactory, intAttrNameParser, jexlTools);

        when(jexlTools.isExpressionValid(anyString())).thenReturn(true);
    }

    private OIDCC4UIProvider provider() {
        OIDCC4UIProvider provider = mock(OIDCC4UIProvider.class);
        when(provider.getItems()).thenReturn(new ArrayList<>());
        when(provider.getActions()).thenReturn(new ArrayList<>());
        when(oidcOPDAO.save(provider)).thenReturn(provider);
        return provider;
    }

    private static Item item(final String intAttrName, final String extAttrName, final boolean connObjectKey) {
        Item item = new Item();
        item.setIntAttrName(intAttrName);
        item.setExtAttrName(extAttrName);
        item.setConnObjectKey(connObjectKey);
        return item;
    }

    private static IntAttrName field(final String name) {
        IntAttrName intAttrName = mock(IntAttrName.class);
        when(intAttrName.getField()).thenReturn(name);
        return intAttrName;
    }

    private static IntAttrName plainSchema(final String key) {
        Schema schema = mock(Schema.class);
        when(schema.getKey()).thenReturn(key);
        IntAttrName intAttrName = mock(IntAttrName.class);
        when(intAttrName.getSchemaInfo()).thenReturn(new IntAttrName.SchemaInfo(schema, SchemaType.PLAIN));
        return intAttrName;
    }

    @Test
    void updatePopulatesEntity() throws Exception {
        OIDCC4UIProvider provider = provider();

        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setName("op");
        opTO.setClientID("clientID");
        opTO.setClientSecret("clientSecret");
        opTO.setIssuer("https://op.example.com");
        opTO.setJwksUri("https://op.example.com/jwks");
        opTO.setAuthorizationEndpoint("https://op.example.com/authorize");
        opTO.setTokenEndpoint("https://op.example.com/token");
        opTO.setUserinfoEndpoint("https://op.example.com/userinfo");
        opTO.setEndSessionEndpoint("https://op.example.com/logout");
        opTO.getScopes().addAll(List.of("openid", "email"));
        opTO.setHasDiscovery(true);
        opTO.setCreateUnmatching(true);
        opTO.setSelfRegUnmatching(true);
        opTO.setUpdateMatching(true);

        opTO.getItems().add(item("username", "email", true));
        opTO.getItems().add(item("firstname", "given_name", false));
        IntAttrName username = field("username");
        IntAttrName firstname = plainSchema("firstname");
        when(intAttrNameParser.parse("username", AnyTypeKind.USER)).thenReturn(username);
        when(intAttrNameParser.parse("firstname", AnyTypeKind.USER)).thenReturn(firstname);

        Implementation action = mock(Implementation.class);
        when(action.getKey()).thenReturn("action");
        doReturn(Optional.of(action)).when(implementationDAO).findById("action");
        opTO.getActions().add("action");

        assertSame(provider, binder.update(provider, opTO));

        verify(provider).setName("op");
        verify(provider).setClientID("clientID");
        verify(provider).setClientSecret("clientSecret");
        verify(provider).setIssuer("https://op.example.com");
        verify(provider).setJwksUri("https://op.example.com/jwks");
        verify(provider).setAuthorizationEndpoint("https://op.example.com/authorize");
        verify(provider).setTokenEndpoint("https://op.example.com/token");
        verify(provider).setUserinfoEndpoint("https://op.example.com/userinfo");
        verify(provider).setEndSessionEndpoint("https://op.example.com/logout");
        verify(provider).setScopes(List.of("openid", "email"));
        verify(provider).setHasDiscovery(true);
        verify(provider).setCreateUnmatching(true);
        verify(provider).setSelfRegUnmatching(true);
        verify(provider).setUpdateMatching(true);
        verify(provider).setConnObjectKeyItem(any(Item.class));
        verify(provider).add(action);
        verify(oidcOPDAO).save(provider);
    }

    @Test
    void updateRemovesUserTemplateWhenNotProvided() {
        OIDCC4UIProvider provider = provider();
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();

        binder.update(provider, opTO);

        verify(provider).setUserTemplate(null);
    }

    @Test
    void missingIntAttrNameIsRejected() {
        OIDCC4UIProvider provider = provider();
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.getItems().add(item(null, "email", false));

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(provider, opTO));
        assertTrue(e.getException(ClientExceptionType.RequiredValuesMissing).isPresent());
    }

    @Test
    void invalidIntAttrNameIsRejected() throws Exception {
        OIDCC4UIProvider provider = provider();
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.getItems().add(item("unknown", "email", false));
        when(intAttrNameParser.parse("unknown", AnyTypeKind.USER)).thenReturn(null);

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(provider, opTO));
        assertTrue(e.getException(ClientExceptionType.InvalidMapping).isPresent());
    }

    @Test
    void passwordCannotBeConnObjectKey() throws Exception {
        OIDCC4UIProvider provider = provider();
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.getItems().add(item("password", "email", true));
        IntAttrName password = field("password");
        when(intAttrNameParser.parse("password", AnyTypeKind.USER)).thenReturn(password);

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(provider, opTO));
        assertTrue(e.getException(ClientExceptionType.InvalidMapping).isPresent());
        assertTrue(e.getException(ClientExceptionType.InvalidMapping).orElseThrow().getElements().
                contains("Password attributes cannot be set as ConnObjectKey"));
    }

    @Test
    void invalidMandatoryConditionIsRejected() throws Exception {
        OIDCC4UIProvider provider = provider();
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        Item item = item("firstname", "given_name", false);
        item.setMandatoryCondition("invalid");
        opTO.getItems().add(item);
        IntAttrName firstname = plainSchema("firstname");
        when(intAttrNameParser.parse("firstname", AnyTypeKind.USER)).thenReturn(firstname);
        when(jexlTools.isExpressionValid("invalid")).thenReturn(false);

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(provider, opTO));
        assertTrue(e.getException(ClientExceptionType.InvalidValues).isPresent());
    }

    @Test
    void toTO() {
        OIDCC4UIProvider provider = provider();

        when(provider.getKey()).thenReturn("key");
        when(provider.getName()).thenReturn("op");
        when(provider.getClientID()).thenReturn("clientID");
        when(provider.getClientSecret()).thenReturn("clientSecret");
        when(provider.getIssuer()).thenReturn("https://op.example.com");
        when(provider.getJwksUri()).thenReturn("https://op.example.com/jwks");
        when(provider.getAuthorizationEndpoint()).thenReturn("https://op.example.com/authorize");
        when(provider.getTokenEndpoint()).thenReturn("https://op.example.com/token");
        when(provider.getUserinfoEndpoint()).thenReturn("https://op.example.com/userinfo");
        when(provider.getEndSessionEndpoint()).thenReturn("https://op.example.com/logout");
        when(provider.getScopes()).thenReturn(List.of("openid", "email"));
        when(provider.getHasDiscovery()).thenReturn(true);
        when(provider.isCreateUnmatching()).thenReturn(true);
        when(provider.isSelfRegUnmatching()).thenReturn(true);
        when(provider.isUpdateMatching()).thenReturn(true);

        Item connObjectKey = item("username", "email", true);
        Item mapped = item("firstname", "given_name", false);
        when(provider.getItems()).thenReturn(List.of(connObjectKey, mapped));

        UserTO userTemplate = new UserTO();
        userTemplate.setUsername("template");
        OIDCC4UIUserTemplate userTemplateEntity = mock(OIDCC4UIUserTemplate.class);
        when(userTemplateEntity.get()).thenReturn(userTemplate);
        when(provider.getUserTemplate()).thenReturn(userTemplateEntity);

        Implementation action = mock(Implementation.class);
        when(action.getKey()).thenReturn("action");
        doReturn(List.of(action)).when(provider).getActions();

        OIDCC4UIProviderTO opTO = binder.getOIDCProviderTO(provider);

        assertEquals("key", opTO.getKey());
        assertEquals("op", opTO.getName());
        assertEquals("clientID", opTO.getClientID());
        assertEquals("clientSecret", opTO.getClientSecret());
        assertEquals("https://op.example.com", opTO.getIssuer());
        assertEquals("https://op.example.com/jwks", opTO.getJwksUri());
        assertEquals("https://op.example.com/authorize", opTO.getAuthorizationEndpoint());
        assertEquals("https://op.example.com/token", opTO.getTokenEndpoint());
        assertEquals("https://op.example.com/userinfo", opTO.getUserinfoEndpoint());
        assertEquals("https://op.example.com/logout", opTO.getEndSessionEndpoint());
        assertEquals(List.of("openid", "email"), opTO.getScopes());
        assertTrue(opTO.getHasDiscovery());
        assertTrue(opTO.isCreateUnmatching());
        assertTrue(opTO.isSelfRegUnmatching());
        assertTrue(opTO.isUpdateMatching());
        assertSame(userTemplate, opTO.getUserTemplate());
        assertEquals("username", opTO.getConnObjectKeyItem().orElseThrow().getIntAttrName());
        assertNotNull(opTO.getItems().stream().filter(i -> "firstname".equals(i.getIntAttrName())).findFirst().
                orElse(null));
        assertEquals(List.of("action"), opTO.getActions());
    }

    @Test
    void createUsesEntityFactory() {
        OIDCC4UIProvider provider = provider();
        when(entityFactory.newEntity(eq(OIDCC4UIProvider.class))).thenReturn(provider);

        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setName("op");

        assertSame(provider, binder.create(opTO));
        verify(entityFactory).newEntity(OIDCC4UIProvider.class);
        verify(oidcOPDAO).save(provider);
    }

    @Test
    void invalidTransformerIsIgnored() throws Exception {
        OIDCC4UIProvider provider = provider();
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        Item item = item("firstname", "given_name", false);
        item.getTransformers().add("missing");
        opTO.getItems().add(item);
        IntAttrName firstname = plainSchema("firstname");
        when(intAttrNameParser.parse("firstname", AnyTypeKind.USER)).thenReturn(firstname);
        when(implementationDAO.findById(anyString())).thenReturn(Optional.empty());

        binder.update(provider, opTO);

        verify(implementationDAO).findById("missing");
    }
}
