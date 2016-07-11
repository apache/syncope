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
package org.apache.syncope.client.console.wizards.resources;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.client.console.commons.ConnIdSpecialAttributeName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MappingPurposePanel;
import org.apache.syncope.client.console.widgets.JEXLTransformerWidget;
import org.apache.syncope.client.console.widgets.MappingItemTransformerWidget;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Resource mapping panel.
 */
public class ResourceMappingPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    private static final Set<String> USER_FIELD_NAMES = new HashSet<>();

    private static final Set<String> GROUP_FIELD_NAMES = new HashSet<>();

    private static final Set<String> ANY_OBJECT_FIELD_NAMES = new HashSet<>();

    static {
        initFieldNames(UserTO.class, USER_FIELD_NAMES);
        initFieldNames(GroupTO.class, GROUP_FIELD_NAMES);
        initFieldNames(AnyObjectTO.class, ANY_OBJECT_FIELD_NAMES);
    }

    private static void initFieldNames(final Class<?> entityClass, final Set<String> keys) {
        List<Class<?>> classes = ClassUtils.getAllSuperclasses(entityClass);
        classes.add(entityClass);
        for (Class<?> clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        && !Collection.class.isAssignableFrom(field.getType())
                        && !Map.class.isAssignableFrom(field.getType())) {

                    keys.add(field.getName());
                }
            }
        }
    }

    /**
     * Any type rest client.
     */
    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    /**
     * Any type class rest client.
     */
    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final Label passwordLabel;

    /**
     * Add mapping button.
     */
    private final AjaxButton addMappingBtn;

    /**
     * All mappings.
     */
    private final ListView<MappingItemTO> mappings;

    /**
     * External resource provisioning configuration instance to be updated.
     */
    private final ProvisionTO provisionTO;

    /**
     * Mapping container.
     */
    private final WebMarkupContainer mappingContainer;

    /**
     * Attribute Mapping Panel.
     *
     * @param id panel id
     * @param resourceTO external resource to be updated
     * @param provisionTO external resource provisioning configuration instance
     * @param mapItemTransformers mapping item transformers toggle panel
     * @param jexlTransformers JEXL transformers toggle panel
     */
    public ResourceMappingPanel(
            final String id,
            final ResourceTO resourceTO,
            final ProvisionTO provisionTO,
            final MappingItemTransformersTogglePanel mapItemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers) {

        super(id);
        setOutputMarkupId(true);

        this.provisionTO = provisionTO;
        if (provisionTO.getMapping() == null) {
            provisionTO.setMapping(new MappingTO());
        }

        final LoadableDetachableModel<List<String>> extAttrNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return new ConnectorRestClient().getExtAttrNames(
                        provisionTO.getObjectClass(),
                        resourceTO.getConnector(),
                        resourceTO.getConfOverride());
            }
        };

        this.mappingContainer = new WebMarkupContainer("mappingContainer");
        this.mappingContainer.setOutputMarkupId(true);
        add(this.mappingContainer);

        mappingContainer.add(new Label("intAttrNameInfo", Model.of()).add(new PopoverBehavior(
                Model.<String>of(),
                Model.of(getString("intAttrNameInfo.help")
                + "<div style=\"font-size: 10px;\">"
                + "<code>groups[groupName].attribute</code>\n"
                + "<code>anyObjects[anyObjectName].attribute</code>\n"
                + "<code>memberships[groupName].attribute</code>\n"
                + "</div>"),
                new PopoverConfig().withHtml(true).withPlacement(TooltipConfig.Placement.bottom)) {

            private static final long serialVersionUID = -7867802555691605021L;

            @Override
            protected String createRelAttribute() {
                return "intAttrNameInfo";
            }
        }));

        mappingContainer.add(Constants.getJEXLPopover(this, TooltipConfig.Placement.bottom));

        passwordLabel = new Label("passwordLabel", new ResourceModel("password"));
        mappingContainer.add(passwordLabel);

        Collections.sort(provisionTO.getMapping().getItems(), new Comparator<MappingItemTO>() {

            @Override
            public int compare(final MappingItemTO left, final MappingItemTO right) {
                int compared;
                if (left == null && right == null) {
                    compared = 0;
                } else if (left == null) {
                    compared = 1;
                } else if (right == null) {
                    compared = -1;
                } else if (left.isConnObjectKey()) {
                    compared = -1;
                } else if (right.isConnObjectKey()) {
                    compared = 1;
                } else if (left.isPassword()) {
                    compared = -1;
                } else if (right.isPassword()) {
                    compared = 1;
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
                } else {
                    compared = left.getIntAttrName().compareTo(right.getIntAttrName());
                }
                return compared;
            }
        });

        mappings = new ListView<MappingItemTO>("mappings", provisionTO.getMapping().getItems()) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<MappingItemTO> item) {
                final MappingItemTO mapItem = item.getModelObject();
                if (mapItem.getPurpose() == null) {
                    mapItem.setPurpose(MappingPurpose.BOTH);
                }

                //--------------------------------
                // Internal attribute
                // -------------------------------
                AjaxTextFieldPanel intAttrName = new AjaxTextFieldPanel(
                        "intAttrName",
                        getString("intAttrName"),
                        new PropertyModel<String>(mapItem, "intAttrName"),
                        false);
                intAttrName.setChoices(Collections.<String>emptyList());
                intAttrName.setRequired(true).hideLabel();
                item.add(intAttrName);
                // -------------------------------

                //--------------------------------
                // External attribute
                // -------------------------------
                final AjaxTextFieldPanel extAttrName = new AjaxTextFieldPanel(
                        "extAttrName",
                        getString("extAttrName"),
                        new PropertyModel<String>(mapItem, "extAttrName"));
                extAttrName.setChoices(extAttrNames.getObject());

                boolean required = !mapItem.isPassword();
                extAttrName.setRequired(required).hideLabel();
                extAttrName.setEnabled(required);
                item.add(extAttrName);
                // -------------------------------

                //--------------------------------
                // JEXL transformers
                // -------------------------------
                item.add(new JEXLTransformerWidget(
                        "jexlTransformers", mapItem, jexlTransformers).setRenderBodyOnly(true));
                // -------------------------------

                //--------------------------------
                // Mapping item transformers
                // -------------------------------
                item.add(new MappingItemTransformerWidget(
                        "mappingItemTransformers", mapItem, mapItemTransformers).setRenderBodyOnly(true));
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
                mandatory.setEnabled(!mapItem.isConnObjectKey());
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
                WebMarkupContainer purpose = new WebMarkupContainer("purpose");
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
                        for (int i = 0; i < provisionTO.getMapping().getItems().size() && index == -1; i++) {
                            if (mapItem.equals(provisionTO.getMapping().getItems().get(i))) {
                                index = i;
                            }
                        }

                        if (index != -1) {
                            provisionTO.getMapping().getItems().remove(index);
                            item.getParent().removeAll();
                            target.add(ResourceMappingPanel.this);
                        }
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.RESOURCE_UPDATE);
                item.add(actions.build("toRemove"));
                // -------------------------------

                intAttrName.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

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
                            mandatory.setModelObject("true");
                            mandatory.setEnabled(false);
                        } else {
                            mapItem.setMandatoryCondition("false");
                            mandatory.setModelObject("false");
                            mandatory.setEnabled(true);
                        }
                        target.add(mandatory);
                    }
                });

                password.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrName.setEnabled(!mapItem.isConnObjectKey() && !password.getModelObject());
                        extAttrName.setModelObject(password.getModelObject()
                                ? ConnIdSpecialAttributeName.PASSWORD : extAttrName.getModelObject());
                        extAttrName.setRequired(!password.getModelObject());
                        target.add(extAttrName);

                        setConnObjectKey(connObjectKey, password);
                        target.add(connObjectKey);
                    }
                });

                setConnObjectKey(connObjectKey, password);
                setAttrNames(intAttrName);

                if (!AnyTypeKind.USER.name().equals(provisionTO.getAnyType())) {
                    password.setVisible(false);

                    // Changes required by clone ....
                    extAttrName.setEnabled(!mapItem.isConnObjectKey());
                    if (mapItem.isPassword()) {
                        // re-enable if and only if cloned objec mapping item was a password
                        intAttrName.setEnabled(true);
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
                provisionTO.getMapping().getItems().add(new MappingItemTO());
                target.add(ResourceMappingPanel.this);
            }
        };
        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(resourceTO.getConnector() != null);
        mappingContainer.add(addMappingBtn);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        passwordLabel.setVisible(AnyTypeKind.USER.name().equals(this.provisionTO.getAnyType()));
    }

    /**
     * Set attribute names for a drop down choice list.
     *
     * @param type attribute type.
     * @param toBeUpdated drop down choice to be updated.
     */
    private void setAttrNames(final AjaxTextFieldPanel toBeUpdated) {
        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        AnyTypeTO anyTypeTO = anyTypeRestClient.read(provisionTO.getAnyType());

        List<AnyTypeClassTO> anyTypeClassTOs = new ArrayList<>();
        anyTypeClassTOs.addAll(anyTypeClassRestClient.list(anyTypeTO.getClasses()));
        for (String auxClass : provisionTO.getAuxClasses()) {
            anyTypeClassTOs.add(anyTypeClassRestClient.read(auxClass));
        }

        List<String> choices = new ArrayList<>();

        switch (provisionTO.getAnyType()) {
            case "USER":
                choices.addAll(USER_FIELD_NAMES);
                break;

            case "GROUP":
                choices.addAll(GROUP_FIELD_NAMES);
                break;

            default:
                choices.addAll(ANY_OBJECT_FIELD_NAMES);
        }

        for (AnyTypeClassTO anyTypeClassTO : anyTypeClassTOs) {
            choices.addAll(anyTypeClassTO.getPlainSchemas());
            choices.addAll(anyTypeClassTO.getDerSchemas());
            choices.addAll(anyTypeClassTO.getVirSchemas());
        }

        Collections.sort(choices);
        toBeUpdated.setChoices(choices);
    }

    /**
     * Enable/Disable connObjectKey checkbox.
     *
     * @param connObjectKey connObjectKey checkbox.
     * @param password password checkbox.
     */
    private void setConnObjectKey(final AjaxCheckBoxPanel connObjectKey, final AjaxCheckBoxPanel password) {
        if (password.getModelObject()) {
            connObjectKey.setReadOnly(true);
            connObjectKey.setModelObject(false);
        } else {
            connObjectKey.setReadOnly(false);
        }
    }
}
