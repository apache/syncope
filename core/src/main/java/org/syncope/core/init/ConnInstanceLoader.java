/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.init;

import org.syncope.core.util.ConnBundleManager;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javassist.NotFoundException;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.types.ConnConfProperty;

/**
 * Load ConnId connector instances.
 */
@Component
public class ConnInstanceLoader extends AbstractLoader {

    private static final Logger LOG = LoggerFactory.getLogger(
            ConnInstanceLoader.class);

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnBundleManager connBundleManager;

    private String getBeanName(final ExternalResource resource) {
        return String.format("connInstance-%d-%s",
                resource.getConnector().getId(), resource.getName());
    }

    /**
     * Get a live connector bean that is registered with the given resource.
     * 
     * @param resource the resource.
     * @return live connector bran for given resource
     * @throws BeansException in case the connector is not registered in the
     * context
     */
    public ConnectorFacadeProxy getConnector(final ExternalResource resource)
            throws BeansException {

        return (ConnectorFacadeProxy) getBeanFactory().getBean(
                getBeanName(resource));
    }

    public ConnectorFacadeProxy createConnectorBean(
            final ExternalResource resource)
            throws NotFoundException {

        final ConnInstance connInstanceClone =
                new ConnInstance(resource.getConnector());

        final Set<ConnConfProperty> configuration =
                new HashSet<ConnConfProperty>();

        final Set<String> propertyNames = new HashSet<String>();

        for (ConnConfProperty prop : resource.getConfiguration()) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                configuration.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        for (ConnConfProperty prop : connInstanceClone.getConfiguration()) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                configuration.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        connInstanceClone.setConfiguration(configuration);

        return new ConnectorFacadeProxy(connInstanceClone, connBundleManager);
    }

    public void registerConnector(final ExternalResource resource)
            throws NotFoundException {

        final ConnectorFacadeProxy connector = createConnectorBean(resource);
        LOG.debug("Connector to be registered: {}", connector);

        final String beanName = getBeanName(resource);

        if (getBeanFactory().containsSingleton(beanName)) {
            unregisterConnector(beanName);
        }

        getBeanFactory().registerSingleton(beanName, connector);
        LOG.debug("Successfully registered bean {}", beanName);
    }

    public void unregisterConnector(final String id) {
        getBeanFactory().destroySingleton(id);
    }

    @Override
    @Transactional(readOnly = true)
    public void load() {
        // This is needed to avoid encoding problems when sending error
        // messages via REST
        CurrentLocale.set(Locale.ENGLISH);

        // Next load all resource-specific connectors.
        for (ExternalResource resource : resourceDAO.findAll()) {
            try {
                LOG.info("Registering resource-connector pair {}-{}",
                        resource, resource.getConnector());
                registerConnector(resource);
            } catch (NotFoundException e) {
                LOG.error(String.format(
                        "While registering resource-connector pair %s-%s",
                        resource, resource.getConnector()), e);
            } catch (RuntimeException e) {
                LOG.error(String.format(
                        "While registering resource-connector pair %s-%s",
                        resource, resource.getConnector()), e);
            }
        }

        LOG.info("Done loading {} connectors.", getBeanFactory().getBeansOfType(
                ConnectorFacadeProxy.class).size());
    }
}
