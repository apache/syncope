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
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthModuleLogicTest {

    private AuthModuleDataBinder binder;

    private AuthModuleDAO authModuleDAO;

    private AuthModuleLogic logic;

    @BeforeEach
    void setUp() {
        binder = mock(AuthModuleDataBinder.class);
        authModuleDAO = mock(AuthModuleDAO.class);
        logic = new AuthModuleLogic(binder, authModuleDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void readMissing() {
        when(authModuleDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void deleteMissing() {
        when(authModuleDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void read() {
        AuthModule authModule = mock(AuthModule.class);
        AuthModuleTO to = new AuthModuleTO();
        to.setKey("key");

        doReturn(Optional.of(authModule)).when(authModuleDAO).findById("key");
        when(binder.getAuthModuleTO(authModule)).thenReturn(to);

        assertSame(to, logic.read("key"));
    }

    @Test
    void delete() {
        AuthModule authModule = mock(AuthModule.class);
        AuthModuleTO to = new AuthModuleTO();
        to.setKey("key");

        doReturn(Optional.of(authModule)).when(authModuleDAO).findById("key");
        when(binder.getAuthModuleTO(authModule)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(authModuleDAO).delete(authModule);
    }

    @Test
    void list() {
        when(authModuleDAO.findAll()).thenReturn(List.of());

        assertEquals(List.of(), logic.list());
    }
}
