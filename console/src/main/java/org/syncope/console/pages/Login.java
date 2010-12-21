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

import java.io.InputStream;
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
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.syncope.console.SyncopeSession;
import org.syncope.console.SyncopeUser;
import org.syncope.console.rest.RestClient;

/**
 * Syncope Login page.
 */
public class Login extends WebPage {

    public Form form;
    public TextField usernameField;
    public TextField passwordField;
    public DropDownChoice<String> languageSelect;
    public InputStream inputStream;

    @SpringBean(name = "restClient")
    protected RestClient restClient;

    public Login(PageParameters parameters) {
        super(parameters);
        form = new Form("login");

        usernameField = new TextField("username", new Model());
        usernameField.setMarkupId("username");
        form.add(usernameField);

        passwordField = new PasswordTextField("password", new Model());
        passwordField.setMarkupId("password");
        form.add(passwordField);

        languageSelect = new LocaleDropDown("language",Arrays.asList
                (new Locale[]{Locale.ENGLISH, Locale.ITALIAN}));
        
        form.add(languageSelect);

        Button submitButton = new Button("submit", new Model(
                getString("submit"))) {

            @Override
            public void onSubmit() {
               SyncopeUser user = authenticate(usernameField.getRawInput(),
                       passwordField.getRawInput());

               if(user != null) {
                ((SyncopeSession)Session.get()).setUser(user);
                setResponsePage(new WelcomePage(null));
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
     * @param username
     * @param password
     * @return SyncopeUser object if the authorization succedes, null value
     *  otherwise.
     */
    public SyncopeUser authenticate(String username, String password) {

        SyncopeUser user = null;
        String roles = "";

        //1.Set provided credentials to check
        ((CommonsClientHttpRequestFactory) restClient.getRestTemplate()
                .getRequestFactory()).getHttpClient().getState()
                .setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        //2.Search authorizations for user specified by credentials
        List<String> auths;

        try {
        auths = Arrays.asList(
                restClient.getRestTemplate().getForObject(
                restClient.getBaseURL()+ "auth/entitlements.json",
                String[].class));
        }
        catch(HttpClientErrorException e){
            //Reset the credentials if exception occurs
            ((CommonsClientHttpRequestFactory) restClient.getRestTemplate()
                .getRequestFactory()).getHttpClient().getState()
                .setCredentials(AuthScope.ANY,null);
            getSession().error(e.getMessage());
            return null;
        }

        if (auths != null && auths.size() > 0) {

            for(int i = 0; i< auths.size(); i++) {
                String role = auths.get(i);
                roles +=role;

                if(i != auths.size())
                    roles += ",";
            }

        user = new SyncopeUser(username, roles);

        return user;
        }
        else {
           //Reset the credentials if no auth exist for the specified user
            ((CommonsClientHttpRequestFactory) restClient.getRestTemplate()
                .getRequestFactory()).getHttpClient().getState()
                .setCredentials(AuthScope.ANY,null);
           getSession().error(getString("login-error"));
           return null;
        }
    }

    /**
     * Getter for restClient attribute.
     * @return RestClient instance
     */
    public RestClient getRestClient() {
        return restClient;
    }

    /**
     * Setter for restClient attribute.
     * @param restClient instance
     */
    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
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
