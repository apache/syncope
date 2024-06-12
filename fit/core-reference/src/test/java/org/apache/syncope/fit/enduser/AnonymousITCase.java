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

import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.SelfPasswordReset;
import org.apache.syncope.client.enduser.pages.SelfRegistration;
import org.apache.syncope.client.enduser.pages.SelfResult;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class AnonymousITCase extends AbstractEnduserITCase {

    @Test
    public void selfCreate() {
        String username = "testUser";

        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        TESTER.clickLink("self-registration");
        TESTER.assertRenderedPage(SelfRegistration.class);

        String form = "body:contentWrapper:content:selfRegistrationPanel:form";
        FormTester formTester = TESTER.newFormTester(form);

        formTester.setValue("userDetailsPanelCard:contentPanel:username:textField", username);

        formTester.setValue(
                "userDetailsPanelCard:contentPanel:password:passwordPanel:passwordInnerForm:password:passwordField",
                "Password123");
        formTester.setValue(
                "userDetailsPanelCard:contentPanel:password:passwordPanel:passwordInnerForm:confirmPassword:"
                + "passwordField",
                "Password123");

        formTester.setValue(
                "plainAttrsPanelCard:contentPanel:plainSchemas:schemas:7:panel:textField",
                "Fullname");

        formTester.setValue(
                "plainAttrsPanelCard:contentPanel:plainSchemas:schemas:13:panel:textField",
                "Surname");

        formTester.setValue(
                "plainAttrsPanelCard:contentPanel:plainSchemas:schemas:15:panel:textField",
                username + "@syncope.apache.org");

        try {
            TESTER.executeAjaxEvent(form + ":submit", Constants.ON_CLICK);

            TESTER.assertRenderedPage(SelfResult.class);

            assertFalse(USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo(username).query()).
                    build()).getResult().isEmpty());

            assertNotNull(USER_SERVICE.read(username));

            TESTER.cleanupFeedbackMessages();
        } finally {
            // cleanup
            try {
                USER_SERVICE.delete(username);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void selfPasswordReset() {
        SecurityQuestionTO question = SECURITY_QUESTION_SERVICE.read("887028ea-66fc-41e7-b397-620d7ea6dfbb");

        UserTO selfpwdreset = USER_SERVICE.read("selfpwdreset");
        USER_SERVICE.update(new UserUR.Builder(selfpwdreset.getKey()).
                securityQuestion(new StringReplacePatchItem.Builder().value(question.getKey()).build()).
                securityAnswer(new StringReplacePatchItem.Builder().value("ananswer").build()).
                build());

        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        TESTER.clickLink("self-pwd-reset");

        TESTER.assertRenderedPage(SelfPasswordReset.class);

        String pwdResetForm = "body:contentWrapper:content:selfPwdResetForm";
        FormTester formTester = TESTER.newFormTester(pwdResetForm);

        // 1. set username to selfpwdreset
        formTester.setValue("selfPasswordResetPanelCard:contentPanel:username", "selfpwdreset");
        TESTER.executeAjaxEvent(
                pwdResetForm + ":selfPasswordResetPanelCard:contentPanel:username", Constants.ON_BLUR);

        // 2. check that the question has been populated
        TESTER.assertModelValue(
                pwdResetForm + ":selfPasswordResetPanelCard:contentPanel:securityQuestion",
                question.getContent());

        // 3. submit form and receive an error
        TESTER.executeAjaxEvent(pwdResetForm + ":submit", Constants.ON_CLICK);
        TESTER.assertErrorMessages("Invalid Security Answer");
        TESTER.cleanupFeedbackMessages();

        // 3.1 set the correct answer
        formTester = TESTER.newFormTester(pwdResetForm);
        formTester.setValue("selfPasswordResetPanelCard:contentPanel:username", "selfpwdreset");
        TESTER.executeAjaxEvent(
                pwdResetForm + ":selfPasswordResetPanelCard:contentPanel:username", Constants.ON_BLUR);
        TESTER.assertModelValue(
                pwdResetForm + ":selfPasswordResetPanelCard:contentPanel:securityQuestion",
                question.getContent());
        formTester.setValue("selfPasswordResetPanelCard:contentPanel:securityAnswer", "ananswer");
        TESTER.executeAjaxEvent(
                pwdResetForm + ":selfPasswordResetPanelCard:contentPanel:securityAnswer", Constants.ON_CHANGE);

        // 4. submit form
        TESTER.executeAjaxEvent(pwdResetForm + ":submit", Constants.ON_CLICK);

        TESTER.assertRenderedPage(SelfResult.class);

        TESTER.cleanupFeedbackMessages();
    }
}
