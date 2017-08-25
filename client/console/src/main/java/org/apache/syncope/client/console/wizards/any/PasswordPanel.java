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
package org.apache.syncope.client.console.wizards.any;

import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
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
            final boolean templateMode) {

        super(id);
        setOutputMarkupId(true);

        Form<?> form = new Form<>("passwordInnerForm");
        add(form);

        FieldPanel<String> confirmPasswordField = new AjaxPasswordFieldPanel(
                "confirmPassword", "confirmPassword", new Model<>(), false);

        confirmPasswordField.setMarkupId("confirmPassword");
        confirmPasswordField.setPlaceholder("confirmPassword");
        ((PasswordTextField) confirmPasswordField.getField()).setResetPassword(false);
        form.add(confirmPasswordField);

        if (templateMode) {
            confirmPasswordField.setEnabled(false);
            confirmPasswordField.setVisible(false);

            AjaxTextFieldPanel passwordField = new AjaxTextFieldPanel(
                    "password", "password", new PropertyModel<>(wrapper.getInnerObject(), "password"), false);
            passwordField.setRequired(true);
            passwordField.setMarkupId("password");
            passwordField.setPlaceholder("password");
            form.add(passwordField);
            passwordField.enableJexlHelp();
        } else {
            AjaxPasswordFieldPanel passwordField = new AjaxPasswordFieldPanel(
                    "password", "password", new PropertyModel<>(wrapper.getInnerObject(), "password"), false);
            passwordField.setRequired(true);
            passwordField.setMarkupId("password");
            passwordField.setPlaceholder("password");
            ((PasswordTextField) passwordField.getField()).setResetPassword(false);
            form.add(passwordField);
            form.add(new EqualPasswordInputValidator(passwordField.getField(), confirmPasswordField.getField()));
        }

        AjaxCheckBoxPanel storePasswordInSyncope = new AjaxCheckBoxPanel("storePasswordInSyncope",
                "storePasswordInSyncope", new PropertyModel<>(wrapper, "storePasswordInSyncope"));
        storePasswordInSyncope.getField().setLabel(new ResourceModel("storePasswordInSyncope"));
        storePasswordInSyncope.setOutputMarkupId(true);
        storePasswordInSyncope.setOutputMarkupPlaceholderTag(true);
        if (wrapper.getInnerObject().getKey() == null) {
            storePasswordInSyncope.getField().setDefaultModelObject(Boolean.TRUE);
        } else {
            storePasswordInSyncope.setVisible(false);
        }
        form.add(storePasswordInSyncope);
    }
}
