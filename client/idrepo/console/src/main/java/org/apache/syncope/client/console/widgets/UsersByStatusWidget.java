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
import org.apache.syncope.client.console.chartjs.ChartJSPanel;
import org.apache.syncope.client.console.chartjs.Doughnut;
import org.apache.syncope.client.console.chartjs.DoughnutAndPieChartData;
import org.apache.wicket.model.Model;

public class UsersByStatusWidget extends BaseWidget {

    private static final long serialVersionUID = -816175678514035085L;

    private static final String[] COLORS = { "green", "orange", "aqua", "red", "gray" };

    private Map<String, Long> usersByStatus;

    private final ChartJSPanel chart;

    public UsersByStatusWidget(final String id, final Map<String, Long> usersByStatus) {
        super(id);
        this.usersByStatus = usersByStatus;
        setOutputMarkupId(true);

        chart = new ChartJSPanel("chart", Model.of(build(usersByStatus)));
        add(chart);
    }

    private static Doughnut build(final Map<String, Long> usersByStatus) {
        Doughnut doughnut = new Doughnut();
        doughnut.getOptions().setResponsive(true);
        doughnut.getOptions().setMaintainAspectRatio(true);

        DoughnutAndPieChartData data = new DoughnutAndPieChartData();
        doughnut.setData(data);

        DoughnutAndPieChartData.DataSet dataset = new DoughnutAndPieChartData.DataSet();
        data.getDatasets().add(dataset);

        int i = 0;
        for (Map.Entry<String, Long> entry : usersByStatus.entrySet()) {
            dataset.getData().add(entry.getValue());
            dataset.getBackgroundColor().add(COLORS[i % 5]);
            data.getLabels().add(entry.getKey());

            i++;
        }

        return doughnut;
    }

    public boolean refresh(final Map<String, Long> usersByStatus) {
        if (!this.usersByStatus.equals(usersByStatus)) {
            this.usersByStatus = usersByStatus;
            chart.setDefaultModelObject(build(usersByStatus));
            return true;
        }
        return false;
    }
}
