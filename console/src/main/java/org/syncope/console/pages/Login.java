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
import org.apache.wicket.Session;
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
import org.syncope.console.SyncopeUser;

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

    private DropDownChoice<String> languageSelect;

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
                SyncopeUser user = authenticate(usernameField.getRawInput(),
                        passwordField.getRawInput());

                if (user != null) {
                    ((SyncopeSession) Session.get()).setUser(user);
                    setResponsePage(WelcomePage.class, parameters);
                }
            }
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);

        add(form);
        add(new FeedbackPanel("feedback"));
    }

    /**
     * Authenticate the user.
     *
     * @param username provided
     * @param password provided
     * @return SyncopeUser object if the authorization succedes, null otherwise.
     */
    public SyncopeUser authenticate(final String username,
            final String password) {

        SyncopeUser user = null;

        //1.Set provided credentials to check
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        //2.Search authorizations for user specified by credentials
        List<String> entitlements = null;
        try {
            entitlements = Arrays.asList(
                    restTemplate.getForObject(
                    baseURL + "auth/entitlements.json", String[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While fetching user's entitlements", e);
            getSession().error(e.getMessage());
        }

        if (entitlements != null && entitlements.size() > 0) {
            StringBuilder roles = new StringBuilder();

            for (int i = 0; i < entitlements.size(); i++) {
                roles.append(entitlements.get(i));
                if (i != entitlements.size() - 1) {
                    roles.append(",");
                }
            }

            user = new SyncopeUser(username, roles.toString());
        } else {
            LOG.error("No entitlements found found for " + username);
            getSession().error(getString("login-error"));
        }

        return user;
    }

    /**
     * Inner class which implements (custom) Locale DropDownChoice component.
     */
    public class LocaleDropDown extends DropDownChoice {

        private class LocaleRenderer extends ChoiceRenderer {

            @Override
            public String getDisplayValue(Object locale) {
                return ((Locale) locale).getDisplayName(getLocale());
            }
        }

        public LocaleDropDown(String id, List<Locale> supportedLocales) {
            super(id, supportedLocales);
            setChoiceRenderer(new LocaleRenderer());
            setModel(new IModel() {

                @Override
                public Object getObject() {
                    return getSession().getLocale();
                }

                @Override
                public void setObject(Object object) {
                    getSession().setLocale((Locale) object);
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
