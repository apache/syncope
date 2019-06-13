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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.SelfPasswordReset;
import org.apache.syncope.client.enduser.pages.MustChangePassword;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.wicket.extensions.markup.html.form.palette.component.Choices;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SelfRegistrationITCase extends AbstractEnduserITCase {

    private static final String WIZARD_FORM = "body:wizard:form";

    @Test
    public void selfCreate() {
        String username = "testUser";

        UTILITY_UI.getTester().startPage(Login.class);
        UTILITY_UI.getTester().assertRenderedPage(Login.class);

        UTILITY_UI.getTester().clickLink("self-registration");

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:username:textField", TextField.class);
        FormTester formTester = UTILITY_UI.getTester().newFormTester(WIZARD_FORM);
        Assertions.assertNotNull(formTester);
        formTester.setValue("view:username:textField", username);
        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        // check required field is correctly set
        UTILITY_UI.getTester().assertNoInfoMessage();
        UTILITY_UI.getTester().assertNoErrorMessage();

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:auxClasses:paletteField:choices", Choices.class);
        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:groupsContainer:groups:form:filter:textField",
                TextField.class);
        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertComponent(UTILITY_UI.findComponentByMarkupId(
                WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "fullname").getPageRelativePath()
                + ":textField",
                TextField.class);
        UTILITY_UI.getTester().assertComponent(UTILITY_UI.findComponentByMarkupId(
                WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "surname").getPageRelativePath()
                + ":textField",
                TextField.class);
        UTILITY_UI.getTester().assertComponent(UTILITY_UI.findComponentByMarkupId(
                WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "userId").getPageRelativePath()
                + ":textField",
                TextField.class);

        formTester = UTILITY_UI.getTester().newFormTester(WIZARD_FORM);
        Assertions.assertNotNull(formTester);
        formTester.setValue(UTILITY_UI.findComponentByMarkupId(WIZARD_FORM
                + ":view:plainSchemas:tabs:0:body:content:schemas",
                "fullname").getPageRelativePath().replace(WIZARD_FORM + ":", StringUtils.EMPTY) + ":textField",
                "User fullname");
        formTester.setValue(UTILITY_UI.findComponentByMarkupId(WIZARD_FORM
                + ":view:plainSchemas:tabs:0:body:content:schemas",
                "surname").getPageRelativePath().replace(WIZARD_FORM + ":", StringUtils.EMPTY) + ":textField",
                "User surname");
        formTester.setValue(UTILITY_UI.
                findComponentByMarkupId(WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "userId").
                getPageRelativePath().replace(WIZARD_FORM + ":", StringUtils.EMPTY) + ":textField",
                "test@email.com");

        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        // check required fields were correctly set
        UTILITY_UI.getTester().assertNoInfoMessage();
        UTILITY_UI.getTester().assertNoErrorMessage();

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM
                + ":view:derSchemas:tabs:0:body:content:schemas:0:panel:textField",
                TextField.class);

        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:virSchemas:tabs:0:body:content:schemas:0:panel:"
                + "multiValueContainer:innerForm:content:field-label",
                Label.class);

        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:resources:paletteField:choices", Choices.class);

        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:finish", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertRenderedPage(Login.class);
        UTILITY_UI.getTester().assertComponent("login:username", TextField.class);

        Assertions.assertFalse(userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo(username).query()).
                build()).getResult().isEmpty());

        Assertions.assertNotNull(userService.read(username));

        UTILITY_UI.getTester().cleanupFeedbackMessages();

        // cleanup
        userService.delete(username);
    }

    @Test
    public void selfPasswordReset() {
        SecurityQuestionTO question = securityQuestionService.read("887028ea-66fc-41e7-b397-620d7ea6dfbb");
        UserTO selfpwdreset = userService.read("selfpwdreset");
        userService.update(new UserUR.Builder(selfpwdreset.getKey())
                .securityQuestion(new StringReplacePatchItem.Builder()
                        .operation(PatchOperation.ADD_REPLACE)
                        .value(question.getKey())
                        .build())
                .securityAnswer(new StringReplacePatchItem.Builder()
                        .operation(PatchOperation.ADD_REPLACE)
                        .value("ananswer")
                        .build())
                .build());

        final String pwdResetForm = "body:content:selfPwdResetForm";
        UTILITY_UI.getTester().startPage(Login.class);
        UTILITY_UI.getTester().assertRenderedPage(Login.class);

        UTILITY_UI.getTester().clickLink("self-pwd-reset");

        UTILITY_UI.getTester().assertRenderedPage(SelfPasswordReset.class);

        UTILITY_UI.getTester().assertComponent(pwdResetForm + ":selfPwdResetPanel:username", TextField.class);
        UTILITY_UI.getTester().assertComponent(pwdResetForm + ":selfPwdResetPanel:securityQuestion", TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(pwdResetForm);
        Assertions.assertNotNull(formTester);
        // 1. set username to selfpwdreset
        formTester.setValue(UTILITY_UI.findComponentById(pwdResetForm + ":selfPwdResetPanel", "username"),
                "selfpwdreset");
        // 2. check that the question has been populated
        UTILITY_UI.getTester().executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:username", Constants.ON_BLUR);
        UTILITY_UI.getTester().assertModelValue(pwdResetForm + ":selfPwdResetPanel:securityQuestion", question.
                getContent());
        // 3. submit form and receive an error
        formTester = UTILITY_UI.getTester().newFormTester(pwdResetForm);
        Assertions.assertNotNull(formTester);
        UTILITY_UI.getTester().executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:submit", Constants.ON_CLICK);
        UTILITY_UI.getTester().assertErrorMessages("InvalidSecurityAnswer []");
        UTILITY_UI.getTester().cleanupFeedbackMessages();
        // 3.1 set the correct answer
        formTester = UTILITY_UI.getTester().newFormTester(pwdResetForm);
        Assertions.assertNotNull(formTester);
        UTILITY_UI.getTester().assertComponent(pwdResetForm + ":selfPwdResetPanel:securityAnswer", TextField.class);
        formTester.setValue("selfPwdResetPanel:securityAnswer", "ananswer");
        UTILITY_UI.getTester().executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:securityAnswer", Constants.ON_CHANGE);
        UTILITY_UI.getTester().assertComponent(pwdResetForm + ":selfPwdResetPanel:securityAnswer",
                TextField.class);
        // 4. submit form
        UTILITY_UI.getTester().assertNoFeedbackMessage(0);
        UTILITY_UI.getTester().assertNoErrorMessage();
        UTILITY_UI.getTester().assertComponent(pwdResetForm + ":selfPwdResetPanel:submit", Button.class);
        UTILITY_UI.getTester().executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:submit", Constants.ON_CLICK);
        UTILITY_UI.getTester().assertRenderedPage(Login.class);
        UTILITY_UI.getTester().assertComponent("login:username", TextField.class);

        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

    @Test
    public void mustChangePassword() {
        UserTO mustchangepassword = userService.read("mustchangepassword");
        userService.update(new UserUR.Builder(mustchangepassword.getKey())
                .mustChangePassword(new BooleanReplacePatchItem.Builder()
                        .operation(PatchOperation.ADD_REPLACE)
                        .value(Boolean.TRUE).build()).build());

        UTILITY_UI.getTester().startPage(Login.class);
        doLogin("mustchangepassword", "password123");

        UTILITY_UI.getTester().assertRenderedPage(MustChangePassword.class);

        final String changePwdForm = "changePassword";
        UTILITY_UI.getTester().assertComponent(changePwdForm + ":username", TextField.class);
        UTILITY_UI.getTester().assertComponent(changePwdForm + ":password:passwordField", PasswordTextField.class);
        UTILITY_UI.getTester().
                assertComponent(changePwdForm + ":confirmPassword:passwordField", PasswordTextField.class);
        UTILITY_UI.getTester().assertModelValue(changePwdForm + ":username", "mustchangepassword");

        FormTester formTester = UTILITY_UI.getTester().newFormTester(changePwdForm);

        Assertions.assertNotNull(formTester);
        // 1. set new password
        formTester.setValue(UTILITY_UI.findComponentById(changePwdForm + ":password", "passwordField"), "password124");
        // 2. confirm password
        formTester.setValue(UTILITY_UI.findComponentById(changePwdForm + ":confirmPassword", "passwordField"),
                "password124");
        // 3. submit form
        UTILITY_UI.getTester().executeAjaxEvent(changePwdForm + ":submit", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertRenderedPage(Login.class);
        UTILITY_UI.getTester().assertComponent("login:username", TextField.class);

        UTILITY_UI.getTester().cleanupFeedbackMessages();

        doLogin("mustchangepassword", "password124");
        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:username:textField", TextField.class);
    }

    @Test
    public void selfUpdate() {
        String username = "selfupdate";
        String newEmail = "selfupdate@email.com";

        UTILITY_UI.getTester().startPage(Login.class);
        doLogin(username, "password123");

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:username:textField", TextField.class);
        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:auxClasses:paletteField:choices", Choices.class);
        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:groupsContainer:groups:form:filter:textField",
                TextField.class);
        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertComponent(UTILITY_UI.findComponentByMarkupId(
                WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "fullname").getPageRelativePath()
                + ":textField",
                TextField.class);
        UTILITY_UI.getTester().assertComponent(UTILITY_UI.findComponentByMarkupId(
                WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "surname").getPageRelativePath()
                + ":textField",
                TextField.class);
        UTILITY_UI.getTester().assertComponent(UTILITY_UI.findComponentByMarkupId(
                WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "userId").getPageRelativePath()
                + ":textField",
                TextField.class);

        FormTester formTester = UTILITY_UI.getTester().newFormTester(WIZARD_FORM);
        Assertions.assertNotNull(formTester);
        UTILITY_UI.getTester().assertComponent(UTILITY_UI.findComponentByMarkupId(
                WIZARD_FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "email").getPageRelativePath()
                + ":textField",
                TextField.class);
        formTester.setValue(UTILITY_UI.findComponentByMarkupId(WIZARD_FORM
                + ":view:plainSchemas:tabs:0:body:content:schemas",
                "email").getPageRelativePath().replace(WIZARD_FORM + ":", StringUtils.EMPTY) + ":textField",
                newEmail);

        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);

        // check required fields were correctly set
        UTILITY_UI.getTester().assertNoInfoMessage();
        UTILITY_UI.getTester().assertNoErrorMessage();

        UTILITY_UI.getTester().assertComponent(WIZARD_FORM
                + ":view:derSchemas:tabs:0:body:content:schemas:0:panel:textField",
                TextField.class);

        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:virSchemas:tabs:0:body:content:schemas:0:panel:"
                + "multiValueContainer:innerForm:content:field-label",
                Label.class);
        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:next", Constants.ON_CLICK);
        UTILITY_UI.getTester().assertComponent(WIZARD_FORM + ":view:resources:paletteField:choices", Choices.class);

        UTILITY_UI.getTester().executeAjaxEvent(WIZARD_FORM + ":buttons:finish", Constants.ON_CLICK);

        UTILITY_UI.getTester().assertRenderedPage(Login.class);
        UTILITY_UI.getTester().assertComponent("login:username", TextField.class);

        Assertions.assertEquals("active", userService.read(username).getStatus());
        Assertions.assertEquals(newEmail, userService.read(username).getPlainAttr("email").get().getValues().get(0));

        UTILITY_UI.getTester().cleanupFeedbackMessages();
    }

}
