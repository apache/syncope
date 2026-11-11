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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.logic.SAML2SP4UIIdPLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SAML2SP4UIIdPServiceImplTest {

    private SAML2SP4UIIdPLogic logic;

    private SAML2SP4UIIdPServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(SAML2SP4UIIdPLogic.class);
        service = new SAML2SP4UIIdPServiceImpl(logic);
    }

    private static SAML2SP4UIIdPTO idp() {
        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();
        idpTO.setKey("key");
        idpTO.setName("idp");
        return idpTO;
    }

    @Test
    void importFromMetadataReturnsCreatedResponse() {
        InputStream input = new ByteArrayInputStream("<metadata/>".getBytes());
        when(logic.importFromMetadata(input)).thenReturn("key");

        Response response = service.importFromMetadata(input);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("key", response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        verify(logic).importFromMetadata(input);
    }

    @Test
    void listDelegatesToLogic() {
        List<SAML2SP4UIIdPTO> idps = List.of(idp());
        when(logic.list()).thenReturn(idps);

        assertSame(idps, service.list());
    }

    @Test
    void readDelegatesToLogic() {
        SAML2SP4UIIdPTO idpTO = idp();
        when(logic.read("key")).thenReturn(idpTO);

        assertSame(idpTO, service.read("key"));
    }

    @Test
    void updateDelegatesToLogic() {
        SAML2SP4UIIdPTO idpTO = idp();

        service.update(idpTO);

        verify(logic).update(idpTO);
    }

    @Test
    void deleteDelegatesToLogic() {
        service.delete("key");

        verify(logic).delete("key");
    }
}
