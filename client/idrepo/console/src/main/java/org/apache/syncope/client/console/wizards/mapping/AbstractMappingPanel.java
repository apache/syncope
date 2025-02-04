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
package org.apache.syncope.client.console.wizards.mapping;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.widgets.ItemTransformerWidget;
import org.apache.syncope.client.console.widgets.JEXLTransformerWidget;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public abstract class AbstractMappingPanel extends Panel {

    private static final long serialVersionUID = -8295587900937040104L;

    protected static final Comparator<Item> ITEM_COMPARATOR = (left, right) -> {
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
    };

    protected final Label connObjectKeyLabel;

    protected final Label passwordLabel;

    protected final Label purposeLabel;

    protected final Label intAttrNameInfo;

    protected final WebMarkupContainer mandatoryHeader;

    /**
     * Add mapping button.
     */
    protected final AjaxButton addMappingBtn;

    /**
     * All mappings.
     */
    protected final ListView<Item> mappings;

    /**
     * Mapping container.
     */
    protected final WebMarkupContainer mappingContainer;

    public AbstractMappingPanel(
            final String id,
            final ItemTransformersTogglePanel itemTransformers,
            final JEXLTransformersTogglePanel jexlTransformers,
            final IModel<List<Item>> model,
            final boolean addMappingBtnVisible,
            final MappingPurpose defaultPurpose) {

        super(id);
        setOutputMarkupId(true);

        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        add(mappingContainer);

        mappingContainer.add(new Label("itemTransformersLabel", Model.of()).setVisible(itemTransformers != null));

        mappingContainer.add(new Label("jexlTransformersLabel", Model.of()).setVisible(jexlTransformers != null));

        connObjectKeyLabel = new Label("connObjectKeyLabel", new ResourceModel("connObjectKey"));
        mappingContainer.add(connObjectKeyLabel);

        passwordLabel = new Label("passwordLabel", new ResourceModel("password"));
        mappingContainer.add(passwordLabel);

        purposeLabel = new Label("purposeLabel", new ResourceModel("purpose"));
        mappingContainer.add(purposeLabel);

        intAttrNameInfo = new Label("intAttrNameInfo", Model.of());
        intAttrNameInfo.add(new PopoverBehavior(
                Model.of(),
                Model.of(getString("intAttrNameInfo.help")
                        + "<code>groups[groupName].attribute</code>, "
                        + "<code>users[userName].attribute</code>, "
                        + "<code>anyObjects[anyObjectName].attribute</code>, "
                        + "<code>relationships[relationshipType][anyType].attribute</code> or "
                        + "<code>memberships[groupName].attribute</code>"),
                new PopoverConfig().withHtml(true).withPlacement(TooltipConfig.Placement.right)) {

            private static final long serialVersionUID = -7867802555691605021L;

            @Override
            protected String createRelAttribute() {
                return "intAttrNameInfo";
            }
        });
        mappingContainer.add(intAttrNameInfo);

        mandatoryHeader = new WebMarkupContainer("mandatoryHeader");
        mandatoryHeader.setOutputMarkupId(true);
        mandatoryHeader.add(Constants.getJEXLPopover(this, TooltipConfig.Placement.bottom));
        mappingContainer.add(mandatoryHeader);

        model.getObject().sort(ITEM_COMPARATOR);

        mappings = new ListView<>("mappings", model) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<Item> item) {
                final Item itemTO = item.getModelObject();
                if (itemTO.getPurpose() == null) {
                    itemTO.setPurpose(defaultPurpose);
                }

                //--------------------------------
                // Internal attribute
                // -------------------------------
                AjaxTextFieldPanel intAttrName = new AjaxTextFieldPanel(
                        "intAttrName",
                        "intAttrName",
                        new PropertyModel<>(itemTO, "intAttrName"),
                        false);
                intAttrName.setChoices(List.of());
                intAttrName.setRequired(true).hideLabel();
                item.add(intAttrName);
                // -------------------------------

                //--------------------------------
                // External attribute
                // -------------------------------
                AjaxTextFieldPanel extAttrName = new AjaxTextFieldPanel(
                        "extAttrName",
                        "extAttrName",
                        new PropertyModel<>(itemTO, "extAttrName"));
                extAttrName.setChoices(getExtAttrNames().getObject());

                boolean required = !itemTO.isPassword();
                extAttrName.setRequired(required).hideLabel();
                extAttrName.setEnabled(required);
                item.add(extAttrName);
                // -------------------------------

                //--------------------------------
                // JEXL transformers
                // -------------------------------
                if (jexlTransformers == null) {
                    item.add(new Label("jexlTransformers").setVisible(false));
                } else {
                    item.add(new JEXLTransformerWidget(
                            "jexlTransformers", itemTO, jexlTransformers).setRenderBodyOnly(true));
                }
                // -------------------------------

                //--------------------------------
                // Mapping item transformers
                // -------------------------------
                if (itemTransformers == null) {
                    item.add(new Label("itemTransformers").setVisible(false));

                } else {
                    item.add(new ItemTransformerWidget(
                            "itemTransformers", itemTO, itemTransformers).setRenderBodyOnly(true));
                }
                // -------------------------------

                //--------------------------------
                // Mandatory
                // -------------------------------
                AjaxTextFieldPanel mandatory = new AjaxTextFieldPanel(
                        "mandatoryCondition",
                        "mandatoryCondition",
                        new PropertyModel<>(itemTO, "mandatoryCondition"));
                mandatory.hideLabel();
                mandatory.setChoices(List.of("true", "false"));
                mandatory.setEnabled(!itemTO.isConnObjectKey());
                item.add(mandatory);
                // -------------------------------

                //--------------------------------
                // Connector object key
                // -------------------------------
                AjaxCheckBoxPanel connObjectKey = new AjaxCheckBoxPanel(
                        "connObjectKey",
                        "connObjectKey",
                        new PropertyModel<>(itemTO, "connObjectKey"), false);
                connObjectKey.hideLabel();
                item.add(connObjectKey);
                // -------------------------------

                //--------------------------------
                // Password
                // -------------------------------
                AjaxCheckBoxPanel password = new AjaxCheckBoxPanel(
                        "password",
                        "password",
                        new PropertyModel<>(itemTO, "password"), false);
                item.add(password.hideLabel());
                // -------------------------------

                //--------------------------------
                // Purpose
                // -------------------------------
                WebMarkupContainer purpose = new WebMarkupContainer("purpose");
                purpose.setOutputMarkupId(true);

                MappingPurposePanel purposeActions = new MappingPurposePanel(
                        "purposeActions", new PropertyModel<>(itemTO, "purpose"), purpose);
                purpose.add(purposeActions.setRenderBodyOnly(true));
                item.add(purpose);
                // -------------------------------

                //--------------------------------
                // Remove
                // -------------------------------
                ActionsPanel<Serializable> actions = new ActionsPanel<>("toRemove", null);
                actions.add(new ActionLink<>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        model.getObject().remove(item.getIndex());
                        item.getParent().removeAll();
                        target.add(AbstractMappingPanel.this);
                    }
                }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true).hideLabel();
                item.add(actions);
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
                            itemTO.setMandatoryCondition("true");
                            mandatory.setModelObject("true");
                            mandatory.setEnabled(false);
                        } else {
                            itemTO.setMandatoryCondition("false");
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
                    if (itemTO.isPassword()) {
                        // re-enable if and only if cloned object mapping item was a password
                        intAttrName.setEnabled(true);
                    }
                    itemTO.setPassword(false);
                }

                purpose.setVisible(!hidePurpose());

                mandatory.setVisible(!hideMandatory());

                connObjectKey.setVisible(!hideConnObjectKey());
            }
        };

        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        addMappingBtn = new IndicatingAjaxButton("addMappingBtn") {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                model.getObject().add(new Item());
                target.add(AbstractMappingPanel.this);
            }
        };
        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(addMappingBtnVisible);
        mappingContainer.add(addMappingBtn);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        passwordLabel.setVisible(!hidePassword());
        purposeLabel.setVisible(!hidePurpose());
        mandatoryHeader.setVisible(!hideMandatory());
        connObjectKeyLabel.setVisible(!hideConnObjectKey());
    }

    protected boolean hidePassword() {
        return true;
    }

    protected boolean hidePurpose() {
        return false;
    }

    protected boolean hideMandatory() {
        return false;
    }

    protected boolean hideConnObjectKey() {
        return false;
    }

    protected abstract IModel<List<String>> getExtAttrNames();

    /**
     * Set attribute names for a drop down choice list.
     *
     * @param toBeUpdated drop down choice to be updated.
     */
    protected abstract void setAttrNames(AjaxTextFieldPanel toBeUpdated);

    /**
     * Enable/Disable connObjectKey checkbox.
     *
     * @param connObjectKey connObjectKey checkbox.
     * @param password password checkbox.
     */
    protected void setConnObjectKey(final AjaxCheckBoxPanel connObjectKey, final AjaxCheckBoxPanel password) {
        if (password.getModelObject()) {
            connObjectKey.setReadOnly(true);
            connObjectKey.setModelObject(false);
        } else {
            connObjectKey.setReadOnly(false);
        }
    }
}
