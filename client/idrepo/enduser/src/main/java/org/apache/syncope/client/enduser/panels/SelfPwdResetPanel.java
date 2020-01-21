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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.pages.BaseEnduserWebPage;
import org.apache.syncope.client.enduser.rest.UserSelfRestClient;
import org.apache.syncope.client.enduser.wizards.any.CaptchaPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DomainDropDown;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfPwdResetPanel extends Panel implements IEventSource {

    private static final long serialVersionUID = -2841210052053545578L;

    private static final Logger LOG = LoggerFactory.getLogger(SelfPwdResetPanel.class);

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

    private String usernameText;

    private String securityAnswerText;

    private final TextField<String> securityQuestion;

    private final CaptchaPanel<Void> captcha;

    public SelfPwdResetPanel(final String id, final PageReference pageRef) {
        super(id);

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
        add(domainSelect);

        TextField<String> username =
                new TextField<>("username", new PropertyModel<>(this, "usernameText"), String.class);
        username.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                loadSecurityQuestion(pageRef, target);
            }
        });
        username.setRequired(true);
        add(username);

        securityQuestion =
                new TextField<>("securityQuestion", new PropertyModel<>(Model.of(), "content"), String.class);
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

        TextField<String> securityAnswer =
                new TextField<>("securityAnswer", new PropertyModel<>(this, "securityAnswerText"), String.class);
        securityAnswer.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // do nothing
            }
        });
        securityAnswer.setRequired(true);
        add(securityAnswer);

        captcha = new CaptchaPanel<>("captchaPanel");
        captcha.setOutputMarkupPlaceholderTag(true);
        captcha.setVisible(SyncopeWebApplication.get().isCaptchaEnabled());
        add(captcha);

        AjaxButton submitButton = new AjaxButton("submit") {

            private static final long serialVersionUID = 4284361595033427185L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                boolean checked = true;
                if (SyncopeWebApplication.get().isCaptchaEnabled()) {
                    checked = captcha.captchaCheck();
                }
                if (!checked) {
                    SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
                    ((BaseEnduserWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
                } else {
                    try {
                        UserSelfRestClient.requestPasswordReset(usernameText, securityAnswerText);
                        PageParameters parameters = new PageParameters();
                        parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("self.pwd.reset.success"));
                        setResponsePage(getApplication().getHomePage(), parameters);
                    } catch (SyncopeClientException sce) {
                        LOG.error("Unable to reset password of [{}]", usernameText, sce);
                        SyncopeEnduserSession.get().onException(sce);
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
                setResponsePage(getApplication().getHomePage());
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
            SyncopeEnduserSession.get().onException(e);
            ((BaseEnduserWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
