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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.to.ConnIdObjectClassTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.JexlHelpUtil;
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
     * User / role.
     */
    private final AttributableType attrType;

    /**
     * Mapping container.
     */
    private final WebMarkupContainer mappingContainer;

    /**
     * AccountLink container.
     */
    private final WebMarkupContainer accountLinkContainer;

    private final AjaxCheckBoxPanel accountLinkCheckbox;

    private MappingTO getMapping() {
        MappingTO result = null;

        if (AttributableType.USER == this.attrType) {
            if (this.resourceTO.getUmapping() == null) {
                this.resourceTO.setUmapping(new MappingTO());
            }
            result = this.resourceTO.getUmapping();
        }
        if (AttributableType.ROLE == this.attrType) {
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
     * @param id panel id
     * @param resourceTO external resource
     * @param attrType USER / ROLE
     */
    public ResourceMappingPanel(final String id, final ResourceTO resourceTO, final AttributableType attrType) {
        super(id);
        setOutputMarkupId(true);

        this.resourceTO = resourceTO;
        this.attrType = attrType;

        this.mappingContainer = new WebMarkupContainer("mappingContainer");
        this.mappingContainer.setOutputMarkupId(true);
        add(this.mappingContainer);

        this.accountLinkContainer = new WebMarkupContainer("accountLinkContainer");
        this.accountLinkContainer.setOutputMarkupId(true);
        add(this.accountLinkContainer);

        if (this.resourceTO.getConnectorId() != null && this.resourceTO.getConnectorId() > 0) {
            schemaNames = getSchemaNames(this.resourceTO.getConnectorId(), this.resourceTO.getConnConfProperties());

            setEnabled();
        } else {
            schemaNames = Collections.<String>emptyList();
        }

        final WebMarkupContainer jexlHelp = JexlHelpUtil.getJexlHelpWebContainer("jexlHelp");
        mappingContainer.add(jexlHelp);

        AjaxLink<Void> questionMarkJexlHelp = JexlHelpUtil.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
        mappingContainer.add(questionMarkJexlHelp);

        final Label passwordLabel = new Label("passwordLabel", new ResourceModel("password"));
        mappingContainer.add(passwordLabel);
        if (AttributableType.USER != ResourceMappingPanel.this.attrType) {
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
                        && right.getPurpose() == MappingPurpose.SYNCHRONIZATION) {
                    compared = -1;
                } else if (left.getPurpose() == MappingPurpose.SYNCHRONIZATION
                        && right.getPurpose() == MappingPurpose.PROPAGATION) {
                    compared = 1;
                } else if (left.isAccountid()) {
                    compared = -1;
                } else if (right.isAccountid()) {
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

                final AjaxDropDownChoicePanel<String> intAttrNames =
                        new AjaxDropDownChoicePanel<String>("intAttrNames", getString("intAttrNames"),
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

                final AjaxDropDownChoicePanel<IntMappingType> intMappingTypes =
                        new AjaxDropDownChoicePanel<IntMappingType>("intMappingTypes",
                        new ResourceModel("intMappingTypes", "intMappingTypes").getObject(),
                        new PropertyModel<IntMappingType>(mapItem, "intMappingType"));
                intMappingTypes.setRequired(true);
                intMappingTypes.setChoices(attrTypes);
                intMappingTypes.setStyleSheet(FIELD_STYLE);
                item.add(intMappingTypes);

                final AjaxDropDownChoicePanel<AttributableType> entitiesPanel =
                        new AjaxDropDownChoicePanel<AttributableType>("entities",
                        new ResourceModel("entities", "entities").getObject(), new Model<AttributableType>(entity));
                entitiesPanel.setChoices(attrType == AttributableType.ROLE
                        ? Collections.<AttributableType>singletonList(AttributableType.ROLE)
                        : Arrays.asList(AttributableType.values()));
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

                final FieldPanel extAttrNames = new AjaxTextFieldPanel("extAttrName",
                        new ResourceModel("extAttrNames", "extAttrNames").getObject(),
                        new PropertyModel<String>(mapItem, "extAttrName"));
                ((AjaxTextFieldPanel) extAttrNames).setChoices(schemaNames);

                boolean required = false;
                boolean accountIdOrPassword = mapItem.isAccountid() || mapItem.isPassword();
                if (accountIdOrPassword) {
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
                mandatory.setChoices(Arrays.asList(new String[] {"true", "false"}));
                mandatory.setStyleSheet(SHORT_FIELD_STYLE);
                item.add(mandatory);

                final AjaxCheckBoxPanel accountId = new AjaxCheckBoxPanel("accountId",
                        new ResourceModel("accountId", "accountId").getObject(),
                        new PropertyModel<Boolean>(mapItem, "accountid"));
                accountId.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrNames.setEnabled(!accountId.getModelObject() && !mapItem.isPassword());
                        extAttrNames.setModelObject(null);
                        extAttrNames.setRequired(!accountId.getModelObject());
                        target.add(extAttrNames);

                        if (accountId.getModelObject()) {
                            mapItem.setMandatoryCondition("true");
                            mandatory.setEnabled(false);
                        } else {
                            mapItem.setMandatoryCondition("false");
                            mandatory.setEnabled(true);
                        }
                        target.add(mandatory);
                    }
                });
                item.add(accountId);

                final AjaxCheckBoxPanel password = new AjaxCheckBoxPanel("password",
                        new ResourceModel("password", "password").getObject(),
                        new PropertyModel<Boolean>(mapItem, "password"));
                password.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrNames.setEnabled(!mapItem.isAccountid() && !password.getModelObject());
                        extAttrNames.setModelObject(null);
                        extAttrNames.setRequired(!password.getModelObject());
                        target.add(extAttrNames);

                        setAccountId(intMappingTypes.getModelObject(), accountId, password);
                        target.add(accountId);
                    }
                });
                item.add(password);
                if (AttributableType.USER != ResourceMappingPanel.this.attrType) {
                    password.setVisible(false);
                }

                final AjaxDropDownChoicePanel<MappingPurpose> purpose =
                        new AjaxDropDownChoicePanel<MappingPurpose>("purpose",
                        new ResourceModel("purpose", "purpose").getObject(),
                        new PropertyModel<MappingPurpose>(mapItem, "purpose"),
                        false);
                purpose.setChoices(Arrays.asList(MappingPurpose.values()));
                purpose.setStyleSheet(FIELD_STYLE);
                purpose.setRequired(true);
                purpose.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                    }
                });

                item.add(purpose);

                intMappingTypes.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        setAttrNames(intMappingTypes.getModelObject(), intAttrNames);
                        target.add(intAttrNames);

                        setAccountId(intMappingTypes.getModelObject(), accountId, password);
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
        };
        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(this.resourceTO.getConnectorId() != null && this.resourceTO.getConnectorId() > 0);
        mappingContainer.add(addMappingBtn);

        boolean accountLinkEnabled = false;
        if (getMapping().getAccountLink() != null) {
            accountLinkEnabled = true;
        }
        accountLinkCheckbox = new AjaxCheckBoxPanel("accountLinkCheckbox",
                new ResourceModel("accountLinkCheckbox", "accountLinkCheckbox").getObject(),
                new Model<Boolean>(Boolean.valueOf(accountLinkEnabled)));
        accountLinkCheckbox.setEnabled(true);

        accountLinkContainer.add(accountLinkCheckbox);

        final AjaxTextFieldPanel accountLink = new AjaxTextFieldPanel("accountLink",
                new ResourceModel("accountLink", "accountLink").getObject(),
                new PropertyModel<String>(getMapping(), "accountLink"));
        accountLink.setEnabled(accountLinkEnabled);
        accountLinkContainer.add(accountLink);

        accountLinkCheckbox.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

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
    }

    private List<String> getSchemaNames(final Long connectorId, final Set<ConnConfProperty> conf) {
        final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setId(connectorId);
        connInstanceTO.getConfiguration().addAll(conf);

        return connRestClient.getSchemaNames(connInstanceTO);
    }

    private void setEnabled() {
        final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setId(this.resourceTO.getConnectorId());
        connInstanceTO.getConfiguration().addAll(this.resourceTO.getConnConfProperties());

        List<ConnIdObjectClassTO> objectClasses = connRestClient.getSupportedObjectClasses(connInstanceTO);

        boolean enabled = objectClasses.isEmpty()
                || (AttributableType.USER == attrType && objectClasses.contains(ConnIdObjectClassTO.ACCOUNT))
                || (AttributableType.ROLE == attrType && objectClasses.contains(ConnIdObjectClassTO.GROUP));
        this.mappingContainer.setEnabled(enabled);
        this.mappingContainer.setVisible(enabled);
        this.accountLinkContainer.setEnabled(enabled);
        this.accountLinkContainer.setVisible(enabled);

        if (!enabled) {
            getMapping().getItems().clear();
            getMapping().setAccountLink(null);
            if (this.accountLinkCheckbox != null) {
                this.accountLinkCheckbox.setModelObject(null);
            }
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ConnConfModEvent) {
            final AjaxRequestTarget target = ((ConnConfModEvent) event.getPayload()).getTarget();

            final List<ConnConfProperty> conf = ((ConnConfModEvent) event.getPayload()).getConfiguration();

            mappings.removeAll();

            addMappingBtn.setEnabled(resourceTO.getConnectorId() != null && resourceTO.getConnectorId() > 0);

            schemaNames.clear();
            schemaNames.addAll(getSchemaNames(resourceTO.getConnectorId(), new HashSet<ConnConfProperty>(conf)));

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

        if (type == null || type.getAttributableType() == null) {
            toBeUpdated.setChoices(Collections.<String>emptyList());
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
                    toBeUpdated.setChoices(schemaRestClient.getDerSchemaNames(type.getAttributableType()));
                    break;

                case UserVirtualSchema:
                case RoleVirtualSchema:
                case MembershipVirtualSchema:
                    toBeUpdated.setChoices(schemaRestClient.getVirSchemaNames(type.getAttributableType()));
                    break;

                case UserId:
                case Password:
                case Username:
                case RoleId:
                case RoleName:
                default:
                    toBeUpdated.setRequired(false);
                    toBeUpdated.setEnabled(false);
                    toBeUpdated.setChoices(Collections.<String>emptyList());
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
