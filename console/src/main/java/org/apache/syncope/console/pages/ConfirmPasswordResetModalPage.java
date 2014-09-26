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

import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class ConfirmPasswordResetModalPage extends BaseModalPage {

    private static final long serialVersionUID = -8419445804421211904L;

    public ConfirmPasswordResetModalPage(final ModalWindow window, final String token) {
        super();
        setOutputMarkupId(true);

        final StatelessForm<?> form = new StatelessForm<Object>(FORM);
        form.setOutputMarkupId(true);

        final FieldPanel<String> password =
                new AjaxPasswordFieldPanel("password", "password", new Model<String>()).setRequired(true);
        ((PasswordTextField) password.getField()).setResetPassword(true);
        form.add(password);

        final FieldPanel<String> confirmPassword =
                new AjaxPasswordFieldPanel("confirmPassword", "confirmPassword", new Model<String>());
        ((PasswordTextField) confirmPassword.getField()).setResetPassword(true);
        form.add(confirmPassword);

        form.add(new EqualPasswordInputValidator(password.getField(), confirmPassword.getField()));

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new ResourceModel(SUBMIT, SUBMIT)) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    userSelfRestClient.confirmPasswordReset(token, password.getModelObject());

                    setResponsePage(new ResultStatusModalPage.Builder(window, new UserTO()).
                            mode(UserModalPage.Mode.SELF).build());
                } catch (Exception e) {
                    LOG.error("While confirming password reset for {}", token, e);
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
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        add(form);
    }
}
