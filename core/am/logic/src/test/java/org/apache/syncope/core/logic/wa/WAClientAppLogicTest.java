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
package org.apache.syncope.core.logic.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WAClientAppLogicTest {

    private WAClientAppDataBinder binder;

    private CASSPClientAppDAO casSPClientAppDAO;

    private OIDCRPClientAppDAO oidcRPClientAppDAO;

    private SAML2SPClientAppDAO saml2SPClientAppDAO;

    private WAClientAppLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(WAClientAppDataBinder.class);
        casSPClientAppDAO = mock(CASSPClientAppDAO.class);
        oidcRPClientAppDAO = mock(OIDCRPClientAppDAO.class);
        saml2SPClientAppDAO = mock(SAML2SPClientAppDAO.class);

        logic = new WAClientAppLogic(binder, casSPClientAppDAO, oidcRPClientAppDAO, saml2SPClientAppDAO);
    }

    @AfterEach
    void tearDown() {
        org.apache.syncope.core.logic.LogicTestAuth.clear();
    }

    @Test
    void list() {
        when(oidcRPClientAppDAO.findAll()).thenReturn(List.of());
        when(saml2SPClientAppDAO.findAll()).thenReturn(List.of());
        when(casSPClientAppDAO.findAll()).thenReturn(List.of());

        assertEquals(List.of(), logic.list());
    }
}
