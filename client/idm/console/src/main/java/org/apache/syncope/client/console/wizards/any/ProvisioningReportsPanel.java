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

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class ProvisioningReportsPanel extends Panel {

    private static final long serialVersionUID = -1450755344104125918L;

    public ProvisioningReportsPanel(
            final String id, final List<ProvisioningReport> results, final PageReference pageRef) {
        super(id);

        List<ProvisioningReport> success = results.stream().
                filter(result -> result.getStatus() == ProvisioningReport.Status.SUCCESS).
                collect(Collectors.toList());
        add(new Accordion("success", List.of(new AbstractTab(
                new Model<>(MessageFormat.format(getString("success"), success.size()))) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new ListViewPanel.Builder<>(ProvisioningReport.class, pageRef).
                        setItems(success).
                        withChecks(ListViewPanel.CheckAvailability.NONE).
                        setCaptionVisible(false).
                        includes("name", "message").
                        build(panelId);
            }
        }), Model.of(-1)).setOutputMarkupId(true));

        List<ProvisioningReport> failure = results.stream().
                filter(result -> result.getStatus() == ProvisioningReport.Status.FAILURE).
                collect(Collectors.toList());
        add(new Accordion("failure", List.of(new AbstractTab(
                new Model<>(MessageFormat.format(getString("failure"), failure.size()))) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new ListViewPanel.Builder<>(ProvisioningReport.class, pageRef).
                        setItems(failure).
                        withChecks(ListViewPanel.CheckAvailability.NONE).
                        setCaptionVisible(false).
                        includes("name", "message").
                        build(panelId);
            }
        }), Model.of(-1)).setOutputMarkupId(true));

        List<ProvisioningReport> ignore = results.stream().
                filter(result -> result.getStatus() == ProvisioningReport.Status.IGNORE).
                collect(Collectors.toList());
        add(new Accordion("ignore", List.of(new AbstractTab(
                new Model<>(MessageFormat.format(getString("ignore"), ignore.size()))) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new ListViewPanel.Builder<>(ProvisioningReport.class, pageRef).
                        setItems(ignore).
                        withChecks(ListViewPanel.CheckAvailability.NONE).
                        setCaptionVisible(false).
                        includes("name", "message").
                        build(panelId);
            }
        }), Model.of(-1)).setOutputMarkupId(true));
    }
}
