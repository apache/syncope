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
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.reports.ReportDirectoryPanel;
import org.apache.syncope.client.console.reports.ReportExecutionDetails;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Reports extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    @SpringBean
    protected ReportRestClient reportRestClient;

    public Reports(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.setMarkupId("reports");
        body.add(content);

        MultilevelPanel reportsPanel = new MultilevelPanel("reportsPanel");
        reportsPanel.setFirstLevel(new ReportDirectoryPanel(reportRestClient, getPageReference()) {

            private static final long serialVersionUID = -2195387360323687302L;

            @Override
            protected void viewReportExecs(final ReportTO reportTO, final AjaxRequestTarget target) {
                reportsPanel.next(
                        new StringResourceModel("report.view", this, new Model<>(reportTO)).getObject(),
                        new ReportExecutionDetails(reportTO, getPageReference()),
                        target);
            }
        });
        content.add(reportsPanel);
    }
}
