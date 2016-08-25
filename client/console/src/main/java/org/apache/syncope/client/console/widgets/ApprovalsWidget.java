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

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.Approvals;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.UserWorkflowService;
import org.apache.wicket.Application;
import org.apache.wicket.PageReference;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.event.WebSocketPushPayload;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;

public class ApprovalsWidget extends AlertWidget<WorkflowFormTO> {

    private static final long serialVersionUID = 7667120094526529934L;

    public ApprovalsWidget(final String id, final PageReference pageRef) {
        super(id);
        setOutputMarkupId(true);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof WebSocketPushPayload) {
            WebSocketPushPayload wsEvent = (WebSocketPushPayload) event.getPayload();
            if (wsEvent.getMessage() instanceof ApprovalsWidgetMessage) {
                List<WorkflowFormTO> updatedApprovals = ((ApprovalsWidgetMessage) wsEvent.getMessage()).
                        getUpdatedApprovals();
                if (!latestAlerts.equals(updatedApprovals)) {
                    latestAlerts.getObject().clear();
                    latestAlerts.getObject().addAll(updatedApprovals);

                    ApprovalsWidget.this.linkAlertsNumber.
                            setDefaultModelObject(ApprovalsWidget.this.latestAlerts.getObject().size());
                    wsEvent.getHandler().add(ApprovalsWidget.this.linkAlertsNumber);

                    ApprovalsWidget.this.headerAlertsNumber.
                            setDefaultModelObject(ApprovalsWidget.this.latestAlerts.getObject().size());
                    wsEvent.getHandler().add(ApprovalsWidget.this.headerAlertsNumber);

                    ApprovalsWidget.this.latestFive.removeAll();
                    wsEvent.getHandler().add(ApprovalsWidget.this.latestAlertsList);
                }
            }
        }
    }

    @Override
    protected IModel<List<WorkflowFormTO>> getLatestAlerts() {

        return new ListModel<WorkflowFormTO>() {

            private static final long serialVersionUID = -2583290457773357445L;

            @Override
            public List<WorkflowFormTO> getObject() {
                return ApprovalInfoUpdater.getLatestAlerts();
            }
        };
    }

    @Override
    protected Panel getAlertLink(final String panelid, final WorkflowFormTO event) {
        return new ApprovalsWidget.InnerPanel(panelid, event);
    }

    @Override
    protected AbstractLink getEventsLink(final String linkid) {
        BookmarkablePageLink<Approvals> approvals = BookmarkablePageLinkBuilder.build(linkid, Approvals.class);
        MetaDataRoleAuthorizationStrategy.authorize(approvals, WebPage.ENABLE, StandardEntitlement.WORKFLOW_FORM_LIST);
        return approvals;
    }

    public static final class InnerPanel extends Panel {

        private static final long serialVersionUID = 3829642687027801451L;

        public InnerPanel(final String id, final WorkflowFormTO alert) {
            super(id);

            final AjaxLink<String> approval = new AjaxLink<String>("approval") {

                private static final long serialVersionUID = 7021195294339489084L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    // do nothing
                }

                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);
                    if (StringUtils.isNotBlank(alert.getUsername())) {
                        tag.put("title", alert.getUsername().trim());
                    }
                }
            };

            add(approval);

            approval.add(new Label("key", new ResourceModel(alert.getKey(), alert.getKey())).
                    setRenderBodyOnly(true));

            approval.add(new Label("owner", alert.getOwner()));

            approval.add(new Label("createTime",
                    SyncopeConsoleSession.get().getDateFormat().format(alert.getCreateTime())).
                    setRenderBodyOnly(true));

            WebMarkupContainer dueDateContainer = new WebMarkupContainer("dueDateContainer");
            dueDateContainer.setOutputMarkupId(true);
            approval.add(dueDateContainer);

            if (alert.getDueDate() == null) {
                dueDateContainer.add(new Label("dueDate"));
                dueDateContainer.setVisible(false);
            } else {
                dueDateContainer.add(new Label("dueDate",
                        SyncopeConsoleSession.get().getDateFormat().format(alert.getDueDate())).
                        setRenderBodyOnly(true));
            }
        }

    }

    public static final class ApprovalInfoUpdater implements Runnable {

        private final Application application;

        private final SyncopeConsoleSession session;

        private final IKey key;

        public ApprovalInfoUpdater(final ConnectedMessage message) {
            this.application = message.getApplication();
            this.session = SyncopeConsoleSession.get();
            this.key = message.getKey();
        }

        @Override
        public void run() {
            try {
                ThreadContext.setApplication(application);
                ThreadContext.setSession(session);

                List<WorkflowFormTO> updatedApprovals = getLatestAlerts();

                WebSocketSettings settings = WebSocketSettings.Holder.get(application);
                WebSocketPushBroadcaster broadcaster = new WebSocketPushBroadcaster(settings.getConnectionRegistry());
                broadcaster.broadcast(
                        new ConnectedMessage(application, session.getId(), key),
                        new ApprovalsWidgetMessage(updatedApprovals));
            } catch (Throwable t) {
                LOG.error("Unexpected error while checking for updated approval info", t);
            } finally {
                ThreadContext.detach();
            }
        }

        protected static List<WorkflowFormTO> getLatestAlerts() {
            final List<WorkflowFormTO> updatedApprovals;
            if (SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_LIST)
                    && SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_READ)) {
                updatedApprovals = SyncopeConsoleSession.get().getService(UserWorkflowService.class).getForms();
            } else {
                updatedApprovals = Collections.<WorkflowFormTO>emptyList();
            }
            Collections.sort(updatedApprovals, new WorkflowFormComparator());
            return updatedApprovals;
        }
    }

    private static class ApprovalsWidgetMessage implements IWebSocketPushMessage, Serializable {

        private static final long serialVersionUID = -824793424112532838L;

        private final List<WorkflowFormTO> updatedApprovals;

        ApprovalsWidgetMessage(final List<WorkflowFormTO> updatedApprovals) {
            this.updatedApprovals = updatedApprovals;
        }

        public List<WorkflowFormTO> getUpdatedApprovals() {
            return updatedApprovals;
        }

    }

    private static class WorkflowFormComparator implements Comparator<WorkflowFormTO> {

        @Override
        public int compare(final WorkflowFormTO o1, final WorkflowFormTO o2) {
            if (o1 == null) {
                return o2 == null ? 0 : 1;
            } else if (o2 == null) {
                return -1;
            } else {
                // inverse
                return o2.getCreateTime().compareTo(o1.getCreateTime());
            }
        }
    }
}
