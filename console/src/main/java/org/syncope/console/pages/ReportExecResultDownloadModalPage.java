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
package org.syncope.console.pages;

import java.util.Arrays;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.Model;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.types.ReportExecExportFormat;

public class ReportExecResultDownloadModalPage extends BaseModalPage {

    private static final long serialVersionUID = 3163146190501510888L;

    public ReportExecResultDownloadModalPage(final ModalWindow window,
            final PageReference callerPageRef) {

        final AjaxDropDownChoicePanel<ReportExecExportFormat> format =
                new AjaxDropDownChoicePanel<ReportExecExportFormat>(
                "format", "format", new Model<ReportExecExportFormat>(), false);

        format.setChoices(Arrays.asList(ReportExecExportFormat.values()));

        format.setChoiceRenderer(new IChoiceRenderer<ReportExecExportFormat>() {

            private static final long serialVersionUID = -3941271550163141339L;

            @Override
            public Object getDisplayValue(final ReportExecExportFormat object) {
                return object.name();
            }

            @Override
            public String getIdValue(final ReportExecExportFormat object,
                    final int index) {

                return object.name();
            }
        });

        format.getField().add(new AjaxFormComponentUpdatingBehavior(
                "onchange") {

            private static final long serialVersionUID =
                    -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ((ReportModalPage) callerPageRef.getPage()).setExportFormat(
                        format.getField().getInput());
                window.close(target);
            }
        });
        add(format);
    }
}
