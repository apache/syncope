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
package org.apache.syncope.client.console;

import java.security.AccessControlException;
import javax.ws.rs.BadRequestException;
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.Page;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.pages.ExceptionErrorPage;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeConsoleRequestCycleListener extends AbstractRequestCycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeConsoleRequestCycleListener.class);

    private boolean instanceOf(final Exception e, final Class<? extends Exception> clazz) {
        return clazz.isAssignableFrom(e.getClass())
                || (e.getCause() != null && clazz.isAssignableFrom(e.getCause().getClass()))
                || (e.getCause() != null && e.getCause().getCause() != null
                && clazz.isAssignableFrom(e.getCause().getCause().getClass()));
    }

    @Override
    public IRequestHandler onException(final RequestCycle cycle, final Exception e) {
        LOG.error("Exception found", e);

        PageParameters errorParameters = new PageParameters();

        Page errorPage;
        if (instanceOf(e, UnauthorizedInstantiationException.class)) {
            errorParameters.add("errorMessage", "unauthorizedInstantiationException");
            errorPage = new Login(errorParameters);
        } else if (instanceOf(e, AccessControlException.class)) {
            errorParameters.add("errorMessage", "accessControlException");
            errorPage = new Login(errorParameters);
        } else if (instanceOf(e, PageExpiredException.class) || !SyncopeConsoleSession.get().isSignedIn()) {
            errorParameters.add("errorMessage", "pageExpiredException");
            errorPage = new Login(errorParameters);
        } else if (instanceOf(e, BadRequestException.class)
                || instanceOf(e, WebServiceException.class)
                || instanceOf(e, SyncopeClientException.class)) {

            errorParameters.add("errorMessage", "restClientException");
            errorPage = new Login(errorParameters);
        } else {
            // redirect to default Wicket error page
            errorPage = new ExceptionErrorPage(e, null);
        }

        if (errorPage instanceof Login) {
            SyncopeConsoleSession.get().cleanup();
            SyncopeConsoleSession.get().invalidateNow();
        }

        return new RenderPageRequestHandler(new PageProvider(errorPage));
    }
}
