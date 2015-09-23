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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.console.topology.TopologyNode;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.CheckBoxMultipleChoiceFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.list.ConnConfPropertyListView;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;

/**
 * Modal window with Connector form.
 */
public class ConnectorModal extends AbstractResourceModal {

    private static final long serialVersionUID = -2025535531121434050L;

    private final Map<String, Map<String, Map<String, ConnBundleTO>>> mapConnBundleTOs;

    private final List<ConnectorCapability> selectedCapabilities;

    private ConnBundleTO bundleTO;

    private List<ConnConfProperty> properties;

    private final WebMarkupContainer propertiesContainer;

    private final ListView<ConnConfProperty> connPropView;

    private final ConnInstanceTO connInstanceTO;

    public ConnectorModal(
            final BaseModal<Serializable> modal, final PageReference pageRef, final ConnInstanceTO connInstanceTO) {

        super(modal, pageRef);

        this.connInstanceTO = connInstanceTO;

        this.add(new Label("new", connInstanceTO.getKey() == 0
                ? new ResourceModel("new")
                : new Model<>(StringUtils.EMPTY)));
        this.add(new Label("key", connInstanceTO.getKey() == 0
                ? StringUtils.EMPTY
                : connInstanceTO.getKey()));

        // general data setup
        selectedCapabilities = new ArrayList<>(connInstanceTO.getKey() == 0
                ? EnumSet.noneOf(ConnectorCapability.class)
                : connInstanceTO.getCapabilities());

        mapConnBundleTOs = new HashMap<>();
        for (ConnBundleTO connBundleTO : connectorRestClient.getAllBundles()) {
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

        propertiesContainer = new WebMarkupContainer("container");
        propertiesContainer.setOutputMarkupId(true);
        add(propertiesContainer);

        final Form<ConnInstanceTO> connectorPropForm = new Form<>("connectorPropForm");
        connectorPropForm.setModel(new CompoundPropertyModel<>(connInstanceTO));
        connectorPropForm.setOutputMarkupId(true);
        propertiesContainer.add(connectorPropForm);

        final AjaxTextFieldPanel displayName = new AjaxTextFieldPanel(
                "displayName", "display name", new PropertyModel<String>(connInstanceTO, "displayName"));
        displayName.setOutputMarkupId(true);
        displayName.addRequiredLabel();
        add(displayName);

        final AjaxDropDownChoicePanel<String> location = new AjaxDropDownChoicePanel<>("location", "location",
                new Model<>(bundleTO == null ? connInstanceTO.getLocation() : bundleTO.getLocation()));
        ((DropDownChoice<String>) location.getField()).setNullValid(true);
        location.setStyleSheet("long_dynamicsize");
        location.setChoices(new ArrayList<>(mapConnBundleTOs.keySet()));
        location.setRequired(true);
        location.addRequiredLabel();
        location.setOutputMarkupId(true);
        location.setEnabled(connInstanceTO.getKey() == 0 && StringUtils.isBlank(connInstanceTO.getLocation()));
        location.getField().setOutputMarkupId(true);
        add(location);

        final AjaxDropDownChoicePanel<String> connectorName = new AjaxDropDownChoicePanel<>("connectorName",
                "connectorName",
                new Model<>(bundleTO == null ? null : bundleTO.getBundleName()));
        ((DropDownChoice<String>) connectorName.getField()).setNullValid(true);
        connectorName.setStyleSheet("long_dynamicsize");
        connectorName.setChoices(bundleTO == null
                ? StringUtils.isBlank(connInstanceTO.getLocation())
                        ? new ArrayList<String>()
                        : new ArrayList<>(mapConnBundleTOs.get(connInstanceTO.getLocation()).keySet())
                : new ArrayList<>(mapConnBundleTOs.get(bundleTO.getLocation()).keySet()));
        connectorName.setRequired(true);
        connectorName.addRequiredLabel();
        connectorName.setOutputMarkupId(true);
        connectorName.setEnabled(connInstanceTO.getKey() == 0);
        connectorName.getField().setOutputMarkupId(true);
        add(connectorName);

        final AjaxDropDownChoicePanel<String> version = new AjaxDropDownChoicePanel<>("version", "version",
                new Model<>(bundleTO == null ? null : bundleTO.getVersion()));
        version.setStyleSheet("long_dynamicsize");
        version.setChoices(bundleTO == null
                ? new ArrayList<String>()
                : new ArrayList<>(mapConnBundleTOs.get(connInstanceTO.getLocation()).
                        get(connInstanceTO.getBundleName()).keySet()));
        version.setRequired(true);
        version.addRequiredLabel();
        version.setEnabled(connInstanceTO.getBundleName() != null);
        version.setOutputMarkupId(true);
        version.addRequiredLabel();
        version.getField().setOutputMarkupId(true);
        add(version);

        final SpinnerFieldPanel<Integer> connRequestTimeout = new SpinnerFieldPanel<>("connRequestTimeout",
                "connRequestTimeout", Integer.class,
                new PropertyModel<Integer>(connInstanceTO, "connRequestTimeout"), 0, null);
        connRequestTimeout.getField().add(new RangeValidator<>(0, Integer.MAX_VALUE));
        add(connRequestTimeout);

        if (connInstanceTO.getPoolConf() == null) {
            connInstanceTO.setPoolConf(new ConnPoolConfTO());
        }
        final SpinnerFieldPanel<Integer> poolMaxObjects = new SpinnerFieldPanel<>("poolMaxObjects", "poolMaxObjects",
                Integer.class,
                new PropertyModel<Integer>(connInstanceTO.getPoolConf(), "maxObjects"), 0, null);
        poolMaxObjects.getField().add(new RangeValidator<>(0, Integer.MAX_VALUE));
        add(poolMaxObjects);
        final SpinnerFieldPanel<Integer> poolMinIdle = new SpinnerFieldPanel<>("poolMinIdle", "poolMinIdle",
                Integer.class,
                new PropertyModel<Integer>(connInstanceTO.getPoolConf(), "minIdle"), 0, null);
        poolMinIdle.getField().add(new RangeValidator<>(0, Integer.MAX_VALUE));
        add(poolMinIdle);
        final SpinnerFieldPanel<Integer> poolMaxIdle = new SpinnerFieldPanel<>("poolMaxIdle", "poolMaxIdle",
                Integer.class,
                new PropertyModel<Integer>(connInstanceTO.getPoolConf(), "maxIdle"), 0, null);
        poolMaxIdle.getField().add(new RangeValidator<>(0, Integer.MAX_VALUE));
        add(poolMaxIdle);
        final SpinnerFieldPanel<Long> poolMaxWait = new SpinnerFieldPanel<>("poolMaxWait", "poolMaxWait", Long.class,
                new PropertyModel<Long>(connInstanceTO.getPoolConf(), "maxWait"), 0L, null);
        poolMaxWait.getField().add(new RangeValidator<>(0L, Long.MAX_VALUE));
        add(poolMaxWait);
        final SpinnerFieldPanel<Long> poolMinEvictableIdleTime = new SpinnerFieldPanel<>("poolMinEvictableIdleTime",
                "poolMinEvictableIdleTime", Long.class,
                new PropertyModel<Long>(connInstanceTO.getPoolConf(), "minEvictableIdleTimeMillis"),
                0L, null);
        poolMinEvictableIdleTime.getField().add(new RangeValidator<>(0L, Long.MAX_VALUE));
        add(poolMinEvictableIdleTime);

        // form - first tab - onchange()
        location.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ((DropDownChoice<String>) location.getField()).setNullValid(false);
                connInstanceTO.setLocation(location.getModelObject());
                target.add(location);

                connectorName.setChoices(new ArrayList<>(
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
                ((DropDownChoice<String>) connectorName.getField()).setNullValid(false);
                connInstanceTO.setBundleName(connectorName.getModelObject());
                target.add(connectorName);

                List<String> versions = new ArrayList<>(
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
        connPropView = new ConnConfPropertyListView("connectorProperties",
                new PropertyModel<List<ConnConfProperty>>(this, "properties"),
                true, connInstanceTO.getConfiguration());
        connPropView.setOutputMarkupId(true);
        connectorPropForm.add(connPropView);

        final AjaxButton check = new IndicatingAjaxButton("check", new ResourceModel("check")) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                final ConnInstanceTO conn = (ConnInstanceTO) form.getModelObject();

                // ensure that connector bundle information is in sync
                conn.setBundleName(bundleTO.getBundleName());
                conn.setVersion(bundleTO.getVersion());
                conn.setConnectorName(bundleTO.getConnectorName());

                if (connectorRestClient.check(conn)) {
                    info(getString("success_connection"));
                } else {
                    error(getString("error_connection"));
                }

                modal.getFeedbackPanel().refresh(target);
            }
        };
        connectorPropForm.add(check);

        // form - third tab (capabilities)
        final IModel<List<ConnectorCapability>> capabilities
                = new LoadableDetachableModel<List<ConnectorCapability>>() {

                    private static final long serialVersionUID = 5275935387613157437L;

                    @Override
                    protected List<ConnectorCapability> load() {
                        return Arrays.asList(ConnectorCapability.values());
                    }
                };
        CheckBoxMultipleChoiceFieldPanel<ConnectorCapability> capabilitiesPalette
                = new CheckBoxMultipleChoiceFieldPanel<>(
                        "capabilitiesPalette",
                        new PropertyModel<List<ConnectorCapability>>(this, "selectedCapabilities"), capabilities);

        capabilitiesPalette.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
            }
        });

        add(capabilitiesPalette);
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
        final List<ConnConfProperty> props = new ArrayList<>();

        if (bundleTO != null) {
            for (ConnConfPropSchema key : bundleTO.getProperties()) {
                final ConnConfProperty property = new ConnConfProperty();
                property.setSchema(key);
                if (connInstanceTO.getKey() != 0
                        && connInstanceTO.getConfigurationMap().containsKey(key.getName())
                        && connInstanceTO.getConfigurationMap().get(key.getName()).getValues() != null) {

                    property.getValues().addAll(connInstanceTO.getConfigurationMap().get(key.getName()).getValues());
                    property.setOverridable(connInstanceTO.getConfigurationMap().get(key.getName()).isOverridable());
                }

                if (property.getValues().isEmpty() && !key.getDefaultValues().isEmpty()) {
                    property.getValues().addAll(key.getDefaultValues());
                }

                props.add(property);
            }
        }

        // re-order properties (implements Comparable)
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

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        final ConnInstanceTO conn = (ConnInstanceTO) form.getModelObject();

        conn.setConnectorName(bundleTO.getConnectorName());
        conn.setBundleName(bundleTO.getBundleName());
        conn.setVersion(bundleTO.getVersion());
        conn.getConfiguration().clear();
        conn.getConfiguration().addAll(connPropView.getModelObject());

        // Set the model object's capabilities to capabilitiesPalette's converted Set
        conn.getCapabilities().clear();
        conn.getCapabilities().addAll(selectedCapabilities.isEmpty()
                ? EnumSet.noneOf(ConnectorCapability.class)
                : EnumSet.copyOf(selectedCapabilities));

        // Reset pool configuration if all fields are null
        if (conn.getPoolConf() != null
                && conn.getPoolConf().getMaxIdle() == null
                && conn.getPoolConf().getMaxObjects() == null
                && conn.getPoolConf().getMaxWait() == null
                && conn.getPoolConf().getMinEvictableIdleTimeMillis() == null
                && conn.getPoolConf().getMinIdle() == null) {

            conn.setPoolConf(null);
        }

        try {
            if (connInstanceTO.getKey() == 0) {
                connectorRestClient.create(conn);
                send(pageRef.getPage(), Broadcast.BREADTH, new CreateEvent(
                        conn.getKey(),
                        conn.getDisplayName(),
                        TopologyNode.Kind.CONNECTOR,
                        conn.getLocation().startsWith(Topology.CONNECTOR_SERVER_LOCATION_PREFIX)
                                ? conn.getLocation() : Topology.ROOT_NAME,
                        target));
            } else {
                connectorRestClient.update(conn);
            }

            ((BasePage) pageRef.getPage()).setModalResult(true);
            modal.close(target);
        } catch (SyncopeClientException e) {
            error(getString(Constants.ERROR) + ": " + e.getMessage());
            modal.getFeedbackPanel().refresh(target);
            ((BasePage) pageRef.getPage()).setModalResult(false);
            LOG.error("While creating or updating connector {}", conn, e);
        }
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        modal.getFeedbackPanel().refresh(target);
    }
}
