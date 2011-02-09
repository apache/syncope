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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.SyncopeSession;

/**
 * Syncope Login page.
 */
public class Login extends WebPage {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            Login.class);

    @SpringBean
    private RestTemplate restTemplate;

    @SpringBean(name = "baseURL")
    private String baseURL;

    private Form form;

    private TextField usernameField;

    private TextField passwordField;

    private DropDownChoice<Locale> languageSelect;

    public Login(final PageParameters parameters) {
        super(parameters);

        form = new Form("login");

        usernameField = new TextField("username", new Model());
        usernameField.setMarkupId("username");
        form.add(usernameField);

        passwordField = new PasswordTextField("password", new Model());
        passwordField.setMarkupId("password");
        form.add(passwordField);

        languageSelect = new LocaleDropDown("language", Arrays.asList(
                new Locale[]{Locale.ENGLISH, Locale.ITALIAN}));

        form.add(languageSelect);

        Button submitButton = new Button("submit", new Model(
                getString("submit"))) {

            @Override
            public void onSubmit() {
                String[] entitlements = authenticate(
                        usernameField.getRawInput(),
                        passwordField.getRawInput());

                if (entitlements == null || entitlements.length == 0) {
                    LOG.error("No entitlements found for "
                            + usernameField.getRawInput());
                    getSession().error(getString("login-error"));
                } else {
                    SyncopeSession.get().setUsername(
                            usernameField.getRawInput());
                    SyncopeSession.get().setEntitlements(
                            entitlements);

                    setResponsePage(WelcomePage.class, parameters);
                }
            }
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

        add(form);
        add(new FeedbackPanel("feedback"));
    }

    public String[] authenticate(final String username, final String password) {
        //1.Set provided credentials to check
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

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

    /**
     * Inner class which implements (custom) Locale DropDownChoice component.
     */
    public class LocaleDropDown extends DropDownChoice<Locale> {

        private class LocaleRenderer extends ChoiceRenderer<Locale> {

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

            //Set default value to English
            getModel().setObject(Locale.ENGLISH);
        }

        @Override
        protected boolean wantOnSelectionChangedNotifications() {
            return true;
        }
    }
}
