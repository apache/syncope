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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
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
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;
import org.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.ConnectorCapability;

/**
 * Modal window with Connector form.
 */
public class ConnectorModalPage extends BaseModalPage {

    private static final long serialVersionUID = -2025535531121434050L;

    private static final String GUARDED_STRING =
            "org.identityconnectors.common.security.GuardedString";

    private static final String STRING_ARRAY = "[Ljava.lang.String;";

    private static final String BOOLEAN = "java.lang.Boolean";

    @SpringBean
    private ConnectorRestClient restClient;

    private CheckBoxMultipleChoice capabilitiesPalette;

    private ConnBundleTO selectedBundleTO = new ConnBundleTO();

    private WebMarkupContainer propertiesContainer;

    private List<ConnectorCapability> selectedCapabilities;

    public ConnectorModalPage(final PageReference callerPageRef,
            final ModalWindow window,
            final ConnInstanceTO connectorTO,
            final boolean createFlag) {

        super();

        selectedCapabilities = new ArrayList(createFlag
                ? EnumSet.noneOf(ConnectorCapability.class)
                : connectorTO.getCapabilities());

        IModel<List<ConnConfProperty>> selectedBundleProperties =
                new LoadableDetachableModel<List<ConnConfProperty>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

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
                            selectedBundleTO.setVersion(
                                    connectorTO.getVersion());
                            result = new ArrayList<ConnConfProperty>(
                                    connectorTO.getConfiguration());
                        }

                        if (result != null && !result.isEmpty()) {
                            Collections.sort(result);
                        }

                        return result;
                    }
                };

        final AjaxTextFieldPanel connectorName = new AjaxTextFieldPanel(
                "connectorName",
                "connector name",
                new PropertyModel<String>(connectorTO, "connectorName"),
                false);

        connectorName.setOutputMarkupId(true);
        connectorName.setEnabled(false);

        final AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName",
                "display name",
                new PropertyModel<String>(connectorTO, "displayName"),
                false);

        displayName.setOutputMarkupId(true);
        displayName.addRequiredLabel();

        final AjaxTextFieldPanel version = new AjaxTextFieldPanel(
                "version",
                "version",
                new PropertyModel<String>(connectorTO, "version"),
                false);

        displayName.setOutputMarkupId(true);
        version.setEnabled(false);

        final IModel<List<ConnBundleTO>> bundles =
                new LoadableDetachableModel<List<ConnBundleTO>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<ConnBundleTO> load() {
                        return restClient.getAllBundles();
                    }
                };

        final AjaxDropDownChoicePanel<ConnBundleTO> bundle =
                new AjaxDropDownChoicePanel<ConnBundleTO>(
                "bundle", "bundle", new Model(null), false);

        bundle.setStyleShet("long_dynamicsize");

        bundle.setChoices(bundles.getObject());

        bundle.setChoiceRenderer(new ChoiceRenderer<ConnBundleTO>() {

            private static final long serialVersionUID =
                    -1945543182376191187L;

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
                return object.getBundleName() + "#" + object.getVersion();
            }
        });

        ((DropDownChoice) bundle.getField()).setNullValid(true);
        bundle.setRequired(true);

        bundle.getField().add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID =
                            -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        //reset all informations stored in connectorTO
                        connectorTO.setConfiguration(
                                new HashSet<ConnConfProperty>());
                        DropDownChoice bundleChoice =
                                (DropDownChoice) bundle.getField();
                        bundleChoice.setNullValid(false);
                        target.add(bundle.getField());
                        target.add(propertiesContainer);
                        target.add(connectorName);
                        target.add(version);
                    }
                });

        bundle.getField().setModel(new IModel<ConnBundleTO>() {

            private static final long serialVersionUID = -3736598995576061229L;

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

        bundle.addRequiredLabel();
        bundle.setEnabled(createFlag);

        final ListView<ConnConfProperty> propView = new ListView<ConnConfProperty>(
                "connectorProperties", selectedBundleProperties) {

            private static final long serialVersionUID =
                    9101744072914090143L;

            @Override
            protected void populateItem(
                    final ListItem<ConnConfProperty> item) {
                final ConnConfProperty property = item.getModelObject();

                final Label label = new Label("connPropAttrSchema",
                        property.getSchema().getDisplayName() == null
                        || property.getSchema().getDisplayName().
                        isEmpty()
                        ? property.getSchema().getName()
                        : property.getSchema().getDisplayName());

                item.add(label);

                final FieldPanel field;

                boolean required = false;

                if (GUARDED_STRING.equalsIgnoreCase(
                        property.getSchema().getType())) {

                    field = new AjaxPasswordFieldPanel(
                            "panel",
                            label.getDefaultModelObjectAsString(),
                            new Model(),
                            true);

                    ((PasswordTextField) field.getField()).
                                    setResetPassword(
                                    false);
                    
                    required = property.getSchema().isRequired();

                } else if (BOOLEAN.equalsIgnoreCase(
                        property.getSchema().getType())) {
                    field = new AjaxCheckBoxPanel(
                            "panel",
                            label.getDefaultModelObjectAsString(),
                            new Model(),
                            true);

                } else {

                    field = new AjaxTextFieldPanel(
                            "panel",
                            label.getDefaultModelObjectAsString(),
                            new Model(),
                            true);

                    required = property.getSchema().isRequired();
                }

                field.setTitle(property.getSchema().getHelpMessage());

                if (required) {
                    field.addRequiredLabel();
                }

                if (STRING_ARRAY.equalsIgnoreCase(
                        property.getSchema().getType())) {

                    field.removeRequiredLabel();

                    item.add(new MultiValueSelectorPanel<String>(
                            "panel",
                            new PropertyModel<List<String>>(property, "values"),
                            String.class,
                            field));
                } else {
                    field.setNewModel(property.getValues());
                    item.add(field);
                }

                final AjaxCheckBoxPanel overridable = new AjaxCheckBoxPanel(
                        "connPropAttrOverridable",
                        "connPropAttrOverridable",
                        new PropertyModel(property, "overridable"),
                        true);

                item.add(overridable);
                connectorTO.getConfiguration().add(property);
            }
        };

        propertiesContainer = new WebMarkupContainer("container");
        propertiesContainer.setOutputMarkupId(true);
        propertiesContainer.add(propView);

        Form connectorForm = new Form("form");
        connectorForm.setModel(new CompoundPropertyModel(connectorTO));
        connectorForm.add(propertiesContainer);

        final AjaxButton submit = new IndicatingAjaxButton("apply", new Model(
                getString("submit"))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                final ConnInstanceTO connector =
                        (ConnInstanceTO) form.getDefaultModelObject();

                ConnBundleTO bundleTO = (ConnBundleTO) bundle.getModelObject();
                connector.setBundleName(bundleTO.getBundleName());

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

                    ((Resources) callerPageRef.getPage()).setModalResult(
                            true);
                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                    ((Resources) callerPageRef.getPage()).setModalResult(
                            false);
                    LOG.error("While creating or updating connector "
                            + connector);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
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

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<ConnectorCapability> load() {
                        return Arrays.asList(ConnectorCapability.values());
                    }
                };

        capabilitiesPalette = new CheckBoxMultipleChoice("capabilitiesPalette",
                new PropertyModel(this, "selectedCapabilities"), capabilities);
        connectorForm.add(capabilitiesPalette);

        connectorForm.add(submit);

        add(connectorForm);
    }
}
