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

import java.time.LocalDateTime;
import java.util.List;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthTokenLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleMfaAuthTokenServiceTest {

    private GoogleMfaAuthTokenLogic logic;

    private GoogleMfaAuthTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(GoogleMfaAuthTokenLogic.class);
        service = new GoogleMfaAuthTokenServiceImpl(logic);
    }

    @Test
    void deleteAllTokens() {
        service.delete((LocalDateTime) null);
        verify(logic).deleteAll();
    }

    @Test
    void deleteExpiredTokens() {
        LocalDateTime expirationDate = LocalDateTime.now();
        service.delete(expirationDate);
        verify(logic).delete(expirationDate);
    }

    @Test
    void deleteByOwnerAndOtp() {
        service.delete("owner", 1);
        verify(logic).delete("owner", 1);
    }

    @Test
    void deleteByOwner() {
        service.delete("owner");
        verify(logic).delete("owner");
    }

    @Test
    void deleteByOtp() {
        service.delete(1);
        verify(logic).delete(1);
    }

    @Test
    void store() {
        GoogleMfaAuthToken token = new GoogleMfaAuthToken();
        service.store("owner", token);
        verify(logic).store("owner", token);
    }

    @Test
    void readByOwnerAndOtp() {
        GoogleMfaAuthToken token = new GoogleMfaAuthToken();
        when(logic.read("owner", 1)).thenReturn(token);
        assertSame(token, service.read("owner", 1));
    }

    @Test
    void readByOwner() {
        GoogleMfaAuthToken token = new GoogleMfaAuthToken();
        when(logic.read("owner")).thenReturn(List.of(token));
        PagedResult<GoogleMfaAuthToken> result = service.read("owner");
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(token), result.getResult());
    }

    @Test
    void list() {
        GoogleMfaAuthToken token = new GoogleMfaAuthToken();
        when(logic.list()).thenReturn(List.of(token));
        PagedResult<GoogleMfaAuthToken> result = service.list();
        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(token), result.getResult());
    }
}
