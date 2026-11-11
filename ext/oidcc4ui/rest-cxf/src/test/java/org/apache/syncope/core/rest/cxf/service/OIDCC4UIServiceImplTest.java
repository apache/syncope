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
package org.apache.syncope.core.rest.cxf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.HttpHeaders;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.syncope.common.lib.oidc.OIDCLoginResponse;
import org.apache.syncope.common.lib.oidc.OIDCRequest;
import org.apache.syncope.core.logic.OIDCC4UILogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OIDCC4UIServiceImplTest {

    private OIDCC4UILogic logic;

    private OIDCC4UIServiceImpl service;

    private HttpHeaders httpHeaders;

    @BeforeEach
    void setUp() {
        logic = mock(OIDCC4UILogic.class);
        service = new OIDCC4UIServiceImpl(logic);

        httpHeaders = mock(HttpHeaders.class);
        MessageContext messageContext = mock(MessageContext.class);
        when(messageContext.getHttpHeaders()).thenReturn(httpHeaders);
        service.messageContext = messageContext;
    }

    @Test
    void createLoginRequestDelegatesToLogic() {
        OIDCRequest request = new OIDCRequest("id", "https://op.example.com/authorize");
        when(logic.createLoginRequest("https://client/callback", "op", true)).thenReturn(request);

        assertSame(request, service.createLoginRequest("https://client/callback", "op", true));
    }

    @Test
    void loginDelegatesToLogic() {
        OIDCLoginResponse response = new OIDCLoginResponse();
        when(logic.login("id", "https://client/callback", "code", "op")).thenReturn(response);

        assertSame(response, service.login("id", "https://client/callback", "code", "op"));
    }

    @Test
    void backChannelLogoutDelegatesToLogic() {
        service.backChannelLogout("logoutToken", "https://client/callback");

        verify(logic).backChannelLogout("logoutToken", "https://client/callback");
    }

    @Test
    void createLogoutRequestUsesBearerToken() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer accessToken");

        OIDCRequest request = new OIDCRequest("id", "https://op.example.com/logout");
        when(logic.createLogoutRequest("accessToken", "https://client/callback")).thenReturn(request);

        assertSame(request, service.createLogoutRequest("https://client/callback"));
        verify(logic).createLogoutRequest("accessToken", "https://client/callback");
    }

    @Test
    void createLogoutRequestWithoutAuthorizationHeader() {
        assertNull(service.createLogoutRequest("https://client/callback"));
        verify(logic, never()).createLogoutRequest(any(), any());
    }

    @Test
    void createLogoutRequestWithMalformedAuthorizationHeader() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer");

        assertNull(service.createLogoutRequest("https://client/callback"));
        verify(logic, never()).createLogoutRequest(anyString(), anyString());
    }

    @Test
    void createLogoutRequestWithNonBearerAuthorizationHeader() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Basic credentials");

        assertNull(service.createLogoutRequest("https://client/callback"));
        verify(logic, never()).createLogoutRequest(anyString(), anyString());
        assertEquals("Basic credentials", httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION));
    }
}
