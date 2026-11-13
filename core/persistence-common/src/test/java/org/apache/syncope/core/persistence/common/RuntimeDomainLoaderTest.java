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
package org.apache.syncope.core.persistence.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class RuntimeDomainLoaderTest {

    @Mock
    private DomainHolder<Domain> domainHolder;

    @Mock
    private DomainRegistry<Domain> domainRegistry;

    @Mock
    private DomainOps domainOps;

    @Mock
    private ConfigurableApplicationContext ctx;

    @Mock
    private ConfigurableListableBeanFactory beanFactory;

    private RuntimeDomainLoader<Domain> loader(
            final Map<String, Domain> domains) {

        when(domainHolder.getDomains()).thenReturn((Map) domains);

        return new RuntimeDomainLoader<>(domainHolder, domainRegistry, domainOps, ctx);
    }

    @Test
    void addedDomainAlreadyInitedSkipped() {
        Map<String, Domain> domains = new HashMap<>();
        domains.put("existing", mock(Domain.class));

        RuntimeDomainLoader<Domain> loader = loader(domains);

        Domain domain = mock(Domain.class);
        when(domain.getKey()).thenReturn("existing");

        loader.added(domain);

        verify(domainRegistry, never()).register(any());
        verify(domainOps, never()).deployed(anyString());
    }

    @Test
    void addedNewDomainRegistersAndDeploys() {
        Map<String, Domain> domains = new HashMap<>();
        Domain domain = mock(Domain.class);
        when(domain.getKey()).thenReturn("new-domain");

        RuntimeDomainLoader<Domain> loader = loader(domains);

        when(ctx.getBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBeansOfType(SyncopeCoreLoader.class)).
                thenReturn(Map.of());

        loader.added(domain);

        verify(domainRegistry).register(domain);
        verify(domainOps).deployed("new-domain");
    }

    @Test
    void addedNewDomainCallsLoaders() {
        Map<String, Domain> domains = new HashMap<>();
        Domain domain = mock(Domain.class);
        when(domain.getKey()).thenReturn("new-domain");

        SyncopeCoreLoader loader1 = mock(SyncopeCoreLoader.class);
        when(loader1.getOrder()).thenReturn(1);

        SyncopeCoreLoader loader2 = mock(SyncopeCoreLoader.class);
        when(loader2.getOrder()).thenReturn(2);

        Map<String, SyncopeCoreLoader> loaders = new HashMap<>();
        loaders.put("loader1", loader1);
        loaders.put("loader2", loader2);

        RuntimeDomainLoader<Domain> runtimeLoader = loader(domains);

        when(ctx.getBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBeansOfType(SyncopeCoreLoader.class)).thenReturn(loaders);

        runtimeLoader.added(domain);

        verify(loader1).load("new-domain");
        verify(loader2).load("new-domain");
        verify(domainOps).deployed("new-domain");
    }

    @Test
    void removedDomainNotInitedSkipped() {
        Map<String, Domain> domains = new HashMap<>();
        domains.put("other", mock(Domain.class));

        RuntimeDomainLoader<Domain> loader = loader(domains);

        loader.removed("missing-domain");

        verify(domainRegistry, never()).unregister(anyString());
        assertTrue(domains.containsKey("other"));
        assertFalse(domains.containsKey("missing-domain"));
    }

    @Test
    void removedDomainUnregistersAndUndeploys() {
        Map<String, Domain> domains = new HashMap<>();
        domains.put("existing-domain", mock(Domain.class));

        SyncopeCoreLoader loader1 = mock(SyncopeCoreLoader.class);

        Map<String, SyncopeCoreLoader> loaders = new HashMap<>();
        loaders.put("loader1", loader1);

        RuntimeDomainLoader<Domain> runtimeLoader = loader(domains);

        when(ctx.getBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBeansOfType(SyncopeCoreLoader.class)).thenReturn(loaders);

        runtimeLoader.removed("existing-domain");

        verify(loader1).unload("existing-domain");
        verify(domainRegistry).unregister("existing-domain");
        assertFalse(domains.containsKey("existing-domain"));
    }

    @Test
    void removedDomainCallsLoadersInReverseOrder() {
        Map<String, Domain> domains = new HashMap<>();
        domains.put("existing-domain", mock(Domain.class));

        SyncopeCoreLoader loader1 = mock(SyncopeCoreLoader.class);
        when(loader1.getOrder()).thenReturn(1);

        SyncopeCoreLoader loader2 = mock(SyncopeCoreLoader.class);
        when(loader2.getOrder()).thenReturn(2);

        Map<String, SyncopeCoreLoader> loaders = new HashMap<>();
        loaders.put("loader1", loader1);
        loaders.put("loader2", loader2);

        RuntimeDomainLoader<Domain> runtimeLoader = loader(domains);

        when(ctx.getBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBeansOfType(SyncopeCoreLoader.class)).thenReturn(loaders);

        runtimeLoader.removed("existing-domain");

        verify(loader1, atLeastOnce()).unload("existing-domain");
        verify(loader2, atLeastOnce()).unload("existing-domain");
        verify(domainRegistry).unregister("existing-domain");
        assertFalse(domains.containsKey("existing-domain"));
    }

    @Test
    void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new RuntimeDomainLoader<>(
                domainHolder, domainRegistry, domainOps, ctx));
    }
}
