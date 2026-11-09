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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.rest.api.beans.MfaTrustedDeviceQuery;
import org.apache.syncope.core.logic.wa.MfaTrusStorageLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

class MfaTrustStorageServiceTest {

    private MfaTrusStorageLogic logic;

    private MfaTrustStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(MfaTrusStorageLogic.class);
        service = new MfaTrustStorageServiceImpl(logic);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        ReflectionTestUtils.setField(service, "uriInfo", uriInfo);
    }

    @SuppressWarnings("unchecked")
    @Test
    void search() {
        MfaTrustedDeviceQuery query = new MfaTrustedDeviceQuery();
        query.setPage(1);
        query.setSize(10);
        query.setPrincipal("user");
        query.setId(1L);
        OffsetDateTime recordDate = OffsetDateTime.now();
        query.setRecordDate(recordDate);

        MfaTrustedDevice device = new MfaTrustedDevice();

        Page<MfaTrustedDevice> page = mock(Page.class);
        when(page.get()).thenReturn(Stream.of(device));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(10);
        when(page.getTotalElements()).thenReturn(1L);
        when(logic.search(eq("user"), eq(1L), eq(recordDate), any())).thenReturn(page);

        PagedResult<MfaTrustedDevice> result = service.search(query);

        assertEquals(1, result.getPage());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getTotalCount());
        assertEquals(List.of(device), result.getResult());
        verify(logic).search(eq("user"), eq(1L), eq(recordDate), any());
    }

    @Test
    void create() {
        MfaTrustedDevice device = new MfaTrustedDevice();
        service.create("owner", device);
        verify(logic).create("owner", device);
    }

    @Test
    void delete() {
        MfaTrustedDeviceQuery query = new MfaTrustedDeviceQuery();
        OffsetDateTime expirationDate = OffsetDateTime.now();
        query.setExpirationDate(expirationDate);
        query.setRecordKey("recordKey");
        service.delete(query);
        verify(logic).delete(expirationDate, "recordKey");
    }
}
