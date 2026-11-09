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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.entity.am.ClientAppUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientAppLogicTest {

    private ServiceOps serviceOps;

    private ClientAppUtilsFactory clientAppUtilsFactory;

    private ClientAppDataBinder binder;

    private CASSPClientAppDAO casSPClientAppDAO;

    private OIDCRPClientAppDAO oidcRPClientAppDAO;

    private SAML2SPClientAppDAO saml2SPClientAppDAO;

    private ClientAppLogic logic;

    @BeforeEach
    void setUp() {
        serviceOps = mock(ServiceOps.class);
        clientAppUtilsFactory = mock(ClientAppUtilsFactory.class);
        binder = mock(ClientAppDataBinder.class);
        casSPClientAppDAO = mock(CASSPClientAppDAO.class);
        oidcRPClientAppDAO = mock(OIDCRPClientAppDAO.class);
        saml2SPClientAppDAO = mock(SAML2SPClientAppDAO.class);

        logic = new ClientAppLogic(
                serviceOps, clientAppUtilsFactory, binder,
                casSPClientAppDAO, oidcRPClientAppDAO, saml2SPClientAppDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void listOIDCRP() {
        when(oidcRPClientAppDAO.findAll()).thenReturn(List.of());

        assertEquals(List.of(), logic.list(ClientAppType.OIDCRP));
    }

    @Test
    void listCASSP() {
        when(casSPClientAppDAO.findAll()).thenReturn(List.of());

        assertEquals(List.of(), logic.list(ClientAppType.CASSP));
    }

    @Test
    void listSAML2SP() {
        when(saml2SPClientAppDAO.findAll()).thenReturn(List.of());

        assertEquals(List.of(), logic.list(ClientAppType.SAML2SP));
    }

    @Test
    void readMissing() {
        when(oidcRPClientAppDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.read(ClientAppType.OIDCRP, "missing"));
    }
}
