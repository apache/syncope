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
package org.apache.syncope.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage information about ConnId connector bundles.
 */
public final class ConnIdBundleManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConnIdBundleManager.class);

    /**
     * Where to find conf properties for ConnId.
     */
    public static final String CONNID_PROPS = "/connid.properties";

    /**
     * Directory path containing ConnId bundles.
     */
    private static String connIdBundlesDir;

    /**
     * Lock for operating on ConnectorInfoManager shared instance.
     */
    private static final Object LOCK = new Object();

    /**
     * ConnectorInfoManager shared instance.
     */
    private static ConnectorInfoManager connManager;

    static {
        InputStream propStream = null;
        try {
            propStream = ConnIdBundleManager.class.getResourceAsStream(CONNID_PROPS);
            Properties props = new Properties();
            props.load(propStream);
            connIdBundlesDir = props.getProperty("bundles.directory");
        } catch (Exception e) {
            LOG.error("Could not load {}", CONNID_PROPS, e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
    }

    private static void initConnManager() {
        // 1. Find bundles inside connidBundlesDir
        File bundleDirectory = new File(connIdBundlesDir);
        String[] bundleFiles = bundleDirectory.list();
        if (bundleFiles == null) {
            throw new NotFoundException("Connector bundles directory " + connIdBundlesDir);
        }

        List<URL> bundleFileURLs = new ArrayList<URL>();
        for (String file : bundleFiles) {
            try {
                bundleFileURLs.add(IOUtil.makeURL(bundleDirectory, file));
            } catch (IOException ignore) {
                // ignore exception and don't add bundle
                LOG.debug("{}/{} is not a valid connector bundle", bundleDirectory.toString(), file, ignore);
            }
        }

        if (bundleFileURLs.isEmpty()) {
            LOG.warn("No connector bundles found in {}", connIdBundlesDir);
        }
        LOG.debug("Bundle file URLs: {}", bundleFileURLs);

        // 2. Get connector info manager
        ConnectorInfoManager manager = ConnectorInfoManagerFactory.getInstance().getLocalManager(
                bundleFileURLs.toArray(new URL[bundleFileURLs.size()]));
        if (manager == null) {
            throw new NotFoundException("Connector Info Manager");
        }

        connManager = manager;
    }

    public static void resetConnManager() {
        synchronized (LOCK) {
            connManager = null;
        }
    }

    public static ConnectorInfoManager getConnManager() {
        synchronized (LOCK) {
            if (connManager == null) {
                initConnManager();
            }
        }
        return connManager;
    }

    public static ConfigurationProperties getConfProps(
            final String bundleName, final String bundleVersion, final String connectorName) {

        // create key for search all properties
        final ConnectorKey key = new ConnectorKey(bundleName, bundleVersion, connectorName);
        if (key == null) {
            throw new NotFoundException("Connector Key");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: " + key.getBundleName()
                    + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        // get the specified connector
        ConnectorInfo info = getConnManager().findConnectorInfo(key);
        if (info == null) {
            throw new NotFoundException("Connector Info for key " + key);
        }

        return getConfProps(info);
    }

    public static ConfigurationProperties getConfProps(final ConnectorInfo info) {
        if (info == null) {
            throw new NotFoundException("Invalid: connector info is null");
        }

        // create default configuration
        final APIConfiguration apiConfig = info.createDefaultAPIConfiguration();
        if (apiConfig == null) {
            throw new NotFoundException("Default API configuration");
        }

        // retrieve the ConfigurationProperties.
        final ConfigurationProperties properties = apiConfig.getConfigurationProperties();
        if (properties == null) {
            throw new NotFoundException("Configuration properties");
        }

        if (LOG.isDebugEnabled()) {
            for (String propName : properties.getPropertyNames()) {
                LOG.debug("Property Name: {}\nProperty Type: {}",
                        properties.getProperty(propName).getName(), properties.getProperty(propName).getType());
            }
        }

        return properties;
    }

    private ConnIdBundleManager() {
        // Empty constructor for static utility class.
    }
}
