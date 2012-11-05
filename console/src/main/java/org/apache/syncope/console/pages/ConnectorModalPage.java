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
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang.StringUtils;
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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.ClassUtils;
import org.apache.syncope.console.commons.CloseOnESCBehavior;
import org.apache.syncope.console.rest.ConnectorRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.apache.syncope.to.ConnBundleTO;
import org.apache.syncope.to.ConnInstanceTO;
import org.apache.syncope.types.ConnConfPropSchema;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.ConnectorCapability;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;

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

    private CheckBoxMultipleChoice capabilitiesPalette;

    private WebMarkupContainer propertiesContainer;

    private List<ConnectorCapability> selectedCapabilities;

    private ConnBundleTO bundleTO;

    private List<ConnConfProperty> properties;

    public ConnectorModalPage(final PageReference callerPageRef, final ModalWindow window,
            final ConnInstanceTO connectorTO) {

        super();

        selectedCapabilities = new ArrayList(connectorTO.getId() == 0
                ? EnumSet.noneOf(ConnectorCapability.class)
                : connectorTO.getCapabilities());

        final IModel<List<ConnectorCapability>> capabilities = new LoadableDetachableModel<List<ConnectorCapability>>() {
            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<ConnectorCapability> load() {
                return Arrays.asList(ConnectorCapability.values());
            }
        };

        final IModel<List<ConnBundleTO>> bundles = new LoadableDetachableModel<List<ConnBundleTO>>() {
            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<ConnBundleTO> load() {
                return restClient.getAllBundles();
            }
        };

        bundleTO = getSelectedBundleTO(bundles.getObject(), connectorTO);
        properties = fillProperties(bundleTO, connectorTO);

        final AjaxTextFieldPanel connectorName = new AjaxTextFieldPanel(
                "connectorName", "connector name", new PropertyModel<String>(connectorTO, "connectorName"));
        connectorName.setOutputMarkupId(true);
        connectorName.setEnabled(false);

        final AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName", "display name", new PropertyModel<String>(connectorTO, "displayName"));
        displayName.setOutputMarkupId(true);
        displayName.addRequiredLabel();

        final AjaxTextFieldPanel version = new AjaxTextFieldPanel(
                "version", "version", new PropertyModel<String>(connectorTO, "version"));
        displayName.setOutputMarkupId(true);
        version.setEnabled(false);

        final AjaxDropDownChoicePanel<ConnBundleTO> bundle =
                new AjaxDropDownChoicePanel<ConnBundleTO>("bundle", "bundle", new Model<ConnBundleTO>(bundleTO));
        bundle.setStyleSheet("long_dynamicsize");
        bundle.setChoices(bundles.getObject());
        bundle.setChoiceRenderer(new ChoiceRenderer<ConnBundleTO>() {
            private static final long serialVersionUID = -1945543182376191187L;

            @Override
            public Object getDisplayValue(final ConnBundleTO object) {
                return object.getBundleName() + " " + object.getVersion();
            }

            @Override
            public String getIdValue(final ConnBundleTO object, final int index) {
                // idValue must include version as well in order to cope
                // with multiple version of the same bundle.
                return object.getBundleName() + "#" + object.getVersion();
            }
        });

        ((DropDownChoice) bundle.getField()).setNullValid(true);
        bundle.setRequired(true);
        bundle.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // reset all information stored in connectorTO
                connectorTO.setConfiguration(new HashSet<ConnConfProperty>());

                ((DropDownChoice) bundle.getField()).setNullValid(false);
                target.add(bundle.getField());

                target.add(connectorName);
                target.add(version);

                target.add(propertiesContainer);
            }
        });
        bundle.getField().setModel(new IModel<ConnBundleTO>() {
            private static final long serialVersionUID = -3736598995576061229L;

            @Override
            public ConnBundleTO getObject() {
                return bundleTO;
            }

            @Override
            public void setObject(final ConnBundleTO object) {
                if (object != null && connectorTO != null) {
                    connectorTO.setBundleName(object.getBundleName());
                    connectorTO.setVersion(object.getVersion());
                    connectorTO.setConnectorName(object.getConnectorName());
                    properties = fillProperties(object, connectorTO);
                    bundleTO = object;
                }
            }

            @Override
            public void detach() {
            }
        });
        bundle.addRequiredLabel();
        bundle.setEnabled(connectorTO.getId() == 0);

        final ListView<ConnConfProperty> view = new ListView<ConnConfProperty>(
                "connectorProperties", new PropertyModel(this, "properties")) {
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

                    field = new AjaxPasswordFieldPanel("panel", label.getDefaultModelObjectAsString(), new Model());

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
                        field = new AjaxNumberFieldPanel("panel", label.getDefaultModelObjectAsString(), new Model(),
                                ClassUtils.resolvePrimitiveIfNecessary(propertySchemaClass));

                        required = property.getSchema().isRequired();
                    } else if (Boolean.class.equals(propertySchemaClass) || boolean.class.equals(propertySchemaClass)) {
                        field = new AjaxCheckBoxPanel("panel", label.getDefaultModelObjectAsString(), new Model());
                    } else {
                        field = new AjaxTextFieldPanel("panel", label.getDefaultModelObjectAsString(), new Model());

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
                        "connPropAttrOverridable", new PropertyModel(property, "overridable"));

                item.add(overridable);
                connectorTO.addConfiguration(property);
            }
        };

        final Form connectorForm = new Form("form");
        connectorForm.setModel(new CompoundPropertyModel(connectorTO));

        final Form connectorPropForm = new Form("connectorPropForm");
        connectorPropForm.setModel(new CompoundPropertyModel(connectorTO));
        connectorPropForm.setOutputMarkupId(true);

        propertiesContainer = new WebMarkupContainer("container");
        propertiesContainer.setOutputMarkupId(true);
        propertiesContainer.add(connectorPropForm);

        connectorForm.add(propertiesContainer);
        connectorPropForm.add(view);

        final AjaxLink check = new IndicatingAjaxLink("check", new ResourceModel("check")) {
            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                connectorTO.setBundleName(bundleTO.getBundleName());
                connectorTO.setVersion(bundleTO.getVersion());

                if (restClient.check(connectorTO).booleanValue()) {
                    info(getString("success_connection"));
                } else {
                    error(getString("error_connection"));
                }

                target.add(feedbackPanel);
            }
        };

        connectorPropForm.add(check);

        final AjaxButton submit = new IndicatingAjaxButton("apply", new Model<String>(getString("submit"))) {
            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ConnInstanceTO conn = (ConnInstanceTO) form.getDefaultModelObject();

                conn.setBundleName(bundleTO.getBundleName());

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

                    ((Resources) callerPageRef.getPage()).setModalResult(true);
                    window.close(target);
                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                    target.add(feedbackPanel);
                    ((Resources) callerPageRef.getPage()).setModalResult(false);
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

        connectorForm.add(connectorName);
        connectorForm.add(displayName);
        connectorForm.add(bundle);
        connectorForm.add(version);

        capabilitiesPalette = new CheckBoxMultipleChoice("capabilitiesPalette", new PropertyModel(this,
                "selectedCapabilities"), capabilities);
        connectorForm.add(capabilitiesPalette);

        connectorForm.add(submit);
        connectorForm.add(cancel);

        add(connectorForm);
        add(new CloseOnESCBehavior(window));
    }

    private ConnBundleTO getSelectedBundleTO(final List<ConnBundleTO> bundles, final ConnInstanceTO connTO) {
        // -------------------------------------
        // Manage bundle and connector beans
        // -------------------------------------

        if (connTO != null && StringUtils.isNotBlank(connTO.getBundleName())
                && StringUtils.isNotBlank(connTO.getVersion())) {

            for (ConnBundleTO to : bundles) {
                if (to.getVersion().equals(connTO.getVersion()) && to.getBundleName().equals(connTO.getBundleName())) {
                    connTO.setConnectorName(to.getConnectorName());
                    connTO.setVersion(to.getVersion());

                    return to;
                }
            }
        }

        return null;
    }

    private List<ConnConfProperty> fillProperties(final ConnBundleTO bundleTO, final ConnInstanceTO connTO) {
        // -------------------------------------
        // Manage bundle properties
        // -------------------------------------
        final List<ConnConfProperty> props = new ArrayList<ConnConfProperty>();

        if (connTO.getId() == 0 && bundleTO != null) {
            for (ConnConfPropSchema key : bundleTO.getProperties()) {
                final ConnConfProperty propertyTO = new ConnConfProperty();
                propertyTO.setSchema(key);
                props.add(propertyTO);
            }
        } else {
            props.addAll(connTO.getConfiguration());
        }

        // re-order properties
        Collections.sort(props);
        // -------------------------------------

        return props;
    }

    public List<ConnConfProperty> getProperties() {
        return properties;
    }
}
