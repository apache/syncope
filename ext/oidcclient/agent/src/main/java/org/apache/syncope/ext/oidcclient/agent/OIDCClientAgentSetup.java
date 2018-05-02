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
package org.apache.syncope.ext.oidcclient.agent;

import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.PropertyUtils;

@WebListener
public class OIDCClientAgentSetup implements ServletContextListener {

    private static final String OIDCCLIENT_AGENT_PROPERTIES = "oidcclient-agent.properties";

    private static <T> T assertNotNull(final T argument, final String name) {
        if (argument == null) {
            throw new IllegalArgumentException("Argument '" + name + "' may not be null.");
        }
        return argument;
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        // read oidcclientagent.properties
        Properties props = PropertyUtils.read(getClass(), OIDCCLIENT_AGENT_PROPERTIES, "conf.directory").getLeft();

        String anonymousUser = props.getProperty("anonymousUser");
        assertNotNull(anonymousUser, "<anonymousUser>");
        String anonymousKey = props.getProperty("anonymousKey");
        assertNotNull(anonymousKey, "<anonymousKey>");

        String scheme = props.getProperty("scheme");
        assertNotNull(scheme, "<scheme>");
        String host = props.getProperty("host");
        assertNotNull(host, "<host>");
        String port = props.getProperty("port");
        assertNotNull(port, "<port>");
        String rootPath = props.getProperty("rootPath");
        assertNotNull(rootPath, "<rootPath>");
        String useGZIPCompression = props.getProperty("useGZIPCompression");
        assertNotNull(useGZIPCompression, "<useGZIPCompression>");

        SyncopeClientFactoryBean clientFactory = new SyncopeClientFactoryBean().
                setAddress(scheme + "://" + host + ":" + port + "/" + rootPath).
                setUseCompression(BooleanUtils.toBoolean(useGZIPCompression));

        sce.getServletContext().setAttribute(Constants.SYNCOPE_CLIENT_FACTORY, clientFactory);
        sce.getServletContext().setAttribute(
                Constants.SYNCOPE_ANONYMOUS_CLIENT,
                clientFactory.create(new AnonymousAuthenticationHandler(anonymousUser, anonymousKey)));
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
    }

}
