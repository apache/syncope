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
import org.apache.syncope.client.console.chartjs.Chart;
import org.apache.syncope.client.console.chartjs.ChartJSPanel;
import org.apache.syncope.client.console.chartjs.ChartType;
import org.apache.syncope.client.console.chartjs.data.ChartData;
import org.apache.syncope.client.console.chartjs.data.Dataset;
import org.apache.syncope.client.console.chartjs.options.ChartOptions;
import org.apache.syncope.client.console.chartjs.options.Plugins;
import org.apache.syncope.client.console.chartjs.options.Scale;
import org.apache.syncope.client.console.chartjs.options.Scales;
import org.apache.syncope.client.console.chartjs.options.TooltipCallback;
import org.apache.syncope.client.console.chartjs.options.TooltipOptions;
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

    private static Chart build(final SystemInfo systeminfo) {

        final List<Double> cpuValues = new ArrayList<>();
        final List<Long> memValues = new ArrayList<>();
        final List<String> labels = new ArrayList<>();

        systeminfo.load().forEach(instant -> {

            labels.add(DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(
                    systeminfo.startTime() + instant.uptime()
            ));

            cpuValues.add(instant.systemLoadAverage() * 1000);
            memValues.add(instant.totalMemory());
        });

        Dataset cpu = new Dataset() {
        };
        cpu.getLabel().add("CPU");
        cpu.getData().addAll(cpuValues);
        cpu.getBorderColor().add("purple");
        cpu.getBackgroundColor().add("purple");
        cpu.setTension(0.4);

        Dataset mem = new Dataset() {
        };
        mem.getLabel().add("MEM");
        mem.getData().addAll(memValues);
        mem.getBorderColor().add("grey");
        mem.getBackgroundColor().add("grey");
        mem.setTension(0.4);

        ChartData<Dataset> data = new ChartData<>();
        data.getLabels().addAll(labels);
        data.getDatasets().addAll(List.of(cpu, mem));

        TooltipOptions tooltip = new TooltipOptions();
        tooltip.setEnabled(true);
        tooltip.setCallbacks(new TooltipCallback().setLabel(
                "function(context) {return context.dataset.label + ': ' + context.formattedValue;}"));

        Plugins plugins = new Plugins();
        plugins.setTooltip(tooltip);

        Scale x = new Scale();
        x.setDisplay(false);

        Scale y = new Scale();
        y.setDisplay(false);

        Scales scales = new Scales();
        scales.setX(x);
        scales.setY(y);

        ChartOptions options = new ChartOptions();
        options.setResponsive(true);
        options.setMaintainAspectRatio(true);
        options.setPlugins(plugins);
        options.setScales(scales);

        Chart chart = new Chart();
        chart.setType(ChartType.line);
        chart.setData(data);
        chart.setOptions(options);

        return chart;
    }

    public void refresh(final SystemInfo systeminfo) {
        chart.setDefaultModelObject(build(systeminfo));
    }
}
