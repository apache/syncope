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
import org.apache.commons.lang3.tuple.Pair;
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

public class CompletenessWidget extends BaseWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    private Map<NumbersInfo.ConfItem, Boolean> confCompleteness;

    private final DoughnutChartPanel chart;

    private final WebMarkupContainer actions;

    private final BookmarkablePageLink<Page> topology;

    private final BookmarkablePageLink<Page> policies;

    private final BookmarkablePageLink<Page> notifications;

    private final BookmarkablePageLink<Page> types;

    private final BookmarkablePageLink<Page> securityquestions;

    private final BookmarkablePageLink<Page> roles;

    public CompletenessWidget(final String id, final Map<NumbersInfo.ConfItem, Boolean> confCompleteness) {
        super(id);
        this.confCompleteness = confCompleteness;
        setOutputMarkupId(true);

        Pair<Doughnut, Integer> built = build(confCompleteness);

        chart = new DoughnutChartPanel("chart", Model.of(built.getLeft()));
        add(chart);

        actions = new WebMarkupContainer("actions");
        actions.setOutputMarkupPlaceholderTag(true);
        actions.setVisible(built.getRight() > 0);

        add(actions);

        topology = BookmarkablePageLinkBuilder.build("topology", Topology.class);
        topology.setOutputMarkupPlaceholderTag(true);
        actions.add(topology);
        MetaDataRoleAuthorizationStrategy.authorize(topology, WebPage.ENABLE,
                String.format("%s,%s", StandardEntitlement.CONNECTOR_LIST, StandardEntitlement.RESOURCE_LIST));
        topology.setVisible(
                !confCompleteness.get(NumbersInfo.ConfItem.RESOURCE)
                || !confCompleteness.get(NumbersInfo.ConfItem.SYNC_TASK));

        policies = BookmarkablePageLinkBuilder.build("policies", Policies.class);
        policies.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(policies, WebPage.ENABLE, StandardEntitlement.POLICY_LIST);
        actions.add(policies);
        policies.setVisible(
                !confCompleteness.get(NumbersInfo.ConfItem.ACCOUNT_POLICY)
                || !confCompleteness.get(NumbersInfo.ConfItem.PASSWORD_POLICY));

        notifications = BookmarkablePageLinkBuilder.build("notifications", Notifications.class);
        notifications.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(
                notifications, WebPage.ENABLE, StandardEntitlement.NOTIFICATION_LIST);
        actions.add(notifications);
        notifications.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.NOTIFICATION));

        types = BookmarkablePageLinkBuilder.build("types", Types.class);
        types.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(types, WebPage.ENABLE, StandardEntitlement.SCHEMA_LIST);
        actions.add(types);
        types.setVisible(
                !confCompleteness.get(NumbersInfo.ConfItem.VIR_SCHEMA)
                || !confCompleteness.get(NumbersInfo.ConfItem.ANY_TYPE));

        securityquestions = BookmarkablePageLinkBuilder.build("securityquestions", SecurityQuestions.class);
        securityquestions.setOutputMarkupPlaceholderTag(true);
        actions.add(securityquestions);
        securityquestions.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.SECURITY_QUESTION));

        roles = BookmarkablePageLinkBuilder.build("roles", Roles.class);
        roles.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(roles, WebPage.ENABLE, StandardEntitlement.ROLE_LIST);
        actions.add(roles);
        roles.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.ROLE));
    }

    private Pair<Doughnut, Integer> build(final Map<NumbersInfo.ConfItem, Boolean> confCompleteness) {
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

        return Pair.of(doughnut, todo);
    }

    public boolean refresh(final Map<NumbersInfo.ConfItem, Boolean> confCompleteness) {
        if (!this.confCompleteness.equals(confCompleteness)) {
            this.confCompleteness = confCompleteness;

            Pair<Doughnut, Integer> built = build(confCompleteness);

            chart.setDefaultModelObject(built.getLeft());

            actions.setVisible(built.getRight() > 0);

            topology.setVisible(
                    !confCompleteness.get(NumbersInfo.ConfItem.RESOURCE)
                    || !confCompleteness.get(NumbersInfo.ConfItem.SYNC_TASK));

            policies.setVisible(
                    !confCompleteness.get(NumbersInfo.ConfItem.ACCOUNT_POLICY)
                    || !confCompleteness.get(NumbersInfo.ConfItem.PASSWORD_POLICY));

            notifications.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.NOTIFICATION));

            types.setVisible(
                    !confCompleteness.get(NumbersInfo.ConfItem.VIR_SCHEMA)
                    || !confCompleteness.get(NumbersInfo.ConfItem.ANY_TYPE));

            securityquestions.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.SECURITY_QUESTION));

            roles.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.ROLE));

            return true;
        }
        return false;
    }
}
