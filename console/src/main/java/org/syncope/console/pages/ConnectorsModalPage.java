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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnectorBundleTO;
import org.syncope.client.to.ConnectorBundleTOs;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.PropertyTO;
import org.syncope.console.rest.ConnectorsRestClient;

/**
 * Modal window with Connector form.
 */
public class ConnectorsModalPage extends WebPage {

    public TextField connectorName;
    public DropDownChoice bundle;
    public TextField version;

    ConnectorBundleTOs bundlesTOs;
    ConnectorBundleTO selectedBundleTO = new ConnectorBundleTO();
    List<PropertyTO> connectorProperties = new ArrayList<PropertyTO>();

    //Set<PropertyTO> connectorPropertiesMap = new HashSet<PropertyTO>();

    public AjaxButton submit;

    @SpringBean(name = "connectorsRestClient")
    ConnectorsRestClient restClient;

    WebMarkupContainer propertiesContainer;
    //WebMarkupContainer container;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public ConnectorsModalPage(final BasePage basePage, final ModalWindow window,
            final ConnectorInstanceTO connectorTO, final boolean createFlag) {

        Form connectorForm = new Form("ConnectorForm");
        
        connectorForm.setModel(new CompoundPropertyModel(connectorTO));

        IModel bundles =  new LoadableDetachableModel()
        {
            protected Object load() {
                return restClient.getAllBundles().getBundles();
            }
        };

        IModel selectedBundleProperties =  new LoadableDetachableModel()
        {
            protected Object load() {
                List<PropertyTO> list;
                
                if(createFlag) {
                connectorTO.setConnectorName(selectedBundleTO.getConnectorName());
                connectorTO.setVersion(selectedBundleTO.getVersion());

                list = new ArrayList<PropertyTO>();
                PropertyTO propertyTO;

                for(String key : selectedBundleTO.getProperties()) {
                    propertyTO = new PropertyTO();
                    propertyTO.setKey(key);

                    list.add(propertyTO);
                }
                }
                else {
                    selectedBundleTO.setBundleName(connectorTO.getBundleName());
                    list = hashSetToList(connectorTO.getConfiguration());
                }
                return list;
            }
        };

        connectorName = new TextField("connectorName");
        connectorName.setEnabled(false);
        connectorName.setOutputMarkupId(true);

        version = new TextField("version");
        version.setEnabled(false);
        version.setOutputMarkupId(true);

        ChoiceRenderer renderer = new ChoiceRenderer("displayName", "bundleName");
        bundle = new DropDownChoice("bundle",bundles,renderer);

        bundle.setModel(new IModel() {

                public Object getObject() {
                    return selectedBundleTO;
                }

                public void setObject(Object object) {
                    selectedBundleTO = (ConnectorBundleTO)object;
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

        ListView propertiesView = (new ListView("connectorProperties",selectedBundleProperties) {

            PropertyTO propertyTO;

            @Override
            protected void populateItem(ListItem item) {
                propertyTO = (PropertyTO)item.getDefaultModelObject();

                item.add(new Label("key", propertyTO.getKey()));
                item.add(new TextField("value",new PropertyModel(propertyTO, "value")));
                
                connectorTO.getConfiguration().add(propertyTO);
            }
        });

        propertiesContainer = new WebMarkupContainer("propertiesContainer");
        propertiesContainer.setOutputMarkupId(true);
        propertiesContainer.add(propertiesView);

        connectorForm.add(propertiesContainer);

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                ConnectorInstanceTO connector = (ConnectorInstanceTO) form.getDefaultModelObject();

                //Set the model object configuration's properties to connectorPropertiesMap reference
                connector.setBundleName(bundle.getModelValue());

                try {

                    if (createFlag) 
                        restClient.createConnector(connector);
                     else
                        restClient.updateConnector(connector);

                    window.close(target);
                    
                } catch (Exception e) {
                    error(getString("error") + ":" + e.getMessage());
                }

                
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get("feedback"));
            }
        };

        connectorForm.add(new FeedbackPanel("feedback").setOutputMarkupId(true));

        connectorForm.add(connectorName);
        connectorForm.add(bundle);
        connectorForm.add(version);
        connectorForm.add(submit);

        add(connectorForm);
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

    /**
     * Convert a List<PropertyTO> object to a corresponding HashSet<PropertyTO>
     * object.
     * @param list
     * @return
     * INUTILIZZATO
     */
    public Set<PropertyTO> listToHashSet(List<PropertyTO> list) {
        Set<PropertyTO> set = new HashSet<PropertyTO>();

        for (PropertyTO propertyTO : list) {
            set.add(propertyTO);
        }

        return set;
    }
}