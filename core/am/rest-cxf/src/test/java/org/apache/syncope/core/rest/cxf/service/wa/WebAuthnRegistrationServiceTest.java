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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.lib.wa.WebAuthnAccount;
import org.apache.syncope.core.logic.wa.WebAuthnRegistrationLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebAuthnRegistrationServiceTest {

    private WebAuthnRegistrationLogic logic;

    private WebAuthnRegistrationServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(WebAuthnRegistrationLogic.class);
        service = new WebAuthnRegistrationServiceImpl(logic);
    }

    @Test
    void list() {
        WebAuthnAccount account = new WebAuthnAccount();
        when(logic.list()).thenReturn(List.of(account));
        assertEquals(List.of(account), service.list());
    }

    @Test
    void read() {
        WebAuthnAccount account = new WebAuthnAccount();
        when(logic.read("owner")).thenReturn(account);
        assertSame(account, service.read("owner"));
    }

    @Test
    void deleteByOwner() {
        service.delete("owner");
        verify(logic).delete("owner");
    }

    @Test
    void deleteByOwnerAndCredential() {
        service.delete("owner", "credential");
        verify(logic).delete("owner", "credential");
    }

    @Test
    void create() {
        WebAuthnAccount account = new WebAuthnAccount();
        service.create("owner", account);
        verify(logic).create("owner", account);
    }

    @Test
    void update() {
        WebAuthnAccount account = new WebAuthnAccount();
        service.update("owner", account);
        verify(logic).update("owner", account);
    }
}
