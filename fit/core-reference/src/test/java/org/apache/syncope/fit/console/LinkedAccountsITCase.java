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
package org.apache.syncope.fit.console;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.core.UserITCase;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

public class LinkedAccountsITCase extends AbstractConsoleITCase {
    private static final String TAB_PANEL = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private static final String RESULT_DATA_TABLE = "searchResult:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:";

    private static final String SEARCH_PANEL = "outerObjectsRepeater:6:outer:form:content:mlpContainer:firstLevelContainer:first:form:view"
        + ":ownerContainer:search:";

    private static final String USER_SEARCH_PANEL = SEARCH_PANEL + "usersearch:";

    private static final String USER_SEARCH_FORM = TAB_PANEL + USER_SEARCH_PANEL + "searchFormContainer:search:multiValueContainer:innerForm:";

    private static final String CONTAINER = TAB_PANEL + "container:content:";

    private static UserTO user;

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);

        // create user with linked account
        String email = "linkedAccount" + RandomStringUtils.randomNumeric(4) + "@syncope.apache.org";
        user = UserITCase.getSampleTO(email);
        String connObjectKeyValue = "uid=" + user.getUsername() + ",ou=People,o=isp";

        LinkedAccountTO account = new LinkedAccountTO.Builder("resource-ldap", connObjectKeyValue).build();
        account.getPlainAttrs().add(new AttrTO.Builder().schema("surname").value("LINKED_SURNAME").build());
        user.getLinkedAccounts().add(account);

        UserService userService = SyncopeConsoleSession.get().getService(UserService.class);
        Response response = userService.create(user, true);
        user = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(user.getKey());
        assertEquals(account.getConnObjectKeyValue(), user.getLinkedAccounts().get(0).getConnObjectKeyValue());
    }

    @AfterEach
    public void cleanUp() {
        SyncopeConsoleSession.get().getService(UserService.class).delete(user.getKey());
    }

    @Test
    public void createLinkedAccountAndMergeWithUser() {

        // Locate and select first user
        TESTER.clickLink("body:realmsLI:realms");
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component component = findComponentByProp("username", CONTAINER
            + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "verdi");
        assertNotNull(component);
        TESTER.executeAjaxEvent(component.getPageRelativePath(), Constants.ON_CLICK);

        // Click action menu to bring up merge window
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
            + "actions:actions:actionRepeater:8:action:action");

        // Add new search clause and construct filter query
        TESTER.executeAjaxEvent(USER_SEARCH_FORM + "content:panelPlus:add", Constants.ON_CLICK);
        DropDownChoice type = (DropDownChoice) TESTER.getComponentFromLastRenderedPage(USER_SEARCH_FORM + "content:view:0:panel:container:type:dropDownChoiceField");
        TESTER.executeAjaxEvent(USER_SEARCH_FORM + "content:view:0:panel:container:type:dropDownChoiceField", Constants.ON_CHANGE);
        type.setModelValue(new String[]{"ATTRIBUTE"});
        type.setDefaultModelObject(SearchClause.Type.ATTRIBUTE);

        TextField property = (TextField) TESTER.getComponentFromLastRenderedPage(USER_SEARCH_FORM + "content:view:0:panel:container:property:textField");
        assertNotNull(property);
        property.setModelValue(new String[]{"username"});

        TextField value = (TextField) TESTER.getComponentFromLastRenderedPage(USER_SEARCH_FORM + "content:view:0:panel:container:value:textField");
        assertNotNull(value);
        value.setModelValue(new String[]{user.getUsername()});

        // Search
        TESTER.executeAjaxEvent(USER_SEARCH_FORM
            + "content:view:0:panel:container:operatorContainer:operator:search", Constants.ON_CLICK);

        // Locate result in data table
        Component comp = findComponentByProp("username", TAB_PANEL + SEARCH_PANEL + RESULT_DATA_TABLE, user.getUsername());
        TESTER.executeAjaxEvent(comp.getPageRelativePath(), Constants.ON_CLICK);
        UserTO userTO = (UserTO) ((OddEvenItem) TESTER.getComponentFromLastRenderedPage(TAB_PANEL + SEARCH_PANEL
            + RESULT_DATA_TABLE + "body:rows:1")).getModel().getObject();
        assertNotNull(userTO);
    }
}
