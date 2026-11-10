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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.syncope.client.console.chartjs.data.ChartData;
import org.apache.syncope.client.console.chartjs.options.ChartOptions;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ChartTest {

    private static final JsonMapper MAPPER = new SyncopeJsonMapper();

    @Test
    void defaults() {
        Chart chart = new Chart();

        assertNotNull(chart);
        assertNull(chart.getType());
        assertNotNull(chart.getData());
        assertNotNull(chart.getOptions());
    }

    @Test
    void getOptionsCreatesInstance() {
        Chart chart = new Chart();

        ChartOptions options = chart.getOptions();
        assertNotNull(options);
        assertSame(options, chart.getOptions());
    }

    @Test
    void settersAndGetters() {
        Chart chart = new Chart();

        chart.setType(ChartType.line);
        assertEquals(ChartType.line, chart.getType());

        ChartData data = new ChartData();
        chart.setData(data);
        assertSame(data, chart.getData());

        ChartOptions options = new ChartOptions();
        chart.setOptions(options);
        assertSame(options, chart.getOptions());
    }

    @Test
    void serialization() throws Exception {
        Chart chart = new Chart();
        chart.setType(ChartType.bar);

        String json = MAPPER.writeValueAsString(chart);
        assertNotNull(json);

        Chart deserialized = MAPPER.readValue(json, Chart.class);
        assertNotNull(deserialized);
        assertEquals(ChartType.bar, deserialized.getType());
        assertNotNull(deserialized.getData());
        assertNotNull(deserialized.getOptions());
    }
}
