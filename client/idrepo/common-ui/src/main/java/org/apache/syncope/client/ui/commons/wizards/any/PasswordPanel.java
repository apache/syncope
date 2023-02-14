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
package org.apache.syncope.client.ui.commons.wizards.any;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.password.strength.PasswordStrengthBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class PasswordPanel extends Panel {

    private static final long serialVersionUID = 6592027822510220463L;

    public PasswordPanel(
            final String id,
            final UserWrapper wrapper,
            final Boolean storePasswordInSyncope,
            final boolean templateMode) {

        this(id, wrapper, templateMode, storePasswordInSyncope, null);
    }

    public PasswordPanel(
            final String id,
            final UserWrapper wrapper,
            final boolean templateMode,
            final Boolean storePasswordInSyncope,
            final PasswordStrengthBehavior passwordStrengthBehavior) {

        super(id);
        setOutputMarkupId(true);

        Form<?> form = new Form<>("passwordInnerForm");
        add(form);

        AjaxPasswordFieldPanel confirmPasswordField = new AjaxPasswordFieldPanel(
                "confirmPassword", "confirmPassword", Model.of(), false);
        ((PasswordTextField) confirmPasswordField.getField()).setResetPassword(false);
        form.add(confirmPasswordField.setPlaceholder("confirmPassword").setMarkupId("confirmPassword"));

        if (templateMode) {
            confirmPasswordField.setEnabled(false);
            confirmPasswordField.setVisible(false);

            AjaxTextFieldPanel passwordField = new AjaxTextFieldPanel(
                    "password", "password", new PropertyModel<>(wrapper.getInnerObject(), "password"), false);
            passwordField.setRequired(false); // [SYNCOPE-1227]
            passwordField.setMarkupId("password");
            passwordField.setPlaceholder("password");
            form.add(passwordField);
            passwordField.enableJexlHelp();
        } else {
            AjaxPasswordFieldPanel passwordField = new AjaxPasswordFieldPanel(
                    "password",
                    "password",
                    new PropertyModel<>(wrapper.getInnerObject(), "password"),
                    false,
                    passwordStrengthBehavior);
            passwordField.setRequired(true);
            passwordField.setMarkupId("password");
            passwordField.setPlaceholder("password");
            ((PasswordTextField) passwordField.getField()).setResetPassword(false);
            form.add(passwordField);
            form.add(new EqualPasswordInputValidator(passwordField.getField(), confirmPasswordField.getField()));
        }

        AjaxCheckBoxPanel storePasswordInSyncopePanel = new AjaxCheckBoxPanel("storePasswordInSyncope",
                "storePasswordInSyncope", new PropertyModel<>(wrapper, "storePasswordInSyncope"));
        storePasswordInSyncopePanel.getField().setLabel(new ResourceModel("storePasswordInSyncope"));
        storePasswordInSyncopePanel.setOutputMarkupId(true);
        storePasswordInSyncopePanel.setOutputMarkupPlaceholderTag(true);
        if (storePasswordInSyncope) {
            storePasswordInSyncopePanel.getField().setDefaultModelObject(Boolean.TRUE);
        } else {
            storePasswordInSyncopePanel.setVisible(false);
        }
        form.add(storePasswordInSyncopePanel);
    }
}
