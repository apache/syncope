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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxDecoratedCheckbox;
import org.syncope.types.PropagationMode;
import org.syncope.types.SourceMappingType;
import org.syncope.types.TrackingMode;

/**
 * Modal window with Resource form.
 */
public class ResourceModalPage extends BaseModalPage {

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

    public ResourceModalPage(final Resources basePage, final ModalWindow window,
            final ResourceTO resourceTO, final boolean createFlag) {

        super();

        uSchemaAttrNames =
                schemaRestClient.getSchemaNames("user");
        uDerSchemaAttrNames =
                schemaRestClient.getDerivedSchemaNames("user");
        uVirSchemaAttrNames =
                schemaRestClient.getVirtualSchemaNames("user");

        final IModel<List<ConnInstanceTO>> connectors =
                new LoadableDetachableModel<List<ConnInstanceTO>>() {

                    @Override
                    protected List<ConnInstanceTO> load() {
                        return connectorRestClient.getAllConnectors();
                    }
                };

        final IModel<List<SourceMappingType>> sourceMappingTypes =
                new LoadableDetachableModel<List<SourceMappingType>>() {

                    @Override
                    protected List<SourceMappingType> load() {
                        return Arrays.asList(SourceMappingType.values());
                    }
                };

        final ConnInstanceTO connectorTO = new ConnInstanceTO();
        if (!createFlag) {
            connectorTO.setId(resourceTO.getConnectorId());
        }

        Form resourceForm = new Form("ResourceForm");
        resourceForm.setModel(new CompoundPropertyModel(resourceTO));

        TextField resourceName = new TextField("name");
        resourceName.setEnabled(createFlag);
        resourceName.setRequired(true);
        resourceName.setOutputMarkupId(true);
        resourceForm.add(resourceName);

        TextField accountLink = new TextField("accountLink");
        accountLink.setOutputMarkupId(true);
        resourceForm.add(accountLink);

        CheckBox forceMandatoryConstraint =
                new CheckBox("forceMandatoryConstraint");
        forceMandatoryConstraint.setOutputMarkupId(true);
        resourceForm.add(forceMandatoryConstraint);

        DropDownChoice<PropagationMode> optionalPropagationMode =
                new DropDownChoice<PropagationMode>("optionalPropagationMode");
        optionalPropagationMode.setModel(new IModel<PropagationMode>() {

            @Override
            public PropagationMode getObject() {
                return resourceTO.getOptionalPropagationMode();
            }

            @Override
            public void setObject(final PropagationMode object) {
                resourceTO.setOptionalPropagationMode(object);
            }

            @Override
            public void detach() {
            }
        });
        optionalPropagationMode.setChoices(
                Arrays.asList(PropagationMode.values()));
        optionalPropagationMode.setOutputMarkupId(true);
        resourceForm.add(optionalPropagationMode);

        final DropDownChoice<TrackingMode> createsTrackingMode =
                new DropDownChoice<TrackingMode>("createsTrackingMode",
                new PropertyModel<TrackingMode>(resourceTO, "createsTrackingMode"),
                Arrays.asList(TrackingMode.values()));
        resourceForm.add(createsTrackingMode);

        final DropDownChoice<TrackingMode> updatesTrackingMode =
                new DropDownChoice<TrackingMode>("updatesTrackingMode",
                new PropertyModel<TrackingMode>(resourceTO, "updatesTrackingMode"),
                Arrays.asList(TrackingMode.values()));
        resourceForm.add(updatesTrackingMode);

        final DropDownChoice<TrackingMode> deletesTrackingMode =
                new DropDownChoice<TrackingMode>("deletesTrackingMode",
                new PropertyModel<TrackingMode>(resourceTO, "deletesTrackingMode"),
                Arrays.asList(TrackingMode.values()));
        resourceForm.add(deletesTrackingMode);

        ChoiceRenderer renderer = new ChoiceRenderer("displayName", "id");
        DropDownChoice<ConnInstanceTO> connector =
                new DropDownChoice<ConnInstanceTO>("connectors",
                new Model<ConnInstanceTO>(connectorTO), connectors, renderer);
        connector.setEnabled(createFlag);
        connector.setModel(new IModel<ConnInstanceTO>() {

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
        connector.setRequired(true);
        connector.setEnabled(createFlag);
        resourceForm.add(connector);

        mappingContainer = new WebMarkupContainer("mappingContainer");
        mappingContainer.setOutputMarkupId(true);
        resourceForm.add(mappingContainer);

        ListView<SchemaMappingTO> mappings = new ListView<SchemaMappingTO>(
                "mappings", resourceTO.getMappings()) {

            @Override
            protected void populateItem(
                    final ListItem<SchemaMappingTO> item) {

                final SchemaMappingTO mappingTO = item.getModelObject();

                item.add(new AjaxDecoratedCheckbox("toRemove",
                        new Model(Boolean.FALSE)) {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        int index = -1;
                        for (int i = 0; i < resourceTO.getMappings().size()
                                && index == -1; i++) {

                            if (mappingTO.equals(
                                    resourceTO.getMappings().get(i))) {

                                index = i;
                            }
                        }

                        if (index != -1) {
                            resourceTO.getMappings().remove(index);
                            item.getParent().removeAll();
                            target.addComponent(mappingContainer);
                        }
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    final CharSequence script) {

                                return "if (confirm('"
                                        + getString("confirmDelete") + "'))"
                                        + "{" + script + "} "
                                        + "else {this.checked = false;}";
                            }
                        };
                    }
                });

                final DropDownChoice<String> schemaAttrChoice =
                        new DropDownChoice<String>(
                        "sourceAttrNames", new PropertyModel<String>(
                        mappingTO, "sourceAttrName"), (IModel) null);

                schemaAttrChoice.add(
                        new AjaxFormComponentUpdatingBehavior("onblur") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget art) {
                                mappingTO.setSourceAttrName(
                                        schemaAttrChoice.getModelObject());
                            }
                        });

                schemaAttrChoice.setOutputMarkupId(true);

                if (mappingTO.getSourceMappingType() == null) {
                    schemaAttrChoice.setChoices(Collections.EMPTY_LIST);
                } else {
                    switch (mappingTO.getSourceMappingType()) {
                        case UserSchema:
                            schemaAttrChoice.setChoices(uSchemaAttrNames);
                            break;

                        case UserDerivedSchema:
                            schemaAttrChoice.setChoices(uDerSchemaAttrNames);
                            break;

                        case UserVirtualSchema:
                            schemaAttrChoice.setChoices(uVirSchemaAttrNames);
                            break;

                        case SyncopeUserId:
                            schemaAttrChoice.setEnabled(false);
                            schemaAttrChoice.setRequired(false);
                            schemaAttrChoice.setChoices(Collections.EMPTY_LIST);
                            mappingTO.setSourceAttrName("SyncopeUserId");
                            break;

                        case Password:
                            schemaAttrChoice.setEnabled(false);
                            schemaAttrChoice.setRequired(false);
                            schemaAttrChoice.setChoices(Collections.EMPTY_LIST);
                            mappingTO.setSourceAttrName("Password");
                            break;

                        default:
                            schemaAttrChoice.setChoices(
                                    Collections.EMPTY_LIST);
                    }
                }
                item.add(schemaAttrChoice);

                item.add(new SourceMappingTypesDropDownChoice(
                        "sourceMappingTypes",
                        new PropertyModel<SourceMappingType>(mappingTO,
                        "sourceMappingType"), sourceMappingTypes,
                        schemaAttrChoice).setRequired(true).
                        setOutputMarkupId(true));

                final TextField<String> destAttrName = new TextField<String>(
                        "destAttrName", new PropertyModel(
                        mappingTO, "destAttrName"));
                destAttrName.setRequired(true);
                destAttrName.setLabel(new Model(getString("fieldName")));
                destAttrName.setOutputMarkupId(true);

                destAttrName.add(
                        new AjaxFormComponentUpdatingBehavior("onblur") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget art) {
                                mappingTO.setDestAttrName(
                                        destAttrName.getModelObject());
                            }
                        });
                item.add(destAttrName);

                final AutoCompleteTextField<String> mandatoryCondirion =
                        new AutoCompleteTextField<String>("mandatoryCondition",
                        new PropertyModel(mappingTO, "mandatoryCondition")) {

                            @Override
                            protected Iterator getChoices(final String input) {
                                List<String> choices;
                                if ("true".startsWith(input.toLowerCase())) {
                                    choices = Collections.singletonList("true");
                                } else if ("false".startsWith(input.toLowerCase())) {
                                    choices = Collections.singletonList("true");
                                } else {
                                    choices = Collections.EMPTY_LIST;
                                }

                                return choices.iterator();
                            }
                        };

                mandatoryCondirion.add(
                        new AjaxFormComponentUpdatingBehavior("onblur") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget art) {
                                mappingTO.setMandatoryCondition(
                                        mandatoryCondirion.getModelObject());
                            }
                        });
                item.add(mandatoryCondirion);

                final CheckBox accountId = new CheckBox("accountId",
                        new PropertyModel(mappingTO, "accountid"));

                accountId.add(
                        new AjaxFormComponentUpdatingBehavior("onchange") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget art) {
                                mappingTO.setAccountid(
                                        accountId.getModelObject());
                            }
                        });

                item.add(accountId);

                final CheckBox password = new CheckBox("password",
                        new PropertyModel(mappingTO, "password"));
                password.add(
                        new AjaxFormComponentUpdatingBehavior("onchange") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget art) {
                                mappingTO.setPassword(
                                        password.getModelObject());
                            }
                        });
                item.add(password);
            }
        };
        mappings.setReuseItems(true);
        mappingContainer.add(mappings);

        AjaxButton addSchemaMappingBtn = new IndicatingAjaxButton(
                "addUserSchemaMappingBtn", new Model(getString("add"))) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                resourceTO.getMappings().add(new SchemaMappingTO());
                target.addComponent(mappingContainer);
            }
        };
        addSchemaMappingBtn.setDefaultFormProcessing(false);
        resourceForm.add(addSchemaMappingBtn);

        AjaxButton submit = new IndicatingAjaxButton("submit", new Model(
                getString("submit"))) {

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
                    basePage.setOperationResult(false);
                } else {
                    try {
                        if (createFlag) {
                            restClient.create(resourceTO);
                        } else {
                            restClient.update(resourceTO);
                        }

                        basePage.setOperationResult(true);
                        window.close(target);
                    } catch (SyncopeClientCompositeErrorException e) {
                        error(getString("error") + ":" + e.getMessage());
                        basePage.setOperationResult(false);

                        LOG.error("While creating or updating resource "
                                + resourceTO);
                    }
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.addComponent(feedbackPanel);
            }
        };
        resourceForm.add(submit);

        add(resourceForm);

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                xmlRolesReader.getAllAllowedRoles("Resources",
                createFlag ? "create" : "update"));
    }

    /**
     * Extension class of DropDownChoice.
     * It's purposed for storing values in the
     * corresponding property model after pressing 'Add' button.
     */
    private class SourceMappingTypesDropDownChoice extends DropDownChoice {

        public SourceMappingTypesDropDownChoice(final String id,
                final PropertyModel<SourceMappingType> model,
                final IModel imodel,
                final DropDownChoice<String> chooserToPopulate) {

            super(id, model, imodel);

            add(new AjaxFormComponentUpdatingBehavior("onchange") {

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    chooserToPopulate.setChoices(
                            new LoadableDetachableModel<List<String>>() {

                                @Override
                                protected List<String> load() {
                                    List<String> result;
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
                                        default:
                                            result = Collections.EMPTY_LIST;
                                    }

                                    return result;
                                }
                            });
                    target.addComponent(chooserToPopulate);
                    target.addComponent(mappingContainer);
                }
            });
        }
    }
}
