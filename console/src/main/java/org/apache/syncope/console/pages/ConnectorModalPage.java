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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ConnectorCapability;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
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

    private static final List<Class> NUMBER = Arrays.asList(new Class[]{Integer.class, Double.class, Long.class,
                Float.class, Number.class, Integer.TYPE, Long.TYPE, Double.TYPE, Float.TYPE});

    @SpringBean
    private ConnectorRestClient restClient;

    private CheckBoxMultipleChoice<ConnectorCapability> capabilitiesPalette;

    private WebMarkupContainer propertiesContainer;

    private List<ConnectorCapability> selectedCapabilities;

    private ConnBundleTO bundleTO;

    private List<ConnConfProperty> properties;

    public ConnectorModalPage(final PageReference pageRef, final ModalWindow window, final ConnInstanceTO connectorTO) {
        super();

        selectedCapabilities = new ArrayList<ConnectorCapability>(connectorTO.getId() == 0
                ? EnumSet.noneOf(ConnectorCapability.class)
                : connectorTO.getCapabilities());

        final IModel<List<ConnectorCapability>> capabilities =
                new LoadableDetachableModel<List<ConnectorCapability>>() {

                    private static final long serialVersionUID = 5275935387613157437L;

                    @Override
                    protected List<ConnectorCapability> load() {
                        return Arrays.asList(ConnectorCapability.values());
                    }
                };

        final Map<String, Map<String, ConnBundleTO>> mapConnBundleTO = new HashMap<String, Map<String, ConnBundleTO>>();
        for (ConnBundleTO connBundleTO : restClient.getAllBundles()) {
            if (!mapConnBundleTO.containsKey(connBundleTO.getBundleName())) {
                mapConnBundleTO.put(connBundleTO.getBundleName(), new HashMap<String, ConnBundleTO>());
            }
            final Map<String, ConnBundleTO> bundleMap = mapConnBundleTO.get(connBundleTO.getBundleName());
            if (!bundleMap.containsKey(connBundleTO.getVersion())) {
                bundleMap.put(connBundleTO.getVersion(), connBundleTO);
            }
        }

        bundleTO = getSelectedBundleTO(mapConnBundleTO, connectorTO);
        properties = fillProperties(bundleTO, connectorTO);

        final AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName", "display name", new PropertyModel<String>(connectorTO, "displayName"));
        displayName.setOutputMarkupId(true);
        displayName.addRequiredLabel();

        final AjaxDropDownChoicePanel<String> bundleName =
                new AjaxDropDownChoicePanel<String>("connectorName", "connectorName",
                new Model<String>(bundleTO != null ? bundleTO.getBundleName() : null));

        ((DropDownChoice) bundleName.getField()).setNullValid(true);

        bundleName.setStyleSheet("long_dynamicsize");
        bundleName.setChoices(new ArrayList<String>(mapConnBundleTO.keySet()));
        bundleName.setRequired(true);
        bundleName.addRequiredLabel();
        bundleName.setOutputMarkupId(true);
        bundleName.setEnabled(connectorTO.getId() == 0);
        bundleName.getField().setOutputMarkupId(true);

        final AjaxDropDownChoicePanel<String> version =
                new AjaxDropDownChoicePanel<String>("version", "version",
                new Model<String>(bundleTO != null ? bundleTO.getVersion() : null));

        version.setStyleSheet("long_dynamicsize");
        version.setChoices(bundleTO != null
                ? new ArrayList<String>(mapConnBundleTO.get(connectorTO.getBundleName()).keySet())
                : new ArrayList<String>());

        version.setRequired(true);
        version.addRequiredLabel();
        version.setEnabled(connectorTO != null && connectorTO.getBundleName() != null);
        version.setOutputMarkupId(true);
        version.addRequiredLabel();
        version.getField().setOutputMarkupId(true);

        final AjaxTextFieldPanel connRequestTimeout = new AjaxTextFieldPanel(
                "connRequestTimeout",
                "connRequestTimeout",
                new PropertyModel<String>(connectorTO, "connRequestTimeout"));

        final ListView<ConnConfProperty> view = new ListView<ConnConfProperty>(
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

                    if (NUMBER.contains(propertySchemaClass)) {
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

                if (isArray) {
                    field.removeRequiredLabel();

                    if (property.getValues().isEmpty()) {
                        property.getValues().add(null);
                    }

                    item.add(new MultiValueSelectorPanel<String>(
                            "panel", new PropertyModel<List<String>>(property, "values"), field));
                } else {
                    if (required) {
                        field.addRequiredLabel();
                    }

                    field.setNewModel(property.getValues());
                    item.add(field);
                }

                final AjaxCheckBoxPanel overridable = new AjaxCheckBoxPanel("connPropAttrOverridable",
                        "connPropAttrOverridable", new PropertyModel<Boolean>(property, "overridable"));

                item.add(overridable);
                connectorTO.addConfiguration(property);
            }
        };

        view.setOutputMarkupId(true);

        bundleName.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ((DropDownChoice) bundleName.getField()).setNullValid(false);
                // reset all information stored in connectorTO
                connectorTO.setConfiguration(new HashSet<ConnConfProperty>());
                connectorTO.setBundleName(bundleName.getField().getModelObject().toString());
                connectorTO.setVersion(null);
                properties.clear();
                version.setEnabled(connectorTO.getBundleName() != null);
                version.getField().setModelValue(null);
                List<String> choices = new ArrayList<String>(mapConnBundleTO.get(connectorTO.getBundleName()).keySet());
                version.setChoices(choices);
                if (choices.size() == 1) {
                    connectorTO.setVersion(choices.get(0));
                    version.getField().setModelObject(choices.get(0));
                    connectorTO.setDisplayName(displayName.getModelObject());
                    bundleTO = getSelectedBundleTO(mapConnBundleTO, connectorTO);
                    properties = fillProperties(bundleTO, connectorTO);
                }
                target.add(bundleName);
                target.add(version);
                target.add(propertiesContainer);
            }
        });

        version.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {

                connectorTO.setVersion(version.getField().getModelObject().toString());
                connectorTO.setDisplayName(displayName.getModelObject());
                bundleTO = getSelectedBundleTO(mapConnBundleTO, connectorTO);
                properties.clear();
                properties = fillProperties(bundleTO, connectorTO);
                target.add(bundleName);
                target.add(version);
                target.add(propertiesContainer);
            }
        });

        final AjaxLink<String> check = new IndicatingAjaxLink<String>("check", new ResourceModel("check")) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                connectorTO.setBundleName(bundleTO.getBundleName());
                connectorTO.setVersion(bundleTO.getVersion());

                if (restClient.check(connectorTO)) {
                    info(getString("success_connection"));
                } else {
                    error(getString("error_connection"));
                }

                target.add(feedbackPanel);
            }
        };

        final AjaxButton submit = new IndicatingAjaxButton("apply", new Model<String>(getString("submit"))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ConnInstanceTO conn = (ConnInstanceTO) form.getModelObject();

                conn.setBundleName(bundleTO.getBundleName());
                conn.setVersion(bundleTO.getVersion());
                conn.setConfiguration(new HashSet<ConnConfProperty>(view.getModelObject()));

                // Set the model object's capabilites to capabilitiesPalette's converted Set
                conn.setCapabilities(selectedCapabilities.isEmpty()
                        ? EnumSet.noneOf(ConnectorCapability.class)
                        : EnumSet.copyOf(selectedCapabilities));
                try {

                    if (connectorTO.getId() == 0) {
                        restClient.create(conn);
                    } else {
                        restClient.update(conn);
                    }

                    ((Resources) pageRef.getPage()).setModalResult(true);
                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
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

        final IndicatingAjaxButton cancel = new IndicatingAjaxButton("cancel", new ResourceModel("cancel")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
            }
        };

        cancel.setDefaultFormProcessing(false);

        String roles = connectorTO.getId() == 0
                ? xmlRolesReader.getAllAllowedRoles("Connectors", "create")
                : xmlRolesReader.getAllAllowedRoles("Connectors", "update");

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, roles);

        capabilitiesPalette = new CheckBoxMultipleChoice<ConnectorCapability>("capabilitiesPalette",
                new PropertyModel<List<ConnectorCapability>>(this,
                "selectedCapabilities"), capabilities);

        final Form<ConnInstanceTO> connectorForm = new Form<ConnInstanceTO>("form");
        connectorForm.setModel(new CompoundPropertyModel<ConnInstanceTO>(connectorTO));
        connectorForm.setOutputMarkupId(true);

        final Form<ConnInstanceTO> connectorPropForm = new Form<ConnInstanceTO>("connectorPropForm");
        connectorPropForm.setModel(new CompoundPropertyModel<ConnInstanceTO>(connectorTO));
        connectorPropForm.setOutputMarkupId(true);

        propertiesContainer = new WebMarkupContainer("container");
        propertiesContainer.setOutputMarkupId(true);
        propertiesContainer.add(connectorPropForm);

        connectorForm.add(propertiesContainer);
        connectorPropForm.add(view);
        connectorPropForm.add(check);

        connectorForm.add(bundleName);
        connectorForm.add(displayName);
        connectorForm.add(version);
        connectorForm.add(connRequestTimeout);
        connectorForm.add(capabilitiesPalette);
        connectorForm.add(submit);
        connectorForm.add(cancel);
        add(connectorForm);
    }

    private ConnBundleTO getSelectedBundleTO(final Map<String, Map<String, ConnBundleTO>> bundles,
            final ConnInstanceTO connTO) {

        if (connTO != null && StringUtils.isNotBlank(connTO.getBundleName())
                && StringUtils.isNotBlank(connTO.getVersion())) {

            for (String bKey : bundles.keySet()) {
                if (bKey.equals(connTO.getBundleName())) {
                    for (String vKey : bundles.get(bKey).keySet()) {
                        ConnBundleTO to = bundles.get(bKey).get(vKey);
                        if (to.getVersion().equals(connTO.getVersion())) {
                            connTO.setConnectorName(to.getConnectorName());
                            connTO.setVersion(to.getVersion());
                            return to;
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<ConnConfProperty> fillProperties(final ConnBundleTO bundleTO, final ConnInstanceTO connTO) {

        final List<ConnConfProperty> props = new ArrayList<ConnConfProperty>();

        if (bundleTO != null) {
            for (ConnConfPropSchema key : bundleTO.getProperties()) {
                final ConnConfProperty propertyTO = new ConnConfProperty();
                propertyTO.setSchema(key);
                if (connTO.getId() != 0 && connTO.getConfigurationMap().containsKey(key.getName())) {
                    propertyTO.setValues(connTO.getConfigurationMap().get(key.getName()).getValues());
                    propertyTO.setOverridable(connTO.getConfigurationMap().get(key.getName()).isOverridable());
                }
                props.add(propertyTO);
            }
        }
        // re-order properties
        Collections.sort(props);
        return props;
    }

    public List<ConnConfProperty> getProperties() {
        return properties;
    }
}
