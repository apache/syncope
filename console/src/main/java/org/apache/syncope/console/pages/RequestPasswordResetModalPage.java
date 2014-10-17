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
package org.apache.syncope.console.pages;

import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.Mode;
import org.apache.syncope.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RequestPasswordResetModalPage extends BaseModalPage {

    private static final long serialVersionUID = -8419445804421211904L;

    @SpringBean
    private SecurityQuestionRestClient securityQuestionRestClient;

    public RequestPasswordResetModalPage(final ModalWindow window) {
        super();
        setOutputMarkupId(true);

        final boolean handleSecurityQuestion = userSelfRestClient.isPwdResetRequiringSecurityQuestions();

        final StatelessForm<?> form = new StatelessForm<Object>(FORM);
        form.setOutputMarkupId(true);

        final Label securityQuestionLabel = new Label("securityQuestionLabel", getString("securityQuestion"));
        securityQuestionLabel.setOutputMarkupPlaceholderTag(true);
        securityQuestionLabel.setVisible(handleSecurityQuestion);
        form.add(securityQuestionLabel);
        final AjaxTextFieldPanel securityQuestion =
                new AjaxTextFieldPanel("securityQuestion", "securityQuestion", new Model<String>());
        securityQuestion.setReadOnly(true);
        securityQuestion.setRequired(true);
        securityQuestion.getField().setOutputMarkupId(true);
        securityQuestion.setOutputMarkupPlaceholderTag(true);
        securityQuestion.setVisible(handleSecurityQuestion);
        form.add(securityQuestion);

        final AjaxTextFieldPanel username =
                new AjaxTextFieldPanel("username", "username", new Model<String>());
        username.setRequired(true);
        username.getField().setOutputMarkupId(true);
        if (handleSecurityQuestion) {
            username.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    getFeedbackMessages().clear();
                    target.add(feedbackPanel);
                    try {
                        SecurityQuestionTO read = securityQuestionRestClient.readByUser(username.getModelObject());
                        securityQuestion.setModelObject(read.getContent());
                    } catch (Exception e) {
                        LOG.error("While fetching security question for {}", username.getModelObject(), e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        feedbackPanel.refresh(target);
                        securityQuestion.setModelObject(null);
                    } finally {
                        target.add(securityQuestion);
                    }
                }
            });
        }
        form.add(username);

        final Label securityAnswerLabel = new Label("securityAnswerLabel", getString("securityAnswer"));
        securityAnswerLabel.setOutputMarkupPlaceholderTag(true);
        securityAnswerLabel.setVisible(handleSecurityQuestion);
        form.add(securityAnswerLabel);
        final AjaxTextFieldPanel securityAnswer =
                new AjaxTextFieldPanel("securityAnswer", "securityAnswer", new Model<String>());
        securityAnswer.setRequired(handleSecurityQuestion);
        securityAnswer.setOutputMarkupPlaceholderTag(true);
        securityAnswer.setVisible(handleSecurityQuestion);
        form.add(securityAnswer);

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(SUBMIT, SUBMIT)) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    userSelfRestClient.requestPasswordReset(username.getModelObject(), securityAnswer.getModelObject());

                    setResponsePage(new ResultStatusModalPage.Builder(window, new UserTO()).
                            mode(Mode.SELF).build());
                } catch (Exception e) {
                    LOG.error("While requesting password reset for {}", username.getModelObject(), e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };
        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                // do nothing
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }
}
