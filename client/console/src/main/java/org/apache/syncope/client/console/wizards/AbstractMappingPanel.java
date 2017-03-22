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
package org.apache.syncope.client.console.wizards;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MappingPurposePanel;
import org.apache.syncope.client.console.widgets.JEXLTransformerWidget;
import org.apache.syncope.client.console.widgets.MappingItemTransformerWidget;
import org.apache.syncope.client.console.wizards.resources.JEXLTransformersTogglePanel;
import org.apache.syncope.client.console.wizards.resources.MappingItemTransformersTogglePanel;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.UserTO;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public abstract class AbstractMappingPanel extends Panel {

    private static final long serialVersionUID = -8295587900937040104L;

    protected static final Set<String> USER_FIELD_NAMES = new HashSet<>();

    protected static final Set<String> GROUP_FIELD_NAMES = new HashSet<>();

    protected static final Set<String> ANY_OBJECT_FIELD_NAMES = new HashSet<>();

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
    protected final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    /**
     * Any type class rest client.
     */
    protected final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    protected final Label passwordLabel;

    protected final Label purposeLabel;

    /**
     * Add mapping button.
     */
    protected final AjaxButton addMappingBtn;

    /**
     * All mappings.
     */
    protected final ListView<MappingItemTO> mappings;

    /**
     * Mapping container.
     */
    protected final WebMarkupContainer mappingContainer;

    protected final IModel<List<MappingItemTO>> model;

    public AbstractMappingPanel(
            final String id,
            final MappingItemTransformersTogglePanel mapItemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers,
            final IModel<List<MappingItemTO>> model,
            final boolean addMappingBtnVisible,
            final boolean hidePurpose,
            final MappingPurpose defaultPurpose) {

        super(id);
        setOutputMarkupId(true);

        this.model = model;

        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        add(mappingContainer);

        passwordLabel = new Label("passwordLabel", new ResourceModel("password"));
        mappingContainer.add(passwordLabel);

        purposeLabel = new Label("purposeLabel", new ResourceModel("purpose"));
        mappingContainer.add(purposeLabel);

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

        Collections.sort(model.getObject(), new Comparator<MappingItemTO>() {

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

        mappings = new ListView<MappingItemTO>("mappings", model.getObject()) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<MappingItemTO> item) {
                final MappingItemTO mapItem = item.getModelObject();
                if (mapItem.getPurpose() == null) {
                    mapItem.setPurpose(defaultPurpose);
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
                extAttrName.setChoices(getExtAttrNames().getObject());

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

                final MappingPurposePanel purposeActions = new MappingPurposePanel(
                        "purposeActions", new PropertyModel<MappingPurpose>(mapItem, "purpose"), purpose);
                purpose.add(purposeActions.setRenderBodyOnly(true));
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
                        for (int i = 0; i < model.getObject().size() && index == -1; i++) {
                            if (mapItem.equals(model.getObject().get(i))) {
                                index = i;
                            }
                        }

                        if (index != -1) {
                            model.getObject().remove(index);
                            item.getParent().removeAll();
                            target.add(AbstractMappingPanel.this);
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
                        extAttrName.setEnabled(!password.getModelObject());
                        extAttrName.setModelObject(password.getModelObject()
                                ? ConnIdSpecialName.PASSWORD : extAttrName.getModelObject());
                        extAttrName.setRequired(!password.getModelObject());
                        target.add(extAttrName);

                        setConnObjectKey(connObjectKey, password);
                        target.add(connObjectKey);
                    }
                });

                setConnObjectKey(connObjectKey, password);
                setAttrNames(intAttrName);

                if (hidePassword()) {
                    password.setVisible(false);

                    // Changes required by clone ....
                    extAttrName.setEnabled(true);
                    if (mapItem.isPassword()) {
                        // re-enable if and only if cloned object mapping item was a password
                        intAttrName.setEnabled(true);
                    }
                    mapItem.setPassword(false);
                }

                if (hidePurpose) {
                    purpose.setVisible(false);
                }
            }
        };

        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        addMappingBtn = new IndicatingAjaxButton("addMappingBtn", new ResourceModel("add")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                model.getObject().add(new MappingItemTO());
                target.add(AbstractMappingPanel.this);
            }
        };
        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(addMappingBtnVisible);
        mappingContainer.add(addMappingBtn);

    }

    protected boolean hidePassword() {
        return true;
    }

    protected abstract IModel<List<String>> getExtAttrNames();

    /**
     * Set attribute names for a drop down choice list.
     *
     * @param toBeUpdated drop down choice to be updated.
     */
    protected abstract void setAttrNames(AjaxTextFieldPanel toBeUpdated);

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        passwordLabel.setVisible(false);
        purposeLabel.setVisible(true);
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
