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

import com.unboundid.ldap.listener.Base64PasswordEncoderOutputFormatter;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.UnsaltedMessageDigestInMemoryPasswordEncoder;
import com.unboundid.ldap.sdk.schema.Schema;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.net.InetAddress;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Start and stop an in-memory LDAP server instance alongside with Servlet Context.
 */
@WebListener
public class LDAPStartStopListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(LDAPStartStopListener.class);

    private InMemoryDirectoryServer ldapServer;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        try {
            ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());

            InMemoryDirectoryServerConfig config =
                    new InMemoryDirectoryServerConfig(ctx.getEnvironment().getProperty("testds.rootDn"));

            config.addAdditionalBindCredentials(
                    ctx.getEnvironment().getProperty("testds.bindDn"),
                    ctx.getEnvironment().getProperty("testds.password"));

            InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPConfig(
                    "test-listener",
                    InetAddress.getLoopbackAddress(),
                    Integer.parseInt(ctx.getEnvironment().getProperty("testds.port")),
                    null);
            config.setListenerConfigs(listenerConfig);

            config.setSchema(Schema.getDefaultStandardSchema());

            config.setPasswordEncoders(
                    new UnsaltedMessageDigestInMemoryPasswordEncoder(
                            "{SHA}",
                            Base64PasswordEncoderOutputFormatter.getInstance(),
                            MessageDigest.getInstance("SHA-1")));

            ldapServer = new InMemoryDirectoryServer(config);
            ldapServer.importFromLDIF(false, ctx.getResource("classpath:/content.ldif").getFile());
            ldapServer.startListening();
        } catch (Exception e) {
            LOG.error("Fatal error in context init", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        if (ldapServer != null) {
            ldapServer.shutDown(true);
        }
    }
}
