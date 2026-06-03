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
package org.apache.syncope.client.console.chartjs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartJSPanel extends Panel {

    private static final long serialVersionUID = -8670277955339192068L;

    private static final Logger LOG = LoggerFactory.getLogger(ChartJSPanel.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().
            findAndAddModules().defaultPropertyInclusion(JsonInclude.Value.ALL_NON_NULL).build();

    private final IModel<Chart> model;

    private final WebMarkupContainer container;

    public ChartJSPanel(final String id, final IModel<Chart> model) {
        super(id, model);

        this.model = model;

        this.container = new WebMarkupContainer("chart");
        this.container.setOutputMarkupId(true);

        add(container);
        container.add(new ChartJSBehavior());
    }

    public Chart getChart() {
        return model.getObject();
    }

    public String generateChart(final String markupId) {
        try {
            String data = MAPPER.writeValueAsString(model.getObject().getData());
            String options = MAPPER.writeValueAsString(model.getObject().getOptions());

            return "WicketCharts['" + markupId + "'] = new Chart("
                    + "getChartCtx('" + markupId + "'),"
                    + "{"
                    + "type: '" + model.getObject().getType() + "',"
                    + "data: " + data + ","
                    + "options: " + options
                    + "});";
        } catch (Exception e) {
            LOG.error("Error rendering chart JS", e);
            return "";
        }
    }
}
