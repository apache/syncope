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

import java.util.Map;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.chartjs.ChartJSPanel;
import org.apache.syncope.client.console.chartjs.Doughnut;
import org.apache.syncope.client.console.chartjs.DoughnutAndPieChartData;
import org.apache.syncope.client.console.pages.Notifications;
import org.apache.syncope.client.console.pages.Policies;
import org.apache.syncope.client.console.pages.Security;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;

public class CompletenessWidget extends BaseWidget {

    private static final long serialVersionUID = 7667120094526529934L;

    private Map<String, Boolean> confCompleteness;

    private final ChartJSPanel chart;

    private final WebMarkupContainer actions;

    private final BookmarkablePageLink<Policies> policies;

    private final BookmarkablePageLink<Notifications> notifications;

    private final BookmarkablePageLink<Types> types;

    private final BookmarkablePageLink<Security> securityquestions;

    private final BookmarkablePageLink<Security> roles;

    public CompletenessWidget(final String id, final Map<String, Boolean> confCompleteness) {
        super(id);
        this.confCompleteness = confCompleteness;
        setOutputMarkupId(true);

        Pair<Doughnut, Integer> built = build(confCompleteness);

        chart = new ChartJSPanel("chart", Model.of(built.getLeft()));
        add(chart);

        actions = new WebMarkupContainer("actions");
        actions.setOutputMarkupPlaceholderTag(true);
        actions.setVisible(built.getRight() > 0);

        add(actions);

        policies = BookmarkablePageLinkBuilder.build("policies", Policies.class);
        policies.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(policies, WebPage.ENABLE, IdRepoEntitlement.POLICY_LIST);
        actions.add(policies);
        policies.setVisible(
                !confCompleteness.get(NumbersInfo.ConfItem.ACCOUNT_POLICY.name())
                || !confCompleteness.get(NumbersInfo.ConfItem.PASSWORD_POLICY.name()));

        notifications = BookmarkablePageLinkBuilder.build("notifications", Notifications.class);
        notifications.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(
                notifications, WebPage.ENABLE, IdRepoEntitlement.NOTIFICATION_LIST);
        actions.add(notifications);
        notifications.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.NOTIFICATION.name()));

        types = BookmarkablePageLinkBuilder.build("types", Types.class);
        types.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(types, WebPage.ENABLE, IdRepoEntitlement.ANYTYPECLASS_LIST);
        actions.add(types);
        types.setVisible(
                !confCompleteness.get(NumbersInfo.ConfItem.VIR_SCHEMA.name())
                || !confCompleteness.get(NumbersInfo.ConfItem.ANY_TYPE.name()));

        securityquestions = BookmarkablePageLinkBuilder.build("securityquestions", Security.class);
        securityquestions.setOutputMarkupPlaceholderTag(true);
        actions.add(securityquestions);
        securityquestions.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.SECURITY_QUESTION.name()));

        roles = BookmarkablePageLinkBuilder.build("roles", Security.class);
        roles.setOutputMarkupPlaceholderTag(true);
        MetaDataRoleAuthorizationStrategy.authorize(roles, WebPage.ENABLE, IdRepoEntitlement.ROLE_LIST);
        actions.add(roles);
        roles.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.ROLE.name()));
    }

    private Pair<Doughnut, Integer> build(final Map<String, Boolean> confCompleteness) {
        Doughnut doughnut = new Doughnut();
        doughnut.getOptions().setResponsive(true);
        doughnut.getOptions().setMaintainAspectRatio(true);
        doughnut.getOptions().setTooltipTemplate("<%= label %>");

        int done = 0;
        int todo = 0;
        for (Map.Entry<String, Boolean> entry : confCompleteness.entrySet()) {
            if (BooleanUtils.isTrue(entry.getValue())) {
                done += NumbersInfo.ConfItem.getScore(entry.getKey());
            } else {
                todo++;
            }
        }

        DoughnutAndPieChartData data = new DoughnutAndPieChartData();
        doughnut.setData(data);

        DoughnutAndPieChartData.DataSet dataset = new DoughnutAndPieChartData.DataSet();
        data.getDatasets().add(dataset);

        dataset.getData().add(done);
        dataset.getData().add(100 - done);

        dataset.getBackgroundColor().add("green");
        dataset.getBackgroundColor().add("red");

        data.getLabels().add(getString("done"));
        data.getLabels().add(getString("todo") + ": " + todo);

        return Pair.of(doughnut, todo);
    }

    public boolean refresh(final Map<String, Boolean> confCompleteness) {
        if (!this.confCompleteness.equals(confCompleteness)) {
            this.confCompleteness = confCompleteness;

            Pair<Doughnut, Integer> built = build(confCompleteness);

            chart.setDefaultModelObject(built.getLeft());

            actions.setVisible(built.getRight() > 0);

            policies.setVisible(
                    !confCompleteness.get(NumbersInfo.ConfItem.ACCOUNT_POLICY.name())
                    || !confCompleteness.get(NumbersInfo.ConfItem.PASSWORD_POLICY.name()));

            notifications.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.NOTIFICATION.name()));

            types.setVisible(
                    !confCompleteness.get(NumbersInfo.ConfItem.VIR_SCHEMA.name())
                    || !confCompleteness.get(NumbersInfo.ConfItem.ANY_TYPE.name()));

            securityquestions.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.SECURITY_QUESTION.name()));

            roles.setVisible(!confCompleteness.get(NumbersInfo.ConfItem.ROLE.name()));

            return true;
        }
        return false;
    }
}
