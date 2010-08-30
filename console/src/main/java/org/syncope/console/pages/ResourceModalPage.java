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

import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
import org.syncope.console.SyncopeApplication;
import org.syncope.console.rest.ConnectorsRestClient;
import org.syncope.console.rest.ResourcesRestClient;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.UpdatingCheckBox;
import org.syncope.console.wicket.markup.html.form.UpdatingDropDownChoice;
import org.syncope.console.wicket.markup.html.form.UpdatingTextField;
import org.syncope.types.SchemaType;

/**
 * Modal window with Connector form.
 */
public class ResourceModalPage extends SyncopeModalPage {

    public TextField resourceName;

    public DropDownChoice connector;

    ConnectorInstanceTO connectorTO = new ConnectorInstanceTO();

    SchemaMappingTOs userSchemas = new SchemaMappingTOs();

    SchemaMappingTOs roleSchemas = new SchemaMappingTOs();

    public AjaxButton submit;

    public AjaxButton addUserSchemaMappingBtn;

    public AjaxButton addRoleSchemaMappingBtn;

    ListView mappingUserSchemaView;

    ListView mappingRoleSchemaView;

    @SpringBean(name = "resourcesRestClient")
    ResourcesRestClient restClient;

    WebMarkupContainer mappingUserSchemaContainer;

    WebMarkupContainer mappingRoleSchemaContainer;

    /**
     *
     * @param basePage base
     * @param modalWindow modal window
     * @param connectorTO
     * @param create : set to true only if a CREATE operation is required
     */
    public ResourceModalPage(final BasePage basePage, final ModalWindow window,
            final ResourceTO resourceTO, final boolean createFlag) {

        setupSchemaLists(resourceTO.getMappings());

        Form resourceForm = new Form("ResourceForm");

        resourceForm.setModel(new CompoundPropertyModel(resourceTO));

        if (!createFlag)
            connectorTO.setId(resourceTO.getConnectorId());

        IModel connectors = new LoadableDetachableModel() {

            @Override
            protected Object load() {

                ConnectorsRestClient connectorRestClient = (ConnectorsRestClient) ((SyncopeApplication) Application.get()).getApplicationContext().
                        getBean("connectorsRestClient");

                return connectorRestClient.getAllConnectors().getInstances();
            }
        };

        final IModel userSchemasList = new LoadableDetachableModel() {

            @Override
            protected Object load() {

                SchemaRestClient schemaRestClient = (SchemaRestClient) ((SyncopeApplication) Application.get()).getApplicationContext().getBean("schemaRestClient");

                return schemaRestClient.getAllUserSchemasNames();
            }
        };

        final IModel roleSchemasList = new LoadableDetachableModel() {

            @Override
            protected Object load() {

                SchemaRestClient schemaRestClient = (SchemaRestClient) ((SyncopeApplication) Application.get()).getApplicationContext().getBean("schemaRestClient");

                return schemaRestClient.getAllRoleSchemasNames();
            }
        };

        resourceName = new TextField("name");
        resourceName.setEnabled(createFlag);
        resourceName.setRequired(true);
        resourceName.setOutputMarkupId(true);

        resourceForm.add(resourceName);

        ChoiceRenderer renderer = new ChoiceRenderer("connectorName", "id");
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

        mappingUserSchemaView = new ListView("mappingsUserSchema", userSchemas.getMappings()) {

            SchemaMappingTO mappingTO = null;

            @Override
            protected void populateItem(ListItem item) {
                mappingTO = (SchemaMappingTO) item.getDefaultModelObject();

                item.add(new AjaxCheckBox("toRemove", new Model(new Boolean(""))) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        int id = new Integer(getParent().getId());
                        userSchemas.getMappings().remove(id);
                        target.addComponent(mappingUserSchemaContainer);
                    }
                });
                item.add(new UpdatingTextField("field", new PropertyModel(mappingTO, "field")).setRequired(true));
                item.add(new UpdatingDropDownChoice("userSchema", new PropertyModel(mappingTO, "userSchema"), userSchemasList).setRequired(true));
                item.add(new UpdatingCheckBox("nullable", new PropertyModel(mappingTO, "nullable")));
                item.add(new UpdatingCheckBox("accountId", new PropertyModel(mappingTO, "accountid")));
                item.add(new UpdatingCheckBox("password", new PropertyModel(mappingTO, "password")));
            }
        };

        mappingUserSchemaContainer = new WebMarkupContainer("mappingUserSchemaContainer");
        mappingUserSchemaContainer.add(mappingUserSchemaView);
        mappingUserSchemaContainer.setOutputMarkupId(true);

        resourceForm.add(mappingUserSchemaContainer);

        addUserSchemaMappingBtn = new AjaxButton("addUserSchemaMappingBtn", new Model(getString("add"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                userSchemas.addMapping(new SchemaMappingTO());
                target.addComponent(mappingUserSchemaContainer);
            }
        };

        addUserSchemaMappingBtn.setDefaultFormProcessing(false);

        resourceForm.add(addUserSchemaMappingBtn);

        mappingRoleSchemaView = new ListView("mappingsRoleSchema", roleSchemas.getMappings()) {

            SchemaMappingTO mappingTO = null;

            @Override
            protected void populateItem(ListItem item) {
                mappingTO = (SchemaMappingTO) item.getDefaultModelObject();

                item.add(new AjaxCheckBox("toRemove", new Model(new Boolean(""))) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        int id = new Integer(getParent().getId());
                        roleSchemas.getMappings().remove(id);
                        target.addComponent(mappingRoleSchemaContainer);
                    }
                });
                item.add(new UpdatingTextField("field", new PropertyModel(mappingTO, "field")).setRequired(true));
                item.add(new UpdatingDropDownChoice("roleSchema", new PropertyModel(mappingTO, "roleSchema"), roleSchemasList).setRequired(true));
                item.add(new UpdatingCheckBox("nullable", new PropertyModel(mappingTO, "nullable")));
                item.add(new UpdatingCheckBox("accountId", new PropertyModel(mappingTO, "accountid")));
                item.add(new UpdatingCheckBox("password", new PropertyModel(mappingTO, "password")));
            }
        };

        mappingRoleSchemaContainer = new WebMarkupContainer("mappingRoleSchemaContainer");
        mappingRoleSchemaContainer.add(mappingRoleSchemaView);
        mappingRoleSchemaContainer.setOutputMarkupId(true);

        resourceForm.add(mappingRoleSchemaContainer);

        addRoleSchemaMappingBtn = new AjaxButton("addRoleSchemaMappingBtn", new Model(getString("add"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                roleSchemas.addMapping(new SchemaMappingTO());
                target.addComponent(mappingRoleSchemaContainer);
            }
        };

        addRoleSchemaMappingBtn.setDefaultFormProcessing(false);

        resourceForm.add(addRoleSchemaMappingBtn);

        submit = new AjaxButton("submit", new Model(getString("submit"))) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                ResourceTO resourceTO = (ResourceTO) form.getDefaultModelObject();
                resourceTO = mergeSchemaLists(resourceTO);
                try {

                    if (createFlag)
                        restClient.createResource(resourceTO);
                    else
                        restClient.updateResource(resourceTO);

                    Resources callerPage = (Resources) basePage;
                    callerPage.setOperationResult(true);

                    window.close(target);

                } catch (Exception e) {
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
     * Set User and Role Schemas list for populating different views.
     * @param schemaMappingTos
     */
    public void setupSchemaLists(SchemaMappingTOs schemaMappingTos) {
        userSchemas = new SchemaMappingTOs();
        roleSchemas = new SchemaMappingTOs();

        SchemaType schemaType = null;

        if (schemaMappingTos != null)
            for (SchemaMappingTO schemaMappingTO :
                    schemaMappingTos.getMappings()) {

                schemaType = schemaMappingTO.getSchemaType();

                if (SchemaType.UserSchema.equals(schemaType)) {
                    userSchemas.addMapping(schemaMappingTO);
                }

                if (SchemaType.RoleSchema.equals(schemaType)) {
                    roleSchemas.addMapping(schemaMappingTO);
                }
            }
        else {
            userSchemas.addMapping(new SchemaMappingTO());
            roleSchemas.addMapping(new SchemaMappingTO());
        }
    }

    /**
     * Merge userSchemas and roleSchemas into resourceTO object.
     * @param resourceTO object
     */
    public ResourceTO mergeSchemaLists(ResourceTO resourceTO) {

        SchemaMappingTOs schemaMappingTOs = new SchemaMappingTOs();

        schemaMappingTOs.getMappings().addAll(userSchemas.getMappings());
        schemaMappingTOs.getMappings().addAll(roleSchemas.getMappings());

        resourceTO.setMappings(schemaMappingTOs);

        return resourceTO;
    }
    /**
     * Extension class of TextField. It's purposed for storing values in the
     * corresponding property model after pressing 'Add' button.
     */
//    private static class UpdatingTextField extends TextField
//    {
//        public UpdatingTextField( String id, IModel model )
//        {
//            super( id, model );
//            add( new AjaxFormComponentUpdatingBehavior( "onblur" )
//            {
//                protected void onUpdate( AjaxRequestTarget target )
//                {
//                }
//            } );
//        }
//    }
    /**
     * Extension class of DropDownChoice. It's purposed for storing values in the
     * corresponding property model after pressing 'Add' button.
     */
//     private static class UpdatingDropDownChoice extends DropDownChoice
//     {
//        private UpdatingDropDownChoice(String id, PropertyModel model, IModel imodel) {
//            super(id, model, imodel);
//            add( new AjaxFormComponentUpdatingBehavior( "onblur" )
//            {
//                protected void onUpdate( AjaxRequestTarget target )
//                {
//                }
//            } );
//        }
//     }
    /**
     * Extension class of CheckBox. It's purposed for storing values in the
     * corresponding property model after pressing 'Add' button.
     */
//     private static class UpdatingCheckBox extends AjaxCheckBox
//     {
//        public UpdatingCheckBox(String id, IModel<Boolean> model) {
//            super(id, model);
//        }
//
//        @Override
//        protected void onUpdate(AjaxRequestTarget target) {
//
//        }
//     }
}
