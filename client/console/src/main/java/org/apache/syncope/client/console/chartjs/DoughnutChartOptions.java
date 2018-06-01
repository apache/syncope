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
 * Provides options, that are available for {@link Doughnut}.
 */
public class DoughnutChartOptions extends PieChartOptions {

    private static final long serialVersionUID = -5356780831848556616L;

    /** The percentage inner cutout. */
    private Integer percentageInnerCutout;

    /**
     * Gets the percentage inner cutout.
     *
     * @return the percentage inner cutout
     */
    public Integer getPercentageInnerCutout() {
        return percentageInnerCutout;
    }

    /**
     * Sets the percentage inner cutout.
     *
     * @param percentageInnerCutout the percentage of the chart that we cut out of the middle (default is 50).
     */
    public void setPercentageInnerCutout(final Integer percentageInnerCutout) {
        this.percentageInnerCutout = percentageInnerCutout;
    }

}
