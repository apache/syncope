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
package org.apache.syncope.client.enduser.pages;

import java.io.Serializable;
import java.util.stream.Collectors;
import org.apache.syncope.client.enduser.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.panels.ResultPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SelfResult extends BasePage {

    private static final long serialVersionUID = 3804053409052140145L;

    private static final String RESULT_PAGE = "page.resultPage";

    public SelfResult(final PageParameters parameters) {
        this(null, parameters);
    }

    @SuppressWarnings("unchecked")
    public SelfResult(final ProvisioningResult<UserTO> provisioningResult, final PageParameters parameters) {
        super(parameters, RESULT_PAGE);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);
        Class<? extends WebPage> page;
        try {
            page = (Class<? extends WebPage>) Class.forName(parameters.get(EnduserConstants.LANDING_PAGE).
                    toString("org.apache.syncope.client.enduser.pages.Login"));
        } catch (ClassNotFoundException e) {
            LOG.debug("Login page not found", e);
            page = Login.class;
        }
        if (page.equals(Login.class)) {
            BookmarkablePageLink<WebPage> login =
                    new BookmarkablePageLink<>("login", Login.class);
            content.add(login.setOutputMarkupId(true));
            disableSidebarAndNavbar();
        } else {
            content.add(BookmarkablePageLinkBuilder.build("login", page));
        }

        content.add(new Label("resultTitle", parameters.get(Constants.NOTIFICATION_TITLE_PARAM).toString()));
        content.add(new Label("resultMessage", parameters.get(Constants.NOTIFICATION_MSG_PARAM).toString()));
        Fragment statusFragment = new Fragment("statusIcon",
                provisioningResult != null
                        && SyncopeWebApplication.get().isReportPropagationErrors()
                        && provisioningResult.getPropagationStatuses().stream()
                        .anyMatch(ps -> ExecStatus.SUCCESS != ps.getStatus())
                        ? "errorIcon" : "successIcon", content);
        // add also details about failed propagations, if enbaled by property enduser.showPropErrors
        if (provisioningResult != null
                && provisioningResult.getPropagationStatuses().stream()
                .anyMatch(ps -> ExecStatus.SUCCESS != ps.getStatus())) {
            statusFragment.add(new ResultPanel("propagationErrors",
                    (Serializable) provisioningResult.getPropagationStatuses().stream()
                            .filter(ps -> ExecStatus.SUCCESS != ps.getStatus())
                            .map(ps -> StatusUtils.getStatusBean(provisioningResult.getEntity(), ps.getResource(),
                                    ps.getAfterObj(), false)).collect(Collectors.toList()), getPageReference())
                    .setOutputMarkupId(true)
                    .setVisible(SyncopeWebApplication.get().isReportPropagationErrorDetails()));
        }
        content.add(statusFragment);
    }
}
