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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.client.to.SchemaMappingTO;
import org.apache.syncope.console.pages.panels.ResourceConnConfPanel.ConnConfModEvent;
import org.apache.syncope.console.rest.ConnectorRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.IntMappingType;

/**
 * Resource mapping panel.
 */
public class ResourceMappingPanel extends Panel {

    /**
     * Serial verion UID.
     */
    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(ResourceMappingPanel.class);

    /**
     * Schema rest client.
     */
    @SpringBean
    private transient SchemaRestClient schemaRestClient;

    /**
     * ConnInstance rest client.
     */
    @SpringBean
    private transient ConnectorRestClient connRestClient;

    /**
     * Resource schema name.
     */
    private transient List<String> schemaNames;

    /**
     * Internal attribute types.
     */
    private transient List<IntMappingType> attrTypes = new ArrayList<IntMappingType>();

    /**
     * Add mapping button.
     */
    private final transient AjaxButton addMappingBtn;

    /**
     * All mappings.
     */
    private final transient ListView mappings;

    /**
     * External resource to be updated.
     */
    private final transient ResourceTO resourceTO;

    /**
     * Mapping container.
     */
    private final transient WebMarkupContainer mappingContainer;
    
    /**
     * AccountLink container.
     */
    private final transient WebMarkupContainer accountLinkContainer;

    /**
     * OnChange event name.
     */
    private static String onchange = "onchange";

    /**
     * Mapping field style sheet.
     */
    private static String fieldStyle = "ui-widget-content ui-corner-all short_fixedsize";

    /**
     * Mapping field style sheet.
     */
    private static String defFieldStyle = "ui-widget-content ui-corner-all";

    /**
     * Mapping field style sheet.
     */
    private static String shortFieldStyle = "ui-widget-content ui-corner-all veryshort_fixedsize";

    /**
     * Attribute Mapping Panel.
     *
     * @param panelid panel id.
     * @param resourceTO external resource.
     */
    public ResourceMappingPanel(final String panelid, final ResourceTO resourceTO) {

        super(panelid);
        setOutputMarkupId(true);

        this.resourceTO = resourceTO;

        initResourceSchemaNames();
        
        accountLinkContainer = new WebMarkupContainer("accountLinkContainer");
        accountLinkContainer.setOutputMarkupId(true);
        add(accountLinkContainer);

        boolean accountLinkEnabled = false;
        if (resourceTO.getAccountLink() != null) {
            accountLinkEnabled = true;
        }
        final AjaxCheckBoxPanel accountLinkCheckbox = new AjaxCheckBoxPanel("accountLinkCheckbox", 
                        new ResourceModel("accountLinkCheckbox", "accountLinkCheckbox").getObject(),
                        new Model<Boolean>(Boolean.valueOf(accountLinkEnabled)));
        accountLinkCheckbox.setEnabled(true);
        
        accountLinkContainer.add(accountLinkCheckbox);

        final AjaxTextFieldPanel accountLink = new AjaxTextFieldPanel("accountLink", new ResourceModel("accountLink",
                "accountLink").getObject(), new PropertyModel<String>(resourceTO, "accountLink"));
        accountLink.setEnabled(accountLinkEnabled);
        
        accountLinkContainer.add(accountLink);
        
        accountLinkCheckbox.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

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

        mappings = new ListView<SchemaMappingTO>("mappings", resourceTO.getMappings()) {

            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<SchemaMappingTO> item) {

                final SchemaMappingTO mappingTO = item.getModelObject();

                final AttributableType entity;
                if (mappingTO.getIntMappingType() != null) {
                    entity = mappingTO.getIntMappingType().getAttributableType();
                } else {
                    entity = null;
                }

                attrTypes = getAttributeTypes(entity);

                item.add(new AjaxDecoratedCheckbox("toRemove", new Model<Boolean>(Boolean.FALSE)) {

                    private static final long serialVersionUID = 7170946748485726506L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        int index = -1;
                        for (int i = 0; i < resourceTO.getMappings().size() && index == -1; i++) {
                            if (mappingTO.equals(resourceTO.getMappings().get(i))) {
                                index = i;
                            }
                        }

                        if (index != -1) {
                            resourceTO.getMappings().remove(index);
                            item.getParent().removeAll();
                            target.add(mappingContainer);
                        }
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                            private static final long serialVersionUID = -7927968187160354605L;

                            @Override
                            public CharSequence preDecorateScript(final CharSequence script) {

                                return "if (confirm('" + getString("confirmDelete") + "'))" + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final AjaxDropDownChoicePanel<String> intAttrNames = new AjaxDropDownChoicePanel<String>("intAttrNames",
                        getString("intAttrNames"), new PropertyModel<String>(mappingTO, "intAttrName"));
                intAttrNames.setChoices(schemaNames);
                intAttrNames.setRequired(true);
                intAttrNames.setStyleSheet(fieldStyle);
                item.add(intAttrNames);

                final AjaxDropDownChoicePanel<IntMappingType> typesPanel = new AjaxDropDownChoicePanel<IntMappingType>("intMappingTypes",
                        new ResourceModel("intMappingTypes", "intMappingTypes").getObject(),
                        new PropertyModel<IntMappingType>(mappingTO, "intMappingType"));

                // typesPanel onChange behavior provided below ...

                typesPanel.setRequired(true);
                typesPanel.setChoices(attrTypes);
                typesPanel.setStyleSheet(fieldStyle);
                item.add(typesPanel);

                final AjaxDropDownChoicePanel mappingTypesPanel = new AjaxDropDownChoicePanel("mappingTypes",
                        new ResourceModel("mappingTypes", "mappingTypes").getObject(), new Model(entity));

                mappingTypesPanel.setChoices(Arrays.asList(AttributableType.values()));
                mappingTypesPanel.setStyleSheet(defFieldStyle);

                item.add(mappingTypesPanel);

                mappingTypesPanel.getField().add(new AjaxFormComponentUpdatingBehavior(onchange) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {

                        attrTypes = getAttributeTypes((AttributableType) mappingTypesPanel.getModelObject());

                        typesPanel.setChoices(attrTypes);
                        List<String> emptyList = Collections.emptyList();
                        intAttrNames.setChoices(emptyList);

                        target.add(typesPanel.getField());
                        target.add(intAttrNames.getField());

                    }
                });

                final FieldPanel extAttrName;

                if (schemaNames.isEmpty()) {
                    extAttrName = new AjaxTextFieldPanel("extAttrName", new ResourceModel("extAttrNames",
                            "extAttrNames").getObject(), new PropertyModel<String>(mappingTO, "extAttrName"));

                } else {
                    extAttrName = new AjaxDropDownChoicePanel<String>("extAttrName", new ResourceModel("extAttrNames",
                            "extAttrNames").getObject(), new PropertyModel(mappingTO, "extAttrName"));
                    ((AjaxDropDownChoicePanel) extAttrName).setChoices(schemaNames);
                }

                boolean required = false;
                if (mappingTO != null && !mappingTO.isAccountid() && !mappingTO.isPassword()) {
                    required = true;
                }


                extAttrName.setRequired(required);
                extAttrName.setEnabled(required);

                extAttrName.setStyleSheet(fieldStyle);
                item.add(extAttrName);

                final AjaxTextFieldPanel mandatory = new AjaxTextFieldPanel("mandatoryCondition", new ResourceModel(
                        "mandatoryCondition", "mandatoryCondition").getObject(), new PropertyModel(mappingTO,
                        "mandatoryCondition"));

                mandatory.setChoices(Arrays.asList(new String[]{"true", "false"}));

                mandatory.setStyleSheet(shortFieldStyle);

                item.add(mandatory);

                final AjaxCheckBoxPanel accountId = new AjaxCheckBoxPanel("accountId", new ResourceModel("accountId",
                        "accountId").getObject(), new PropertyModel(mappingTO, "accountid"));

                accountId.getField().add(new AjaxFormComponentUpdatingBehavior(onchange) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrName.setEnabled(!accountId.getModelObject() && !mappingTO.isPassword());
                        extAttrName.setModelObject(null);
                        extAttrName.setRequired(!accountId.getModelObject());
                        target.add(extAttrName);
                    }
                });

                item.add(accountId);

                final AjaxCheckBoxPanel password = new AjaxCheckBoxPanel("password", new ResourceModel("password",
                        "password").getObject(), new PropertyModel(mappingTO, "password"));

                password.getField().add(new AjaxFormComponentUpdatingBehavior(onchange) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        extAttrName.setEnabled(!mappingTO.isAccountid() && !password.getModelObject());
                        extAttrName.setModelObject(null);
                        extAttrName.setRequired(!password.getModelObject());
                        target.add(extAttrName);

                        setAccountId((IntMappingType) typesPanel.getModelObject(), accountId, password);
                        target.add(accountId);
                    }
                });

                item.add(password);

                typesPanel.getField().add(new AjaxFormComponentUpdatingBehavior(onchange) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        setAttrNames((IntMappingType) typesPanel.getModelObject(), intAttrNames);
                        target.add(intAttrNames);

                        setAccountId((IntMappingType) typesPanel.getModelObject(), accountId, password);
                        target.add(accountId);
                    }
                });

                setAttrNames(mappingTO.getIntMappingType(), intAttrNames);
                setAccountId(mappingTO.getIntMappingType(), accountId, password);
            }
        };

        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        addMappingBtn = new IndicatingAjaxButton("addUserSchemaMappingBtn", new ResourceModel("add")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                resourceTO.getMappings().add(new SchemaMappingTO());
                target.add(mappingContainer);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                // ignore errors
            }
        };

        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(resourceTO.getConnectorId() != null && resourceTO.getConnectorId() > 0);
        mappingContainer.add(addMappingBtn);

    }

    /**
     * Initialize resource schema names.
     */
    private void initResourceSchemaNames() {
        if (resourceTO != null && resourceTO.getConnectorId() != null && resourceTO.getConnectorId() > 0) {
            schemaNames = getResourceSchemaNames(resourceTO.getConnectorId(), resourceTO.getConnConfProperties());
        } else {
            schemaNames = Collections.emptyList();
        }
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

            schemaNames = getResourceSchemaNames(resourceTO.getConnectorId(), new HashSet<ConnConfProperty>(conf));

            target.add(mappingContainer);
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

                case SyncopeUserId:
                case Password:
                case Username:
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
            res.addAll(IntMappingType.getAttributeTypes(AttributableType.valueOf(entity.toString())));
        }

        return res;
    }
}
