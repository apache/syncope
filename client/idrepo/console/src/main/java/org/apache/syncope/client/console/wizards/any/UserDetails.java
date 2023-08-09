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
package org.apache.syncope.client.console.wizards.any;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.any.PasswordPanel;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class UserDetails extends Details<UserTO> {

    private static final long serialVersionUID = 6592027822510220463L;

    public UserDetails(
            final UserWrapper wrapper,
            final boolean templateMode,
            final boolean includeStatusPanel,
            final boolean showPasswordManagement,
            final PageReference pageRef) {

        super(wrapper, templateMode, includeStatusPanel, pageRef);

        UserTO userTO = wrapper.getInnerObject();

        // ------------------------
        // Username
        // ------------------------
        AjaxTextFieldPanel username = new AjaxTextFieldPanel(
                Constants.USERNAME_FIELD_NAME, Constants.USERNAME_FIELD_NAME,
                new PropertyModel<>(userTO, Constants.USERNAME_FIELD_NAME), false);

        if (wrapper.getPreviousUserTO() != null && StringUtils.compare(
                wrapper.getPreviousUserTO().getUsername(), wrapper.getInnerObject().getUsername()) != 0) {

            username.showExternAction(new LabelInfo("externalAction", wrapper.getPreviousUserTO().getUsername()));
        }

        if (templateMode) {
            username.enableJexlHelp();
        } else {
            username.addRequiredLabel();
        }
        add(username);
        // ------------------------

        // ------------------------
        // mustChangePassword
        // ------------------------
        AjaxCheckBoxPanel mustChangePassword = new AjaxCheckBoxPanel(
                "mustChangePassword", "mustChangePassword", new PropertyModel<>(userTO, "mustChangePassword"));

        add(mustChangePassword.setOutputMarkupPlaceholderTag(true).setVisible(templateMode));
        // ------------------------

        // ------------------------
        // Password
        // ------------------------
        Model<Integer> model = Model.of(-1);

        Accordion accordion = new Accordion("accordionPanel", List.of(
                new AbstractTab(new ResourceModel("password.change", "Change password")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public Panel getPanel(final String panelId) {
                EditUserPasswordPanel panel = new EditUserPasswordPanel(panelId, wrapper, templateMode);
                panel.setEnabled(model.getObject() >= 0);
                return panel;
            }
        }), model) {

            private static final long serialVersionUID = -2898628183677758699L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Accordion.State state) {
                return new AjaxLink<Integer>(markupId) {

                    private static final long serialVersionUID = 7021195294339489084L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                        Component passwordPanel = getParent().get("body:content");
                        passwordPanel.setEnabled(model.getObject() >= 0);
                        target.add(passwordPanel);
                    }
                }.setBody(new ResourceModel("password.change", "Change password ..."));
            }
        };

        accordion.setOutputMarkupId(true);
        accordion.setVisible(showPasswordManagement);
        add(accordion);
        // ------------------------
    }

    @Override
    protected AnnotatedBeanPanel getGeneralStatusInformation(final String id, final UserTO anyTO) {
        return new UserInformationPanel(id, anyTO);
    }

    public static class EditUserPasswordPanel extends Panel {

        private static final long serialVersionUID = -8198836979773590078L;

        public EditUserPasswordPanel(final String id, final UserWrapper wrapper, final boolean templateMode) {
            super(id);
            setOutputMarkupId(true);
            add(new Label("warning", new ResourceModel("password.change.warning")));
            add(new PasswordPanel("passwordPanel", wrapper, false, templateMode));
        }
    }
}
