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
package org.syncope.console.pages.panels;

import java.util.Date;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.syncope.client.to.AbstractAttributableTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.wicket.markup.html.form.AjaxNumberFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;

public class UserDetailsPanel extends Panel {

    private static final long serialVersionUID = 6592027822510220463L;

    public <T extends AbstractAttributableTO> UserDetailsPanel(
            final String id,
            final UserTO userTO,
            final Form form,
            final boolean resetPassword,
            final boolean templateMode) {

        super(id);

        // ------------------------
        // Username
        // ------------------------
        final FieldPanel username = new AjaxTextFieldPanel(
                "username", "username",
                new PropertyModel<String>(userTO, "username"), true);
        if (!templateMode) {
            username.addRequiredLabel();
        }
        add(username);
        // ------------------------

        // ------------------------
        // Password
        // ------------------------
        final FieldPanel password;
        final Label confirmPasswordLabel = new Label("confirmPasswordLabel",
                getString("confirmPassword"));
        final FieldPanel confirmPassword;
        if (templateMode) {
            password = new AjaxTextFieldPanel("password", "password",
                    new PropertyModel<String>(userTO, "password"), true);

            confirmPasswordLabel.setVisible(false);
            confirmPassword = new AjaxTextFieldPanel("confirmPassword",
                    "confirmPassword", new Model<String>(), false);
            confirmPassword.setEnabled(false);
            confirmPassword.setVisible(false);
        } else {
            password = new AjaxPasswordFieldPanel("password", "password",
                    new PropertyModel<String>(userTO, "password"), true);
            password.setRequired(userTO.getId() == 0);
            ((PasswordTextField) password.getField()).setResetPassword(
                    resetPassword);

            confirmPassword = new AjaxPasswordFieldPanel("confirmPassword",
                    "confirmPassword", new Model<String>(), true);
            if (!resetPassword) {
                confirmPassword.getField().setModelObject(
                        userTO.getPassword());
            }
            confirmPassword.setRequired(userTO.getId() == 0);
            ((PasswordTextField) confirmPassword.getField()).setResetPassword(
                    resetPassword);

            form.add(new EqualPasswordInputValidator(
                    password.getField(), confirmPassword.getField()));
        }
        add(password);
        add(confirmPasswordLabel);
        add(confirmPassword);

        final WebMarkupContainer mandatoryPassword =
                new WebMarkupContainer("mandatory_pwd");
        mandatoryPassword.add(new Behavior() {

            private static final long serialVersionUID =
                    1469628524240283489L;

            @Override
            public void onComponentTag(
                    final Component component, final ComponentTag tag) {

                if (userTO.getId() > 0) {
                    tag.put("style", "display:none;");
                }
            }
        });

        add(mandatoryPassword);
        // ------------------------

        // ------------------------
        // Status
        // ------------------------
        final AjaxTextFieldPanel status = new AjaxTextFieldPanel(
                "status",
                "status",
                new Model<String>(userTO.getStatus()),
                false);

        status.setReadOnly(true);
        add(status);
        // ------------------------

        // ------------------------
        // Creation date
        // ------------------------
        final DateTimeFieldPanel creationDate = new DateTimeFieldPanel(
                "creationDate",
                "creationDate",
                new Model<Date>(userTO.getCreationDate()),
                false);

        creationDate.setReadOnly(true);
        add(creationDate);
        // ------------------------

        // ------------------------
        // Change password date
        // ------------------------
        final DateTimeFieldPanel changePwdDate = new DateTimeFieldPanel(
                "changePwdDate",
                "changePwdDate",
                new Model<Date>(userTO.getChangePwdDate()),
                false);

        changePwdDate.setReadOnly(true);
        add(changePwdDate);
        // ------------------------

        // ------------------------
        // Last login date
        // ------------------------
        final DateTimeFieldPanel lastLoginDate = new DateTimeFieldPanel(
                "lastLoginDate",
                "lastLoginDate",
                new Model<Date>(userTO.getLastLoginDate()),
                false);

        lastLoginDate.setReadOnly(true);
        add(lastLoginDate);
        // ------------------------

        // ------------------------
        // Failed logins
        // ------------------------
        final AjaxNumberFieldPanel failedLogins = new AjaxNumberFieldPanel(
                "failedLogins",
                "failedLogins",
                new Model<Number>(userTO.getFailedLogins()),
                false);

        failedLogins.setReadOnly(true);
        add(failedLogins);
        // ------------------------

        // ------------------------
        // Token
        // ------------------------
        final AjaxTextFieldPanel token = new AjaxTextFieldPanel(
                "token",
                "token",
                new Model<String>(userTO.getToken()),
                false);

        token.setReadOnly(true);
        add(token);
        // ------------------------

        // ------------------------
        // Token expire time
        // ------------------------
        final DateTimeFieldPanel tokenExpireTime = new DateTimeFieldPanel(
                "tokenExpireTime",
                "tokenExpireTime",
                new Model<Date>(userTO.getTokenExpireTime()),
                false);

        tokenExpireTime.setReadOnly(true);
        add(tokenExpireTime);
        // ------------------------
    }
}
