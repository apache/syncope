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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.apache.syncope.client.enduser.pages.Dashboard;
import org.apache.syncope.client.enduser.pages.EditUser;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.MustChangePassword;
import org.apache.syncope.client.enduser.pages.SelfResult;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class AuthenticatedITCase extends AbstractEnduserITCase {

    @Test
    public void login() throws IOException {
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        doLogin("bellini", "password");

        TESTER.assertNoErrorMessage();
        TESTER.assertRenderedPage(Dashboard.class);
    }

    @Test
    public void mustChangePassword() {
        UserTO mustchangepassword = USER_SERVICE.read("mustchangepassword");
        USER_SERVICE.update(new UserUR.Builder(mustchangepassword.getKey()).
                mustChangePassword(new BooleanReplacePatchItem.Builder().value(Boolean.TRUE).build()).build());

        TESTER.startPage(Login.class);
        doLogin("mustchangepassword", "password123");

        TESTER.assertRenderedPage(MustChangePassword.class);

        String changePwdForm = "body:contentWrapper:content:changePasswordPanel:changePassword";
        TESTER.assertComponent(changePwdForm + ":password", AjaxPasswordFieldPanel.class);
        TESTER.assertComponent(changePwdForm + ":confirmPassword", AjaxPasswordFieldPanel.class);

        FormTester formTester = TESTER.newFormTester(changePwdForm);

        assertNotNull(formTester);
        // 1. set new password
        formTester.setValue(findComponentById(changePwdForm + ":password", "passwordField"), "password124");
        // 2. confirm password
        formTester.setValue(findComponentById(changePwdForm + ":confirmPassword", "passwordField"), "password124");
        // 3. submit form
        TESTER.executeAjaxEvent(changePwdForm + ":submit", Constants.ON_CLICK);

        TESTER.assertRenderedPage(Login.class);

        TESTER.cleanupFeedbackMessages();

        doLogin("mustchangepassword", "password124");

        TESTER.assertNoErrorMessage();
        TESTER.assertRenderedPage(Dashboard.class);
    }

    @Test
    public void selfUpdate() {
        String username = "selfupdate";
        String newEmail = "selfupdate@email.com";

        TESTER.startPage(Login.class);
        doLogin(username, "password123");

        TESTER.assertComponent("body:contentWrapper:content:userProfileInfo:userProfile", WebMarkupContainer.class);

        TESTER.clickLink("body:sidebar:profileLI:profileUL:edituserLI:edituser", false);
        TESTER.assertRenderedPage(EditUser.class);

        String form = "body:contentWrapper:content:editUserPanel:form";
        FormTester formTester = TESTER.newFormTester(form);
        formTester.setValue(
                "plainAttrsPanelCard:contentPanel:plainSchemas:schemas:5:panel:textField",
                newEmail);

        // check required fields were correctly set
        TESTER.assertNoInfoMessage();
        TESTER.assertNoErrorMessage();

        TESTER.executeAjaxEvent(form + ":submit", Constants.ON_CLICK);

        TESTER.assertRenderedPage(SelfResult.class);

        assertEquals(IS_FLOWABLE_ENABLED
                ? "active" : "created", USER_SERVICE.read(username).getStatus());
        assertEquals(newEmail, USER_SERVICE.read(username).getPlainAttr("email").get().getValues().getFirst());

        TESTER.cleanupFeedbackMessages();
    }
}
