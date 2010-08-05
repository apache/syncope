/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConfigurationTO;

import org.syncope.console.SyncopeApplication;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.rest.UsersRestClient;

/**
 * Modal window with User form.
 */
public class DisplayAttributesModalPage extends SyncopeModalPage {

    @SpringBean(name = "usersRestClient")
    UsersRestClient restClient;
    
    List<String> selections;
    ConfigurationTO configuration;

    public AjaxButton submit;
    
    public DisplayAttributesModalPage(final BasePage basePage, final ModalWindow window,
            final boolean createFlag) {

        Form userAttributesForm = new Form("UserAttributesForm");

        userAttributesForm.setModel(new CompoundPropertyModel(this));

        setupSelections();


        final IModel attributes = new LoadableDetachableModel() {

            @Override
            protected Object load() {
                SchemaRestClient schemaRestClient = (SchemaRestClient)
                        ((SyncopeApplication)Application.get()).getApplicationContext().
                        getBean("schemaRestClient");

                return schemaRestClient.getAllUserSchemasNames();
            }
        };


        userAttributesForm.add(new CheckBoxMultipleChoice("usersSchemasList",
                               new PropertyModel(this,"selections"), attributes));

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                
                boolean res = saveConfiguration();

                if(res)
                    window.close(target);

                else
                    error(getString("generic_error"));
                
            }
        };
        userAttributesForm.add(submit);

        add(userAttributesForm);
    }
    
    /**
     * Setup user selections.
     * @return selections' names.
     */
    public void setupSelections(){

        configuration = restClient.readConfigurationAttributes();

        if(configuration != null) {
            String conf = configuration.getConfValue();
            StringTokenizer st = new StringTokenizer(conf,";");

            selections = new ArrayList<String>();
            
            while(st.hasMoreTokens()) {
                  this.selections.add(st.nextToken());
             }
        }
        else 
            selections = new ArrayList<String>();

    }
    
     /**
     * Store the selected selections into db.
     */
    public boolean saveConfiguration() {

        boolean create = (configuration == null) ? true : false;

        configuration = new ConfigurationTO();

        String value = "";

        for (String name : selections) 
            value += name + ";";
        

        configuration.setConfKey("users.attributes.view");
        configuration.setConfValue(value);

        if (create) {

            if (!restClient.createConfigurationAttributes(configuration))
                return false;
        }

        else  if (!create) {

            if (!restClient.updateConfigurationAttributes(configuration)) 
                return false;
        }

        return true;
    }
}