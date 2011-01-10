/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console;

import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Response;
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.pages.AccessDeniedPage;
import org.apache.wicket.markup.html.pages.ExceptionErrorPage;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.syncope.console.pages.ErrorPage;

/**
 * SyncopeRequestCycle.
 */
public class SyncopeRequestCycle extends WebRequestCycle {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SyncopeRequestCycle.class);

    /**
     * SyncopeRequestCycle constructor.
     *
     * @param application the web application
     * @param request the web request
     * @param response the web response
     */
    public SyncopeRequestCycle(final WebApplication application,
            final WebRequest request, final Response response) {

        super(application, request, response);
    }

    @Override
    public final Page onRuntimeException(final Page cause,
            final RuntimeException e) {

        LOG.error("An exception was thrown", e);

        if (e instanceof UnauthorizedInstantiationException) {
            return new AccessDeniedPage();
        }

        if (e instanceof PageExpiredException
                || !((SyncopeSession) getSession()).isAuthenticated()) {

            PageParameters errorParameters = new PageParameters();

            errorParameters.add("errorTitle",
                    new StringResourceModel("alert", null).getString());

            errorParameters.add("errorMessage",
                    new StringResourceModel("pageExpiredException", null).
                    getString());

            return new ErrorPage(errorParameters);
        }

        if (e.getCause() != null && e.getCause().getCause() != null
                && e.getCause().getCause() instanceof RestClientException) {

            PageParameters errorParameters = new PageParameters();

            errorParameters.add("errorTitle",
                    new StringResourceModel("alert", null).getString());

            errorParameters.add("errorMessage",
                    new StringResourceModel("restClientException", null).
                    getString());

            return new ErrorPage(errorParameters);
        }

        //Redirect to default Wicket error page
        return new ExceptionErrorPage(e, cause);
    }
}
