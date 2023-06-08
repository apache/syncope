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

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.pages.AbstractMustChangePassword;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class MustChangePassword extends AbstractMustChangePassword {

    private static final long serialVersionUID = 8581970794722709800L;

    @SpringBean
    protected UserSelfRestClient userSelfRestClient;

    public MustChangePassword(final PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void doSubmit(final AjaxRequestTarget target) {
        try {
            userSelfRestClient.changePassword(passwordField.getModelObject());

            SyncopeConsoleSession.get().invalidate();

            setResponsePage(getApplication().getHomePage());
        } catch (Exception e) {
            LOG.error("While changing password for {}",
                    SyncopeConsoleSession.get().getSelfTO().getUsername(), e);
            SyncopeConsoleSession.get().onException(e);
            notificationPanel.refresh(target);
        }
    }

    @Override
    protected UserTO getLoggedUser() {
        return SyncopeConsoleSession.get().getSelfTO();
    }

    @Override
    protected void doCancel() {
        setResponsePage(Login.class);
    }
}
