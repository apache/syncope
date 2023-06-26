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

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.AjaxBootstrapTabbedPanel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.panels.CommandsPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.tasks.MacroTaskDirectoryPanel;
import org.apache.syncope.client.console.tasks.SchedTaskDirectoryPanel;
import org.apache.syncope.client.console.tasks.TaskExecutionDetails;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Engagements extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    @SpringBean
    protected TaskRestClient taskRestClient;

    public Engagements(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.setMarkupId("engagements");
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", buildTabList()));
        body.add(content);
    }

    protected List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(new ResourceModel("schedTasks")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                MultilevelPanel mlp = new MultilevelPanel(panelId);
                mlp.setFirstLevel(new SchedTaskDirectoryPanel<>(MultilevelPanel.FIRST_LEVEL_ID, taskRestClient,
                        null, null, TaskType.SCHEDULED, new SchedTaskTO(), getPageReference(), true) {

                    private static final long serialVersionUID = -2195387360323687302L;

                    @Override
                    protected void viewTaskExecs(final SchedTaskTO taskTO, final AjaxRequestTarget target) {
                        mlp.next(
                                new StringResourceModel(
                                        "task.view", this, new Model<>(Pair.of(null, taskTO))).getObject(),
                                new TaskExecutionDetails<>(taskTO, pageRef),
                                target);
                    }
                });
                return mlp;
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("commands")) {

            private static final long serialVersionUID = -6815067322125799251L;

            @Override
            public Panel getPanel(final String panelId) {
                return new CommandsPanel(panelId, getPageReference());
            }
        });

        tabs.add(new AbstractTab(new ResourceModel("macroTasks")) {

            private static final long serialVersionUID = 5211692813425391144L;

            @Override
            public Panel getPanel(final String panelId) {
                MultilevelPanel mlp = new MultilevelPanel(panelId);
                mlp.setFirstLevel(new MacroTaskDirectoryPanel(taskRestClient, mlp, getPageReference()));
                return mlp;
            }
        });

        return tabs;
    }
}
