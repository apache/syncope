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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class Policies extends BasePage {

    private static final long serialVersionUID = -1100228004207271271L;

    public Policies(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        content.setMarkupId("policies");

        List<ITab> tabs = SyncopeWebApplication.get().getPolicyTabProviders().stream().
                map(p -> p.buildTabList(getPageReference())).
                flatMap(List::stream).
                sorted(Comparator.comparing(tab -> tab.getTitle().getObject())).
                collect(Collectors.toList());
        content.add(new AjaxBootstrapTabbedPanel<>("tabbedPanel", tabs));

        body.add(content);
    }
}
