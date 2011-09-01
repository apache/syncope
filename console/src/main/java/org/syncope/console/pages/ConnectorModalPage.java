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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
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
import org.syncope.client.to.ConnBundleTO;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.ConnConfProperty;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnectorCapability;

/**
 * Modal window with Connector form.
 */
public class ConnectorModalPage extends BaseModalPage {

    private static final long serialVersionUID = -2025535531121434050L;

    @SpringBean
    private ConnectorRestClient restClient;

    private CheckBoxMultipleChoice capabilitiesPalette;

    private ConnBundleTO selectedBundleTO = new ConnBundleTO();

    private WebMarkupContainer propertiesContainer;

    private List<ConnectorCapability> selectedCapabilities;

    public ConnectorModalPage(final Connectors basePage,
            final ModalWindow window,
            final ConnInstanceTO connectorTO,
            final boolean createFlag) {

        super();

        selectedCapabilities = new ArrayList(createFlag
                ? EnumSet.noneOf(ConnectorCapability.class)
                : connectorTO.getCapabilities());

        IModel<List<ConnConfProperty>> selectedBundleProperties =
                new LoadableDetachableModel<List<ConnConfProperty>>() {

                    @Override
                    protected List<ConnConfProperty> load() {
                        List<ConnConfProperty> result;

                        if (createFlag) {
                            connectorTO.setConnectorName(
                                    selectedBundleTO.getConnectorName());
                            connectorTO.setVersion(
                                    selectedBundleTO.getVersion());

                            result = new ArrayList<ConnConfProperty>();
                            ConnConfProperty propertyTO;

                            for (ConnConfPropSchema key :
                                    selectedBundleTO.getProperties()) {

                                propertyTO = new ConnConfProperty();
                                propertyTO.setSchema(key);

                                result.add(propertyTO);
                            }
                        } else {
                            selectedBundleTO.setBundleName(
                                    connectorTO.getBundleName());
                            result = new ArrayList(
                                    connectorTO.getConfiguration());
                        }
                        return result;
                    }
                };

        final TextField connectorName = new TextField("connectorName");
        connectorName.setEnabled(false);
        connectorName.setOutputMarkupId(true);

        TextField displayName = new TextField("displayName");
        displayName.setOutputMarkupId(true);
        displayName.setRequired(true);

        final TextField version = new TextField("version");
        version.setEnabled(false);
        version.setOutputMarkupId(true);


        final IModel<List<ConnBundleTO>> bundles =
                new LoadableDetachableModel<List<ConnBundleTO>>() {

                    @Override
                    protected List<ConnBundleTO> load() {
                        return restClient.getAllBundles();
                    }
                };
        final DropDownChoice<ConnBundleTO> bundle =
                new DropDownChoice<ConnBundleTO>("bundle", bundles,
                new ChoiceRenderer<ConnBundleTO>() {

                    @Override
                    public Object getDisplayValue(final ConnBundleTO object) {
                        return object.getBundleName() + " "
                                + object.getVersion();
                    }

                    @Override
                    public String getIdValue(final ConnBundleTO object,
                            final int index) {

                        // idValue must include version as well in order to cope
                        //with multiple version of the same bundle.
                        return object.getBundleName() + "#"
                                + object.getVersion();
                    }
                });
        bundle.setModel(new IModel<ConnBundleTO>() {

            @Override
            public ConnBundleTO getObject() {
                return selectedBundleTO;
            }

            @Override
            public void setObject(final ConnBundleTO object) {
                selectedBundleTO = object;
            }

            @Override
            public void detach() {
            }
        });
        bundle.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                //reset all informations stored in connectorTO
                connectorTO.setConfiguration(new HashSet<ConnConfProperty>());

                target.addComponent(propertiesContainer);
                target.addComponent(connectorName);
                target.addComponent(version);
            }
        });
        bundle.setRequired(true);
        bundle.setEnabled(createFlag);

        ListView<ConnConfProperty> propView = new ListView<ConnConfProperty>(
                "connectorProperties", selectedBundleProperties) {

            @Override
            protected void populateItem(final ListItem<ConnConfProperty> item) {
                ConnConfProperty property = item.getModelObject();

                Label label = new Label("connPropAttrSchema",
                        property.getSchema().getDisplayName() == null
                        || property.getSchema().getDisplayName().isEmpty()
                        ? property.getSchema().getName()
                        : property.getSchema().getDisplayName());

                item.add(label);

                final FieldPanel field = new AjaxTextFieldPanel(
                        "connPropAttrValue",
                        label.getDefaultModelObjectAsString(),
                        new PropertyModel<String>(property, "value"),
                        true).setRequired(property.getSchema().isRequired()).
                        setTitle(property.getSchema().getHelpMessage());

                if (property.getSchema().isRequired()) {
                    field.addRequiredLabel();
                }

                item.add(field);

                connectorTO.getConfiguration().add(property);
            }
        };

        propertiesContainer = new WebMarkupContainer("propertiesContainer");
        propertiesContainer.setOutputMarkupId(true);
        propertiesContainer.add(propView);

        Form connectorForm = new Form("ConnectorForm");
        connectorForm.setModel(new CompoundPropertyModel(connectorTO));
        connectorForm.add(propertiesContainer);

        AjaxButton submit = new IndicatingAjaxButton("submit", new Model(
                getString("submit"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                ConnInstanceTO connector =
                        (ConnInstanceTO) form.getDefaultModelObject();

                // bundle.getModelValue() will call bundle.getChoiceRenderer.
                // getIdValue(), so we need to remove version number appended
                // above
                connector.setBundleName(bundle.getModelValue().substring(
                        0, bundle.getModelValue().lastIndexOf('#')));

                // Set the model object's capabilites to
                // capabilitiesPalette's converted Set
                if (!selectedCapabilities.isEmpty()) {
                    // exception if selectedCapabilities is empy
                    connector.setCapabilities(
                            EnumSet.copyOf(selectedCapabilities));
                } else {
                    connector.setCapabilities(
                            EnumSet.noneOf(ConnectorCapability.class));
                }
                try {
                    if (createFlag) {
                        restClient.create(connector);
                    } else {
                        restClient.update(connector);
                    }

                    basePage.setOperationResult(true);
                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                    basePage.setOperationResult(false);

                    LOG.error("While creating or updating connector "
                            + connector);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles = createFlag
                ? xmlRolesReader.getAllAllowedRoles("Connectors", "create")
                : xmlRolesReader.getAllAllowedRoles("Connectors", "update");

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                allowedRoles);

        connectorForm.add(connectorName);
        connectorForm.add(displayName);
        connectorForm.add(bundle);
        connectorForm.add(version);

        final IModel<List<ConnectorCapability>> capabilities =
                new LoadableDetachableModel<List<ConnectorCapability>>() {

                    @Override
                    protected List<ConnectorCapability> load() {
                        return Arrays.asList(ConnectorCapability.values());
                    }
                };

        capabilitiesPalette = new CheckBoxMultipleChoice("capabilitiesPalette",
                new PropertyModel(this, "selectedCapabilities"), capabilities);
        connectorForm.add(capabilitiesPalette);


        final CheckBox resetToken =
                new CheckBox("resetToken", new Model(getString("resetToken")));

        resetToken.add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget art) {
                        if (resetToken.getModelObject()) {
                            connectorTO.setSyncToken(null);
                        }
                    }
                });
        connectorForm.add(resetToken);

        connectorForm.add(submit);

        add(connectorForm);
    }
}
