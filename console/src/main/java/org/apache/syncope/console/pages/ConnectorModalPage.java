/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ConnectorCapability;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.markup.html.list.AltListView;
import org.apache.syncope.console.rest.ConnectorRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.ClassUtils;

/**
 * Modal window with Connector form.
 */
public class ConnectorModalPage extends BaseModalPage {

    private static final long serialVersionUID = -2025535531121434050L;

    // GuardedString is not in classpath
    private static final String GUARDED_STRING = "org.identityconnectors.common.security.GuardedString";

    // GuardedByteArray is not in classpath
    private static final String GUARDED_BYTE_ARRAY = "org.identityconnectors.common.security.GuardedByteArray";

    private static final Class[] NUMBER = {Integer.class, Double.class, Long.class,
        Float.class, Number.class, Integer.TYPE, Long.TYPE, Double.TYPE, Float.TYPE};

    @SpringBean
    private ConnectorRestClient restClient;

    private final Map<String, Map<String, Map<String, ConnBundleTO>>> mapConnBundleTOs;

    private final List<ConnectorCapability> selectedCapabilities;

    private ConnBundleTO bundleTO;

    private List<ConnConfProperty> properties;

    private final WebMarkupContainer propertiesContainer;

    public ConnectorModalPage(final PageReference pageRef, final ModalWindow window,
            final ConnInstanceTO connInstanceTO) {

        super();

        // general data setup

        selectedCapabilities = new ArrayList<ConnectorCapability>(connInstanceTO.getId() == 0
                ? EnumSet.noneOf(ConnectorCapability.class)
                : connInstanceTO.getCapabilities());

        mapConnBundleTOs = new HashMap<String, Map<String, Map<String, ConnBundleTO>>>();
        for (ConnBundleTO connBundleTO : restClient.getAllBundles()) {
            // by location
            if (!mapConnBundleTOs.containsKey(connBundleTO.getLocation())) {
                mapConnBundleTOs.put(connBundleTO.getLocation(), new HashMap<String, Map<String, ConnBundleTO>>());
            }
            final Map<String, Map<String, ConnBundleTO>> byLocation = mapConnBundleTOs.get(connBundleTO.getLocation());

            // by name
            if (!byLocation.containsKey(connBundleTO.getBundleName())) {
                byLocation.put(connBundleTO.getBundleName(), new HashMap<String, ConnBundleTO>());
            }
            final Map<String, ConnBundleTO> byName = byLocation.get(connBundleTO.getBundleName());

            // by version
            if (!byName.containsKey(connBundleTO.getVersion())) {
                byName.put(connBundleTO.getVersion(), connBundleTO);
            }
        }

        bundleTO = getSelectedBundleTO(connInstanceTO);
        properties = fillProperties(bundleTO, connInstanceTO);

        // form - first tab

        final Form<ConnInstanceTO> connectorForm = new Form<ConnInstanceTO>(FORM);
        connectorForm.setModel(new CompoundPropertyModel<ConnInstanceTO>(connInstanceTO));
        connectorForm.setOutputMarkupId(true);
        add(connectorForm);

        propertiesContainer = new WebMarkupContainer("container");
        propertiesContainer.setOutputMarkupId(true);
        connectorForm.add(propertiesContainer);

        final Form<ConnInstanceTO> connectorPropForm = new Form<ConnInstanceTO>("connectorPropForm");
        connectorPropForm.setModel(new CompoundPropertyModel<ConnInstanceTO>(connInstanceTO));
        connectorPropForm.setOutputMarkupId(true);
        propertiesContainer.add(connectorPropForm);

        final AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName", "display name", new PropertyModel<String>(connInstanceTO, "displayName"));
        displayName.setOutputMarkupId(true);
        displayName.addRequiredLabel();
        connectorForm.add(displayName);

        final AjaxDropDownChoicePanel<String> location =
                new AjaxDropDownChoicePanel<String>("location", "location",
                new Model<String>(bundleTO == null ? null : bundleTO.getLocation()));
        ((DropDownChoice) location.getField()).setNullValid(true);
        location.setStyleSheet("long_dynamicsize");
        location.setChoices(new ArrayList<String>(mapConnBundleTOs.keySet()));
        location.setRequired(true);
        location.addRequiredLabel();
        location.setOutputMarkupId(true);
        location.setEnabled(connInstanceTO.getId() == 0);
        location.getField().setOutputMarkupId(true);
        connectorForm.add(location);

        final AjaxDropDownChoicePanel<String> connectorName =
                new AjaxDropDownChoicePanel<String>("connectorName", "connectorName",
                new Model<String>(bundleTO == null ? null : bundleTO.getBundleName()));
        ((DropDownChoice) connectorName.getField()).setNullValid(true);
        connectorName.setStyleSheet("long_dynamicsize");
        connectorName.setChoices(bundleTO == null
                ? new ArrayList<String>()
                : new ArrayList<String>(mapConnBundleTOs.get(connInstanceTO.getLocation()).keySet()));
        connectorName.setRequired(true);
        connectorName.addRequiredLabel();
        connectorName.setEnabled(connInstanceTO.getLocation() != null);
        connectorName.setOutputMarkupId(true);
        connectorName.setEnabled(connInstanceTO.getId() == 0);
        connectorName.getField().setOutputMarkupId(true);
        connectorForm.add(connectorName);

        final AjaxDropDownChoicePanel<String> version =
                new AjaxDropDownChoicePanel<String>("version", "version",
                new Model<String>(bundleTO == null ? null : bundleTO.getVersion()));
        version.setStyleSheet("long_dynamicsize");
        version.setChoices(bundleTO == null
                ? new ArrayList<String>()
                : new ArrayList<String>(mapConnBundleTOs.get(connInstanceTO.getLocation()).
                get(connInstanceTO.getBundleName()).keySet()));
        version.setRequired(true);
        version.addRequiredLabel();
        version.setEnabled(connInstanceTO.getBundleName() != null);
        version.setOutputMarkupId(true);
        version.addRequiredLabel();
        version.getField().setOutputMarkupId(true);
        connectorForm.add(version);

        final AjaxTextFieldPanel connRequestTimeout = new AjaxTextFieldPanel(
                "connRequestTimeout",
                "connRequestTimeout",
                new PropertyModel<String>(connInstanceTO, "connRequestTimeout"));
        connectorForm.add(connRequestTimeout);

        // form - first tab - onchange()
        location.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ((DropDownChoice) location.getField()).setNullValid(false);
                connInstanceTO.setLocation(location.getModelObject());
                target.add(location);

                connectorName.setChoices(new ArrayList<String>(
                        mapConnBundleTOs.get(location.getModelObject()).keySet()));
                connectorName.setEnabled(true);
                connectorName.getField().setModelValue(null);
                target.add(connectorName);

                version.setChoices(new ArrayList<String>());
                version.getField().setModelValue(null);
                version.setEnabled(false);
                target.add(version);

                properties.clear();
                target.add(propertiesContainer);
            }
        });
        connectorName.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ((DropDownChoice) connectorName.getField()).setNullValid(false);
                connInstanceTO.setBundleName(connectorName.getModelObject());
                target.add(connectorName);

                List<String> versions = new ArrayList<String>(
                        mapConnBundleTOs.get(location.getModelObject()).get(connectorName.getModelObject()).keySet());
                version.setChoices(versions);
                version.setEnabled(true);
                if (versions.size() == 1) {
                    selectVersion(target, connInstanceTO, version, versions.get(0));
                    version.getField().setModelObject(versions.get(0));
                } else {
                    version.getField().setModelValue(null);
                    properties.clear();
                    target.add(propertiesContainer);
                }
                target.add(version);
            }
        });
        version.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                selectVersion(target, connInstanceTO, version, version.getModelObject());
            }
        });

        // form - second tab (properties)

        final ListView<ConnConfProperty> connPropView = new AltListView<ConnConfProperty>(
                "connectorProperties", new PropertyModel<List<ConnConfProperty>>(this, "properties")) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<ConnConfProperty> item) {
                final ConnConfProperty property = item.getModelObject();

                final Label label = new Label("connPropAttrSchema", property.getSchema().getDisplayName() == null
                        || property.getSchema().getDisplayName().isEmpty()
                        ? property.getSchema().getName()
                        : property.getSchema().getDisplayName());

                item.add(label);

                final FieldPanel field;
                boolean required = false;
                boolean isArray = false;
                if (property.getSchema().isConfidential()
                        || GUARDED_STRING.equalsIgnoreCase(property.getSchema().getType())
                        || GUARDED_BYTE_ARRAY.equalsIgnoreCase(property.getSchema().getType())) {

                    field = new AjaxPasswordFieldPanel("panel",
                            label.getDefaultModelObjectAsString(), new Model<String>());

                    ((PasswordTextField) field.getField()).setResetPassword(false);

                    required = property.getSchema().isRequired();
                } else {
                    Class<?> propertySchemaClass;

                    try {
                        propertySchemaClass =
                                ClassUtils.forName(property.getSchema().getType(), ClassUtils.getDefaultClassLoader());
                    } catch (Exception e) {
                        LOG.error("Error parsing attribute type", e);
                        propertySchemaClass = String.class;
                    }
                    if (ArrayUtils.contains(NUMBER, propertySchemaClass)) {
                        field = new AjaxNumberFieldPanel("panel",
                                label.getDefaultModelObjectAsString(), new Model<Number>(),
                                ClassUtils.resolvePrimitiveIfNecessary(propertySchemaClass));

                        required = property.getSchema().isRequired();
                    } else if (Boolean.class.equals(propertySchemaClass) || boolean.class.equals(propertySchemaClass)) {
                        field = new AjaxCheckBoxPanel("panel",
                                label.getDefaultModelObjectAsString(), new Model<Boolean>());
                    } else {
                        field = new AjaxTextFieldPanel("panel",
                                label.getDefaultModelObjectAsString(), new Model<String>());

                        required = property.getSchema().isRequired();
                    }

                    if (String[].class.equals(propertySchemaClass)) {
                        isArray = true;
                    }
                }

                field.setTitle(property.getSchema().getHelpMessage());

                if (required) {
                    field.addRequiredLabel();
                }

                if (isArray) {
                    if (property.getValues().isEmpty()) {
                        property.getValues().add(null);
                    }

                    item.add(new MultiValueSelectorPanel<String>(
                            "panel", new PropertyModel<List<String>>(property, "values"), field));
                } else {
                    field.setNewModel(property.getValues());
                    item.add(field);
                }

                final AjaxCheckBoxPanel overridable = new AjaxCheckBoxPanel("connPropAttrOverridable",
                        "connPropAttrOverridable", new PropertyModel<Boolean>(property, "overridable"));

                item.add(overridable);
                connInstanceTO.getConfiguration().add(property);
            }
        };
        connPropView.setOutputMarkupId(true);
        connectorPropForm.add(connPropView);

        final AjaxLink<String> check = new IndicatingAjaxLink<String>("check", new ResourceModel("check")) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                connInstanceTO.setBundleName(bundleTO.getBundleName());
                connInstanceTO.setVersion(bundleTO.getVersion());
                connInstanceTO.setConnectorName(bundleTO.getConnectorName());

                if (restClient.check(connInstanceTO)) {
                    info(getString("success_connection"));
                } else {
                    error(getString("error_connection"));
                }

                target.add(feedbackPanel);
            }
        };
        connectorPropForm.add(check);

        // form - third tab (capabilities)

        final IModel<List<ConnectorCapability>> capabilities =
                new LoadableDetachableModel<List<ConnectorCapability>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<ConnectorCapability> load() {
                return Arrays.asList(ConnectorCapability.values());
            }
        };
        CheckBoxMultipleChoice<ConnectorCapability> capabilitiesPalette =
                new CheckBoxMultipleChoice<ConnectorCapability>("capabilitiesPalette",
                new PropertyModel<List<ConnectorCapability>>(this, "selectedCapabilities"), capabilities);
        connectorForm.add(capabilitiesPalette);

        // form - submit / cancel buttons

        final AjaxButton submit = new IndicatingAjaxButton(APPLY, new Model<String>(getString(SUBMIT))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ConnInstanceTO conn = (ConnInstanceTO) form.getModelObject();

                conn.setConnectorName(bundleTO.getConnectorName());
                conn.setBundleName(bundleTO.getBundleName());
                conn.setVersion(bundleTO.getVersion());
                conn.getConfiguration().addAll(connPropView.getModelObject());

                // Set the model object's capabilites to capabilitiesPalette's converted Set
                conn.getCapabilities().addAll(selectedCapabilities.isEmpty()
                        ? EnumSet.noneOf(ConnectorCapability.class)
                        : EnumSet.copyOf(selectedCapabilities));
                try {
                    if (connInstanceTO.getId() == 0) {
                        restClient.create(conn);
                    } else {
                        restClient.update(conn);
                    }

                    ((Resources) pageRef.getPage()).setModalResult(true);
                    window.close(target);
                } catch (SyncopeClientException e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    target.add(feedbackPanel);
                    ((Resources) pageRef.getPage()).setModalResult(false);
                    LOG.error("While creating or updating connector {}", conn, e);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {

                target.add(feedbackPanel);
            }
        };
        String roles = connInstanceTO.getId() == 0
                ? xmlRolesReader.getAllAllowedRoles("Connectors", "create")
                : xmlRolesReader.getAllAllowedRoles("Connectors", "update");
        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, roles);
        connectorForm.add(submit);

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }
        };
        cancel.setDefaultFormProcessing(false);
        connectorForm.add(cancel);
    }

    private ConnBundleTO getSelectedBundleTO(final ConnInstanceTO connInstanceTO) {
        ConnBundleTO result = null;
        if (connInstanceTO != null
                && StringUtils.isNotBlank(connInstanceTO.getLocation())
                && StringUtils.isNotBlank(connInstanceTO.getBundleName())
                && StringUtils.isNotBlank(connInstanceTO.getVersion())
                && mapConnBundleTOs.containsKey(connInstanceTO.getLocation())) {

            Map<String, Map<String, ConnBundleTO>> byLocation = mapConnBundleTOs.get(connInstanceTO.getLocation());
            if (byLocation.containsKey(connInstanceTO.getBundleName())) {
                Map<String, ConnBundleTO> byName = byLocation.get(connInstanceTO.getBundleName());
                if (byName.containsKey(connInstanceTO.getVersion())) {
                    result = byName.get(connInstanceTO.getVersion());
                }
            }
        }
        return result;
    }

    private List<ConnConfProperty> fillProperties(final ConnBundleTO bundleTO, final ConnInstanceTO connInstanceTO) {
        final List<ConnConfProperty> props = new ArrayList<ConnConfProperty>();

        if (bundleTO != null) {
            for (ConnConfPropSchema key : bundleTO.getProperties()) {
                final ConnConfProperty propertyTO = new ConnConfProperty();
                propertyTO.setSchema(key);
                if (connInstanceTO.getId() != 0 && connInstanceTO.getConfigurationMap().containsKey(key.getName())) {
                    propertyTO.getValues().addAll(connInstanceTO.getConfigurationMap().get(key.getName()).getValues());
                    propertyTO.setOverridable(connInstanceTO.getConfigurationMap().get(key.getName()).isOverridable());
                }
                props.add(propertyTO);
            }
        }
        // re-order properties
        Collections.sort(props);
        return props;
    }

    private void selectVersion(final AjaxRequestTarget target, final ConnInstanceTO connInstanceTO,
            final AjaxDropDownChoicePanel<String> version, final String versionValue) {

        connInstanceTO.setVersion(versionValue);
        target.add(version);

        bundleTO = getSelectedBundleTO(connInstanceTO);
        properties = fillProperties(bundleTO, connInstanceTO);
        target.add(propertiesContainer);
    }

    public List<ConnConfProperty> getProperties() {
        return properties;
    }
}
