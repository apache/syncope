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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.syncope.common.lib.saml2.SAML2LoginResponse;
import org.apache.syncope.common.lib.saml2.SAML2Request;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.syncope.core.logic.SAML2SP4UILogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SAML2SP4UIServiceImplTest {

    private SAML2SP4UILogic logic;

    private SAML2SP4UIServiceImpl service;

    private HttpHeaders httpHeaders;

    @BeforeEach
    void setUp() {
        logic = mock(SAML2SP4UILogic.class);
        service = new SAML2SP4UIServiceImpl(logic);

        httpHeaders = mock(HttpHeaders.class);
        MessageContext messageContext = mock(MessageContext.class);
        when(messageContext.getHttpHeaders()).thenReturn(httpHeaders);
        service.messageContext = messageContext;
    }

    @Test
    void getMetadataDelegatesToLogic() throws Exception {
        doAnswer(invocation -> {
            invocation.getArgument(2, OutputStream.class).write("metadata".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(logic).getMetadata(eq("https://sp.example.com/"), eq("saml2sp4ui"), any());

        Response response = service.getMetadata("https://sp.example.com", "saml2sp4ui");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_TYPE, response.getMediaType());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(os);

        verify(logic).getMetadata(eq("https://sp.example.com/"), eq("saml2sp4ui"), any());
        assertEquals("metadata", os.toString(StandardCharsets.UTF_8));
    }

    @Test
    void createLoginRequestAppendsSlashToSpEntityID() {
        SAML2Request request = new SAML2Request();
        when(logic.createLoginRequest("https://sp.example.com/", "saml2sp4ui", "idp", true)).
                thenReturn(request);

        assertSame(request, service.createLoginRequest("https://sp.example.com", "saml2sp4ui", "idp", true));
    }

    @Test
    void validateLoginResponseDelegatesToLogic() {
        SAML2Response input = new SAML2Response();
        SAML2LoginResponse response = new SAML2LoginResponse();
        when(logic.validateLoginResponse(input)).thenReturn(response);

        assertSame(response, service.validateLoginResponse(input));
    }

    @Test
    void createLogoutRequestUsesBearerToken() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer accessToken");

        SAML2Request request = new SAML2Request();
        when(logic.createLogoutRequest("accessToken", "https://sp.example.com/", "saml2sp4ui")).
                thenReturn(request);

        assertSame(request, service.createLogoutRequest("https://sp.example.com", "saml2sp4ui"));
        verify(logic).createLogoutRequest("accessToken", "https://sp.example.com/", "saml2sp4ui");
    }

    @Test
    void createLogoutRequestWithoutAuthorizationHeader() {
        service.createLogoutRequest("https://sp.example.com", "saml2sp4ui");

        verify(logic).createLogoutRequest(null, "https://sp.example.com/", "saml2sp4ui");
    }

    @Test
    void createLogoutRequestWithMalformedAuthorizationHeader() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer");

        service.createLogoutRequest("https://sp.example.com", "saml2sp4ui");

        verify(logic).createLogoutRequest(null, "https://sp.example.com/", "saml2sp4ui");
    }

    @Test
    void createLogoutRequestWithNonBearerAuthorizationHeader() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Basic credentials");

        service.createLogoutRequest("https://sp.example.com", "saml2sp4ui");

        verify(logic).createLogoutRequest(null, "https://sp.example.com/", "saml2sp4ui");
    }

    @Test
    void validateLogoutResponseDelegatesToLogic() {
        SAML2Response response = new SAML2Response();

        service.validateLogoutResponse(response);

        verify(logic).validateLogoutResponse(response);
    }
}
