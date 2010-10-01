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
import java.util.List;
import java.util.Map;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.rest.ConnectorsRestClient;
import org.syncope.console.rest.ResourcesRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.UpdatingCheckBox;
import org.syncope.console.wicket.markup.html.form.UpdatingDropDownChoice;
import org.syncope.console.wicket.markup.html.form.UpdatingTextField;
import org.syncope.types.SchemaType;

/**
 * Modal window with Resource form.
 */
public class ResourceModalPage extends SyncopeModalPage {

    public TextField resourceName;
    public DropDownChoice connector;
    public CheckBox forceMandatoryConstraint;

    ConnectorInstanceTO connectorTO = new ConnectorInstanceTO();
    SchemaMappingTOs schemaMappingTOs = new SchemaMappingTOs();

    public AjaxButton submit;
    public AjaxButton addSchemaMappingBtn;

    private List<String> accountIdAttributesNames;
    private List<String> passwordAttributesNames;
    private List<String> userSchemaAttributesNames;
    private List<String> roleSchemaAttributesNames;
    private List<String> membershipSchemaAttributesNames;

    /** Custom validation's errors map*/
    private Map<String,String> errors = new HashMap<String,String>();

    ListView mappingUserSchemaView;

    @SpringBean(name = "resourcesRestClient")
    ResourcesRestClient restClient;

    WebMarkupContainer mappingUserSchemaContainer;

    SchemaRestClient schemaRestClient;
    
    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public ResourceModalPage(final BasePage basePage, final ModalWindow window,
            final ResourceTO resourceTO, final boolean createFlag) {

        schemaRestClient = (SchemaRestClient) ((SyncopeApplication) Application.get())
                .getApplicationContext().getBean("schemaRestClient");

        setupChoiceListsPopulators();

        setupSchemaMappingsList(resourceTO.getMappings());

        Form resourceForm = new Form("ResourceForm");

        resourceForm.setModel(new CompoundPropertyModel(resourceTO));

        if (!createFlag) {
            connectorTO.setId(resourceTO.getConnectorId());
        }

        IModel connectors = new LoadableDetachableModel() {

            @Override
            protected Object load() {

                ConnectorsRestClient connectorRestClient = (ConnectorsRestClient)
                        ((SyncopeApplication) Application.get()).getApplicationContext().
                        getBean("connectorsRestClient");

                return connectorRestClient.getAllConnectors().getInstances();
            }
        };

        final IModel schemaTypesList = new LoadableDetachableModel() {

            @Override
            protected Object load() {

                return Arrays.asList(SchemaType.values());

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
        connector = new DropDownChoice("connectors", new Model(connectorTO), connectors, renderer);
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

        mappingUserSchemaView = new ListView("mappingsUserSchema", schemaMappingTOs.getMappings()) {

            SchemaMappingTO mappingTO = null;
            UpdatingDropDownChoice schemaAttributeChoice = null;

            @Override
            protected void populateItem(ListItem item) {
                mappingTO = (SchemaMappingTO) item.getDefaultModelObject();

                item.add(new AjaxCheckBox("toRemove", new Model(new Boolean(""))) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        int id = new Integer(getParent().getId());
                        schemaMappingTOs.getMappings().remove(id);
                        target.addComponent(mappingUserSchemaContainer);
                    }
                });
                item.add(new UpdatingTextField("field", new PropertyModel(mappingTO, "field")).
                        setRequired(true).setLabel(new Model(getString("fieldName"))));

                schemaAttributeChoice = new UpdatingDropDownChoice("schemaAttributes",
                        new PropertyModel(mappingTO, "schemaName"),null);
                
                if(mappingTO.getSchemaType() == null)
                    schemaAttributeChoice.setChoices(Collections.emptyList());
                else if (mappingTO.getSchemaType().equals(SchemaType.UserSchema)){
                    schemaAttributeChoice.setChoices(userSchemaAttributesNames);
                    schemaAttributeChoice.setRequired(true);
                }
                else if (mappingTO.getSchemaType().equals(SchemaType.RoleSchema)){
                    schemaAttributeChoice.setChoices(roleSchemaAttributesNames);
                    schemaAttributeChoice.setRequired(true);
                }
                else if (mappingTO.getSchemaType().equals(SchemaType.MembershipSchema)){
                    schemaAttributeChoice.setChoices(membershipSchemaAttributesNames);
                    schemaAttributeChoice.setRequired(true);
                }
                else if (mappingTO.getSchemaType().equals(SchemaType.AccountId)){
                    schemaAttributeChoice.setEnabled(false);
                    schemaAttributeChoice.setRequired(false);
                    schemaAttributeChoice.setChoices(Collections.emptyList());
                    mappingTO.setSchemaName("AccountId");
                }
                else if (mappingTO.getSchemaType().equals(SchemaType.Password)){
                    schemaAttributeChoice.setEnabled(false);
                    schemaAttributeChoice.setRequired(false);
                    schemaAttributeChoice.setChoices(Collections.emptyList());
                    mappingTO.setSchemaName("Password");
                }

                schemaAttributeChoice.setOutputMarkupId(true);
                item.add(schemaAttributeChoice);

                item.add(new SchemaTypesDropDownChoice("schemaTypes",
                         new PropertyModel(mappingTO, "schemaType"), schemaTypesList, schemaAttributeChoice)
                        .setRequired(true)
                        .setOutputMarkupId(true));

                item.add(new UpdatingCheckBox("nullable", new PropertyModel(mappingTO, "nullable")));
                item.add(new UpdatingCheckBox("accountId", new PropertyModel(mappingTO, "accountid")));
                item.add(new UpdatingCheckBox("password", new PropertyModel(mappingTO, "password")));
            }
        };

        mappingUserSchemaContainer = new WebMarkupContainer("mappingUserSchemaContainer");
        mappingUserSchemaContainer.add(mappingUserSchemaView);
        mappingUserSchemaContainer.setOutputMarkupId(true);

        resourceForm.add(mappingUserSchemaContainer);

        addSchemaMappingBtn = new AjaxButton("addUserSchemaMappingBtn", new Model(getString("add"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                schemaMappingTOs.addMapping(new SchemaMappingTO());
                target.addComponent(mappingUserSchemaContainer);
            }
        };

        addSchemaMappingBtn.setDefaultFormProcessing(false);

        resourceForm.add(addSchemaMappingBtn);

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                ResourceTO resourceTO = (ResourceTO) form.getDefaultModelObject();
                resourceTO.setMappings(schemaMappingTOs);

                try {
                    resourceFormCustomValidation();
                }
                catch (IllegalArgumentException e) {

                    for(String error : errors.values())
                        error(error);
                    
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

                } 
                catch (SyncopeClientCompositeErrorException e) {
                    error(getString("error") + ":" + e.getMessage());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(form.get("feedback"));
            }
        };

        resourceForm.add(submit);

        resourceForm.add(new FeedbackPanel("feedback").setOutputMarkupId(true));

        add(resourceForm);
    }

    /**
     * Checks that at most one SchemaMapping has been set as 'AccountId' and as
     * 'Password'.
     */
    public void resourceFormCustomValidation() {
        int count = 0;

        for (SchemaMappingTO schemaMapping : schemaMappingTOs) {

            if (schemaMapping.isAccountid()) 
                count++;

            if (count > 1){
               errors.put("accountId", getString("accountIdValidation"));
               break;
            }

            }

        count = 0;

        for (SchemaMappingTO schemaMapping : schemaMappingTOs) {

            if (schemaMapping.isPassword())
                count++;

            if (count > 1){
               errors.put("password", getString("passwordValidation"));
               break;
            }


            }

         if(errors.size() > 0)
         throw new IllegalArgumentException(getString("customValidation"));
    }


    /**
     * Set User and Role Schemas list for populating different views.
     * @param schemaMappingTos
     */
    public void setupSchemaMappingsList(SchemaMappingTOs schemaMappingTos) {
        schemaMappingTOs = new SchemaMappingTOs();

        if (schemaMappingTos != null) {
            for (SchemaMappingTO schemaMappingTO :
                    schemaMappingTos.getMappings()) {
                schemaMappingTOs.addMapping(schemaMappingTO);
            }
        } else {
            schemaMappingTOs.addMapping(new SchemaMappingTO());
        }
    }

    /**
     * Setup choice-list populators.
     */
    public void setupChoiceListsPopulators(){
        List<String> accountIdList = new ArrayList<String>();
        accountIdList.add("accountId");
        setAccountIdAttributesNames(accountIdList);

        List<String> passwordList = new ArrayList<String>();
        passwordList.add("password");
        setPasswordAttributesNames(passwordList);

        setRoleSchemaAttributesNames(schemaRestClient.getAllRoleSchemasNames());
        setUserSchemaAttributesNames(schemaRestClient.getAllUserSchemasNames());
        setMembershipSchemaAttributesNames(schemaRestClient.getAllMembershipSchemasNames());
    }

    public List<String> getMembershipSchemaAttributesNames() {
        return membershipSchemaAttributesNames;
    }

    public void setMembershipSchemaAttributesNames(List<String> membershipSchemaAttributesNames) {
        this.membershipSchemaAttributesNames = membershipSchemaAttributesNames;
    }

    public List<String> getRoleSchemaAttributesNames() {
        return roleSchemaAttributesNames;
    }

    public void setRoleSchemaAttributesNames(List<String> roleSchemaAttributesNames) {
        this.roleSchemaAttributesNames = roleSchemaAttributesNames;
    }

    public List<String> getUserSchemaAttributesNames() {
        return userSchemaAttributesNames;
    }

    public void setUserSchemaAttributesNames(List<String> userSchemaAttributesNames) {
        this.userSchemaAttributesNames = userSchemaAttributesNames;
    }

    public List<String> getAccountIdAttributesNames() {
        return accountIdAttributesNames;
    }

    public void setAccountIdAttributesNames(List<String> accountIdAttributesNames) {
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
    public class SchemaTypesDropDownChoice extends DropDownChoice {

        SchemaMappingTO schemaMappingModel;

        public SchemaTypesDropDownChoice(String id, final PropertyModel model,
                IModel imodel, final DropDownChoice chooserToPopulate) {
            super(id, model, imodel);

            schemaMappingModel = (SchemaMappingTO) model.getTarget();

            add(new AjaxFormComponentUpdatingBehavior("onchange") {

                protected void onUpdate(AjaxRequestTarget target) {
                    chooserToPopulate.setChoices(new LoadableDetachableModel() {

                        @Override
                        protected Object load() {
                            SchemaType schemaType = schemaMappingModel.getSchemaType();

                            if (schemaType == null) {
                                return Collections.emptyList();
                            }
                            if (schemaType.equals(SchemaType.RoleSchema)) {
                                return roleSchemaAttributesNames;
                            }
                            else if (schemaType.equals(SchemaType.UserSchema)) {
                                return userSchemaAttributesNames;
                            }
                            else if (schemaType.equals(SchemaType.MembershipSchema)) {
                                return membershipSchemaAttributesNames;
                            } 
                            else if (schemaType.equals(SchemaType.AccountId)) {
                                return Collections.emptyList();
                            } 
                            else if (schemaType.equals(SchemaType.Password)) {
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
