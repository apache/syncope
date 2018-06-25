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

/**
 * Provides chart data used by pie charts.
 */
public class PieChartData extends SimpleColorValueChartData {

    private static final long serialVersionUID = -5122104387810776812L;

    private String label;

    /**
     * Instantiates a new pie chart data.
     *
     * @param value the value
     * @param color the color
     */
    public PieChartData(final Number value, final String color) {
        super(value, color);
    }

    public PieChartData(final Number value, final String color, final String label) {
        super(value, color);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }
}
