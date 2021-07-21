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
package org.apache.syncope.client.console.reports;

import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.ExecutionRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.tasks.ExecutionsDirectoryPanel;
import org.apache.syncope.client.console.wicket.ajax.form.AjaxDownloadBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * Modal window with report executions.
 */
public class ReportExecutionDetails extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = -4110576026663173545L;

    public ReportExecutionDetails(final ReportTO reportTO, final PageReference pageRef) {
        super();

        final MultilevelPanel mlp = new MultilevelPanel("executions");
        add(mlp);

        mlp.setFirstLevel(new ReportExecutionDirectoryPanel(mlp, reportTO.getKey(), new ReportRestClient(), pageRef));
    }

    private static class ReportExecutionDirectoryPanel extends ExecutionsDirectoryPanel {

        private static final long serialVersionUID = 5691719817252887541L;

        private final MultilevelPanel mlp;

        private final AjaxDownloadBehavior downloadBehavior;

        ReportExecutionDirectoryPanel(
                final MultilevelPanel multiLevelPanelRef,
                final String key,
                final ExecutionRestClient executionRestClient,
                final PageReference pageRef) {
            super(multiLevelPanelRef, key, executionRestClient, pageRef);
            this.mlp = multiLevelPanelRef;

            this.downloadBehavior = new AjaxDownloadBehavior();
            this.add(downloadBehavior);
        }

        @Override
        protected void next(
                final String title,
                final MultilevelPanel.SecondLevel slevel,
                final AjaxRequestTarget target) {
            mlp.next(title, slevel, target);
        }

        @Override
        protected void addFurtherActions(final ActionsPanel<ExecTO> panel, final IModel<ExecTO> model) {
            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                    downloadBehavior.setResponse(new ResponseHolder(ReportRestClient.exportExecutionResult(
                        model.getObject().getKey(), ReportExecExportFormat.CSV)));
                    downloadBehavior.initiate(target);
                }
            }, ActionLink.ActionType.EXPORT_CSV, IdRepoEntitlement.REPORT_READ);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                    downloadBehavior.setResponse(new ResponseHolder(ReportRestClient.exportExecutionResult(
                        model.getObject().getKey(), ReportExecExportFormat.HTML)));
                    downloadBehavior.initiate(target);
                }
            }, ActionLink.ActionType.EXPORT_HTML, IdRepoEntitlement.REPORT_READ);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                    downloadBehavior.setResponse(new ResponseHolder(ReportRestClient.exportExecutionResult(
                        model.getObject().getKey(), ReportExecExportFormat.PDF)));
                    downloadBehavior.initiate(target);
                }
            }, ActionLink.ActionType.EXPORT_PDF, IdRepoEntitlement.REPORT_READ);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                    downloadBehavior.setResponse(new ResponseHolder(ReportRestClient.exportExecutionResult(
                        model.getObject().getKey(), ReportExecExportFormat.RTF)));
                    downloadBehavior.initiate(target);
                }
            }, ActionLink.ActionType.EXPORT_RTF, IdRepoEntitlement.REPORT_READ);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ExecTO ignore) {
                    downloadBehavior.setResponse(new ResponseHolder(ReportRestClient.exportExecutionResult(
                        model.getObject().getKey(), ReportExecExportFormat.XML)));
                    downloadBehavior.initiate(target);
                }
            }, ActionLink.ActionType.EXPORT_XML, IdRepoEntitlement.REPORT_READ);
        }
    }
}
