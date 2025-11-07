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
import org.apache.commons.lang3.Strings;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.markup.html.pages.ExceptionErrorPage;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.widget.notification.Notification;

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

        PageParameters parameters = new PageParameters();
        parameters.add(Constants.NOTIFICATION_LEVEL_PARAM, Notification.ERROR);

        if (instanceOf(e, UnauthorizedInstantiationException.class).isPresent()) {
            parameters.add(Constants.NOTIFICATION_MSG_PARAM, BaseSession.Error.AUTHORIZATION.message());
        } else if (instanceOf(e, NotAuthorizedException.class).isPresent()) {
            NotAuthorizedException nae = instanceOf(e, NotAuthorizedException.class).get();
            if (Strings.CI.contains(nae.getMessage(), "expired")) {
                parameters.add(Constants.NOTIFICATION_MSG_PARAM, BaseSession.Error.SESSION_EXPIRED.message());
            } else {
                parameters.add(Constants.NOTIFICATION_MSG_PARAM, BaseSession.Error.AUTHORIZATION.message());
            }
        } else if (instanceOf(e, SyncopeClientException.class).isPresent()) {
            SyncopeClientException sce = instanceOf(e, SyncopeClientException.class).get();
            String errorMessage = sce.getType() == ClientExceptionType.Unknown
                    ? String.join("", sce.getElements())
                    : sce.getMessage();
            parameters.add(Constants.NOTIFICATION_MSG_PARAM, errorMessage);
        } else if (instanceOf(e, BadRequestException.class).isPresent()
                || instanceOf(e, WebServiceException.class).isPresent()) {

            parameters.add(Constants.NOTIFICATION_MSG_PARAM, BaseSession.Error.REST.message());
        } else if (instanceOf(e, PageExpiredException.class).isPresent() || !isSignedIn()) {
            parameters.add(Constants.NOTIFICATION_MSG_PARAM, BaseSession.Error.SESSION_EXPIRED.message());
        } else {
            Optional<ForbiddenException> cause = instanceOf(e, ForbiddenException.class);
            if (cause.isPresent()) {
                parameters.add(Constants.NOTIFICATION_MSG_PARAM, cause.get().getMessage());
            } else {
                // redirect to default Wicket error page
                return new RenderPageRequestHandler(new PageProvider(new ExceptionErrorPage(e, null)));
            }
        }

        try {
            invalidateSession();
        } catch (Throwable t) {
            // ignore
            LOG.debug("Unexpected error while forcing logout after error", t);
        }

        return new RenderPageRequestHandler(getErrorPageClass(), parameters);
    }

    protected abstract boolean isSignedIn();

    protected abstract void invalidateSession();

    protected abstract Class<? extends BaseLogin> getErrorPageClass();
}
