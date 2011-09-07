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

import org.syncope.console.SchemaModalPageFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.AbstractBaseBean;
import org.syncope.client.to.DerivedSchemaTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.VirtualSchemaTO;
import org.syncope.console.commons.Constants;
import org.syncope.console.commons.PreferenceManager;
import org.syncope.console.commons.SelectChoiceRenderer;
import org.syncope.console.commons.SortableDataProviderComparator;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.ajax.markup.html.IndicatingDeleteOnConfirmAjaxLink;
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

    private enum SchemaVirtualType {

        RoleVirtualSchema,
        UserVirtualSchema,
        MembershipVirtualSchema
    };

    private static final int WIN_WIDTH = 550;

    private static final int WIN_HEIGHT = 200;

    public static String enumValuesSeparator = ";";

    @SpringBean
    private SchemaRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final ModalWindow createUserSchemaWin;

    private final ModalWindow editUserSchemaWin;

    private final ModalWindow createUserDerivedSchemaWin;

    private final ModalWindow editUserDerivedSchemaWin;

    private final ModalWindow createUserVirtualSchemaWin;

    private final ModalWindow editUserVirtualSchemaWin;

    private final ModalWindow createRoleSchemaWin;

    private final ModalWindow editRoleSchemaWin;

    private final ModalWindow createRoleDerivedSchemaWin;

    private final ModalWindow editRoleDerivedSchemaWin;

    private final ModalWindow createRoleVirtualSchemaWin;

    private final ModalWindow editRoleVirtualSchemaWin;

    private final ModalWindow createMembershipSchemaWin;

    private final ModalWindow editMembershipSchemaWin;

    private final ModalWindow createMembershipDerivedSchemaWin;

    private final ModalWindow editMembershipDerivedSchemaWin;

    private final ModalWindow createMembershipVirtualSchemaWin;

    private final ModalWindow editMembershipVirtualSchemaWin;

    private WebMarkupContainer userSchemasContainer;

    private WebMarkupContainer userDerivedSchemasContainer;

    private WebMarkupContainer userVirtualSchemasContainer;

    private WebMarkupContainer roleSchemasContainer;

    private WebMarkupContainer roleDerivedSchemasContainer;

    private WebMarkupContainer roleVirtualSchemasContainer;

    private WebMarkupContainer membershipSchemaContainer;

    private WebMarkupContainer membershipDerivedSchemaContainer;

    private WebMarkupContainer membershipVirtualSchemaContainer;

    private int userSchemaPageRows;

    private int userDerSchemaPageRows;

    private int userVirSchemaPageRows;

    private int rolePageRows;

    private int roleDerPageRows;

    private int roleVirPageRows;

    private int membershipPageRows;

    private int membershipDerPageRows;

    private int membershipVirPageRows;

    /*
    Response flag set by the Modal Window after the operation is completed
     */
    private boolean operationResult = false;

    public Schema(PageParameters parameters) {
        super(parameters);

        userSchemasContainer =
                new WebMarkupContainer("userSchemasContainer");

        userDerivedSchemasContainer =
                new WebMarkupContainer("userDerivedSchemasContainer");

        userVirtualSchemasContainer =
                new WebMarkupContainer("userVirtualSchemasContainer");

        roleSchemasContainer =
                new WebMarkupContainer("roleSchemasContainer");

        roleDerivedSchemasContainer =
                new WebMarkupContainer("roleDerivedSchemasContainer");

        roleVirtualSchemasContainer =
                new WebMarkupContainer("roleVirtualSchemasContainer");

        membershipSchemaContainer =
                new WebMarkupContainer("membershipSchemaContainer");

        membershipDerivedSchemaContainer =
                new WebMarkupContainer("membershipDerivedSchemaContainer");

        membershipVirtualSchemaContainer =
                new WebMarkupContainer("membershipVirtualSchemaContainer");

        add(createRoleSchemaWin = new ModalWindow("createRoleSchemaWin"));
        add(editRoleSchemaWin = new ModalWindow("editRoleSchemaWin"));

        add(createRoleDerivedSchemaWin = new ModalWindow(
                "createRoleDerivedSchemaWin"));

        add(createRoleVirtualSchemaWin = new ModalWindow(
                "createRoleVirtualSchemaWin"));

        add(editRoleDerivedSchemaWin = new ModalWindow(
                "editRoleDerivedSchemaWin"));

        add(editRoleVirtualSchemaWin = new ModalWindow(
                "editRoleVirtualSchemaWin"));

        add(createUserSchemaWin = new ModalWindow("createUserSchemaWin"));

        add(editUserSchemaWin = new ModalWindow("editUserSchemaWin"));

        add(createUserDerivedSchemaWin = new ModalWindow(
                "createUserDerSchemaWin"));
        add(createUserVirtualSchemaWin = new ModalWindow(
                "createUserVirSchemaWin"));

        add(editUserDerivedSchemaWin = new ModalWindow(
                "editUserDerSchemaWin"));
        add(editUserVirtualSchemaWin = new ModalWindow(
                "editUserVirSchemaWin"));

        add(createMembershipSchemaWin = new ModalWindow(
                "createMembershipSchemaWin"));
        add(editMembershipSchemaWin = new ModalWindow(
                "editMembershipSchemaWin"));

        add(createMembershipDerivedSchemaWin = new ModalWindow(
                "createMembershipDerSchemaWin"));
        add(createMembershipVirtualSchemaWin = new ModalWindow(
                "createMembershipVirSchemaWin"));

        add(editMembershipDerivedSchemaWin = new ModalWindow(
                "editMembershipDerSchemaWin"));
        add(editMembershipVirtualSchemaWin = new ModalWindow(
                "editMembershipVirSchemaWin"));

        rolePageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_ROLE_SCHEMA_PAGINATOR_ROWS);

        roleDerPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS);

        roleVirPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_ROLE_VIR_SCHEMA_PAGINATOR_ROWS);

        userSchemaPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_USER_SCHEMA_PAGINATOR_ROWS);

        userDerSchemaPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_USER_DER_SCHEMA_PAGINATOR_ROWS);

        userVirSchemaPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS);

        membershipPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS);

        membershipDerPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS);

        membershipVirPageRows = prefMan.getPaginatorRows(
                getWebRequestCycle().getWebRequest(),
                Constants.PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS);

        final String allowedCreateRoles = xmlRolesReader.getAllAllowedRoles(
                "Schema", "create");

        final String allowedReadRoles = xmlRolesReader.getAllAllowedRoles(
                "Schema", "read");

        final String allowedDeleteRoles = xmlRolesReader.getAllAllowedRoles(
                "Schema", "delete");

        List<IColumn> rolesColumns = getColumnsForSchema(
                roleSchemasContainer,
                editRoleSchemaWin,
                SchemaModalPageFactory.Entity.role,
                SchemaModalPageFactory.SchemaType.NORMAL,
                Constants.SCHEMA_FIELDS,
                allowedReadRoles,
                allowedCreateRoles);

        final AjaxFallbackDefaultDataTable tableRoles =
                new AjaxFallbackDefaultDataTable("datatable", rolesColumns,
                new SchemaProvider(SchemaType.RoleSchema), rolePageRows);

        add(getPaginatorForm(
                roleSchemasContainer,
                tableRoles,
                "RolesPaginatorForm",
                "rolePageRows",
                Constants.PREF_ROLE_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsRolesDer = getColumnsForSchema(
                roleDerivedSchemasContainer,
                editRoleDerivedSchemaWin,
                SchemaModalPageFactory.Entity.role,
                SchemaModalPageFactory.SchemaType.DERIVED,
                Constants.DERIVED_SCHEMA_FIELDS,
                allowedReadRoles,
                allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableRolesDer =
                new AjaxFallbackDefaultDataTable("datatable", columnsRolesDer,
                new DerivedSchemaProvider(SchemaDerivedType.RoleDerivedSchema),
                roleDerPageRows);

        add(getPaginatorForm(
                roleDerivedSchemasContainer,
                tableRolesDer,
                "RolesDerPaginatorForm",
                "roleDerPageRows",
                Constants.PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsRolesVir = getColumnsForSchema(
                roleVirtualSchemasContainer,
                editRoleVirtualSchemaWin,
                SchemaModalPageFactory.Entity.role,
                SchemaModalPageFactory.SchemaType.VIRTUAL,
                Constants.VIRTUAL_SCHEMA_FIELDS,
                allowedReadRoles,
                allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableRolesVir =
                new AjaxFallbackDefaultDataTable("datatable", columnsRolesVir,
                new VirtualSchemaProvider(SchemaVirtualType.RoleVirtualSchema),
                roleVirPageRows);

        add(getPaginatorForm(
                roleVirtualSchemasContainer,
                tableRolesVir,
                "RolesVirPaginatorForm",
                "roleVirPageRows",
                Constants.PREF_ROLE_VIR_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> userColumns = getColumnsForSchema(
                userSchemasContainer,
                editUserSchemaWin,
                SchemaModalPageFactory.Entity.user,
                SchemaModalPageFactory.SchemaType.NORMAL,
                Constants.SCHEMA_FIELDS,
                allowedReadRoles,
                allowedCreateRoles);

        final AjaxFallbackDefaultDataTable tableUsers =
                new AjaxFallbackDefaultDataTable("datatable", userColumns,
                new SchemaProvider(SchemaType.UserSchema), userSchemaPageRows);

        tableUsers.setMarkupId("tableUsers");

        add(getPaginatorForm(
                userSchemasContainer,
                tableUsers,
                "UsersPaginatorForm",
                "userSchemaPageRows",
                Constants.PREF_USER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsUsersDer = getColumnsForSchema(
                userDerivedSchemasContainer,
                editUserDerivedSchemaWin,
                SchemaModalPageFactory.Entity.user,
                SchemaModalPageFactory.SchemaType.DERIVED,
                Constants.DERIVED_SCHEMA_FIELDS,
                allowedReadRoles,
                allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableUsersDer =
                new AjaxFallbackDefaultDataTable("datatable", columnsUsersDer,
                new DerivedSchemaProvider(SchemaDerivedType.UserDerivedSchema),
                userDerSchemaPageRows);

        add(getPaginatorForm(
                userDerivedSchemasContainer,
                tableUsersDer,
                "UsersDerPaginatorForm",
                "userDerSchemaPageRows",
                Constants.PREF_USER_DER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsUsersVir = getColumnsForSchema(
                userVirtualSchemasContainer,
                editUserVirtualSchemaWin,
                SchemaModalPageFactory.Entity.user,
                SchemaModalPageFactory.SchemaType.VIRTUAL,
                Constants.VIRTUAL_SCHEMA_FIELDS,
                allowedReadRoles,
                allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableUsersVir =
                new AjaxFallbackDefaultDataTable("datatable", columnsUsersVir,
                new VirtualSchemaProvider(SchemaVirtualType.UserVirtualSchema),
                userVirSchemaPageRows);

        add(getPaginatorForm(
                userVirtualSchemasContainer,
                tableUsersVir,
                "UsersVirPaginatorForm",
                "userVirSchemaPageRows",
                Constants.PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> membershipsColumns = getColumnsForSchema(
                membershipSchemaContainer,
                editMembershipSchemaWin,
                SchemaModalPageFactory.Entity.membership,
                SchemaModalPageFactory.SchemaType.NORMAL,
                Constants.SCHEMA_FIELDS,
                allowedReadRoles,
                allowedCreateRoles);

        final AjaxFallbackDefaultDataTable tableMemberships =
                new AjaxFallbackDefaultDataTable(
                "datatable", membershipsColumns,
                new SchemaProvider(SchemaType.MembershipSchema),
                membershipPageRows);

        add(getPaginatorForm(
                membershipSchemaContainer,
                tableMemberships,
                "MembershipPaginatorForm",
                "membershipPageRows",
                Constants.PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsMembershipsDer = getColumnsForSchema(
                membershipDerivedSchemaContainer,
                editMembershipDerivedSchemaWin,
                SchemaModalPageFactory.Entity.membership,
                SchemaModalPageFactory.SchemaType.DERIVED,
                Constants.DERIVED_SCHEMA_FIELDS,
                allowedReadRoles,
                allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableMembershipsDer =
                new AjaxFallbackDefaultDataTable("datatable",
                columnsMembershipsDer,
                new DerivedSchemaProvider(
                SchemaDerivedType.MembershipDerivedSchema),
                membershipDerPageRows);

        add(getPaginatorForm(
                membershipDerivedSchemaContainer,
                tableMembershipsDer,
                "MembershipDerPaginatorForm",
                "membershipDerPageRows",
                Constants.PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsMembershipsVir = getColumnsForSchema(
                membershipVirtualSchemaContainer,
                editMembershipVirtualSchemaWin,
                SchemaModalPageFactory.Entity.membership,
                SchemaModalPageFactory.SchemaType.VIRTUAL,
                Constants.VIRTUAL_SCHEMA_FIELDS,
                allowedReadRoles,
                allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableMembershipsVir =
                new AjaxFallbackDefaultDataTable("datatable",
                columnsMembershipsVir,
                new VirtualSchemaProvider(
                SchemaVirtualType.MembershipVirtualSchema),
                membershipVirPageRows);

        add(getPaginatorForm(
                membershipVirtualSchemaContainer,
                tableMembershipsVir,
                "MembershipVirPaginatorForm",
                "membershipVirPageRows",
                Constants.PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS));

        roleSchemasContainer.add(tableRoles);
        roleSchemasContainer.setOutputMarkupId(true);

        roleDerivedSchemasContainer.add(tableRolesDer);
        roleDerivedSchemasContainer.setOutputMarkupId(true);

        roleVirtualSchemasContainer.add(tableRolesVir);
        roleVirtualSchemasContainer.setOutputMarkupId(true);

        userSchemasContainer.add(tableUsers);
        userSchemasContainer.setOutputMarkupId(true);

        userDerivedSchemasContainer.add(tableUsersDer);
        userDerivedSchemasContainer.setOutputMarkupId(true);

        userVirtualSchemasContainer.add(tableUsersVir);
        userVirtualSchemasContainer.setOutputMarkupId(true);

        membershipSchemaContainer.add(tableMemberships);
        membershipSchemaContainer.setOutputMarkupId(true);

        membershipDerivedSchemaContainer.add(tableMembershipsDer);
        membershipDerivedSchemaContainer.setOutputMarkupId(true);

        membershipVirtualSchemaContainer.add(tableMembershipsVir);
        membershipVirtualSchemaContainer.setOutputMarkupId(true);

        add(roleSchemasContainer);
        add(roleDerivedSchemasContainer);
        add(roleVirtualSchemasContainer);

        add(userSchemasContainer);
        add(userDerivedSchemasContainer);
        add(userVirtualSchemasContainer);

        add(membershipSchemaContainer);
        add(membershipDerivedSchemaContainer);
        add(membershipVirtualSchemaContainer);

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

        createUserVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        createUserVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        createUserVirtualSchemaWin.setPageMapName("modal-5");
        createUserVirtualSchemaWin.setCookieName("modal-5");

        editUserVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        editUserVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        editUserVirtualSchemaWin.setPageMapName("modal-6");
        editUserVirtualSchemaWin.setCookieName("modal-7");

        createRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleSchemaWin.setPageMapName("modal-7");
        createRoleSchemaWin.setCookieName("modal-7");

        editRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleSchemaWin.setPageMapName("modal-8");
        editRoleSchemaWin.setCookieName("modal-8");

        createRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createRoleDerivedSchemaWin.setPageMapName("modal-9");
        createRoleDerivedSchemaWin.setCookieName("modal-9");

        editRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        editRoleDerivedSchemaWin.setPageMapName("modal-10");
        editRoleDerivedSchemaWin.setCookieName("modal-10");

        createRoleVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        createRoleVirtualSchemaWin.setPageMapName("modal-11");
        createRoleVirtualSchemaWin.setCookieName("modal-11");

        editRoleVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        editRoleVirtualSchemaWin.setPageMapName("modal-12");
        editRoleVirtualSchemaWin.setCookieName("modal-12");

        createMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipSchemaWin.setPageMapName("modal-13");
        createMembershipSchemaWin.setCookieName("modal-13");

        editMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipSchemaWin.setPageMapName("modal-14");
        editMembershipSchemaWin.setCookieName("modal-14");

        createMembershipDerivedSchemaWin.setCssClassName(
                ModalWindow.CSS_CLASS_GRAY);
        createMembershipDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createMembershipDerivedSchemaWin.setPageMapName("modal-15");
        createMembershipDerivedSchemaWin.setCookieName("modal-15");

        editMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipSchemaWin.setPageMapName("modal-16");
        editMembershipSchemaWin.setCookieName("modal-16");

        createMembershipVirtualSchemaWin.setCssClassName(
                ModalWindow.CSS_CLASS_GRAY);
        createMembershipVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        createMembershipVirtualSchemaWin.setPageMapName("modal-17");
        createMembershipVirtualSchemaWin.setCookieName("modal-17");

        editMembershipVirtualSchemaWin.setCssClassName(
                ModalWindow.CSS_CLASS_GRAY);
        editMembershipVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        editMembershipVirtualSchemaWin.setPageMapName("modal-18");
        editMembershipVirtualSchemaWin.setCookieName("modal-18");

        setWindowClosedCallback(createUserSchemaWin, userSchemasContainer);

        setWindowClosedCallback(editUserSchemaWin, userSchemasContainer);

        setWindowClosedCallback(createUserDerivedSchemaWin,
                userDerivedSchemasContainer);

        setWindowClosedCallback(createUserVirtualSchemaWin,
                userVirtualSchemasContainer);

        setWindowClosedCallback(editUserDerivedSchemaWin,
                userDerivedSchemasContainer);

        setWindowClosedCallback(editUserVirtualSchemaWin,
                userVirtualSchemasContainer);

        setWindowClosedCallback(createRoleSchemaWin, roleSchemasContainer);

        setWindowClosedCallback(editRoleSchemaWin, roleSchemasContainer);

        setWindowClosedCallback(createRoleDerivedSchemaWin,
                roleDerivedSchemasContainer);

        setWindowClosedCallback(createRoleVirtualSchemaWin,
                roleVirtualSchemasContainer);

        setWindowClosedCallback(editRoleDerivedSchemaWin,
                roleDerivedSchemasContainer);

        setWindowClosedCallback(editRoleVirtualSchemaWin,
                roleVirtualSchemasContainer);

        setWindowClosedCallback(createMembershipSchemaWin,
                membershipSchemaContainer);

        setWindowClosedCallback(editMembershipSchemaWin,
                membershipSchemaContainer);

        setWindowClosedCallback(createMembershipDerivedSchemaWin,
                membershipDerivedSchemaContainer);

        setWindowClosedCallback(createMembershipVirtualSchemaWin,
                membershipVirtualSchemaContainer);

        setWindowClosedCallback(editMembershipDerivedSchemaWin,
                membershipDerivedSchemaContainer);

        setWindowClosedCallback(editMembershipVirtualSchemaWin,
                membershipVirtualSchemaContainer);

        add(getCreateSchemaWindow(
                createRoleSchemaWin,
                SchemaModalPageFactory.Entity.role,
                SchemaModalPageFactory.SchemaType.NORMAL,
                "createRoleSchemaWinLink",
                "createRoleSchemaWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createRoleDerivedSchemaWin,
                SchemaModalPageFactory.Entity.role,
                SchemaModalPageFactory.SchemaType.DERIVED,
                "createRoleDerivedSchemaWinLink",
                "createRoleDerivedSchemaWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createRoleVirtualSchemaWin,
                SchemaModalPageFactory.Entity.role,
                SchemaModalPageFactory.SchemaType.VIRTUAL,
                "createRoleVirtualSchemaWinLink",
                "createRoleVirtualSchemaWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createUserSchemaWin,
                SchemaModalPageFactory.Entity.user,
                SchemaModalPageFactory.SchemaType.NORMAL,
                "createUserSchemaWinLink",
                "createUserSchemaModalWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createUserDerivedSchemaWin,
                SchemaModalPageFactory.Entity.user,
                SchemaModalPageFactory.SchemaType.DERIVED,
                "createUserDerSchemaWinLink",
                "createUserDerSchemaModalWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createUserVirtualSchemaWin,
                SchemaModalPageFactory.Entity.user,
                SchemaModalPageFactory.SchemaType.VIRTUAL,
                "createUserVirSchemaWinLink",
                "createUserVirSchemaModalWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createMembershipSchemaWin,
                SchemaModalPageFactory.Entity.membership,
                SchemaModalPageFactory.SchemaType.NORMAL,
                "createMembershipSchemaWinLink",
                "createMembershipSchemaModalWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createMembershipDerivedSchemaWin,
                SchemaModalPageFactory.Entity.membership,
                SchemaModalPageFactory.SchemaType.DERIVED,
                "createMembershipDerSchemaWinLink",
                "createMembershipDerivedSchemaWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(
                createMembershipVirtualSchemaWin,
                SchemaModalPageFactory.Entity.membership,
                SchemaModalPageFactory.SchemaType.VIRTUAL,
                "createMembershipVirSchemaWinLink",
                "createMembershipVirtualSchemaWin",
                allowedCreateRoles));
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(final ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.addComponent(container);
                        if (operationResult) {
                            info(getString("operation_succeded"));
                            target.addComponent(feedbackPanel);
                            operationResult = false;
                        }
                    }
                });
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }

    private class SchemaProvider extends SortableDataProvider<SchemaTO> {

        private final SortableDataProviderComparator<SchemaTO> comparator;

        private final SchemaType schemaType;

        public SchemaProvider(final SchemaType schemaType) {
            super();
            this.schemaType = schemaType;

            //Default sorting
            setSort("name", true);

            comparator = new SortableDataProviderComparator<SchemaTO>(this);
        }

        @Override
        public Iterator<SchemaTO> iterator(final int first, final int count) {
            List<SchemaTO> list = getSchemaDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getSchemaDB().size();
        }

        @Override
        public IModel<SchemaTO> model(final SchemaTO object) {
            return new CompoundPropertyModel<SchemaTO>(object);
        }

        private List<SchemaTO> getSchemaDB() {
            List<SchemaTO> list;
            switch (schemaType) {
                case UserSchema:
                    list = restClient.getSchemas("user");
                    break;

                case RoleSchema:
                    list = restClient.getSchemas("role");
                    break;

                case MembershipSchema:
                    list = restClient.getSchemas("membership");
                    break;

                default:
                    list = Collections.EMPTY_LIST;
            }

            return list;
        }
    }

    private class DerivedSchemaProvider
            extends SortableDataProvider<DerivedSchemaTO> {

        private SortableDataProviderComparator<DerivedSchemaTO> comparator;

        private SchemaDerivedType schema;

        public DerivedSchemaProvider(final SchemaDerivedType schema) {
            super();
            this.schema = schema;

            //Default sorting
            setSort("name", true);
            comparator =
                    new SortableDataProviderComparator<DerivedSchemaTO>(this);
        }

        @Override
        public Iterator<DerivedSchemaTO> iterator(int first, int count) {
            List<DerivedSchemaTO> list = getDerivedSchemaDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getDerivedSchemaDB().size();
        }

        @Override
        public IModel<DerivedSchemaTO> model(final DerivedSchemaTO object) {
            return new CompoundPropertyModel<DerivedSchemaTO>(object);
        }

        private List<DerivedSchemaTO> getDerivedSchemaDB() {

            List<DerivedSchemaTO> list = null;

            if (schema == SchemaDerivedType.RoleDerivedSchema) {
                list = restClient.getDerivedSchemas("role");
            } else if (schema == SchemaDerivedType.UserDerivedSchema) {
                list = restClient.getDerivedSchemas("user");
            } else if (schema == SchemaDerivedType.MembershipDerivedSchema) {
                list = restClient.getDerivedSchemas("membership");
            }

            return list;
        }
    }

    private class VirtualSchemaProvider
            extends SortableDataProvider<VirtualSchemaTO> {

        private SortableDataProviderComparator<VirtualSchemaTO> comparator;

        private SchemaVirtualType schema;

        public VirtualSchemaProvider(final SchemaVirtualType schema) {
            super();
            this.schema = schema;

            //Default sorting
            setSort("name", true);
            comparator =
                    new SortableDataProviderComparator<VirtualSchemaTO>(this);
        }

        @Override
        public Iterator<VirtualSchemaTO> iterator(int first, int count) {
            List<VirtualSchemaTO> list = getVirtualSchemaDB();

            Collections.sort(list, comparator);

            return list.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            return getVirtualSchemaDB().size();
        }

        @Override
        public IModel<VirtualSchemaTO> model(final VirtualSchemaTO object) {
            return new CompoundPropertyModel<VirtualSchemaTO>(object);
        }

        private List<VirtualSchemaTO> getVirtualSchemaDB() {

            List<VirtualSchemaTO> list = null;

            if (schema == SchemaVirtualType.RoleVirtualSchema) {
                list = restClient.getVirtualSchemas("role");
            } else if (schema == SchemaVirtualType.UserVirtualSchema) {
                list = restClient.getVirtualSchemas("user");
            } else if (schema == SchemaVirtualType.MembershipVirtualSchema) {
                list = restClient.getVirtualSchemas("membership");
            }

            return list;
        }
    }

    private <T extends AbstractSchemaModalPage> List<IColumn> getColumnsForSchema(
            final WebMarkupContainer webContainer,
            final ModalWindow modalWindow,
            final SchemaModalPageFactory.Entity entity,
            final SchemaModalPageFactory.SchemaType schemaType,
            final String[] fields,
            final String readPermissions,
            final String deletePermissions) {

        List<IColumn> columns = new ArrayList<IColumn>();

        for (String field : fields) {
            columns.add(
                    new PropertyColumn(new Model(getString(field)),
                    field,
                    field));
        }

        columns.add(new AbstractColumn<AbstractBaseBean>(
                new Model<String>(getString("edit"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<AbstractBaseBean>> cellItem,
                    final String componentId,
                    final IModel<AbstractBaseBean> model) {

                final AbstractBaseBean schemaTO = model.getObject();

                AjaxLink editLink = new IndicatingAjaxLink("editLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        modalWindow.setPageCreator(
                                new ModalWindow.PageCreator() {

                                    @Override
                                    public Page createPage() {
                                        AbstractSchemaModalPage page =
                                                SchemaModalPageFactory.getSchemaModalPage(entity, schemaType);

                                        page.setSchemaModalPage(
                                                Schema.this,
                                                modalWindow,
                                                schemaTO,
                                                false);

                                        return page;
                                    }
                                });

                        modalWindow.show(target);
                    }
                };

                EditLinkPanel panel = new EditLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(
                        panel, ENABLE, readPermissions);

                panel.add(editLink);

                cellItem.add(panel);
            }
        });

        columns.add(new AbstractColumn<AbstractBaseBean>(
                new Model<String>(getString("delete"))) {

            @Override
            public void populateItem(
                    final Item<ICellPopulator<AbstractBaseBean>> cellItem,
                    final String componentId,
                    final IModel<AbstractBaseBean> model) {

                final AbstractBaseBean schemaTO = model.getObject();

                AjaxLink deleteLink = new IndicatingDeleteOnConfirmAjaxLink(
                        "deleteLink") {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        switch (schemaType) {
                            case DERIVED:
                                restClient.deleteDerivedSchema(
                                        entity.toString(),
                                        ((DerivedSchemaTO) schemaTO).getName());
                                break;
                            case VIRTUAL:
                                restClient.deleteVirtualSchema(
                                        entity.toString(),
                                        ((VirtualSchemaTO) schemaTO).getName());
                                break;
                            default:
                                restClient.deleteSchema(
                                        entity.toString(),
                                        ((SchemaTO) schemaTO).getName());
                                break;
                        }

                        info(getString("operation_succeded"));
                        target.addComponent(feedbackPanel);

                        target.addComponent(webContainer);
                    }
                };

                DeleteLinkPanel panel = new DeleteLinkPanel(componentId, model);

                MetaDataRoleAuthorizationStrategy.authorize(
                        panel, ENABLE, deletePermissions);

                panel.add(deleteLink);

                cellItem.add(panel);
            }
        });

        return columns;
    }

    private Form getPaginatorForm(
            final WebMarkupContainer webContainer,
            final AjaxFallbackDefaultDataTable dataTable,
            final String formname,
            final String rowname,
            final String rowsPerPagePrefName) {

        Form usersPaginatorForm = new Form(formname);

        final DropDownChoice rowChooser = new DropDownChoice(
                "rowsChooser",
                new PropertyModel(this, rowname),
                prefMan.getPaginatorChoices(),
                new SelectChoiceRenderer());

        rowChooser.add(
                new AjaxFormComponentUpdatingBehavior("onchange") {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        prefMan.set(getWebRequestCycle().getWebRequest(),
                                getWebRequestCycle().getWebResponse(),
                                rowsPerPagePrefName,
                                String.valueOf(rowChooser.getInput()));
                        dataTable.setRowsPerPage(
                                Integer.parseInt(rowChooser.getInput()));

                        target.addComponent(webContainer);
                    }
                });

        usersPaginatorForm.add(rowChooser);

        return usersPaginatorForm;
    }

    private <T extends AbstractSchemaModalPage> AjaxLink getCreateSchemaWindow(
            final ModalWindow createSchemaWin,
            final SchemaModalPageFactory.Entity entity,
            final SchemaModalPageFactory.SchemaType schemaType,
            final String winLinkName,
            final String winName,
            final String createPermissions) {

        AjaxLink createSchemaWinLink = new IndicatingAjaxLink(winLinkName) {

            @Override
            public void onClick(final AjaxRequestTarget target) {

                createSchemaWin.setPageCreator(
                        new ModalWindow.PageCreator() {

                            public Page createPage() {
                                AbstractSchemaModalPage page =
                                        SchemaModalPageFactory.getSchemaModalPage(entity, schemaType);

                                page.setSchemaModalPage(
                                        Schema.this,
                                        new ModalWindow(winName),
                                        null,
                                        true);

                                return page;
                            }
                        });

                createSchemaWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(
                createSchemaWinLink, ENABLE, createPermissions);

        return createSchemaWinLink;
    }
}
