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

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.panels.SCIMConfPanel;
import org.apache.syncope.client.console.rest.SCIMConfRestClient;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.common.lib.scim.types.SCIMEntitlement;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

@ExtPage(label = "SCIM 2.0", icon = "fa fa-cloud", listEntitlement = SCIMEntitlement.SCIM_CONF_GET, priority = 500)
public class SCIMConfPage extends BaseExtPage {

    private static final long serialVersionUID = -8156063343062111770L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @SpringBean
    protected SCIMConfRestClient scimConfRestClient;

    public SCIMConfPage(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        body.add(content);

        content.add(new SCIMConfPanel("scimConf", scimConfRestClient.get(), SCIMConfPage.this.getPageReference()));
    }
}
