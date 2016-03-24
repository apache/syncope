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
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.TODOs;
import org.apache.syncope.client.console.rest.UserWorkflowRestClient;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
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
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.event.WebSocketPushPayload;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TODOsWidget extends Panel {

    private static final long serialVersionUID = 7667120094526529934L;

    private static final Logger LOG = LoggerFactory.getLogger(TODOsWidget.class);

    private static final int UPDATE_PERIOD = 30;

    private final Label linkTODOsNumber;

    private final Label headerTODOsNumber;

    private final WebMarkupContainer lastTODOsList;

    private final ListView<WorkflowFormTO> lastFive;

    private List<WorkflowFormTO> lastTODOs;

    public TODOsWidget(final String id, final PageReference pageRef) {
        super(id);
        setOutputMarkupId(true);

        LoadableDetachableModel<List<WorkflowFormTO>> model = new LoadableDetachableModel<List<WorkflowFormTO>>() {

            private static final long serialVersionUID = 7474274077691068779L;

            @Override
            protected List<WorkflowFormTO> load() {
                return TODOsWidget.this.lastTODOs.subList(
                        0, TODOsWidget.this.lastTODOs.size() < 6 ? TODOsWidget.this.lastTODOs.size() : 5);
            }
        };

        lastTODOs = getLastTODOs();
        Collections.sort(lastTODOs, new WorkflowFormComparator());

        linkTODOsNumber = new Label("todos", lastTODOs.size()) {

            private static final long serialVersionUID = 4755868673082976208L;

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                if (Integer.valueOf(getDefaultModelObject().toString()) > 0) {
                    tag.put("class", "label label-danger");
                } else {
                    tag.put("class", "label label-info");
                }
            }

        };
        add(linkTODOsNumber.setOutputMarkupId(true));

        headerTODOsNumber = new Label("number", lastTODOs.size());
        headerTODOsNumber.setOutputMarkupId(true);
        add(headerTODOsNumber);

        lastTODOsList = new WebMarkupContainer("lastTODOsList");
        lastTODOsList.setOutputMarkupId(true);
        add(lastTODOsList);

        lastFive = new ListView<WorkflowFormTO>("lastTODOs", model) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<WorkflowFormTO> item) {
                final WorkflowFormTO modelObject = item.getModelObject();

                final AjaxLink<String> todo = new AjaxLink<String>("todo") {

                    private static final long serialVersionUID = 7021195294339489084L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        // do nothing
                    }

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (StringUtils.isNotBlank(modelObject.getDescription())) {
                            tag.put("title", modelObject.getDescription().trim());
                        }
                    }
                };

                item.add(todo);

                todo.add(new Label("key", new ResourceModel(modelObject.getKey(), modelObject.getKey())).
                        setRenderBodyOnly(true));

                todo.add(new Label("owner", modelObject.getOwner()));

                todo.add(new Label("createTime",
                        SyncopeConsoleSession.get().getDateFormat().format(modelObject.getCreateTime())).
                        setRenderBodyOnly(true));

                WebMarkupContainer dueDateContainer = new WebMarkupContainer("dueDateContainer");
                dueDateContainer.setOutputMarkupId(true);
                todo.add(dueDateContainer);

                if (modelObject.getDueDate() == null) {
                    dueDateContainer.add(new Label("dueDate"));
                    dueDateContainer.setVisible(false);
                } else {
                    dueDateContainer.add(new Label("dueDate",
                            SyncopeConsoleSession.get().getDateFormat().format(modelObject.getDueDate())).
                            setRenderBodyOnly(true));
                }

            }
        };
        lastTODOsList.add(lastFive.setReuseItems(false).setOutputMarkupId(true));

        final BookmarkablePageLink<Object> todos = BookmarkablePageLinkBuilder.build("todosLink", TODOs.class);
        add(todos);

        MetaDataRoleAuthorizationStrategy.authorize(todos, WebPage.ENABLE, StandardEntitlement.WORKFLOW_FORM_LIST);

        add(new WebSocketBehavior() {

            private static final long serialVersionUID = 7944352891541344021L;

            @Override
            protected void onConnect(final ConnectedMessage message) {
                super.onConnect(message);
                SyncopeConsoleSession.get().scheduleAtFixedRate(
                        new TODOInfoUpdater(message), 0, UPDATE_PERIOD, TimeUnit.SECONDS);
            }
        });
    }

    private List<WorkflowFormTO> getLastTODOs() {
        if (SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_LIST)) {
            return new UserWorkflowRestClient().getForms();
        } else {
            return Collections.<WorkflowFormTO>emptyList();
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof WebSocketPushPayload) {
            WebSocketPushPayload wsEvent = (WebSocketPushPayload) event.getPayload();
            if (wsEvent.getMessage() instanceof UpdateMessage) {

                TODOsWidget.this.linkTODOsNumber.setDefaultModelObject(TODOsWidget.this.lastTODOs.size());
                wsEvent.getHandler().add(TODOsWidget.this.linkTODOsNumber);

                TODOsWidget.this.headerTODOsNumber.setDefaultModelObject(TODOsWidget.this.lastTODOs.size());
                wsEvent.getHandler().add(TODOsWidget.this.headerTODOsNumber);

                TODOsWidget.this.lastFive.removeAll();
                wsEvent.getHandler().add(TODOsWidget.this.lastTODOsList);
            }
        } else {
            super.onEvent(event);
        }
    }

    protected final class TODOInfoUpdater implements Runnable {

        private final Application application;

        private final SyncopeConsoleSession session;

        private final IKey key;

        public TODOInfoUpdater(final ConnectedMessage message) {
            this.application = message.getApplication();
            this.session = SyncopeConsoleSession.get();
            this.key = message.getKey();
        }

        @Override
        public void run() {
            try {
                ThreadContext.setApplication(application);
                ThreadContext.setSession(session);

                final List<WorkflowFormTO> actual = getLastTODOs();
                Collections.sort(actual, new WorkflowFormComparator());

                if (!actual.equals(lastTODOs)) {
                    LOG.debug("Update TODOs");

                    lastTODOs.clear();
                    lastTODOs = actual;

                    WebSocketSettings settings = WebSocketSettings.Holder.get(application);
                    WebSocketPushBroadcaster broadcaster
                            = new WebSocketPushBroadcaster(settings.getConnectionRegistry());
                    broadcaster.broadcast(new ConnectedMessage(application, session.getId(), key), new UpdateMessage());
                }
            } catch (Throwable t) {
                LOG.error("Unexpected error while checking for updated Job info", t);
            } finally {
                ThreadContext.detach();
            }
        }
    }

    private static class UpdateMessage implements IWebSocketPushMessage, Serializable {

        private static final long serialVersionUID = -824793424112532838L;

    }

    private class WorkflowFormComparator implements Comparator<WorkflowFormTO> {

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
