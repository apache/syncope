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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a simple implementation of chart.js pie chart.
 *
 * @see <a href="http://www.chartjs.org/docs/#pieChart">chart.js docs</a>
 */
public class Pie extends SimpleChart<PieChartData, PieChartOptions> implements Serializable {

    private static final long serialVersionUID = -6898362145345731457L;

    @Override
    public PieChartOptions getOptions() {
        if (options == null) {
            options = new PieChartOptions();
        }
        return options;
    }

    @Override
    public List<PieChartData> getData() {
        if (data == null) {
            data = new ArrayList<>();
        }
        return data;
    }
}
