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
package org.apache.syncope.client.console.pages;

import static org.apache.wicket.Component.ENABLE;

import org.apache.syncope.client.console.panels.RoleSearchResultPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.role.RoleHandler;
import org.apache.syncope.client.console.wizards.role.RoleWizardBuilder;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Roles extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    public Roles(final PageParameters parameters) {
        super(parameters);

        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.add(new Label("header", getString("header_title", new Model<>(), "Roles")));
        content.setOutputMarkupId(true);
        add(content);

        final WizardMgtPanel<RoleHandler> roleSearchResultPanel
                = new RoleSearchResultPanel.Builder(getPageReference()) {

            private static final long serialVersionUID = -5960765294082359003L;

        }.addNewItemPanelBuilder(new RoleWizardBuilder(BaseModal.CONTENT_ID, new RoleTO(), getPageReference()), false).
                build("roles");

        final AjaxLink<RoleTO> createLink = new AjaxLink<RoleTO>("add") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(roleSearchResultPanel, Broadcast.EXACT, new AjaxWizard.NewItemActionEvent<RoleTO>(null, target));
            }
        };

        content.add(createLink);
        MetaDataRoleAuthorizationStrategy.authorize(createLink, ENABLE, StandardEntitlement.ROLE_CREATE);

        content.add(roleSearchResultPanel);
    }
}
