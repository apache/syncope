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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.LogStatementPanel;
import org.apache.syncope.client.console.rest.LoggerRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogViewer extends WebPage {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewer.class);

    private static final int MAX_STATEMENTS_PER_APPENDER = 50;

    private static final long serialVersionUID = -7578329899052708105L;

    private final LoggerRestClient restClient = new LoggerRestClient();

    public LogViewer() {
        final WebMarkupContainer viewer = new WebMarkupContainer("viewer");
        viewer.setOutputMarkupId(true);
        add(viewer);

        final AjaxDropDownChoicePanel<String> appenders = new AjaxDropDownChoicePanel<>(
                "appenders", "Appender", new Model<String>(), false);
        MetaDataRoleAuthorizationStrategy.authorize(appenders, ENABLE, StandardEntitlement.LOG_READ);
        appenders.setChoices(restClient.listMemoryAppenders());
        viewer.add(appenders);

        final WebMarkupContainer stContainer = new WebMarkupContainer("stContainer");
        stContainer.setOutputMarkupId(true);
        viewer.add(stContainer);

        final Model<Long> lastTimeInMillis = Model.of(0L);
        final IModel<List<LogStatementTO>> statementViewModel = new ListModel<>(new ArrayList<LogStatementTO>());
        final ListView<LogStatementTO> statementView = new ListView<LogStatementTO>("statements", statementViewModel) {

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
        stContainer.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(10)) {

            private static final long serialVersionUID = 7298597675929755960L;

            @Override
            protected void onPostProcessTarget(final AjaxRequestTarget target) {
                // save scroll position
                target.prependJavaScript(
                        String.format("window.scrollTop = $('#%s').scrollTop();", stContainer.getMarkupId()));

                List<LogStatementTO> recentLogStatements = appenders.getModelObject() == null
                        ? new ArrayList<LogStatementTO>()
                        : restClient.getLastLogStatements(appenders.getModelObject(), lastTimeInMillis.getObject());
                if (!recentLogStatements.isEmpty()) {
                    lastTimeInMillis.setObject(recentLogStatements.get(recentLogStatements.size() - 1).getTimeMillis());

                    int currentSize = statementView.getModelObject().size();
                    int recentSize = recentLogStatements.size();

                    List<LogStatementTO> newModelObject = SetUniqueList.<LogStatementTO>setUniqueList(
                            new ArrayList<LogStatementTO>(MAX_STATEMENTS_PER_APPENDER));
                    if (currentSize <= MAX_STATEMENTS_PER_APPENDER - recentSize) {
                        newModelObject.addAll(statementView.getModelObject());
                    } else {
                        newModelObject.addAll(statementView.getModelObject().
                                subList(currentSize - (MAX_STATEMENTS_PER_APPENDER - recentSize), currentSize));
                    }
                    newModelObject.addAll(recentLogStatements);

                    statementViewModel.setObject(newModelObject);
                    target.add(stContainer);

                }

                // restore scroll position - might not work perfectly if items were removed from the top
                target.appendJavaScript(
                        String.format("$('#%s').scrollTop(window.scrollTop);", stContainer.getMarkupId()));
            }
        });

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
    }
}
