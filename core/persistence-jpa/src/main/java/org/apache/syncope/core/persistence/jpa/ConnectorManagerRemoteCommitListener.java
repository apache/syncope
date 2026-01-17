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

import jakarta.persistence.EntityManagerFactory;
import java.io.Serializable;
import java.util.List;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of connectors' Spring beans (un)registration in case HA is set up and the actual change is performed by
 * another node in the Hibernate cluster.
 */
public class ConnectorManagerRemoteCommitListener
        implements CacheEntryCreatedListener<Object, Object>,
        CacheEntryUpdatedListener<Object, Object>,
        CacheEntryRemovedListener<Object, Object>,
        Serializable {

    private static final long serialVersionUID = 5260753255454140460L;

    protected static final Logger LOG = LoggerFactory.getLogger(ConnectorManagerRemoteCommitListener.class);

    protected final EntityManagerFactory entityManagerFactory;

    protected final String domain;

    public ConnectorManagerRemoteCommitListener(
            final EntityManagerFactory entityManagerFactory,
            final String domain) {

        this.entityManagerFactory = entityManagerFactory;
        this.domain = domain;
    }

    protected void registerForExternalResource(final String resourceKey) {
        AuthContextUtils.runAsAdmin(domain, () -> {
            if (entityManagerFactory.unwrap(SessionFactoryImpl.class).getCache().
                    contains(JPAExternalResource.class, resourceKey)) {

                LOG.debug("Found in L1 cache, likely originating from local node; ignoring");
                return;
            }

            ApplicationContextProvider.getApplicationContext().
                    getBean(ExternalResourceDAO.class).findById(resourceKey).ifPresentOrElse(
                    resource -> {
                        try {
                            ApplicationContextProvider.getApplicationContext().
                                    getBean(ConnectorManager.class).registerConnector(resource);
                        } catch (Exception e) {
                            LOG.error("While registering connector for resource {}", resourceKey, e);
                        }
                    },
                    () -> LOG.debug("No resource found for '{}', ignoring", resourceKey));
        });
    }

    protected void registerForConnInstance(final String connInstanceKey) {
        AuthContextUtils.runAsAdmin(domain, () -> {
            if (entityManagerFactory.unwrap(SessionFactoryImpl.class).getCache().
                    contains(JPAConnInstance.class, connInstanceKey)) {

                LOG.debug("Found in L1 cache, likely originating from local node; ignoring");
                return;
            }

            List<ExternalResource> resources = ApplicationContextProvider.getApplicationContext().
                    getBean(ExternalResourceDAO.class).findByConnInstance(connInstanceKey);
            if (resources.isEmpty()) {
                LOG.debug("No resources found for connInstance '{}', ignoring", connInstanceKey);
            }

            resources.forEach(resource -> {
                try {
                    ApplicationContextProvider.getApplicationContext().
                            getBean(ConnectorManager.class).registerConnector(resource);
                } catch (Exception e) {
                    LOG.error("While registering connector {} for resource {}", connInstanceKey, resource, e);
                }
            });
        });
    }

    protected void unregister(final String resourceKey) {
        AuthContextUtils.runAsAdmin(domain, () -> {
            ApplicationContextProvider.getApplicationContext().
                    getBean(ExternalResourceDAO.class).findById(resourceKey).ifPresentOrElse(
                    resource -> {
                        try {
                            ApplicationContextProvider.getApplicationContext().
                                    getBean(ConnectorManager.class).unregisterConnector(resource);
                        } catch (Exception e) {
                            LOG.error("While unregistering connector for resource {}", resourceKey, e);
                        }
                    },
                    () -> LOG.debug("No resource found for '{}', ignoring", resourceKey));
        });
    }

    @Override
    public void onCreated(final Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends Object, ? extends Object> event : events) {
            String[] split = event.getKey().toString().split("#");
            if (split.length > 1) {
                if (JPAExternalResource.class.getName().equals(split[0])) {
                    registerForExternalResource(split[1]);
                } else if (JPAConnInstance.class.getName().equals(split[0])) {
                    registerForConnInstance(split[1]);
                }
            }
        }
    }

    @Override
    public void onUpdated(final Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends Object, ? extends Object> event : events) {
            String[] split = event.getKey().toString().split("#");
            if (split.length > 1) {
                if (JPAExternalResource.class.getName().equals(split[0])) {
                    registerForExternalResource(split[1]);
                } else if (JPAConnInstance.class.getName().equals(split[0])) {
                    registerForConnInstance(split[1]);
                }
            }
        }
    }

    @Override
    public void onRemoved(final Iterable<CacheEntryEvent<? extends Object, ? extends Object>> events)
            throws CacheEntryListenerException {

        for (CacheEntryEvent<? extends Object, ? extends Object> event : events) {
            String[] split = event.getKey().toString().split("#");
            if (split.length > 1) {
                if (JPAExternalResource.class.getName().equals(split[0])) {
                    unregister(split[1]);
                }
            }
        }
    }
}
