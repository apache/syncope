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
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.annotations.ExtWidget;
import org.apache.syncope.client.console.rest.CamelRoutesRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.common.lib.to.CamelMetrics;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

@ExtWidget(cssClass = "col-md-6")
public class CamelMetricsWidget extends BaseExtWidget {

    private static final long serialVersionUID = 4157815058487313617L;

    private List<CamelMetrics.MeanRate> meanRates;

    private final BarChartPanel chart;

    private final CamelRoutesRestClient restClient = new CamelRoutesRestClient();

    public CamelMetricsWidget(final String id, final PageReference pageRef) {
        super(id, pageRef);

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        CamelMetrics metrics = restClient.metrics();
        meanRates = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            meanRates.add(metrics.getResponseMeanRates().get(i));
        }

        chart = new BarChartPanel("chart", Model.of(build(meanRates)));
        container.add(chart);

        container.add(new IndicatorAjaxTimerBehavior(Duration.seconds(60)) {

            private static final long serialVersionUID = -4426283634345968585L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                CamelMetrics metrics = restClient.metrics();
                List<CamelMetrics.MeanRate> updatedMeanRates = new ArrayList<>(5);
                for (int i = 0; i < 5; i++) {
                    updatedMeanRates.add(metrics.getResponseMeanRates().get(i));
                }

                if (refresh(updatedMeanRates)) {
                    target.add(CamelMetricsWidget.this);
                }
            }
        });
    }

    private Bar build(final List<CamelMetrics.MeanRate> meanRates) {
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

        bar.getData().setLabels(CollectionUtils.collect(meanRates, new Transformer<CamelMetrics.MeanRate, String>() {

            @Override
            public String transform(final CamelMetrics.MeanRate input) {
                return input.getRouteId();
            }
        }, new ArrayList<String>()));

        BarDataSet dataset = new BarDataSet(CollectionUtils.collect(meanRates,
                new Transformer<CamelMetrics.MeanRate, Double>() {

            @Override
            public Double transform(final CamelMetrics.MeanRate input) {
                return input.getValue();
            }
        }, new ArrayList<Double>()));
        dataset.setFillColor("blue");
        bar.getData().setDatasets(Collections.singletonList(dataset));

        return bar;
    }

    private boolean refresh(final List<CamelMetrics.MeanRate> meanRates) {
        if (!this.meanRates.equals(meanRates)) {
            this.meanRates = meanRates;

            chart.setDefaultModelObject(build(meanRates));
            return true;
        }
        return false;
    }
}
