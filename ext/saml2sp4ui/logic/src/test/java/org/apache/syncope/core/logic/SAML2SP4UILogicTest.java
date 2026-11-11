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
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Base64;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.saml2.SAML2Request;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SAML2BindingType;
import org.apache.syncope.core.logic.saml2.SAML2ClientCache;
import org.apache.syncope.core.logic.saml2.SAML2SP4UIUserManager;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.OkAction;
import org.springframework.core.io.support.ResourcePatternResolver;

class SAML2SP4UILogicTest {

    private SAML2SP4UILogic logic;

    @BeforeEach
    void setUp() {
        logic = new SAML2SP4UILogic(
                new SAML2SP4UIProperties(),
                mock(ResourcePatternResolver.class),
                mock(AccessTokenDataBinder.class),
                mock(SAML2ClientCache.class),
                mock(SAML2ClientCache.class),
                mock(SAML2SP4UIUserManager.class),
                mock(SAML2SP4UIIdPDAO.class),
                mock(AuthDataAccessor.class),
                mock(EncryptorManager.class));
    }

    @Test
    void validUrlIsReturnedUnchanged() {
        assertEquals(
                "https://sp.example.com/saml2sp4ui",
                SAML2SP4UILogic.validateUrl("https://sp.example.com/saml2sp4ui"));
    }

    @Test
    void urlWithParentDirectoryIsRejected() {
        SyncopeClientException sce = assertThrows(
                SyncopeClientException.class,
                () -> SAML2SP4UILogic.validateUrl("https://sp.example.com/../saml2sp4ui"));
        assertEquals(ClientExceptionType.Unknown, sce.getType());
        assertTrue(sce.getElements().stream().anyMatch(e -> e.startsWith("Invalid URL:")));
    }

    @Test
    void nonUrlIsRejected() {
        assertThrows(SyncopeClientException.class, () -> SAML2SP4UILogic.validateUrl("not a URL"));
    }

    @Test
    void callbackUrlAppendsAssertionConsumer() {
        assertEquals(
                "https://sp.example.com/saml2sp4ui/assertion-consumer",
                SAML2SP4UILogic.getCallbackUrl("https://sp.example.com/", "saml2sp4ui"));
    }

    @Test
    void redirectActionBecomesRedirectRequest() {
        SAML2Request request = SAML2SP4UILogic.buildRequest(
                "https://idp.example.com", new FoundAction("https://idp.example.com/sso"));

        assertEquals("https://idp.example.com", request.getIdpEntityID());
        assertEquals(SAML2BindingType.REDIRECT, request.getBindingType());
        assertEquals("https://idp.example.com/sso", request.getContent());
    }

    @Test
    void contentActionBecomesPostRequest() {
        SAML2Request request = SAML2SP4UILogic.buildRequest(
                "https://idp.example.com", new OkAction("<html/>"));

        assertEquals("https://idp.example.com", request.getIdpEntityID());
        assertEquals(SAML2BindingType.POST, request.getBindingType());
        assertEquals(
                "<html/>",
                new String(Base64.getMimeDecoder().decode(request.getContent())));
    }

    @Test
    void resolveReferenceIsNeverResolved() throws Exception {
        Method method = SAML2SP4UILogic.class.getDeclaredMethod("resolveReference", Method.class, Object[].class);

        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveReference(method, "idp"));
    }
}
