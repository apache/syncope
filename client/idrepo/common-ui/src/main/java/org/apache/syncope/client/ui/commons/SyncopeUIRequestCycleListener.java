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
package org.apache.syncope.client.ui.commons;

import java.security.AccessControlException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.pages.ExceptionErrorPage;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SyncopeUIRequestCycleListener implements IRequestCycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeUIRequestCycleListener.class);

    private static Throwable instanceOf(final Exception e, final Class<? extends Exception> clazz) {
        return clazz.isAssignableFrom(e.getClass())
                ? e
                : e.getCause() != null && clazz.isAssignableFrom(e.getCause().getClass())
                ? e.getCause()
                : e.getCause() != null && e.getCause().getCause() != null
                && clazz.isAssignableFrom(e.getCause().getCause().getClass())
                ? e.getCause().getCause()
                : null;
    }

    @Override
    public IRequestHandler onException(final RequestCycle cycle, final Exception e) {
        LOG.error("Exception found", e);

        PageParameters errorParameters = new PageParameters();

        IRequestablePage errorPage;
        if (instanceOf(e, UnauthorizedInstantiationException.class) != null) {
            errorParameters.add("errorMessage", BaseSession.Error.AUTHORIZATION.fallback());
            errorPage = getErrorPage(errorParameters);
        } else if (instanceOf(e, AccessControlException.class) != null) {
            if (StringUtils.containsIgnoreCase(instanceOf(e, AccessControlException.class).getMessage(), "expired")) {
                errorParameters.add("errorMessage", BaseSession.Error.SESSION_EXPIRED.fallback());
            } else {
                errorParameters.add("errorMessage", BaseSession.Error.AUTHORIZATION.fallback());
            }
            errorPage = getErrorPage(errorParameters);
        } else if (instanceOf(e, PageExpiredException.class) != null || !isSignedIn()) {
            errorParameters.add("errorMessage", BaseSession.Error.SESSION_EXPIRED.fallback());
            errorPage = getErrorPage(errorParameters);
        } else if (instanceOf(e, BadRequestException.class) != null
                || instanceOf(e, WebServiceException.class) != null
                || instanceOf(e, SyncopeClientException.class) != null) {

            errorParameters.add("errorMessage", BaseSession.Error.REST.fallback());
            errorPage = getErrorPage(errorParameters);
        } else {
            Throwable cause = instanceOf(e, ForbiddenException.class);
            if (cause == null) {
                // redirect to default Wicket error page
                errorPage = new ExceptionErrorPage(e, null);
            } else {
                errorParameters.add("errorMessage", cause.getMessage());
                errorPage = getErrorPage(errorParameters);
            }
        }

        if (errorPage instanceof BaseLogin) {
            try {
                invalidateSession();
            } catch (Throwable t) {
                // ignore
                LOG.debug("Unexpected error while forcing logout after error", t);
            }
        }

        return new RenderPageRequestHandler(new PageProvider(errorPage));
    }

    protected abstract boolean isSignedIn();

    protected abstract void invalidateSession();

    protected abstract IRequestablePage getErrorPage(PageParameters errorParameters);
}
