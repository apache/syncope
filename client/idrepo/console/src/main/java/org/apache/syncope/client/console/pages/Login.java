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
package org.apache.syncope.client.console.pages;

import jakarta.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.BaseLogin;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.panels.BaseSSOLoginFormPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Login extends BaseLogin {

    private static final long serialVersionUID = 5889157642852559004L;

    public Login(final PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected BaseSession getBaseSession() {
        return SyncopeConsoleSession.get();
    }

    @Override
    protected List<Panel> getSSOLoginFormPanels() {
        List<Panel> ssoLoginFormPanels = new ArrayList<>();
        SyncopeWebApplication.get().getLookup().getClasses(BaseSSOLoginFormPanel.class).forEach(ssoLoginFormPanel -> {
            try {
                ssoLoginFormPanels.add(ssoLoginFormPanel.getConstructor(String.class, BaseSession.class).newInstance(
                        "ssoLogin", SyncopeConsoleSession.get()));
            } catch (Exception e) {
                LOG.error("Could not initialize the provided SSO login form panel", e);
            }
        });
        return ssoLoginFormPanels;
    }

    @Override
    protected void sendError(final String error) {
        SyncopeConsoleSession.get().error(error);
    }

    @Override
    protected void authenticate(final String username, final String password, final AjaxRequestTarget target)
            throws NotAuthorizedException {

        if (SyncopeWebApplication.get().getAnonymousUser().equals(username)) {
            throw new NotAuthorizedException("Illegal username");
        }

        IAuthenticationStrategy strategy = getApplication().getSecuritySettings().getAuthenticationStrategy();

        if (AuthenticatedWebSession.get().signIn(username, password)) {
            // If login has been called because the user was not yet logged in, than continue to the
            // original destination, otherwise to the Home page
            continueToOriginalDestination();
            setResponsePage(getApplication().getHomePage());
        } else {
            SyncopeConsoleSession.get().error(getString("login-error"));
            notificationPanel.refresh(target);
        }
        strategy.remove();
    }
}
