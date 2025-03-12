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

import java.util.Comparator;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Async;

public class RuntimeDomainLoader<D extends Domain> implements DomainWatcher {

    protected static final Logger LOG = LoggerFactory.getLogger(RuntimeDomainLoader.class);

    protected final DomainHolder<?> domainHolder;

    protected final DomainRegistry<D> domainRegistry;

    protected final DomainOps domainOps;

    public RuntimeDomainLoader(
            final DomainHolder<?> domainHolder,
            final DomainRegistry<D> domainRegistry,
            final DomainOps domainOps,
            final ConfigurableApplicationContext ctx) {

        this.domainHolder = domainHolder;
        this.domainRegistry = domainRegistry;
        this.domainOps = domainOps;

        // only needed by ZookeeperDomainOps' early init on afterPropertiesSet
        if (ApplicationContextProvider.getApplicationContext() == null) {
            ApplicationContextProvider.setApplicationContext(ctx);
        }
    }

    @Async
    @SuppressWarnings("unchecked")
    @Override
    public void added(final Domain domain) {
        if (domainHolder.getDomains().containsKey(domain.getKey())) {
            LOG.debug("Domain {} already inited, skipping", domain.getKey());
        } else {
            LOG.info("Domain {} registration", domain.getKey());

            domainRegistry.register((D) domain);

            ApplicationContextProvider.getBeanFactory().getBeansOfType(SyncopeCoreLoader.class).values().
                    stream().sorted(Comparator.comparing(SyncopeCoreLoader::getOrder)).
                    forEachOrdered(loader -> {
                        String loaderName = AopUtils.getTargetClass(loader).getName();

                        LOG.debug("[{}] Starting on domain '{}'", loaderName, domain);

                        loader.load(domain.getKey());

                        LOG.debug("[{}] Completed on domain '{}'", loaderName, domain);
                    });

            domainOps.deployed(domain.getKey());
            LOG.info("Domain {} successfully deployed", domain.getKey());
        }
    }

    @Async
    @Override
    public void removed(final String domain) {
        if (domainHolder.getDomains().containsKey(domain)) {
            LOG.info("Domain {} unregistration", domain);

            ApplicationContextProvider.getBeanFactory().getBeansOfType(SyncopeCoreLoader.class).values().
                    stream().sorted(Comparator.comparing(SyncopeCoreLoader::getOrder).reversed()).
                    forEachOrdered(loader -> {
                        String loaderName = AopUtils.getTargetClass(loader).getName();

                        LOG.debug("[{}] Starting dispose on domain '{}'", loaderName, domain);

                        loader.unload(domain);

                        LOG.debug("[{}] Dispose completed on domain '{}'", loaderName, domain);
                    });

            domainRegistry.unregister(domain);

            domainHolder.getDomains().remove(domain);

            LOG.info("Domain {} successfully undeployed", domain);
        } else {
            LOG.debug("Domain {} not inited, skipping", domain);
        }
    }
}
