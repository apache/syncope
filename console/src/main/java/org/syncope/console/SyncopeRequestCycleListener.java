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
import org.apache.wicket.authorization.UnauthorizedInstantiationException;
import org.apache.wicket.markup.html.pages.ExceptionErrorPage;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.ComponentRenderingRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.syncope.console.pages.ErrorPage;

public class SyncopeRequestCycleListener extends AbstractRequestCycleListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public IRequestHandler onException(final RequestCycle cycle,
            final Exception e) {

        final Page errorPage;
        PageParameters errorParameters = new PageParameters();
        errorParameters.add("errorTitle",
                new StringResourceModel("alert", null).getString());

        if (e instanceof UnauthorizedInstantiationException) {
            errorParameters.add("errorMessage", new StringResourceModel(
                    "unauthorizedInstantiationException", null).getString());

            errorPage = new ErrorPage(errorParameters);
        } else if (e instanceof HttpClientErrorException) {
            errorParameters.add("errorMessage",
                    new StringResourceModel("httpClientException", null).
                    getString());

            errorPage = new ErrorPage(errorParameters);
        } else if (e instanceof PageExpiredException
                || !(SyncopeSession.get()).isAuthenticated()) {

            errorParameters.add("errorMessage",
                    new StringResourceModel("pageExpiredException", null).
                    getString());

            errorPage = new ErrorPage(errorParameters);
        } else if (e.getCause() != null && e.getCause().getCause() != null
                && e.getCause().getCause() instanceof RestClientException) {

            errorParameters.add("errorMessage",
                    new StringResourceModel("restClientException", null).
                    getString());

            errorPage = new ErrorPage(errorParameters);
        } else {
            // redirect to default Wicket error page
            errorPage = new ExceptionErrorPage(e, null);
        }

        return new ComponentRenderingRequestHandler(errorPage);
    }
}
