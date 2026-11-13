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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.api.entity.am.SAML2IdPEntity;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPEntityDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SAML2IdPEntityLogicTest {

    private SAML2IdPEntityDataBinder binder;

    private SAML2IdPEntityDAO saml2IdPEntityDAO;

    private SAML2IdPEntityLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(SAML2IdPEntityDataBinder.class);
        saml2IdPEntityDAO = mock(SAML2IdPEntityDAO.class);
        logic = new SAML2IdPEntityLogic(binder, saml2IdPEntityDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void getMissing() {
        when(saml2IdPEntityDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.get("missing"));
    }

    @Test
    void get() {
        SAML2IdPEntity entity = mock(SAML2IdPEntity.class);
        SAML2IdPEntityTO to = new SAML2IdPEntityTO();
        to.setKey("key");

        doReturn(Optional.of(entity)).when(saml2IdPEntityDAO).findById("key");
        when(binder.getSAML2IdPEntityTO(entity)).thenReturn(to);

        assertSame(to, logic.get("key"));
    }

    @Test
    void delete() {
        logic.delete("key");

        verify(saml2IdPEntityDAO).deleteById("key");
    }

    @Test
    void list() {
        when(saml2IdPEntityDAO.findAll()).thenReturn(List.of());

        assertEquals(List.of(), logic.list());
    }
}
