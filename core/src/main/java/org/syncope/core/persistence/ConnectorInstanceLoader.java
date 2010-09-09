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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
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

    public static ConnectorInfoManager getConnectorManager()
            throws NotFoundException {

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        // 1. Bundles directory
        SyncopeConfigurationDAO syncopeConfigurationDAO =
                (SyncopeConfigurationDAO) context.getBean(
                "syncopeConfigurationDAOImpl");
        SyncopeConfiguration connectorBundleDir = null;
        try {
            connectorBundleDir = syncopeConfigurationDAO.find(
                    "identityconnectors.bundle.directory");
        } catch (MissingConfKeyException e) {
            LOG.error("Missing configuration", e);
        }

        // 2. Find bundles inside that directory
        File bundleDirectory = new File(connectorBundleDir.getConfValue());
        String[] bundleFiles = bundleDirectory.list();
        if (bundleFiles == null) {
            throw new NotFoundException("Bundles from dir "
                    + connectorBundleDir.getConfValue());
        }

        List<URL> bundleFileURLs = new ArrayList<URL>();
        for (String file : bundleFiles) {
            try {
                bundleFileURLs.add(IOUtil.makeURL(bundleDirectory, file));
            } catch (Exception ignore) {
                // ignore exception and don't add bundle
                if (LOG.isDebugEnabled()) {
                    LOG.debug(bundleDirectory.toString() + "/" + file + "\""
                            + " is not a valid connector bundle.", ignore);
                }
            }
        }
        if (bundleFileURLs.isEmpty()) {
            throw new NotFoundException("Bundles from dir "
                    + connectorBundleDir.getConfValue());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Bundle file URLs: " + bundleFileURLs);
        }

        // 3. Get connector info manager
        ConnectorInfoManager manager =
                ConnectorInfoManagerFactory.getInstance().getLocalManager(
                bundleFileURLs.toArray(new URL[0]));
        if (manager == null) {
            throw new NotFoundException("Connector Info Manager");
        }

        return manager;
    }

    private static synchronized DefaultListableBeanFactory getBeanFactory() {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        return (DefaultListableBeanFactory) context.getBeanFactory();
    }

    private static ConnectorFacade getConnectorFacade(String bundlename,
            String bundleversion, String connectorname,
            Set<PropertyTO> configuration) throws NotFoundException {

        // specify a connector.
        ConnectorKey key = new ConnectorKey(
                bundlename,
                bundleversion,
                connectorname);

        if (key == null) {
            throw new NotFoundException("Connector Key");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: " + key.getBundleName()
                    + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        // get the specified connector.
        ConnectorInfo info = getConnectorManager().findConnectorInfo(key);

        if (info == null) {
            throw new NotFoundException("Connector Info");
        }

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        if (apiConfig == null) {
            throw new NotFoundException("Default API configuration");
        }

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties =
                apiConfig.getConfigurationProperties();

        if (properties == null) {
            throw new NotFoundException("Configuration properties");
        }

        // Print out what the properties are (not necessary)
        if (LOG.isDebugEnabled()) {
            for (String propName : properties.getPropertyNames()) {
                LOG.debug("\nProperty Name: "
                        + properties.getProperty(propName).getName()
                        + "\nProperty Type: "
                        + properties.getProperty(propName).getType());
            }
        }

        // Set all of the ConfigurationProperties needed by the connector.
        for (PropertyTO property : configuration) {
            properties.setPropertyValue(
                    property.getKey(), property.getValue());
        }

        // Use the ConnectorFacadeFactory's newInstance() method to get
        // a new connector.
        ConnectorFacade connector =
                ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        if (connector == null) {
            throw new NotFoundException("Connector");
        }

        // Make sure we have set up the Configuration properly
        connector.validate();
        //connector.test(); //needs a target resource deployed

        return connector;
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

        ConnectorFacade connector = getConnectorFacade(
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
