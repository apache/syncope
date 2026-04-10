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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.panels.captcha.CaptchaPanel;
import org.apache.syncope.client.enduser.rest.MfaRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.panels.CardPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class DismissMfa extends BaseReauthPage {

    private static final long serialVersionUID = -7594699771879725510L;

    private static final String DISMISS_MFA = "page.dismissmfa";

    @SpringBean
    protected MfaRestClient mfaRestClient;

    public DismissMfa(final PageParameters pageParameters) {
        super(pageParameters, DISMISS_MFA);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);

        StatelessForm<Void> form = new StatelessForm<>("dismissmfa");
        content.add(form);

        AjaxCheckBoxPanel consensus = new AjaxCheckBoxPanel("consensus", "consensus", Model.of());
        form.add(consensus.setRequired(true));

        CaptchaPanel<Void> captcha = new CaptchaPanel<>(EnduserConstants.CONTENT_PANEL);
        captcha.setOutputMarkupPlaceholderTag(true);
        form.add(new CardPanel.Builder<CaptchaPanel<Void>>().
                setName("captcha").
                setComponent(captcha).
                isVisible(SyncopeWebApplication.get().isCaptchaEnabled()).build("captchaPanelCard"));

        AjaxButton submitButton = new AjaxButton("submit", new Model<>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                if (BooleanUtils.isNotTrue(consensus.getModelObject())) {
                    SyncopeEnduserSession.get().warn(getString("dismiss.mfa.consensus.required"));
                    getNotificationPanel().refresh(target);
                    return;
                }

                if (SyncopeWebApplication.get().isCaptchaEnabled() ? captcha.check() : true) {
                    try {
                        mfaRestClient.dismiss();

                        SyncopeEnduserSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        SyncopeEnduserSession.get().invalidate();

                        PageParameters parameters = new PageParameters();
                        parameters.add(EnduserConstants.STATUS, Constants.OPERATION_SUCCEEDED);
                        parameters.add(Constants.NOTIFICATION_MSG_PARAM, getString("dismiss.mfa.success"));
                        parameters.add(
                                EnduserConstants.LANDING_PAGE,
                                SyncopeWebApplication.get().getPageClass("profile", Dashboard.class).getName());
                        setResponsePage(getApplication().getHomePage(), parameters);
                    } catch (Exception e) {
                        LOG.error("While dismissing MFA for {}",
                                SyncopeEnduserSession.get().getSelfTO().getUsername(), e);
                        SyncopeEnduserSession.get().onException(e);
                        getNotificationPanel().refresh(target);
                    }
                } else {
                    SyncopeEnduserSession.get().error(getString(Constants.CAPTCHA_ERROR));
                    getNotificationPanel().refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                getNotificationPanel().refresh(target);
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
