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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javassist.NotFoundException;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.propagation.ConnectorFacadeProxy;
import org.syncope.core.util.ApplicationContextManager;

/**
 * Load ConnId connectos instances.
 */
@Component
public class ConnInstanceLoader {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ConnInstanceLoader.class);

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ConfDAO confDAO;

    private DefaultListableBeanFactory getBeanFactory() {
        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        return (DefaultListableBeanFactory) context.getBeanFactory();
    }

    public ConnectorInfoManager getConnectorManager()
            throws NotFoundException, MissingConfKeyException {

        // 1. Bundles directory
        SyncopeConf connectorBundleDir =
                confDAO.find("connid.bundles.directory");

        // 2. Find bundles inside that directory
        File bundleDirectory = new File(connectorBundleDir.getValue());
        String[] bundleFiles = bundleDirectory.list();
        if (bundleFiles == null) {
            throw new NotFoundException("Bundles from dir "
                    + connectorBundleDir.getValue());
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
                    + connectorBundleDir.getValue());
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

    public ConnectorFacadeProxy getConnector(final String id)
            throws BeansException {

        return (ConnectorFacadeProxy) getBeanFactory().getBean(id);
    }

    public void registerConnector(final ConnInstance instance)
            throws NotFoundException {

        if (getBeanFactory().containsSingleton(instance.getId().toString())) {
            unregisterConnector(instance.getId().toString());
        }

        ConnectorFacadeProxy connector =
                new ConnectorFacadeProxy(instance, this);
        LOG.debug("Connector to be registered: {}", connector);

        getBeanFactory().registerSingleton(
                instance.getId().toString(), connector);
        LOG.debug("Successfully registered bean {}",
                instance.getId().toString());
    }

    public void unregisterConnector(final String id) {
        getBeanFactory().destroySingleton(id);
    }

    @Transactional(readOnly = true)
    public void loadAllConnInstances() {
        // This is needed to avoid encoding problems when sending error
        // messages via REST
        CurrentLocale.set(Locale.ENGLISH);

        List<ConnInstance> instances = connInstanceDAO.findAll();
        for (ConnInstance instance : instances) {
            try {
                LOG.info("Registering connector {}", instance);
                registerConnector(instance);
            } catch (NotFoundException e) {
                LOG.error("While loading connector bundle for instance "
                        + instance, e);
            } catch (RuntimeException e) {
                LOG.error("While validating connector bundle for instance "
                        + instance, e);
            }
        }
    }
}
