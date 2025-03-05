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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.xml.ws.WebServiceException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
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

    @SuppressWarnings("unchecked")
    private static <T extends Exception> Optional<T> instanceOf(final Exception e, final Class<T> clazz) {
        if (clazz.isAssignableFrom(e.getClass())) {
            return Optional.of((T) e);
        }

        if (e.getCause() != null && clazz.isAssignableFrom(e.getCause().getClass())) {
            return Optional.of((T) e.getCause());
        }

        if (e.getCause() != null && e.getCause().getCause() != null
                && clazz.isAssignableFrom(e.getCause().getCause().getClass())) {

            return Optional.of((T) e.getCause().getCause());
        }

        return Optional.empty();
    }

    @Override
    public IRequestHandler onException(final RequestCycle cycle, final Exception e) {
        LOG.error("Exception found", e);

        PageParameters errorParameters = new PageParameters();

        IRequestablePage errorPage;
        if (instanceOf(e, UnauthorizedInstantiationException.class).isPresent()) {
            errorParameters.add("errorMessage", BaseSession.Error.AUTHORIZATION.message());
            errorPage = getErrorPage(errorParameters);
        } else if (instanceOf(e, NotAuthorizedException.class).isPresent()) {
            NotAuthorizedException nae = instanceOf(e, NotAuthorizedException.class).get();
            if (StringUtils.containsIgnoreCase(nae.getMessage(), "expired")) {
                errorParameters.add("errorMessage", BaseSession.Error.SESSION_EXPIRED.message());
            } else {
                errorParameters.add("errorMessage", BaseSession.Error.AUTHORIZATION.message());
            }
            errorPage = getErrorPage(errorParameters);
        } else if (instanceOf(e, SyncopeClientException.class).isPresent()) {
            SyncopeClientException sce = instanceOf(e, SyncopeClientException.class).get();
            String errorMessage = sce.getType() == ClientExceptionType.Unknown
                    ? String.join("", sce.getElements())
                    : sce.getMessage();
            errorParameters.add("errorMessage", errorMessage);
            errorPage = getErrorPage(errorParameters);
        } else if (instanceOf(e, BadRequestException.class).isPresent()
                || instanceOf(e, WebServiceException.class).isPresent()) {

            errorParameters.add("errorMessage", BaseSession.Error.REST.message());
            errorPage = getErrorPage(errorParameters);
        } else if (instanceOf(e, PageExpiredException.class).isPresent() || !isSignedIn()) {
            errorParameters.add("errorMessage", BaseSession.Error.SESSION_EXPIRED.message());
            errorPage = getErrorPage(errorParameters);
        } else {
            Optional<ForbiddenException> cause = instanceOf(e, ForbiddenException.class);
            if (cause.isPresent()) {
                errorParameters.add("errorMessage", cause.get().getMessage());
                errorPage = getErrorPage(errorParameters);
            } else {
                // redirect to default Wicket error page
                errorPage = new ExceptionErrorPage(e, null);
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
