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

import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.common.RuntimeDomainLoader;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingEntityManagerFactory;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.context.ConfigurableApplicationContext;

public class JPARuntimeDomainLoader extends RuntimeDomainLoader<JPADomain> {

    protected final DomainRoutingEntityManagerFactory entityManagerFactory;

    public JPARuntimeDomainLoader(
            final DomainHolder<?> domainHolder,
            final DomainRegistry<JPADomain> domainRegistry,
            final DomainRoutingEntityManagerFactory entityManagerFactory,
            final ConfigurableApplicationContext ctx) {

        super(domainHolder, domainRegistry, ctx);
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    protected void onAdd(final Domain domain) {
        AuthContextUtils.runAsAdmin(domain.getKey(), () -> entityManagerFactory.initJPASchema());
    }
}
