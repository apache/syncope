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
package org.apache.syncope.core.rest.cxf.service.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.syncope.common.lib.auth.SAML2IdPAuthModuleConf;
import org.apache.syncope.core.logic.AuthModuleLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WASAML2SPServiceTest {

    private AuthModuleLogic logic;

    private WASAML2SPServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(AuthModuleLogic.class);
        service = new WASAML2SPServiceImpl(logic);
    }

    @Test
    void getSAML2SPKeystore() {
        SAML2IdPAuthModuleConf conf = new SAML2IdPAuthModuleConf();
        conf.setKeystore("keystore");
        when(logic.readSAML2SPConf("client")).thenReturn(conf);

        Response response = service.getSAML2SPKeystore("client");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("keystore", response.getEntity());
    }

    @Test
    void getSAML2SPKeystoreNotFound() {
        SAML2IdPAuthModuleConf conf = new SAML2IdPAuthModuleConf();
        when(logic.readSAML2SPConf("client")).thenReturn(conf);

        Response response = service.getSAML2SPKeystore("client");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void setSAML2SPKeystore() {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        service.setSAML2SPKeystore("client", in);
        verify(logic).setSAML2SPKeystore("client", in);
    }

    @Test
    void getSAML2SPMetadata() {
        SAML2IdPAuthModuleConf conf = new SAML2IdPAuthModuleConf();
        conf.setServiceProviderMetadata("metadata");
        when(logic.readSAML2SPConf("client")).thenReturn(conf);

        Response response = service.getSAML2SPMetadata("client");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("metadata", response.getEntity());
    }

    @Test
    void getSAML2SPMetadataNotFound() {
        SAML2IdPAuthModuleConf conf = new SAML2IdPAuthModuleConf();
        when(logic.readSAML2SPConf("client")).thenReturn(conf);

        Response response = service.getSAML2SPMetadata("client");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void setSAML2SPMetadata() {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        service.setSAML2SPMetadata("client", in);
        verify(logic).setSAML2SPMetadata("client", in);
    }
}
