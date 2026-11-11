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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.logic.saml2.SAML2ClientCache;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.provisioning.api.data.SAML2SP4UIIdPDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.pac4j.saml.config.SAML2Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

class SAML2SP4UIIdPLogicTest {

    private static Method deleteMethod() {
        try {
            return SAML2SP4UIIdPLogic.class.getMethod("delete", String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Method DELETE = deleteMethod();

    private SAML2ClientCache loginCache;

    private SAML2ClientCache logoutCache;

    private SAML2SP4UIIdPDataBinder binder;

    private SAML2SP4UIIdPDAO idpDAO;

    private SAML2SP4UIIdPLogic logic;

    @BeforeEach
    void setUp() {
        loginCache = mock(SAML2ClientCache.class);
        logoutCache = mock(SAML2ClientCache.class);
        binder = mock(SAML2SP4UIIdPDataBinder.class);
        idpDAO = mock(SAML2SP4UIIdPDAO.class);

        logic = new SAML2SP4UIIdPLogic(
                new SAML2SP4UIProperties(),
                mock(ResourcePatternResolver.class),
                loginCache,
                logoutCache,
                binder,
                idpDAO);
    }

    private static SAML2SP4UIIdP idp(final String key, final String entityID) {
        SAML2SP4UIIdP idp = mock(SAML2SP4UIIdP.class);
        when(idp.getKey()).thenReturn(key);
        when(idp.getEntityID()).thenReturn(entityID);
        return idp;
    }

    @Test
    void listBindsAllIdPs() {
        SAML2SP4UIIdP idp = idp("key", "https://idp.example.com");
        doReturn(List.of(idp)).when(idpDAO).findAll();

        SAML2SP4UIIdPTO bound = new SAML2SP4UIIdPTO();
        bound.setKey("key");
        when(binder.getIdPTO(idp)).thenReturn(bound);

        assertEquals(List.of(bound), logic.list());
    }

    @Test
    void listHandlesEmptyDAO() {
        doReturn(List.of()).when(idpDAO).findAll();

        assertTrue(logic.list().isEmpty());
    }

    @Test
    void readMissingIdP() {
        doReturn(Optional.empty()).when(idpDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readExistingIdP() {
        SAML2SP4UIIdP idp = idp("key", "https://idp.example.com");
        doReturn(Optional.of(idp)).when(idpDAO).findById("key");

        SAML2SP4UIIdPTO bound = new SAML2SP4UIIdPTO();
        bound.setKey("key");
        when(binder.getIdPTO(idp)).thenReturn(bound);

        assertSame(bound, logic.read("key"));
    }

    @Test
    void updateEvictsCachedClients() {
        SAML2SP4UIIdP idp = idp("key", "https://idp.example.com");
        doReturn(Optional.of(idp)).when(idpDAO).findById("key");
        when(binder.update(eq(idp), any(SAML2SP4UIIdPTO.class))).thenReturn(idp);

        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setKey("key");

        logic.update(idpTO);

        verify(binder).update(idp, idpTO);
        verify(loginCache).removeAll("https://idp.example.com");
        verify(logoutCache).removeAll("https://idp.example.com");
    }

    @Test
    void updateMissingIdP() {
        doReturn(Optional.empty()).when(idpDAO).findById("key");

        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setKey("key");

        assertThrows(NotFoundException.class, () -> logic.update(idpTO));
    }

    @Test
    void deleteEvictsCachedClients() {
        SAML2SP4UIIdP idp = idp("key", "https://idp.example.com");
        doReturn(Optional.of(idp)).when(idpDAO).findById("key");

        logic.delete("key");

        verify(idpDAO).deleteById("key");
        verify(loginCache).removeAll("https://idp.example.com");
        verify(logoutCache).removeAll("https://idp.example.com");
    }

    @Test
    void deleteMissingIdP() {
        doReturn(Optional.empty()).when(idpDAO).findById("key");

        assertThrows(NotFoundException.class, () -> logic.delete("key"));
        verify(idpDAO, never()).deleteById(any());
    }

    @Test
    void importFromMetadataCreatesIdP() throws Exception {
        SAML2SP4UIIdPLogic spy = spy(logic);
        doReturn(mock(SAML2Configuration.class)).when(spy).newSAML2Configuration();

        InputStream input = new ByteArrayInputStream("<metadata/>".getBytes());
        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setEntityID("https://idp.example.com");

        SAML2SP4UIIdP idp = idp("key", "https://idp.example.com");
        when(binder.create(idpTO)).thenReturn(idp);

        try (MockedStatic<SAML2ClientCache> mocked = mockStatic(SAML2ClientCache.class)) {
            mocked.when(() -> SAML2ClientCache.importMetadata(any(), any())).thenReturn(idpTO);

            assertEquals("key", spy.importFromMetadata(input));
        }

        verify(binder).create(idpTO);
    }

    @Test
    void importFromMetadataWrapsFailures() throws Exception {
        SAML2SP4UIIdPLogic spy = spy(logic);
        doReturn(mock(SAML2Configuration.class)).when(spy).newSAML2Configuration();

        try (MockedStatic<SAML2ClientCache> mocked = mockStatic(SAML2ClientCache.class)) {
            mocked.when(() -> SAML2ClientCache.importMetadata(any(), any())).thenThrow(new IOException("boom"));

            SyncopeClientException sce = assertThrows(
                    SyncopeClientException.class,
                    () -> spy.importFromMetadata(new ByteArrayInputStream(new byte[0])));
            assertEquals(ClientExceptionType.InvalidEntity, sce.getType());
            assertTrue(sce.getElements().contains("boom"));
        }

        verify(binder, never()).create(any());
    }

    @Test
    void importFromMetadataPropagatesClientExceptions() throws Exception {
        SAML2SP4UIIdPLogic spy = spy(logic);
        doReturn(mock(SAML2Configuration.class)).when(spy).newSAML2Configuration();

        SyncopeClientException expected = SyncopeClientException.build(ClientExceptionType.InvalidEntity);
        try (MockedStatic<SAML2ClientCache> mocked = mockStatic(SAML2ClientCache.class)) {
            mocked.when(() -> SAML2ClientCache.importMetadata(any(), any())).thenThrow(expected);

            assertSame(expected, assertThrows(
                    SyncopeClientException.class,
                    () -> spy.importFromMetadata(new ByteArrayInputStream(new byte[0]))));
        }
    }

    @Test
    void resolveReferenceFromStringArgument() throws Exception {
        SAML2SP4UIIdP idp = idp("key", "https://idp.example.com");
        doReturn(Optional.of(idp)).when(idpDAO).findById("key");

        SAML2SP4UIIdPTO bound = new SAML2SP4UIIdPTO();
        bound.setKey("key");
        when(binder.getIdPTO(idp)).thenReturn(bound);

        assertEquals("key", logic.resolveReference(DELETE, "key").getKey());
    }

    @Test
    void resolveReferenceFromTOArgument() throws Exception {
        SAML2SP4UIIdP idp = idp("key", "https://idp.example.com");
        doReturn(Optional.of(idp)).when(idpDAO).findById("key");

        SAML2SP4UIIdPTO bound = new SAML2SP4UIIdPTO();
        bound.setKey("key");
        when(binder.getIdPTO(idp)).thenReturn(bound);

        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setKey("key");

        assertEquals("key", logic.resolveReference(DELETE, idpTO).getKey());
    }

    @Test
    void resolveReferenceWithoutKeyFails() {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveReference(DELETE, 42));
    }

    @Test
    void resolveReferenceWithMissingIdPFails() {
        doReturn(Optional.empty()).when(idpDAO).findById("key");

        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveReference(DELETE, "key"));
    }
}
