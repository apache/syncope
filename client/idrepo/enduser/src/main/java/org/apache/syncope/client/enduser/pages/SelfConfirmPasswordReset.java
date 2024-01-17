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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.password.strength.PasswordStrengthBehavior;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.SyncopePasswordStrengthConfig;
import org.apache.syncope.client.ui.commons.panels.CardPanel;
import org.apache.syncope.client.ui.commons.wizards.any.PasswordPanel;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SelfConfirmPasswordReset extends BasePage {

    private static final long serialVersionUID = -2166782304542750726L;

    private static final String CONFIRM_PASSWORD_RESET = "confirmPasswordReset";

    public SelfConfirmPasswordReset(final PageParameters parameters) {
        super(parameters, CONFIRM_PASSWORD_RESET);

        setDomain(parameters);
        disableSidebarAndNavbar();

        if (parameters == null || parameters.get("token").isEmpty()) {
            LOG.error("No token parameter found in the request url");

            PageParameters homeParameters = new PageParameters();
            homeParameters.add("errorMessage", getString("self.confirm.pwd.reset.error.empty"));
            setResponsePage(getApplication().getHomePage(), homeParameters);
        }

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);

        Form<?> form = new StatelessForm<>("selfConfirmPwdResetForm");
        form.setOutputMarkupId(true);
        content.add(form);

        UserTO fakeUserTO = new UserTO();
        PasswordPanel passwordPanel = new PasswordPanel(
                EnduserConstants.CONTENT_PANEL,
                new UserWrapper(fakeUserTO),
                false,
                false,
                new PasswordStrengthBehavior(new SyncopePasswordStrengthConfig()));
        passwordPanel.setOutputMarkupId(true);

        form.add(new CardPanel.Builder<PasswordPanel>()
                .setName("selfConfirmPasswordResetPanel")
                .setComponent(passwordPanel)
                .isVisible(true)
                .build("selfConfirmPasswordResetPanelCard"));

        AjaxButton submit = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 509325877101838812L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                PageParameters params = new PageParameters();
                try {
                    SyncopeEnduserSession.get().getService(UserSelfService.class).confirmPasswordReset(
                            parameters.get("token").toString(), fakeUserTO.getPassword());
                    params.add(EnduserConstants.STATUS, Constants.OPERATION_SUCCEEDED);
                    params.add(Constants.NOTIFICATION_TITLE_PARAM, getString("self.confirm.pwd.reset.success"));
                    params.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.confirm.pwd.reset.success.msg"));
                    SyncopeEnduserSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    parameters.add(EnduserConstants.LANDING_PAGE, Login.class.getName());
                    setResponsePage(SelfResult.class, params);
                } catch (SyncopeClientException sce) {
                    LOG.error("Unable to complete the 'Password Reset Confirmation' process", sce);
                    params.add(EnduserConstants.STATUS, Constants.OPERATION_ERROR);
                    params.add(Constants.NOTIFICATION_TITLE_PARAM, getString("self.confirm.pwd.reset.error"));
                    params.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.confirm.pwd.reset.error.msg"));
                    SyncopeEnduserSession.get().onException(sce);
                    ((BasePage) getPageReference().getPage()).getNotificationPanel().refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                notificationPanel.refresh(target);
            }
        };
        form.setDefaultButton(submit);
        form.add(submit);

        Button cancel = new Button("cancel") {

            private static final long serialVersionUID = 3669569969172391336L;

            @Override
            public void onSubmit() {
                setResponsePage(getApplication().getHomePage());
            }
        };
        cancel.setOutputMarkupId(true);
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
    }
}
