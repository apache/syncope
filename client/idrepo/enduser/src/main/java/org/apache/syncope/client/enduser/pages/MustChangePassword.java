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

import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class MustChangePassword extends AbstractChangePassword {

    private static final long serialVersionUID = 8581970794722709800L;

    public MustChangePassword(final PageParameters parameters) {
        super(parameters);

        setDomain(parameters);
        disableSidebar();
    }

    @Override
    protected void doPwdSubmit(final AjaxRequestTarget target, final AjaxPasswordFieldPanel passwordField) {
        PageParameters parameters = new PageParameters();
        try {
            UserSelfRestClient.changePassword(passwordField.getModelObject());

            SyncopeEnduserSession.get().invalidate();
            parameters.add(EnduserConstants.STATUS, Constants.OPERATION_SUCCEEDED);
            parameters.add(Constants.NOTIFICATION_TITLE_PARAM, getString("self.pwd.change.success"));
            parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.pwd.change.success"));
            SyncopeEnduserSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
        } catch (Exception e) {
            LOG.error("While changing password for {}",
                    SyncopeEnduserSession.get().getSelfTO().getUsername(), e);
            parameters.add(EnduserConstants.STATUS, Constants.OPERATION_ERROR);
            parameters.add(Constants.NOTIFICATION_TITLE_PARAM, getString("self.pwd.change.error"));
            parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.pwd.change.error.msg"));
            SyncopeEnduserSession.get().onException(e);
        }
        notificationPanel.refresh(target);
        setResponsePage(SelfResult.class, parameters);
    }

    @Override
    protected UserTO getPwdLoggedUser() {
        return SyncopeEnduserSession.get().getSelfTO();
    }

    @Override
    protected void doPwdCancel() {
        SyncopeEnduserSession.get().invalidate();
        final PageParameters parameters = new PageParameters();
        setResponsePage(getApplication().getHomePage(), parameters);
    }
}
