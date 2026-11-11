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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.OIDCC4UIEntitlement;
import org.apache.syncope.core.logic.oidc.OIDCClientCache;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.provisioning.api.data.OIDCC4UIProviderDataBinder;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class OIDCC4UIProviderLogicTest {

    private static Method nonReadOnlyMethod() {
        try {
            return OIDCC4UIProviderLogic.class.getMethod("delete", String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Method METHOD = nonReadOnlyMethod();

    private OIDCClientCache loginCache;

    private OIDCClientCache logoutCache;

    private OIDCC4UIProviderDAO opDAO;

    private OIDCC4UIProviderDataBinder binder;

    private OIDCC4UIProviderLogic logic;

    @BeforeEach
    void setUp() {
        loginCache = mock(OIDCClientCache.class);
        logoutCache = mock(OIDCClientCache.class);
        opDAO = mock(OIDCC4UIProviderDAO.class);
        binder = mock(OIDCC4UIProviderDataBinder.class);

        logic = new OIDCC4UIProviderLogic(loginCache, logoutCache, opDAO, binder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(final String entitlement) {
        List<GrantedAuthority> authorities =
                List.of(new SyncopeGrantedAuthority(entitlement, "master"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "password", authorities));
    }

    private static OIDCC4UIProvider provider(final String key, final String name, final String issuer) {
        OIDCC4UIProvider op = mock(OIDCC4UIProvider.class);
        when(op.getKey()).thenReturn(key);
        when(op.getName()).thenReturn(name);
        when(op.getIssuer()).thenReturn(issuer);
        return op;
    }

    @Test
    void createAddsDefaultConnObjectKeyItem() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setName("op");
        assertTrue(opTO.getConnObjectKeyItem().isEmpty());

        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        when(binder.create(opTO)).thenReturn(op);

        assertEquals("key", logic.create(opTO));

        Item connObjectKeyItem = opTO.getConnObjectKeyItem().orElseThrow();
        assertEquals("username", connObjectKeyItem.getIntAttrName());
        assertEquals("email", connObjectKeyItem.getExtAttrName());
        assertTrue(connObjectKeyItem.isConnObjectKey());
        assertEquals("true", connObjectKeyItem.getMandatoryCondition());
        verify(binder).create(opTO);
    }

    @Test
    void createKeepsProvidedConnObjectKeyItem() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setName("op");
        Item item = new Item();
        item.setIntAttrName("email");
        opTO.setConnObjectKeyItem(item);

        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        when(binder.create(opTO)).thenReturn(op);

        assertEquals("key", logic.create(opTO));

        assertEquals("email", opTO.getConnObjectKeyItem().orElseThrow().getIntAttrName());
        verify(binder).create(opTO);
    }

    @Test
    void createFromDiscoveryImportsMetadataThenCreates() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setIssuer("https://op.example.com");

        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        when(binder.create(opTO)).thenReturn(op);

        try (MockedStatic<OIDCClientCache> mocked = mockStatic(OIDCClientCache.class)) {
            assertEquals("key", logic.createFromDiscovery(opTO));
            mocked.verify(() -> OIDCClientCache.importMetadata(opTO));
        }

        verify(binder).create(opTO);
    }

    @Test
    void createFromDiscoveryWrapsFailures() {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setIssuer("https://op.example.com");

        try (MockedStatic<OIDCClientCache> mocked = mockStatic(OIDCClientCache.class)) {
            mocked.when(() -> OIDCClientCache.importMetadata(opTO)).thenThrow(new IOException("boom"));

            SyncopeClientException sce = assertThrows(SyncopeClientException.class,
                    () -> logic.createFromDiscovery(opTO));
            assertEquals(ClientExceptionType.Unknown, sce.getType());
            assertTrue(sce.getElements().contains("boom"));
        }

        verify(binder, never()).create(any());
    }

    @Test
    void listHidesClientSecretForNonAdmin() {
        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        doReturn(List.of(op)).when(opDAO).findAll();

        OIDCC4UIProviderTO bound = new OIDCC4UIProviderTO();
        bound.setKey("key");
        bound.setClientSecret("secret");
        when(binder.getOIDCProviderTO(op)).thenReturn(bound);

        List<OIDCC4UIProviderTO> providers = logic.list();

        assertEquals(1, providers.size());
        assertEquals(null, providers.getFirst().getClientSecret());
    }

    @Test
    void listKeepsClientSecretForAdmin() {
        authenticate(OIDCC4UIEntitlement.OP_READ);

        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        doReturn(List.of(op)).when(opDAO).findAll();

        OIDCC4UIProviderTO bound = new OIDCC4UIProviderTO();
        bound.setKey("key");
        bound.setClientSecret("secret");
        when(binder.getOIDCProviderTO(op)).thenReturn(bound);

        List<OIDCC4UIProviderTO> providers = logic.list();

        assertEquals(1, providers.size());
        assertEquals("secret", providers.getFirst().getClientSecret());
    }

    @Test
    void readMissingProvider() {
        doReturn(Optional.empty()).when(opDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readExistingProvider() {
        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        OIDCC4UIProviderTO bound = new OIDCC4UIProviderTO();
        bound.setKey("key");
        doReturn(Optional.of(op)).when(opDAO).findById("key");
        when(binder.getOIDCProviderTO(op)).thenReturn(bound);

        assertEquals("key", logic.read("key").getKey());
    }

    @Test
    void updateRejectsIssuerMismatch() {
        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        doReturn(Optional.of(op)).when(opDAO).findById("key");

        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setKey("key");
        opTO.setIssuer("https://other.example.com");

        SyncopeClientException sce = assertThrows(SyncopeClientException.class, () -> logic.update(opTO));
        assertEquals(ClientExceptionType.InvalidEntity, sce.getType());
        verify(binder, never()).update(any(), any());
    }

    @Test
    void updateEvictsCachedClients() {
        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        doReturn(Optional.of(op)).when(opDAO).findById("key");

        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setKey("key");
        opTO.setIssuer("https://op.example.com");

        logic.update(opTO);

        verify(binder).update(op, opTO);
        verify(loginCache).removeAll("op");
        verify(logoutCache).removeAll("op");
    }

    @Test
    void updateMissingProvider() {
        doReturn(Optional.empty()).when(opDAO).findById("key");

        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setKey("key");

        assertThrows(NotFoundException.class, () -> logic.update(opTO));
    }

    @Test
    void deleteEvictsCachedClients() {
        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        doReturn(Optional.of(op)).when(opDAO).findById("key");

        logic.delete("key");

        verify(opDAO).deleteById("key");
        verify(loginCache).removeAll("op");
        verify(logoutCache).removeAll("op");
    }

    @Test
    void deleteMissingProvider() {
        doReturn(Optional.empty()).when(opDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.delete("key"));
        verify(opDAO, never()).deleteById(any());
    }

    @Test
    void resolveReferenceFromStringArgument() throws Exception {
        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        OIDCC4UIProviderTO bound = new OIDCC4UIProviderTO();
        bound.setKey("key");
        doReturn(Optional.of(op)).when(opDAO).findById("key");
        when(binder.getOIDCProviderTO(op)).thenReturn(bound);

        assertEquals("key", logic.resolveBeanReference(METHOD, "key").getKey());
    }

    @Test
    void resolveReferenceFromTOArgument() throws Exception {
        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        OIDCC4UIProviderTO bound = new OIDCC4UIProviderTO();
        bound.setKey("key");
        doReturn(Optional.of(op)).when(opDAO).findById("key");
        when(binder.getOIDCProviderTO(op)).thenReturn(bound);

        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();
        opTO.setKey("key");

        assertEquals("key", logic.resolveBeanReference(METHOD, opTO).getKey());
    }

    @Test
    void resolveReferenceWithoutKeyFails() {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveBeanReference(METHOD, 42));
    }

    @Test
    void resolveReferenceWithMissingProviderFails() {
        doReturn(Optional.empty()).when(opDAO).findById("key");

        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveBeanReference(METHOD, "key"));
    }

    @Test
    void resolveReferenceForReadOnlyMethodFails() throws Exception {
        Method read = OIDCC4UIProviderLogic.class.getMethod("read", String.class);

        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveBeanReference(read, "key"));
        verify(opDAO, never()).findById(eq("key"));
    }

    @Test
    void listHandlesEmptyDAO() {
        doReturn(List.of()).when(opDAO).findAll();

        assertTrue(logic.list().isEmpty());
        verifyNoInteractionsWithCaches();
    }

    private void verifyNoInteractionsWithCaches() {
        verify(loginCache, never()).get(any());
        verify(logoutCache, never()).get(any());
        verify(loginCache, never()).add(any(), any());
        verify(logoutCache, never()).add(any(), any());
        verify(loginCache, never()).removeAll(any());
        verify(logoutCache, never()).removeAll(any());
    }

    @Test
    void authenticatedUserWithoutEntitlementGetsNoSecret() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", Set.of()));

        OIDCC4UIProvider op = provider("key", "op", "https://op.example.com");
        doReturn(List.of(op)).when(opDAO).findAll();

        OIDCC4UIProviderTO bound = new OIDCC4UIProviderTO();
        bound.setKey("key");
        bound.setClientSecret("secret");
        when(binder.getOIDCProviderTO(op)).thenReturn(bound);

        assertEquals(null, logic.list().getFirst().getClientSecret());
    }
}
