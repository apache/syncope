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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.chartjs.Chart;
import org.apache.syncope.client.console.chartjs.ChartJSPanel;
import org.apache.syncope.client.console.chartjs.ChartType;
import org.apache.syncope.client.console.chartjs.data.Dataset;
import org.apache.syncope.client.console.chartjs.options.Plugins;
import org.apache.syncope.client.console.chartjs.options.Scale;
import org.apache.syncope.client.console.chartjs.options.Scales;
import org.apache.syncope.client.console.chartjs.options.TooltipCallback;
import org.apache.syncope.client.console.chartjs.options.TooltipOptions;
import org.apache.wicket.model.Model;

public class AnyByRealmWidget extends BaseWidget {

    private static final long serialVersionUID = -816175678514035085L;

    private static final int MAX_REALMS = 9;

    private Map<String, Long> usersByRealm;
    private Map<String, Long> groupsByRealm;

    private String anyType1;
    private Map<String, Long> any1ByRealm;

    private String anyType2;
    private Map<String, Long> any2ByRealm;

    private final ChartJSPanel chart;

    public AnyByRealmWidget(
            final String id,
            final Map<String, Long> usersByRealm,
            final Map<String, Long> groupsByRealm,
            final String anyType1,
            final Map<String, Long> any1ByRealm,
            final String anyType2,
            final Map<String, Long> any2ByRealm) {

        super(id);

        this.usersByRealm = usersByRealm;
        this.groupsByRealm = groupsByRealm;
        this.anyType1 = anyType1;
        this.any1ByRealm = any1ByRealm;
        this.anyType2 = anyType2;
        this.any2ByRealm = any2ByRealm;

        setOutputMarkupId(true);

        chart = new ChartJSPanel("chart",
                Model.of(build(usersByRealm, groupsByRealm, anyType1, any1ByRealm, anyType2, any2ByRealm)));

        add(chart);
    }

    private static Chart build(
            final Map<String, Long> usersByRealm,
            final Map<String, Long> groupsByRealm,
            final String anyType1,
            final Map<String, Long> any1ByRealm,
            final String anyType2,
            final Map<String, Long> any2ByRealm) {

        final List<String> labels = new ArrayList<>();
        final List<Long> userValues = new ArrayList<>();
        final List<Long> groupValues = new ArrayList<>();
        final List<Long> any1Values = new ArrayList<>();
        final List<Long> any2Values = new ArrayList<>();

        final Set<String> realmSet = new HashSet<>();
        realmSet.addAll(usersByRealm.keySet());
        realmSet.addAll(groupsByRealm.keySet());
        if (any1ByRealm != null) {
            realmSet.addAll(any1ByRealm.keySet());
        }
        if (any2ByRealm != null) {
            realmSet.addAll(any2ByRealm.keySet());
        }
        List<String> realms = new ArrayList<>(realmSet);
        realms.sort(Comparator.naturalOrder());

        final int limit = Math.min(realms.size(), MAX_REALMS);

        for (int i = 0; i < limit; i++) {
            final String realm = realms.get(i);

            labels.add(StringUtils.substringAfterLast(realm, "/"));

            userValues.add(usersByRealm.getOrDefault(realm, 0L));
            groupValues.add(groupsByRealm.getOrDefault(realm, 0L));

            if (any1ByRealm != null) {
                any1Values.add(any1ByRealm.getOrDefault(realm, 0L));
            }
            if (any2ByRealm != null) {
                any2Values.add(any2ByRealm.getOrDefault(realm, 0L));
            }
        }

        final Chart chart = new Chart();
        chart.setType(ChartType.bar);

        chart.getData().setLabels(labels);

        chart.getOptions().setResponsive(true);
        chart.getOptions().setMaintainAspectRatio(true);

        final TooltipOptions tooltip = new TooltipOptions();
        tooltip.setEnabled(true);

        final TooltipCallback callbacks = new TooltipCallback();
        callbacks.setLabel("function(context) {return context.dataset.label + ': ' + context.formattedValue;}");

        tooltip.setCallbacks(callbacks);

        final Plugins plugins = new Plugins();
        plugins.setTooltip(tooltip);

        chart.getOptions().setPlugins(plugins);

        final Scale x = new Scale();
        x.setDisplay(true);

        final Scale y = new Scale();
        y.setDisplay(true);
        y.setMin(0);

        final Scales scales = new Scales();
        scales.setX(x);
        scales.setY(y);

        chart.getOptions().setScales(scales);

        chart.getData().getDatasets().add(dataset("Users", "orange", userValues));
        chart.getData().getDatasets().add(dataset("Groups", "red", groupValues));

        if (anyType1 != null) {
            chart.getData().getDatasets().add(dataset(anyType1, "green", any1Values));
        }
        if (anyType2 != null) {
            chart.getData().getDatasets().add(dataset(anyType2, "aqua", any2Values));
        }

        return chart;
    }

    private static Dataset dataset(final String label, final String color, final List<Long> values) {
        final Dataset ds = new Dataset() {
        };
        ds.setLabel(label);
        ds.setBackgroundColor(color);
        ds.setBorderColor(color);
        ds.setData(values);
        return ds;
    }

    public boolean refresh(
            final Map<String, Long> usersByRealm,
            final Map<String, Long> groupsByRealm,
            final String anyType1,
            final Map<String, Long> any1ByRealm,
            final String anyType2,
            final Map<String, Long> any2ByRealm) {

        if (!Objects.equals(this.usersByRealm, usersByRealm)
                || !Objects.equals(this.groupsByRealm, groupsByRealm)
                || !Objects.equals(this.anyType1, anyType1)
                || !Objects.equals(this.any1ByRealm, any1ByRealm)
                || !Objects.equals(this.anyType2, anyType2)
                || !Objects.equals(this.any2ByRealm, any2ByRealm)) {

            this.usersByRealm = usersByRealm;
            this.groupsByRealm = groupsByRealm;
            this.anyType1 = anyType1;
            this.any1ByRealm = any1ByRealm;
            this.anyType2 = anyType2;
            this.any2ByRealm = any2ByRealm;

            chart.setDefaultModelObject(
                    build(usersByRealm, groupsByRealm, anyType1, any1ByRealm, anyType2, any2ByRealm));

            return true;
        }

        return false;
    }
}
