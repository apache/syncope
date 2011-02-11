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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnectorBundleTO;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.PropertyTO;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.types.ConnectorCapability;

/**
 * Modal window with Connector form.
 */
public class ConnectorModalPage extends BaseModalPage {

    private TextField connectorName;

    private TextField displayName;

    private DropDownChoice bundle;

    private TextField version;

    private CheckBoxMultipleChoice capabilitiesPalette;

    private List<ConnectorBundleTO> bundlesTOs;

    private ConnectorBundleTO selectedBundleTO = new ConnectorBundleTO();

    private List<PropertyTO> connectorProperties = new ArrayList<PropertyTO>();

    private AjaxButton submit;

    @SpringBean
    private ConnectorRestClient restClient;

    //WebMarkupContainer container;
    private WebMarkupContainer propertiesContainer;

    private List<ConnectorCapability> selections;

    public ConnectorModalPage(final BasePage basePage,
            final ModalWindow window,
            final ConnectorInstanceTO connectorTO,
            final boolean createFlag) {

        Form connectorForm = new Form("ConnectorForm");

        connectorForm.setModel(new CompoundPropertyModel(connectorTO));

        if (!createFlag) {
            setupSelections(connectorTO);
        }

        IModel bundles = new LoadableDetachableModel() {

            protected Object load() {
                return restClient.getAllBundles();
            }
        };

        IModel selectedBundleProperties = new LoadableDetachableModel() {

            protected Object load() {
                List<PropertyTO> list;

                if (createFlag) {
                    connectorTO.setConnectorName(selectedBundleTO.
                            getConnectorName());
                    connectorTO.setVersion(selectedBundleTO.getVersion());

                    list = new ArrayList<PropertyTO>();
                    PropertyTO propertyTO;

                    for (String key : selectedBundleTO.getProperties()) {
                        propertyTO = new PropertyTO();
                        propertyTO.setKey(key);

                        list.add(propertyTO);
                    }
                } else {
                    selectedBundleTO.setBundleName(connectorTO.getBundleName());
                    list = hashSetToList(connectorTO.getConfiguration());
                }
                return list;
            }
        };

        connectorName = new TextField("connectorName");
        connectorName.setEnabled(false);
        connectorName.setOutputMarkupId(true);

        displayName = new TextField("displayName");
        displayName.setOutputMarkupId(true);

        version = new TextField("version");
        version.setEnabled(false);
        version.setOutputMarkupId(true);

        ChoiceRenderer renderer =
                new ChoiceRenderer("bundleName", "bundleName");
        bundle = new DropDownChoice("bundle", bundles, renderer);

        bundle.setModel(new IModel() {

            public Object getObject() {
                return selectedBundleTO;
            }

            public void setObject(Object object) {
                selectedBundleTO = (ConnectorBundleTO) object;
            }

            public void detach() {
            }
        });

        bundle.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                //reset all informations stored in connectorTO

                connectorTO.setConfiguration(new HashSet<PropertyTO>());

                target.addComponent(propertiesContainer);
                target.addComponent(connectorName);
                target.addComponent(version);
            }
        });
        bundle.setRequired(true);
        bundle.setEnabled(createFlag);

        ListView propertiesView = (new ListView("connectorProperties",
                selectedBundleProperties) {

            PropertyTO propertyTO;

            @Override
            protected void populateItem(ListItem item) {
                propertyTO = (PropertyTO) item.getDefaultModelObject();

                item.add(new Label("key", propertyTO.getKey()));
                item.add(
                        new TextField("value", new PropertyModel(propertyTO,
                        "value")).setLabel(
                        new Model<String>(propertyTO.getKey())).setRequired(true));

                connectorTO.getConfiguration().add(propertyTO);
            }
        });

        propertiesContainer = new WebMarkupContainer("propertiesContainer");
        propertiesContainer.setOutputMarkupId(true);
        propertiesContainer.add(propertiesView);

        connectorForm.add(propertiesContainer);

        submit = new IndicatingAjaxButton("submit", new Model(
                getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                ConnectorInstanceTO connector = (ConnectorInstanceTO) form.
                        getDefaultModelObject();

                //Set the model object configuration's properties to connectorPropertiesMap reference
                connector.setBundleName(bundle.getModelValue());
                //Set the model object's capabilites to capabilitiesPalette's converted Set
                connector.setCapabilities(getResourcesSet(selections));

                try {

                    if (createFlag) {
                        restClient.createConnector(connector);
                    } else {
                        restClient.updateConnector(connector);
                    }

                    Connectors callerPage = (Connectors) basePage;
                    callerPage.setOperationResult(true);

                    window.close(target);

                } catch (Exception e) {
                    error(getString("error") + ":" + e.getMessage());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Connectors",
                    "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Connectors",
                    "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                allowedRoles);

        connectorForm.add(connectorName);
        connectorForm.add(displayName);
        connectorForm.add(bundle);
        connectorForm.add(version);

        final IModel capabilities = new LoadableDetachableModel() {

            @Override
            protected Object load() {
                return Arrays.asList(ConnectorCapability.values());
            }
        };

        capabilitiesPalette = new CheckBoxMultipleChoice("capabilitiesPalette",
                new PropertyModel(this, "selections"), capabilities);
        connectorForm.add(capabilitiesPalette);

        connectorForm.add(submit);

        add(connectorForm);
    }

    /**
     * Setup capabilities.
     * @return void
     */
    public void setupSelections(ConnectorInstanceTO connectorTO) {
        selections = new ArrayList<ConnectorCapability>();

        for (ConnectorCapability capability : connectorTO.getCapabilities()) {
            selections.add(capability);
        }

    }

    /**
     * Covert a capabilities List<ConnectorCapability> to Set<ConnectorCapability>.
     * @return Set<ConnectorCapability>
     */
    public Set<ConnectorCapability> getResourcesSet(
            Collection<ConnectorCapability> capabilitiesList) {

        Set<ConnectorCapability> capabilitiesSet =
                new HashSet<ConnectorCapability>();

        for (ConnectorCapability capability : capabilitiesList) {
            capabilitiesSet.add(capability);
        }

        return capabilitiesSet;
    }

    /**
     * Originals : connector's  capabilities
     * @param connectorTO
     * @return List<ConnectorCapability>
     */
    public List<ConnectorCapability> getSelectedCapabilities(
            ConnectorInstanceTO connectorTO) {

        List<ConnectorCapability> capabilities =
                new ArrayList<ConnectorCapability>();

        for (ConnectorCapability capability : connectorTO.getCapabilities()) {
            capabilities.add(capability);
        }

        return capabilities;
    }

    /**
     * Destinations : available capabilities
     * @param connectorTO
     * @return List<ConnectorCapability>
     */
    public List<ConnectorCapability> getAvailableCapabilities() {

        List<ConnectorCapability> capabilities =
                new ArrayList<ConnectorCapability>();

        capabilities = Arrays.asList(ConnectorCapability.values());

        return capabilities;
    }

    /**
     * Convert an HashSet<PropertyTO> object to a corresponding List<PropertyTO>
     * object.
     * @param hashset
     * @return
     */
    public List<PropertyTO> hashSetToList(Set<PropertyTO> set) {
        List<PropertyTO> list = new ArrayList<PropertyTO>();

        for (PropertyTO propertyTO : set) {
            list.add(propertyTO);
        }

        return list;
    }
}
