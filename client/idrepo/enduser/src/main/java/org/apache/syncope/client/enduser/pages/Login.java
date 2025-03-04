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

import jakarta.ws.rs.NotAuthorizedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.BaseLogin;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Login extends BaseLogin {

    private static final long serialVersionUID = 5889157642852559004L;

    private final BookmarkablePageLink<Void> selfPwdReset;

    private final BookmarkablePageLink<Void> selfRegistration;

    public Login(final PageParameters parameters) {
        super(parameters);

        selfPwdReset = new BookmarkablePageLink<>("self-pwd-reset", SelfPasswordReset.class);
        selfPwdReset.getPageParameters().add("domain", SyncopeEnduserSession.get().getDomain());
        selfPwdReset.setVisible(SyncopeEnduserSession.get().getPlatformInfo().isPwdResetAllowed());
        add(selfPwdReset.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));

        selfRegistration = new BookmarkablePageLink<>("self-registration", SelfRegistration.class);
        selfRegistration.getPageParameters().add("domain", SyncopeEnduserSession.get().getDomain());
        selfRegistration.setVisible(SyncopeEnduserSession.get().getPlatformInfo().isSelfRegAllowed());
        add(selfRegistration.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));
    }

    @Override
    protected Collection<Component> getLanguageOnChangeComponents() {
        return Stream.concat(
                super.getLanguageOnChangeComponents().stream(),
                Stream.of(selfRegistration, selfPwdReset)).
                collect(Collectors.toList());
    }

    @Override
    protected BaseSession getBaseSession() {
        return SyncopeEnduserSession.get();
    }

    @Override
    protected List<Panel> getSSOLoginFormPanels() {
        List<Panel> ssoLoginFormPanels = new ArrayList<>();
        SyncopeWebApplication.get().getLookup().getSSOLoginFormPanels().forEach(ssoLoginFormPanel -> {
            try {
                ssoLoginFormPanels.add(ssoLoginFormPanel.getConstructor(String.class, BaseSession.class).newInstance(
                        "ssoLogin", SyncopeEnduserSession.get()));
            } catch (Exception e) {
                LOG.error("Could not initialize the provided SSO login form panel", e);
            }
        });
        return ssoLoginFormPanels;
    }

    @Override
    protected void sendError(final String error) {
        SyncopeEnduserSession.get().error(error);
    }

    @Override
    protected void authenticate(final String username, final String password, final AjaxRequestTarget target)
            throws NotAuthorizedException {

        if (SyncopeWebApplication.get().getAnonymousUser().equals(username)
                || SyncopeWebApplication.get().getAdminUser().equals(username)) {

            throw new NotAuthorizedException("Illegal username");
        }

        if (SyncopeEnduserSession.get().authenticate(username, password)) {
            // If login has been called because the user was not yet logged in, than continue to the
            // original destination, otherwise to the Home page
            continueToOriginalDestination();
            setResponsePage(getApplication().getHomePage());
        } else {
            SyncopeEnduserSession.get().error(getString("login-error"));
            notificationPanel.refresh(target);
        }
    }
}
