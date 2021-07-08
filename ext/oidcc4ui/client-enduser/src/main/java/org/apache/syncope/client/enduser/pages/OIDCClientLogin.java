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
package org.apache.syncope.client.enduser.pages;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.OIDCC4UIConstants;
import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OIDCClientLogin extends WebPage {

    private static final long serialVersionUID = 8581614051773949262L;

    private static final Logger LOG = LoggerFactory.getLogger(OIDCClientLogin.class);

    private static final String OIDC_ACCESS_ERROR = "OIDC access error";

    public OIDCClientLogin(final PageParameters parameters) {
        super(parameters);

        String token = parameters.get(OIDCC4UIConstants.OIDCC4UI_JWT).toOptionalString();
        if (StringUtils.isBlank(token)) {
            LOG.error("No JWT found, redirecting to default greeter");

            PageParameters params = new PageParameters();
            params.add("errorMessage", OIDC_ACCESS_ERROR);
            setResponsePage(Login.class, params);
        }

        IAuthenticationStrategy strategy = getApplication().getSecuritySettings().getAuthenticationStrategy();

        if (SyncopeEnduserSession.get().authenticate(token)) {
            if (parameters.get(OIDCC4UIConstants.OIDCC4UI_SLO_SUPPORTED).toBoolean(false)) {
                SyncopeEnduserSession.get().setAttribute(Constants.BEFORE_LOGOUT_PAGE, OIDCClientBeforeLogout.class);
            }

            // If login has been called because the user was not yet logged in, than continue to the
            // original destination, otherwise to the Home page
            continueToOriginalDestination();
            setResponsePage(getApplication().getHomePage());
        } else {
            PageParameters params = new PageParameters();
            params.add("errorMessage", OIDC_ACCESS_ERROR);
            setResponsePage(Login.class, params);
        }
        strategy.remove();
    }
}
