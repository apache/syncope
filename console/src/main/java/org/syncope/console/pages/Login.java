/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.SyncopeSession;
import org.syncope.console.wicket.markup.html.form.LinkPanel;

/**
 * Syncope Login page.
 */
public class Login extends WebPage {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            Login.class);

    private static final long serialVersionUID = -3744389270366566218L;

    private final static int SELF_REG_WIN_HEIGHT = 550;

    private final static int SELF_REG_WIN_WIDTH = 800;

    @SpringBean
    private RestTemplate restTemplate;

    @SpringBean
    private HttpClient httpClient;

    @SpringBean(name = "baseURL")
    private String baseURL;

    private Form form;

    private TextField userIdField;

    private TextField passwordField;

    private DropDownChoice<Locale> languageSelect;

    public Login(final PageParameters parameters) {
        super(parameters);

        form = new Form("login");

        userIdField = new TextField("userId", new Model());
        userIdField.setMarkupId("userId");
        form.add(userIdField);

        passwordField = new PasswordTextField("password", new Model());
        passwordField.setMarkupId("password");
        form.add(passwordField);

        languageSelect = new LocaleDropDown("language", Arrays.asList(
                new Locale[]{Locale.ENGLISH, Locale.ITALIAN}));

        form.add(languageSelect);

        Button submitButton = new Button("submit", new Model(
                getString("submit"))) {

            private static final long serialVersionUID = 429178684321093953L;

            @Override
            public void onSubmit() {
                String[] entitlements = authenticate(
                        userIdField.getRawInput(),
                        passwordField.getRawInput());

                SyncopeSession.get().setUserId(
                        userIdField.getRawInput());
                SyncopeSession.get().setEntitlements(
                        entitlements);
                SyncopeSession.get().setCoreVersion(getCoreVersion());

                setResponsePage(WelcomePage.class, parameters);
            }
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

        add(form);
        add(new FeedbackPanel("feedback"));

        // Modal window for self registration
        final ModalWindow editProfileModalWin =
                new ModalWindow("selfRegModal");
        editProfileModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editProfileModalWin.setInitialHeight(SELF_REG_WIN_HEIGHT);
        editProfileModalWin.setInitialWidth(SELF_REG_WIN_WIDTH);
        editProfileModalWin.setCookieName("self-reg-modal");
        add(editProfileModalWin);

        Fragment selfRegFrag;
        if (restTemplate.getForObject(
                baseURL + "user/request/create/allowed",
                Boolean.class)) {

            selfRegFrag =
                    new Fragment("selfRegistration", "selfRegAllowed", this);

            AjaxLink selfRegLink =
                    new IndicatingAjaxLink("link") {

                        private static final long serialVersionUID =
                                -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            editProfileModalWin.setPageCreator(
                                    new ModalWindow.PageCreator() {

                                        @Override
                                        public Page createPage() {
                                            return new UserModalPage(
                                                    Login.this.getPageReference(),
                                                    editProfileModalWin,
                                                    new UserTO(),
                                                    UserModalPage.Mode.SELF);
                                        }
                                    });

                            editProfileModalWin.show(target);
                        }
                    };
            selfRegLink.add(
                    new Label("linkTitle", getString("selfRegistration")));

            Panel panel = new LinkPanel("selfRegistration",
                    new ResourceModel("selfRegistration"));
            panel.add(selfRegLink);
            selfRegFrag.add(panel);
        } else {
            selfRegFrag =
                    new Fragment("selfRegistration", "selfRegNotAllowed", this);
        }
        add(selfRegFrag);
    }

    private String[] authenticate(final String userId, final String password) {
        //1.Set provided credentials to check
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(userId, password));

        //2.Search authorizations for user specified by credentials
        String[] entitlements = null;
        try {
            entitlements = restTemplate.getForObject(
                    baseURL + "auth/entitlements.json", String[].class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While fetching user's entitlements", e);
            getSession().error(e.getMessage());
        }

        return entitlements;
    }

    private String getCoreVersion() {
        String version = "";
        try {
            HttpMethod get = new GetMethod(baseURL + "../version.jsp");
            httpClient.executeMethod(get);
            version = get.getResponseBodyAsString().trim();
        } catch (IOException e) {
            LOG.error("While fetching core version", e);
            getSession().error(e.getMessage());
        }

        return version;
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

        public LocaleDropDown(final String id,
                final List<Locale> supportedLocales) {

            super(id, supportedLocales);

            setChoiceRenderer(new LocaleRenderer());
            setModel(new IModel<Locale>() {

                @Override
                public Locale getObject() {
                    return getSession().getLocale();
                }

                @Override
                public void setObject(Locale object) {
                    getSession().setLocale(object);
                }

                @Override
                public void detach() {
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
