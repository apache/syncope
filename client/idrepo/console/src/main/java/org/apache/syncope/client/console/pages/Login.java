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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.ui.commons.BaseLogin;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authentication.IAuthenticationStrategy;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Login extends BaseLogin {

    private static final Logger LOG = LoggerFactory.getLogger(Login.class);

    private static final long serialVersionUID = 5889157642852559004L;

    @SpringBean
    private ClassPathScanImplementationLookup lookup;

    public Login(final PageParameters parameters) {
        super(parameters);

        DomainDropDown domainSelect = new DomainDropDown("domain");
        if (SyncopeWebApplication.get().getDomains().size() == 1) {
            domainSelect.setOutputMarkupPlaceholderTag(true);
        }
        domainSelect.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // nothing to do
            }
        }).add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // nothing to do
            }
        });
        form.add(domainSelect);
    }

    @Override
    protected List<Panel> getSSOLoginFormPanels() {
        List<Panel> ssoLoginFormPanels = new ArrayList<>();
        lookup.getSSOLoginFormPanels().forEach(ssoLoginFormPanel -> {
            try {
                ssoLoginFormPanels.add(ssoLoginFormPanel.getConstructor(String.class).newInstance("ssoLogin"));
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
    protected String getAnonymousUser() {
        return SyncopeWebApplication.get().getAnonymousUser();
    }

    @Override
    protected List<Locale> getSupportedLocales() {
        return SyncopeWebApplication.SUPPORTED_LOCALES;
    }

    @Override
    protected void authenticate(final String username, final String password, final AjaxRequestTarget target) throws
            AccessControlException {
        if (SyncopeWebApplication.get().getAnonymousUser().equals(username)) {
            throw new AccessControlException("Illegal username");
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

    /**
     * Inner class which implements (custom) Domain DropDownChoice component.
     */
    private class DomainDropDown extends BootstrapSelect<String> {

        private static final long serialVersionUID = -7401167913360133325L;

        DomainDropDown(final String id) {
            super(id, SyncopeWebApplication.get().getDomains());

            setModel(new IModel<String>() {

                private static final long serialVersionUID = -1124206668056084806L;

                @Override
                public String getObject() {
                    return SyncopeConsoleSession.get().getDomain();
                }

                @Override
                public void setObject(final String object) {
                    SyncopeConsoleSession.get().setDomain(object);
                }

                @Override
                public void detach() {
                    // Empty.
                }
            });

            // set default value to Master Domain
            getModel().setObject(SyncopeConstants.MASTER_DOMAIN);
        }
    }
}
