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

import static org.apache.wicket.Component.ENABLE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.LogStatementPanel;
import org.apache.syncope.client.console.rest.LoggerRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Application;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketPushBroadcaster;
import org.apache.wicket.protocol.ws.api.event.WebSocketPushPayload;
import org.apache.wicket.protocol.ws.api.message.ConnectedMessage;
import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.IKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogViewer extends WebPage {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewer.class);

    private static final int MAX_STATEMENTS_PER_APPENDER = 50;

    private static final long serialVersionUID = -7578329899052708105L;

    private final LoggerRestClient restClient = new LoggerRestClient();

    private final IModel<Long> lastTimeInMillis = Model.of(0L);

    private final WebMarkupContainer stContainer;

    private final IModel<List<LogStatementTO>> statementViewModel;

    private final ListView<LogStatementTO> statementView;

    public LogViewer() {
        final WebMarkupContainer viewer = new WebMarkupContainer("viewer");
        viewer.setOutputMarkupId(true);
        add(viewer);

        final AjaxDropDownChoicePanel<String> appenders = new AjaxDropDownChoicePanel<>(
                "appenders", "Appender", new Model<String>(), false);
        MetaDataRoleAuthorizationStrategy.authorize(appenders, ENABLE, StandardEntitlement.LOG_READ);
        appenders.setChoices(restClient.listMemoryAppenders());
        viewer.add(appenders);

        stContainer = new WebMarkupContainer("stContainer");
        stContainer.setOutputMarkupId(true);
        viewer.add(stContainer);

        statementViewModel = new ListModel<>(new ArrayList<LogStatementTO>());
        statementView = new ListView<LogStatementTO>("statements", statementViewModel) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<LogStatementTO> item) {
                LogStatementPanel panel = new LogStatementPanel("statement", item.getModelObject());
                panel.setOutputMarkupId(true);
                item.add(panel);
            }
        };
        statementView.setOutputMarkupId(true);
        stContainer.add(statementView);

        appenders.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                List<LogStatementTO> lastStatements = appenders.getModelObject() == null
                        ? new ArrayList<LogStatementTO>()
                        : restClient.getLastLogStatements(appenders.getModelObject(), 0);
                statementViewModel.setObject(lastStatements);
                target.add(stContainer);

                lastTimeInMillis.setObject(0L);
            }
        });

        add(new WebSocketBehavior() {

            private static final long serialVersionUID = 3507933905864454312L;

            @Override
            protected void onConnect(final ConnectedMessage message) {
                super.onConnect(message);

                SyncopeConsoleSession.get().scheduleAtFixedRate(
                        new LogStatementUpdater(message, restClient, appenders, lastTimeInMillis),
                        0, 10, TimeUnit.SECONDS);
            }

        });
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof WebSocketPushPayload) {
            WebSocketPushPayload wsEvent = (WebSocketPushPayload) event.getPayload();
            if (wsEvent.getMessage() instanceof LogViewerMessage) {
                List<LogStatementTO> recentLogStatements =
                        ((LogViewerMessage) wsEvent.getMessage()).getRecentLogStatements();

                if (!recentLogStatements.isEmpty()) {
                    // save scroll position
                    wsEvent.getHandler().prependJavaScript(
                            String.format("window.scrollTop = $('#%s').scrollTop();", stContainer.getMarkupId()));

                    int currentSize = statementView.getModelObject().size();
                    int recentSize = recentLogStatements.size();

                    List<LogStatementTO> newModelObject = SetUniqueList.<LogStatementTO>setUniqueList(
                            new ArrayList<LogStatementTO>(MAX_STATEMENTS_PER_APPENDER));
                    if (currentSize <= MAX_STATEMENTS_PER_APPENDER - recentSize) {
                        newModelObject.addAll(statementView.getModelObject());
                    } else {
                        newModelObject.addAll(statementView.getModelObject().subList(recentSize, currentSize));
                    }
                    newModelObject.addAll(recentLogStatements);

                    statementViewModel.setObject(newModelObject);
                    wsEvent.getHandler().add(LogViewer.this.stContainer);

                    // restore scroll position - might not work perfectly if items were removed from the top
                    wsEvent.getHandler().appendJavaScript(
                            String.format("$('#%s').scrollTop(window.scrollTop);", stContainer.getMarkupId()));
                }
            }
        }
    }

    private static final class LogStatementUpdater implements Runnable {

        private final Application application;

        private final SyncopeConsoleSession session;

        private final IKey key;

        private final LoggerRestClient restClient;

        private final AjaxDropDownChoicePanel<String> appenders;

        private final IModel<Long> lastTimeInMillis;

        LogStatementUpdater(
                final ConnectedMessage message,
                final LoggerRestClient restClient,
                final AjaxDropDownChoicePanel<String> appenders,
                final IModel<Long> lastTimeInMillis) {

            this.application = message.getApplication();
            this.session = SyncopeConsoleSession.get();
            this.key = message.getKey();
            this.restClient = restClient;
            this.appenders = appenders;
            this.lastTimeInMillis = lastTimeInMillis;
        }

        @Override
        public void run() {
            try {
                ThreadContext.setApplication(application);
                ThreadContext.setSession(session);

                List<LogStatementTO> recentLogStatements = appenders.getModelObject() == null
                        ? new ArrayList<LogStatementTO>()
                        : restClient.getLastLogStatements(appenders.getModelObject(), lastTimeInMillis.getObject());
                if (!recentLogStatements.isEmpty()) {
                    lastTimeInMillis.setObject(recentLogStatements.get(recentLogStatements.size() - 1).getTimeMillis());
                }

                WebSocketSettings settings = WebSocketSettings.Holder.get(application);
                WebSocketPushBroadcaster broadcaster = new WebSocketPushBroadcaster(settings.getConnectionRegistry());
                broadcaster.broadcast(
                        new ConnectedMessage(application, session.getId(), key),
                        new LogViewerMessage(recentLogStatements));
            } catch (Throwable t) {
                LOG.error("Unexpected error while checking for recent log statements", t);
            } finally {
                ThreadContext.detach();
            }
        }
    }

    private static class LogViewerMessage implements IWebSocketPushMessage, Serializable {

        private static final long serialVersionUID = 7241149017008105769L;

        private final List<LogStatementTO> recentLogStatements;

        LogViewerMessage(final List<LogStatementTO> recentLogStatements) {
            this.recentLogStatements = recentLogStatements;
        }

        public List<LogStatementTO> getRecentLogStatements() {
            return recentLogStatements;
        }

    }
}
