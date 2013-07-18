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
package org.apache.syncope.core.init;

import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.ConnectorRegistry;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.propagation.impl.ConnectorFacadeProxy;
import org.apache.syncope.core.rest.data.ResourceDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.ConnIdBundleManager;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Load ConnId connector instances.
 */
@Component
class ConnectorManager implements ConnectorRegistry, ConnectorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorManager.class);

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    private String getBeanName(final ExternalResource resource) {
        return String.format("connInstance-%d-%s", resource.getConnector().getId(), resource.getName());
    }

    @Override
    public Connector getConnector(final ExternalResource resource) {
        // Try to re-create connector bean from underlying resource (useful for managing failover scenarios)
        if (!ApplicationContextProvider.getBeanFactory().containsBean(getBeanName(resource))) {
            registerConnector(resource);
        }

        return (Connector) ApplicationContextProvider.getBeanFactory().getBean(getBeanName(resource));
    }

    @Override
    public Connector createConnector(final ConnInstance connInstance, final Set<ConnConfProperty> configuration) {
        final ConnInstance connInstanceClone = (ConnInstance) SerializationUtils.clone(connInstance);

        connInstanceClone.setConfiguration(configuration);

        Connector connector = new ConnectorFacadeProxy(connInstanceClone);
        ApplicationContextProvider.getBeanFactory().autowireBean(connector);

        return connector;
    }

    @Override
    public void registerConnector(final ExternalResource resource) {
        final ConnInstance connInstance = resourceDataBinder.getConnInstance(resource);
        final Connector connector = createConnector(resource.getConnector(), connInstance.getConfiguration());
        LOG.debug("Connector to be registered: {}", connector);

        final String beanName = getBeanName(resource);

        if (ApplicationContextProvider.getBeanFactory().containsSingleton(beanName)) {
            unregisterConnector(beanName);
        }

        ApplicationContextProvider.getBeanFactory().registerSingleton(beanName, connector);
        LOG.debug("Successfully registered bean {}", beanName);
    }

    @Override
    public void unregisterConnector(final String id) {
        ApplicationContextProvider.getBeanFactory().destroySingleton(id);
    }

    @Transactional(readOnly = true)
    @Override
    public void load() {
        // This is needed in order to avoid encoding problems when sending error messages via REST
        CurrentLocale.set(Locale.ENGLISH);

        // Load all connector bundles
        ConnIdBundleManager.getConnManagers();

        // Load all resource-specific connectors
        int connectors = 0;
        for (ExternalResource resource : resourceDAO.findAll()) {
            LOG.info("Registering resource-connector pair {}-{}", resource, resource.getConnector());
            try {
                registerConnector(resource);
                connectors++;
            } catch (Exception e) {
                LOG.error("While registering resource-connector pair {}-{}", resource, resource.getConnector(), e);
            }
        }

        LOG.info("Done loading {} connectors", connectors);
    }

    @Transactional(readOnly = true)
    @Override
    public void unload() {
        int connectors = 0;
        for (ExternalResource resource : resourceDAO.findAll()) {
            final String beanName = getBeanName(resource);
            if (ApplicationContextProvider.getBeanFactory().containsSingleton(beanName)) {
                LOG.info("Unegistering resource-connector pair {}-{}", resource, resource.getConnector());
                unregisterConnector(beanName);
                connectors++;
            }
        }

        LOG.info("Done unloading {} connectors", connectors);

        ConnectorFacadeFactory.getInstance().dispose();
        ConnIdBundleManager.resetConnManagers();
        LOG.info("All connector resources disposed");
    }
}
