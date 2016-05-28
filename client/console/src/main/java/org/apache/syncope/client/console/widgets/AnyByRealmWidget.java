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

import com.pingunaut.wicket.chartjs.chart.impl.Bar;
import com.pingunaut.wicket.chartjs.core.panel.BarChartPanel;
import com.pingunaut.wicket.chartjs.data.sets.BarDataSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.Model;

public class AnyByRealmWidget extends BaseWidget {

    private static final long serialVersionUID = -816175678514035085L;

    private static final int MAX_REALMS = 9;

    private Map<String, Integer> usersByRealm;

    private Map<String, Integer> groupsByRealm;

    private String anyType1;

    private Map<String, Integer> any1ByRealm;

    private String anyType2;

    private Map<String, Integer> any2ByRealm;

    private final BarChartPanel chart;

    public AnyByRealmWidget(
            final String id,
            final Map<String, Integer> usersByRealm,
            final Map<String, Integer> groupsByRealm,
            final String anyType1,
            final Map<String, Integer> any1ByRealm,
            final String anyType2,
            final Map<String, Integer> any2ByRealm) {

        super(id);
        this.usersByRealm = usersByRealm;
        this.groupsByRealm = groupsByRealm;
        this.anyType1 = anyType1;
        this.any1ByRealm = any1ByRealm;
        this.anyType2 = anyType2;
        this.any2ByRealm = any2ByRealm;
        setOutputMarkupId(true);

        chart = new BarChartPanel(
                "chart",
                Model.of(build(usersByRealm, groupsByRealm, anyType1, any1ByRealm, anyType2, any2ByRealm)));
        add(chart);
    }

    private Bar build(
            final Map<String, Integer> usersByRealm,
            final Map<String, Integer> groupsByRealm,
            final String anyType1,
            final Map<String, Integer> any1ByRealm,
            final String anyType2,
            final Map<String, Integer> any2ByRealm) {

        List<String> labels = new ArrayList<>();

        List<Integer> userValues = new ArrayList<>();
        List<Integer> groupValues = new ArrayList<>();
        List<Integer> any1Values = new ArrayList<>();
        List<Integer> any2Values = new ArrayList<>();

        Set<String> realmSet = new HashSet<>();
        realmSet.addAll(usersByRealm.keySet());
        realmSet.addAll(groupsByRealm.keySet());
        if (any1ByRealm != null) {
            realmSet.addAll(any1ByRealm.keySet());
        }
        if (any2ByRealm != null) {
            realmSet.addAll(any2ByRealm.keySet());
        }
        List<String> realms = new ArrayList<>(realmSet);
        Collections.sort(realms);

        for (int i = 0; i < realms.size() && i < MAX_REALMS; i++) {
            labels.add(StringUtils.prependIfMissing(StringUtils.substringAfterLast(realms.get(i), "/"), "/"));

            userValues.add(usersByRealm.get(realms.get(i)));
            groupValues.add(groupsByRealm.get(realms.get(i)));
            if (any1ByRealm != null) {
                any1Values.add(any1ByRealm.get(realms.get(i)));
            }
            if (any2ByRealm != null) {
                any2Values.add(any2ByRealm.get(realms.get(i)));
            }
        }

        Bar bar = new Bar();
        bar.getOptions().setScaleBeginAtZero(true);
        bar.getOptions().setScaleShowGridLines(true);
        bar.getOptions().setScaleGridLineWidth(1);
        bar.getOptions().setBarShowStroke(true);
        bar.getOptions().setBarStrokeWidth(2);
        bar.getOptions().setBarValueSpacing(5);
        bar.getOptions().setBarDatasetSpacing(1);
        bar.getOptions().setResponsive(true);
        bar.getOptions().setMaintainAspectRatio(true);
        bar.getOptions().setMultiTooltipTemplate("<%= datasetLabel %> - <%= value %>");

        bar.getData().setLabels(labels);

        List<BarDataSet> datasets = new ArrayList<>();
        LabeledBarDataSet userDataSet = new LabeledBarDataSet(userValues);
        userDataSet.setFillColor("orange");
        userDataSet.setLabel(getString("users"));
        datasets.add(userDataSet);
        LabeledBarDataSet groupDataSet = new LabeledBarDataSet(groupValues);
        groupDataSet.setFillColor("red");
        groupDataSet.setLabel(getString("groups"));
        datasets.add(groupDataSet);
        if (anyType1 != null) {
            LabeledBarDataSet any1DataSet = new LabeledBarDataSet(any1Values);
            any1DataSet.setFillColor("green");
            any1DataSet.setLabel(anyType1);
            datasets.add(any1DataSet);
        }
        if (anyType2 != null) {
            LabeledBarDataSet any2DataSet = new LabeledBarDataSet(any2Values);
            any2DataSet.setFillColor("aqua");
            any2DataSet.setLabel(anyType2);
            datasets.add(any2DataSet);
        }
        bar.getData().setDatasets(datasets);

        return bar;
    }

    public boolean refresh(
            final Map<String, Integer> usersByRealm,
            final Map<String, Integer> groupsByRealm,
            final String anyType1,
            final Map<String, Integer> any1ByRealm,
            final String anyType2,
            final Map<String, Integer> any2ByRealm) {

        if (!this.usersByRealm.equals(usersByRealm)
                || !this.groupsByRealm.equals(groupsByRealm)
                || (!(this.anyType1 == null && anyType1 == null) && !this.anyType1.equals(anyType1))
                || !this.any1ByRealm.equals(any1ByRealm)
                || (!(this.anyType2 == null && anyType2 == null) && !this.anyType2.equals(anyType2))
                || !this.any2ByRealm.equals(any2ByRealm)) {

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
