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
package org.apache.syncope.client.console.widgets;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesomeIconTypeBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.Approvals;
import org.apache.syncope.client.console.rest.UserWorkflowRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.time.Duration;

public class ApprovalsWidget extends AlertWidget<WorkflowFormTO> {

    private static final long serialVersionUID = 7667120094526529934L;

    private final UserWorkflowRestClient restClient = new UserWorkflowRestClient();

    private final List<WorkflowFormTO> lastApprovals = new ArrayList<>();

    public ApprovalsWidget(final String id, final PageReference pageRef) {
        super(id);
        setOutputMarkupId(true);

        latestAlertsList.add(new IndicatorAjaxTimerBehavior(Duration.seconds(30)) {

            private static final long serialVersionUID = 7298597675929755960L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                if (!latestAlerts.getObject().equals(lastApprovals)) {
                    refreshLatestAlerts(target);
                }
            }
        });
    }

    public final void refreshLatestAlerts(final AjaxRequestTarget target) {
        latestAlerts.getObject().clear();
        latestAlerts.getObject().addAll(lastApprovals);

        int latestAlertSize = getLatestAlertsSize();
        linkAlertsNumber.setDefaultModelObject(latestAlertSize);
        target.add(linkAlertsNumber);

        headerAlertsNumber.setDefaultModelObject(latestAlertSize);
        target.add(headerAlertsNumber);

        target.add(latestAlertsList);

        lastApprovals.clear();
        lastApprovals.addAll(latestAlerts.getObject());
    }

    @Override
    protected int getLatestAlertsSize() {
        return SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_LIST)
                && SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_READ)
                ? restClient.countForms()
                : 0;
    }

    @Override
    protected IModel<List<WorkflowFormTO>> getLatestAlerts() {
        return new ListModel<WorkflowFormTO>() {

            private static final long serialVersionUID = -2583290457773357445L;

            @Override
            public List<WorkflowFormTO> getObject() {
                List<WorkflowFormTO> updatedApprovals;
                if (SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_LIST)
                        && SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_READ)) {

                    updatedApprovals = restClient.getForms(1, MAX_SIZE, new SortParam<>("createTime", true));
                } else {
                    updatedApprovals = Collections.<WorkflowFormTO>emptyList();
                }

                return updatedApprovals;
            }
        };
    }

    @Override
    protected AbstractLink getEventsLink(final String linkid) {
        BookmarkablePageLink<Approvals> approvals = BookmarkablePageLinkBuilder.build(linkid, Approvals.class);
        MetaDataRoleAuthorizationStrategy.authorize(approvals, WebPage.ENABLE, StandardEntitlement.WORKFLOW_FORM_LIST);
        return approvals;
    }

    @Override
    protected Icon getIcon(final String iconid) {
        return new Icon(iconid,
                FontAwesomeIconTypeBuilder.on(FontAwesomeIconTypeBuilder.FontAwesomeGraphic.handshake_o).build());
    }
}
