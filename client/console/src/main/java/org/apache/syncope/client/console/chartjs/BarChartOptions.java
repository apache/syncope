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
 * Provides options, that are available for {@link Bar}.
 */
public class BarChartOptions extends ChartOptions {

    private static final long serialVersionUID = -5356780831848556616L;

    /**
     * Whether to show horizontal lines (except X axis).
     */
    private Boolean scaleShowHorizontalLines = true;

    /**
     * Whether to show vertical lines (except Y axis).
     */
    private Boolean scaleShowVerticalLines = true;

    /**
     * A legend template.
     */
    private String legendTemplate = "<ul class=\"<%=name.toLowerCase()%>-legend\">"
            + "<% for (var i=0; i<datasets.length; i++){%><li>"
            + "<span style=\"background-color:<%=datasets[i].fillColor%>\"></span>"
            + "<%if(datasets[i].label){%><%=datasets[i].label%><%}%></li><%}%></ul>";

    /**
     * The bar show stroke.
     */
    private Boolean barShowStroke;

    /**
     * The bar stroke width.
     */
    private Integer barStrokeWidth;

    /** The bar value spacing. */
    private Integer barValueSpacing;

    /** The bar dataset spacing. */
    private Integer barDatasetSpacing;

    public Boolean getScaleShowHorizontalLines() {
        return scaleShowHorizontalLines;
    }

    public void setScaleShowHorizontalLines(final Boolean scaleShowHorizontalLines) {
        this.scaleShowHorizontalLines = scaleShowHorizontalLines;
    }

    public Boolean getScaleShowVerticalLines() {
        return scaleShowVerticalLines;
    }

    public void setScaleShowVerticalLines(final Boolean scaleShowVerticalLines) {
        this.scaleShowVerticalLines = scaleShowVerticalLines;
    }

    public String getLegendTemplate() {
        return legendTemplate;
    }

    public void setLegendTemplate(final String legendTemplate) {
        this.legendTemplate = legendTemplate;
    }

    /**
     * Gets the bar show stroke.
     *
     * @return the bar show stroke
     */
    public Boolean getBarShowStroke() {
        return barShowStroke;
    }

    /**
     * Sets the bar show stroke.
     *
     * @param barShowStroke the new bar show stroke (default is true).
     */
    public void setBarShowStroke(final Boolean barShowStroke) {
        this.barShowStroke = barShowStroke;
    }

    /**
     * Gets the bar stroke width.
     *
     * @return the bar stroke width
     */
    public Integer getBarStrokeWidth() {
        return barStrokeWidth;
    }

    /**
     * Sets the bar stroke width.
     *
     * @param barStrokeWidth the new bar stroke width (default is 2).
     */
    public void setBarStrokeWidth(final Integer barStrokeWidth) {
        this.barStrokeWidth = barStrokeWidth;
    }

    /**
     * Gets the bar value spacing.
     *
     * @return the bar value spacing
     */
    public Integer getBarValueSpacing() {
        return barValueSpacing;
    }

    /**
     * Sets the bar value spacing.
     *
     * @param barValueSpacing the spacing between each of the X value sets (default is 5)
     */
    public void setBarValueSpacing(final Integer barValueSpacing) {
        this.barValueSpacing = barValueSpacing;
    }

    /**
     * Gets the bar dataset spacing.
     *
     * @return the bar dataset spacing
     */
    public Integer getBarDatasetSpacing() {
        return barDatasetSpacing;
    }

    /**
     * Sets the bar dataset spacing.
     *
     * @param barDatasetSpacing the spacing between data sets within X values (default is 1).
     */
    public void setBarDatasetSpacing(final Integer barDatasetSpacing) {
        this.barDatasetSpacing = barDatasetSpacing;
    }
}
