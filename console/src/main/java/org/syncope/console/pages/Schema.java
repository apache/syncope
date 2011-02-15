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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.DeleteLinkPanel;
import org.syncope.console.wicket.markup.html.form.EditLinkPanel;

/**
 * Schema WebPage.
 */
public class Schema extends BasePage {

    private enum SchemaType {

        RoleSchema,
        UserSchema,
        MembershipSchema

    };

    private enum SchemaDerivedType {

        RoleDerivedSchema,
        UserDerivedSchema,
        MembershipDerivedSchema

    };
    private static final int WIN_WIDTH = 400;

    private static final int WIN_HEIGHT = 200;

    @SpringBean
    private SchemaRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createUserSchemaWin;

    private final ModalWindow editUserSchemaWin;

    private final ModalWindow createUserDerivedSchemaWin;

    private final ModalWindow editUserDerivedSchemaWin;

    private final ModalWindow createRoleSchemaWin;

    private final ModalWindow editRoleSchemaWin;

    private final ModalWindow createRoleDerivedSchemaWin;

    private final ModalWindow editRoleDerivedSchemaWin;

    private final ModalWindow createMembershipSchemaWin;

    private final ModalWindow editMembershipSchemaWin;

    private final ModalWindow createMembershipDerivedSchemaWin;

    private final ModalWindow editMembershipDerivedSchemaWin;

    private WebMarkupContainer userSchemaContainer;

    private WebMarkupContainer userDerivedSchemaContainer;

    private WebMarkupContainer roleSchemasContainer;

    private WebMarkupContainer roleDerivedSchemasContainer;

    private WebMarkupContainer membershipSchemaContainer;

    private WebMarkupContainer membershipDerivedSchemaContainer;

    private int userSchemaPageRows;

    private int userDerSchemaPageRows;

    private int rolePageRows;

    private int roleDerPageRows;

    private int membershipPageRows;

    private int membershipDerPageRows;

    /*
    Response flag set by the Modal Window after the operation is completed
     */
    private boolean operationResult = false;

    public Schema(PageParameters parameters) {
        super(parameters);

        add(createRoleSchemaWin = new ModalWindow("createRoleSchemaWin"));
        add(editRoleSchemaWin = new ModalWindow("editRoleSchemaWin"));

        add(createRoleDerivedSchemaWin = new ModalWindow(
                "createRoleDerivedSchemaWin"));

        add(editRoleDerivedSchemaWin = new ModalWindow(
                "editRoleDerivedSchemaWin"));

        add(createUserSchemaWin = new ModalWindow("createUserSchemaWin"));

        add(editUserSchemaWin = new ModalWindow("editUserSchemaWin"));

        add(createUserDerivedSchemaWin = new ModalWindow(
                "createUserDerSchemaWin"));
        add(editUserDerivedSchemaWin = new ModalWindow(
                "editUserDerSchemaWin"));

        add(createMembershipSchemaWin = new ModalWindow(
                "createMembershipSchemaWin"));
        add(editMembershipSchemaWin = new ModalWindow(
                "editMembershipSchemaWin"));

        add(createMembershipDerivedSchemaWin = new ModalWindow(
                "createMembershipDerSchemaWin"));
        add(editMembershipDerivedSchemaWin = new ModalWindow(
                "editMembershipDerSchemaWin"));

        rolePageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_ROLE_SCHEMA_PAGINATOR_ROWS);

        roleDerPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS);

        userSchemaPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_USER_SCHEMA_PAGINATOR_ROWS);

        userDerSchemaPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_USER_DER_SCHEMA_PAGINATOR_ROWS);

        membershipPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS);

        membershipDerPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS);

        final String allowedCreateRoles = xmlRolesReader.getAllAllowedRoles(
                "Schema", "create");

        final String allowedReadRoles = xmlRolesReader.getAllAllowedRoles(
                "Schema", "read");

        final String allowedDeleteRoles = xmlRolesReader.getAllAllowedRoles(
                "Schema", "delete");

        List<IColumn> rolesColumns = new ArrayList<IColumn>();

        rolesColumns.add(new PropertyColumn(new Model(getString("name")),
                "name", "name"));

        rolesColumns.add(new PropertyColumn(new Model(getString("type")),
                "type", "type"));

        rolesColumns.add(new PropertyColumn(new Model(getString("attributes")),
                "attributes", "attributes"));

        rolesColumns.add(new AbstractColumn<SchemaTO>(new Model<String>(
                getString("name"))) {

            public void populateItem(
                    Item<ICellPopulator<SchemaTO>> cellItem,
                    String componentId, IModel<SchemaTO> model) {

                final SchemaTO schemaTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editRoleSchemaWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        SchemaModalPage window =
                                                new SchemaModalPage(
                                                Schema.this, editRoleSchemaWin,
                                                schemaTO, false);
                                        window.setEntity(
                                                SchemaModalPage.Entity.ROLE);
                                        return window;
                                    }
                                });

                        editRoleSchemaWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);
                panel.add(editLink);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedReadRoles);

                cellItem.add(panel);
            }
        });

        rolesColumns.add(new AbstractColumn<SchemaTO>(
                new Model<String>(getString("delete"))) {

            public void populateItem(Item<ICellPopulator<SchemaTO>> cellItem,
                    String componentId, IModel<SchemaTO> model) {

                final SchemaTO schemaTO = model.getObject();

                AjaxLink deleteLink = new IndicatingAjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteRoleSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(roleSchemasContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {

                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedDeleteRoles);

                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable tableRoles =
                new AjaxFallbackDefaultDataTable("datatable", rolesColumns,
                new SchemaProvider(SchemaType.RoleSchema), rolePageRows);

        Form rolesPaginatorForm = new Form("RolesPaginatorForm");

        final DropDownChoice rowsRoleChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "rolePageRows"),
                prefMan.getPaginatorChoices());

        rowsRoleChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_ROLE_SCHEMA_PAGINATOR_ROWS,
                        String.valueOf(rolePageRows));
                tableRoles.setRowsPerPage(rolePageRows);

                target.addComponent(roleSchemasContainer);
            }
        });

        rolesPaginatorForm.add(rowsRoleChooser);
        add(rolesPaginatorForm);

        List<IColumn> columnsRolesDer = new ArrayList<IColumn>();

        columnsRolesDer.add(new PropertyColumn(new Model(getString("name")),
                "name", "name"));

        columnsRolesDer.add(new PropertyColumn(
                new Model(getString("expression")),
                "expression", "expression"));

        columnsRolesDer.add(new PropertyColumn(
                new Model(getString("attributes")),
                "derivedAttributes", "derivedAttributes"));

        columnsRolesDer.add(new AbstractColumn<DerivedSchemaTO>(
                new Model<String>(getString("edit"))) {

            public void populateItem(
                    Item<ICellPopulator<DerivedSchemaTO>> cellItem,
                    String componentId, IModel<DerivedSchemaTO> model) {

                final DerivedSchemaTO schemaTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editRoleDerivedSchemaWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        DerivedSchemaModalPage form =
                                                new DerivedSchemaModalPage(
                                                Schema.this,
                                                editRoleDerivedSchemaWin,
                                                schemaTO, false);
                                        form.setEntity(
                                                DerivedSchemaModalPage.Entity.ROLE);
                                        return form;
                                    }
                                });

                        editRoleDerivedSchemaWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedReadRoles);

                panel.add(editLink);

                cellItem.add(panel);
            }
        });

        columnsRolesDer.add(new AbstractColumn<DerivedSchemaTO>(
                new Model<String>(getString("delete"))) {

            public void populateItem(
                    Item<ICellPopulator<DerivedSchemaTO>> cellItem,
                    String componentId, IModel<DerivedSchemaTO> model) {

                final DerivedSchemaTO schemaTO = model.getObject();

                AjaxLink deleteLink = new IndicatingAjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteRoleDerivedSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(roleDerivedSchemasContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {

                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedDeleteRoles);

                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable tableRolesDer =
                new AjaxFallbackDefaultDataTable("datatable", columnsRolesDer,
                new DerivedSchemaProvider(SchemaDerivedType.RoleDerivedSchema),
                roleDerPageRows);

        Form rolesDerPaginatorForm = new Form("RolesDerPaginatorForm");

        DropDownChoice rowsRolesDerChooser = new DropDownChoice("rowsChooser",
                new PropertyModel(this, "roleDerPageRows"),
                prefMan.getPaginatorChoices());

        rowsRolesDerChooser.add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        prefMan.set(getWebRequestCycle().getWebRequest(),
                                getWebRequestCycle().getWebResponse(),
                                Constants.PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS,
                                String.valueOf(roleDerPageRows));
                        tableRolesDer.setRowsPerPage(roleDerPageRows);

                        target.addComponent(roleDerivedSchemasContainer);
                    }
                });

        rolesDerPaginatorForm.add(rowsRolesDerChooser);
        add(rolesDerPaginatorForm);

        List<IColumn> userColumns = new ArrayList<IColumn>();

        userColumns.add(new PropertyColumn(new Model(getString("name")),
                "name", "name"));

        userColumns.add(new PropertyColumn(new Model(getString("type")),
                "type", "type"));

        userColumns.add(new PropertyColumn(new Model(getString("attributes")),
                "attributes", "attributes"));

        userColumns.add(new AbstractColumn<SchemaTO>(new Model<String>(
                getString("edit"))) {

            public void populateItem(
                    Item<ICellPopulator<SchemaTO>> cellItem,
                    String componentId, IModel<SchemaTO> model) {

                final SchemaTO schemaTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editUserSchemaWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        SchemaModalPage form =
                                                new SchemaModalPage(
                                                Schema.this, editUserSchemaWin,
                                                schemaTO, false);
                                        form.setEntity(
                                                SchemaModalPage.Entity.USER);
                                        return form;
                                    }
                                });

                        editUserSchemaWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedReadRoles);

                panel.add(editLink);

                cellItem.add(panel);
            }
        });

        userColumns.add(new AbstractColumn<SchemaTO>(new Model<String>(getString(
                "delete"))) {

            public void populateItem(
                    Item<ICellPopulator<SchemaTO>> cellItem,
                    String componentId, IModel<SchemaTO> model) {

                final SchemaTO schemaTO = model.getObject();

                AjaxLink deleteLink = new IndicatingAjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteUserSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(userSchemaContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {

                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };


                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedDeleteRoles);

                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable tableUsers =
                new AjaxFallbackDefaultDataTable("datatable", userColumns,
                new SchemaProvider(SchemaType.UserSchema), userSchemaPageRows);

        tableUsers.setMarkupId("tableUsers");

        Form usersPaginatorForm = new Form("UsersPaginatorForm");

        final DropDownChoice usersRowsChooser = new DropDownChoice(
                "rowsChooser", new PropertyModel(this, "userSchemaPageRows"),
                prefMan.getPaginatorChoices());

        usersRowsChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_USER_SCHEMA_PAGINATOR_ROWS,
                        String.valueOf(userSchemaPageRows));
                tableUsers.setRowsPerPage(userSchemaPageRows);

                target.addComponent(userSchemaContainer);
            }
        });

        usersPaginatorForm.add(usersRowsChooser);
        add(usersPaginatorForm);

        List<IColumn> columnsUsersDer = new ArrayList<IColumn>();

        columnsUsersDer.add(new PropertyColumn(new Model(getString("name")),
                "name", "name"));

        columnsUsersDer.add(new PropertyColumn(
                new Model(getString("expression")),
                "expression", "expression"));

        columnsUsersDer.add(new PropertyColumn(
                new Model(getString("attributes")),
                "derivedAttributes", "derivedAttributes"));

        columnsUsersDer.add(new AbstractColumn<DerivedSchemaTO>(
                new Model<String>(getString("edit"))) {

            public void populateItem(
                    Item<ICellPopulator<DerivedSchemaTO>> cellItem,
                    String componentId, IModel<DerivedSchemaTO> model) {

                final DerivedSchemaTO schemaTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editUserDerivedSchemaWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        DerivedSchemaModalPage window =
                                                new DerivedSchemaModalPage(
                                                Schema.this,
                                                editUserDerivedSchemaWin,
                                                schemaTO, false);
                                        window.setEntity(
                                                DerivedSchemaModalPage.Entity.USER);
                                        return window;
                                    }
                                });

                        editUserDerivedSchemaWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedReadRoles);

                panel.add(editLink);

                cellItem.add(panel);
            }
        });

        columnsUsersDer.add(new AbstractColumn<DerivedSchemaTO>(
                new Model<String>(getString("delete"))) {

            public void populateItem(
                    Item<ICellPopulator<DerivedSchemaTO>> cellItem,
                    String componentId, IModel<DerivedSchemaTO> model) {

                final DerivedSchemaTO schemaTO = model.getObject();

                AjaxLink deleteLink = new IndicatingAjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteUserDerivedSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(userDerivedSchemaContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {

                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };


                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedDeleteRoles);

                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable tableUsersDer =
                new AjaxFallbackDefaultDataTable("datatable", columnsUsersDer,
                new DerivedSchemaProvider(SchemaDerivedType.UserDerivedSchema),
                userDerSchemaPageRows);

        Form usersDerPaginatorForm = new Form("UsersDerPaginatorForm");

        final DropDownChoice usersDerRowsChooser = new DropDownChoice(
                "rowsChooser",
                new PropertyModel(this, "userDerSchemaPageRows"),
                prefMan.getPaginatorChoices());

        usersDerRowsChooser.add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        prefMan.set(getWebRequestCycle().getWebRequest(),
                                getWebRequestCycle().getWebResponse(),
                                Constants.PREF_USER_DER_SCHEMA_PAGINATOR_ROWS,
                                String.valueOf(userDerSchemaPageRows));
                        tableUsersDer.setRowsPerPage(userDerSchemaPageRows);

                        target.addComponent(userDerivedSchemaContainer);
                    }
                });

        usersDerPaginatorForm.add(usersDerRowsChooser);
        add(usersDerPaginatorForm);

        List<IColumn> membershipsColumns = new ArrayList<IColumn>();

        membershipsColumns.add(new PropertyColumn(new Model(getString("name")),
                "name", "name"));

        membershipsColumns.add(new PropertyColumn(new Model(getString("type")),
                "type", "type"));

        membershipsColumns.add(new PropertyColumn(new Model(getString(
                "attributes")),
                "attributes", "attributes"));

        membershipsColumns.add(new AbstractColumn<SchemaTO>(new Model<String>(
                getString("name"))) {

            public void populateItem(
                    Item<ICellPopulator<SchemaTO>> cellItem,
                    String componentId, IModel<SchemaTO> model) {

                final SchemaTO schemaTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editMembershipSchemaWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        SchemaModalPage form =
                                                new SchemaModalPage(
                                                Schema.this,
                                                editMembershipSchemaWin,
                                                schemaTO, false);
                                        form.setEntity(
                                                SchemaModalPage.Entity.MEMBERSHIP);
                                        return form;
                                    }
                                });

                        editMembershipSchemaWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedReadRoles);

                panel.add(editLink);

                cellItem.add(panel);
            }
        });

        membershipsColumns.add(new AbstractColumn<SchemaTO>(
                new Model<String>(getString("delete"))) {

            public void populateItem(
                    Item<ICellPopulator<SchemaTO>> cellItem,
                    String componentId, IModel<SchemaTO> model) {

                final SchemaTO schemaTO = model.getObject();

                AjaxLink deleteLink = new IndicatingAjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteMemberhipSchema(schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(membershipSchemaContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {
                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedDeleteRoles);

                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });


        final AjaxFallbackDefaultDataTable tableMemberships =
                new AjaxFallbackDefaultDataTable(
                "datatable", membershipsColumns,
                new SchemaProvider(SchemaType.MembershipSchema),
                membershipPageRows);

        Form membershipPaginatorForm = new Form("MembershipPaginatorForm");

        final DropDownChoice membershipRowsChooser = new DropDownChoice(
                "rowsChooser",
                new PropertyModel(this, "membershipPageRows"),
                prefMan.getPaginatorChoices());

        membershipRowsChooser.add(new AjaxFormComponentUpdatingBehavior(
                "onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS,
                        String.valueOf(membershipPageRows));
                tableMemberships.setRowsPerPage(membershipPageRows);

                target.addComponent(membershipSchemaContainer);
            }
        });

        membershipPaginatorForm.add(membershipRowsChooser);
        add(membershipPaginatorForm);

        List<IColumn> columnsMembershipsDer = new ArrayList<IColumn>();

        columnsMembershipsDer.add(new PropertyColumn(
                new Model(getString("name")),
                "name", "name"));

        columnsMembershipsDer.add(new PropertyColumn(new Model(getString(
                "expression")),
                "expression", "expression"));

        columnsMembershipsDer.add(new PropertyColumn(new Model(getString(
                "attributes")),
                "derivedAttributes", "derivedAttributes"));

        columnsMembershipsDer.add(new AbstractColumn<DerivedSchemaTO>(
                new Model<String>(getString("edit"))) {

            public void populateItem(
                    Item<ICellPopulator<DerivedSchemaTO>> cellItem,
                    String componentId, IModel<DerivedSchemaTO> model) {

                final DerivedSchemaTO schemaTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        editMembershipDerivedSchemaWin.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    public Page createPage() {
                                        DerivedSchemaModalPage window =
                                                new DerivedSchemaModalPage(
                                                Schema.this,
                                                editMembershipDerivedSchemaWin,
                                                schemaTO, false);
                                        window.setEntity(
                                                DerivedSchemaModalPage.Entity.MEMBERSHIP);
                                        return window;
                                    }
                                });

                        editMembershipDerivedSchemaWin.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedReadRoles);

                panel.add(editLink);

                cellItem.add(panel);
            }
        });

        columnsMembershipsDer.add(new AbstractColumn<DerivedSchemaTO>(
                new Model<String>(getString("delete"))) {

            public void populateItem(
                    Item<ICellPopulator<DerivedSchemaTO>> cellItem,
                    String componentId, IModel<DerivedSchemaTO> model) {

                final DerivedSchemaTO schemaTO = model.getObject();

                AjaxLink deleteLink = new IndicatingAjaxLink("deleteLink") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        restClient.deleteMembershipDerivedSchema(
                                schemaTO.getName());

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(membershipDerivedSchemaContainer);
                    }

                    @Override
                    protected IAjaxCallDecorator getAjaxCallDecorator() {
                        return new AjaxPreprocessingCallDecorator(super.
                                getAjaxCallDecorator()) {

                            @Override
                            public CharSequence preDecorateScript(
                                    CharSequence script) {
                                return "if (confirm('" + getString(
                                        "confirmDelete") + "'))"
                                        + "{" + script + "}";
                            }
                        };
                    }
                };


                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(panel, ENABLE,
                        allowedDeleteRoles);

                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });

        final AjaxFallbackDefaultDataTable tableMembershipsDer =
                new AjaxFallbackDefaultDataTable("datatable",
                columnsMembershipsDer,
                new DerivedSchemaProvider(
                SchemaDerivedType.MembershipDerivedSchema),
                membershipDerPageRows);

        Form membershipDerPaginatorForm = new Form("MembershipDerPaginatorForm");

        final DropDownChoice membershipDerRowsChooser = new DropDownChoice(
                "rowsChooser",
                new PropertyModel(this, "membershipDerPageRows"),
                prefMan.getPaginatorChoices());

        membershipDerRowsChooser.add(new AjaxFormComponentUpdatingBehavior(
                "onchange") {

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getWebRequestCycle().getWebRequest(),
                        getWebRequestCycle().getWebResponse(),
                        Constants.PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS,
                        String.valueOf(membershipDerPageRows));

                tableMembershipsDer.setRowsPerPage(membershipDerPageRows);

                target.addComponent(membershipDerivedSchemaContainer);
            }
        });

        membershipDerPaginatorForm.add(membershipDerRowsChooser);
        add(membershipDerPaginatorForm);

        roleSchemasContainer = new WebMarkupContainer("roleSchemasContainer");
        roleSchemasContainer.add(tableRoles);
        roleSchemasContainer.setOutputMarkupId(true);

        roleDerivedSchemasContainer = new WebMarkupContainer(
                "roleDerivedSchemasContainer");
        roleDerivedSchemasContainer.add(tableRolesDer);
        roleDerivedSchemasContainer.setOutputMarkupId(true);

        userSchemaContainer = new WebMarkupContainer("userSchemaContainer");
        userSchemaContainer.add(tableUsers);
        userSchemaContainer.setOutputMarkupId(true);

        userDerivedSchemaContainer = new WebMarkupContainer(
                "userDerivedSchemaContainer");
        userDerivedSchemaContainer.add(tableUsersDer);
        userDerivedSchemaContainer.setOutputMarkupId(true);

        membershipSchemaContainer = new WebMarkupContainer(
                "membershipSchemaContainer");
        membershipSchemaContainer.add(tableMemberships);
        membershipSchemaContainer.setOutputMarkupId(true);

        membershipDerivedSchemaContainer = new WebMarkupContainer(
                "membershipDerivedSchemaContainer");
        membershipDerivedSchemaContainer.add(tableMembershipsDer);
        membershipDerivedSchemaContainer.setOutputMarkupId(true);

        add(roleSchemasContainer);
        add(roleDerivedSchemasContainer);

        add(userSchemaContainer);
        add(userDerivedSchemaContainer);

        add(membershipSchemaContainer);
        add(membershipDerivedSchemaContainer);

        createUserSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserSchemaWin.setInitialWidth(WIN_WIDTH);
        createUserSchemaWin.setPageMapName("modal-1");
        createUserSchemaWin.setCookieName("modal-1");
        createUserSchemaWin.setMarkupId("createUserSchemaWin");

        editUserSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserSchemaWin.setInitialWidth(WIN_WIDTH);
        editUserSchemaWin.setPageMapName("modal-2");
        editUserSchemaWin.setCookieName("modal-2");
        editUserSchemaWin.setMarkupId("editUserSchemaWin");

        createUserDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createUserDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createUserDerivedSchemaWin.setPageMapName("modal-3");
        createUserDerivedSchemaWin.setCookieName("modal-3");

        editUserDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        editUserDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        editUserDerivedSchemaWin.setPageMapName("modal-4");
        editUserDerivedSchemaWin.setCookieName("modal-4");

        createRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleSchemaWin.setPageMapName("modal-5");
        createRoleSchemaWin.setCookieName("modal-5");

        editRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleSchemaWin.setPageMapName("modal-6");
        editRoleSchemaWin.setCookieName("modal-6");

        createRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createRoleDerivedSchemaWin.setPageMapName("modal-7");
        createRoleDerivedSchemaWin.setCookieName("modal-7");

        editRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        editRoleDerivedSchemaWin.setPageMapName("modal-8");
        editRoleDerivedSchemaWin.setCookieName("modal-8");

        createMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipSchemaWin.setPageMapName("modal-9");
        createMembershipSchemaWin.setCookieName("modal-9");

        editMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipSchemaWin.setPageMapName("modal-10");
        editMembershipSchemaWin.setCookieName("modal-10");

        createMembershipDerivedSchemaWin.setCssClassName(
                ModalWindow.CSS_CLASS_GRAY);
        createMembershipDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createMembershipDerivedSchemaWin.setPageMapName("modal-11");
        createMembershipDerivedSchemaWin.setCookieName("modal-11");

        editMembershipDerivedSchemaWin.setCssClassName(
                ModalWindow.CSS_CLASS_GRAY);
        editMembershipDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        editMembershipDerivedSchemaWin.setPageMapName("modal-12");
        editMembershipDerivedSchemaWin.setCookieName("modal-12");

        setWindowClosedCallback(createUserSchemaWin, userSchemaContainer);
        setWindowClosedCallback(editUserSchemaWin, userSchemaContainer);

        setWindowClosedCallback(createUserDerivedSchemaWin,
                userDerivedSchemaContainer);
        setWindowClosedCallback(editUserDerivedSchemaWin,
                userDerivedSchemaContainer);

        setWindowClosedCallback(createRoleSchemaWin, roleSchemasContainer);
        setWindowClosedCallback(editRoleSchemaWin, roleSchemasContainer);

        setWindowClosedCallback(createRoleDerivedSchemaWin,
                roleDerivedSchemasContainer);
        setWindowClosedCallback(editRoleDerivedSchemaWin,
                roleDerivedSchemasContainer);

        setWindowClosedCallback(createMembershipSchemaWin,
                membershipSchemaContainer);
        setWindowClosedCallback(editMembershipSchemaWin,
                membershipSchemaContainer);

        setWindowClosedCallback(createMembershipDerivedSchemaWin,
                membershipDerivedSchemaContainer);
        setWindowClosedCallback(editMembershipDerivedSchemaWin,
                membershipDerivedSchemaContainer);

        AjaxLink createRoleSchemaWinLink = new IndicatingAjaxLink(
                "createRoleSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createRoleSchemaWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                SchemaModalPage form = new SchemaModalPage(
                                        Schema.this,
                                        new ModalWindow("createRoleSchemaWin"),
                                        null,
                                        true);
                                form.setEntity(SchemaModalPage.Entity.ROLE);
                                return form;
                            }
                        });

                createRoleSchemaWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createRoleSchemaWinLink,
                ENABLE,
                allowedCreateRoles);

        add(createRoleSchemaWinLink);


        AjaxLink createRoleDerivedSchemaWinLink = new IndicatingAjaxLink(
                "createRoleDerivedSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createRoleDerivedSchemaWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                DerivedSchemaModalPage form =
                                        new DerivedSchemaModalPage(
                                        Schema.this,
                                        new ModalWindow(
                                        "createRoleDerivedSchemaWin"),
                                        null, true);
                                form.setEntity(
                                        DerivedSchemaModalPage.Entity.ROLE);
                                return form;
                            }
                        });

                createRoleDerivedSchemaWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                createRoleDerivedSchemaWinLink, ENABLE,
                allowedCreateRoles);

        add(createRoleDerivedSchemaWinLink);

        AjaxLink createUserSchemaWinLink = new IndicatingAjaxLink(
                "createUserSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createUserSchemaWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                SchemaModalPage form = new SchemaModalPage(
                                        Schema.this,
                                        new ModalWindow("createUserSchemaWin"),
                                        null,
                                        true);
                                form.setEntity(SchemaModalPage.Entity.USER);
                                return form;
                            }
                        });

                createUserSchemaWin.show(target);
            }
        };

        add(createUserSchemaWinLink);

        MetaDataRoleAuthorizationStrategy.authorize(createUserSchemaWinLink,
                ENABLE,
                allowedCreateRoles);

        AjaxLink createUserDerSchemaWinLink = new IndicatingAjaxLink(
                "createUserDerSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createUserDerivedSchemaWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                DerivedSchemaModalPage form =
                                        new DerivedSchemaModalPage(
                                        Schema.this,
                                        new ModalWindow(
                                        "createUserDerSchemaModalWin"),
                                        null, true);
                                form.setEntity(
                                        DerivedSchemaModalPage.Entity.USER);

                                return form;
                            }
                        });

                createUserDerivedSchemaWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createUserDerSchemaWinLink,
                ENABLE, allowedCreateRoles);

        add(createUserDerSchemaWinLink);


        AjaxLink createMembershipSchemaWinLink = new IndicatingAjaxLink(
                "createMembershipSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createMembershipSchemaWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                SchemaModalPage form = new SchemaModalPage(
                                        Schema.this,
                                        new ModalWindow(
                                        "createMembershipSchemaModalWin"),
                                        null, true);
                                form.setEntity(
                                        SchemaModalPage.Entity.MEMBERSHIP);
                                return form;
                            }
                        });

                createMembershipSchemaWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                createMembershipSchemaWinLink,
                ENABLE, allowedCreateRoles);

        add(createMembershipSchemaWinLink);

        AjaxLink createMembershipDerSchemaWinLink = new IndicatingAjaxLink(
                "createMembershipDerSchemaWinLink") {

            @Override
            public void onClick(AjaxRequestTarget target) {

                createMembershipDerivedSchemaWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                DerivedSchemaModalPage form =
                                        new DerivedSchemaModalPage(
                                        Schema.this,
                                        new ModalWindow(
                                        "createMembershipDerivedSchemaWin"),
                                        null, true);
                                form.setEntity(
                                        DerivedSchemaModalPage.Entity.MEMBERSHIP);

                                return form;
                            }
                        });

                createMembershipDerivedSchemaWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                createMembershipDerSchemaWinLink,
                ENABLE, allowedCreateRoles);

        add(createMembershipDerSchemaWinLink);
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                        if (operationResult) {
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

    class SchemaProvider extends SortableDataProvider<SchemaTO> {

        private SortableDataProviderComparator comparator =
                new SortableDataProviderComparator();

        private SchemaType schema;

        public SchemaProvider(SchemaType schema) {

            this.schema = schema;

            //Default sorting
            setSort("name", true);
        }

        @Override
        public Iterator<SchemaTO> iterator(int first, int count) {
            List<SchemaTO> list = getAttributesSchemaListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getAttributesSchemaListDB().size();
        }

        @Override
        public IModel<SchemaTO> model(final SchemaTO user) {
            return new AbstractReadOnlyModel<SchemaTO>() {

                @Override
                public SchemaTO getObject() {
                    return user;
                }
            };
        }

        public List<SchemaTO> getAttributesSchemaListDB() {

            List<SchemaTO> list = null;

            if (schema == SchemaType.RoleSchema) {
                list = restClient.getAllRoleSchemas();
            } else if (schema == SchemaType.UserSchema) {
                list = restClient.getAllUserSchemas();
            } else if (schema == SchemaType.MembershipSchema) {
                list = restClient.getAllMemberhipSchemas();
            }

            return list;
        }

        class SortableDataProviderComparator implements
                Comparator<SchemaTO>, Serializable {

            public int compare(final SchemaTO o1,
                    final SchemaTO o2) {
                PropertyModel<Comparable> model1 =
                        new PropertyModel<Comparable>(o1,
                        getSort().getProperty());
                PropertyModel<Comparable> model2 =
                        new PropertyModel<Comparable>(o2,
                        getSort().getProperty());

                int result = 1;

                if (model1.getObject() == null && model2.getObject() == null) {
                    result = 0;
                } else if (model1.getObject() == null) {
                    result = 1;
                } else if (model2.getObject() == null) {
                    result = -1;
                } else {
                    result = ((Comparable) model1.getObject()).compareTo(
                            model2.getObject());
                }

                result = getSort().isAscending() ? result : -result;

                return result;
            }
        }
    }

    class DerivedSchemaProvider extends SortableDataProvider<DerivedSchemaTO> {

        private SortableDataProviderComparator comparator =
                new SortableDataProviderComparator();

        private SchemaDerivedType schema;

        public DerivedSchemaProvider(SchemaDerivedType schema) {

            this.schema = schema;

            //Default sorting
            setSort("name", true);
        }

        @Override
        public Iterator<DerivedSchemaTO> iterator(int first, int count) {
            List<DerivedSchemaTO> list = getAttributesSchemaListDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getAttributesSchemaListDB().size();
        }

        @Override
        public IModel<DerivedSchemaTO> model(final DerivedSchemaTO schema) {
            return new AbstractReadOnlyModel<DerivedSchemaTO>() {

                @Override
                public DerivedSchemaTO getObject() {
                    return schema;
                }
            };
        }

        public List<DerivedSchemaTO> getAttributesSchemaListDB() {

            List<DerivedSchemaTO> list = null;

            if (schema == SchemaDerivedType.RoleDerivedSchema) {
                list = restClient.getAllRoleDerivedSchemas();
            } else if (schema == SchemaDerivedType.UserDerivedSchema) {
                list = restClient.getAllUserDerivedSchemas();
            } else if (schema == SchemaDerivedType.MembershipDerivedSchema) {
                list = restClient.getAllMembershipDerivedSchemas();
            }

            return list;
        }

        class SortableDataProviderComparator implements
                Comparator<DerivedSchemaTO>, Serializable {

            public int compare(final DerivedSchemaTO o1,
                    final DerivedSchemaTO o2) {
                PropertyModel<Comparable> model1 =
                        new PropertyModel<Comparable>(o1,
                        getSort().getProperty());
                PropertyModel<Comparable> model2 =
                        new PropertyModel<Comparable>(o2,
                        getSort().getProperty());

                int result = 1;

                if (model1.getObject() == null && model2.getObject() == null) {
                    result = 0;
                } else if (model1.getObject() == null) {
                    result = 1;
                } else if (model2.getObject() == null) {
                    result = -1;
                } else {
                    result = ((Comparable) model1.getObject()).compareTo(
                            model2.getObject());
                }

                result = getSort().isAscending() ? result : -result;

                return result;
            }
        }
    }
}
