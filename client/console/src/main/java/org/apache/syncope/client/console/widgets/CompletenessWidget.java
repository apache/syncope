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

import com.pingunaut.wicket.chartjs.chart.impl.Doughnut;
import com.pingunaut.wicket.chartjs.core.panel.DoughnutChartPanel;
import java.util.Map;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.pages.Notifications;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.client.console.pages.Roles;
import org.apache.syncope.client.console.pages.SecurityQuestions;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Page;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;

public class CompletenessWidget extends AbstractWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    public CompletenessWidget(final String id, final Map<NumbersInfo.ConfItem, Boolean> confCompleteness) {
        super(id);

        Doughnut doughnut = new Doughnut();
        doughnut.getOptions().setResponsive(true);
        doughnut.getOptions().setMaintainAspectRatio(true);
        doughnut.getOptions().setTooltipTemplate("<%= label %>");

        int done = 0;
        int todo = 0;
        for (Map.Entry<NumbersInfo.ConfItem, Boolean> entry : confCompleteness.entrySet()) {
            if (entry.getValue()) {
                done += entry.getKey().getScore();
            } else {
                todo++;
            }
        }

        doughnut.getData().add(
                new LabeledDoughnutChartData(done, "blue", getString("done")));
        doughnut.getData().add(
                new LabeledDoughnutChartData(100 - done, "red", getString("todo") + ": " + todo));

        add(new DoughnutChartPanel("chart", Model.of(doughnut)));

        WebMarkupContainer actions = new WebMarkupContainer("actions");
        actions.setOutputMarkupPlaceholderTag(true);
        if (todo == 0) {
            actions.setVisible(false);
        }
        add(actions);

        BookmarkablePageLink<Page> link = BookmarkablePageLinkBuilder.build("topology", Topology.class);
        link.setOutputMarkupPlaceholderTag(true);
        actions.add(link);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.ENABLE,
                String.format("%s,%s", StandardEntitlement.CONNECTOR_LIST, StandardEntitlement.RESOURCE_LIST));
        if (confCompleteness.get(NumbersInfo.ConfItem.RESOURCE)
                || confCompleteness.get(NumbersInfo.ConfItem.SYNC_TASK)) {

            link.setVisible(false);
        }

        link = BookmarkablePageLinkBuilder.build("policies", Policies.class);
        link.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.ENABLE, StandardEntitlement.POLICY_LIST);
        actions.add(link);
        if (confCompleteness.get(NumbersInfo.ConfItem.ACCOUNT_POLICY)
                || confCompleteness.get(NumbersInfo.ConfItem.PASSWORD_POLICY)) {

            link.setVisible(false);
        }

        link = BookmarkablePageLinkBuilder.build("notifications", Notifications.class);
        link.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.ENABLE, StandardEntitlement.NOTIFICATION_LIST);
        actions.add(link);
        if (confCompleteness.get(NumbersInfo.ConfItem.NOTIFICATION)) {
            link.setVisible(false);
        }

        link = BookmarkablePageLinkBuilder.build("types", Types.class);
        link.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.ENABLE, StandardEntitlement.SCHEMA_LIST);
        actions.add(link);
        if (confCompleteness.get(NumbersInfo.ConfItem.VIR_SCHEMA)
                || confCompleteness.get(NumbersInfo.ConfItem.ANY_TYPE)) {

            link.setVisible(false);
        }

        link = BookmarkablePageLinkBuilder.build("securityquestions", SecurityQuestions.class);
        link.setOutputMarkupPlaceholderTag(true);
        actions.add(link);
        if (confCompleteness.get(NumbersInfo.ConfItem.SECURITY_QUESTION)) {
            link.setVisible(false);
        }

        link = BookmarkablePageLinkBuilder.build("roles", Roles.class);
        link.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(link, WebPage.ENABLE, StandardEntitlement.ROLE_LIST);
        actions.add(link);
        if (confCompleteness.get(NumbersInfo.ConfItem.ROLE)) {
            link.setVisible(false);
        }
    }

}
