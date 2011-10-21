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
package org.syncope.core.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javassist.NotFoundException;

import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.syncope.core.persistence.beans.SyncopeConf;
import org.syncope.core.persistence.dao.ConfDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;

@Component
public class ConnBundleManager {

    private static final Logger LOG = LoggerFactory.getLogger(
            ConnBundleManager.class);

    @Autowired
    private ConfDAO confDAO;

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

    public ConfigurationProperties getConfigurationProperties(
            final String bundleName,
            final String version,
            final String connectorName)
            throws NotFoundException {

        //Create key for search all properties
        final ConnectorKey key =
                new ConnectorKey(bundleName, version, connectorName);

        if (key == null) {
            throw new NotFoundException("Connector Key");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: " + key.getBundleName()
                    + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        //get the specified connector.
        ConnectorInfo info;
        try {
            info = getConnectorManager().findConnectorInfo(key);
            if (info == null) {
                throw new NotFoundException("Connector Info for key " + key);
            }
        } catch (MissingConfKeyException e) {
            throw new NotFoundException("Connector Info for key " + key, e);
        }

        return getConfigurationProperties(info);
    }

    public ConfigurationProperties getConfigurationProperties(
            final ConnectorInfo info)
            throws NotFoundException {

        if (info == null) {
            throw new NotFoundException("Invalid connector info " + info);
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

        return properties;
    }
}
