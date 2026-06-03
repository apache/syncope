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

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class ChartJSPanel extends Panel {

    private static final long serialVersionUID = -8670277955339192068L;

    private final IModel<Chart> model;

    private final WebMarkupContainer container;

    private final ChartJSRenderer renderer = new ChartJSRenderer();

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
        return renderer.render(markupId, model.getObject());
    }
}
