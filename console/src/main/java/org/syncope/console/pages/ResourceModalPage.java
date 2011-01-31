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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.authorization.strategies.role.metadata
                                            .MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
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
import org.apache.wicket.util.string.Strings;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.rest.ConnectorRestClient;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.UpdatingAutoCompleteTextField;
import org.syncope.console.wicket.markup.html.form.UpdatingCheckBox;
import org.syncope.console.wicket.markup.html.form.UpdatingDropDownChoice;
import org.syncope.console.wicket.markup.html.form.UpdatingTextField;
import org.syncope.types.SourceMappingType;

/**
 * Modal window with Resource form.
 */
public class ResourceModalPage extends BaseModalPage {

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private ConnectorRestClient connectorRestClient;

    private TextField resourceName;

    private DropDownChoice connector;

    private CheckBox forceMandatoryConstraint;

    private ConnectorInstanceTO connectorTO = new ConnectorInstanceTO();

    private ResourceTO resource;

    private AjaxButton submit;

    private AjaxButton addSchemaMappingBtn;

    private List<String> accountIdAttributesNames;

    private List<String> passwordAttributesNames;

    private List<String> userSchemaAttributesNames;

    private List<String> roleSchemaAttributesNames;

    private List<String> membershipSchemaAttributesNames;

    /** Custom validation's errors map*/
    private Map<String, String> errors = new HashMap<String, String>();

    private ListView mappingUserSchemaView;

    @SpringBean
    private ResourceRestClient restClient;

    private WebMarkupContainer mappingUserSchemaContainer;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public ResourceModalPage(final BasePage basePage, final ModalWindow window,
            final ResourceTO resourceTO, final boolean createFlag) {

        this.resource = resourceTO;

        setupChoiceListsPopulators();

        //setupSchemaMappingsList(resourceTO.getMappings());

        Form resourceForm = new Form("ResourceForm");

        resourceForm.setModel(new CompoundPropertyModel(resourceTO));

        if (!createFlag) {
            connectorTO.setId(resourceTO.getConnectorId());
        }

        IModel connectors = new LoadableDetachableModel() {

            @Override
            protected Object load() {
                return connectorRestClient.getAllConnectors();
            }
        };

        final IModel sourceMappingTypes = new LoadableDetachableModel() {

            @Override
            protected Object load() {

                return Arrays.asList(SourceMappingType.values());

            }
        };

        resourceName = new TextField("name");
        resourceName.setEnabled(createFlag);
        resourceName.setRequired(true);
        resourceName.setOutputMarkupId(true);

        resourceForm.add(resourceName);

        forceMandatoryConstraint = new CheckBox("forceMandatoryConstraint");
        forceMandatoryConstraint.setOutputMarkupId(true);
        resourceForm.add(forceMandatoryConstraint);

        ChoiceRenderer renderer = new ChoiceRenderer("displayName", "id");
        connector = new DropDownChoice("connectors", new Model(connectorTO),
                connectors, renderer);
        connector.setEnabled(createFlag);
        connector.setModel(new IModel() {

            @Override
            public Object getObject() {
                return connectorTO;
            }

            @Override
            public void setObject(Object object) {
                ConnectorInstanceTO connector = (ConnectorInstanceTO) object;
                resourceTO.setConnectorId(connector.getId());
            }

            @Override
            public void detach() {
            }
        });

        connector.setRequired(true);
        connector.setEnabled(createFlag);

        resourceForm.add(connector);

        mappingUserSchemaView = new ListView("mappingsUserSchema",
                resourceTO.getMappings()) {

            SchemaMappingTO mappingTO = null;

            UpdatingDropDownChoice schemaAttributeChoice = null;

            @Override
            protected void populateItem(ListItem item) {
                mappingTO = (SchemaMappingTO) item.getDefaultModelObject();

                item.add(new AjaxCheckBox("toRemove", new Model(new Boolean(""))) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        int id = new Integer(getParent().getId());
                        resourceTO.getMappings().remove(id);
                        target.addComponent(mappingUserSchemaContainer);
                    }
                });
                item.add(new UpdatingTextField("field",
                        new PropertyModel(mappingTO, "destAttrName")).
                        setRequired(true).
                        setLabel(
                        new Model(getString("fieldName"))));

                schemaAttributeChoice =
                        new UpdatingDropDownChoice("schemaAttributes",
                        new PropertyModel(mappingTO, "sourceAttrName"), null);

                if (mappingTO.getSourceMappingType() == null) {
                    schemaAttributeChoice.setChoices(Collections.emptyList());
                } else if (mappingTO.getSourceMappingType().equals(
                        SourceMappingType.UserSchema)) {
                    schemaAttributeChoice.setChoices(userSchemaAttributesNames);
                    schemaAttributeChoice.setRequired(true);
                } else if (mappingTO.getSourceMappingType().equals(
                        SourceMappingType.RoleSchema)) {
                    schemaAttributeChoice.setChoices(roleSchemaAttributesNames);
                    schemaAttributeChoice.setRequired(true);
                } else if (mappingTO.getSourceMappingType().equals(
                        SourceMappingType.MembershipSchema)) {
                    schemaAttributeChoice.setChoices(
                            membershipSchemaAttributesNames);
                    schemaAttributeChoice.setRequired(true);
                } else if (mappingTO.getSourceMappingType().equals(
                        SourceMappingType.SyncopeUserId)) {
                    schemaAttributeChoice.setEnabled(false);
                    schemaAttributeChoice.setRequired(false);
                    schemaAttributeChoice.setChoices(Collections.emptyList());
                    mappingTO.setSourceAttrName("SyncopeUserId");
                } else if (mappingTO.getSourceMappingType().equals(
                        SourceMappingType.Password)) {
                    schemaAttributeChoice.setEnabled(false);
                    schemaAttributeChoice.setRequired(false);
                    schemaAttributeChoice.setChoices(Collections.emptyList());
                    mappingTO.setSourceAttrName("Password");
                }

                schemaAttributeChoice.setOutputMarkupId(true);
                item.add(schemaAttributeChoice);

                item.add(new SourceMappingTypesDropDownChoice(
                        "sourceMappingTypes",
                        new PropertyModel(mappingTO, "sourceMappingType"),
                        sourceMappingTypes, schemaAttributeChoice).setRequired(
                        true).
                        setOutputMarkupId(true));

                item.add(new UpdatingAutoCompleteTextField("mandatoryCondition",
                        new PropertyModel(mappingTO, "mandatoryCondition")) {

                    @Override
                    protected Iterator getChoices(String input) {
                        List<String> choices = new ArrayList<String>();

                        if (Strings.isEmpty(input)) {
                            choices = Collections.emptyList();
                            return choices.iterator();
                        }

                        if ("true".startsWith(input.toLowerCase())) {
                            choices.add("true");
                        } else if ("false".startsWith(input.toLowerCase())) {
                            choices.add("false");
                        }


                        return choices.iterator();
                    }
                });
                item.add(new UpdatingCheckBox("accountId",
                        new PropertyModel(mappingTO, "accountid")));
                item.add(new UpdatingCheckBox("password",
                        new PropertyModel(mappingTO, "password")));
            }
        };

        mappingUserSchemaContainer =
                new WebMarkupContainer("mappingUserSchemaContainer");
        mappingUserSchemaContainer.add(mappingUserSchemaView);
        mappingUserSchemaContainer.setOutputMarkupId(true);

        resourceForm.add(mappingUserSchemaContainer);

        addSchemaMappingBtn = new IndicatingAjaxButton("addUserSchemaMappingBtn",
                new Model(getString("add"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                resourceTO.getMappings().add(new SchemaMappingTO());
                target.addComponent(mappingUserSchemaContainer);
            }
        };

        addSchemaMappingBtn.setDefaultFormProcessing(false);

        resourceForm.add(addSchemaMappingBtn);

        submit = new IndicatingAjaxButton("submit", new Model(
                getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                ResourceTO resourceTO =
                        (ResourceTO) form.getDefaultModelObject();

                try {
                    resourceFormCustomValidation();
                } catch (IllegalArgumentException e) {

                    for (String error : errors.values()) {
                        error(error);
                    }

                    errors.clear();
                    return;
                }

                try {

                    if (createFlag) {
                        restClient.createResource(resourceTO);
                    } else {
                        restClient.updateResource(resourceTO);
                    }

                    Resources callerPage = (Resources) basePage;
                    callerPage.setOperationResult(true);

                    window.close(target);

                } catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedbackPanel);
            }
        };

        String allowedRoles;

        if (createFlag) {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Resources",
                    "create");
        } else {
            allowedRoles = xmlRolesReader.getAllAllowedRoles("Resources",
                    "update");
        }

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                allowedRoles);

        resourceForm.add(submit);

        add(resourceForm);
    }

    /**
     * Checks that at most one SchemaMapping has been set as 'AccountId' and as
     * 'Password'.
     */
    public void resourceFormCustomValidation() {
        int count = 0;

        for (SchemaMappingTO schemaMapping : resource.getMappings()) {

            if (schemaMapping.isAccountid()) {
                count++;
            }

            if (count > 1) {
                errors.put("accountId", getString("accountIdValidation"));
                break;
            }

        }

        count = 0;

        for (SchemaMappingTO schemaMapping : resource.getMappings()) {

            if (schemaMapping.isPassword()) {
                count++;
            }

            if (count > 1) {
                errors.put("password", getString("passwordValidation"));
                break;
            }


        }

        if (errors.size() > 0) {
            throw new IllegalArgumentException(getString("customValidation"));
        }
    }

    /**
     * Set User and Role Schemas list for populating different views.
     * @param schemaMappingTos
    
    public void setupSchemaMappingsList(List<SchemaMappingTO> mappings) {
    schemaMappingTOs = new ArrayList<SchemaMappingTO>();

    if (mappings != null) {
    for (SchemaMappingTO schemaMappingTO :  mappings) {
    schemaMappingTOs.add(schemaMappingTO);
    }
    } else {
    schemaMappingTOs.add(new SchemaMappingTO());
    }
    }*/
    /**
     * Setup choice-list populators.
     */
    public void setupChoiceListsPopulators() {
        List<String> accountIdList = new ArrayList<String>();
        accountIdList.add("accountId");
        setAccountIdAttributesNames(accountIdList);

        List<String> passwordList = new ArrayList<String>();
        passwordList.add("password");
        setPasswordAttributesNames(passwordList);

        setRoleSchemaAttributesNames(schemaRestClient.getAllRoleSchemasNames());
        setUserSchemaAttributesNames(schemaRestClient.getAllUserSchemasNames());
        setMembershipSchemaAttributesNames(schemaRestClient.
                getAllMembershipSchemasNames());
    }

    public List<String> getMembershipSchemaAttributesNames() {
        return membershipSchemaAttributesNames;
    }

    public void setMembershipSchemaAttributesNames(
            List<String> membershipSchemaAttributesNames) {
        this.membershipSchemaAttributesNames = membershipSchemaAttributesNames;
    }

    public List<String> getRoleSchemaAttributesNames() {
        return roleSchemaAttributesNames;
    }

    public void setRoleSchemaAttributesNames(
            List<String> roleSchemaAttributesNames) {
        this.roleSchemaAttributesNames = roleSchemaAttributesNames;
    }

    public List<String> getUserSchemaAttributesNames() {
        return userSchemaAttributesNames;
    }

    public void setUserSchemaAttributesNames(
            List<String> userSchemaAttributesNames) {
        this.userSchemaAttributesNames = userSchemaAttributesNames;
    }

    public List<String> getAccountIdAttributesNames() {
        return accountIdAttributesNames;
    }

    public void setAccountIdAttributesNames(
            List<String> accountIdAttributesNames) {
        this.accountIdAttributesNames = accountIdAttributesNames;
    }

    public List<String> getPasswordAttributesNames() {
        return passwordAttributesNames;
    }

    public void setPasswordAttributesNames(List<String> passwordAttributesNames) {
        this.passwordAttributesNames = passwordAttributesNames;
    }

    /**
     * Extension class of DropDownChoice. It's purposed for storing values in the
     * corresponding property model after pressing 'Add' button.
     */
    public class SourceMappingTypesDropDownChoice extends DropDownChoice {

        SchemaMappingTO schemaMappingModel;

        public SourceMappingTypesDropDownChoice(String id, final PropertyModel model,
                IModel imodel, final DropDownChoice chooserToPopulate) {
            super(id, model, imodel);

            schemaMappingModel = (SchemaMappingTO) model.getTarget();

            add(new AjaxFormComponentUpdatingBehavior("onchange") {

                protected void onUpdate(AjaxRequestTarget target) {
                    chooserToPopulate.setChoices(new LoadableDetachableModel() {

                        @Override
                        protected Object load() {
                            SourceMappingType sourceMType = schemaMappingModel.
                                    getSourceMappingType();

                            if (sourceMType == null) {
                                return Collections.emptyList();
                            }
                            if (sourceMType.equals(SourceMappingType.RoleSchema)) {
                                return roleSchemaAttributesNames;
                            } else if (sourceMType.equals(
                                    SourceMappingType.UserSchema)) {
                                return userSchemaAttributesNames;
                            } else if (sourceMType.equals(
                                    SourceMappingType.MembershipSchema)) {
                                return membershipSchemaAttributesNames;
                            } else if (sourceMType.equals(
                                    SourceMappingType.SyncopeUserId)) {
                                return Collections.emptyList();
                            } else if (sourceMType.equals(
                                    SourceMappingType.Password)) {
                                return Collections.emptyList();
                            } else {
                                return Collections.emptyList();
                            }

                        }
                    });
                    target.addComponent(chooserToPopulate);
                    target.addComponent(mappingUserSchemaContainer);
                }
            });
        }
    }
}
