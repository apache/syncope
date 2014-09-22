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

import static org.apache.syncope.console.pages.AbstractBasePage.FORM;
import static org.apache.syncope.console.pages.AbstractBasePage.LOG;

import org.apache.syncope.common.to.SecurityQuestionTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
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

        final Form<?> form = new Form<Object>(FORM);
        form.setOutputMarkupId(true);

        final AjaxTextFieldPanel securityQuestion =
                new AjaxTextFieldPanel("securityQuestion", "securityQuestion", new Model<String>());
        securityQuestion.setReadOnly(true);
        securityQuestion.setRequired(true);
        securityQuestion.getField().setOutputMarkupId(true);
        form.add(securityQuestion);

        final AjaxTextFieldPanel username =
                new AjaxTextFieldPanel("username", "username", new Model<String>());
        username.setRequired(true);
        username.getField().setOutputMarkupId(true);
        username.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                try {
                    SecurityQuestionTO read = securityQuestionRestClient.readByUser(username.getModelObject());
                    securityQuestion.setModelObject(read.getContent());
                    target.add(securityQuestion);
                } catch (Exception e) {
                    LOG.error("While fetching security question for {}", username.getModelObject(), e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        });
        form.add(username);

        final AjaxTextFieldPanel securityAnswer =
                new AjaxTextFieldPanel("securityAnswer", "securityAnswer", new Model<String>());
        securityAnswer.setRequired(true);
        form.add(securityAnswer);

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(SUBMIT, SUBMIT)) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    userSelfRestClient.requestPasswordReset(username.getModelObject(), securityAnswer.getModelObject());
                    window.close(target);
                } catch (Exception e) {
                    LOG.error("While requesting password reset for {}", username.getModelObject(), e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
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
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }
}
