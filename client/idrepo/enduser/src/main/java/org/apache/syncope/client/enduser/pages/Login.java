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
import java.util.stream.Stream;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.pages.BaseLogin;
import org.apache.syncope.client.ui.commons.pages.BaseMfaEnroll;
import org.apache.syncope.client.ui.commons.panels.BaseSSOLoginFormPanel;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Login extends BaseLogin {

    private static final long serialVersionUID = 5889157642852559004L;

    protected final BookmarkablePageLink<Void> selfPwdReset;

    protected final BookmarkablePageLink<Void> selfRegistration;

    public Login(final PageParameters parameters) {
        super(parameters);

        selfPwdReset = new BookmarkablePageLink<>("self-pwd-reset", SelfPasswordReset.class);
        selfPwdReset.getPageParameters().add("domain", SyncopeEnduserSession.get().getDomain());
        selfPwdReset.setVisible(confParamOps.get(
                getBaseSession().getDomain(), StandardConfParams.PASSWORD_RESET_ALLOWED, false, boolean.class));
        add(selfPwdReset.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));

        selfRegistration = new BookmarkablePageLink<>("self-registration", SelfRegistration.class);
        selfRegistration.getPageParameters().add("domain", SyncopeEnduserSession.get().getDomain());
        selfRegistration.setVisible(confParamOps.get(
                getBaseSession().getDomain(), StandardConfParams.SELF_REGISTRATION_ALLOWED, false, boolean.class));
        add(selfRegistration.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));
    }

    @Override
    protected Collection<Component> getLanguageOnChangeComponents() {
        return Stream.concat(
                super.getLanguageOnChangeComponents().stream(),
                Stream.of(selfRegistration, selfPwdReset)).
                toList();
    }

    @Override
    protected BaseSession getBaseSession() {
        return SyncopeEnduserSession.get();
    }

    @Override
    protected Class<? extends BaseMfaEnroll> getMfaEnrollPage() {
        return MfaEnroll.class;
    }

    @Override
    protected List<BaseSSOLoginFormPanel> getSSOLoginFormPanels() {
        List<BaseSSOLoginFormPanel> ssoLoginFormPanels = new ArrayList<>();
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

    protected void onAuthenticateSuccess(final AjaxRequestTarget target) {
        // If login has been called because the user was not yet logged in, than continue to the
        // original destination, otherwise to the Home page
        continueToOriginalDestination();
        setResponsePage(getApplication().getHomePage());
    }

    protected void onAuthenticateFailure(final AjaxRequestTarget target) {
        SyncopeEnduserSession.get().error(getString("login-error"));
        notificationPanel.refresh(target);
    }

    @Override
    protected void authenticate(final String username, final String password, final AjaxRequestTarget target)
            throws NotAuthorizedException {

        if (SyncopeWebApplication.get().getAnonymousUser().equals(username)
                || SyncopeWebApplication.get().getAdminUser().equals(username)) {

            throw new NotAuthorizedException("Illegal username");
        }

        if (SyncopeEnduserSession.get().authenticate(username, password)) {
            onAuthenticateSuccess(target);
        } else {
            onAuthenticateFailure(target);
        }
    }
}
