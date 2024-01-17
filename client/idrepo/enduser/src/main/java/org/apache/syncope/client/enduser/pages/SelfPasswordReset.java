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

import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.panels.captcha.CaptchaPanel;
import org.apache.syncope.client.enduser.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.panels.CardPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SelfPasswordReset extends BaseNoSidebarPage {

    private static final long serialVersionUID = 164651008547631054L;

    protected static final String SELF_PWD_RESET = "page.selfPwdReset";

    @SpringBean
    protected UserSelfRestClient userSelfRestClient;

    @SpringBean
    protected SecurityQuestionRestClient securityQuestionRestClient;

    protected String usernameValue;

    protected String securityAnswerValue;

    protected final CaptchaPanel<Void> captcha;

    protected final SelfPwdResetPanel pwdResetPanel;

    public SelfPasswordReset(final PageParameters parameters) {
        super(parameters, SELF_PWD_RESET);

        setDomain(parameters);
        disableSidebarAndNavbar();

        captcha = new CaptchaPanel<>("captchaPanel");
        captcha.setOutputMarkupPlaceholderTag(true);
        captcha.setVisible(SyncopeWebApplication.get().isCaptchaEnabled());

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);

        Form<?> form = new Form<>("selfPwdResetForm");
        content.add(form);

        pwdResetPanel = new SelfPwdResetPanel(EnduserConstants.CONTENT_PANEL, captcha, getPageReference());
        pwdResetPanel.setOutputMarkupId(true);

        form.add(new CardPanel.Builder<SelfPwdResetPanel>()
                .setName("selfPasswordResetPanel")
                .setComponent(pwdResetPanel)
                .isVisible(true)
                .build("selfPasswordResetPanelCard"));

        AjaxButton submitButton = new AjaxButton("submit") {

            private static final long serialVersionUID = 4284361595033427185L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                if (SyncopeWebApplication.get().isCaptchaEnabled() && !captcha.check()) {
                    SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
                    SelfPasswordReset.this.getNotificationPanel().refresh(target);
                } else {
                    PageParameters parameters = new PageParameters();
                    try {
                        userSelfRestClient.requestPasswordReset(usernameValue, securityAnswerValue);
                        parameters.add(EnduserConstants.STATUS, Constants.OPERATION_SUCCEEDED);
                        parameters.add(Constants.NOTIFICATION_TITLE_PARAM, getString("self.pwd.reset.success"));
                        parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.pwd.reset.success.msg"));
                        parameters.add(EnduserConstants.LANDING_PAGE, Login.class.getName());
                        setResponsePage(SelfResult.class, parameters);
                    } catch (SyncopeClientException sce) {
                        LOG.error("Unable to reset password of [{}]", usernameValue, sce);
                        SyncopeEnduserSession.get().onException(sce);
                        SelfPasswordReset.this.getNotificationPanel().refresh(target);
                    }
                }
            }

        };
        submitButton.setOutputMarkupId(true);
        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

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

    public class SelfPwdResetPanel extends Panel {

        private static final long serialVersionUID = -2841210052053545578L;

        private final TextField<String> securityQuestion;

        SelfPwdResetPanel(final String id, final CaptchaPanel<Void> captcha, final PageReference pageRef) {
            super(id);

            boolean isSecurityQuestionEnabled =
                    SyncopeEnduserSession.get().getPlatformInfo().isPwdResetRequiringSecurityQuestions();

            TextField<String> username = new TextField<>("username",
                    new PropertyModel<>(SelfPasswordReset.this, "usernameValue"), String.class);
            username.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (isSecurityQuestionEnabled) {
                        loadSecurityQuestion(pageRef, target);
                    }
                }
            });
            username.setRequired(true);
            add(username);

            Label sqLabel =
                    new Label("securityQuestionLabel", new ResourceModel("securityQuestion", "securityQuestion"));
            sqLabel.setOutputMarkupPlaceholderTag(true);
            sqLabel.setVisible(isSecurityQuestionEnabled);
            add(sqLabel);

            securityQuestion =
                    new TextField<>("securityQuestion", new PropertyModel<>(Model.of(), "content"), String.class);
            securityQuestion.setOutputMarkupId(true);
            securityQuestion.setEnabled(false);
            securityQuestion.setOutputMarkupPlaceholderTag(true);
            securityQuestion.setVisible(isSecurityQuestionEnabled);
            add(securityQuestion);

            Label notLoading = new Label("not.loading", new ResourceModel("not.loading", "not.loading"));
            notLoading.setOutputMarkupPlaceholderTag(true);
            notLoading.setVisible(isSecurityQuestionEnabled);
            add(notLoading);

            AjaxLink<Void> reloadLink = new AjaxLink<>("reloadLink") {

                private static final long serialVersionUID = -817438685948164787L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    loadSecurityQuestion(pageRef, target);
                }
            };
            reloadLink.setOutputMarkupPlaceholderTag(true);
            reloadLink.setVisible(isSecurityQuestionEnabled);
            add(reloadLink);

            Label saLabel = new Label("securityAnswerLabel", new ResourceModel("securityAnswer", "securityAnswer"));
            saLabel.setOutputMarkupPlaceholderTag(true);
            saLabel.setVisible(isSecurityQuestionEnabled);
            add(saLabel);

            TextField<String> securityAnswer =
                    new TextField<>("securityAnswer", new PropertyModel<>(SelfPasswordReset.this,
                            "securityAnswerValue"), String.class);
            securityAnswer.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // do nothing
                }
            });
            securityAnswer.setRequired(isSecurityQuestionEnabled);
            securityAnswer.setOutputMarkupPlaceholderTag(true);
            securityAnswer.setVisible(isSecurityQuestionEnabled);
            add(securityAnswer);

            add(captcha);
        }

        protected void loadSecurityQuestion(final PageReference pageRef, final AjaxRequestTarget target) {
            try {
                SecurityQuestionTO securityQuestionTO = securityQuestionRestClient.readByUser(usernameValue);
                // set security question field model
                securityQuestion.setModel(Model.of(securityQuestionTO.getContent()));
                target.add(securityQuestion);
            } catch (Exception e) {
                LOG.error("Unable to get security question for [{}]", usernameValue, e);
                SyncopeEnduserSession.get().onException(e);
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }
    }
}
