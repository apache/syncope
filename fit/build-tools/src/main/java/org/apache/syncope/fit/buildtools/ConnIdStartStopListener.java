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
package org.apache.syncope.fit.buildtools;

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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ConnIdStartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ConnIdStartStopListener.class);

    private static final String SERVER = "ConnIdConnectorServer";

    /**
     * Build list of URLs from bundles available under /WEB-INF/lib
     *
     * @param ctx ServletContext needed for getting ConnId jar bundles URLs
     */
    private List<URL> getBundleURLs(final ServletContext ctx) {
        final List<URL> bundleURLs = new ArrayList<>();

        for (String bundleFile : new String[] {
            "testconnectorserver.soap.bundle",
            "testconnectorserver.rest.bundle",
            "testconnectorserver.dbtable.bundle",
            "testconnectorserver.scriptedsql.bundle",
            "testconnectorserver.csvdir.bundle",
            "testconnectorserver.ldap.bundle" }) {

            URL url = null;
            try {
                url = ctx.getResource("/WEB-INF/lib/"
                        + WebApplicationContextUtils.getWebApplicationContext(ctx).getBean(bundleFile, String.class));
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
        ConnectorServer server = ConnectorServer.newInstance();
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
        try {
            server.setPort(Integer.parseInt(ctx.getBean("testconnectorserver.port", String.class)));

            server.setBundleURLs(getBundleURLs(sce.getServletContext()));

            server.setKeyHash(SecurityUtil.computeBase64SHA1Hash(
                    ctx.getBean("testconnectorserver.key", String.class).toCharArray()));

            server.start();
            LOG.info("ConnId connector server listening on port " + server.getPort());
        } catch (Exception e) {
            LOG.error("Could not start ConnId connector server", e);
        }

        sce.getServletContext().setAttribute(SERVER, server);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        final ConnectorServer server = (ConnectorServer) sce.getServletContext().getAttribute(SERVER);
        if (server != null && server.isStarted()) {
            server.stop();
        }
        ThreadClassLoaderManager.clearInstance();
    }
}
