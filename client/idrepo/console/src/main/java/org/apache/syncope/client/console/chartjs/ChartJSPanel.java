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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartJSPanel extends Panel {

    private static final long serialVersionUID = -3844680585650585253L;

    private static final Logger LOG = LoggerFactory.getLogger(ChartJSPanel.class);

    private static final ObjectMapper MAPPER = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);

    private final IModel<? extends Chart<?>> model;

    private final WebMarkupContainer container;

    public ChartJSPanel(
            final String id,
            final IModel<? extends Chart<?>> model) {

        super(id, model);

        this.model = model;
        this.container = new WebMarkupContainer("chart");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(container);
        container.setOutputMarkupId(true);
        container.add(new ChartJSBehavior());
    }

    public Chart<?> getChart() {
        return model.getObject();
    }

    public String generateChart() {
        String dataString = null;
        String optionString = null;
        try {
            Object data = (model.getObject() instanceof SimpleChart)
                    ? ((SimpleChart) model.getObject()).getData()
                    : ((DataSetChart) model.getObject()).getData();
            dataString = MAPPER.writeValueAsString(data);
            optionString = MAPPER.writeValueAsString(model.getObject().getOptions());
        } catch (JsonProcessingException e) {
            LOG.error("Unexpected error during JSON serialization", e);
        }

        return "var " + container.getMarkupId() + " = WicketCharts['" + container.getMarkupId() + "']."
                + model.getObject().getClass().getSimpleName() + '(' + dataString + ", " + optionString + ");";
    }
}
