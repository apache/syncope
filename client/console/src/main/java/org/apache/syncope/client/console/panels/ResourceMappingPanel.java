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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.JexlHelpUtils;
import org.apache.syncope.client.console.panels.ResourceConnConfPanel.ConnConfModEvent;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MappingPurposePanel;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEvent;
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
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Resource mapping panel.
 */
public class ResourceMappingPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Mapping field style sheet.
     */
    private static final String FIELD_STYLE = "ui-widget-content ui-corner-all short_fixedsize";

    /**
     * Mapping field style sheet.
     */
    private static final String DEF_FIELD_STYLE = "ui-widget-content ui-corner-all";

    /**
     * Mapping field style sheet.
     */
    private static final String SHORT_FIELD_STYLE = "ui-widget-content ui-corner-all veryshort_fixedsize";

    /**
     * Schema rest client.
     */
    @SpringBean
    private SchemaRestClient schemaRestClient;

    /**
     * ConnInstance rest client.
     */
    @SpringBean
    private ConnectorRestClient connRestClient;

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

    private final WebMarkupContainer connObjectLinkContainer;

    private final AjaxCheckBoxPanel connObjectLinkCheckbox;

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

        this.connObjectLinkContainer = new WebMarkupContainer("connObjectLinkContainer");
        this.connObjectLinkContainer.setOutputMarkupId(true);
        add(this.connObjectLinkContainer);

        if (resourceTO.getConnector() != null && resourceTO.getConnector() > 0) {
            schemaNames = getSchemaNames(resourceTO.getConnector(), resourceTO.getConnConfProperties());
            setEnabled();
        } else {
            schemaNames = Collections.<String>emptyList();
        }

        final WebMarkupContainer jexlHelp = JexlHelpUtils.getJexlHelpWebContainer("jexlHelp");

        AjaxLink<Void> questionMarkJexlHelp = JexlHelpUtils.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
        mappingContainer.add(questionMarkJexlHelp);
        questionMarkJexlHelp.add(jexlHelp);

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
                        && (right.getPurpose() == MappingPurpose.SYNCHRONIZATION
                        || right.getPurpose() == MappingPurpose.NONE)) {
                    compared = -1;
                } else if (left.getPurpose() == MappingPurpose.SYNCHRONIZATION
                        && right.getPurpose() == MappingPurpose.PROPAGATION) {
                    compared = 1;
                } else if (left.getPurpose() == MappingPurpose.SYNCHRONIZATION
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
                if (mapItem.getIntMappingType() != null) {
                    entity = mapItem.getIntMappingType().getAnyTypeKind();
                }

                item.add(new AjaxDecoratedCheckbox("toRemove", new Model<>(Boolean.FALSE)) {

                    private static final long serialVersionUID = 7170946748485726506L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
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

                    @Override
                    protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);

                        AjaxCallListener ajaxCallListener = new AjaxCallListener() {

                            private static final long serialVersionUID = 7160235486520935153L;

                            @Override
                            public CharSequence getPrecondition(final Component component) {
                                return "if (!confirm('" + getString("confirmDelete") + "')) return false;";
                            }
                        };
                        attributes.getAjaxCallListeners().add(ajaxCallListener);
                    }
                });

                final AjaxDropDownChoicePanel<String> intAttrNames = new AjaxDropDownChoicePanel<>("intAttrNames",
                        getString("intAttrNames"),
                        new PropertyModel<String>(mapItem, "intAttrName"), false);
                intAttrNames.setChoices(schemaNames);
                intAttrNames.setRequired(true);
                intAttrNames.setStyleSheet(FIELD_STYLE);
                intAttrNames.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                    }
                });
                item.add(intAttrNames);

                final List<IntMappingType> attrTypes = new ArrayList<>(getAttributeTypes(entity));
                final AjaxDropDownChoicePanel<IntMappingType> intMappingTypes = new AjaxDropDownChoicePanel<>(
                        "intMappingTypes",
                        new ResourceModel("intMappingTypes", "intMappingTypes").getObject(),
                        new PropertyModel<IntMappingType>(mapItem, "intMappingType"));
                intMappingTypes.setRequired(true);
                intMappingTypes.setChoices(attrTypes);
                intMappingTypes.setStyleSheet(FIELD_STYLE);
                item.add(intMappingTypes);

                final AjaxDropDownChoicePanel<AnyTypeKind> entitiesPanel = new AjaxDropDownChoicePanel<>("entities",
                        new ResourceModel("entities", "entities").getObject(),
                        new Model<>(entity));

                entitiesPanel.setChoices(provisionTO.getAnyType().equals(AnyTypeKind.GROUP.name())
                        ? Collections.<AnyTypeKind>singletonList(AnyTypeKind.GROUP)
                        : Arrays.asList(AnyTypeKind.values()));

                entitiesPanel.setStyleSheet(DEF_FIELD_STYLE);
                entitiesPanel.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        attrTypes.clear();
                        attrTypes.addAll(getAttributeTypes(entitiesPanel.getModelObject()));
                        intMappingTypes.setChoices(attrTypes);

                        intAttrNames.setChoices(Collections.<String>emptyList());

                        target.add(intMappingTypes.getField());
                        target.add(intAttrNames.getField());
                    }
                });
                item.add(entitiesPanel);

                final FieldPanel<String> extAttrNames = new AjaxTextFieldPanel("extAttrName",
                        new ResourceModel("extAttrNames", "extAttrNames").getObject(),
                        new PropertyModel<String>(mapItem, "extAttrName"));
                ((AjaxTextFieldPanel) extAttrNames).setChoices(schemaNames);

                boolean required = false;
                if (mapItem.isPassword()) {
                    ((AjaxTextFieldPanel) extAttrNames).setModelObject(null);
                } else {
                    required = true;
                }
                extAttrNames.setRequired(required);
                extAttrNames.setEnabled(required);
                extAttrNames.setStyleSheet(FIELD_STYLE);
                item.add(extAttrNames);

                final AjaxTextFieldPanel mandatory = new AjaxTextFieldPanel("mandatoryCondition",
                        new ResourceModel("mandatoryCondition", "mandatoryCondition").getObject(),
                        new PropertyModel<String>(mapItem, "mandatoryCondition"));
                mandatory.setChoices(Arrays.asList(new String[] { "true", "false" }));
                mandatory.setStyleSheet(SHORT_FIELD_STYLE);
                item.add(mandatory);

                final AjaxCheckBoxPanel connObjectKey = new AjaxCheckBoxPanel("connObjectKey",
                        new ResourceModel("connObjectKey", "connObjectKey").getObject(),
                        new PropertyModel<Boolean>(mapItem, "connObjectKey"));
                connObjectKey.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

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
                item.add(connObjectKey);

                final AjaxCheckBoxPanel password = new AjaxCheckBoxPanel("password",
                        new ResourceModel("password", "password").getObject(),
                        new PropertyModel<Boolean>(mapItem, "password"));
                password.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrNames.setEnabled(!mapItem.isConnObjectKey() && !password.getModelObject());
                        extAttrNames.setModelObject(null);
                        extAttrNames.setRequired(!password.getModelObject());
                        target.add(extAttrNames);

                        setConnObjectKey(intMappingTypes.getModelObject(), connObjectKey, password);
                        target.add(connObjectKey);
                    }
                });
                item.add(password);
                if (!AnyTypeKind.USER.name().equals(provisionTO.getAnyType())) {
                    password.setVisible(false);
                }

                final WebMarkupContainer purpose = new WebMarkupContainer("purpose");
                purpose.setOutputMarkupId(Boolean.TRUE);

                final MappingPurposePanel panel = new MappingPurposePanel("purposeActions",
                        new PropertyModel<MappingPurpose>(mapItem, "purpose"), purpose);

                purpose.add(panel);

                item.add(purpose);

                intMappingTypes.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        setAttrNames(intMappingTypes.getModelObject(), intAttrNames);
                        target.add(intAttrNames);

                        setConnObjectKey(intMappingTypes.getModelObject(), connObjectKey, password);
                        target.add(connObjectKey);
                    }
                });

                setAttrNames(mapItem.getIntMappingType(), intAttrNames);
                setConnObjectKey(mapItem.getIntMappingType(), connObjectKey, password);
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

        boolean connObjectLinkEnabled = false;
        if (getMapping().getConnObjectLink() != null) {
            connObjectLinkEnabled = true;
        }
        connObjectLinkCheckbox = new AjaxCheckBoxPanel("connObjectLinkCheckbox",
                new ResourceModel("connObjectLinkCheckbox", "connObjectLinkCheckbox").getObject(),
                new Model<>(connObjectLinkEnabled));
        connObjectLinkCheckbox.setEnabled(true);

        connObjectLinkContainer.add(connObjectLinkCheckbox);

        final AjaxTextFieldPanel connObjectLink = new AjaxTextFieldPanel("connObjectLink",
                new ResourceModel("connObjectLink", "connObjectLink").getObject(),
                new PropertyModel<String>(getMapping(), "connObjectLink"));
        connObjectLink.setEnabled(connObjectLinkEnabled);
        connObjectLinkContainer.add(connObjectLink);

        connObjectLinkCheckbox.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (connObjectLinkCheckbox.getModelObject()) {
                    connObjectLink.setEnabled(Boolean.TRUE);
                    connObjectLink.setModelObject("");
                } else {
                    connObjectLink.setEnabled(Boolean.FALSE);
                    connObjectLink.setModelObject("");
                }

                target.add(connObjectLink);
            }
        });
    }

    private List<String> getSchemaNames(final Long connectorId, final Set<ConnConfProperty> conf) {
        final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setKey(connectorId);
        connInstanceTO.getConfiguration().addAll(conf);

        return connRestClient.getSchemaNames(connInstanceTO);
    }

    private void setEnabled() {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setKey(resourceTO.getConnector());
        connInstanceTO.getConfiguration().addAll(resourceTO.getConnConfProperties());

        boolean enabled = provisionTO != null;

        this.mappingContainer.setEnabled(enabled);
        this.mappingContainer.setVisible(enabled);
        this.connObjectLinkContainer.setEnabled(enabled);
        this.connObjectLinkContainer.setVisible(enabled);

        if (!enabled) {
            getMapping().getItems().clear();
            getMapping().setConnObjectLink(null);
            if (this.connObjectLinkCheckbox != null) {
                this.connObjectLinkCheckbox.setModelObject(null);
            }
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ConnConfModEvent) {
            final AjaxRequestTarget target = ((ConnConfModEvent) event.getPayload()).getTarget();

            final List<ConnConfProperty> conf = ((ConnConfModEvent) event.getPayload()).getConfiguration();

            mappings.removeAll();

            addMappingBtn.setEnabled(resourceTO.getConnector() != null && resourceTO.getConnector() > 0);

            schemaNames.clear();
            schemaNames.addAll(getSchemaNames(resourceTO.getConnector(), new HashSet<ConnConfProperty>(conf)));

            setEnabled();

            target.add(this);
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
            switch (type) {
                // user attribute names
                case UserPlainSchema:
                case GroupPlainSchema:
                case AnyObjectPlainSchema:
                    toBeUpdated.setChoices(schemaRestClient.getPlainSchemaNames());
                    break;

                case UserDerivedSchema:
                case GroupDerivedSchema:
                case AnyObjectDerivedSchema:
                    toBeUpdated.setChoices(schemaRestClient.getDerSchemaNames());
                    break;

                case UserVirtualSchema:
                case GroupVirtualSchema:
                case AnyObjectVirtualSchema:
                    toBeUpdated.setChoices(schemaRestClient.getVirSchemaNames());
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
                    toBeUpdated.setChoices(Collections.<String>emptyList());
            }
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
