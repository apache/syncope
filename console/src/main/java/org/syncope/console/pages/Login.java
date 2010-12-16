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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
import org.syncope.console.SyncopeSession;
import org.syncope.console.SyncopeUser;

/**
 * Syncope Login page.
 */
public class Login extends WebPage {

    public Form form;
    public TextField usernameField;
    public TextField passwordField;
    public DropDownChoice<String> languageSelect;
    public InputStream inputStream;

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
               SyncopeUser user = authenticate(usernameField
                       .getRawInput(), passwordField.getRawInput());

               if(user != null) {
                ((SyncopeSession)Session.get()).setUser(user);
                setResponsePage(new WelcomePage(null));
               }
               else
                    error(getString("login-error"));
            }
        };

        submitButton.setDefaultFormProcessing(false);
        form.add(submitButton);
        
        add(form);
        add(new FeedbackPanel("feedback"));
    }

    /**
     *
     * @param username
     * @param password
     * @return
     */
    public SyncopeUser authenticate(String username, String password) {

        SyncopeUser user = null;
        String roles = "";

        if ("admin".equals(username) && "password".equals(password)) {


            List<String> rolesList = getAdminRoles();

            for(int i = 0; i< rolesList.size(); i++) {
                String role = rolesList.get(i);
                roles +=role;

                if(i != rolesList.size())
                    roles += ",";
            }

            user = new SyncopeUser(username, roles);

            return user;
        }
        else  if ("manager".equals(username) && "password".equals(password)) {

            List<String> rolesList = getManagerRoles();

            for (int i = 0; i < rolesList.size(); i++) {
                String role = rolesList.get(i);
                roles += role;

                if (i != rolesList.size())
                    roles += ",";

            }

            user = new SyncopeUser(username, roles);

            return user;
        }
        else
            return null;
    }

    public List<String> getAdminRoles() {
        List<String> roles = new ArrayList<String>();

        roles.add("USER_CREATE");
        roles.add("USER_LIST");
        roles.add("USER_READ");
        roles.add("USER_DELETE");
        roles.add("USER_UPDATE");
        roles.add("USER_VIEW");

        roles.add("SCHEMA_CREATE");
        roles.add("SCHEMA_LIST");
        roles.add("SCHEMA_READ");
        roles.add("SCHEMA_DELETE");
        roles.add("SCHEMA_UPDATE");

        roles.add("ROLE_CREATE");
        roles.add("ROLE_LIST");
        roles.add("ROLE_READ");
        roles.add("ROLE_DELETE");
        roles.add("ROLE_UPDATE");

        roles.add("RESOURCE_CREATE");
        roles.add("RESOURCE_LIST");
        roles.add("RESOURCE_READ");
        roles.add("RESOURCE_DELETE");
        roles.add("RESOURCE_UPDATE");

        roles.add("CONNECTOR_CREATE");
        roles.add("CONNECTOR_LIST");
        roles.add("CONNECTOR_READ");
        roles.add("CONNECTOR_DELETE");
        roles.add("CONNECTOR_UPDATE");

        roles.add("REPORT_LIST");

        roles.add("CONFIGURATION_CREATE");
        roles.add("CONFIGURATION_LIST");
        roles.add("CONFIGURATION_READ");
        roles.add("CONFIGURATION_DELETE");
        roles.add("CONFIGURATION_UPDATE");

        roles.add("TASK_CREATE");
        roles.add("TASK_LIST");
        roles.add("TASK_READ");
        roles.add("TASK_DELETE");
        roles.add("TASK_UPDATE");
        roles.add("TASK_EXECUTE");

        return roles;
    }

    public List<String> getManagerRoles() {
        List<String> roles = new ArrayList<String>();

        //roles.add("USER_CREATE");
        roles.add("USER_LIST");
        roles.add("USER_READ");
        roles.add("USER_DELETE");
//        roles.add("USER_UPDATE");

//        roles.add("SCHEMA_CREATE");
        roles.add("SCHEMA_LIST");
//        roles.add("SCHEMA_READ");
//        roles.add("SCHEMA_DELETE");
//        roles.add("SCHEMA_UPDATE");

         roles.add("CONNECTOR_LIST");
         roles.add("REPORT_LIST");

//        roles.add("ROLE_CREATE");
        roles.add("ROLE_LIST");
        roles.add("ROLE_READ");
//        roles.add("ROLE_DELETE");
//        roles.add("ROLE_UPDATE");
        roles.add("TASK_LIST");

        return roles;
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
