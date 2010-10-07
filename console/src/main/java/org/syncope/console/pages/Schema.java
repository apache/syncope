/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.console.rest.SchemaRestClient;

/**
 * Schema WebPage.
 */
public class Schema extends BasePage
{
    @SpringBean(name = "schemaRestClient")
    SchemaRestClient restClient;

    final ModalWindow createUserSchemaWin;
    final ModalWindow editUserSchemaWin;

    final ModalWindow createUserDerivedSchemaWin;
    final ModalWindow editUserDerivedSchemaWin;

    final ModalWindow createRoleSchemaWin;
    final ModalWindow editRoleSchemaWin;
    
    final ModalWindow createRoleDerivedSchemaWin;
    final ModalWindow editRoleDerivedSchemaWin;

    final ModalWindow createMembershipSchemaWin;
    final ModalWindow editMembershipSchemaWin;

    final ModalWindow createMembershipDerivedSchemaWin;
    final ModalWindow editMembershipDerivedSchemaWin;
    
    WebMarkupContainer userSchemaContainer;
    WebMarkupContainer userDerivedSchemaContainer;

    WebMarkupContainer roleSchemasContainer;
    WebMarkupContainer roleDerivedSchemasContainer;

    WebMarkupContainer membershipSchemaContainer;
    WebMarkupContainer membershipDerivedSchemaContainer;

    /** Response flag set by the Modal Window after the operation is completed  */
    boolean operationResult = false;
    FeedbackPanel feedbackPanel;

    /** Navigator's rows to display for single view */
    final int ROWS_TO_DISPLAY = 10;
    
    public Schema(PageParameters parameters)
    {
        super(parameters);

        add(createRoleSchemaWin = new ModalWindow("createRoleSchemaWin"));
        add(editRoleSchemaWin = new ModalWindow("editRoleSchemaWin"));

        add(createRoleDerivedSchemaWin = new ModalWindow("createRoleDerivedSchemaWin"));
        add(editRoleDerivedSchemaWin = new ModalWindow("editRoleDerivedSchemaWin"));

        add(createUserSchemaWin = new ModalWindow("createUserSchemaWin"));
        add(editUserSchemaWin = new ModalWindow("editUserSchemaWin"));
        
        add(createUserDerivedSchemaWin = new ModalWindow("createUserDerSchemaWin"));
        add(editUserDerivedSchemaWin = new ModalWindow("editUserDerSchemaWin"));

        add(createMembershipSchemaWin = new ModalWindow("createMembershipSchemaWin"));
        add(editMembershipSchemaWin = new ModalWindow("editMembershipSchemaWin"));

        add(createMembershipDerivedSchemaWin = new ModalWindow("createMembershipDerSchemaWin"));
        add(editMembershipDerivedSchemaWin = new ModalWindow("editMembershipDerSchemaWin"));

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId( true );
        
        add(feedbackPanel);

        IModel userSchemas =  new LoadableDetachableModel()
        {
            protected Object load() {
                return restClient.getAllUserSchemas().getSchemas();
            }
        };

        IModel userDerivedSchemas =  new LoadableDetachableModel()
        {
            protected Object load() {
                return restClient.getAllUserDerivedSchemas().getDerivedSchemas();
            }
        };

        IModel roleSchemas =  new LoadableDetachableModel()
        {
            protected Object load() {
                return restClient.getAllRoleSchemas().getSchemas();
            }
        };

        IModel roleDerivedSchemas =  new LoadableDetachableModel()
        {
            protected Object load() {
                return restClient.getAllRoleDerivedSchemas().getDerivedSchemas();
            }
        };

        IModel membershipSchemas =  new LoadableDetachableModel()
        {
            protected Object load() {
                return restClient.getAllMemberhipSchemas().getSchemas();
            }
        };

        IModel membershipDerivedSchemas =  new LoadableDetachableModel()
        {
            protected Object load() {
                return restClient.getAllMembershipDerivedSchemas().getDerivedSchemas();
            }
        };

        PageableListView roleSchemasView = new PageableListView("roleSchemas",
                roleSchemas, ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
                final SchemaTO schemaTO = (SchemaTO) item.getDefaultModelObject();

                item.add(new Label("name", schemaTO.getName()));
                item.add(new Label("type", schemaTO.getType().getClassName()));
                item.add(new Label("attributes", schemaTO.getAttributes() + ""));


                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final SchemaTO schemaTO = (SchemaTO) item.getDefaultModelObject();

                        editRoleSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                        public Page createPage() {
                            SchemaModalPage form = new SchemaModalPage(Schema.this, editRoleSchemaWin, schemaTO, false);
                            form.setEntity(SchemaModalPage.Entity.ROLE);
                            return form;
                        }
                        });

                        editRoleSchemaWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink"){

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteRoleSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(roleSchemasContainer);
                    }

                };

                item.add(deleteLink);
            }
        };

        add(new AjaxPagingNavigator("rolesNavigator", roleSchemasView));

        PageableListView roleDerSchemasView = new PageableListView("roleDerivedSchemas",
                roleDerivedSchemas, ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
               final DerivedSchemaTO schemaTO = (DerivedSchemaTO) item.getDefaultModelObject();

                item.add(new Label("name", schemaTO.getName()));
                item.add(new Label("expression", schemaTO.getExpression()));
                item.add(new Label("attributes", schemaTO.getDerivedAttributes() + ""));

                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final DerivedSchemaTO schemaTO = (DerivedSchemaTO) item.getDefaultModelObject();

                        editRoleDerivedSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                        public Page createPage() {
                            DerivedSchemaModalPage form = new DerivedSchemaModalPage
                                    (Schema.this, editRoleDerivedSchemaWin, schemaTO, false);
                            form.setEntity(DerivedSchemaModalPage.Entity.ROLE);
                            return form;
                        }
                        });

                        editRoleDerivedSchemaWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink"){

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteRoleDerivedSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(roleDerivedSchemasContainer);
                    }

                };

                item.add(deleteLink);
            }
        };

        add(new AjaxPagingNavigator("rolesDerivedNavigator", roleDerSchemasView));

        PageableListView userSchemasView = new PageableListView("userSchemas",
                userSchemas, ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
                final SchemaTO schemaTO = (SchemaTO) item.getDefaultModelObject();

                item.add(new Label("name", schemaTO.getName()));
                item.add(new Label("type", schemaTO.getType().getClassName()));
                item.add(new Label("attributes", schemaTO.getAttributes() + ""));


                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editUserSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                        public Page createPage() {
                            SchemaModalPage form = new SchemaModalPage(Schema.this, editUserSchemaWin, schemaTO, false);
                            form.setEntity(SchemaModalPage.Entity.USER);
                            return form;
                        }
                        });

                        editUserSchemaWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink"){

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteUserSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(userSchemaContainer);
                    }

                };

                item.add(deleteLink);
            }
        };

        add(new AjaxPagingNavigator("usersSchemaNavigator", userSchemasView));

        PageableListView userDerSchemasView = new PageableListView("userDerivedSchemas",
                userDerivedSchemas, ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
                final DerivedSchemaTO schemaTO = (DerivedSchemaTO) item.getDefaultModelObject();

                item.add(new Label("name", schemaTO.getName()));
                item.add(new Label("expression", schemaTO.getExpression()));
                item.add(new Label("attributes", schemaTO.getDerivedAttributes() + ""));

                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final DerivedSchemaTO schemaTO = (DerivedSchemaTO) item.getDefaultModelObject();

                        editUserDerivedSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                        public Page createPage() {
                            DerivedSchemaModalPage form = new DerivedSchemaModalPage
                                    (Schema.this, editUserSchemaWin, schemaTO, false);
                            form.setEntity(DerivedSchemaModalPage.Entity.USER);
                            return form;
                        }
                        });

                        editUserDerivedSchemaWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink"){

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteUserDerivedSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(userDerivedSchemaContainer);
                    }

                };

                item.add(deleteLink);
            }
        };
       
        add(new AjaxPagingNavigator("usersDerivedSchemaNavigator", userSchemasView));

        add(userDerSchemasView);

       PageableListView membershipSchemasView = new PageableListView("membershipSchemas",
               membershipSchemas, ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
                final SchemaTO schemaTO = (SchemaTO) item.getDefaultModelObject();

                item.add(new Label("name", schemaTO.getName()));
                item.add(new Label("type", schemaTO.getType().getClassName()));
                item.add(new Label("attributes", schemaTO.getAttributes() + ""));


                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final SchemaTO schemaTO = (SchemaTO) item.getDefaultModelObject();

                        editMembershipSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                        public Page createPage() {
                            SchemaModalPage form = new SchemaModalPage(Schema.this,
                                    editMembershipSchemaWin, schemaTO, false);
                            form.setEntity(SchemaModalPage.Entity.MEMBERSHIP);
                            return form;
                        }
                        });

                        editMembershipSchemaWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink"){

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteMemberhipSchema((schemaTO.getName()));

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(membershipSchemaContainer);
                    }

                };

                item.add(deleteLink);
            }
        };
       add(new AjaxPagingNavigator("membershipsNavigator", membershipSchemasView));

       PageableListView membershipDerSchemasView = new PageableListView
               ("membershipDerivedSchemas", membershipDerivedSchemas, ROWS_TO_DISPLAY) {

            @Override
            protected void populateItem(final ListItem item) {
               final DerivedSchemaTO schemaTO = (DerivedSchemaTO) item.getDefaultModelObject();

                item.add(new Label("name", schemaTO.getName()));
                item.add(new Label("expression", schemaTO.getExpression()));
                item.add(new Label("attributes", schemaTO.getDerivedAttributes() + ""));

                AjaxLink editLink = new AjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final DerivedSchemaTO schemaTO = (DerivedSchemaTO) item.getDefaultModelObject();

                        editMembershipDerivedSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                        public Page createPage() {
                            DerivedSchemaModalPage form = new DerivedSchemaModalPage
                                    (Schema.this, editMembershipDerivedSchemaWin, schemaTO, false);
                            form.setEntity(DerivedSchemaModalPage.Entity.MEMBERSHIP);
                            return form;
                        }
                        });

                        editMembershipDerivedSchemaWin.show(target);
                    }
                };

                item.add(editLink);

                AjaxLink deleteLink = new AjaxLink("deleteLink"){

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteMembershipDerivedSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(membershipDerivedSchemaContainer);
                    }

                };

                item.add(deleteLink);
            }
        };

        add(new AjaxPagingNavigator("membershipsDerNavigator",
                membershipDerSchemasView));

        roleSchemasContainer = new WebMarkupContainer("roleSchemasContainer");
        roleSchemasContainer.add(roleSchemasView);
        roleSchemasContainer.setOutputMarkupId(true);

        roleDerivedSchemasContainer = new WebMarkupContainer("roleDerivedSchemasContainer");
        roleDerivedSchemasContainer.add(roleDerSchemasView);
        roleDerivedSchemasContainer.setOutputMarkupId(true);

        userSchemaContainer = new WebMarkupContainer("userSchemaContainer");
        userSchemaContainer.add(userSchemasView);
        userSchemaContainer.setOutputMarkupId(true);

        userDerivedSchemaContainer = new WebMarkupContainer("userDerivedSchemaContainer");
        userDerivedSchemaContainer.add(userDerSchemasView);
        userDerivedSchemaContainer.setOutputMarkupId(true);

        membershipSchemaContainer = new WebMarkupContainer("membershipSchemaContainer");
        membershipSchemaContainer.add(membershipSchemasView);
        membershipSchemaContainer.setOutputMarkupId(true);

        membershipDerivedSchemaContainer = new WebMarkupContainer("membershipDerivedSchemaContainer");
        membershipDerivedSchemaContainer.add(membershipDerSchemasView);
        membershipDerivedSchemaContainer.setOutputMarkupId(true);
        
        add(roleSchemasContainer);
        add(roleDerivedSchemasContainer);

        add(userSchemaContainer);
        add(userDerivedSchemaContainer);

        add(membershipSchemaContainer);
        add(membershipDerivedSchemaContainer);

        createUserSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserSchemaWin.setPageMapName("modal-1");
        createUserSchemaWin.setCookieName("modal-1");

        editUserSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserSchemaWin.setPageMapName("modal-2");
        editUserSchemaWin.setCookieName("modal-2");

        createUserDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserDerivedSchemaWin.setPageMapName("modal-3");
        createUserDerivedSchemaWin.setCookieName("modal-3");

        editUserDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserDerivedSchemaWin.setPageMapName("modal-4");
        editUserDerivedSchemaWin.setCookieName("modal-4");
        
        createRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleSchemaWin.setPageMapName("modal-5");
        createRoleSchemaWin.setCookieName("modal-5");
        
        editRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleSchemaWin.setPageMapName("modal-6");
        editRoleSchemaWin.setCookieName("modal-6");
        
        createRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleDerivedSchemaWin.setPageMapName("modal-7");
        createRoleDerivedSchemaWin.setCookieName("modal-7");
        
        editRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleDerivedSchemaWin.setPageMapName("modal-8");
        editRoleDerivedSchemaWin.setCookieName("modal-8");

        createMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipSchemaWin.setPageMapName("modal-9");
        createMembershipSchemaWin.setCookieName("modal-9");

        createMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipSchemaWin.setPageMapName("modal-10");
        createMembershipSchemaWin.setCookieName("modal-10");

        createMembershipDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipDerivedSchemaWin.setPageMapName("modal-11");
        createMembershipDerivedSchemaWin.setCookieName("modal-11");

        editMembershipDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editMembershipDerivedSchemaWin.setPageMapName("modal-12");
        editMembershipDerivedSchemaWin.setCookieName("modal-12");

        setWindowClosedCallback(createUserSchemaWin, userSchemaContainer);
        setWindowClosedCallback(editUserSchemaWin, userSchemaContainer);

        setWindowClosedCallback(createUserDerivedSchemaWin, userDerivedSchemaContainer);
        setWindowClosedCallback(editUserDerivedSchemaWin, userDerivedSchemaContainer);

        setWindowClosedCallback(createRoleSchemaWin, roleSchemasContainer);
        setWindowClosedCallback(editRoleSchemaWin, roleSchemasContainer);

        setWindowClosedCallback(createRoleDerivedSchemaWin, roleDerivedSchemasContainer);
        setWindowClosedCallback(editRoleDerivedSchemaWin, roleDerivedSchemasContainer);

        setWindowClosedCallback(createMembershipSchemaWin, membershipSchemaContainer);
        setWindowClosedCallback(editMembershipSchemaWin, membershipSchemaContainer);

        setWindowClosedCallback(createMembershipDerivedSchemaWin, membershipDerivedSchemaContainer);
        setWindowClosedCallback(editMembershipDerivedSchemaWin, membershipDerivedSchemaContainer);

        add(new AjaxLink("createRoleSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createRoleSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        SchemaModalPage form = new SchemaModalPage(Schema.this,
                                new ModalWindow("createRoleSchemaWin"), null, true);
                        form.setEntity(SchemaModalPage.Entity.ROLE);
                        return form;
                    }
                });

                createRoleSchemaWin.show(target);
            }
        });

        add(new AjaxLink("createRoleDerivedSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createRoleDerivedSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        DerivedSchemaModalPage form = new DerivedSchemaModalPage(Schema.this,
                                new ModalWindow("createRoleDerivedSchemaWin"), null, true);
                        form.setEntity(DerivedSchemaModalPage.Entity.ROLE);
                        return form;
                    }
                });

                createRoleDerivedSchemaWin.show(target);
            }
        });

        add(new AjaxLink("createUserSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createUserSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        SchemaModalPage form = new SchemaModalPage(Schema.this,
                                new ModalWindow("createUserSchemaWin"), null, true);
                        form.setEntity(SchemaModalPage.Entity.USER);
                        return form;
                    }
                });
                
                createUserSchemaWin.show(target);
            }
        });

        add(new AjaxLink("createUserDerSchemaWinLink") {
            
            @Override
            public void onClick(AjaxRequestTarget target) {

            createUserDerivedSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

            public Page createPage() {
                DerivedSchemaModalPage form = new DerivedSchemaModalPage(Schema.this,
                        new ModalWindow("createUserDerSchemaModalWin"), null, true);
                form.setEntity(DerivedSchemaModalPage.Entity.USER);
                
                return form;
            }
            });

            createUserDerivedSchemaWin.show(target);
            }
        });

        add(new AjaxLink("createMembershipSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createMembershipSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                    public Page createPage() {
                        SchemaModalPage form = new SchemaModalPage(Schema.this,
                                new ModalWindow("createMembershipSchemaModalWin"), null, true);
                        form.setEntity(SchemaModalPage.Entity.MEMBERSHIP);
                        return form;
                    }
                });

                createMembershipSchemaWin.show(target);
            }
        });

            add(new AjaxLink("createMembershipDerSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

            createMembershipDerivedSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

            public Page createPage() {
                DerivedSchemaModalPage form = new DerivedSchemaModalPage(Schema.this,
                        new ModalWindow("createMembershipDerivedSchemaWin"), null, true);
                form.setEntity(DerivedSchemaModalPage.Entity.MEMBERSHIP);

                return form;
            }
            });

            createMembershipDerivedSchemaWin.show(target);
            }
        });

    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window,final WebMarkupContainer container) {

        window.setWindowClosedCallback(
            new ModalWindow.WindowClosedCallback() {
                public void onClose(AjaxRequestTarget target) {
                    target.addComponent(container);
                    if(operationResult){
                        info(getString("operation_succeded"));  
                        target.addComponent(feedbackPanel);
                        operationResult = false;
                    }
                }
            });
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }
}