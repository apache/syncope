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
package org.apache.syncope.core.provisioning.java;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.utils.URIUtils;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
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

public class DefaultConnIdBundleManager implements ConnIdBundleManager {

    protected static final Logger LOG = LoggerFactory.getLogger(ConnIdBundleManager.class);

    /**
     * ConnId Locations.
     */
    protected final List<URI> locations;

    /**
     * ConnectorInfoManager instances.
     */
    protected final Map<URI, ConnectorInfoManager> connInfoManagers =
            Collections.synchronizedMap(new LinkedHashMap<>());

    public DefaultConnIdBundleManager(final List<String> stringLocations) {
        locations = new ArrayList<>();
        stringLocations.forEach(location -> {
            try {
                locations.add(URIUtils.buildForConnId(location));
                LOG.info("Valid ConnId location: {}", location.trim());
            } catch (Exception e) {
                LOG.error("Invalid ConnId location: {}", location.trim(), e);
            }
        });
    }

    @Override
    public List<URI> getLocations() {
        return locations;
    }

    protected void initLocal(final URI location) {
        // 1. Find bundles inside local directory
        File bundleDirectory = Path.of(location).toFile();
        String[] bundleFiles = bundleDirectory.list();
        if (bundleFiles == null) {
            throw new NotFoundException("Local bundles directory " + location);
        }

        List<URL> bundleFileURLs = new ArrayList<>();
        for (String file : bundleFiles) {
            try {
                bundleFileURLs.add(IOUtil.makeURL(bundleDirectory.toPath(), file));
            } catch (IOException ignore) {
                // ignore exception and don't add bundle
                LOG.debug("{}/{} is not a valid connector bundle", bundleDirectory, file, ignore);
            }
        }

        if (bundleFileURLs.isEmpty()) {
            LOG.warn("No connector bundles found in {}", location);
        }
        LOG.debug("Configuring local connector server:\n\tFiles: {}", bundleFileURLs);

        // 2. Get connector info manager
        ConnectorInfoManager manager =
                ConnectorInfoManagerFactory.getInstance().getLocalManager(bundleFileURLs.toArray(URL[]::new));
        if (manager == null) {
            throw new NotFoundException("Local ConnectorInfoManager");
        }

        connInfoManagers.put(location, manager);
    }

    protected void initRemote(final URI location) {
        // 1. Extract conf params for remote connection from given URI
        String host = location.getHost();
        int port = location.getPort();
        GuardedString key = new GuardedString(location.getUserInfo().toCharArray());
        boolean useSSL = location.getScheme().equals("connids");

        List<TrustManager> trustManagers = new ArrayList<>();
        String[] params = StringUtils.isBlank(location.getQuery()) ? null : location.getQuery().split("&");
        if (params != null && params.length > 0) {
            final String[] trustAllCerts = params[0].split("=");
            if (trustAllCerts.length > 1
                    && "trustAllCerts".equalsIgnoreCase(trustAllCerts[0])
                    && "true".equalsIgnoreCase(trustAllCerts[1])) {

                trustManagers.add(new X509TrustManager() {

                    @Override
                    public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                        // no checks, trust all
                    }

                    @Override
                    public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
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

        connInfoManagers.put(location, manager);
    }

    @Override
    public void resetConnManagers() {
        connInfoManagers.clear();
    }

    @Override
    public Map<URI, ConnectorInfoManager> getConnManagers() {
        if (connInfoManagers.isEmpty()) {
            locations.forEach(location -> {
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
            });
        }

        if (LOG.isDebugEnabled()) {
            connInfoManagers.entrySet().stream().peek(
                entry -> LOG.debug("Connector bundles found at {}", entry.getKey())).
                        forEach(entry -> entry.getValue().getConnectorInfos().forEach(
                        connInfo -> LOG.debug("\t{}", connInfo.getConnectorDisplayName())));
        }

        return connInfoManagers;
    }

    @Override
    public Pair<URI, ConnectorInfo> getConnectorInfo(final ConnInstance connInstance) {
        // check ConnIdLocation
        URI uriLocation = null;
        try {
            uriLocation = URIUtils.buildForConnId(connInstance.getLocation());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ConnId location " + connInstance.getLocation(), e);
        }

        // create key for search all properties
        ConnectorKey key = new ConnectorKey(
                connInstance.getBundleName(), connInstance.getVersion(), connInstance.getConnectorName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: {}\nBundle version: {}\nBundle class: {}",
                    key.getBundleName(), key.getBundleVersion(), key.getConnectorName());
        }

        // get the specified connector
        ConnectorInfo info = null;
        if (getConnManagers().containsKey(uriLocation)) {
            info = getConnManagers().get(uriLocation).findConnectorInfo(key);
        }
        if (info == null) {
            throw new NotFoundException("ConnectorInfo for location " + connInstance.getLocation() + " and key " + key);
        }

        return Pair.of(uriLocation, info);
    }

    @Override
    public Map<URI, ConnectorInfoManager> getConnInfoManagers() {
        return connInfoManagers;
    }

    @Override
    public ConfigurationProperties getConfigurationProperties(final ConnectorInfo info) {
        if (info == null) {
            throw new NotFoundException("Invalid: connector info is null");
        }

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties = apiConfig.getConfigurationProperties();
        if (properties == null) {
            throw new NotFoundException("Configuration properties");
        }

        if (LOG.isDebugEnabled()) {
            properties.getPropertyNames().forEach(propName -> LOG.debug("Property Name: {}"
                    + "\nProperty Type: {}",
                    properties.getProperty(propName).getName(),
                    properties.getProperty(propName).getType()));
        }

        return properties;
    }
}
