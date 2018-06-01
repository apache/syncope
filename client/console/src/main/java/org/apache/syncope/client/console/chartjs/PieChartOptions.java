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
 * Provides options, that are available for {@link Pie}.
 */
public class PieChartOptions extends ChartOptions {

    private static final long serialVersionUID = -5356780831848556616L;

    /** The segment show stroke. */
    private Boolean segmentShowStroke;

    /** The segment stroke color. */
    private String segmentStrokeColor;

    /** The segment stroke width. */
    private Integer segmentStrokeWidth;

    /** The animate rotate. */
    private Boolean animateRotate;

    /** The animate scale. */
    private Boolean animateScale;

    private String legendTemplate = "<ul class=\"<%=name.toLowerCase()%>-legend\">"
            + "<% for (var i=0; i<segments.length; i++){%><li>"
            + "<span style=\"background-color:<%=segments[i].fillColor%>\">"
            + "<%if(segments[i].label){%><%=segments[i].label%><%}%></span></li><%}%></ul>";

    public String getLegendTemplate() {
        return legendTemplate;
    }

    public void setLegendTemplate(final String legendTemplate) {
        this.legendTemplate = legendTemplate;
    }

    /**
     * Gets the segment show stroke.
     *
     * @return the segment show stroke
     */
    public Boolean getSegmentShowStroke() {
        return segmentShowStroke;
    }

    /**
     * Sets the segment show stroke.
     *
     * @param segmentShowStroke decides whether we should show a stroke on each segment (default is true)
     */
    public void setSegmentShowStroke(final Boolean segmentShowStroke) {
        this.segmentShowStroke = segmentShowStroke;
    }

    /**
     * Gets the segment stroke color.
     *
     * @return the segment stroke color
     */
    public String getSegmentStrokeColor() {
        return segmentStrokeColor;
    }

    /**
     * Sets the segment stroke color.
     *
     * @param segmentStrokeColor the new segment stroke color (default is "#fff").
     */
    public void setSegmentStrokeColor(final String segmentStrokeColor) {
        this.segmentStrokeColor = segmentStrokeColor;
    }

    /**
     * Gets the segment stroke width.
     *
     * @return the segment stroke width
     */
    public Integer getSegmentStrokeWidth() {
        return segmentStrokeWidth;
    }

    /**
     * Sets the segment stroke width.
     *
     * @param segmentStrokeWidth the new segment stroke width (default is 2).
     */
    public void setSegmentStrokeWidth(final Integer segmentStrokeWidth) {
        this.segmentStrokeWidth = segmentStrokeWidth;
    }

    /**
     * Gets the animate rotate.
     *
     * @return the animate rotate
     */
    public Boolean getAnimateRotate() {
        return animateRotate;
    }

    /**
     * Sets the animate rotate.
     *
     * @param animateRotate decides whether we animate the rotation of the pie (default is true).
     */
    public void setAnimateRotate(final Boolean animateRotate) {
        this.animateRotate = animateRotate;
    }

    /**
     * Gets the animate scale.
     *
     * @return the animate scale
     */
    public Boolean getAnimateScale() {
        return animateScale;
    }

    /**
     * Sets the animate scale.
     *
     * @param animateScale decides whether we animate scaling the Pie from the center (default is false).
     */
    public void setAnimateScale(final Boolean animateScale) {
        this.animateScale = animateScale;
    }
}
