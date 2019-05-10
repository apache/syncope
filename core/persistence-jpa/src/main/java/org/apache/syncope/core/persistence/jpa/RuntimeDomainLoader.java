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
package org.apache.syncope.core.persistence.jpa;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.DomainWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.springframework.aop.support.AopUtils;

@Component
public class RuntimeDomainLoader implements DomainWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeDomainLoader.class);

    @Autowired
    private DomainHolder domainHolder;

    @Autowired
    private DomainOps domainOps;

    @Autowired
    private DomainRegistry domainRegistry;

    @Override
    public void update(final List<String> domains) {
        domains.stream().filter(domain -> !domainHolder.getDomains().containsKey(domain)).
                map(domain -> {
                    try {
                        return domainOps.read(domain);
                    } catch (Exception e) {
                        LOG.error("Could not read {}", domain, e);
                        return null;
                    }
                }).
                filter(Objects::nonNull).
                forEach(domain -> {
                    LOG.info("Domain {} initialization", domain.getKey());

                    domainRegistry.register(domain);

                    ApplicationContextProvider.getApplicationContext().getBeansOfType(SyncopeCoreLoader.class).values().
                            stream().sorted(Comparator.comparing(SyncopeCoreLoader::getOrder)).
                            forEach(loader -> {
                                String loaderName = AopUtils.getTargetClass(loader).getName();

                                loader.load();

                                LOG.debug("[{}] Starting on domain '{}'", loaderName, domain);
                                loader.load(domain.getKey(), domainHolder.getDomains().get(domain.getKey()));
                                LOG.debug("[{}] Completed on domain '{}'", loaderName, domain);
                            });

                    LOG.info("Domain {} successfully inited", domain.getKey());
                });
    }
}
