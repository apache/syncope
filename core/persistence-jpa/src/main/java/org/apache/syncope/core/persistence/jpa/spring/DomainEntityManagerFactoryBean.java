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
package org.apache.syncope.core.persistence.jpa.spring;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryListener;
import org.apache.syncope.core.persistence.jpa.ConnectorManagerCacheEntryListener;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.openjpa.ImplementationManagerCacheEntryListener;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Extension of {@link LocalContainerEntityManagerFactoryBean} relying on {@link CommonEntityManagerFactoryConf} for
 * common configuration options.
 */
public class DomainEntityManagerFactoryBean extends LocalContainerEntityManagerFactoryBean {

    private static final long serialVersionUID = 49152547930966545L;

    protected final List<CacheEntryListener<Object, Object>> cacheEntryListeners = new ArrayList<>();

    protected ConnectorManagerCacheEntryListener connectorManagerCacheEntryListener;

    protected ImplementationManagerCacheEntryListener implementationManagerCacheEntryListener;

    public void setCommonEntityManagerFactoryConf(final CommonEntityManagerFactoryConf commonEMFConf) {
        super.setJpaPropertyMap(commonEMFConf.getJpaPropertyMap());

        Optional.ofNullable(commonEMFConf.getPackagesToScan()).
                ifPresent(super::setPackagesToScan);

        super.setValidationMode(commonEMFConf.getValidationMode());

        Optional.ofNullable(commonEMFConf.getPersistenceUnitPostProcessors()).
                ifPresent(super::setPersistenceUnitPostProcessors);
    }

    public void setConnectorManagerCacheEntryListener(
            final ConnectorManagerCacheEntryListener connectorManagerCacheEntryListener) {

        this.connectorManagerCacheEntryListener = connectorManagerCacheEntryListener;
    }

    public void setImplementationManagerCacheEntryListener(
            final ImplementationManagerCacheEntryListener implementationManagerCacheEntryListener) {

        this.implementationManagerCacheEntryListener = implementationManagerCacheEntryListener;
    }

    @Override
    protected void postProcessEntityManagerFactory(final EntityManagerFactory emf, final PersistenceUnitInfo pui) {
        super.postProcessEntityManagerFactory(emf, pui);

        Optional.ofNullable(Caching.getCachingProvider().getCacheManager().
                getCache(RegionNameQualifier.INSTANCE.qualify(
                        pui.getPersistenceUnitName(), JPAConnInstance.class.getName()))).
                ifPresent(cache -> cache.registerCacheEntryListener(
                new MutableCacheEntryListenerConfiguration<Object, Object>(
                        FactoryBuilder.factoryOf(connectorManagerCacheEntryListener),
                        null, false, false)));
        Optional.ofNullable(Caching.getCachingProvider().getCacheManager().
                getCache(RegionNameQualifier.INSTANCE.qualify(
                        pui.getPersistenceUnitName(), JPAExternalResource.class.getName()))).
                ifPresent(cache -> cache.registerCacheEntryListener(
                new MutableCacheEntryListenerConfiguration<Object, Object>(
                        FactoryBuilder.factoryOf(connectorManagerCacheEntryListener),
                        null, false, false)));

        Optional.ofNullable(Caching.getCachingProvider().getCacheManager().
                getCache(RegionNameQualifier.INSTANCE.qualify(
                        pui.getPersistenceUnitName(), JPAImplementation.class.getName()))).
                ifPresent(cache -> cache.registerCacheEntryListener(
                new MutableCacheEntryListenerConfiguration<Object, Object>(
                        FactoryBuilder.factoryOf(implementationManagerCacheEntryListener),
                        null, false, false)));
    }
}
