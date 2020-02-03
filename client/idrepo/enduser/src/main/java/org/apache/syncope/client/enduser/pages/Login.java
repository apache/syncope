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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.ui.commons.BaseLogin;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Login extends BaseLogin {

    private static final long serialVersionUID = -3422492668689122688L;

    @SpringBean
    private ClassPathScanImplementationLookup lookup;

    private final BookmarkablePageLink<Void> selfRegistration;

    private final BookmarkablePageLink<Void> selfPwdReset;

    public Login(final PageParameters parameters) {
        super(parameters);

        selfRegistration = new BookmarkablePageLink<>("self-registration", Self.class);
        add(selfRegistration.setOutputMarkupId(true));

        selfPwdReset = new BookmarkablePageLink<>("self-pwd-reset", SelfPasswordReset.class);
        add(selfPwdReset.setOutputMarkupId(true));
    }

    @Override
    protected Collection<Component> getLanguageOnChangeComponents() {
        return Stream.concat(
                super.getLanguageOnChangeComponents().stream(),
                List.of(selfRegistration, selfPwdReset).stream()).
                collect(Collectors.toList());
    }

    @Override
    protected BaseSession getBaseSession() {
        return SyncopeEnduserSession.get();
    }

    @Override
    protected List<Panel> getSSOLoginFormPanels() {
        List<Panel> ssoLoginFormPanels = new ArrayList<>();
        lookup.getSSOLoginFormPanels().forEach(ssoLoginFormPanel -> {
            try {
                ssoLoginFormPanels.add(ssoLoginFormPanel.getConstructor(String.class, BaseSession.class).
                        newInstance("ssoLogin", SyncopeEnduserSession.get()));
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
    protected String getAnonymousUser() {
        return SyncopeWebApplication.get().getAnonymousUser();
    }

    @Override
    protected List<Locale> getSupportedLocales() {
        return SyncopeWebApplication.SUPPORTED_LOCALES;
    }

    @Override
    protected void authenticate(
            final String username,
            final String password,
            final AjaxRequestTarget target) throws AccessControlException {

        if (!SyncopeWebApplication.get().getAdminUser().equalsIgnoreCase(username)
                && !SyncopeWebApplication.get().getAnonymousUser().equalsIgnoreCase(username)
                && SyncopeEnduserSession.get().authenticate(username, password)) {

            // user has been authenticated successfully
            continueToOriginalDestination();
            setResponsePage(getApplication().getHomePage());
        } else {
            // not authenticated
            sendError(getString("login-error"));
            notificationPanel.refresh(target);
        }
    }
}
