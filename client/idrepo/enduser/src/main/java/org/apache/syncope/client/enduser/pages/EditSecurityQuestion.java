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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.panels.captcha.CaptchaPanel;
import org.apache.syncope.client.enduser.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.panels.CardPanel;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class EditSecurityQuestion extends BasePage {

    private static final long serialVersionUID = -537205681762708502L;

    private static final String EDIT_SECURITY_QUESTION = "page.editSecurityQuestion";

    private final UserSelfRestClient userSelfRestClient = new UserSelfRestClient();

    private final AjaxDropDownChoicePanel<String> securityQuestion;

    private final FieldPanel<String> securityAnswer;

    private final UserTO userTO;

    public EditSecurityQuestion(final PageParameters parameters) {
        super(parameters, EDIT_SECURITY_QUESTION);

        userTO = SyncopeEnduserSession.get().getSelfTO(true);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);

        StatelessForm<Void> form = new StatelessForm<>("securityQuestionForm");
        form.setOutputMarkupId(true);
        content.add(form);

        securityQuestion = new AjaxDropDownChoicePanel<>("securityQuestion",
                "securityQuestion", new PropertyModel<>(userTO, "securityQuestion"));
        securityQuestion.setNullValid(true);

        List<SecurityQuestionTO> securityQuestions = SecurityQuestionRestClient.list();
        securityQuestion.setChoices(securityQuestions.stream().
                map(SecurityQuestionTO::getKey).collect(Collectors.toList()));
        securityQuestion.setChoiceRenderer(new IChoiceRenderer<>() {

            private static final long serialVersionUID = -4421146737845000747L;

            @Override
            public Object getDisplayValue(final String value) {
                return securityQuestions.stream().filter(sq -> value.equals(sq.getKey())).
                    map(SecurityQuestionTO::getContent).findFirst().orElse(null);
            }

            @Override
            public String getIdValue(final String value, final int index) {
                return value;
            }

            @Override
            public String getObject(final String id, final IModel<? extends List<? extends String>> choices) {
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

        form.add(securityQuestion);

        securityAnswer = new AjaxTextFieldPanel("securityAnswer", "securityAnswer",
                new PropertyModel<>(userTO, "securityAnswer"), false);
        form.add(securityAnswer.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).
                setEnabled(StringUtils.isNotBlank(securityQuestion.getModelObject())));

        CaptchaPanel<Void> captcha = new CaptchaPanel<>(EnduserConstants.CONTENT_PANEL);
        captcha.setOutputMarkupPlaceholderTag(true);

        form.add(new CardPanel.Builder<CaptchaPanel<Void>>()
                .setName("captcha")
                .setComponent(captcha)
                .isVisible(SyncopeWebApplication.get().isCaptchaEnabled()).build("captchaPanelCard"));

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                if (StringUtils.isBlank(securityQuestion.getModelObject())
                        || StringUtils.isBlank(securityAnswer.getModelObject())) {

                    SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
                    ((BasePage) getPageReference().getPage()).getNotificationPanel().refresh(target);
                } else {
                    boolean checked = true;
                    if (SyncopeWebApplication.get().isCaptchaEnabled()) {
                        checked = captcha.check();
                    }
                    if (!checked) {
                        SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
                        ((BasePage) getPageReference().getPage()).getNotificationPanel().refresh(target);
                    } else {
                        PageParameters parameters = new PageParameters();
                        try {
                            UserUR req = new UserUR();
                            req.setKey(userTO.getKey());
                            req.setSecurityQuestion(new StringReplacePatchItem.Builder().
                                    value(securityQuestion.getModelObject()).build());
                            req.setSecurityAnswer(new StringReplacePatchItem.Builder().
                                    value(securityAnswer.getModelObject()).build());
                            userSelfRestClient.update(userTO.getETagValue(), req);

                            parameters.add(EnduserConstants.STATUS, Constants.OPERATION_SUCCEEDED);
                            parameters.add(Constants.NOTIFICATION_TITLE_PARAM,
                                    getString("self.securityquestion.change.success"));
                            parameters.add(Constants.NOTIFICATION_MSG_PARAM,
                                    getString("self.securityquestion.change.success.msg"));
                            SyncopeEnduserSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("While changing password for {}",
                                    SyncopeEnduserSession.get().getSelfTO().getUsername(), e);
                            parameters.add(EnduserConstants.STATUS, Constants.OPERATION_ERROR);
                            parameters.add(Constants.NOTIFICATION_TITLE_PARAM,
                                    getString("self.securityquestion.change.error"));
                            parameters.add(Constants.NOTIFICATION_MSG_PARAM,
                                    getString("self.securityquestion.change.error.msg"));
                            SyncopeEnduserSession.get().onException(e);
                            notificationPanel.refresh(target);
                        }
                        parameters.add(
                                EnduserConstants.LANDING_PAGE,
                                SyncopeWebApplication.get().getPageClass("profile", Dashboard.class).getName());
                        setResponsePage(SelfResult.class, parameters);
                    }
                }
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
                setResponsePage(getApplication().getHomePage());
            }
        };

        cancel.setOutputMarkupId(true);
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
    }
}
