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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.pages.Dashboard;
import org.apache.syncope.client.enduser.pages.EditUser;
import org.apache.syncope.client.enduser.pages.Login;
import org.apache.syncope.client.enduser.pages.MustChangePassword;
import org.apache.syncope.client.enduser.pages.SelfPasswordReset;
import org.apache.syncope.client.enduser.pages.SelfRegistration;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.fit.FlowableDetector;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.form.palette.component.Choices;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EnduserITCase extends AbstractEnduserITCase {

    private static final String FORM = "body:contentWrapper:content:selfRegistrationPanel:form";

    private Map<String, String> getConfiguredSecurityHeaders() throws IOException {
        Map<String, String> securityHeaders = new HashMap<>();

        @SuppressWarnings("unchecked")
        Enumeration<String> propNames = (Enumeration<String>) PROPS.propertyNames();
        while (propNames.hasMoreElements()) {
            String name = propNames.nextElement();
            if (name.startsWith("security.headers.")) {
                securityHeaders.put(StringUtils.substringAfter(name, "security.headers."), PROPS.getProperty(name));
            }
        }

        return securityHeaders;
    }

    @Test
    public void login() throws IOException {
        Map<String, String> securityHeaders = getConfiguredSecurityHeaders();
        assertEquals(4, securityHeaders.size());

        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        TESTER.assertComponent("login:username", TextField.class);
        TESTER.assertComponent("login:password", TextField.class);

        TESTER.assertComponent("login:language", BootstrapSelect.class);
        TESTER.assertComponent("login:domain", BootstrapSelect.class);

        TESTER.assertComponent("login:submit", AjaxButton.class);

        TESTER.assertComponent("self-pwd-reset", BookmarkablePageLink.class);
        TESTER.assertComponent("self-registration", BookmarkablePageLink.class);

        doLogin("bellini", "password");

        TESTER.assertNoErrorMessage();
        TESTER.assertRenderedPage(Dashboard.class);
        securityHeaders.forEach((key, value) -> assertEquals(value, TESTER.getLastResponse().getHeader(key)));
    }

    @Test
    public void selfCreate() {
        String username = "testUser";

        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);
        TESTER.clickLink("self-registration");
        TESTER.assertRenderedPage(SelfRegistration.class);
        TESTER.assertComponent(FORM + ":userDetailsPanelCard:contentPanel:username:textField",
                AjaxTextFieldPanel.class);
        FormTester formTester = TESTER.newFormTester(FORM);
        assertNotNull(formTester);
        formTester.setValue("userDetailsPanelCard:contentPanel:textField", username);

        // check required field is correctly set
        TESTER.assertNoInfoMessage();
        TESTER.assertNoErrorMessage();

        TESTER.assertComponent(FORM + ":view:auxClasses:paletteField:choices", Choices.class);
        TESTER.executeAjaxEvent(FORM + ":buttons:next", Constants.ON_CLICK);

        TESTER.assertComponent(FORM + ":view:groupsContainer:groups:form:filter:textField",
                TextField.class);
        TESTER.executeAjaxEvent(FORM + ":buttons:next", Constants.ON_CLICK);

        TESTER.assertComponent(findComponentByMarkupId(
                FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "fullname").getPageRelativePath()
                        + ":textField",
                TextField.class);
        TESTER.assertComponent(findComponentByMarkupId(
                FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "surname").getPageRelativePath()
                        + ":textField",
                TextField.class);
        TESTER.assertComponent(findComponentByMarkupId(
                FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "userId").getPageRelativePath()
                        + ":textField",
                TextField.class);

        formTester = TESTER.newFormTester(FORM);
        assertNotNull(formTester);
        formTester.setValue(findComponentByMarkupId(FORM
                        + ":view:plainSchemas:tabs:0:body:content:schemas",
                "fullname").getPageRelativePath().replace(FORM + ':', StringUtils.EMPTY) + ":textField",
                "User fullname");
        formTester.setValue(findComponentByMarkupId(FORM
                        + ":view:plainSchemas:tabs:0:body:content:schemas",
                "surname").getPageRelativePath().replace(FORM + ':', StringUtils.EMPTY) + ":textField",
                "User surname");
        formTester.setValue(
                findComponentByMarkupId(FORM + ":view:plainSchemas:tabs:0:body:content:schemas", "userId").
                        getPageRelativePath().replace(FORM + ':', StringUtils.EMPTY) + ":textField",
                "test@email.com");

        TESTER.executeAjaxEvent(FORM + ":buttons:next", Constants.ON_CLICK);

        // check required fields were correctly set
        TESTER.assertNoInfoMessage();
        TESTER.assertNoErrorMessage();

        TESTER.assertComponent(FORM
                        + ":view:derSchemas:tabs:0:body:content:schemas:0:panel:textField",
                TextField.class);

        TESTER.executeAjaxEvent(FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(FORM + ":view:virSchemas:tabs:0:body:content:schemas:0:panel:"
                        + "multiValueContainer:innerForm:content:field-label",
                Label.class);

        TESTER.executeAjaxEvent(FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(FORM + ":view:resources:paletteField:choices", Choices.class);

        TESTER.executeAjaxEvent(FORM + ":buttons:finish", Constants.ON_CLICK);

        TESTER.assertRenderedPage(Login.class);
        TESTER.assertComponent("login:username", TextField.class);

        assertFalse(userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo(username).query()).
                build()).getResult().isEmpty());

        assertNotNull(userService.read(username));

        TESTER.cleanupFeedbackMessages();

        // cleanup
        userService.delete(username);
    }

    @Test
    public void selfPasswordReset() {
        SecurityQuestionTO question = securityQuestionService.read("887028ea-66fc-41e7-b397-620d7ea6dfbb");

        UserTO selfpwdreset = userService.read("selfpwdreset");
        userService.update(new UserUR.Builder(selfpwdreset.getKey()).
                securityQuestion(new StringReplacePatchItem.Builder().value(question.getKey()).build()).
                securityAnswer(new StringReplacePatchItem.Builder().value("ananswer").build()).
                build());

        String pwdResetForm = "body:content:selfPwdResetForm";
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        TESTER.clickLink("self-pwd-reset");

        TESTER.assertRenderedPage(SelfPasswordReset.class);

        TESTER.assertComponent(pwdResetForm + ":selfPwdResetPanel:username", TextField.class);
        TESTER.assertComponent(pwdResetForm + ":selfPwdResetPanel:securityQuestion", TextField.class);

        FormTester formTester = TESTER.newFormTester(pwdResetForm);
        assertNotNull(formTester);

        // 1. set username to selfpwdreset
        formTester.setValue(findComponentById(pwdResetForm + ":selfPwdResetPanel", "username"), "selfpwdreset");

        // 2. check that the question has been populated
        TESTER.executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:username", Constants.ON_BLUR);
        TESTER.assertModelValue(pwdResetForm + ":selfPwdResetPanel:securityQuestion", question.getContent());

        // 3. submit form and receive an error
        formTester = TESTER.newFormTester(pwdResetForm);
        assertNotNull(formTester);
        TESTER.executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:submit", Constants.ON_CLICK);
        TESTER.assertErrorMessages("Invalid security answer");
        TESTER.cleanupFeedbackMessages();

        // 3.1 set the correct answer
        formTester = TESTER.newFormTester(pwdResetForm);
        assertNotNull(formTester);
        TESTER.assertComponent(pwdResetForm + ":selfPwdResetPanel:securityAnswer", TextField.class);
        formTester.setValue("selfPwdResetPanel:securityAnswer", "ananswer");
        TESTER.executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:securityAnswer", Constants.ON_CHANGE);
        TESTER.assertComponent(pwdResetForm + ":selfPwdResetPanel:securityAnswer", TextField.class);

        // 4. submit form
        TESTER.assertNoFeedbackMessage(0);
        TESTER.assertNoErrorMessage();
        TESTER.assertComponent(pwdResetForm + ":selfPwdResetPanel:submit", Button.class);
        TESTER.executeAjaxEvent(pwdResetForm + ":selfPwdResetPanel:submit", Constants.ON_CLICK);
        TESTER.assertRenderedPage(Login.class);
        TESTER.assertComponent("login:username", TextField.class);

        TESTER.cleanupFeedbackMessages();
    }

    @Test
    public void mustChangePassword() {
        UserTO mustchangepassword = userService.read("mustchangepassword");
        userService.update(new UserUR.Builder(mustchangepassword.getKey()).
                mustChangePassword(new BooleanReplacePatchItem.Builder().value(Boolean.TRUE).build()).build());

        TESTER.startPage(Login.class);
        doLogin("mustchangepassword", "password123");

        TESTER.assertRenderedPage(MustChangePassword.class);

        final String changePwdForm = "body:contentWrapper:content:changePasswordPanel:changePassword";
        TESTER.assertComponent(changePwdForm + ":password", AjaxPasswordFieldPanel.class);
        TESTER.assertComponent(changePwdForm + ":confirmPassword", AjaxPasswordFieldPanel.class);

        FormTester formTester = TESTER.newFormTester(changePwdForm);

        assertNotNull(formTester);
        // 1. set new password
        formTester.setValue(findComponentById(changePwdForm + ":password", "passwordField"), "password124");
        // 2. confirm password
        formTester.setValue(findComponentById(changePwdForm + ":confirmPassword", "passwordField"),
                "password124");
        // 3. submit form
        TESTER.executeAjaxEvent(changePwdForm + ":submit", Constants.ON_CLICK);

        TESTER.assertRenderedPage(Login.class);
        TESTER.assertComponent("login:username", TextField.class);

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

        TESTER.clickLink("body:sidebar:profileLI:profileUL:edituserLI:edituser");
        TESTER.assertRenderedPage(EditUser.class);
        FormTester formTester = TESTER.newFormTester("body:contentWrapper:content:editUserPanel:form");
        assertNotNull(formTester);
        TESTER.assertComponent(
                "body:contentWrapper:content:editUserPanel:form:plainAttrsPanelCard:contentPanel" +
                        ":plainSchemas:schemas:4:panel:textField", TextField.class);
        formTester.setValue("body:contentWrapper:content:editUserPanel:form:plainAttrsPanelCard:contentPanel" +
                        ":plainSchemas:schemas:4:panel:textField", newEmail);

        // check required fields were correctly set
        TESTER.assertNoInfoMessage();
        TESTER.assertNoErrorMessage();

        TESTER.assertComponent(FORM
                        + ":view:derSchemas:tabs:0:body:content:schemas:0:panel:textField",
                TextField.class);

        TESTER.executeAjaxEvent(FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(FORM + ":view:virSchemas:tabs:0:body:content:schemas:0:panel:"
                        + "multiValueContainer:innerForm:content:field-label",
                Label.class);
        TESTER.executeAjaxEvent(FORM + ":buttons:next", Constants.ON_CLICK);
        TESTER.assertComponent(FORM + ":view:resources:paletteField:choices", Choices.class);

        TESTER.executeAjaxEvent(FORM + ":buttons:finish", Constants.ON_CLICK);

        TESTER.assertRenderedPage(Login.class);
        TESTER.assertComponent("login:username", TextField.class);

        assertEquals(FlowableDetector.isFlowableEnabledForUserWorkflow(SYNCOPE_SERVICE)
                ? "active" : "created", userService.read(username).getStatus());
        assertEquals(newEmail, userService.read(username).getPlainAttr("email").get().getValues().get(0));

        TESTER.cleanupFeedbackMessages();
    }
}
