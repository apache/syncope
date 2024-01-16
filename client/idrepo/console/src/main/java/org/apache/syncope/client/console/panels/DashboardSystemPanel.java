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
package org.apache.syncope.client.console.panels;

import java.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.HttpResourceStream;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class DashboardSystemPanel extends Panel {

    @SpringBean
    protected SyncopeRestClient syncopeRestClient;

    private static final long serialVersionUID = -776362411304859269L;

    public DashboardSystemPanel(final String id) {
        super(id);

        Pair<String, String> gitAndBuildInfo = SyncopeConsoleSession.get().gitAndBuildInfo();
        Label version = new Label("version", gitAndBuildInfo.getRight());
        String versionLink =
                StringUtils.isNotBlank(gitAndBuildInfo.getLeft()) && gitAndBuildInfo.getRight().endsWith("-SNAPSHOT") 
                        ? "https://gitbox.apache.org/repos/asf?p=syncope.git;a=commit;h=" + gitAndBuildInfo.getLeft()
                        : "https://cwiki.apache.org/confluence/display/SYNCOPE/Maggiore";
        version.add(new AttributeModifier("onclick", "window.open('" + versionLink + "', '_blank')"));
        add(version);

        SystemInfo systemInfo = SyncopeConsoleSession.get().getSystemInfo();
        add(new Label("hostname", systemInfo.getHostname()));
        add(new Label("processors", systemInfo.getAvailableProcessors()));
        add(new Label("os", systemInfo.getOs()));
        add(new Label("jvm", systemInfo.getJvm()));

        Model<Integer> tableThresholdModel = Model.of(100);
        add(new TextField<>("tableThreshold", tableThresholdModel).add(new IndicatorAjaxFormComponentUpdatingBehavior(
                Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // nothing to do
            }
        }));

        Link<Void> dbExportLink = new Link<>("dbExportLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                try {
                    HttpResourceStream stream =
                            new HttpResourceStream(new ResponseHolder(syncopeRestClient.exportInternalStorageContent(
                                    tableThresholdModel.getObject())));

                    ResourceStreamRequestHandler rsrh = new ResourceStreamRequestHandler(stream);
                    rsrh.setFileName(
                            stream.getFilename() == null 
                                    ? SyncopeConsoleSession.get().getDomain() + "Content.xml"
                                    : stream.getFilename());
                    rsrh.setContentDisposition(ContentDisposition.ATTACHMENT);
                    rsrh.setCacheDuration(Duration.ZERO);

                    getRequestCycle().scheduleRequestHandlerAfterCurrent(rsrh);
                } catch (Exception e) {
                    SyncopeConsoleSession.get().onException(e);
                }
            }
        };
        MetaDataRoleAuthorizationStrategy.authorize(dbExportLink, WebPage.RENDER, IdRepoEntitlement.KEYMASTER);
        add(dbExportLink);
    }
}
