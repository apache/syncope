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

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.Collapsible;
import java.util.Collections;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.JexlHelpUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class UserDetails extends WizardStep {

    private static final long serialVersionUID = 6592027822510220463L;

    private static final String PASSWORD_CONTENT_PATH = "tabs:0:body:content";

    public UserDetails(final UserTO userTO, final boolean resetPassword, final boolean templateMode) {
        // ------------------------
        // Username
        // ------------------------
        final FieldPanel<String> username = new AjaxTextFieldPanel(
                "username", "username", new PropertyModel<String>(userTO, "username"), false);

        final WebMarkupContainer jexlHelp = JexlHelpUtils.getJexlHelpWebContainer("usernameJexlHelp");

        final AjaxLink<?> questionMarkJexlHelp = JexlHelpUtils.getAjaxLink(jexlHelp, "usernameQuestionMarkJexlHelp");
        add(questionMarkJexlHelp);
        questionMarkJexlHelp.add(jexlHelp);

        if (!templateMode) {
            username.addRequiredLabel();
            questionMarkJexlHelp.setVisible(false);
        }
        add(username);
        // ------------------------

        // ------------------------
        // Password
        // ------------------------
        final Model<Integer> model = Model.of(-1);

        final Collapsible collapsible = new Collapsible("collapsePanel", Collections.<ITab>singletonList(
                new AbstractTab(new ResourceModel("password.change", "Change password")) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public Panel getPanel(final String panelId) {
                        final PasswordPanel panel = new PasswordPanel(panelId, userTO, resetPassword, templateMode);
                        panel.setEnabled(model.getObject() >= 0);
                        return panel;
                    }
                }
        ), model) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Collapsible.State state) {
                return super.newTitle(markupId, tab, state).add(new AjaxEventBehavior(Constants.ON_CLICK) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                        final Component passwordPanel = get(PASSWORD_CONTENT_PATH);
                        passwordPanel.setEnabled(model.getObject() >= 0);
                        target.add(passwordPanel);
                    }
                });
            }

        };

        collapsible.setOutputMarkupId(true);
        add(collapsible);
        // ------------------------
    }
}
