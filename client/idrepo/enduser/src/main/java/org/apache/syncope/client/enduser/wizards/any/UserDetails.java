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
package org.apache.syncope.client.enduser.wizards.any;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.password.strength.PasswordStrengthBehavior;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.password.strength.PasswordStrengthConfig;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.rest.RealmRestClient;
import org.apache.syncope.client.enduser.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wizards.any.PasswordPanel;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class UserDetails extends WizardStep {

    private static final long serialVersionUID = 6592027822510220463L;

    private static final String PASSWORD_CONTENT_PATH = "body:content";

    private final FieldPanel<String> realm;

    protected final AjaxTextFieldPanel username;

    private final FieldPanel<String> securityQuestion;

    private final FieldPanel<String> securityAnswer;

    protected final UserTO userTO;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public UserDetails(
            final UserWrapper wrapper,
            final boolean templateMode,
            final boolean showPasswordManagement) {

        super();

        userTO = wrapper.getInnerObject();
        // ------------------------
        // Username
        // ------------------------
        username = new AjaxTextFieldPanel(Constants.USERNAME_FIELD_NAME, Constants.USERNAME_FIELD_NAME,
                new PropertyModel<>(userTO, Constants.USERNAME_FIELD_NAME), false);

        if (wrapper.getPreviousUserTO() != null && StringUtils.
                compare(wrapper.getPreviousUserTO().getUsername(), wrapper.getInnerObject().getUsername()) != 0) {
            username.showExternAction(new LabelInfo("externalAction", wrapper.getPreviousUserTO().getUsername()));
        }

        if (templateMode) {
            username.enableJexlHelp();
        } else {
            username.addRequiredLabel();
        }
        add(username);
        // ------------------------

        // ------------------------
        // Realm
        // ------------------------
        realm = new AjaxDropDownChoicePanel<>(
                "destinationRealm", "destinationRealm", new PropertyModel<>(userTO, "realm"), false);

        ((AjaxDropDownChoicePanel<String>) realm).setChoices(
                RealmRestClient.list().stream().map(RealmTO::getFullPath).collect(Collectors.toList()));
        add(realm);

        // ------------------------
        // Password
        // ------------------------
        Model<Integer> model = Model.of(-1);

        Accordion accordion = new Accordion("accordionPanel", List.of(
                new AbstractTab(new ResourceModel("password.change", "Change password")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public Panel getPanel(final String panelId) {
                EditUserPasswordPanel panel = new EditUserPasswordPanel(panelId, wrapper, templateMode);
                panel.setEnabled(model.getObject() >= 0);
                return panel;
            }
        }), model) {

            private static final long serialVersionUID = -2898628183677758699L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Accordion.State state) {
                return new AjaxLink<Integer>(markupId) {

                    private static final long serialVersionUID = 7021195294339489084L;

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("style", "color: #337ab7");
                    }

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                        Component passwordPanel = getParent().get(PASSWORD_CONTENT_PATH);
                        passwordPanel.setEnabled(model.getObject() >= 0);
                        target.add(passwordPanel);
                    }
                }.setBody(new ResourceModel("password.change", "Change password ..."));
            }
        };

        accordion.setOutputMarkupId(true);
        accordion.setVisible(showPasswordManagement);
        add(accordion);
        // ------------------------

        // ------------------------
        // Security Question
        // ------------------------
        securityQuestion = new AjaxDropDownChoicePanel("securityQuestion", "securityQuestion", new PropertyModel<>(
                userTO, "securityQuestion"));
        ((AjaxDropDownChoicePanel) securityQuestion).setNullValid(true);

        final List<SecurityQuestionTO> securityQuestions = SecurityQuestionRestClient.list();
        ((AjaxDropDownChoicePanel<String>) securityQuestion).setChoices(securityQuestions.stream().map(
                SecurityQuestionTO::getKey).collect(Collectors.toList()));
        ((AjaxDropDownChoicePanel<String>) securityQuestion).setChoiceRenderer(
                new IChoiceRenderer<String>() {

            private static final long serialVersionUID = -4421146737845000747L;

            @Override
            public Object getDisplayValue(final String value) {
                return securityQuestions.stream().filter(sq -> value.equals(sq.getKey()))
                        .map(SecurityQuestionTO::getContent).findFirst().orElse(null);
            }

            @Override
            public String getIdValue(final String value, final int index) {
                return value;
            }

            @Override
            public String getObject(
                    final String id,
                    final IModel<? extends List<? extends String>> choices) {
                return id;
            }
        });

        securityQuestion.add(new AjaxEventBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = 192359260308762078L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                securityAnswer.setEnabled(StringUtils.isNotBlank(securityQuestion.getModelObject()));
                target.add(securityAnswer);
            }
        });

        add(securityQuestion);
        // ------------------------

        // ------------------------
        // Security Answer
        // ------------------------
        securityAnswer =
                new AjaxTextFieldPanel("securityAnswer", "securityAnswer",
                        new PropertyModel<>(userTO, "securityAnswer"), false);
        add(securityAnswer.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setEnabled(StringUtils.
                isNotBlank(securityQuestion.getModelObject())));
        // ------------------------
    }

    public static class EditUserPasswordPanel extends Panel {

        private static final long serialVersionUID = -8198836979773590078L;

        public EditUserPasswordPanel(
                final String id,
                final UserWrapper wrapper,
                final boolean templateMode) {
            super(id);
            setOutputMarkupId(true);
            add(new Label("warning", new ResourceModel("password.change.warning")));
            add(new PasswordPanel("passwordPanel", wrapper, templateMode, new PasswordStrengthBehavior(
                    new PasswordStrengthConfig()
                            .withDebug(true)
                            .withShowVerdictsInsideProgressBar(true)
                            .withShowProgressBar(true))));
        }

    }
}
