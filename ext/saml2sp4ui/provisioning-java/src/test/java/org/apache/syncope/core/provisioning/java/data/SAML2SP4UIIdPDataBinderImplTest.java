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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIEntityFactory;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIUserTemplate;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SAML2SP4UIIdPDataBinderImplTest {

    private AnyTypeDAO anyTypeDAO;

    private SAML2SP4UIIdPDAO idpDAO;

    private ImplementationDAO implementationDAO;

    private SAML2SP4UIEntityFactory entityFactory;

    private IntAttrNameParser intAttrNameParser;

    private JexlTools jexlTools;

    private SAML2SP4UIIdPDataBinderImpl binder;

    @BeforeEach
    void setUp() {
        anyTypeDAO = mock(AnyTypeDAO.class);
        idpDAO = mock(SAML2SP4UIIdPDAO.class);
        implementationDAO = mock(ImplementationDAO.class);
        entityFactory = mock(SAML2SP4UIEntityFactory.class);
        intAttrNameParser = mock(IntAttrNameParser.class);
        jexlTools = mock(JexlTools.class);

        binder = new SAML2SP4UIIdPDataBinderImpl(
                anyTypeDAO, idpDAO, implementationDAO, entityFactory, intAttrNameParser, jexlTools);

        when(jexlTools.isExpressionValid(anyString())).thenReturn(true);
    }

    private SAML2SP4UIIdP idp() {
        SAML2SP4UIIdP idp = mock(SAML2SP4UIIdP.class);
        when(idp.getItems()).thenReturn(new ArrayList<>());
        when(idp.getActions()).thenReturn(new ArrayList<>());
        when(idpDAO.save(idp)).thenReturn(idp);
        return idp;
    }

    private static SAML2SP4UIIdPTO idpTO() {
        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setMetadata("");
        return idpTO;
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
        SAML2SP4UIIdP idp = idp();

        SAML2SP4UIIdPTO idpTO = idpTO();
        idpTO.setEntityID("https://idp.example.com");
        idpTO.setName("idp");
        idpTO.setMetadata(Base64.getMimeEncoder().encodeToString(
                "metadata".getBytes(StandardCharsets.UTF_8)));
        idpTO.setLogoutSupported(true);
        idpTO.setCreateUnmatching(true);
        idpTO.setSelfRegUnmatching(false);
        idpTO.setUpdateMatching(true);
        idpTO.setBindingType(SAML2BindingType.POST);

        Implementation authnContextProvider = mock(Implementation.class);
        when(authnContextProvider.getKey()).thenReturn("authnContextProvider");
        doReturn(Optional.of(authnContextProvider)).when(implementationDAO).findById("authnContextProvider");
        idpTO.setRequestedAuthnContextProvider("authnContextProvider");

        AnyType user = mock(AnyType.class);
        when(anyTypeDAO.getUser()).thenReturn(user);
        SAML2SP4UIUserTemplate userTemplate = mock(SAML2SP4UIUserTemplate.class);
        when(entityFactory.newEntity(SAML2SP4UIUserTemplate.class)).thenReturn(userTemplate);
        UserTO userTemplateTO = new UserTO();
        userTemplateTO.setUsername("template");
        idpTO.setUserTemplate(userTemplateTO);

        idpTO.getItems().add(item("username", "NameID", true));
        idpTO.getItems().add(item("firstname", "given_name", false));
        IntAttrName username = field("username");
        IntAttrName firstname = plainSchema("firstname");
        when(intAttrNameParser.parse("username", AnyTypeKind.USER)).thenReturn(username);
        when(intAttrNameParser.parse("firstname", AnyTypeKind.USER)).thenReturn(firstname);

        Implementation action = mock(Implementation.class);
        when(action.getKey()).thenReturn("action");
        doReturn(Optional.of(action)).when(implementationDAO).findById("action");
        idpTO.getActions().add("action");

        assertSame(idp, binder.update(idp, idpTO));

        verify(idp).setEntityID("https://idp.example.com");
        verify(idp).setName("idp");
        verify(idp).setMetadata("metadata".getBytes(StandardCharsets.UTF_8));
        verify(idp).setLogoutSupported(true);
        verify(idp).setCreateUnmatching(true);
        verify(idp).setSelfRegUnmatching(false);
        verify(idp).setUpdateMatching(true);
        verify(idp).setBindingType(SAML2BindingType.POST);
        verify(idp).setRequestedAuthnContextProvider(authnContextProvider);
        verify(userTemplate).setAnyType(user);
        verify(userTemplate).setIdP(idp);
        verify(userTemplate).set(userTemplateTO);
        verify(idp).setUserTemplate(userTemplate);

        ArgumentCaptor<Item> connObjectKey = ArgumentCaptor.forClass(Item.class);
        verify(idp).setConnObjectKeyItem(connObjectKey.capture());
        assertEquals("username", connObjectKey.getValue().getIntAttrName());
        assertEquals(MappingPurpose.NONE, connObjectKey.getValue().getPurpose());

        assertEquals(1, idp.getItems().size());
        assertEquals("firstname", idp.getItems().getFirst().getIntAttrName());

        verify(idp).add(action);
        verify(idpDAO).save(idp);
    }

    @Test
    void updateReusesExistingUserTemplate() {
        SAML2SP4UIIdP idp = idp();
        SAML2SP4UIUserTemplate userTemplate = mock(SAML2SP4UIUserTemplate.class);
        when(idp.getUserTemplate()).thenReturn(userTemplate);

        SAML2SP4UIIdPTO idpTO = idpTO();
        UserTO userTemplateTO = new UserTO();
        idpTO.setUserTemplate(userTemplateTO);

        binder.update(idp, idpTO);

        verify(userTemplate).set(userTemplateTO);
        verify(entityFactory, org.mockito.Mockito.never()).newEntity(SAML2SP4UIUserTemplate.class);
    }

    @Test
    void updateRemovesUserTemplateWhenNotProvided() {
        SAML2SP4UIIdP idp = idp();

        binder.update(idp, idpTO());

        verify(idp).setUserTemplate(null);
    }

    @Test
    void missingIntAttrNameIsRejected() {
        SAML2SP4UIIdP idp = idp();
        SAML2SP4UIIdPTO idpTO = idpTO();
        idpTO.getItems().add(item(null, "email", false));

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(idp, idpTO));
        assertTrue(e.getException(ClientExceptionType.RequiredValuesMissing).isPresent());
    }

    @Test
    void invalidIntAttrNameIsRejected() throws Exception {
        SAML2SP4UIIdP idp = idp();
        SAML2SP4UIIdPTO idpTO = idpTO();
        idpTO.getItems().add(item("unknown", "email", false));
        when(intAttrNameParser.parse("unknown", AnyTypeKind.USER)).thenReturn(null);

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(idp, idpTO));
        assertTrue(e.getException(ClientExceptionType.InvalidMapping).isPresent());
    }

    @Test
    void passwordCannotBeConnObjectKey() throws Exception {
        SAML2SP4UIIdP idp = idp();
        SAML2SP4UIIdPTO idpTO = idpTO();
        idpTO.getItems().add(item("password", "email", true));
        IntAttrName password = field("password");
        when(intAttrNameParser.parse("password", AnyTypeKind.USER)).thenReturn(password);

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(idp, idpTO));
        assertTrue(e.getException(ClientExceptionType.InvalidMapping).isPresent());
        assertTrue(e.getException(ClientExceptionType.InvalidMapping).orElseThrow().getElements().
                contains("Password attributes cannot be set as ConnObjectKey"));
    }

    @Test
    void invalidMandatoryConditionIsRejected() throws Exception {
        SAML2SP4UIIdP idp = idp();
        SAML2SP4UIIdPTO idpTO = idpTO();
        Item item = item("firstname", "given_name", false);
        item.setMandatoryCondition("invalid");
        idpTO.getItems().add(item);
        IntAttrName firstname = plainSchema("firstname");
        when(intAttrNameParser.parse("firstname", AnyTypeKind.USER)).thenReturn(firstname);
        when(jexlTools.isExpressionValid("invalid")).thenReturn(false);

        SyncopeClientCompositeException e = assertThrows(
                SyncopeClientCompositeException.class, () -> binder.update(idp, idpTO));
        assertTrue(e.getException(ClientExceptionType.InvalidValues).isPresent());
    }

    @Test
    void toTO() {
        SAML2SP4UIIdP idp = idp();

        when(idp.getKey()).thenReturn("key");
        when(idp.getEntityID()).thenReturn("https://idp.example.com");
        when(idp.getName()).thenReturn("idp");
        when(idp.getMetadata()).thenReturn("metadata".getBytes(StandardCharsets.UTF_8));
        when(idp.isLogoutSupported()).thenReturn(true);
        when(idp.getBindingType()).thenReturn(SAML2BindingType.REDIRECT);
        when(idp.isCreateUnmatching()).thenReturn(true);
        when(idp.isSelfRegUnmatching()).thenReturn(true);
        when(idp.isUpdateMatching()).thenReturn(true);

        Implementation authnContextProvider = mock(Implementation.class);
        when(authnContextProvider.getKey()).thenReturn("authnContextProvider");
        when(idp.getRequestedAuthnContextProvider()).thenReturn(authnContextProvider);

        UserTO userTemplate = new UserTO();
        userTemplate.setUsername("template");
        SAML2SP4UIUserTemplate userTemplateEntity = mock(SAML2SP4UIUserTemplate.class);
        when(userTemplateEntity.get()).thenReturn(userTemplate);
        when(idp.getUserTemplate()).thenReturn(userTemplateEntity);

        Item connObjectKey = item("username", "NameID", true);
        Item mapped = item("firstname", "given_name", false);
        when(idp.getItems()).thenReturn(List.of(connObjectKey, mapped));

        Implementation action = mock(Implementation.class);
        when(action.getKey()).thenReturn("action");
        doReturn(List.of(action)).when(idp).getActions();

        SAML2SP4UIIdPTO idpTO = binder.getIdPTO(idp);

        assertEquals("key", idpTO.getKey());
        assertEquals("https://idp.example.com", idpTO.getEntityID());
        assertEquals("idp", idpTO.getName());
        assertEquals(
                Base64.getMimeEncoder().encodeToString("metadata".getBytes(StandardCharsets.UTF_8)),
                idpTO.getMetadata());
        assertTrue(idpTO.isLogoutSupported());
        assertEquals(SAML2BindingType.REDIRECT, idpTO.getBindingType());
        assertTrue(idpTO.isCreateUnmatching());
        assertTrue(idpTO.isSelfRegUnmatching());
        assertTrue(idpTO.isUpdateMatching());
        assertEquals("authnContextProvider", idpTO.getRequestedAuthnContextProvider());
        assertSame(userTemplate, idpTO.getUserTemplate());
        assertEquals("username", idpTO.getConnObjectKeyItem().orElseThrow().getIntAttrName());
        assertNotNull(idpTO.getItems().stream().
                filter(i -> "firstname".equals(i.getIntAttrName())).findFirst().orElse(null));
        assertEquals(List.of("action"), idpTO.getActions());
    }

    @Test
    void createUsesEntityFactory() {
        SAML2SP4UIIdP idp = idp();
        when(entityFactory.newEntity(eq(SAML2SP4UIIdP.class))).thenReturn(idp);

        SAML2SP4UIIdPTO idpTO = idpTO();
        idpTO.setEntityID("https://idp.example.com");
        idpTO.setName("idp");

        assertSame(idp, binder.create(idpTO));

        verify(entityFactory).newEntity(SAML2SP4UIIdP.class);
        verify(idpDAO).save(idp);
    }

    @Test
    void ignoredInvalidImplementationReferences() {
        SAML2SP4UIIdP idp = idp();
        SAML2SP4UIIdPTO idpTO = idpTO();
        idpTO.setRequestedAuthnContextProvider("invalid");
        idpTO.getActions().add("invalid");

        binder.update(idp, idpTO);

        verify(idp, org.mockito.Mockito.never()).setRequestedAuthnContextProvider(any());
        verify(idp, org.mockito.Mockito.never()).add(any());
    }
}
