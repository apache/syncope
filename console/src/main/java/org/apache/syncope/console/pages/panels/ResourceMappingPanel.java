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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.console.pages.panels.ResourceConnConfPanel.ConnConfModEvent;
import org.apache.syncope.console.rest.ConnectorRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource mapping panel.
 */
public class ResourceMappingPanel extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMappingPanel.class);

    /**
     * OnChange event name.
     */
    private static final String ON_CHANGE = "onchange";

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
    private final ListView mappings;

    /**
     * External resource to be updated.
     */
    private final ResourceTO resourceTO;

    private final AttributableType attrType;

    /**
     * Mapping container.
     */
    private final WebMarkupContainer mappingContainer;

    /**
     * AccountLink container.
     */
    private final WebMarkupContainer accountLinkContainer;

    private MappingTO getMapping() {
        MappingTO result = null;

        if (AttributableType.USER == attrType) {
            if (this.resourceTO.getUmapping() == null) {
                this.resourceTO.setUmapping(new MappingTO());
            }
            result = this.resourceTO.getUmapping();
        }
        if (AttributableType.ROLE == attrType) {
            if (this.resourceTO.getRmapping() == null) {
                this.resourceTO.setRmapping(new MappingTO());
            }
            result = this.resourceTO.getRmapping();
        }

        return result;
    }

    /**
     * Attribute Mapping Panel.
     *
     * @param panelid panel id
     * @param resourceTO external resource
     * @param attrType USER / ROLE
     */
    public ResourceMappingPanel(final String panelid, final ResourceTO resourceTO, final AttributableType attrType) {
        super(panelid);
        setOutputMarkupId(true);

        this.resourceTO = resourceTO;
        this.attrType = attrType;

        if (this.resourceTO.getConnectorId() != null && this.resourceTO.getConnectorId() > 0) {
            schemaNames =
                    getResourceSchemaNames(this.resourceTO.getConnectorId(), this.resourceTO.getConnConfProperties());
        } else {
            schemaNames = Collections.<String>emptyList();
        }

        accountLinkContainer = new WebMarkupContainer("accountLinkContainer");
        accountLinkContainer.setOutputMarkupId(true);
        add(accountLinkContainer);

        boolean accountLinkEnabled = false;
        if (getMapping().getAccountLink() != null) {
            accountLinkEnabled = true;
        }
        final AjaxCheckBoxPanel accountLinkCheckbox = new AjaxCheckBoxPanel("accountLinkCheckbox",
                new ResourceModel("accountLinkCheckbox", "accountLinkCheckbox").getObject(),
                new Model<Boolean>(Boolean.valueOf(accountLinkEnabled)));
        accountLinkCheckbox.setEnabled(true);

        accountLinkContainer.add(accountLinkCheckbox);

        final AjaxTextFieldPanel accountLink = new AjaxTextFieldPanel("accountLink",
                new ResourceModel("accountLink", "accountLink").getObject(),
                new PropertyModel<String>(getMapping(), "accountLink"));
        accountLink.setEnabled(accountLinkEnabled);
        accountLinkContainer.add(accountLink);

        accountLinkCheckbox.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (accountLinkCheckbox.getModelObject()) {
                    accountLink.setEnabled(Boolean.TRUE);
                    accountLink.setModelObject("");
                } else {
                    accountLink.setEnabled(Boolean.FALSE);
                    accountLink.setModelObject("");
                }

                target.add(accountLink);
            }
        });

        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        add(mappingContainer);

        final Label passwordLabel = new Label("passwordLabel", new ResourceModel("password"));
        mappingContainer.add(passwordLabel);
        if (AttributableType.USER != ResourceMappingPanel.this.attrType) {
            passwordLabel.setVisible(false);
        }

        mappings = new ListView<MappingItemTO>("mappings", getMapping().getItems()) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<MappingItemTO> item) {
                final MappingItemTO mapItem = item.getModelObject();

                AttributableType entity = null;
                if (mapItem.getIntMappingType() != null) {
                    entity = mapItem.getIntMappingType().getAttributableType();
                }

                final List<IntMappingType> attrTypes = new ArrayList<IntMappingType>(getAttributeTypes(entity));

                item.add(new AjaxDecoratedCheckbox("toRemove", new Model<Boolean>(Boolean.FALSE)) {

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

                        final AjaxCallListener ajaxCallListener = new AjaxCallListener() {

                            private static final long serialVersionUID = 7160235486520935153L;

                            @Override
                            public CharSequence getPrecondition(final Component component) {
                                return "if (!confirm('" + getString("confirmDelete") + "')) return false;";
                            }
                        };
                        attributes.getAjaxCallListeners().add(ajaxCallListener);
                    }
                });

                final AjaxDropDownChoicePanel<String> intAttrNames = new AjaxDropDownChoicePanel<String>("intAttrNames",
                        getString("intAttrNames"), new PropertyModel<String>(mapItem, "intAttrName"));
                intAttrNames.setChoices(schemaNames);
                intAttrNames.setRequired(true);
                intAttrNames.setStyleSheet(FIELD_STYLE);
                item.add(intAttrNames);

                final AjaxDropDownChoicePanel<IntMappingType> typesPanel =
                        new AjaxDropDownChoicePanel<IntMappingType>("intMappingTypes",
                        new ResourceModel("intMappingTypes", "intMappingTypes").getObject(),
                        new PropertyModel<IntMappingType>(mapItem, "intMappingType"));
                typesPanel.setRequired(true);
                typesPanel.setChoices(attrTypes);
                typesPanel.setStyleSheet(FIELD_STYLE);
                item.add(typesPanel);

                final AjaxDropDownChoicePanel entitiesPanel = new AjaxDropDownChoicePanel("entities",
                        new ResourceModel("entities", "entities").getObject(), new Model(entity));
                entitiesPanel.setChoices(attrType == AttributableType.ROLE
                        ? Collections.singletonList(AttributableType.ROLE)
                        : Arrays.asList(AttributableType.values()));
                entitiesPanel.setStyleSheet(DEF_FIELD_STYLE);
                entitiesPanel.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        attrTypes.clear();
                        attrTypes.addAll(getAttributeTypes((AttributableType) entitiesPanel.getModelObject()));
                        typesPanel.setChoices(attrTypes);

                        intAttrNames.setChoices(Collections.<String>emptyList());

                        target.add(typesPanel.getField());
                        target.add(intAttrNames.getField());
                    }
                });
                item.add(entitiesPanel);

                final FieldPanel extAttrName;
                if (schemaNames.isEmpty()) {
                    extAttrName = new AjaxTextFieldPanel("extAttrName", new ResourceModel("extAttrNames",
                            "extAttrNames").getObject(), new PropertyModel<String>(mapItem, "extAttrName"));
                } else {
                    extAttrName = new AjaxDropDownChoicePanel<String>("extAttrName", new ResourceModel("extAttrNames",
                            "extAttrNames").getObject(), new PropertyModel(mapItem, "extAttrName"));
                    ((AjaxDropDownChoicePanel) extAttrName).setChoices(schemaNames);
                }
                boolean required = false;
                if (mapItem != null && !mapItem.isAccountid() && !mapItem.isPassword()) {
                    required = true;
                }
                extAttrName.setRequired(required);
                extAttrName.setEnabled(required);
                extAttrName.setStyleSheet(FIELD_STYLE);
                item.add(extAttrName);

                final AjaxTextFieldPanel mandatory = new AjaxTextFieldPanel("mandatoryCondition", new ResourceModel(
                        "mandatoryCondition", "mandatoryCondition").getObject(), new PropertyModel(mapItem,
                        "mandatoryCondition"));
                mandatory.setChoices(Arrays.asList(new String[]{"true", "false"}));
                mandatory.setStyleSheet(SHORT_FIELD_STYLE);
                item.add(mandatory);

                final AjaxCheckBoxPanel accountId = new AjaxCheckBoxPanel("accountId", new ResourceModel("accountId",
                        "accountId").getObject(), new PropertyModel(mapItem, "accountid"));
                accountId.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrName.setEnabled(!accountId.getModelObject() && !mapItem.isPassword());
                        extAttrName.setModelObject(null);
                        extAttrName.setRequired(!accountId.getModelObject());
                        target.add(extAttrName);
                    }
                });
                item.add(accountId);

                final AjaxCheckBoxPanel password = new AjaxCheckBoxPanel("password",
                        new ResourceModel("password", "password").getObject(), new PropertyModel(mapItem, "password"));
                password.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrName.setEnabled(!mapItem.isAccountid() && !password.getModelObject());
                        extAttrName.setModelObject(null);
                        extAttrName.setRequired(!password.getModelObject());
                        target.add(extAttrName);

                        setAccountId((IntMappingType) typesPanel.getModelObject(), accountId, password);
                        target.add(accountId);
                    }
                });
                item.add(password);
                if (AttributableType.USER != ResourceMappingPanel.this.attrType) {
                    password.setVisible(false);
                }

                typesPanel.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        setAttrNames((IntMappingType) typesPanel.getModelObject(), intAttrNames);
                        target.add(intAttrNames);

                        setAccountId((IntMappingType) typesPanel.getModelObject(), accountId, password);
                        target.add(accountId);
                    }
                });

                setAttrNames(mapItem.getIntMappingType(), intAttrNames);
                setAccountId(mapItem.getIntMappingType(), accountId, password);
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

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                // ignore errors
            }
        };
        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(this.resourceTO.getConnectorId() != null && this.resourceTO.getConnectorId() > 0);
        mappingContainer.add(addMappingBtn);
    }

    /**
     * Get resource schema names.
     *
     * @param connectorId connector id.
     * @param conf connector configuration properties.
     * @return resource schema names.
     */
    private List<String> getResourceSchemaNames(final Long connectorId, final Set<ConnConfProperty> conf) {
        final List<String> names = new ArrayList<String>();

        try {
            final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
            connInstanceTO.setId(connectorId);
            connInstanceTO.setConfiguration(conf);

            names.addAll(connRestClient.getSchemaNames(connInstanceTO));
        } catch (Exception e) {
            LOG.warn("Error retrieving resource schema names", e);
        }

        return names;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ConnConfModEvent) {
            final AjaxRequestTarget target = ((ConnConfModEvent) event.getPayload()).getTarget();

            final List<ConnConfProperty> conf = ((ConnConfModEvent) event.getPayload()).getConfiguration();

            mappings.removeAll();

            addMappingBtn.setEnabled(resourceTO.getConnectorId() != null && resourceTO.getConnectorId() > 0);

            schemaNames.clear();
            schemaNames.addAll(
                    getResourceSchemaNames(resourceTO.getConnectorId(), new HashSet<ConnConfProperty>(conf)));

            target.add(this);
        }
    }

    /**
     * Set attribute names for a drop down chice list.
     *
     * @param type attribute type.
     * @param toBeUpdated drop down choice to be updated.
     */
    private void setAttrNames(final IntMappingType type, final AjaxDropDownChoicePanel toBeUpdated) {

        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        if (type == null || type.getAttributableType() == null) {
            toBeUpdated.setChoices(Collections.emptyList());
        } else {

            switch (type) {
                // user attribute names
                case UserSchema:
                case RoleSchema:
                case MembershipSchema:
                    toBeUpdated.setChoices(schemaRestClient.getSchemaNames(type.getAttributableType()));
                    break;

                case UserDerivedSchema:
                case RoleDerivedSchema:
                case MembershipDerivedSchema:
                    toBeUpdated.setChoices(schemaRestClient.getDerivedSchemaNames(type.getAttributableType()));
                    break;

                case UserVirtualSchema:
                case RoleVirtualSchema:
                case MembershipVirtualSchema:
                    toBeUpdated.setChoices(schemaRestClient.getVirtualSchemaNames(type.getAttributableType()));
                    break;

                case UserId:
                case Password:
                case Username:
                case RoleId:
                case RoleName:
                default:
                    toBeUpdated.setRequired(false);
                    toBeUpdated.setEnabled(false);
                    toBeUpdated.setChoices(Collections.emptyList());
            }
        }
    }

    /**
     * Enable/Disable accountId checkbox.
     *
     * @param type attribute type.
     * @param accountId accountId checkbox.
     * @param password password checkbox.
     */
    private void setAccountId(final IntMappingType type, final AjaxCheckBoxPanel accountId,
            final AjaxCheckBoxPanel password) {

        if (type != null && type.getAttributableType() != null) {
            switch (type) {
                case UserVirtualSchema:
                case RoleVirtualSchema:
                case MembershipVirtualSchema:
                // Virtual accountId is not permitted
                case Password:
                    // AccountId cannot be derived from password.
                    accountId.setReadOnly(true);
                    accountId.setModelObject(false);
                    break;

                default:
                    if (password.getModelObject()) {
                        accountId.setReadOnly(true);
                        accountId.setModelObject(false);
                    } else {
                        accountId.setReadOnly(false);
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
    private List<IntMappingType> getAttributeTypes(final AttributableType entity) {
        final List<IntMappingType> res = new ArrayList<IntMappingType>();

        if (entity != null) {
            res.addAll(IntMappingType.getAttributeTypes(AttributableType.valueOf(entity.name())));
        }

        return res;
    }
}
