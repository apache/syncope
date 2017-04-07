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

import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.WorkflowDirectoryPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Workflow extends BasePage {

    private static final long serialVersionUID = -8781434495150074529L;

    public Workflow(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        body.add(content);

        WebMarkupContainer disabled = new WebMarkupContainer("disabled");
        disabled.setOutputMarkupPlaceholderTag(true);
        content.add(disabled);

        WizardMgtPanel<WorkflowDefinitionTO> workflowsPanel = new WorkflowDirectoryPanel.Builder(getPageReference()) {

            private static final long serialVersionUID = -5960765294082359003L;

        }.disableCheckBoxes().build("workflowsPanel");
        workflowsPanel.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(workflowsPanel, ENABLE, StandardEntitlement.WORKFLOW_DEF_LIST);

        content.add(workflowsPanel);

        if (SyncopeConsoleSession.get().getPlatformInfo().getUserWorkflowAdapter().contains("Activiti")) {
            disabled.setVisible(false);
        } else {
            workflowsPanel.setVisible(false);
        }
    }
}
