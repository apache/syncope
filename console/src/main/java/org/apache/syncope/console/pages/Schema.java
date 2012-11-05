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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.syncope.AbstractBaseBean;
import org.apache.syncope.to.DerivedSchemaTO;
import org.apache.syncope.to.SchemaTO;
import org.apache.syncope.to.VirtualSchemaTO;
import org.apache.syncope.types.AttributableType;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SchemaModalPageFactory;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;

/**
 * Schema WebPage.
 */
public class Schema extends BasePage {

    private static final long serialVersionUID = 8091922398776299403L;

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

    public Schema(PageParameters parameters) {
        super(parameters);

        userSchemasContainer = new WebMarkupContainer("userSchemasContainer");

        userDerivedSchemasContainer = new WebMarkupContainer("userDerivedSchemasContainer");

        userVirtualSchemasContainer = new WebMarkupContainer("userVirtualSchemasContainer");

        roleSchemasContainer = new WebMarkupContainer("roleSchemasContainer");

        roleDerivedSchemasContainer = new WebMarkupContainer("roleDerivedSchemasContainer");

        roleVirtualSchemasContainer = new WebMarkupContainer("roleVirtualSchemasContainer");

        membershipSchemaContainer = new WebMarkupContainer("membershipSchemaContainer");

        membershipDerivedSchemaContainer = new WebMarkupContainer("membershipDerivedSchemaContainer");

        membershipVirtualSchemaContainer = new WebMarkupContainer("membershipVirtualSchemaContainer");

        add(createRoleSchemaWin = new ModalWindow("createRoleSchemaWin"));
        add(editRoleSchemaWin = new ModalWindow("editRoleSchemaWin"));

        add(createRoleDerivedSchemaWin = new ModalWindow("createRoleDerivedSchemaWin"));

        add(createRoleVirtualSchemaWin = new ModalWindow("createRoleVirtualSchemaWin"));

        add(editRoleDerivedSchemaWin = new ModalWindow("editRoleDerivedSchemaWin"));

        add(editRoleVirtualSchemaWin = new ModalWindow("editRoleVirtualSchemaWin"));

        add(createUserSchemaWin = new ModalWindow("createUserSchemaWin"));

        add(editUserSchemaWin = new ModalWindow("editUserSchemaWin"));

        add(createUserDerivedSchemaWin = new ModalWindow("createUserDerSchemaWin"));
        add(createUserVirtualSchemaWin = new ModalWindow("createUserVirSchemaWin"));

        add(editUserDerivedSchemaWin = new ModalWindow("editUserDerSchemaWin"));
        add(editUserVirtualSchemaWin = new ModalWindow("editUserVirSchemaWin"));

        add(createMembershipSchemaWin = new ModalWindow("createMembershipSchemaWin"));
        add(editMembershipSchemaWin = new ModalWindow("editMembershipSchemaWin"));

        add(createMembershipDerivedSchemaWin = new ModalWindow("createMembershipDerSchemaWin"));
        add(createMembershipVirtualSchemaWin = new ModalWindow("createMembershipVirSchemaWin"));

        add(editMembershipDerivedSchemaWin = new ModalWindow("editMembershipDerSchemaWin"));
        add(editMembershipVirtualSchemaWin = new ModalWindow("editMembershipVirSchemaWin"));

        rolePageRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_ROLE_SCHEMA_PAGINATOR_ROWS);

        roleDerPageRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS);

        roleVirPageRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_ROLE_VIR_SCHEMA_PAGINATOR_ROWS);

        userSchemaPageRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_USER_SCHEMA_PAGINATOR_ROWS);

        userDerSchemaPageRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_USER_DER_SCHEMA_PAGINATOR_ROWS);

        userVirSchemaPageRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS);

        membershipPageRows = prefMan.getPaginatorRows(getRequest(), Constants.PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS);

        membershipDerPageRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS);

        membershipVirPageRows = prefMan.getPaginatorRows(getRequest(),
                Constants.PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS);

        final String allowedCreateRoles = xmlRolesReader.getAllAllowedRoles("Schema", "create");

        final String allowedReadRoles = xmlRolesReader.getAllAllowedRoles("Schema", "read");

        final String allowedDeleteRoles = xmlRolesReader.getAllAllowedRoles("Schema", "delete");

        List<IColumn> rolesColumns = getColumnsForSchema(roleSchemasContainer, editRoleSchemaWin,
                AttributableType.ROLE, SchemaModalPageFactory.SchemaType.NORMAL, Constants.SCHEMA_FIELDS,
                allowedReadRoles, allowedCreateRoles);

        final AjaxFallbackDefaultDataTable tableRoles = new AjaxFallbackDefaultDataTable("datatable", rolesColumns,
                new SchemaProvider(SchemaType.RoleSchema), rolePageRows);

        add(getPaginatorForm(roleSchemasContainer, tableRoles, "RolesPaginatorForm", "rolePageRows",
                Constants.PREF_ROLE_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsRolesDer = getColumnsForSchema(roleDerivedSchemasContainer, editRoleDerivedSchemaWin,
                AttributableType.ROLE, SchemaModalPageFactory.SchemaType.DERIVED, Constants.DERIVED_SCHEMA_FIELDS,
                allowedReadRoles, allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableRolesDer = new AjaxFallbackDefaultDataTable("datatable",
                columnsRolesDer, new DerivedSchemaProvider(SchemaDerivedType.RoleDerivedSchema), roleDerPageRows);

        add(getPaginatorForm(roleDerivedSchemasContainer, tableRolesDer, "RolesDerPaginatorForm", "roleDerPageRows",
                Constants.PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsRolesVir = getColumnsForSchema(roleVirtualSchemasContainer, editRoleVirtualSchemaWin,
                AttributableType.ROLE, SchemaModalPageFactory.SchemaType.VIRTUAL, Constants.VIRTUAL_SCHEMA_FIELDS,
                allowedReadRoles, allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableRolesVir = new AjaxFallbackDefaultDataTable("datatable",
                columnsRolesVir, new VirtualSchemaProvider(SchemaVirtualType.RoleVirtualSchema), roleVirPageRows);

        add(getPaginatorForm(roleVirtualSchemasContainer, tableRolesVir, "RolesVirPaginatorForm", "roleVirPageRows",
                Constants.PREF_ROLE_VIR_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> userColumns = getColumnsForSchema(userSchemasContainer, editUserSchemaWin, AttributableType.USER,
                SchemaModalPageFactory.SchemaType.NORMAL, Constants.SCHEMA_FIELDS, allowedReadRoles, allowedCreateRoles);

        final AjaxFallbackDefaultDataTable tableUsers = new AjaxFallbackDefaultDataTable("datatable", userColumns,
                new SchemaProvider(SchemaType.UserSchema), userSchemaPageRows);

        tableUsers.setMarkupId("tableUsers");

        add(getPaginatorForm(userSchemasContainer, tableUsers, "UsersPaginatorForm", "userSchemaPageRows",
                Constants.PREF_USER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsUsersDer = getColumnsForSchema(userDerivedSchemasContainer, editUserDerivedSchemaWin,
                AttributableType.USER, SchemaModalPageFactory.SchemaType.DERIVED, Constants.DERIVED_SCHEMA_FIELDS,
                allowedReadRoles, allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableUsersDer = new AjaxFallbackDefaultDataTable("datatable",
                columnsUsersDer, new DerivedSchemaProvider(SchemaDerivedType.UserDerivedSchema), userDerSchemaPageRows);

        add(getPaginatorForm(userDerivedSchemasContainer, tableUsersDer, "UsersDerPaginatorForm",
                "userDerSchemaPageRows", Constants.PREF_USER_DER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsUsersVir = getColumnsForSchema(userVirtualSchemasContainer, editUserVirtualSchemaWin,
                AttributableType.USER, SchemaModalPageFactory.SchemaType.VIRTUAL, Constants.VIRTUAL_SCHEMA_FIELDS,
                allowedReadRoles, allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableUsersVir = new AjaxFallbackDefaultDataTable("datatable",
                columnsUsersVir, new VirtualSchemaProvider(SchemaVirtualType.UserVirtualSchema), userVirSchemaPageRows);

        add(getPaginatorForm(userVirtualSchemasContainer, tableUsersVir, "UsersVirPaginatorForm",
                "userVirSchemaPageRows", Constants.PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> membershipsColumns = getColumnsForSchema(membershipSchemaContainer, editMembershipSchemaWin,
                AttributableType.MEMBERSHIP, SchemaModalPageFactory.SchemaType.NORMAL, Constants.SCHEMA_FIELDS,
                allowedReadRoles, allowedCreateRoles);

        final AjaxFallbackDefaultDataTable tableMemberships = new AjaxFallbackDefaultDataTable("datatable",
                membershipsColumns, new SchemaProvider(SchemaType.MembershipSchema), membershipPageRows);

        add(getPaginatorForm(membershipSchemaContainer, tableMemberships, "MembershipPaginatorForm",
                "membershipPageRows", Constants.PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsMembershipsDer = getColumnsForSchema(membershipDerivedSchemaContainer,
                editMembershipDerivedSchemaWin, AttributableType.MEMBERSHIP, SchemaModalPageFactory.SchemaType.DERIVED,
                Constants.DERIVED_SCHEMA_FIELDS, allowedReadRoles, allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableMembershipsDer = new AjaxFallbackDefaultDataTable("datatable",
                columnsMembershipsDer, new DerivedSchemaProvider(SchemaDerivedType.MembershipDerivedSchema),
                membershipDerPageRows);

        add(getPaginatorForm(membershipDerivedSchemaContainer, tableMembershipsDer, "MembershipDerPaginatorForm",
                "membershipDerPageRows", Constants.PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS));

        List<IColumn> columnsMembershipsVir = getColumnsForSchema(membershipVirtualSchemaContainer,
                editMembershipVirtualSchemaWin, AttributableType.MEMBERSHIP, SchemaModalPageFactory.SchemaType.VIRTUAL,
                Constants.VIRTUAL_SCHEMA_FIELDS, allowedReadRoles, allowedDeleteRoles);

        final AjaxFallbackDefaultDataTable tableMembershipsVir = new AjaxFallbackDefaultDataTable("datatable",
                columnsMembershipsVir, new VirtualSchemaProvider(SchemaVirtualType.MembershipVirtualSchema),
                membershipVirPageRows);

        add(getPaginatorForm(membershipVirtualSchemaContainer, tableMembershipsVir, "MembershipVirPaginatorForm",
                "membershipVirPageRows", Constants.PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS));

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
        createUserSchemaWin.setCookieName("modal-1");
        createUserSchemaWin.setMarkupId("createUserSchemaWin");

        editUserSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserSchemaWin.setInitialWidth(WIN_WIDTH);
        editUserSchemaWin.setCookieName("modal-2");
        editUserSchemaWin.setMarkupId("editUserSchemaWin");

        createUserDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createUserDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createUserDerivedSchemaWin.setCookieName("modal-3");

        editUserDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        editUserDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        editUserDerivedSchemaWin.setCookieName("modal-4");

        createUserVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createUserVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        createUserVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        createUserVirtualSchemaWin.setCookieName("modal-5");

        editUserVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editUserVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        editUserVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        editUserVirtualSchemaWin.setCookieName("modal-7");

        createRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleSchemaWin.setCookieName("modal-7");

        editRoleSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleSchemaWin.setCookieName("modal-8");

        createRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createRoleDerivedSchemaWin.setCookieName("modal-9");

        editRoleDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        editRoleDerivedSchemaWin.setCookieName("modal-10");

        createRoleVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        createRoleVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        createRoleVirtualSchemaWin.setCookieName("modal-11");

        editRoleVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editRoleVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        editRoleVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        editRoleVirtualSchemaWin.setCookieName("modal-12");

        createMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipSchemaWin.setCookieName("modal-13");

        editMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipSchemaWin.setCookieName("modal-14");

        createMembershipDerivedSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipDerivedSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipDerivedSchemaWin.setInitialHeight(WIN_HEIGHT);
        createMembershipDerivedSchemaWin.setCookieName("modal-15");

        editMembershipSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editMembershipSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipSchemaWin.setCookieName("modal-16");

        createMembershipVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createMembershipVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        createMembershipVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        createMembershipVirtualSchemaWin.setCookieName("modal-17");

        editMembershipVirtualSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editMembershipVirtualSchemaWin.setInitialWidth(WIN_WIDTH);
        editMembershipVirtualSchemaWin.setInitialHeight(WIN_HEIGHT);
        editMembershipVirtualSchemaWin.setCookieName("modal-18");

        setWindowClosedCallback(createUserSchemaWin, userSchemasContainer);

        setWindowClosedCallback(editUserSchemaWin, userSchemasContainer);

        setWindowClosedCallback(createUserDerivedSchemaWin, userDerivedSchemasContainer);

        setWindowClosedCallback(createUserVirtualSchemaWin, userVirtualSchemasContainer);

        setWindowClosedCallback(editUserDerivedSchemaWin, userDerivedSchemasContainer);

        setWindowClosedCallback(editUserVirtualSchemaWin, userVirtualSchemasContainer);

        setWindowClosedCallback(createRoleSchemaWin, roleSchemasContainer);

        setWindowClosedCallback(editRoleSchemaWin, roleSchemasContainer);

        setWindowClosedCallback(createRoleDerivedSchemaWin, roleDerivedSchemasContainer);

        setWindowClosedCallback(createRoleVirtualSchemaWin, roleVirtualSchemasContainer);

        setWindowClosedCallback(editRoleDerivedSchemaWin, roleDerivedSchemasContainer);

        setWindowClosedCallback(editRoleVirtualSchemaWin, roleVirtualSchemasContainer);

        setWindowClosedCallback(createMembershipSchemaWin, membershipSchemaContainer);

        setWindowClosedCallback(editMembershipSchemaWin, membershipSchemaContainer);

        setWindowClosedCallback(createMembershipDerivedSchemaWin, membershipDerivedSchemaContainer);

        setWindowClosedCallback(createMembershipVirtualSchemaWin, membershipVirtualSchemaContainer);

        setWindowClosedCallback(editMembershipDerivedSchemaWin, membershipDerivedSchemaContainer);

        setWindowClosedCallback(editMembershipVirtualSchemaWin, membershipVirtualSchemaContainer);

        add(getCreateSchemaWindow(createRoleSchemaWin, AttributableType.ROLE, SchemaModalPageFactory.SchemaType.NORMAL,
                "createRoleSchemaWinLink", "createRoleSchemaWin", allowedCreateRoles));

        add(getCreateSchemaWindow(createRoleDerivedSchemaWin, AttributableType.ROLE,
                SchemaModalPageFactory.SchemaType.DERIVED, "createRoleDerivedSchemaWinLink",
                "createRoleDerivedSchemaWin", allowedCreateRoles));

        add(getCreateSchemaWindow(createRoleVirtualSchemaWin, AttributableType.ROLE,
                SchemaModalPageFactory.SchemaType.VIRTUAL, "createRoleVirtualSchemaWinLink",
                "createRoleVirtualSchemaWin", allowedCreateRoles));

        add(getCreateSchemaWindow(createUserSchemaWin, AttributableType.USER, SchemaModalPageFactory.SchemaType.NORMAL,
                "createUserSchemaWinLink", "createUserSchemaModalWin", allowedCreateRoles));

        add(getCreateSchemaWindow(createUserDerivedSchemaWin, AttributableType.USER,
                SchemaModalPageFactory.SchemaType.DERIVED, "createUserDerSchemaWinLink", "createUserDerSchemaModalWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(createUserVirtualSchemaWin, AttributableType.USER,
                SchemaModalPageFactory.SchemaType.VIRTUAL, "createUserVirSchemaWinLink", "createUserVirSchemaModalWin",
                allowedCreateRoles));

        add(getCreateSchemaWindow(createMembershipSchemaWin, AttributableType.MEMBERSHIP,
                SchemaModalPageFactory.SchemaType.NORMAL, "createMembershipSchemaWinLink",
                "createMembershipSchemaModalWin", allowedCreateRoles));

        add(getCreateSchemaWindow(createMembershipDerivedSchemaWin, AttributableType.MEMBERSHIP,
                SchemaModalPageFactory.SchemaType.DERIVED, "createMembershipDerSchemaWinLink",
                "createMembershipDerivedSchemaWin", allowedCreateRoles));

        add(getCreateSchemaWindow(createMembershipVirtualSchemaWin, AttributableType.MEMBERSHIP,
                SchemaModalPageFactory.SchemaType.VIRTUAL, "createMembershipVirSchemaWinLink",
                "createMembershipVirtualSchemaWin", allowedCreateRoles));
    }

    private class SchemaProvider extends SortableDataProvider<SchemaTO> {

        private static final long serialVersionUID = 712816496206559637L;

        private final SortableDataProviderComparator<SchemaTO> comparator;

        private final SchemaType schemaType;

        public SchemaProvider(final SchemaType schemaType) {
            super();
            this.schemaType = schemaType;

            //Default sorting
            setSort("name", SortOrder.ASCENDING);

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
                    list = restClient.getSchemas(AttributableType.USER);
                    break;

                case RoleSchema:
                    list = restClient.getSchemas(AttributableType.ROLE);
                    break;

                case MembershipSchema:
                    list = restClient.getSchemas(AttributableType.MEMBERSHIP);
                    break;

                default:
                    list = Collections.emptyList();
            }

            return list;
        }
    }

    private class DerivedSchemaProvider extends SortableDataProvider<DerivedSchemaTO> {

        private static final long serialVersionUID = -8518694430295937917L;

        private SortableDataProviderComparator<DerivedSchemaTO> comparator;

        private SchemaDerivedType schema;

        public DerivedSchemaProvider(final SchemaDerivedType schema) {
            super();
            this.schema = schema;

            //Default sorting
            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<DerivedSchemaTO>(this);
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
                list = restClient.getDerivedSchemas(AttributableType.ROLE);
            } else if (schema == SchemaDerivedType.UserDerivedSchema) {
                list = restClient.getDerivedSchemas(AttributableType.USER);
            } else if (schema == SchemaDerivedType.MembershipDerivedSchema) {
                list = restClient.getDerivedSchemas(AttributableType.MEMBERSHIP);
            }

            return list;
        }
    }

    private class VirtualSchemaProvider extends SortableDataProvider<VirtualSchemaTO> {

        private static final long serialVersionUID = -5431560608852987760L;

        private SortableDataProviderComparator<VirtualSchemaTO> comparator;

        private SchemaVirtualType schema;

        public VirtualSchemaProvider(final SchemaVirtualType schema) {
            super();
            this.schema = schema;

            //Default sorting
            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<VirtualSchemaTO>(this);
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
                list = restClient.getVirtualSchemas(AttributableType.ROLE);
            } else if (schema == SchemaVirtualType.UserVirtualSchema) {
                list = restClient.getVirtualSchemas(AttributableType.USER);
            } else if (schema == SchemaVirtualType.MembershipVirtualSchema) {
                list = restClient.getVirtualSchemas(AttributableType.MEMBERSHIP);
            }

            return list;
        }
    }

    private <T extends AbstractSchemaModalPage> List<IColumn> getColumnsForSchema(
            final WebMarkupContainer webContainer, final ModalWindow modalWindow,
            final AttributableType attributableType, final SchemaModalPageFactory.SchemaType schemaType,
            final String[] fields, final String readPermissions, final String deletePermissions) {

        List<IColumn> columns = new ArrayList<IColumn>();

        for (String field : fields) {
            columns.add(new PropertyColumn(new ResourceModel(field), field, field));
        }

        columns.add(new AbstractColumn<AbstractBaseBean>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<AbstractBaseBean>> cellItem, final String componentId,
                    final IModel<AbstractBaseBean> model) {

                final AbstractBaseBean schemaTO = model.getObject();

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        modalWindow.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                AbstractSchemaModalPage page = SchemaModalPageFactory.getSchemaModalPage(
                                        attributableType, schemaType);

                                page.setSchemaModalPage(Schema.this.getPageReference(), modalWindow, schemaTO, false);

                                return page;
                            }
                        });

                        modalWindow.show(target);
                    }
                }, ActionType.EDIT, readPermissions);

                panel.add(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        switch (schemaType) {
                            case DERIVED:
                                restClient
                                        .deleteDerivedSchema(attributableType, ((DerivedSchemaTO) schemaTO).getName());
                                break;
                            case VIRTUAL:
                                restClient
                                        .deleteVirtualSchema(attributableType, ((VirtualSchemaTO) schemaTO).getName());
                                break;
                            default:
                                restClient.deleteSchema(attributableType, ((SchemaTO) schemaTO).getName());
                                break;
                        }

                        info(getString("operation_succeded"));
                        target.add(feedbackPanel);

                        target.add(webContainer);
                    }
                }, ActionType.DELETE, deletePermissions);

                cellItem.add(panel);
            }
        });

        return columns;
    }

    private Form getPaginatorForm(final WebMarkupContainer webContainer, final AjaxFallbackDefaultDataTable dataTable,
            final String formname, final String rowname, final String rowsPerPagePrefName) {

        Form usersPaginatorForm = new Form(formname);

        final DropDownChoice rowChooser = new DropDownChoice("rowsChooser", new PropertyModel(this, rowname), prefMan
                .getPaginatorChoices(), new SelectChoiceRenderer());

        rowChooser.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), rowsPerPagePrefName, String.valueOf(rowChooser.getInput()));
                dataTable.setItemsPerPage(Integer.parseInt(rowChooser.getInput()));

                target.add(webContainer);
            }
        });

        usersPaginatorForm.add(rowChooser);

        return usersPaginatorForm;
    }

    private <T extends AbstractSchemaModalPage> AjaxLink getCreateSchemaWindow(final ModalWindow createSchemaWin,
            final AttributableType attributableType, final SchemaModalPageFactory.SchemaType schemaType,
            final String winLinkName, final String winName, final String createPermissions) {

        AjaxLink createSchemaWinLink = new IndicatingAjaxLink(winLinkName) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {

                createSchemaWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        AbstractSchemaModalPage page = SchemaModalPageFactory.getSchemaModalPage(attributableType,
                                schemaType);

                        page.setSchemaModalPage(Schema.this.getPageReference(), new ModalWindow(winName), null, true);

                        return page;
                    }
                });

                createSchemaWin.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(createSchemaWinLink, ENABLE, createPermissions);

        return createSchemaWinLink;
    }
}
