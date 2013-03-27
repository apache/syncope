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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.api.RemoteFrameworkConnectionInfo;
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
     * ConnId Locations.
     */
    private static final List<URI> LOCATIONS;

    /**
     * ConnectorInfoManager instances.
     */
    private static final Map<URI, ConnectorInfoManager> CONN_MANAGERS;

    static {
        String[] stringLocations = null;

        InputStream propStream = null;
        try {
            propStream = ConnIdBundleManager.class.getResourceAsStream(CONNID_PROPS);
            Properties props = new Properties();
            props.load(propStream);
            stringLocations = props.getProperty("connid.locations").split(",");

            LOG.debug("ConnId locations: {}", Arrays.asList(stringLocations));
        } catch (Exception e) {
            LOG.error("Could not load {}", CONNID_PROPS, e);
            stringLocations = new String[0];
        } finally {
            IOUtils.closeQuietly(propStream);
        }

        List<URI> locations = new ArrayList<URI>();
        for (String location : stringLocations) {
            try {
                locations.add(new URI(location.trim()));
                LOG.info("Valid ConnId location: {}", location.trim());
            } catch (URISyntaxException e) {
                LOG.error("Invalid ConnId location: {}", location.trim(), e);
            }
        }
        LOCATIONS = Collections.unmodifiableList(locations);
        CONN_MANAGERS = Collections.synchronizedMap(new LinkedHashMap<URI, ConnectorInfoManager>());
    }

    private static void initLocal(final URI location) {
        // 1. Find bundles inside local directory
        File bundleDirectory = new File(location);
        String[] bundleFiles = bundleDirectory.list();
        if (bundleFiles == null) {
            throw new NotFoundException("Local bundles directory " + location);
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
            LOG.warn("No connector bundles found in {}", location);
        }
        LOG.debug("Configuring local connector server:"
                + "\n\tFiles: {}", bundleFileURLs);

        // 2. Get connector info manager
        ConnectorInfoManager manager = ConnectorInfoManagerFactory.getInstance().getLocalManager(
                bundleFileURLs.toArray(new URL[bundleFileURLs.size()]));
        if (manager == null) {
            throw new NotFoundException("Local ConnectorInfoManager");
        }

        CONN_MANAGERS.put(location, manager);
    }

    private static void initRemote(final URI location) {
        // 1. Extract conf params for remote connection from given URI
        final String host = location.getHost();
        final int port = location.getPort();
        final GuardedString key = new GuardedString(location.getUserInfo().toCharArray());
        final boolean useSSL = location.getScheme().equals("connids");

        final List<TrustManager> trustManagers = new ArrayList<TrustManager>();
        final String[] params = StringUtils.isBlank(location.getQuery()) ? null : location.getQuery().split("&");
        if (params != null && params.length > 0) {
            final String[] trustAllCerts = params[0].split("=");
            if (trustAllCerts != null && trustAllCerts.length > 1
                    && "trustAllCerts".equalsIgnoreCase(trustAllCerts[0])
                    && "true".equalsIgnoreCase(trustAllCerts[1])) {

                trustManagers.add(new X509TrustManager() {

                    @Override
                    public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                            throws CertificateException {
                        // no checks, trust all
                    }

                    @Override
                    public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                            throws CertificateException {
                        // no checks, trust all
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                });
            }
        }

        LOG.debug("Configuring remote connector server:"
                + "\n\tHost: {}"
                + "\n\tPort: {}"
                + "\n\tKey: {}"
                + "\n\tUseSSL: {}"
                + "\n\tTrustAllCerts: {}",
                host, port, key, useSSL, !trustManagers.isEmpty());

        RemoteFrameworkConnectionInfo info =
                new RemoteFrameworkConnectionInfo(host, port, key, useSSL, trustManagers, 60 * 1000);
        LOG.debug("Remote connection info: {}", info);

        // 2. Get connector info manager
        ConnectorInfoManager manager = ConnectorInfoManagerFactory.getInstance().getRemoteManager(info);
        if (manager == null) {
            throw new NotFoundException("Remote ConnectorInfoManager");
        }

        CONN_MANAGERS.put(location, manager);
    }

    public static void resetConnManagers() {
        CONN_MANAGERS.clear();
    }

    public static Map<URI, ConnectorInfoManager> getConnManagers() {
        if (CONN_MANAGERS.isEmpty()) {
            for (URI location : LOCATIONS) {
                try {
                    if ("file".equals(location.getScheme())) {
                        LOG.debug("Local initialization: {}", location);
                        initLocal(location);
                    } else if (location.getScheme().startsWith("connid")) {
                        LOG.debug("Remote initialization: {}", location);
                        initRemote(location);
                    } else {
                        LOG.warn("Unsupported scheme: {}", location);
                    }
                } catch (Exception e) {
                    LOG.error("Could not process {}", location, e);
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            for (Map.Entry<URI, ConnectorInfoManager> entry : CONN_MANAGERS.entrySet()) {
                LOG.debug("Connector bundles found at {}", entry.getKey());
                for (ConnectorInfo connInfo : entry.getValue().getConnectorInfos()) {
                    LOG.debug("\t{}", connInfo.getConnectorDisplayName());
                }
            }
        }

        return CONN_MANAGERS;
    }

    public static ConnectorInfo getConnectorInfo(
            final String location, final String bundleName, final String bundleVersion, final String connectorName) {

        // check ConnIdLocation
        URI uriLocation = null;
        try {
            uriLocation = new URI(location);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid ConnId location " + location, e);
        }

        // create key for search all properties
        final ConnectorKey key = new ConnectorKey(bundleName, bundleVersion, connectorName);

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: " + key.getBundleName()
                    + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        // get the specified connector
        ConnectorInfo info = null;
        if (getConnManagers().containsKey(uriLocation)) {
            info = getConnManagers().get(uriLocation).findConnectorInfo(key);
        }
        if (info == null) {
            throw new NotFoundException("Connector Info for location " + location + " and key " + key);
        }

        return info;
    }

    public static Map<String, List<ConnectorInfo>> getConnectorInfos() {
        final Map<String, List<ConnectorInfo>> infos = new LinkedHashMap<String, List<ConnectorInfo>>();
        for (Map.Entry<URI, ConnectorInfoManager> entry : CONN_MANAGERS.entrySet()) {
            infos.put(entry.getKey().toString(), entry.getValue().getConnectorInfos());
        }
        return infos;
    }

    public static ConfigurationProperties getConfigurationProperties(final ConnectorInfo info) {
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
                LOG.debug("Property Name: {}"
                        + "\nProperty Type: {}",
                        properties.getProperty(propName).getName(),
                        properties.getProperty(propName).getType());
            }
        }

        return properties;
    }

    private ConnIdBundleManager() {
        // Empty constructor for static utility class.
    }
}
