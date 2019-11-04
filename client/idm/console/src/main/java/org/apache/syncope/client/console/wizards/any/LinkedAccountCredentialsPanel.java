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
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.client.console.commons.LinkedAccountPlainAttrProperty;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
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

    public LinkedAccountCredentialsPanel(final LinkedAccountTO linkedAccountTO) {
        super();
        setOutputMarkupId(true);

        AjaxTextFieldPanel usernameField = new AjaxTextFieldPanel(
                "username",
                "username",
                new PropertyModel<>(linkedAccountTO, "username"),
                false);
        usernameField.setOutputMarkupId(true);
        FieldPanel.class.cast(usernameField).setReadOnly(true);
        LinkedAccountPlainAttrProperty property = new LinkedAccountPlainAttrProperty();
        property.setOverridable(false);
        property.setSchema("username");
        property.getValues().add(linkedAccountTO.getUsername());
        usernameField.showExternAction(checkboxToggle(property, usernameField));
        add(usernameField);

        AjaxPasswordFieldPanel passwordField = new AjaxPasswordFieldPanel(
                "password",
                "password",
                new PropertyModel<>(linkedAccountTO, "password"));
        passwordField.setOutputMarkupId(true);
        passwordField.setRequired(true);
        passwordField.setMarkupId("password");
        FieldPanel.class.cast(passwordField).setReadOnly(true);
        property = new LinkedAccountPlainAttrProperty();
        property.setOverridable(false);
        property.setSchema("password");
        property.getValues().add(linkedAccountTO.getPassword());
        passwordField.showExternAction(checkboxToggle(property, passwordField));
        ((PasswordTextField) passwordField.getField()).setResetPassword(true);
        add(passwordField);
    }

    private FormComponent<?> checkboxToggle(
            final LinkedAccountPlainAttrProperty property, final FieldPanel<?> panel) {

        final BootstrapToggleConfig config = new BootstrapToggleConfig().
                withOnStyle(BootstrapToggleConfig.Style.success).
                withOffStyle(BootstrapToggleConfig.Style.danger).
                withSize(BootstrapToggleConfig.Size.mini);

        return new BootstrapToggle("externalAction", new PropertyModel<Boolean>(property, "overridable"), config) {

            private static final long serialVersionUID = -875219845189261873L;

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                final CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        panel.setReadOnly(!model.getObject());
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
