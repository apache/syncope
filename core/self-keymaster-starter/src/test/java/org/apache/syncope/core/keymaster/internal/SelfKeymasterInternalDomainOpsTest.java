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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.KeymasterProperties;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.DomainLogic;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

class SelfKeymasterInternalDomainOpsTest {

    private final DomainLogic logic = mock(DomainLogic.class);

    private final KeymasterProperties props = new KeymasterProperties();

    private SelfKeymasterInternalDomainOps ops;

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

        ops = new SelfKeymasterInternalDomainOps(logic, props);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ApplicationContextProvider.setApplicationContext(previousContext);
        ApplicationContextProvider.setBeanFactory(previousBeanFactory);
    }

    @Test
    void list() {
        List<Domain> expected = List.of(mock(Domain.class));
        when(logic.list()).thenReturn(expected);

        assertEquals(expected, ops.list());
        verify(logic).list();
    }

    @Test
    void read() {
        String key = "domain";
        Domain expected = mock(Domain.class);
        when(logic.read(key)).thenReturn(expected);

        assertSame(expected, ops.read(key));
        verify(logic).read(key);
    }

    @Test
    void create() {
        Domain domain = mock(Domain.class);
        when(domain.getKey()).thenReturn("domain");

        ops.create(domain);

        verify(logic).create(domain);
    }

    @Test
    void deployed() {
        String key = "domain";

        ops.deployed(key);

        verify(logic).deployed(key);
    }

    @Test
    void changeAdminPassword() {
        String key = "domain";
        String password = "new-password";

        ops.changeAdminPassword(key, password, null);

        verify(logic).changeAdminPassword(key, password, null);
    }

    @Test
    void setAdminMfaSecret() {
        String key = "domain";
        String secret = "secret";

        ops.setAdminMfaSecret(key, secret);

        verify(logic).setAdminMfaSecret(key, secret);
    }

    @Test
    void adjustPoolSize() {
        String key = "domain";

        ops.adjustPoolSize(key, 20, 5);

        verify(logic).adjustPoolSize(props.getUsername(), 20, 5);
    }

    @Test
    void delete() {
        String key = "domain";

        ops.delete(key);

        verify(logic).delete(key);
    }
}
