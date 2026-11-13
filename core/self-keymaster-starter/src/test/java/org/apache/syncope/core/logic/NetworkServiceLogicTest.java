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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.keymaster.NetworkServiceDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.keymaster.NetworkServiceEntity;
import org.junit.jupiter.api.Test;

class NetworkServiceLogicTest {

    private final NetworkServiceDAO serviceDAO = mock(NetworkServiceDAO.class);

    private final EntityFactory entityFactory = mock(EntityFactory.class);

    private final NetworkServiceLogic logic = new NetworkServiceLogic(serviceDAO, entityFactory);

    @Test
    void listByType() {
        NetworkService.Type type = NetworkService.Type.CORE;
        NetworkServiceEntity entity = mock(NetworkServiceEntity.class);
        when(entity.getType()).thenReturn(type);
        when(entity.getAddress()).thenReturn("address");
        when(entity.getDomain()).thenReturn("domain");
        when(serviceDAO.findAll(type)).thenReturn(List.of(entity));

        List<NetworkService> result = logic.list(type);

        assertEquals(1, result.size());
        assertEquals(type, result.getFirst().getType());
        assertEquals("address", result.getFirst().getAddress());
        assertEquals("domain", result.getFirst().getDomain());
    }

    @Test
    void listByTypeAndDomain() {
        NetworkService.Type type = NetworkService.Type.CORE;
        String domain = "domain";
        NetworkServiceEntity entity = mock(NetworkServiceEntity.class);
        when(entity.getType()).thenReturn(type);
        when(entity.getAddress()).thenReturn("address");
        when(entity.getDomain()).thenReturn(domain);
        when(serviceDAO.findAll(type, domain)).thenReturn(List.of(entity));

        List<NetworkService> result = logic.list(type, domain);

        assertEquals(1, result.size());
        assertEquals(type, result.getFirst().getType());
        assertEquals("address", result.getFirst().getAddress());
        assertEquals(domain, result.getFirst().getDomain());
    }

    @Test
    void getWhenEmpty() {
        NetworkService.Type type = NetworkService.Type.CORE;
        when(serviceDAO.findAll(type)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> logic.get(type));
    }

    @Test
    void getWhenSingle() {
        NetworkService.Type type = NetworkService.Type.CORE;
        NetworkServiceEntity entity = mock(NetworkServiceEntity.class);
        when(entity.getType()).thenReturn(type);
        when(entity.getAddress()).thenReturn("address");
        when(entity.getDomain()).thenReturn("domain");
        when(serviceDAO.findAll(type)).thenReturn(List.of(entity));

        NetworkService result = logic.get(type);

        assertEquals(type, result.getType());
        assertEquals("address", result.getAddress());
        assertEquals("domain", result.getDomain());
    }

    @Test
    void getWhenMultipleReturnsOne() {
        NetworkService.Type type = NetworkService.Type.CORE;
        NetworkServiceEntity e1 = mock(NetworkServiceEntity.class);
        when(e1.getType()).thenReturn(type);
        when(e1.getAddress()).thenReturn("addr1");
        when(e1.getDomain()).thenReturn("d1");
        NetworkServiceEntity e2 = mock(NetworkServiceEntity.class);
        when(e2.getType()).thenReturn(type);
        when(e2.getAddress()).thenReturn("addr2");
        when(e2.getDomain()).thenReturn("d2");
        when(serviceDAO.findAll(type)).thenReturn(List.of(e1, e2));

        NetworkService result = logic.get(type);

        assertNotNull(result);
        assertEquals(type, result.getType());
    }

    @Test
    void getByTypeAndDomainWhenEmpty() {
        NetworkService.Type type = NetworkService.Type.CORE;
        String domain = "domain";
        when(serviceDAO.findAll(type, domain)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> logic.get(type, domain));
    }

    @Test
    void registerSkipsWhenAlreadyExists() {
        NetworkService service = mock(NetworkService.class);
        when(service.getType()).thenReturn(NetworkService.Type.CORE);
        when(service.getAddress()).thenReturn("address");
        when(service.getDomain()).thenReturn("domain");

        NetworkServiceEntity existing = mock(NetworkServiceEntity.class);
        when(existing.getAddress()).thenReturn("address");
        when(serviceDAO.findAll(NetworkService.Type.CORE)).thenReturn(List.of(existing));

        logic.register(service);

        verify(entityFactory, never()).newEntity(any());
        verify(serviceDAO, never()).save(any());
    }

    @Test
    void registerCreatesWhenNotExists() {
        NetworkService service = mock(NetworkService.class);
        when(service.getType()).thenReturn(NetworkService.Type.CORE);
        when(service.getAddress()).thenReturn("address");
        when(service.getDomain()).thenReturn("domain");

        when(serviceDAO.findAll(NetworkService.Type.CORE)).thenReturn(List.of());

        NetworkServiceEntity entity = mock(NetworkServiceEntity.class);
        when(entityFactory.newEntity(eq(NetworkServiceEntity.class))).thenReturn(entity);

        logic.register(service);

        verify(entity).setType(NetworkService.Type.CORE);
        verify(entity).setAddress("address");
        verify(entity).setDomain("domain");
        verify(serviceDAO).save(entity);
    }

    @Test
    void unregister() {
        NetworkService service = mock(NetworkService.class);

        logic.unregister(service);

        verify(serviceDAO).deleteAll(service);
    }

    @Test
    void resolveReferenceThrows() {
        assertThrows(UnsupportedOperationException.class,
                () -> logic.resolveReference(null, new Object[0]));
    }
}
