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
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.wicket.model.Model;

public class AnyByRealmWidget extends AbstractWidget {

    private static final long serialVersionUID = -816175678514035085L;

    private static final int MAX_REALMS = 9;

    public AnyByRealmWidget(
            final String id,
            final Map<String, Integer> usersByRealm,
            final Map<String, Integer> groupsByRealm,
            final String anyType1,
            final Map<String, Integer> any1ByRealm,
            final String anyType2,
            final Map<String, Integer> any2ByRealm) {

        super(id);

        List<String> labels = new ArrayList<>();

        List<Integer> userValues = new ArrayList<>();
        List<Integer> groupValues = new ArrayList<>();
        List<Integer> any1Values = new ArrayList<>();
        List<Integer> any2Values = new ArrayList<>();

        List<RealmTO> realms = SyncopeConsoleSession.get().getService(RealmService.class).list();
        for (int i = 0; i < realms.size() && i < MAX_REALMS; i++) {
            RealmTO realm = realms.get(i);

            labels.add(realm.getName());

            userValues.add(usersByRealm.get(realm.getFullPath()));
            groupValues.add(groupsByRealm.get(realm.getFullPath()));
            if (any1ByRealm != null) {
                any1Values.add(any1ByRealm.get(realm.getFullPath()));
            }
            if (any2ByRealm != null) {
                any2Values.add(any2ByRealm.get(realm.getFullPath()));
            }
        }

        Bar bar = new Bar();
        bar.getData().setLabels(labels);
        bar.getOptions().setMultiTooltipTemplate("<%= datasetLabel %> - <%= value %>");

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

        add(new BarChartPanel("chart", Model.of(bar), MEDIUM_WIDTH, MEDIUM_HEIGHT));
    }

}
