/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.pages;

import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.enduser.panels.ChangePasswordPanel;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class AbstractChangePassword extends BasePage {

    private static final long serialVersionUID = 5889157642852559004L;

    private static final String CHANGE_PASSWORD = "page.changePassword";

    public AbstractChangePassword(final PageParameters parameters) {
        super(parameters, CHANGE_PASSWORD);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);
        
        ChangePasswordPanel changePasswordPanel = getPasswordPanel();
        content.add(changePasswordPanel);
        content.add(new AttributeModifier("style", "height: \"100%\""));
    }

    protected ChangePasswordPanel getPasswordPanel() {
        ChangePasswordPanel changePasswordPanel = new ChangePasswordPanel("changePasswordPanel", notificationPanel) {

            private static final long serialVersionUID = 5195544218030499386L;

            @Override
            protected void doSubmit(final AjaxRequestTarget target, final AjaxPasswordFieldPanel passwordField) {
                doPwdSubmit(target, passwordField);
            }

            @Override
            protected void doCancel() {
                doPwdCancel();
            }

            @Override
            protected UserTO getLoggedUser() {
                return getPwdLoggedUser();
            }
            };

        changePasswordPanel.setOutputMarkupId(true);
        return changePasswordPanel;
    }

    protected abstract void doPwdSubmit(AjaxRequestTarget target, AjaxPasswordFieldPanel passwordField);

    protected abstract void doPwdCancel();

    protected abstract UserTO getPwdLoggedUser();
}
