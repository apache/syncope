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
package org.apache.syncope.fit.enduser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.wicket.extensions.markup.html.form.palette.component.Choices;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class SelfRegistrationITCase extends AbstractEnduserITCase {

    private static final String WIZARD_FORM = "body:wizard:form";

    @Test
    public void selfCreate() {
        String username = "testUser";

        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        TESTER.clickLink("self-registration");

        TESTER.assertComponent(WIZARD_FORM + ":view:username:textField", TextField.class);
        FormTester formTester = TESTER.newFormTester(WIZARD_FORM);
        assertNotNull(formTester);
        formTester.setValue("view:username:textField", username);
        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        // check required field is correctly set
        TESTER.assertNoInfoMessage();
        TESTER.assertNoErrorMessage();

        TESTER.assertComponent(WIZARD_FORM + ":view:auxClasses:paletteField:choices", Choices.class);
        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        TESTER.assertComponent(WIZARD_FORM + ":view:groupsContainer:groups:form:filter:textField", TextField.class);
        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        TESTER.assertComponent(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas:6:panel:textField",
                TextField.class);
        TESTER.assertComponent(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas:12:panel:textField",
                TextField.class);
        TESTER.assertComponent(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas:14:panel:textField",
                TextField.class);

        formTester = TESTER.newFormTester(WIZARD_FORM);
        assertNotNull(formTester);
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:6:panel:textField",
                "User fullname");
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:12:panel:textField",
                "User surname");
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:14:panel:textField",
                "test@email.com");

        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        // check required fields were correctly set
        TESTER.assertNoInfoMessage();
        TESTER.assertNoErrorMessage();

        TESTER.assertComponent(WIZARD_FORM + ":view:derSchemas:tabs:0:body:content:schemas:0:panel:textField",
                TextField.class);

        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(WIZARD_FORM + ":view:virSchemas:tabs:0:body:content:schemas:0:panel:"
                + "multiValueContainer:innerForm:content:field-label",
                Label.class);

        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(WIZARD_FORM + ":view:resources:paletteField:choices", Choices.class);

        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:finish", Constants.ON_CLICK);

        TESTER.assertRenderedPage(Login.class);
        TESTER.assertComponent("login:username", TextField.class);

        assertFalse(userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo(username).query()).
                build()).getResult().isEmpty());

        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void selfPasswordReset() {
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        TESTER.clickLink("self-pwd-reset");
    }

    @Test
    public void selfUpdate() {
        String username = "puccini";
        String newEmail = "giacomo.puccini@email.com";

        TESTER.startPage(Login.class);
        doLogin(username, "password");

        TESTER.assertComponent(WIZARD_FORM + ":view:username:textField", TextField.class);
        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        TESTER.assertComponent(WIZARD_FORM + ":view:auxClasses:paletteField:choices", Choices.class);
        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        TESTER.assertComponent(WIZARD_FORM + ":view:groupsContainer:groups:form:filter:textField", TextField.class);
        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        TESTER.assertComponent(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas:6:panel:textField",
                TextField.class);
        TESTER.assertComponent(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas:12:panel:textField",
                TextField.class);
        TESTER.assertComponent(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas:14:panel:textField",
                TextField.class);

        FormTester formTester = TESTER.newFormTester(WIZARD_FORM);
        assertNotNull(formTester);
        TESTER.assertComponent(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas:4:panel:textField",
                TextField.class);
        formTester.setValue("view:plainSchemas:tabs:0:body:content:schemas:4:panel:textField", newEmail);

        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        // check required fields were correctly set
        TESTER.assertNoInfoMessage();
        TESTER.assertNoErrorMessage();

        TESTER.assertComponent(WIZARD_FORM + ":view:derSchemas:tabs:0:body:content:schemas:0:panel:textField",
                TextField.class);

        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(WIZARD_FORM + ":view:virSchemas:tabs:0:body:content:schemas:0:panel:"
                + "multiValueContainer:innerForm:content:field-label",
                Label.class);
        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(WIZARD_FORM + ":view:resources:paletteField:choices", Choices.class);

        TESTER.executeAjaxEvent(WIZARD_FORM + ":buttons:finish", Constants.ON_CLICK);

        TESTER.assertRenderedPage(Login.class);
        TESTER.assertComponent("login:username", TextField.class);

        assertTrue(userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo(username).query()).
                build()).getResult().stream().anyMatch(userTO -> {
                    return userTO.getPlainAttr("email").get().getValues().get(0).equals(newEmail);
                }));

        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void mustChangePassword() {

    }
}
