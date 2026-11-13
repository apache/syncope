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
package org.apache.syncope.core.keymaster.rest.cxf.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.core.logic.NetworkServiceLogic;
import org.junit.jupiter.api.Test;

class NetworkServiceServiceImplTest {

    private final NetworkServiceLogic logic = mock(NetworkServiceLogic.class);

    private final NetworkServiceServiceImpl service = new NetworkServiceServiceImpl(logic);

    @Test
    void listWithDomain() {
        NetworkService.Type type = NetworkService.Type.CORE;
        String domain = "domain";
        List<NetworkService> expected = List.of(mock(NetworkService.class));
        when(logic.list(type, domain)).thenReturn(expected);

        assertSame(expected, service.list(type, domain));
        verify(logic).list(type, domain);
    }

    @Test
    void listWithoutDomain() {
        NetworkService.Type type = NetworkService.Type.CORE;
        List<NetworkService> expected = List.of(mock(NetworkService.class));
        when(logic.list(type)).thenReturn(expected);

        assertSame(expected, service.list(type, null));
        verify(logic).list(type);
    }

    @Test
    void getWithDomain() {
        NetworkService.Type type = NetworkService.Type.CORE;
        String domain = "domain";
        NetworkService expected = mock(NetworkService.class);
        when(logic.get(type, domain)).thenReturn(expected);

        assertSame(expected, service.get(type, domain));
        verify(logic).get(type, domain);
    }

    @Test
    void getWithoutDomain() {
        NetworkService.Type type = NetworkService.Type.CORE;
        NetworkService expected = mock(NetworkService.class);
        when(logic.get(type)).thenReturn(expected);

        assertSame(expected, service.get(type, null));
        verify(logic).get(type);
    }

    @Test
    void actionRegister() {
        NetworkService networkService = mock(NetworkService.class);

        this.service.action(networkService, NetworkServiceServiceImpl.Action.register);

        verify(logic).register(networkService);
    }

    @Test
    void actionUnregister() {
        NetworkService networkService = mock(NetworkService.class);

        this.service.action(networkService, NetworkServiceServiceImpl.Action.unregister);

        verify(logic).unregister(networkService);
    }
}
