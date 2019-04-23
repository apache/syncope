/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.enduser.panels;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.pages.BaseEnduserWebPage;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.wizards.any.CaptchaPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfPwdResetPanel extends Panel implements IEventSource {

    private static final long serialVersionUID = -2841210052053545578L;

    private static final Logger LOG = LoggerFactory.getLogger(SelfPwdResetPanel.class);

    private String usernameText;

    private final TextField<String> securityQuestion;

    private final CaptchaPanel<Void> captcha;

    protected final Model<String> securityAnswerModel = new Model<>();

    public SelfPwdResetPanel(final String id, final PageReference pageRef) {
        super(id);

        TextField<String> username = new TextField<>("username", new PropertyModel<>(SelfPwdResetPanel.this,
                "usernameText"), String.class);
        username.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                loadSecurityQuestion(pageRef, target);
            }

        });
        username.setRequired(true);
        add(username);

        securityQuestion = new TextField<>("securityQuestion", new PropertyModel<>(Model.of(), "content"),
                String.class);
        securityQuestion.setOutputMarkupId(true);
        securityQuestion.setEnabled(false);
        add(securityQuestion);

        AjaxLink<Void> reloadLink = new AjaxLink<>("reloadLink") {

            private static final long serialVersionUID = -817438685948164787L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                loadSecurityQuestion(pageRef, target);
            }
        };
        add(reloadLink);

        AjaxTextFieldPanel securityAnswer = new AjaxTextFieldPanel("securityAnswer", "securityAnswer",
                securityAnswerModel);
        securityAnswer.setOutputMarkupId(true);
        securityAnswer.setOutputMarkupPlaceholderTag(true);
        securityAnswer.setRequired(true);
        add(securityAnswer);

        captcha = new CaptchaPanel<>("captchaPanel");
        add(captcha);

        AjaxButton submitButton = new AjaxButton("submit") {

            private static final long serialVersionUID = 4284361595033427185L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                // captcha check
                if (!captcha.captchaCheck()) {
                    SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
                    ((BaseEnduserWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                } else {
                    try {
                        SyncopeEnduserSession.get().getService(UserSelfService.class)
                                .requestPasswordReset(usernameText, securityAnswerModel.getObject());
                        final PageParameters parameters = new PageParameters();
                        parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.pwd.reset.success"));
                        setResponsePage(Login.class, parameters);
                    } catch (SyncopeClientException sce) {
                        LOG.error("Unable to reset password of [{}]", usernameText, sce);
                        SyncopeEnduserSession.get().error(StringUtils.isBlank(sce.getMessage())
                                ? sce.getClass().getName()
                                : sce.getMessage());
                        ((BaseEnduserWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }
            }

        };
        submitButton.setOutputMarkupId(true);
        submitButton.setDefaultFormProcessing(false);
        add(submitButton);

        Button cancel = new Button("cancel") {

            private static final long serialVersionUID = 3669569969172391336L;

            @Override
            public void onSubmit() {
                setResponsePage(Login.class);
            }

        };
        cancel.setOutputMarkupId(true);
        cancel.setDefaultFormProcessing(false);
        add(cancel);
    }

    protected void loadSecurityQuestion(final PageReference pageRef, final AjaxRequestTarget target) {
        try {
            SecurityQuestionTO securityQuestionTO = SyncopeEnduserSession.get().getService(
                    SecurityQuestionService.class).readByUser(usernameText);
            // set security question field model
            securityQuestion.setModel(Model.of(securityQuestionTO.getContent()));
            target.add(securityQuestion);
        } catch (Exception e) {
            LOG.error("Unable to get security question for [{}]", usernameText, e);
            SyncopeEnduserSession.get().error(StringUtils.isBlank(e.getMessage())
                    ? e.getClass().getName()
                    : e.getMessage());
            ((BaseEnduserWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }

}
