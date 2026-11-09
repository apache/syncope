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
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthAccountLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleMfaAuthAccountServiceTest {

    private GoogleMfaAuthAccountLogic logic;

    private GoogleMfaAuthAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(GoogleMfaAuthAccountLogic.class);
        service = new GoogleMfaAuthAccountServiceImpl(logic);
    }

    @Test
    void create() {
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount();
        service.create(account);
        verify(logic).create(account);
    }

    @Test
    void update() {
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount();
        service.update(account);
        verify(logic).update(account);
    }

    @Test
    void deleteByOwner() {
        service.delete("owner");
        verify(logic).delete("owner");
    }

    @Test
    void deleteById() {
        service.delete(1L);
        verify(logic).delete(1L);
    }

    @Test
    void deleteAll() {
        service.deleteAll();
        verify(logic).deleteAll();
    }

    @Test
    void readByOwner() {
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount();
        when(logic.read("owner")).thenReturn(List.of(account));
        PagedResult<GoogleMfaAuthAccount> result = service.read("owner");
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(account), result.getResult());
    }

    @Test
    void readById() {
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount();
        when(logic.read(1L)).thenReturn(account);
        assertSame(account, service.read(1L));
    }

    @Test
    void list() {
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount();
        when(logic.list()).thenReturn(List.of(account));
        PagedResult<GoogleMfaAuthAccount> result = service.list();
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(account), result.getResult());
    }
}
