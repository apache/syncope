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
 * Provides options for {@link Line}.
 */
public class LineChartOptions extends ChartOptions {

    private static final long serialVersionUID = -5356780831848556616L;

    private Boolean scaleShowHorizontalLines = true;

    private Boolean scaleShowVerticalLines = true;

    private Double bezierCurveTension = 0.4;

    private Integer pointHitDetectionRadius = 20;

    private String legendTemplate = "<ul class=\"<%=name.toLowerCase()%>-legend\">"
            + "<% for (var i=0; i<datasets.length; i++){%><li>"
            + "<span style=\"background-color:<%=datasets[i].strokeColor%>\"></span>"
            + "<%if(datasets[i].label){%><%=datasets[i].label%><%}%></li><%}%></ul>";

    /** The bezier curve. */
    private Boolean bezierCurve;

    /** The point dot. */
    private Boolean pointDot;

    /** The point dot radius. */
    private Integer pointDotRadius;

    /** The point dot stroke width. */
    private Integer pointDotStrokeWidth;

    /** The dataset stroke. */
    private Boolean datasetStroke;

    /** The dataset stroke width. */
    private Integer datasetStrokeWidth;

    /** The dataset fill. */
    private Boolean datasetFill;

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

    public Double getBezierCurveTension() {
        return bezierCurveTension;
    }

    public void setBezierCurveTension(final Double bezierCurveTension) {
        this.bezierCurveTension = bezierCurveTension;
    }

    public Integer getPointHitDetectionRadius() {
        return pointHitDetectionRadius;
    }

    public void setPointHitDetectionRadius(final Integer pointHitDetectionRadius) {
        this.pointHitDetectionRadius = pointHitDetectionRadius;
    }

    public String getLegendTemplate() {
        return legendTemplate;
    }

    public void setLegendTemplate(final String legendTemplate) {
        this.legendTemplate = legendTemplate;
    }

    /**
     * Gets the bezier curve.
     *
     * @return the bezier curve
     */
    public Boolean getBezierCurve() {
        return bezierCurve;
    }

    /**
     * Sets the bezier curve.
     *
     * @param bezierCurve decides whether the line is curved between points (default is true).
     */
    public void setBezierCurve(final Boolean bezierCurve) {
        this.bezierCurve = bezierCurve;
    }

    /**
     * Gets the point dot.
     *
     * @return the point dot
     */
    public Boolean getPointDot() {
        return pointDot;
    }

    /**
     * Sets the point dot.
     *
     * @param pointDot decides whether to show a dot for each point (default is true).
     */
    public void setPointDot(final Boolean pointDot) {
        this.pointDot = pointDot;
    }

    /**
     * Gets the point dot radius.
     *
     * @return the point dot radius
     */
    public Integer getPointDotRadius() {
        return pointDotRadius;
    }

    /**
     * Sets the point dot radius.
     *
     * @param pointDotRadius the new point dot radius (default is 3).
     */
    public void setPointDotRadius(final Integer pointDotRadius) {
        this.pointDotRadius = pointDotRadius;
    }

    /**
     * Gets the point dot stroke width.
     *
     * @return the point dot stroke width
     */
    public Integer getPointDotStrokeWidth() {
        return pointDotStrokeWidth;
    }

    /**
     * Sets the point dot stroke width.
     *
     * @param pointDotStrokeWidth the new point dot stroke width (default is 1).
     */
    public void setPointDotStrokeWidth(final Integer pointDotStrokeWidth) {
        this.pointDotStrokeWidth = pointDotStrokeWidth;
    }

    /**
     * Gets the dataset stroke.
     *
     * @return the dataset stroke
     */
    public Boolean getDatasetStroke() {
        return datasetStroke;
    }

    /**
     * Sets the dataset stroke.
     *
     * @param datasetStroke decides whether to show a stroke for datasets (default is true)
     */
    public void setDatasetStroke(final Boolean datasetStroke) {
        this.datasetStroke = datasetStroke;
    }

    /**
     * Gets the dataset stroke width.
     *
     * @return the dataset stroke width
     */
    public Integer getDatasetStrokeWidth() {
        return datasetStrokeWidth;
    }

    /**
     * Sets the dataset stroke width.
     *
     * @param datasetStrokeWidth the new dataset stroke width (default is 2).
     */
    public void setDatasetStrokeWidth(final Integer datasetStrokeWidth) {
        this.datasetStrokeWidth = datasetStrokeWidth;
    }

    /**
     * Gets the dataset fill.
     *
     * @return the dataset fill
     */
    public Boolean getDatasetFill() {
        return datasetFill;
    }

    /**
     * Sets the dataset fill.
     *
     * @param datasetFill whether to fill the dataset with a color (default is true)
     */
    public void setDatasetFill(final Boolean datasetFill) {
        this.datasetFill = datasetFill;
    }
}
