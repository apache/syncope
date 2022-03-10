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
import java.io.Serializable;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.SCIMConfPanel;
import org.apache.syncope.client.console.rest.SCIMConfRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.any.ResultPage;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.types.SCIMEntitlement;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@ExtPage(label = "SCIM 2.0", icon = "fa fa-cloud", listEntitlement = SCIMEntitlement.SCIM_CONF_GET, priority = 500)
public class SCIMConfPage extends BaseExtPage {

    private static final long serialVersionUID = -8156063343062111770L;

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final WebMarkupContainer content;

    public SCIMConfPage(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        content = new WebMarkupContainer("content");

        content.add(new Label("body", "General"));
        content.setOutputMarkupId(true);
        body.add(content);

        updateSCIMGeneralConfContent(SCIMConfRestClient.get());
    }

    private WebMarkupContainer updateSCIMGeneralConfContent(final SCIMConf scimConf) {
        if (scimConf == null) {
            return content;
        }
        content.addOrReplace(new SCIMConfPanel("body", scimConf, SCIMConfPage.this.getPageReference()) {

            private static final long serialVersionUID = 8221398624379357183L;

            @Override
            protected void setWindowClosedReloadCallback(final BaseModal<?> modal) {
                modal.setWindowClosedCallback(target -> {
                    if (modal.getContent() instanceof ResultPage) {
                        Serializable result = ResultPage.class.cast(modal.getContent()).getResult();
                        try {
                            SCIMConfRestClient.set(MAPPER.readValue(result.toString(), SCIMConf.class));

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            modal.show(false);
                            target.add(content);
                        } catch (Exception e) {
                            LOG.error("While setting SCIM configuration", e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                });
            }
        });

        return content;
    }
}
