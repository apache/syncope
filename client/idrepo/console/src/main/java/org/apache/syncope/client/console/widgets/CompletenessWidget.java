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
import org.apache.syncope.client.console.chartjs.Chart;
import org.apache.syncope.client.console.chartjs.ChartJSPanel;
import org.apache.syncope.client.console.chartjs.ChartType;
import org.apache.syncope.client.console.chartjs.data.Dataset;
import org.apache.syncope.client.console.chartjs.options.Plugins;
import org.apache.syncope.client.console.chartjs.options.TooltipCallback;
import org.apache.syncope.client.console.chartjs.options.TooltipOptions;
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

        Pair<Chart, Long> built = build(confCompleteness);

        chart = new ChartJSPanel("chart", Model.of(built.getLeft()));
        add(chart);

        actions = new WebMarkupContainer("actions");
        actions.setOutputMarkupPlaceholderTag(true);
        actions.setVisible(built.getRight() > 0);
        add(actions);

        policies = BookmarkablePageLinkBuilder.build("policies", Policies.class);
        MetaDataRoleAuthorizationStrategy.authorize(policies, WebPage.ENABLE, IdRepoEntitlement.POLICY_LIST);
        actions.add(policies);

        notifications = BookmarkablePageLinkBuilder.build("notifications", Notifications.class);
        MetaDataRoleAuthorizationStrategy.authorize(notifications, WebPage.ENABLE, IdRepoEntitlement.NOTIFICATION_LIST);
        actions.add(notifications);

        types = BookmarkablePageLinkBuilder.build("types", Types.class);
        MetaDataRoleAuthorizationStrategy.authorize(types, WebPage.ENABLE, IdRepoEntitlement.ANYTYPECLASS_LIST);
        actions.add(types);

        securityquestions = BookmarkablePageLinkBuilder.build("securityquestions", Security.class);
        actions.add(securityquestions);

        roles = BookmarkablePageLinkBuilder.build("roles", Security.class);
        MetaDataRoleAuthorizationStrategy.authorize(roles, WebPage.ENABLE, IdRepoEntitlement.ROLE_LIST);
        actions.add(roles);

        notifications.setVisible(!Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.NOTIFICATION.name())));
        types.setVisible(!Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.ANY_TYPE.name())));
        securityquestions.setVisible(!Boolean.TRUE.equals(confCompleteness.get(
                NumbersInfo.ConfItem.SECURITY_QUESTION.name())));
        roles.setVisible(!Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.ROLE.name())));
        policies.setVisible(!Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.ACCOUNT_POLICY.name()))
                || !Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.PASSWORD_POLICY.name())));
    }

    private static Pair<Long, Long> buildWeights(final Map<String, Boolean> confCompleteness) {
        long done = 0;
        long total = 0;
        for (NumbersInfo.ConfItem item : NumbersInfo.ConfItem.values()) {
            long score = NumbersInfo.ConfItem.getScore(item);
            if (score <= 0) {
                continue;
            }
            total += score;
            if (BooleanUtils.isTrue(confCompleteness.get(item.name()))) {
                done += score;
            }
        }
        return Pair.of(done, total);
    }

    private Pair<Chart, Long> build(final Map<String, Boolean> confCompleteness) {
        Pair<Long, Long> weights = buildWeights(confCompleteness);
        long done = weights.getLeft();
        long total = weights.getRight();
        long todo = Math.max(0, total - done);
        long donePercentage = total == 0 ? 0 : Math.round((double) done * 100 / total);
        long todoPercentage = total == 0 ? 0 : 100 - donePercentage;

        final Chart resultChart = new Chart();
        resultChart.setType(ChartType.doughnut);
        resultChart.getOptions().setResponsive(true);
        resultChart.getOptions().setMaintainAspectRatio(true);

        final TooltipOptions tooltip = new TooltipOptions();
        tooltip.setEnabled(true);

        final TooltipCallback callbacks = new TooltipCallback();
        callbacks.setLabel("function(context) {return context.label;}");

        tooltip.setCallbacks(callbacks);

        final Plugins plugins = new Plugins();
        plugins.setTooltip(tooltip);
        resultChart.getOptions().setPlugins(plugins);

        final Dataset ds = new Dataset() {
        };
        ds.setBackgroundColor("green", "red");
        ds.setBorderColor("green", "red");
        ds.setData(java.util.List.of(done, todo));

        resultChart.getData().setLabels(java.util.List.of(
                getString("done") + " (" + donePercentage + "%)",
                getString("todo") + " (" + todoPercentage + "%)"));
        resultChart.getData().getDatasets().add(ds);
        return Pair.of(resultChart, todo);
    }

    public boolean refresh(final Map<String, Boolean> confCompleteness) {

        if (!this.confCompleteness.equals(confCompleteness)) {
            this.confCompleteness = confCompleteness;

            Pair<Chart, Long> built = build(confCompleteness);

            chart.setDefaultModelObject(built.getLeft());
            actions.setVisible(built.getRight() > 0);

            notifications.setVisible(
                    !Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.NOTIFICATION.name())));
            types.setVisible(!Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.ANY_TYPE.name())));
            securityquestions.setVisible(!Boolean.TRUE.equals(confCompleteness.get(
                    NumbersInfo.ConfItem.SECURITY_QUESTION.name())));
            roles.setVisible(!Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.ROLE.name())));
            policies.setVisible(!Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.ACCOUNT_POLICY.name()))
                    || !Boolean.TRUE.equals(confCompleteness.get(NumbersInfo.ConfItem.PASSWORD_POLICY.name())));

            return true;
        }
        return false;
    }
}
