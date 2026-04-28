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
package org.apache.syncope.client.ui.commons.pages;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.client.ui.commons.rest.AnonymousRestClient;
import org.apache.syncope.client.ui.commons.wizards.SyncopeWizardButtonBar;
import org.apache.syncope.common.lib.types.Mfa;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardButton;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.kendo.ui.widget.notification.Notification;

public abstract class BaseMfaEnroll extends WebPage {

    private static final long serialVersionUID = 7727175131303606541L;

    protected static final Logger LOG = LoggerFactory.getLogger(BaseMfaEnroll.class);

    protected class QR extends WizardStep {

        private static final long serialVersionUID = 4500944566145320340L;

        protected QR(final Mfa mfa) {
            super(new ResourceModel("qr.title"), Model.of());

            add(new Image("qrCode", new UrlResourceReference(Url.parse(mfa.dataUri()))) {

                private static final long serialVersionUID = 5324383636091304067L;

                @Override
                protected boolean shouldAddAntiCacheParameter() {
                    return false;
                }
            });

            AjaxTextFieldPanel otp = new AjaxTextFieldPanel("otp", "otp", Model.of(), true);
            add(otp.hideLabel().setRequired(true));
            otp.getField().add(new IValidator<String>() {

                private static final long serialVersionUID = 3978328825079032964L;

                @Override
                public void validate(final IValidatable<String> validatable) {
                    if (!anonymousRestClient.mfaCheck(mfa.secret(), otp.getField().getRawInput())) {
                        validatable.error(new ValidationError(getString("incorrect.otp")));
                    }
                }
            });
        }
    }

    protected static class RecoveryCodes extends WizardStep {

        private static final long serialVersionUID = 4500944566145320340L;

        protected RecoveryCodes(final List<String> recoveryCodes) {
            super(new ResourceModel("recoveryCodes.title"), Model.of());
            add(new Label("recoveryCodes", Model.of(recoveryCodes.stream().collect(Collectors.joining("\n")))));
        }
    }

    @SpringBean
    protected AnonymousRestClient anonymousRestClient;

    public BaseMfaEnroll(final PageParameters parameters) {
        super(parameters);
        setStatelessHint(true);

        Mfa mfa;
        try {
            mfa = getBaseSession().generateMfa();
        } catch (IllegalStateException e) {
            PageParameters params = new PageParameters();
            params.add(Constants.NOTIFICATION_MSG_PARAM, getString("mfa.enroll.error"));
            params.add(Constants.NOTIFICATION_LEVEL_PARAM, Notification.ERROR);
            throw new RestartResponseException(getLoginPage(), params);
        }

        WizardModel wizardModel = new WizardModel();
        wizardModel.add(new QR(mfa));
        wizardModel.add(new RecoveryCodes(mfa.recoveryCodes()));
        Wizard wizard = new Wizard("mfa", wizardModel) {

            private static final long serialVersionUID = -8826151743631312577L;

            protected NotificationPanel notificationPanel;

            @Override
            protected Component newFeedbackPanel(final String id) {
                notificationPanel = new NotificationPanel(id);
                return notificationPanel;
            }

            @Override
            protected Component newButtonBar(final String id) {
                return new SyncopeWizardButtonBar(id, this) {

                    private static final long serialVersionUID = 5372047072232798839L;

                    @Override
                    protected AjaxFormSubmitBehavior newAjaxFormSubmitBehavior(final WizardButton button) {
                        return new SyncopeWizardAjaxFormSubmitBehavior(button) {

                            private static final long serialVersionUID = 1044283891209419681L;

                            @Override
                            protected NotificationPanel getNotificationPanel() {
                                return notificationPanel;
                            }
                        };
                    }
                };
            }

            @Override
            public void onCancel() {
                PageParameters params = new PageParameters();
                params.add(Constants.NOTIFICATION_MSG_PARAM, getString("mfa.enroll.aborted"));
                params.add(Constants.NOTIFICATION_LEVEL_PARAM, Notification.WARNING);
                throw new RestartResponseException(getLoginPage(), params);
            }

            @Override
            public void onFinish() {
                try {
                    getBaseSession().enrollMfa(mfa);
                } catch (Exception e) {
                    LOG.error("While enrolling MFA", e);

                    PageParameters params = new PageParameters();
                    params.add(Constants.NOTIFICATION_MSG_PARAM, getString("mfa.enroll.error"));
                    params.add(Constants.NOTIFICATION_LEVEL_PARAM, Notification.ERROR);
                    throw new RestartResponseException(getLoginPage(), params);
                }

                PageParameters params = new PageParameters();
                params.add(Constants.NOTIFICATION_MSG_PARAM, getString("mfa.enroll.success"));
                params.add(Constants.NOTIFICATION_LEVEL_PARAM, Notification.INFO);
                setResponsePage(getLoginPage(), params);
            }
        };
        add(wizard);
    }

    protected abstract BaseSession getBaseSession();

    protected abstract Class<? extends BaseLogin> getLoginPage();
}
