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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.core.UserITCase;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LinkedAccountsITCase extends AbstractConsoleITCase {

    private static final String TAB_PANEL = "body:content:body:container:content:tabbedPanel:panel:searchResult:";

    private static final String RESULT_DATA_TABLE =
            "searchResult:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:";

    private static final String RESOURCES_DATA_TABLE =
            "view:resources:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:";

    private static final String SELECT_USER_ACTION = "searchResult:outerObjectsRepeater:1:outer:container:content:"
            + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action";

    private static final String SELECT_RESOURCE_ACTION =
            "view:resources:outerObjectsRepeater:1:outer:container:content:"
            + "togglePanelContainer:container:actions:actions:actionRepeater:0:action:action";

    private static final String PARENT_FORM = "outerObjectsRepeater:2:outer:form:";

    private static final String FORM = PARENT_FORM + "content:form:";

    private static final String SEARCH_PANEL = FORM + "view:ownerContainer:search:";

    private static final String USER_SEARCH_PANEL = SEARCH_PANEL + "usersearch:";

    private static final String USER_SEARCH_FORM = TAB_PANEL + USER_SEARCH_PANEL
            + "searchFormContainer:search:multiValueContainer:innerForm:";

    private static final String CONTAINER = TAB_PANEL + "container:content:";

    private static UserTO USER;

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);

        // create user with linked account
        String email = "linkedAccount" + RandomStringUtils.insecure().nextNumeric(4) + "@syncope.apache.org";
        UserCR userCR = UserITCase.getSample(email);
        String connObjectKeyValue = "uid=" + userCR.getUsername() + ",ou=People,o=isp";

        LinkedAccountTO account = new LinkedAccountTO.Builder("resource-ldap", connObjectKeyValue).build();
        account.getPlainAttrs().add(new Attr.Builder("surname").value("LINKED_SURNAME").build());
        userCR.getLinkedAccounts().add(account);

        UserService userService = SyncopeConsoleSession.get().getService(UserService.class);
        Response response = userService.create(userCR);
        USER = response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(USER.getKey());
        assertEquals(account.getConnObjectKeyValue(), USER.getLinkedAccounts().getFirst().getConnObjectKeyValue());
    }

    @AfterEach
    public void cleanUp() {
        try {
            SyncopeConsoleSession.get().getService(UserService.class).delete(USER.getKey());
        } catch (SyncopeClientException e) {
            if (e.getType() != ClientExceptionType.NotFound) {
                throw e;
            }
        }
    }

    @Test
    public void createLinkedAccountAndMergeWithUser() {
        // Locate and select first user
        TESTER.clickLink("body:realmsLI:realms", false);
        TESTER.clickLink("body:content:body:container:content:tabbedPanel:tabs-container:tabs:1:link");

        Component verdiUserComponent = findComponentByProp("username", CONTAINER
                + ":searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable", "verdi");
        assertNotNull(verdiUserComponent);
        TESTER.executeAjaxEvent(verdiUserComponent.getPageRelativePath(), Constants.ON_CLICK);

        // Click action menu to bring up merge window
        TESTER.clickLink(TAB_PANEL + "outerObjectsRepeater:1:outer:container:content:togglePanelContainer:container:"
                + "actions:actions:actionRepeater:6:action:action");
        // Search for user
        TESTER.executeAjaxEvent(USER_SEARCH_FORM + "content:panelPlus:add", Constants.ON_CLICK);
        FormTester formTester = TESTER.newFormTester(USER_SEARCH_FORM);

        DropDownChoice<?> type = (DropDownChoice<?>) TESTER.getComponentFromLastRenderedPage(USER_SEARCH_FORM
                + "content:view:0:panel:container:type:dropDownChoiceField");
        TESTER.executeAjaxEvent(USER_SEARCH_FORM + "content:view:0:panel:container:type:dropDownChoiceField",
                Constants.ON_CHANGE);
        type.setModelValue(new String[] { "ATTRIBUTE" });
        type.setDefaultModelObject(SearchClause.Type.ATTRIBUTE);

        formTester.setValue("content:view:0:panel:container:property:textField", "username");
        TESTER.executeAjaxEvent(formTester.getForm().
                get("content:view:0:panel:container:property:textField"), Constants.ON_KEYDOWN);
        formTester.setValue("content:view:0:panel:container:value:textField", USER.getUsername());
        TESTER.executeAjaxEvent(formTester.getForm().
                get("content:view:0:panel:container:value:textField"), Constants.ON_KEYDOWN);

        TESTER.cleanupFeedbackMessages();
        Component searchButton = formTester.getForm().
                get("content:view:0:panel:container:operatorContainer:operator:search");
        TESTER.clickLink(searchButton);
        TESTER.executeAjaxEvent(searchButton.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.assertNoErrorMessage();

        // Locate result in data table
        Component comp = findComponentByProp("username", TAB_PANEL + SEARCH_PANEL + RESULT_DATA_TABLE, USER.
                getUsername());
        TESTER.executeAjaxEvent(comp.getPageRelativePath(), Constants.ON_CLICK);

        // Select user
        TESTER.clickLink(TAB_PANEL + SEARCH_PANEL + SELECT_USER_ACTION);

        // move onto the next panel
        TESTER.getComponentFromLastRenderedPage(TAB_PANEL + FORM + "view").setEnabled(false);
        formTester = TESTER.newFormTester(TAB_PANEL + FORM);
        formTester.submit("buttons:next");

        // Select a resource
        comp = findComponentByProp("key", TAB_PANEL + FORM + RESOURCES_DATA_TABLE + "body:rows", "resource-ldap");
        assertNotNull(comp);
        TESTER.executeAjaxEvent(comp.getPageRelativePath(), Constants.ON_CLICK);
        TESTER.clickLink(TAB_PANEL + FORM + SELECT_RESOURCE_ACTION);

        // move onto the next panel
        TESTER.getComponentFromLastRenderedPage(TAB_PANEL + FORM + "view").setEnabled(false);
        formTester = TESTER.newFormTester(TAB_PANEL + FORM);
        formTester.submit("buttons:next");

        // Finish merge
        TESTER.getComponentFromLastRenderedPage(TAB_PANEL + FORM + "view").setEnabled(false);
        formTester = TESTER.newFormTester(TAB_PANEL + FORM);
        formTester.submit("buttons:finish");

        UserService userService = SyncopeConsoleSession.get().getService(UserService.class);

        // User must have been deleted after the merge
        try {
            userService.read(USER.getKey());
            fail("User must have been deleted; expect an exception here");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
        // User must include merged accounts now
        UserTO verdi = userService.read("verdi");
        assertFalse(verdi.getLinkedAccounts().isEmpty());
    }
}
