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
package org.apache.syncope.core.persistence.jpa.openjpa;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.apache.openjpa.event.RemoteCommitEvent;
import org.apache.openjpa.event.RemoteCommitListener;
import org.apache.openjpa.util.StringId;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of connectors' Spring beans (un)registration in case HA is set up and the actual change is performed by
 * another node in the OpenJPA cluster.
 */
public class ConnectorManagerRemoteCommitListener implements RemoteCommitListener, Serializable {

    private static final long serialVersionUID = 5260753255454140460L;

    protected static final Logger LOG = LoggerFactory.getLogger(ConnectorManagerRemoteCommitListener.class);

    protected final String domain;

    public ConnectorManagerRemoteCommitListener(final String domain) {
        this.domain = domain;
    }

    protected void registerForExternalResource(final String resourceKey) {
        AuthContextUtils.runAsAdmin(domain, () -> {
            ExternalResource resource = ApplicationContextProvider.getApplicationContext().
                    getBean(ExternalResourceDAO.class).findById(resourceKey).orElse(null);
            if (resource == null) {
                LOG.debug("No resource found for '{}', ignoring", resourceKey);
            } else {
                try {
                    ApplicationContextProvider.getApplicationContext().
                            getBean(ConnectorManager.class).registerConnector(resource);
                } catch (Exception e) {
                    LOG.error("While registering connector for resource {}", resourceKey, e);
                }
            }
        });
    }

    protected void registerForConnInstance(final String connInstanceKey) {
        AuthContextUtils.runAsAdmin(domain, () -> {
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
            ExternalResource resource = ApplicationContextProvider.getApplicationContext().
                    getBean(ExternalResourceDAO.class).findById(resourceKey).orElse(null);
            if (resource == null) {
                LOG.debug("No resource found for '{}', ignoring", resourceKey);
            } else {
                try {
                    ApplicationContextProvider.getApplicationContext().
                            getBean(ConnectorManager.class).unregisterConnector(resource);
                } catch (Exception e) {
                    LOG.error("While unregistering connector for resource {}", resourceKey, e);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterCommit(final RemoteCommitEvent event) {
        if (event.getPayloadType() == RemoteCommitEvent.PAYLOAD_OIDS_WITH_ADDS) {
            ((Collection<Object>) event.getPersistedObjectIds()).stream().
                    filter(StringId.class::isInstance).
                    map(StringId.class::cast).
                    forEach(id -> {
                        if (JPAExternalResource.class.isAssignableFrom(id.getType())) {
                            registerForExternalResource(id.getId());
                        } else if (JPAConnInstance.class.isAssignableFrom(id.getType())) {
                            registerForConnInstance(id.getId());
                        }
                    });
        }

        if (event.getPayloadType() != RemoteCommitEvent.PAYLOAD_EXTENTS) {
            ((Collection<Object>) event.getUpdatedObjectIds()).stream().
                    filter(StringId.class::isInstance).
                    map(StringId.class::cast).
                    forEach(id -> {
                        if (JPAExternalResource.class.isAssignableFrom(id.getType())) {
                            registerForExternalResource(id.getId());
                        } else if (JPAConnInstance.class.isAssignableFrom(id.getType())) {
                            registerForConnInstance(id.getId());
                        }
                    });

            ((Collection<Object>) event.getDeletedObjectIds()).stream().
                    filter(StringId.class::isInstance).
                    map(StringId.class::cast).
                    forEach(id -> {
                        if (JPAExternalResource.class.isAssignableFrom(id.getType())) {
                            unregister(id.getId());
                        }
                    });
        }
    }

    @Override
    public void close() {
        // nothing to do
    }
}
