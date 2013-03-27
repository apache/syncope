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
package org.apache.syncope.buildtools;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.impl.api.local.ThreadClassLoaderManager;
import org.identityconnectors.framework.server.ConnectorServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnIdStartStopListener implements ServletContextListener {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConnIdStartStopListener.class);

    private static final String SERVER = "ConnIdConnectorServer";

    /**
     * Build list of URLs from bundles available under /WEB-INF/lib
     *
     * @param ctx ServletContext needed for getting ConnId jar bundles URLs
     */
    private List<URL> getBundleURLs(final ServletContext ctx) {
        final List<URL> bundleURLs = new ArrayList<URL>();

        for (String bundleFile : new String[] {
            "testconnectorserver.soap.bundle",
            "testconnectorserver.db.bundle",
            "testconnectorserver.csvdir.bundle",
            "testconnectorserver.ldap.bundle"}) {

            URL url = null;
            try {
                url = ctx.getResource("/WEB-INF/lib/" + ctx.getInitParameter(bundleFile));
            } catch (MalformedURLException e) {
                // ignore
            }
            if (url != null) {
                bundleURLs.add(url);
            }
        }

        LOG.info("ConnId bundles loaded: " + bundleURLs);

        return bundleURLs;
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        final ConnectorServer _server = ConnectorServer.newInstance();

        try {
            _server.setPort(Integer.parseInt(
                    sce.getServletContext().getInitParameter("testconnectorserver.port")));

            _server.setBundleURLs(getBundleURLs(sce.getServletContext()));

            _server.setKeyHash(SecurityUtil.computeBase64SHA1Hash(
                    sce.getServletContext().getInitParameter("testconnectorserver.key").toCharArray()));

            _server.start();
            LOG.info("ConnId connector server listening on port " + _server.getPort());
        } catch (Exception e) {
            LOG.error("Could not start ConnId connector server", e);
        }

        sce.getServletContext().setAttribute(SERVER, _server);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        final ConnectorServer _server = (ConnectorServer) sce.getServletContext().getAttribute(SERVER);
        if (_server != null && _server.isStarted()) {
            _server.stop();
        }
        ThreadClassLoaderManager.clearInstance();
    }
}
