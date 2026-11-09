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
package org.apache.syncope.core.keymaster.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.NetworkServiceLogic;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SelfKeymasterInternalServiceOpsTest {

    private final NetworkServiceLogic logic = mock(NetworkServiceLogic.class);

    private final KeymasterProperties props = new KeymasterProperties();

    private SelfKeymasterInternalServiceOps ops;

    private ConfigurableApplicationContext previousContext;

    private DefaultListableBeanFactory previousBeanFactory;

    @BeforeEach
    void setUp() {
        previousContext = ApplicationContextProvider.getApplicationContext();
        previousBeanFactory = ApplicationContextProvider.getBeanFactory();

        SecurityContextHolder.clearContext();

        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());

        props.setUsername("keymaster");
        props.setPassword("password");

        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setAdminUser("admin");

        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("securityProperties", securityProperties);
        context.refresh();

        ApplicationContextProvider.setApplicationContext(context);
        ApplicationContextProvider.setBeanFactory((DefaultListableBeanFactory) context.getBeanFactory());

        ops = new SelfKeymasterInternalServiceOps(logic, props);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ApplicationContextProvider.setApplicationContext(previousContext);
        ApplicationContextProvider.setBeanFactory(previousBeanFactory);
    }

    @Test
    void listByType() {
        NetworkService.Type type = NetworkService.Type.CORE;
        List<NetworkService> expected = List.of(mock(NetworkService.class));
        when(logic.list(type)).thenReturn(expected);

        assertEquals(expected, ops.list(type));
        verify(logic).list(type);
    }

    @Test
    void listByTypeAndDomain() {
        NetworkService.Type type = NetworkService.Type.CORE;
        String domain = "domain";
        List<NetworkService> expected = List.of(mock(NetworkService.class));
        when(logic.list(type, domain)).thenReturn(expected);

        assertEquals(expected, ops.list(type, domain));
        verify(logic).list(type, domain);
    }

    @Test
    void getByType() {
        NetworkService.Type type = NetworkService.Type.CORE;
        NetworkService expected = mock(NetworkService.class);
        when(logic.get(type)).thenReturn(expected);

        assertSame(expected, ops.get(type));
        verify(logic).get(type);
    }

    @Test
    void getByTypeAndDomain() {
        NetworkService.Type type = NetworkService.Type.CORE;
        String domain = "domain";
        NetworkService expected = mock(NetworkService.class);
        when(logic.get(type, domain)).thenReturn(expected);

        assertSame(expected, ops.get(type, domain));
        verify(logic).get(type, domain);
    }

    @Test
    void getWrapsException() {
        NetworkService.Type type = NetworkService.Type.CORE;
        when(logic.get(type)).thenThrow(new RuntimeException("boom"));

        assertThrows(KeymasterException.class, () -> ops.get(type));
        verify(logic).get(type);
    }

    @Test
    void register() {
        NetworkService service = mock(NetworkService.class);

        ops.register(service);

        verify(logic).register(service);
    }

    @Test
    void unregister() {
        NetworkService service = mock(NetworkService.class);

        ops.unregister(service);

        verify(logic).unregister(service);
    }
}
