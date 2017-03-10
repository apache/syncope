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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.console.rest.UserSelfRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MustChangePassword extends WebPage {

    private static final long serialVersionUID = 5889157642852559004L;

    private static final Logger LOG = LoggerFactory.getLogger(MustChangePassword.class);

    private final StatelessForm<Void> form;

    private final TextField<String> usernameField;

    private final AjaxPasswordFieldPanel passwordField;

    private final AjaxPasswordFieldPanel confirmPasswordField;

    private final UserSelfRestClient restClient = new UserSelfRestClient();

    public MustChangePassword(final PageParameters parameters) {
        super(parameters);

        final NotificationPanel notificationPanel = new NotificationPanel(Constants.FEEDBACK);
        add(notificationPanel);

        form = new StatelessForm<>("changePassword");
        form.setOutputMarkupId(true);

        usernameField = new TextField<>("username", new Model<>(SyncopeConsoleSession.get().getSelfTO().getUsername()));
        usernameField.setMarkupId("username");
        usernameField.setEnabled(false);
        form.add(usernameField);

        passwordField = new AjaxPasswordFieldPanel("password", "password", new Model<String>());
        passwordField.setRequired(true);
        passwordField.setMarkupId("password");
        passwordField.setPlaceholder("password");
        ((PasswordTextField) passwordField.getField()).setResetPassword(true);
        form.add(passwordField);

        confirmPasswordField = new AjaxPasswordFieldPanel("confirmPassword", "confirmPassword", new Model<String>());
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setMarkupId("confirmPassword");
        confirmPasswordField.setPlaceholder("confirmPassword");
        ((PasswordTextField) confirmPasswordField.getField()).setResetPassword(true);
        form.add(confirmPasswordField);

        form.add(new EqualPasswordInputValidator(passwordField.getField(), confirmPasswordField.getField()));

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    restClient.changePassword(passwordField.getModelObject());

                    SyncopeConsoleSession.get().invalidate();

                    setResponsePage(getApplication().getHomePage());
                } catch (Exception e) {
                    LOG.error("While changing password for {}",
                            SyncopeConsoleSession.get().getSelfTO().getUsername(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                    notificationPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                notificationPanel.refresh(target);
            }

        };
        form.add(submitButton);
        form.setDefaultButton(submitButton);

        add(form);
    }
}
