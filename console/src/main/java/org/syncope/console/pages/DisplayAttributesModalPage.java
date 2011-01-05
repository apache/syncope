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
import org.syncope.console.commons.Constants;
import org.syncope.console.rest.ConfigurationRestClient;
import org.syncope.console.rest.SchemaRestClient;

/**
 * Modal window with Display attributes form.
 */
public class DisplayAttributesModalPage extends SyncopeModalPage {

    @SpringBean
    private ConfigurationRestClient restClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    private List<String> selections;

    private ConfigurationTO configuration;

    public AjaxButton submit;

    public DisplayAttributesModalPage(final BasePage basePage,
            final ModalWindow window, final boolean createFlag) {

        Form userAttributesForm = new Form("UserAttributesForm");
        userAttributesForm.setModel(new CompoundPropertyModel(this));
        setupSelections();

        final IModel attributes = new LoadableDetachableModel() {

            @Override
            protected Object load() {
                SyncopeApplication app = (SyncopeApplication) Application.get();

                return schemaRestClient.getAllUserSchemasNames();
            }
        };

        userAttributesForm.add(new CheckBoxMultipleChoice("usersSchemasList",
                new PropertyModel(this, "selections"), attributes));

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                if (saveConfiguration()) {

                    Users callerPage = (Users) basePage;
                    callerPage.setOperationResult(true);
                    window.close(target);

                } else {
                    error(getString("generic_error"));
                }

            }
        };
        userAttributesForm.add(submit);
        add(userAttributesForm);
    }

    /**
     * Setup user selections.
     * @return selections' names.
     */
    public final void setupSelections() {
        selections = new ArrayList<String>();

        configuration = restClient.readConfiguration(
                Constants.CONF_USERS_ATTRIBUTES_VIEW);

        if (configuration != null && configuration.getConfValue() != null) {
            String conf = configuration.getConfValue();
            StringTokenizer st = new StringTokenizer(conf, ";");

            while (st.hasMoreTokens()) {
                this.selections.add(st.nextToken());
            }
        }
    }

    /**
     * Store the selected selections into db.
     */
    public boolean saveConfiguration() {
        boolean create = (configuration == null
                || configuration.getConfValue() == null) ? true : false;

        configuration = new ConfigurationTO();

        StringBuilder value = new StringBuilder();
        for (String name : selections) {
            value.append(name).append(';');
        }

        configuration.setConfKey(Constants.CONF_USERS_ATTRIBUTES_VIEW);
        configuration.setConfValue(value.toString());

        return create
                ? restClient.createConfiguration(configuration)
                : restClient.updateConfiguration(configuration);
    }
}
