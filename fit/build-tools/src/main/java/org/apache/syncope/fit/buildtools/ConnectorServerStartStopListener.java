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

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.impl.api.local.ThreadClassLoaderManager;
import org.identityconnectors.framework.server.ConnectorServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@WebListener
public class ConnectorServerStartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorServerStartStopListener.class);

    private static final String SERVER = "ConnIdConnectorServer";

    /**
     * Build list of URLs from bundles available in the classpath.
     *
     * @param ctx ApplicationContext needed for getting ConnId jar bundles URLs
     */
    private static List<URL> getBundleURLs(final ApplicationContext ctx) {
        final List<URL> bundleURLs = new ArrayList<>();

        try {
            for (Resource bundle : ctx.getResources("classpath*:/bundles/*.jar")) {
                bundleURLs.add(bundle.getURL());
            }
        } catch (IOException e) {
            LOG.error("While getting bundled ConnId bundles", e);
        }

        LOG.info("ConnId bundles loaded: {}", bundleURLs);

        return bundleURLs;
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ConnectorServer server = ConnectorServer.newInstance();
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
        try {
            server.setPort(Objects.requireNonNull(ctx).getEnvironment()
                .getProperty("testconnectorserver.port", Integer.class));

            server.setBundleURLs(getBundleURLs(ctx));

            server.setKeyHash(SecurityUtil.computeBase64SHA1Hash(
                    ctx.getEnvironment().getProperty("testconnectorserver.key", String.class).toCharArray()));

            server.start();
            LOG.info("ConnId connector server listening on port {}", server.getPort());
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
