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
import org.apache.commons.lang.SerializationUtils;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.ConnectorRegistry;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.propagation.ConnectorFacadeProxy;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.rest.data.ResourceDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.ConnBundleManager;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.types.ConnConfProperty;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Load ConnId connector instances.
 */
@Component
class ConnInstanceLoader implements ConnectorRegistry, ConnectorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConnInstanceLoader.class);

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnBundleManager connBundleManager;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    private String getBeanName(final ExternalResource resource) {
        return String.format("connInstance-%d-%s", resource.getConnector().getId(), resource.getName());
    }

    /* (non-Javadoc)
     * @see org.apache.syncope.core.init.ConnectorFactory#getConnector(org.apache.syncope.core.persistence.beans.ExternalResource)
     */
    @Override
    public ConnectorFacadeProxy getConnector(final ExternalResource resource)
            throws BeansException, NotFoundException {

        // Try to re-create connector bean from underlying resource
        // (useful for managing failover scenarios)
        if (!ApplicationContextProvider.getBeanFactory().containsBean(getBeanName(resource))) {
            registerConnector(resource);
        }

        return (ConnectorFacadeProxy) ApplicationContextProvider.getBeanFactory().getBean(getBeanName(resource));
    }

    public ConnectorFacadeProxy createConnectorBean(final ExternalResource resource)
            throws NotFoundException {

        final ConnInstance connInstanceClone = resourceDataBinder.getConnInstance(resource);
        return createConnectorBean(resource.getConnector(), connInstanceClone.getConfiguration());
    }

    /**
     * Create connector bean starting from connector instance and configuration properties. This method must be used to
     * create a connector instance without any linked external resource.
     *
     * @param connInstance connector instance.
     * @param configuration configuration properties.
     * @return connector facade proxy.
     * @throws NotFoundException when not able to fetch all the required data.
     */
    public ConnectorFacadeProxy createConnectorBean(final ConnInstance connInstance,
            final Set<ConnConfProperty> configuration)
            throws NotFoundException {

        final ConnInstance connInstanceClone = (ConnInstance) SerializationUtils.clone(connInstance);

        connInstanceClone.setConfiguration(configuration);

        return new ConnectorFacadeProxy(connInstanceClone, connBundleManager);
    }

    /* (non-Javadoc)
     * @see org.apache.syncope.core.init.ConnectorRegistry#registerConnector(org.apache.syncope.core.persistence.beans.ExternalResource)
     */
    @Override
    public void registerConnector(final ExternalResource resource)
            throws NotFoundException {

        final ConnectorFacadeProxy connector = createConnectorBean(resource);
        LOG.debug("Connector to be registered: {}", connector);

        final String beanName = getBeanName(resource);

        if (ApplicationContextProvider.getBeanFactory().containsSingleton(beanName)) {
            unregisterConnector(beanName);
        }

        ApplicationContextProvider.getBeanFactory().registerSingleton(beanName, connector);
        LOG.debug("Successfully registered bean {}", beanName);
    }

    /* (non-Javadoc)
     * @see org.apache.syncope.core.init.ConnectorRegistry#unregisterConnector(java.lang.String)
     */
    @Override
    public void unregisterConnector(final String id) {
        ApplicationContextProvider.getBeanFactory().destroySingleton(id);
    }

    @Transactional(readOnly = true)
    public void load() {
        // This is needed to avoid encoding problems when sending error messages via REST
        CurrentLocale.set(Locale.ENGLISH);

        // Next load all resource-specific connectors.
        for (ExternalResource resource : resourceDAO.findAll()) {
            try {
                LOG.info("Registering resource-connector pair {}-{}", resource, resource.getConnector());
                registerConnector(resource);
            } catch (Exception e) {
                LOG.error("While registering resource-connector pair {}-{}", new Object[]{resource,
                            resource.getConnector(), e});
            }
        }

        LOG.info("Done loading {} connectors.", ApplicationContextProvider.getBeanFactory().
                getBeansOfType(ConnectorFacadeProxy.class).size());
    }
}
