/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.pages.panels.ResourceSecurityPanel;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.PropagationMode;
import org.syncope.types.IntMappingType;
import org.syncope.types.TraceLevel;

/**
 * Modal window with Resource form.
 */
public class ResourceModalPage extends BaseModalPage {

    private static final long serialVersionUID = 1734415311027284221L;

    private static final String GUARDED_STRING =
            "org.identityconnectors.common.security.GuardedString";

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private ConnectorRestClient connectorRestClient;

    @SpringBean
    private ResourceRestClient restClient;

    private List<String> uSchemaAttrNames;

    private List<String> uDerSchemaAttrNames;

    private List<String> uVirSchemaAttrNames;

    private WebMarkupContainer mappingContainer;

    private WebMarkupContainer connectorPropertiesContainer;

    private Set<ConnConfProperty> overridableConnectorProperties;

    private List<String> resourceSchemaNames;

    public ResourceModalPage(final PageReference callPageRef,
            final ModalWindow window, final ResourceTO resourceTO,
            final boolean createFlag) {

        super();

        uSchemaAttrNames =
                schemaRestClient.getSchemaNames("user");
        uDerSchemaAttrNames =
                schemaRestClient.getDerivedSchemaNames("user");
        uVirSchemaAttrNames =
                schemaRestClient.getVirtualSchemaNames("user");

        final IModel<List<ConnInstanceTO>> connectors =
                new LoadableDetachableModel<List<ConnInstanceTO>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<ConnInstanceTO> load() {
                        return connectorRestClient.getAllConnectors();
                    }
                };

        final IModel<List<IntMappingType>> intMappingTypes =
                new LoadableDetachableModel<List<IntMappingType>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<IntMappingType> load() {
                        return Arrays.asList(IntMappingType.values());
                    }
                };
        final IModel<List<ConnConfProperty>> connectorPropertiesModel =
                new LoadableDetachableModel<List<ConnConfProperty>>() {

                    private static final long serialVersionUID =
                            5275935387613157437L;

                    @Override
                    protected List<ConnConfProperty> load() {
                        Set<ConnConfProperty> props =
                                resourceTO.getConnectorConfigurationProperties();

                        if (props == null || props.isEmpty() || createFlag) {
                            props = overridableConnectorProperties;
                        }
                        return new ArrayList<ConnConfProperty>(props);
                    }
                };

        updateResourceSchemaNames(resourceTO);
        updateConnectorProperties(resourceTO.getConnectorId());

        final ConnInstanceTO connectorTO = new ConnInstanceTO();
        if (!createFlag) {
            connectorTO.setId(resourceTO.getConnectorId());
        }

        final Form form = new Form("form");
        form.setModel(new CompoundPropertyModel(resourceTO));

        final AjaxTextFieldPanel resourceName = new AjaxTextFieldPanel(
                "name", getString("name"),
                new PropertyModel<String>(resourceTO, "name"), false);
        resourceName.setEnabled(createFlag);
        resourceName.addRequiredLabel();
        form.add(resourceName);

        final AjaxTextFieldPanel accountLink = new AjaxTextFieldPanel(
                "accountLink", getString("accountLink"),
                new PropertyModel<String>(resourceTO, "accountLink"), false);
        form.add(accountLink);

        final AjaxCheckBoxPanel forceMandatoryConstraint =
                new AjaxCheckBoxPanel(
                "forceMandatoryConstraint",
                getString("forceMandatoryConstraint"),
                new PropertyModel<Boolean>(resourceTO,
                "forceMandatoryConstraint"),
                false);
        form.add(forceMandatoryConstraint);

        final AjaxCheckBoxPanel propagationPrimary =
                new AjaxCheckBoxPanel(
                "propagationPrimary",
                getString("propagationPrimary"),
                new PropertyModel<Boolean>(resourceTO,
                "propagationPrimary"),
                false);
        form.add(propagationPrimary);

        final AjaxNumberFieldPanel propagationPriority =
                new AjaxNumberFieldPanel(
                "propagationPriority", getString("propagationPriority"),
                new PropertyModel<Number>(resourceTO, "propagationPriority"),
                false);
        form.add(propagationPriority);

        final AjaxDropDownChoicePanel<PropagationMode> propagationMode =
                new AjaxDropDownChoicePanel<PropagationMode>(
                "propagationMode",
                getString("propagationMode"),
                new PropertyModel(resourceTO, "propagationMode"),
                false);
        propagationMode.setChoices(
                Arrays.asList(PropagationMode.values()));
        form.add(propagationMode);

        final AjaxDropDownChoicePanel<TraceLevel> createTraceLevel =
                new AjaxDropDownChoicePanel<TraceLevel>("createTraceLevel",
                getString("createTraceLevel"),
                new PropertyModel(resourceTO, "createTraceLevel"),
                false);
        createTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        form.add(createTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> updateTraceLevel =
                new AjaxDropDownChoicePanel<TraceLevel>("updateTraceLevel",
                getString("updateTraceLevel"),
                new PropertyModel(resourceTO, "updateTraceLevel"),
                false);
        updateTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        form.add(updateTraceLevel);

        final AjaxDropDownChoicePanel<TraceLevel> deleteTraceLevel =
                new AjaxDropDownChoicePanel<TraceLevel>("deleteTraceLevel",
                getString("deleteTraceLevel"),
                new PropertyModel(resourceTO, "deleteTraceLevel"),
                false);
        deleteTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        form.add(deleteTraceLevel);        

        final AjaxDropDownChoicePanel<TraceLevel> syncTraceLevel =
                new AjaxDropDownChoicePanel<TraceLevel>("syncTraceLevel",
                getString("syncTraceLevel"),
                new PropertyModel(resourceTO, "syncTraceLevel"),
                false);
        syncTraceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        form.add(syncTraceLevel);

        final AjaxCheckBoxPanel resetToken = new AjaxCheckBoxPanel(
                "resetToken", getString("resetToken"), new Model(null), false);
        resetToken.getField().add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID =
                            -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget art) {
                        if (resetToken.getModelObject()) {
                            resourceTO.setSyncToken(null);
                        }
                    }
                });
        form.add(resetToken);
        
        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        form.add(mappingContainer);

        connectorPropertiesContainer =
                new WebMarkupContainer("connectorPropertiesContainer");
        connectorPropertiesContainer.setOutputMarkupId(true);
        form.add(connectorPropertiesContainer);

        final AjaxDropDownChoicePanel<ConnInstanceTO> connector =
                new AjaxDropDownChoicePanel<ConnInstanceTO>("connector",
                getString("connector"),
                new Model<ConnInstanceTO>(connectorTO),
                false);
        connector.setChoices(connectors.getObject());
        connector.setChoiceRenderer(new ChoiceRenderer("displayName", "id"));
        connector.getField().setModel(new IModel<ConnInstanceTO>() {

            private static final long serialVersionUID = -4202872830392400310L;

            @Override
            public ConnInstanceTO getObject() {
                return connectorTO;
            }

            @Override
            public void setObject(final ConnInstanceTO connector) {
                resourceTO.setConnectorId(connector.getId());
            }

            @Override
            public void detach() {
            }
        });

        connector.addRequiredLabel();
        connector.setEnabled(createFlag);
        form.add(connector);

        final ListView<SchemaMappingTO> mappings =
                new ListView<SchemaMappingTO>(
                "mappings", resourceTO.getMappings()) {

                    private static final long serialVersionUID =
                            4949588177564901031L;

                    @Override
                    protected void populateItem(
                            final ListItem<SchemaMappingTO> item) {

                        final SchemaMappingTO mappingTO = item.getModelObject();

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
                        intAttrNames.setChoices(resourceSchemaNames);
                        intAttrNames.setRequired(true);
                        intAttrNames.setStyleShet(
                                "ui-widget-content ui-corner-all short_fixedsize");

                        if (mappingTO.getIntMappingType() == null) {
                            intAttrNames.setChoices(Collections.EMPTY_LIST);
                        } else {
                            switch (mappingTO.getIntMappingType()) {
                                case UserSchema:
                                    intAttrNames.setChoices(uSchemaAttrNames);
                                    break;

                                case UserDerivedSchema:
                                    intAttrNames.setChoices(
                                            uDerSchemaAttrNames);
                                    break;

                                case UserVirtualSchema:
                                    intAttrNames.setChoices(
                                            uVirSchemaAttrNames);
                                    break;

                                case SyncopeUserId:
                                    intAttrNames.setEnabled(false);
                                    intAttrNames.setRequired(false);
                                    intAttrNames.setChoices(
                                            Collections.EMPTY_LIST);
                                    mappingTO.setIntAttrName("SyncopeUserId");
                                    break;

                                case Password:
                                    intAttrNames.setEnabled(false);
                                    intAttrNames.setRequired(false);
                                    intAttrNames.setChoices(
                                            Collections.EMPTY_LIST);
                                    mappingTO.setIntAttrName("Password");
                                    break;

                                case Username:
                                    intAttrNames.setEnabled(false);
                                    intAttrNames.setRequired(false);
                                    intAttrNames.setChoices(
                                            Collections.EMPTY_LIST);
                                    mappingTO.setIntAttrName("Username");
                                    break;

                                default:
                                    intAttrNames.setChoices(
                                            Collections.EMPTY_LIST);
                            }
                        }
                        item.add(intAttrNames);

                        final IntMappingTypesDropDownChoice mappingTypesPanel =
                                new IntMappingTypesDropDownChoice(
                                "intMappingTypes",
                                getString("intMappingTypes"),
                                new PropertyModel<IntMappingType>(
                                mappingTO, "intMappingType"),
                                intAttrNames);

                        mappingTypesPanel.setRequired(true);
                        mappingTypesPanel.setChoices(intMappingTypes.getObject());
                        mappingTypesPanel.setStyleShet(
                                "ui-widget-content ui-corner-all short_fixedsize");
                        item.add(mappingTypesPanel);

                        final FieldPanel extAttrName;

                        if (resourceSchemaNames.isEmpty()) {
                            extAttrName = new AjaxTextFieldPanel(
                                    "extAttrName", getString("extAttrNames"),
                                    new PropertyModel<String>(mappingTO,
                                    "extAttrName"),
                                    true);

                        } else {
                            extAttrName =
                                    new AjaxDropDownChoicePanel<String>(
                                    "extAttrName", getString("extAttrNames"),
                                    new PropertyModel(mappingTO, "extAttrName"),
                                    true);
                            ((AjaxDropDownChoicePanel) extAttrName).setChoices(
                                    resourceSchemaNames);

                        }

                        boolean required = mappingTO != null
                                && !mappingTO.isAccountid()
                                && !mappingTO.isPassword();

                        extAttrName.setRequired(required);
                        extAttrName.setEnabled(required);

                        extAttrName.setStyleShet(
                                "ui-widget-content ui-corner-all short_fixedsize");
                        item.add(extAttrName);

                        final AjaxTextFieldPanel mandatoryCondition =
                                new AjaxTextFieldPanel(
                                "mandatoryCondition",
                                getString("mandatoryCondition"),
                                new PropertyModel(mappingTO,
                                "mandatoryCondition"),
                                true);

                        mandatoryCondition.setChoices(Arrays.asList(
                                new String[]{"true", "false"}));

                        mandatoryCondition.setStyleShet(
                                "ui-widget-content ui-corner-all short_fixedsize");

                        item.add(mandatoryCondition);

                        final AjaxCheckBoxPanel accountId =
                                new AjaxCheckBoxPanel(
                                "accountId", getString("accountId"),
                                new PropertyModel(mappingTO, "accountid"), false);

                        accountId.getField().add(
                                new AjaxFormComponentUpdatingBehavior("onchange") {

                                    private static final long serialVersionUID =
                                            -1107858522700306810L;

                                    @Override
                                    protected void onUpdate(
                                            AjaxRequestTarget target) {
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
                                "password", getString("password"),
                                new PropertyModel(mappingTO, "password"), true);

                        password.getField().add(
                                new AjaxFormComponentUpdatingBehavior("onchange") {

                                    private static final long serialVersionUID =
                                            -1107858522700306810L;

                                    @Override
                                    protected void onUpdate(
                                            AjaxRequestTarget target) {
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

        final AjaxButton addSchemaMappingBtn = new IndicatingAjaxButton(
                "addUserSchemaMappingBtn", new ResourceModel("add")) {

            private static final long serialVersionUID = -4804368561204623354L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                resourceTO.getMappings().add(new SchemaMappingTO());
                target.add(mappingContainer);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                // ignore errors
            }
        };

        addSchemaMappingBtn.setDefaultFormProcessing(false);
        addSchemaMappingBtn.setEnabled(!createFlag);
        mappingContainer.add(addSchemaMappingBtn);

        /*
         * the list of overridable connector properties 
         */
        connectorPropertiesContainer.add(new ListView<ConnConfProperty>(
                "connectorProperties", connectorPropertiesModel) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<ConnConfProperty> item) {
                final ConnConfProperty property = item.getModelObject();

                final Label label = new Label("connPropAttrSchema",
                        property.getSchema().getDisplayName() == null
                        || property.getSchema().getDisplayName().isEmpty()
                        ? property.getSchema().getName()
                        : property.getSchema().getDisplayName());

                item.add(label);

                final FieldPanel field;

                if (GUARDED_STRING.equals(
                        property.getSchema().getType())) {

                    field = new AjaxPasswordFieldPanel(
                            "connPropAttrValue",
                            label.getDefaultModelObjectAsString(),
                            new PropertyModel<String>(property,
                            "value"), true).setRequired(
                            property.getSchema().
                            isRequired()).setTitle(property.getSchema().
                            getHelpMessage());
                } else {

                    field = new AjaxTextFieldPanel(
                            "connPropAttrValue",
                            label.getDefaultModelObjectAsString(),
                            new PropertyModel<String>(property, "value"),
                            false).setRequired(property.getSchema().isRequired()).
                            setTitle(property.getSchema().getHelpMessage());

                    if (property.getSchema().isRequired()) {
                        field.addRequiredLabel();
                    }
                }

                field.getField().add(
                        new AjaxFormComponentUpdatingBehavior("onchange") {

                            private static final long serialVersionUID =
                                    -1107858522700306810L;

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                mappings.removeAll();
                                addSchemaMappingBtn.setEnabled(
                                        resourceTO.getConnectorId() != null
                                        && resourceTO.getConnectorId() > 0);

                                updateResourceSchemaNames(resourceTO);

                                target.add(mappingContainer);
                            }
                        });

                item.add(field);
                resourceTO.getConnectorConfigurationProperties().add(property);
            }
        });

        connector.getField().add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    private static final long serialVersionUID =
                            -1107858522700306810L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        mappings.removeAll();
                        addSchemaMappingBtn.setEnabled(
                                resourceTO.getConnectorId() != null
                                && resourceTO.getConnectorId() > 0);

                        updateResourceSchemaNames(resourceTO);
                        updateConnectorProperties(resourceTO.getConnectorId());

                        target.add(mappingContainer);
                        target.add(connectorPropertiesContainer);
                    }
                });

        //--------------------------------
        // Security container
        //--------------------------------
        form.add(new ResourceSecurityPanel("security", resourceTO));
        //--------------------------------

        AjaxButton submit = new IndicatingAjaxButton(
                "apply", new ResourceModel("submit")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                ResourceTO resourceTO =
                        (ResourceTO) form.getDefaultModelObject();

                int accountIdCount = 0;
                for (SchemaMappingTO mapping : resourceTO.getMappings()) {
                    if (mapping.isAccountid()) {
                        accountIdCount++;
                    }
                }
                if (accountIdCount == 0 || accountIdCount > 1) {
                    error(getString("accountIdValidation"));
                    target.add(feedbackPanel);
                } else {
                    try {
                        if (createFlag) {
                            restClient.create(resourceTO);
                        } else {
                            restClient.update(resourceTO);
                        }

                        ((Resources) callPageRef.getPage()).setOperationResult(
                                true);
                        window.close(target);
                    } catch (SyncopeClientCompositeErrorException e) {
                        LOG.error("While creating/updating resource {}",
                                resourceTO);
                        error(getString("error") + ":" + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };
        form.add(submit);

        add(form);

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Resources",
                createFlag ? "create" : "update"));
    }

    /**
     * Extension class of DropDownChoice.
     * It's purposed for storing values in the
     * corresponding property model after pressing 'Add' button.
     */
    private class IntMappingTypesDropDownChoice
            extends AjaxDropDownChoicePanel {

        private static final long serialVersionUID = -2855668124505116627L;

        public IntMappingTypesDropDownChoice(
                final String id,
                final String name,
                final PropertyModel<IntMappingType> model,
                final AjaxDropDownChoicePanel<String> chooserToPopulate) {

            super(id, name, model, false);

            field.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                private static final long serialVersionUID =
                        -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {

                    chooserToPopulate.setRequired(true);
                    chooserToPopulate.setEnabled(true);

                    final List<String> result;

                    switch (model.getObject()) {
                        case UserSchema:
                            result = uSchemaAttrNames;
                            break;

                        case UserDerivedSchema:
                            result = uDerSchemaAttrNames;
                            break;

                        case UserVirtualSchema:
                            result = uVirSchemaAttrNames;
                            break;

                        case SyncopeUserId:
                        case Password:
                        case Username:
                        default:
                            chooserToPopulate.setRequired(false);
                            chooserToPopulate.setEnabled(false);
                            result = Collections.EMPTY_LIST;
                    }

                    chooserToPopulate.setChoices(result);
                    target.add(chooserToPopulate);
                }
            });
        }
    }

    public final void updateResourceSchemaNames(final ResourceTO resourceTO) {
        try {

            resourceSchemaNames =
                    (resourceTO != null && resourceTO.getConnectorId() != null)
                    ? connectorRestClient.getSchemaNames(resourceTO)
                    : Collections.EMPTY_LIST;

        } catch (Exception e) {
            LOG.warn("Error retrieving resource schema names", e);
            resourceSchemaNames = Collections.EMPTY_LIST;
        }
    }

    public final void updateConnectorProperties(final Long connectorId) {
        if (connectorId != null && connectorId > 0) {
            Set<ConnConfProperty> overridableProperties =
                    new HashSet<ConnConfProperty>();
            for (ConnConfProperty p :
                    connectorRestClient.getConnectorProperties(connectorId)) {
                if (p.isOverridable()) {
                    overridableProperties.add(p);
                }
            }
            overridableConnectorProperties = overridableProperties;
        } else {
            overridableConnectorProperties = Collections.emptySet();
        }
    }
}
