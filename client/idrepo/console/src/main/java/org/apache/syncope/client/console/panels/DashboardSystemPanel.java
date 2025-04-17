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
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.HttpResourceStream;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
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

        AjaxNumberFieldPanel<Integer> threshold = new AjaxNumberFieldPanel.Builder<Integer>().
                min(0).step(10).enableOnChange().
                build("threshold", "threshold", Integer.class, Model.of(100));
        add(threshold.addRequiredLabel());

        ListModel<String> elementsModel = new ListModel<>(new ArrayList<>());
        add(new MultiFieldPanel.Builder<>(elementsModel).build(
                "elements",
                "elements",
                new AjaxTextFieldPanel("panel", "elements", Model.of())));

        Link<Void> dbExportLink = new Link<>("dbExportLink") {

            private static final long serialVersionUID = -4331619903296515985L;

            @Override
            public void onClick() {
                try {
                    HttpResourceStream stream = new HttpResourceStream(new ResponseHolder(
                            syncopeRestClient.exportInternalStorageContent(
                                    threshold.getModelObject(), elementsModel.getObject())));

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
