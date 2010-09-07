/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence;

import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.client.to.PropertyTO;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.SyncopeConfiguration;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.SyncopeConfigurationDAO;
import org.syncope.core.persistence.util.ApplicationContextManager;
import org.syncope.core.rest.controller.ConnectorInstanceController;
import org.syncope.core.rest.data.ConnectorInstanceDataBinder;

/**
 * Load identity connector instances on application startup.
 */
public class ConnectorInstanceLoader implements ServletContextListener {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ConnectorInstanceLoader.class);

    private static ConnectorInfoManager getConnectorManager() {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        SyncopeConfigurationDAO syncopeConfigurationDAO =
                (SyncopeConfigurationDAO) context.getBean(
                "syncopeConfigurationDAOImpl");

        ConnectorInfoManager manager = null;
        try {
            SyncopeConfiguration connectorBundleDir =
                    syncopeConfigurationDAO.find(
                    "identityconnectors.bundle.directory");
            manager = ConnectorInstanceController.getConnectorManager(
                    connectorBundleDir.getConfValue());
        } catch (MissingConfKeyException e) {
            LOG.error("Missing configuration", e);
        } catch (NotFoundException e) {
            LOG.error("Could not find Connector Manager", e);
        }

        return manager;
    }

    private static synchronized DefaultListableBeanFactory getBeanFactory() {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        return (DefaultListableBeanFactory) context.getBeanFactory();
    }

    public static ConnectorFacade getConnectorFacade(final String id)
            throws BeansException {

        return (ConnectorFacade) getBeanFactory().getBean(id);
    }

    public static void registerConnectorFacade(final ConnectorInstance instance)
            throws NotFoundException {

        if (getBeanFactory().containsSingleton(instance.getId().toString())) {
            removeConnectorFacade(instance.getId().toString());
        }

        ConnectorFacade connector =
                ConnectorInstanceController.getConnectorFacade(
                getConnectorManager(),
                instance.getBundleName(),
                instance.getVersion(),
                instance.getConnectorName(),
                (Set<PropertyTO>) ConnectorInstanceDataBinder.buildFromXML(
                instance.getXmlConfiguration()));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Connector instance " + connector);
        }

        getBeanFactory().registerSingleton(
                instance.getId().toString(), connector);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Registered bean " + instance.getId().toString());
        }
    }

    public static void removeConnectorFacade(final String id) {
        getBeanFactory().destroySingleton(id);
    }

    @Override
    public final void contextInitialized(final ServletContextEvent sce) {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        ConnectorInstanceDAO connectorInstanceDAO =
                (ConnectorInstanceDAO) context.getBean(
                "connectorInstanceDAOImpl");

        List<ConnectorInstance> instances = connectorInstanceDAO.findAll();
        for (ConnectorInstance instance : instances) {
            try {
                registerConnectorFacade(instance);
            } catch (NotFoundException e) {
                LOG.error("While loading connector bundle for instance "
                        + instance, e);
            }
        }
    }

    @Override
    public final void contextDestroyed(final ServletContextEvent sce) {
    }
}
