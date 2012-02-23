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
package org.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
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
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.console.pages.panels.ResourceConnConfPanel.ConnConfModEvent;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.Entity;
import org.syncope.types.IntMappingType;

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
    protected static final Logger LOG =
            LoggerFactory.getLogger(ResourceMappingPanel.class);

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
    private List<String> schemaNames;

    /**
     * Internal attribute types.
     */
    private List<IntMappingType> attrTypes = new ArrayList<IntMappingType>();

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

    /**
     * Mapping container.
     */
    private final WebMarkupContainer mappingContainer;

    /**
     * Create flag.
     */
    private final boolean createFlag;

    /**
     * OnChange event name.
     */
    private static String onchange = "onchange";

    /**
     * Mapping field style sheet.
     */
    private static String fieldStyle =
            "ui-widget-content ui-corner-all short_fixedsize";

    /**
     * Mapping field style sheet.
     */
    private static String defFieldStyle =
            "ui-widget-content ui-corner-all";

    /**
     * Mapping field style sheet.
     */
    private static String shortFieldStyle =
            "ui-widget-content ui-corner-all veryshort_fixedsize";

    /**
     * Attribute Mapping Panel.
     *
     * @param panelid panel id.
     * @param resourceTO external resource.
     * @param createFlag create flag.
     */
    public ResourceMappingPanel(
            final String panelid,
            final ResourceTO resourceTO,
            final boolean createFlag) {

        super(panelid);
        setOutputMarkupId(true);

        this.resourceTO = resourceTO;
        this.createFlag = createFlag;

        initResourceSchemaNames();

        final AjaxTextFieldPanel accountLink = new AjaxTextFieldPanel(
                "accountLink",
                new ResourceModel("accountLink", "accountLink").getObject(),
                new PropertyModel<String>(resourceTO, "accountLink"), false);
        add(accountLink);

        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        add(mappingContainer);

        mappings = new ListView<SchemaMappingTO>(
                "mappings", resourceTO.getMappings()) {

            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(
                    final ListItem<SchemaMappingTO> item) {

                final SchemaMappingTO mappingTO = item.getModelObject();

                final Entity entity = mappingTO.getIntMappingType() == null
                        ? null : mappingTO.getIntMappingType().getEntity();

                attrTypes = getAttributeTypes(entity);

                item.add(new AjaxDecoratedCheckbox("toRemove",
                        new Model(Boolean.FALSE)) {

                    private static final long serialVersionUID =
                            7170946748485726506L;

                    @Override
                    protected void onUpdate(
                            final AjaxRequestTarget target) {
                        int index = -1;
                        for (int i = 0; i < resourceTO.getMappings().
                                size()
                                && index == -1; i++) {

                            if (mappingTO.equals(
                                    resourceTO.getMappings().get(i))) {

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
                        return new AjaxPreprocessingCallDecorator(
                                super.getAjaxCallDecorator()) {

                            private static final long serialVersionUID =
                                    -7927968187160354605L;

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete")
                                        + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final AjaxDropDownChoicePanel intAttrNames =
                        new AjaxDropDownChoicePanel<String>(
                        "intAttrNames",
                        getString("intAttrNames"),
                        new PropertyModel(mappingTO, "intAttrName"),
                        true);
                intAttrNames.setChoices(schemaNames);
                intAttrNames.setRequired(true);
                intAttrNames.setStyleShet(fieldStyle);

                setAttrNames(mappingTO.getIntMappingType(), intAttrNames);

                item.add(intAttrNames);


                final AjaxDropDownChoicePanel typesPanel =
                        new AjaxDropDownChoicePanel(
                        "intMappingTypes",
                        new ResourceModel("intMappingTypes", "intMappingTypes").
                        getObject(),
                        new PropertyModel<IntMappingType>(
                        mappingTO, "intMappingType"), false);

                typesPanel.getField().add(
                        new AjaxFormComponentUpdatingBehavior(onchange) {

                            private static final long serialVersionUID =
                                    -1107858522700306810L;

                            @Override
                            protected void onUpdate(
                                    final AjaxRequestTarget target) {
                                setAttrNames(
                                        (IntMappingType) typesPanel.
                                        getModelObject(), intAttrNames);
                                target.add(intAttrNames);
                            }
                        });

                typesPanel.setRequired(true);
                typesPanel.setChoices(attrTypes);
                typesPanel.setStyleShet(fieldStyle);
                item.add(typesPanel);

                final AjaxDropDownChoicePanel mappingTypesPanel =
                        new AjaxDropDownChoicePanel(
                        "mappingTypes",
                        new ResourceModel("mappingTypes", "mappingTypes").
                        getObject(), new Model(entity), false);

                mappingTypesPanel.setChoices(Arrays.asList(Entity.values()));
                mappingTypesPanel.setStyleShet(defFieldStyle);

                item.add(mappingTypesPanel);

                mappingTypesPanel.getField().add(
                        new AjaxFormComponentUpdatingBehavior(onchange) {

                            private static final long serialVersionUID =
                                    -1107858522700306810L;

                            @Override
                            protected void onUpdate(
                                    final AjaxRequestTarget target) {

                                attrTypes = getAttributeTypes(
                                        (Entity) mappingTypesPanel.
                                        getModelObject());

                                typesPanel.setChoices(attrTypes);
                                intAttrNames.setChoices(Collections.EMPTY_LIST);

                                target.add(typesPanel.getField());
                                target.add(intAttrNames.getField());

                            }
                        });

                final FieldPanel extAttrName;

                if (schemaNames.isEmpty()) {
                    extAttrName = new AjaxTextFieldPanel(
                            "extAttrName",
                            new ResourceModel("extAttrNames", "extAttrNames").
                            getObject(),
                            new PropertyModel<String>(mappingTO, "extAttrName"),
                            true);

                } else {
                    extAttrName =
                            new AjaxDropDownChoicePanel<String>(
                            "extAttrName",
                            new ResourceModel("extAttrNames", "extAttrNames").
                            getObject(),
                            new PropertyModel(mappingTO, "extAttrName"),
                            true);
                    ((AjaxDropDownChoicePanel) extAttrName).setChoices(
                            schemaNames);

                }

                boolean required = mappingTO != null
                        && !mappingTO.isAccountid() && !mappingTO.isPassword();

                extAttrName.setRequired(required);
                extAttrName.setEnabled(required);

                extAttrName.setStyleShet(fieldStyle);
                item.add(extAttrName);

                final AjaxTextFieldPanel mandatory =
                        new AjaxTextFieldPanel(
                        "mandatoryCondition",
                        new ResourceModel(
                        "mandatoryCondition", "mandatoryCondition").getObject(),
                        new PropertyModel(mappingTO, "mandatoryCondition"),
                        true);

                mandatory.setChoices(
                        Arrays.asList(new String[]{"true", "false"}));

                mandatory.setStyleShet(shortFieldStyle);

                item.add(mandatory);

                final AjaxCheckBoxPanel accountId =
                        new AjaxCheckBoxPanel(
                        "accountId",
                        new ResourceModel("accountId", "accountId").getObject(),
                        new PropertyModel(mappingTO, "accountid"), false);

                accountId.getField().add(
                        new AjaxFormComponentUpdatingBehavior(onchange) {

                            private static final long serialVersionUID =
                                    -1107858522700306810L;

                            @Override
                            protected void onUpdate(
                                    final AjaxRequestTarget target) {
                                extAttrName.setEnabled(
                                        !accountId.getModelObject()
                                        && !mappingTO.isPassword());
                                extAttrName.setModelObject(null);
                                extAttrName.setRequired(
                                        !accountId.getModelObject());
                                target.add(extAttrName);
                            }
                        });

                item.add(accountId);

                final AjaxCheckBoxPanel password =
                        new AjaxCheckBoxPanel(
                        "password",
                        new ResourceModel("password", "password").getObject(),
                        new PropertyModel(mappingTO, "password"), true);

                password.getField().add(
                        new AjaxFormComponentUpdatingBehavior(onchange) {

                            private static final long serialVersionUID =
                                    -1107858522700306810L;

                            @Override
                            protected void onUpdate(
                                    final AjaxRequestTarget target) {
                                extAttrName.setEnabled(
                                        !mappingTO.isAccountid()
                                        && !password.getModelObject());
                                extAttrName.setModelObject(null);
                                extAttrName.setRequired(
                                        !password.getModelObject());
                                target.add(extAttrName);
                            }
                        });

                item.add(password);
            }
        };

        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        addMappingBtn = new IndicatingAjaxButton(
                "addUserSchemaMappingBtn", new ResourceModel("add")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                resourceTO.getMappings().add(new SchemaMappingTO());
                target.add(mappingContainer);
            }

            @Override
            protected void onError(
                    final AjaxRequestTarget target, final Form<?> form) {
                // ignore errors
            }
        };

        addMappingBtn.setDefaultFormProcessing(false);
        addMappingBtn.setEnabled(!createFlag);
        mappingContainer.add(addMappingBtn);

    }

    /**
     * Initialize resource schema names.
     */
    private void initResourceSchemaNames() {
        if (resourceTO != null
                && resourceTO.getConnectorId() != null
                && resourceTO.getConnectorId() > 0) {

            final ConnInstanceTO connInstanceTO = new ConnInstanceTO();
            connInstanceTO.setId(resourceTO.getConnectorId());

            connInstanceTO.setConfiguration(
                    resourceTO.getConnConfProperties());

            schemaNames = getResourceSchemaNames(
                    resourceTO.getConnectorId(),
                    resourceTO.getConnConfProperties());

        } else {
            schemaNames = Collections.EMPTY_LIST;
        }
    }

    /**
     * Get resource schema names.
     *
     * @param connectorId connector id.
     * @param conf connector configuration properties.
     * @return resource schema names.
     */
    private List<String> getResourceSchemaNames(
            final Long connectorId, final Set<ConnConfProperty> conf) {
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

            final AjaxRequestTarget target =
                    ((ConnConfModEvent) event.getPayload()).getTarget();

            final List<ConnConfProperty> conf =
                    ((ConnConfModEvent) event.getPayload()).getConfiguration();

            mappings.removeAll();

            addMappingBtn.setEnabled(
                    resourceTO.getConnectorId() != null
                    && resourceTO.getConnectorId() > 0);

            schemaNames = getResourceSchemaNames(
                    resourceTO.getConnectorId(),
                    new HashSet<ConnConfProperty>(conf));

            target.add(mappingContainer);
        }
    }

    /**
     * Seta attribute names for a drop down chice list.
     *
     * @param attrType attribute type.
     * @param toBeUpdated drop down choice to be updated.
     */
    private void setAttrNames(
            final IntMappingType attrType,
            final AjaxDropDownChoicePanel toBeUpdated) {

        toBeUpdated.setRequired(true);
        toBeUpdated.setEnabled(true);

        if (attrType == null || attrType.getEntity() == null) {
            toBeUpdated.setChoices(Collections.EMPTY_LIST);
        } else {

            switch (attrType) {
                // user attribute names
                case UserSchema:
                case RoleSchema:
                case MembershipSchema:
                    toBeUpdated.setChoices(
                            schemaRestClient.getSchemaNames(
                            attrType.getEntity().toString().toLowerCase()));
                    break;

                case UserDerivedSchema:
                case RoleDerivedSchema:
                case MembershipDerivedSchema:
                    toBeUpdated.setChoices(
                            schemaRestClient.getDerivedSchemaNames(
                            attrType.getEntity().toString().toLowerCase()));
                    break;

                case UserVirtualSchema:
                case RoleVirtualSchema:
                case MembershipVirtualSchema:
                    toBeUpdated.setChoices(
                            schemaRestClient.getVirtualSchemaNames(
                            attrType.getEntity().toString().toLowerCase()));
                    break;

                case SyncopeUserId:
                    toBeUpdated.setEnabled(false);
                    toBeUpdated.setRequired(false);
                    toBeUpdated.setChoices(Collections.EMPTY_LIST);
                    break;

                case Password:
                    toBeUpdated.setEnabled(false);
                    toBeUpdated.setRequired(false);
                    toBeUpdated.setChoices(Collections.EMPTY_LIST);
                    break;

                case Username:
                    toBeUpdated.setEnabled(false);
                    toBeUpdated.setRequired(false);
                    toBeUpdated.setChoices(Collections.EMPTY_LIST);
                    break;

                default:
                    toBeUpdated.setRequired(false);
                    toBeUpdated.setEnabled(false);
                    toBeUpdated.setChoices(Collections.EMPTY_LIST);
            }
        }
    }

    /**
     * Get all attribute types from a selected attribute type.
     *
     * @param entity entity.
     * @return all attribute types.
     */
    private List<IntMappingType> getAttributeTypes(final Entity entity) {
        final List<IntMappingType> res = new ArrayList<IntMappingType>();

        if (entity != null) {
            final EnumSet types = IntMappingType.getAttributeTypes(
                    Entity.valueOf(entity.toString()));

            for (Object type : types) {
                res.add(IntMappingType.valueOf(
                        type.toString()));
            }
        }

        return res;
    }
}
