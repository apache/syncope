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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.console.commons.ConnIdSpecialAttributeName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MappingPurposePanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Resource mapping panel.
 */
public class ResourceMappingPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Mapping field style sheet.
     */
    private static final String DEF_FIELD_STYLE = "";

    /**
     * Any type rest client.
     */
    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    /**
     * Any type class rest client.
     */
    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    /**
     * ConnInstance rest client.
     */
    private final ConnectorRestClient connRestClient = new ConnectorRestClient();

    /**
     * Resource schema name.
     */
    private final List<String> schemaNames;

    /**
     * Add mapping button.
     */
    private final AjaxButton addMappingBtn;

    /**
     * All mappings.
     */
    private final ListView<MappingItemTO> mappings;

    /**
     * External resource to be updated.
     */
    private final ResourceTO resourceTO;

    /**
     * External resource provisioning configuration instance to be updated.
     */
    private final ProvisionTO provisionTO;

    /**
     * Mapping container.
     */
    private final WebMarkupContainer mappingContainer;

    private MappingTO getMapping() {
        if (provisionTO.getMapping() == null) {
            provisionTO.setMapping(new MappingTO());
        }

        return provisionTO.getMapping();
    }

    /**
     * Attribute Mapping Panel.
     *
     * @param id panel id
     * @param resourceTO external resource to be updated.
     * @param provisionTO external resource provisioning configuration instance.
     */
    public ResourceMappingPanel(final String id, final ResourceTO resourceTO, final ProvisionTO provisionTO) {
        super(id);
        setOutputMarkupId(true);

        this.resourceTO = resourceTO;
        this.provisionTO = provisionTO == null ? new ProvisionTO() : provisionTO;

        this.mappingContainer = new WebMarkupContainer("mappingContainer");
        this.mappingContainer.setOutputMarkupId(true);
        add(this.mappingContainer);

        if (resourceTO.getConnector() != null && resourceTO.getConnector() > 0) {
            schemaNames = getSchemaNames(resourceTO.getConnector(), resourceTO.getConfOverride());
            setEnabled();
        } else {
            schemaNames = Collections.<String>emptyList();
        }

        mappingContainer.add(Constants.getJEXLPopover(this, TooltipConfig.Placement.bottom));

        final Label passwordLabel = new Label("passwordLabel", new ResourceModel("password"));
        mappingContainer.add(passwordLabel);

        if (!AnyTypeKind.USER.name().equals(this.provisionTO.getAnyType())) {
            passwordLabel.setVisible(false);
        }

        Collections.sort(getMapping().getItems(), new Comparator<MappingItemTO>() {

            @Override
            public int compare(final MappingItemTO left, final MappingItemTO right) {
                int compared;
                if (left == null && right == null) {
                    compared = 0;
                } else if (left == null) {
                    compared = 1;
                } else if (right == null) {
                    compared = -1;
                } else if (left.getPurpose() == MappingPurpose.BOTH && right.getPurpose() != MappingPurpose.BOTH) {
                    compared = -1;
                } else if (left.getPurpose() != MappingPurpose.BOTH && right.getPurpose() == MappingPurpose.BOTH) {
                    compared = 1;
                } else if (left.getPurpose() == MappingPurpose.PROPAGATION
                        && (right.getPurpose() == MappingPurpose.PULL
                        || right.getPurpose() == MappingPurpose.NONE)) {
                    compared = -1;
                } else if (left.getPurpose() == MappingPurpose.PULL
                        && right.getPurpose() == MappingPurpose.PROPAGATION) {
                    compared = 1;
                } else if (left.getPurpose() == MappingPurpose.PULL
                        && right.getPurpose() == MappingPurpose.NONE) {
                    compared = -1;
                } else if (left.getPurpose() == MappingPurpose.NONE
                        && right.getPurpose() != MappingPurpose.NONE) {
                    compared = 1;
                } else if (left.isConnObjectKey()) {
                    compared = -1;
                } else if (right.isConnObjectKey()) {
                    compared = 1;
                } else if (left.isPassword()) {
                    compared = -1;
                } else if (right.isPassword()) {
                    compared = 1;
                } else {
                    compared = left.getIntAttrName().compareTo(right.getIntAttrName());
                }
                return compared;
            }
        });

        mappings = new ListView<MappingItemTO>("mappings", getMapping().getItems()) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<MappingItemTO> item) {
                final MappingItemTO mapItem = item.getModelObject();
                if (mapItem.getPurpose() == null) {
                    mapItem.setPurpose(MappingPurpose.BOTH);
                }

                AnyTypeKind entity = null;
                if (provisionTO.getAnyType().equals(AnyTypeKind.GROUP.name())) {
                    // support for clone
                    entity = AnyTypeKind.GROUP;
                } else if (mapItem.getIntMappingType() != null) {
                    entity = mapItem.getIntMappingType().getAnyTypeKind();
                }

                // it will happen just in case of clone to create a new mapping for group object
                if (mapItem.getIntMappingType() != null && mapItem.getIntMappingType().getAnyTypeKind() != entity) {
                    mapItem.setIntMappingType(null);
                    mapItem.setIntAttrName(null);
                }

                //--------------------------------
                // Entity
                // -------------------------------
                final AjaxDropDownChoicePanel<AnyTypeKind> entitiesPanel = new AjaxDropDownChoicePanel<>(
                        "entities",
                        new ResourceModel("entities", "entities").getObject(),
                        new Model<>(entity));

                entitiesPanel.hideLabel();
                entitiesPanel.setChoices(provisionTO.getAnyType().equals(AnyTypeKind.GROUP.name())
                        ? Collections.<AnyTypeKind>singletonList(AnyTypeKind.GROUP)
                        : Arrays.asList(AnyTypeKind.values()));

                entitiesPanel.setStyleSheet(false, DEF_FIELD_STYLE);
                item.add(entitiesPanel);
                // -------------------------------

                //--------------------------------
                // Internal attribute type
                // -------------------------------
                final List<IntMappingType> attrTypes = new ArrayList<>(getAttributeTypes(entity));
                final AjaxDropDownChoicePanel<IntMappingType> intMappingTypes = new AjaxDropDownChoicePanel<>(
                        "intMappingTypes",
                        new ResourceModel("intMappingTypes", "intMappingTypes").getObject(),
                        new PropertyModel<IntMappingType>(mapItem, "intMappingType"),
                        false);
                intMappingTypes.setNullValid(true).setRequired(true).hideLabel();
                intMappingTypes.setChoices(attrTypes);
                item.add(intMappingTypes);
                // -------------------------------

                //--------------------------------
                // Internal attribute
                // -------------------------------
                final AjaxDropDownChoicePanel<String> intAttrNames = new AjaxDropDownChoicePanel<>(
                        "intAttrNames",
                        getString("intAttrNames"),
                        new PropertyModel<String>(mapItem, "intAttrName"),
                        false);
                intAttrNames.setChoices(Collections.<String>emptyList());
                intAttrNames.setNullValid(true).setRequired(true).hideLabel();
                item.add(intAttrNames);
                // -------------------------------

                //--------------------------------
                // External attribute
                // -------------------------------
                final AjaxTextFieldPanel extAttrNames = new AjaxTextFieldPanel(
                        "extAttrName",
                        new ResourceModel("extAttrNames", "extAttrNames").getObject(),
                        new PropertyModel<String>(mapItem, "extAttrName"));
                extAttrNames.setChoices(schemaNames);

                boolean required = !mapItem.isPassword();
                extAttrNames.setRequired(required).hideLabel();
                extAttrNames.setEnabled(required);
                item.add(extAttrNames);
                // -------------------------------

                //--------------------------------
                // Mandatory
                // -------------------------------
                final AjaxTextFieldPanel mandatory = new AjaxTextFieldPanel(
                        "mandatoryCondition",
                        new ResourceModel("mandatoryCondition", "mandatoryCondition").getObject(),
                        new PropertyModel<String>(mapItem, "mandatoryCondition"));
                mandatory.hideLabel();
                mandatory.setChoices(Arrays.asList(new String[] { "true", "false" }));
                item.add(mandatory);
                // -------------------------------

                //--------------------------------
                // Connector object key
                // -------------------------------
                final AjaxCheckBoxPanel connObjectKey = new AjaxCheckBoxPanel(
                        "connObjectKey",
                        new ResourceModel("connObjectKey", "connObjectKey").getObject(),
                        new PropertyModel<Boolean>(mapItem, "connObjectKey"), false);
                connObjectKey.hideLabel();
                item.add(connObjectKey);
                // -------------------------------

                //--------------------------------
                // Password
                // -------------------------------
                final AjaxCheckBoxPanel password = new AjaxCheckBoxPanel(
                        "password",
                        new ResourceModel("password", "password").getObject(),
                        new PropertyModel<Boolean>(mapItem, "password"), false);
                item.add(password.hideLabel());
                // -------------------------------

                //--------------------------------
                // Purpose
                // -------------------------------
                final WebMarkupContainer purpose = new WebMarkupContainer("purpose");
                purpose.setOutputMarkupId(Boolean.TRUE);

                final MappingPurposePanel panel = new MappingPurposePanel(
                        "purposeActions", new PropertyModel<MappingPurpose>(mapItem, "purpose"), purpose);

                purpose.add(panel.setRenderBodyOnly(true));
                item.add(purpose);
                // -------------------------------

                //--------------------------------
                // Remove
                // -------------------------------
                final ActionLinksPanel.Builder<Serializable> actions = ActionLinksPanel.builder();
                actions.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        int index = -1;
                        for (int i = 0; i < getMapping().getItems().size() && index == -1; i++) {
                            if (mapItem.equals(getMapping().getItems().get(i))) {
                                index = i;
                            }
                        }

                        if (index != -1) {
                            getMapping().getItems().remove(index);
                            item.getParent().removeAll();
                            target.add(ResourceMappingPanel.this);
                        }
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.RESOURCE_UPDATE);
                item.add(actions.build("toRemove"));
                // -------------------------------

                entitiesPanel.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        attrTypes.clear();
                        attrTypes.addAll(getAttributeTypes(entitiesPanel.getModelObject()));
                        intMappingTypes.setChoices(attrTypes);

                        intAttrNames.setChoices(Collections.<String>emptyList());

                        target.add(intMappingTypes);
                        target.add(intAttrNames);
                    }
                });

                intMappingTypes.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        setAttrNames(intMappingTypes.getModelObject(), intAttrNames);
                        target.add(intAttrNames);

                        setConnObjectKey(intMappingTypes.getModelObject(), connObjectKey, password);
                        target.add(connObjectKey);
                    }
                });

                intAttrNames.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                    }
                });

                connObjectKey.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        if (connObjectKey.getModelObject()) {
                            mapItem.setMandatoryCondition("true");
                            mandatory.setEnabled(false);
                        } else {
                            mapItem.setMandatoryCondition("false");
                            mandatory.setEnabled(true);
                        }
                        target.add(mandatory);
                    }
                });

                password.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrNames.setEnabled(!mapItem.isConnObjectKey() && !password.getModelObject());
                        extAttrNames.setModelObject(password.getModelObject()
                                ? ConnIdSpecialAttributeName.PASSWORD : extAttrNames.getModelObject());
                        extAttrNames.setRequired(!password.getModelObject());
                        target.add(extAttrNames);

                        setConnObjectKey(intMappingTypes.getModelObject(), connObjectKey, password);
                        target.add(connObjectKey);
                    }
                });

                setAttrNames(mapItem.getIntMappingType(), intAttrNames);
                setConnObjectKey(mapItem.getIntMappingType(), connObjectKey, password);

                if (!AnyTypeKind.USER.name().equals(provisionTO.getAnyType())) {
                    password.setVisible(false);

                    // Changes required by clone ....
                    extAttrNames.setEnabled(!mapItem.isConnObjectKey());
                    if (mapItem.isPassword()) {
                        // re-enable if and only if cloned objec mapping item was a password
                        intAttrNames.setEnabled(true);
                    }
                    mapItem.setPassword(false);
                }
            }
        };

        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        addMappingBtn = new IndicatingAjaxButton("addMappingBtn", new ResourceModel("add")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                getMapping().getItems().add(new MappingItemTO());
                target.add(ResourceMappingPanel.this);
            }
        };
        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(resourceTO.getConnector() != null && resourceTO.getConnector() > 0);
        mappingContainer.add(addMappingBtn);
    }

    private List<String> getSchemaNames(final Long connectorId, final Set<ConnConfProperty> conf) {
        final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setKey(connectorId);
        connInstanceTO.getConf().addAll(conf);

        // SYNCOPE-156: use provided info to give schema names (and type!) by ObjectClass
        ConnIdObjectClassTO clazz = IterableUtils.find(
                connRestClient.buildObjectClassInfo(connInstanceTO, true), new Predicate<ConnIdObjectClassTO>() {

            @Override
            public boolean evaluate(final ConnIdObjectClassTO object) {
                return object.getType().equalsIgnoreCase(ResourceMappingPanel.this.provisionTO.getObjectClass());
            }
        });

        return clazz == null ? new ArrayList<String>()
                : IterableUtils.toList(IterableUtils.filteredIterable(clazz.getAttributes(), new Predicate<String>() {

                    @Override
                    public boolean evaluate(final String object) {
                        return !(ConnIdSpecialAttributeName.NAME.equals(object)
                                || ConnIdSpecialAttributeName.ENABLE.equals(object)
                                || ConnIdSpecialAttributeName.PASSWORD.equals(object));
                    }
                }));
    }

    private void setEnabled() {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setKey(resourceTO.getConnector());
        connInstanceTO.getConf().addAll(resourceTO.getConfOverride());

        boolean enabled = provisionTO != null;

        this.mappingContainer.setEnabled(enabled);
        this.mappingContainer.setVisible(enabled);

        if (!enabled) {
            getMapping().getItems().clear();
            getMapping().setConnObjectLink(null);
        }
    }

    /**
     * Set attribute names for a drop down choice list.
     *
     * @param type attribute type.
     * @param toBeUpdated drop down choice to be updated.
     */
    private void setAttrNames(final IntMappingType type, final AjaxDropDownChoicePanel<String> toBeUpdated) {

        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        if (type == null || type.getAnyTypeKind() == null) {
            toBeUpdated.setChoices(Collections.<String>emptyList());
        } else {
            Collection<AnyTypeTO> anyTypeTOs = type.getAnyTypeKind() == AnyTypeKind.ANY_OBJECT
                    ? CollectionUtils.select(anyTypeRestClient.list(), new Predicate<AnyTypeTO>() {

                        @Override
                        public boolean evaluate(final AnyTypeTO object) {
                            return object.getKind() == AnyTypeKind.ANY_OBJECT;
                        }
                    })
                    : Collections.singletonList(anyTypeRestClient.get(type.getAnyTypeKind().name()));

            final List<AnyTypeClassTO> anyTypeClassTOs = new ArrayList<>();
            for (AnyTypeTO anyTypeTO : anyTypeTOs) {
                anyTypeClassTOs.addAll(anyTypeClassRestClient.list(anyTypeTO.getClasses()));
            }

            List<String> choices;

            switch (type) {
                // user attribute names
                case UserPlainSchema:
                case GroupPlainSchema:
                case AnyObjectPlainSchema:
                    final Set<String> plains = new HashSet<>();
                    for (AnyTypeClassTO anyTypeClassTO : anyTypeClassTOs) {
                        plains.addAll(anyTypeClassTO.getPlainSchemas());
                    }
                    choices = new ArrayList<>(plains);
                    break;

                case UserDerivedSchema:
                case GroupDerivedSchema:
                case AnyObjectDerivedSchema:
                    final Set<String> deriveds = new HashSet<>();
                    for (AnyTypeClassTO anyTypeClassTO : anyTypeClassTOs) {
                        deriveds.addAll(anyTypeClassTO.getDerSchemas());
                    }
                    choices = new ArrayList<>(deriveds);
                    break;

                case UserVirtualSchema:
                case GroupVirtualSchema:
                case AnyObjectVirtualSchema:
                    final Set<String> virtuals = new HashSet<>();
                    for (AnyTypeClassTO anyTypeClassTO : anyTypeClassTOs) {
                        virtuals.addAll(anyTypeClassTO.getVirSchemas());
                    }
                    choices = new ArrayList<>(virtuals);
                    break;

                case UserKey:
                case Password:
                case Username:
                case GroupKey:
                case GroupName:
                case AnyObjectKey:
                default:
                    toBeUpdated.setRequired(false);
                    toBeUpdated.setEnabled(false);
                    choices = Collections.<String>emptyList();
            }
            Collections.sort(choices);
            toBeUpdated.setChoices(choices);
        }
    }

    /**
     * Enable/Disable connObjectKey checkbox.
     *
     * @param type attribute type.
     * @param connObjectKey connObjectKey checkbox.
     * @param password password checkbox.
     */
    private void setConnObjectKey(
            final IntMappingType type, final AjaxCheckBoxPanel connObjectKey, final AjaxCheckBoxPanel password) {

        if (type != null && type.getAnyTypeKind() != null) {
            switch (type) {
                case UserVirtualSchema:
                case GroupVirtualSchema:
                case AnyObjectVirtualSchema:
                // Virtual connObjectKey is not permitted
                case Password:
                    // connObjectKey cannot be derived from password.
                    connObjectKey.setReadOnly(true);
                    connObjectKey.setModelObject(false);
                    break;

                default:
                    if (password.getModelObject()) {
                        connObjectKey.setReadOnly(true);
                        connObjectKey.setModelObject(false);
                    } else {
                        connObjectKey.setReadOnly(false);
                    }
            }
        }
    }

    /**
     * Get all attribute types from a selected attribute type.
     *
     * @param entity entity.
     * @return all attribute types.
     */
    private List<IntMappingType> getAttributeTypes(final AnyTypeKind entity) {
        final List<IntMappingType> res = new ArrayList<>();

        if (entity != null) {
            res.addAll(IntMappingType.getAttributeTypes(entity));
        }

        return res;
    }
}
