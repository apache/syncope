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
import javassist.NotFoundException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.propagation.ConnectorFacadeProxy;
import org.syncope.core.util.ApplicationContextManager;

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
        ConfDAO syncopeConfigurationDAO =
                (ConfDAO) context.getBean("confDAOImpl");
        SyncopeConf connectorBundleDir = null;
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
        LOG.debug("Bundle file URLs: {}", bundleFileURLs);

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

    public static ConnectorFacadeProxy getConnector(final String id)
            throws BeansException {

        return (ConnectorFacadeProxy) getBeanFactory().getBean(id);
    }

    public static void registerConnector(final ConnectorInstance instance)
            throws NotFoundException {

        if (getBeanFactory().containsSingleton(instance.getId().toString())) {
            removeConnector(instance.getId().toString());
        }

        ConnectorFacadeProxy connector = new ConnectorFacadeProxy(instance);
        LOG.debug("Connector to be registered: {}", connector);

        getBeanFactory().registerSingleton(
                instance.getId().toString(), connector);
        LOG.debug("Successfully registered bean {}",
                instance.getId().toString());
    }

    public static void removeConnector(final String id) {
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
                LOG.error("register connector {}", instance);
                registerConnector(instance);
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
