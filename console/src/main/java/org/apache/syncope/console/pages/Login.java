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

import java.security.AccessControlException;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.wrap.EntitlementTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.pages.panels.NotificationPanel;
import org.apache.syncope.console.rest.UserSelfRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.markup.html.form.LinkPanel;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Syncope Login page.
 */
public class Login extends WebPage {

    private static final long serialVersionUID = -3744389270366566218L;

    private final static int SELF_REG_WIN_HEIGHT = 550;

    private final static int SELF_REG_WIN_WIDTH = 800;

    private final static int PWD_RESET_WIN_HEIGHT = 300;

    private final static int PWD_RESET_WIN_WIDTH = 800;

    @SpringBean(name = "version")
    private String version;

    @SpringBean(name = "anonymousUser")
    private String anonymousUser;

    @SpringBean(name = "anonymousKey")
    private String anonymousKey;

    @SpringBean
    private UserSelfRestClient userSelfRestClient;

    private final StatelessForm<Void> form;

    private final TextField<String> userIdField;

    private final TextField<String> passwordField;

    private final DropDownChoice<Locale> languageSelect;

    private final NotificationPanel feedbackPanel;

    public Login(final PageParameters parameters) {
        super(parameters);
        setStatelessHint(true);

        feedbackPanel = new NotificationPanel(Constants.FEEDBACK);
        add(feedbackPanel);

        form = new StatelessForm<Void>("login");

        userIdField = new TextField<String>("userId", new Model<String>());
        userIdField.setMarkupId("userId");
        form.add(userIdField);

        passwordField = new PasswordTextField("password", new Model<String>());
        passwordField.setMarkupId("password");
        form.add(passwordField);

        languageSelect = new LocaleDropDown("language");

        form.add(languageSelect);

        AjaxButton submitButton = new AjaxButton("submit", new Model<String>(getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    if (anonymousUser.equals(userIdField.getRawInput())) {
                        throw new AccessControlException("Illegal username");
                    }

                    authenticate(userIdField.getRawInput(), passwordField.getRawInput());

                    setResponsePage(WelcomePage.class, parameters);
                } catch (AccessControlException e) {
                    error(getString("login-error"));
                    feedbackPanel.refresh(target);
                    SyncopeSession.get().resetClients();
                }
            }
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

        add(form);

        // Modal window for self registration
        final ModalWindow selfRegModalWin = new ModalWindow("selfRegModal");
        selfRegModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        selfRegModalWin.setInitialHeight(SELF_REG_WIN_HEIGHT);
        selfRegModalWin.setInitialWidth(SELF_REG_WIN_WIDTH);
        selfRegModalWin.setCookieName("self-reg-modal");
        add(selfRegModalWin);

        Fragment selfRegFrag;
        if (userSelfRestClient.isSelfRegistrationAllowed()) {
            selfRegFrag = new Fragment("selfRegistration", "selfRegAllowed", this);

            final AjaxLink<Void> selfRegLink = new ClearIndicatingAjaxLink<Void>("link", getPageReference()) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                protected void onClickInternal(final AjaxRequestTarget target) {
                    selfRegModalWin.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            // anonymous authentication needed for self-registration
                            authenticate(anonymousUser, anonymousKey);

                            return new UserSelfModalPage(
                                    Login.this.getPageReference(), selfRegModalWin, new UserTO());
                        }
                    });

                    selfRegModalWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                        private static final long serialVersionUID = 251794406325329768L;

                        @Override
                        public void onClose(final AjaxRequestTarget target) {
                            SyncopeSession.get().invalidate();
                        }
                    });

                    selfRegModalWin.show(target);
                }
            };
            selfRegLink.add(new Label("linkTitle", getString("selfRegistration")));

            Panel panel = new LinkPanel("selfRegistration", new ResourceModel("selfRegistration"));
            panel.add(selfRegLink);
            selfRegFrag.add(panel);
        } else {
            selfRegFrag = new Fragment("selfRegistration", "selfRegNotAllowed", this);
        }
        add(selfRegFrag);

        // Modal window for password reset request
        final ModalWindow pwdResetReqModalWin = new ModalWindow("pwdResetReqModal");
        pwdResetReqModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        pwdResetReqModalWin.setInitialHeight(PWD_RESET_WIN_HEIGHT);
        pwdResetReqModalWin.setInitialWidth(PWD_RESET_WIN_WIDTH);
        pwdResetReqModalWin.setCookieName("pwd-reset-req-modal");
        add(pwdResetReqModalWin);

        Fragment pwdResetFrag;
        if (userSelfRestClient.isPasswordResetAllowed()) {
            pwdResetFrag = new Fragment("passwordReset", "pwdResetAllowed", this);

            final AjaxLink<Void> pwdResetLink = new ClearIndicatingAjaxLink<Void>("link", getPageReference()) {

                private static final long serialVersionUID = -6957616042924610290L;

                @Override
                protected void onClickInternal(final AjaxRequestTarget target) {
                    pwdResetReqModalWin.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            // anonymous authentication needed for password reset request
                            authenticate(anonymousUser, anonymousKey);

                            return new RequestPasswordResetModalPage(pwdResetReqModalWin);
                        }
                    });

                    pwdResetReqModalWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                        private static final long serialVersionUID = 8804221891699487139L;

                        @Override
                        public void onClose(final AjaxRequestTarget target) {
                            SyncopeSession.get().invalidate();
                            setResponsePage(Login.class);
                        }
                    });

                    pwdResetReqModalWin.show(target);
                }
            };
            pwdResetLink.add(new Label("linkTitle", getString("passwordReset")));

            Panel panel = new LinkPanel("passwordReset", new ResourceModel("passwordReset"));
            panel.add(pwdResetLink);
            pwdResetFrag.add(panel);
        } else {
            pwdResetFrag = new Fragment("passwordReset", "pwdResetNotAllowed", this);
        }
        add(pwdResetFrag);

        // Modal window for password reset confirm - automatically shown when token is available as request parameter
        final String pwdResetToken = RequestCycle.get().getRequest().getRequestParameters().
                getParameterValue(Constants.PARAM_PASSWORD_RESET_TOKEN).toOptionalString();
        final ModalWindow pwdResetConfModalWin = new ModalWindow("pwdResetConfModal");
        if (StringUtils.isNotBlank(pwdResetToken)) {
            pwdResetConfModalWin.add(new AbstractDefaultAjaxBehavior() {

                private static final long serialVersionUID = 3109256773218160485L;

                @Override
                protected void respond(final AjaxRequestTarget target) {
                    ModalWindow window = (ModalWindow) getComponent();
                    window.show(target);
                }

                @Override
                public void renderHead(final Component component, final IHeaderResponse response) {
                    response.render(JavaScriptHeaderItem.forScript(getCallbackScript(), null));
                }
            });
        }
        pwdResetConfModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        pwdResetConfModalWin.setInitialHeight(PWD_RESET_WIN_HEIGHT);
        pwdResetConfModalWin.setInitialWidth(PWD_RESET_WIN_WIDTH);
        pwdResetConfModalWin.setCookieName("pwd-reset-conf-modal");
        pwdResetConfModalWin.setPageCreator(new ModalWindow.PageCreator() {

            private static final long serialVersionUID = -7834632442532690940L;

            @Override
            public Page createPage() {
                // anonymous authentication needed for password reset confirm
                authenticate(anonymousUser, anonymousKey);

                return new ConfirmPasswordResetModalPage(pwdResetConfModalWin, pwdResetToken);
            }
        });
        pwdResetConfModalWin.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                SyncopeSession.get().invalidate();
                setResponsePage(Login.class);
            }
        });
        add(pwdResetConfModalWin);
    }

    private void authenticate(final String username, final String password) {
        List<EntitlementTO> entitlements = SyncopeSession.get().
                getService(EntitlementService.class, username, password).getOwnEntitlements();

        SyncopeSession.get().setUsername(username);
        SyncopeSession.get().setPassword(password);
        SyncopeSession.get().setEntitlements(CollectionWrapper.unwrap(entitlements).toArray(new String[0]));
        SyncopeSession.get().setVersion(version);
    }

    /**
     * Inner class which implements (custom) Locale DropDownChoice component.
     */
    private class LocaleDropDown extends DropDownChoice<Locale> {

        private static final long serialVersionUID = 2349382679992357202L;

        private class LocaleRenderer extends ChoiceRenderer<Locale> {

            private static final long serialVersionUID = -3657529581555164741L;

            @Override
            public String getDisplayValue(final Locale locale) {
                return locale.getDisplayName(getLocale());
            }
        }

        public LocaleDropDown(final String id) {
            super(id, SyncopeSession.SUPPORTED_LOCALES);

            setChoiceRenderer(new LocaleRenderer());
            setModel(new IModel<Locale>() {

                private static final long serialVersionUID = -6985170095629312963L;

                @Override
                public Locale getObject() {
                    return getSession().getLocale();
                }

                @Override
                public void setObject(final Locale object) {
                    getSession().setLocale(object);
                }

                @Override
                public void detach() {
                    // Empty.
                }
            });

            // set default value to English
            getModel().setObject(Locale.ENGLISH);
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications() {
            return true;
        }
    }
}
