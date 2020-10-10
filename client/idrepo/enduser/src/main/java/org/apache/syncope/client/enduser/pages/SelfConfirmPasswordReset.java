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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DomainDropDown;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SelfConfirmPasswordReset extends BaseEnduserWebPage {

    private static final long serialVersionUID = -2166782304542750726L;

    @SpringBean
    private DomainOps domainOps;

    private final LoadableDetachableModel<List<String>> domains = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 4659376149825914247L;

        @Override
        protected List<String> load() {
            List<String> current = new ArrayList<>();
            current.addAll(domainOps.list().stream().map(Domain::getKey).sorted().collect(Collectors.toList()));
            current.add(0, SyncopeConstants.MASTER_DOMAIN);
            return current;
        }
    };

    public SelfConfirmPasswordReset(final PageParameters parameters) {
        super(parameters);

        if (parameters.get("token").isEmpty()) {
            LOG.debug("No token parameter found in the request url");
            parameters.add("errorMessage", getString("self.confirm.pwd.reset.error.empty"));
            setResponsePage(getApplication().getHomePage(), parameters);
        }

        navbar.setEnabled(false);
        navbar.setVisible(false);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        body.add(content);

        Form<?> form = new StatelessForm<>("selfConfirmPwdResetForm");
        form.setOutputMarkupId(true);
        content.add(form);

        DomainDropDown domainSelect = new DomainDropDown("domain", domains);
        domainSelect.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // nothing to do
            }
        }).add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // nothing to do
            }
        });
        form.add(domainSelect);

        AjaxPasswordFieldPanel passwordField = new AjaxPasswordFieldPanel(
                "password", getString("password"), new Model<>());
        passwordField.setRequired(true);
        passwordField.setMarkupId("password");
        passwordField.setPlaceholder(getString("password"));
        ((PasswordTextField) passwordField.getField()).setResetPassword(false);
        form.add(passwordField);

        FieldPanel<String> confirmPasswordField = new AjaxPasswordFieldPanel(
                "confirmPassword", getString("confirm-password"), new Model<>());
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setMarkupId("confirmPassword");
        confirmPasswordField.setPlaceholder(getString("confirm-password"));
        ((PasswordTextField) confirmPasswordField.getField()).setResetPassword(false);
        form.add(confirmPasswordField);

        form.add(new EqualPasswordInputValidator(passwordField.getField(), confirmPasswordField.getField()));

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 509325877101838812L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                try {
                    SyncopeEnduserSession.get().getService(UserSelfService.class).confirmPasswordReset(
                            parameters.get("token").toString(), passwordField.getDefaultModelObjectAsString());
                    PageParameters parameters = new PageParameters();
                    parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.confirm.pwd.reset.success"));
                    setResponsePage(getApplication().getHomePage(), parameters);
                } catch (SyncopeClientException sce) {
                    LOG.error("Unable to complete the 'Password Reset Confirmation' process", sce);
                    SyncopeEnduserSession.get().onException(sce);
                    ((BaseEnduserWebPage) getPageReference().getPage()).getNotificationPanel().refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                notificationPanel.refresh(target);
            }
        };
        form.setDefaultButton(submitButton);
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
}
