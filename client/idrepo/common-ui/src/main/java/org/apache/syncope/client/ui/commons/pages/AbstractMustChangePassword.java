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
package org.apache.syncope.client.ui.commons.pages;

import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMustChangePassword extends WebPage {

    private static final long serialVersionUID = 5889157642852559004L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractMustChangePassword.class);

    protected final StatelessForm<Void> form;

    protected final TextField<String> usernameField;

    protected final PasswordTextField passwordField;

    protected final PasswordTextField confirmPasswordField;

    protected final NotificationPanel notificationPanel = new NotificationPanel(Constants.FEEDBACK);

    public AbstractMustChangePassword(final PageParameters parameters) {
        super(parameters);

        add(notificationPanel);

        form = new StatelessForm<>("changePassword");
        form.setOutputMarkupId(true);

        usernameField = new TextField<>("username", new Model<>(getLoggedUser().getUsername()));
        usernameField.setMarkupId("username");
        usernameField.setEnabled(false);
        form.add(usernameField);

        passwordField = new PasswordTextField("password", new Model<>());
        passwordField.setRequired(true);
        passwordField.setMarkupId("password");
        passwordField.setResetPassword(true);
        form.add(passwordField);

        confirmPasswordField = new PasswordTextField("confirmPassword", new Model<>());
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setMarkupId("confirmPassword");
        confirmPasswordField.setResetPassword(true);
        form.add(confirmPasswordField);

        form.add(new EqualPasswordInputValidator(passwordField, confirmPasswordField));

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                doSubmit(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                notificationPanel.refresh(target);
            }

        };
        form.add(submitButton);
        form.setDefaultButton(submitButton);

        Button cancel = new Button("cancel") {

            private static final long serialVersionUID = 3669569969172391336L;

            @Override
            public void onSubmit() {
                doCancel();
            }

        };
        cancel.setOutputMarkupId(true);
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);

        add(new AttributeModifier("style", "height: \"100%\""));
    }

    protected abstract void doSubmit(AjaxRequestTarget target);

    protected abstract void doCancel();

    protected abstract UserTO getLoggedUser();
}
