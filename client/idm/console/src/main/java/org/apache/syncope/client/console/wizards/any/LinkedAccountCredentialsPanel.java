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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggle;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggleConfig;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.LinkedAccountPlainAttrProperty;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class LinkedAccountCredentialsPanel extends WizardStep {

    private static final long serialVersionUID = 5116461957402341603L;

    private String usernameValue;

    private String passwordValue;

    private final LinkedAccountTO linkedAccountTO;

    public LinkedAccountCredentialsPanel(
            final EntityWrapper<LinkedAccountTO> modelObject, final List<String> whichCredentials) {
        super();
        setOutputMarkupId(true);

        linkedAccountTO = modelObject.getInnerObject();

        boolean isUsernameManagementEnabled = whichCredentials.contains(Constants.USERNAME_FIELD_NAME);
        AjaxTextFieldPanel usernameField = new AjaxTextFieldPanel(
                Constants.USERNAME_FIELD_NAME,
                Constants.USERNAME_FIELD_NAME,
                new PropertyModel<>(linkedAccountTO, Constants.USERNAME_FIELD_NAME));
        FieldPanel.class.cast(usernameField).setReadOnly(StringUtils.isBlank(linkedAccountTO.getUsername()));
        LinkedAccountPlainAttrProperty usernameProperty = new LinkedAccountPlainAttrProperty();
        usernameProperty.setOverridable(StringUtils.isNotBlank(linkedAccountTO.getUsername()));
        usernameProperty.setSchema(Constants.USERNAME_FIELD_NAME);
        usernameProperty.getValues().add(linkedAccountTO.getUsername());
        usernameField.showExternAction(
                checkboxToggle(usernameProperty, usernameField).setEnabled(isUsernameManagementEnabled));
        add(usernameField.setOutputMarkupId(true));
        usernameField.setEnabled(isUsernameManagementEnabled);

        boolean isPasswordManagementEnabled = whichCredentials.contains("password");
        AjaxPasswordFieldPanel passwordField = new AjaxPasswordFieldPanel(
                "password",
                "password",
                new PropertyModel<>(linkedAccountTO, "password"),
                false);
        passwordField.setMarkupId("password");
        passwordField.setRequired(true);
        FieldPanel.class.cast(passwordField).setReadOnly(StringUtils.isBlank(linkedAccountTO.getPassword()));
        LinkedAccountPlainAttrProperty passwordProperty = new LinkedAccountPlainAttrProperty();
        passwordProperty.setOverridable(StringUtils.isNotBlank(linkedAccountTO.getPassword()));
        passwordProperty.setSchema("password");
        passwordProperty.getValues().add(linkedAccountTO.getPassword());
        passwordField.showExternAction(
                checkboxToggle(passwordProperty, passwordField).setEnabled(isPasswordManagementEnabled));
        ((PasswordTextField) passwordField.getField()).setResetPassword(false);
        add(passwordField.setOutputMarkupId(true));
        passwordField.setEnabled(isPasswordManagementEnabled);
    }

    private FormComponent<?> checkboxToggle(
            final LinkedAccountPlainAttrProperty property, final FieldPanel<?> panel) {

        final BootstrapToggleConfig config = new BootstrapToggleConfig().
                withOnStyle(BootstrapToggleConfig.Style.success).
                withOffStyle(BootstrapToggleConfig.Style.danger).
                withSize(BootstrapToggleConfig.Size.mini);

        return new BootstrapToggle("externalAction", new PropertyModel<>(property, "overridable"), config) {

            private static final long serialVersionUID = -875219845189261873L;

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                final CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        FieldPanel.class.cast(panel).setReadOnly(!model.getObject());
                        if (model.getObject()) {
                            if (property.getSchema().equals("password")) {
                                linkedAccountTO.setPassword(passwordValue);
                            } else if (property.getSchema().equals(Constants.USERNAME_FIELD_NAME)) {
                                linkedAccountTO.setUsername(usernameValue);
                            }
                        } else {
                            if (property.getSchema().equals("password")) {
                                passwordValue = linkedAccountTO.getPassword();
                                linkedAccountTO.setPassword(null);
                            } else if (property.getSchema().equals(Constants.USERNAME_FIELD_NAME)) {
                                usernameValue = linkedAccountTO.getUsername();
                                linkedAccountTO.setUsername(null);
                            }
                        }
                        target.add(panel);
                    }
                });
                return checkBox;
            }

            @Override
            protected IModel<String> getOnLabel() {
                return Model.of("Override");
            }

            @Override
            protected IModel<String> getOffLabel() {
                return Model.of("Override?");
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.append("class", "overridable", " ");
            }
        };
    }
}
