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
import java.util.List;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.console.chartjs.ChartJSPanel;
import org.apache.syncope.client.console.chartjs.ChartOptions;
import org.apache.syncope.client.console.chartjs.Line;
import org.apache.syncope.client.console.chartjs.LineDataSet;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.wicket.model.Model;

public class LoadWidget extends BaseWidget {

    private static final long serialVersionUID = -816175678514035085L;

    private final ChartJSPanel chart;

    public LoadWidget(final String id, final SystemInfo systeminfo) {
        super(id);
        setOutputMarkupId(true);

        chart = new ChartJSPanel("chart", Model.of(build(systeminfo)));
        add(chart);
    }

    private static Line build(final SystemInfo systeminfo) {
        List<Double> cpuValues = new ArrayList<>();
        List<Long> memValues = new ArrayList<>();

        Line line = new Line();
        line.getOptions().setPointDot(false);
        line.getOptions().setDatasetFill(false);
        line.getOptions().setResponsive(true);
        line.getOptions().setMaintainAspectRatio(true);
        line.getOptions().setTension(0.4);
        line.getOptions().setMultiTooltipTemplate("<%= datasetLabel %>");

        ChartOptions.Axis x = new ChartOptions.Axis();
        x.setDisplay(false);
        ChartOptions.Axis y = new ChartOptions.Axis();
        y.setDisplay(false);
        ChartOptions.Scales scales = new ChartOptions.Scales();
        scales.setX(x);
        scales.setY(y);
        line.getOptions().setScales(scales);

        systeminfo.getLoad().forEach(instant -> {
            line.getData().getLabels().add(
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(
                            systeminfo.getStartTime() + instant.getUptime()));

            cpuValues.add(instant.getSystemLoadAverage() * 1000);
            memValues.add(instant.getTotalMemory());
        });

        LineDataSet cpuDataSet = new LineDataSet(cpuValues);
        cpuDataSet.setLabel("CPU");
        cpuDataSet.setPointColor("purple");
        cpuDataSet.setBorderColor("purple");
        line.getData().getDatasets().add(cpuDataSet);

        LineDataSet memDataSet = new LineDataSet(memValues);
        memDataSet.setLabel("MEM");
        memDataSet.setPointColor("grey");
        memDataSet.setBorderColor("grey");
        line.getData().getDatasets().add(memDataSet);

        return line;
    }

    public void refresh(final SystemInfo systeminfo) {
        chart.setDefaultModelObject(build(systeminfo));
    }
}
