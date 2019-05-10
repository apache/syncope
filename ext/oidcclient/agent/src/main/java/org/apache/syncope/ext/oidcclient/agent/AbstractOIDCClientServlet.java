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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

class AbstractOIDCClientServlet extends HttpServlet {

    private static final long serialVersionUID = 4738590657326972169L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractOIDCClientServlet.class);

    private static final String SYNCOPE_CLIENT_FACTORY = "SyncopeClientFactory";

    private static final String SYNCOPE_ANONYMOUS_CLIENT = "SyncopeAnonymousClient";

    private final ApplicationContext ctx;

    protected AbstractOIDCClientServlet(final ApplicationContext ctx) {
        super();
        this.ctx = ctx;
    }

    protected SyncopeClientFactoryBean getClientFactory(
            final ServletContext servletContext,
            final boolean useGZIPCompression) {

        SyncopeClientFactoryBean clientFactory =
                (SyncopeClientFactoryBean) servletContext.getAttribute(SYNCOPE_CLIENT_FACTORY);
        if (clientFactory == null) {
            ServiceOps serviceOps = ctx.getBean(ServiceOps.class);
            clientFactory = new SyncopeClientFactoryBean().
                    setAddress(serviceOps.get(NetworkService.Type.CORE).getAddress()).
                    setUseCompression(useGZIPCompression);

            servletContext.setAttribute(SYNCOPE_CLIENT_FACTORY, clientFactory);
        }

        return clientFactory;
    }

    protected SyncopeClient getAnonymousClient(
            final ServletContext servletContext,
            final String anonymousUser,
            final String anonymousKey,
            final boolean useGZIPCompression) {

        SyncopeClient anonymousClient = (SyncopeClient) servletContext.getAttribute(SYNCOPE_ANONYMOUS_CLIENT);
        if (anonymousClient == null) {
            SyncopeClientFactoryBean clientFactory = getClientFactory(servletContext, useGZIPCompression);
            anonymousClient = clientFactory.create(new AnonymousAuthenticationHandler(anonymousUser, anonymousKey));

            servletContext.setAttribute(SYNCOPE_ANONYMOUS_CLIENT, anonymousClient);
        }

        return anonymousClient;
    }
}
