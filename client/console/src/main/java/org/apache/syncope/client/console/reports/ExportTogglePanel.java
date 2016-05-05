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

import java.io.Serializable;
import java.util.Arrays;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.HttpResourceStream;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.AbstractAjaxDownloadBehavior;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;

public class ExportTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -3195479265440591519L;

    private String execution;

    private IModel<ReportExecExportFormat> model = Model.of(ReportExecExportFormat.CSV);

    public ExportTogglePanel(final String id) {
        super(id);

        final AjaxDropDownChoicePanel<ReportExecExportFormat> format = new AjaxDropDownChoicePanel<>(
                "format",
                "format",
                model,
                false);

        format.setChoices(Arrays.asList(ReportExecExportFormat.values())).setOutputMarkupId(true);

        final AjaxExportDownloadBehavior downloadBehavior = new AjaxExportDownloadBehavior();
        format.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                try {
                    LOG.info("Downloding report execution {} [{}]", execution, format.getModelObject());
                    downloadBehavior.setExportFormat(format.getModelObject()).initiate(target);
                    toggle(target, false);
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ":" + e.getMessage());
                    SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                }
            }
        }).add(downloadBehavior);

        addInnerObject(format.hideLabel());
    }

    public void setExecution(final String execution) {
        this.execution = execution;
    }

    private class AjaxExportDownloadBehavior extends AbstractAjaxDownloadBehavior {

        private static final long serialVersionUID = 3109256773218160485L;

        private ReportExecExportFormat exportFormat;

        private HttpResourceStream stream;

        public AjaxExportDownloadBehavior setExportFormat(final ReportExecExportFormat exportFormat) {
            this.exportFormat = exportFormat;
            return this;
        }

        private void createResourceStream() {
            if (stream == null) {
                stream = new HttpResourceStream(new ReportRestClient().exportExecutionResult(execution, exportFormat));
            }
        }

        @Override
        protected String getFileName() {
            createResourceStream();
            return stream == null ? null : stream.getFilename();
        }

        @Override
        protected IResourceStream getResourceStream() {
            createResourceStream();
            return stream;
        }
    }

}
