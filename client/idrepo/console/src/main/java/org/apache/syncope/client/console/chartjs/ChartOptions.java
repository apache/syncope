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

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.io.Serializable;

/**
 * Provides some basic options, that are available for all kinds of charts.
 */
public abstract class ChartOptions implements Serializable {

    private static final long serialVersionUID = 2401861279216541412L;

    public static class Axis implements Serializable {

        private static final long serialVersionUID = 1L;

        private Boolean display;

        public Boolean getDisplay() {
            return display;
        }

        public void setDisplay(final Boolean display) {
            this.display = display;
        }
    }

    public static class Scales implements Serializable {

        private static final long serialVersionUID = 1L;

        private Axis x;

        private Axis y;

        public Axis getX() {
            return x;
        }

        public void setX(final Axis x) {
            this.x = x;
        }

        public Axis getY() {
            return y;
        }

        public void setY(final Axis y) {
            this.y = y;
        }
    }

    /** The animation. */
    private Boolean animation;

    /** The animation steps. */
    private Integer animationSteps;

    /** The animation easing. */
    private String animationEasing;

    /** The on animation complete. */
    private String onAnimationComplete;

    @JsonRawValue
    private String customTooltips;

    private Boolean responsive;

    private Boolean maintainAspectRatio;

    private Boolean showTooltips;

    private String[] tooltipEvents = { "mousemove", "touchstart", "touchmove" };

    private String tooltipFillColor;

    private String tooltipFontFamily;

    private Integer tooltipFontSize;

    private String tooltipFontStyle;

    private String tooltipFontColor;

    private String tooltipTitleFontFamily;

    private Integer tooltipTitleFontSize;

    private String tooltipTitleFontStyle;

    private String tooltipTitleFontColor;

    private Integer tooltipYPadding;

    private Integer tooltipXPadding;

    private Integer tooltipCaretSize;

    private Integer tooltipCornerRadius;

    private Integer tooltipXOffset;

    private String tooltipTemplate;

    private String multiTooltipTemplate;

    private Scales scales;

    @JsonRawValue
    private String onAnimationProgress;

    public String getCustomTooltips() {
        return customTooltips;
    }

    public void setCustomTooltips(final String customTooltips) {
        this.customTooltips = customTooltips;
    }

    /**
     * Gets the animation.
     *
     * @return the animation
     */
    public Boolean getAnimation() {
        return animation;
    }

    /**
     * Sets the animation.
     *
     * @param animation decides whether to animate the chart (default is true)
     */
    public void setAnimation(final Boolean animation) {
        this.animation = animation;
    }

    /**
     * Gets the animation steps.
     *
     * @return the animation steps
     */
    public Integer getAnimationSteps() {
        return animationSteps;
    }

    /**
     * Sets the animation steps.
     *
     * @param animationSteps the number of animation steps (default is 60)
     */
    public void setAnimationSteps(final Integer animationSteps) {
        this.animationSteps = animationSteps;
    }

    /**
     * Gets the animation easing.
     *
     * @return the animation easing
     */
    public String getAnimationEasing() {
        return animationEasing;
    }

    /**
     * Sets the animation easing.
     *
     * @param animationEasing the animation easing effect (default is "easeOutQuart")
     */
    public void setAnimationEasing(final String animationEasing) {
        this.animationEasing = animationEasing;
    }

    /**
     * Gets the javascript function that fires on animation complete.
     *
     * @return the onAnimationComplete javascript function
     */
    public String getOnAnimationComplete() {
        return onAnimationComplete;
    }

    /**
     * Sets the onAnimationComplete javascript function.
     *
     * @param onAnimationComplete a function, that fires when the animation is complete
     */
    public void setOnAnimationComplete(final String onAnimationComplete) {
        this.onAnimationComplete = onAnimationComplete;
    }

    public Boolean getResponsive() {
        return responsive;
    }

    public void setResponsive(final Boolean responsive) {
        this.responsive = responsive;
    }

    public Boolean getMaintainAspectRatio() {
        return maintainAspectRatio;
    }

    public void setMaintainAspectRatio(final Boolean maintainAspectRatio) {
        this.maintainAspectRatio = maintainAspectRatio;
    }

    public Boolean getShowTooltips() {
        return showTooltips;
    }

    public void setShowTooltips(final Boolean showTooltips) {
        this.showTooltips = showTooltips;
    }

    public String[] getTooltipEvents() {
        return tooltipEvents;
    }

    public void setTooltipEvents(final String[] tooltipEvents) {
        this.tooltipEvents = tooltipEvents;
    }

    public String getTooltipFillColor() {
        return tooltipFillColor;
    }

    public void setTooltipFillColor(final String tooltipFillColor) {
        this.tooltipFillColor = tooltipFillColor;
    }

    public String getTooltipFontFamily() {
        return tooltipFontFamily;
    }

    public void setTooltipFontFamily(final String tooltipFontFamily) {
        this.tooltipFontFamily = tooltipFontFamily;
    }

    public Integer getTooltipFontSize() {
        return tooltipFontSize;
    }

    public void setTooltipFontSize(final Integer tooltipFontSize) {
        this.tooltipFontSize = tooltipFontSize;
    }

    public String getTooltipFontStyle() {
        return tooltipFontStyle;
    }

    public void setTooltipFontStyle(final String tooltipFontStyle) {
        this.tooltipFontStyle = tooltipFontStyle;
    }

    public String getTooltipFontColor() {
        return tooltipFontColor;
    }

    public void setTooltipFontColor(final String tooltipFontColor) {
        this.tooltipFontColor = tooltipFontColor;
    }

    public String getTooltipTitleFontFamily() {
        return tooltipTitleFontFamily;
    }

    public void setTooltipTitleFontFamily(final String tooltipTitleFontFamily) {
        this.tooltipTitleFontFamily = tooltipTitleFontFamily;
    }

    public Integer getTooltipTitleFontSize() {
        return tooltipTitleFontSize;
    }

    public void setTooltipTitleFontSize(final Integer tooltipTitleFontSize) {
        this.tooltipTitleFontSize = tooltipTitleFontSize;
    }

    public String getTooltipTitleFontStyle() {
        return tooltipTitleFontStyle;
    }

    public void setTooltipTitleFontStyle(final String tooltipTitleFontStyle) {
        this.tooltipTitleFontStyle = tooltipTitleFontStyle;
    }

    public String getTooltipTitleFontColor() {
        return tooltipTitleFontColor;
    }

    public void setTooltipTitleFontColor(final String tooltipTitleFontColor) {
        this.tooltipTitleFontColor = tooltipTitleFontColor;
    }

    public Integer getTooltipYPadding() {
        return tooltipYPadding;
    }

    public void setTooltipYPadding(final Integer tooltipYPadding) {
        this.tooltipYPadding = tooltipYPadding;
    }

    public Integer getTooltipXPadding() {
        return tooltipXPadding;
    }

    public void setTooltipXPadding(final Integer tooltipXPadding) {
        this.tooltipXPadding = tooltipXPadding;
    }

    public Integer getTooltipCaretSize() {
        return tooltipCaretSize;
    }

    public void setTooltipCaretSize(final Integer tooltipCaretSize) {
        this.tooltipCaretSize = tooltipCaretSize;
    }

    public Integer getTooltipCornerRadius() {
        return tooltipCornerRadius;
    }

    public void setTooltipCornerRadius(final Integer tooltipCornerRadius) {
        this.tooltipCornerRadius = tooltipCornerRadius;
    }

    public Integer getTooltipXOffset() {
        return tooltipXOffset;
    }

    public void setTooltipXOffset(final Integer tooltipXOffset) {
        this.tooltipXOffset = tooltipXOffset;
    }

    public String getTooltipTemplate() {
        return tooltipTemplate;
    }

    public void setTooltipTemplate(final String tooltipTemplate) {
        this.tooltipTemplate = tooltipTemplate;
    }

    public String getMultiTooltipTemplate() {
        return multiTooltipTemplate;
    }

    public void setMultiTooltipTemplate(final String multiTooltipTemplate) {
        this.multiTooltipTemplate = multiTooltipTemplate;
    }

    public String getOnAnimationProgress() {
        return onAnimationProgress;
    }

    public void setOnAnimationProgress(final String onAnimationProgress) {
        this.onAnimationProgress = onAnimationProgress;
    }

    /**
     * Tests whether this chart is responsive or not.
     *
     * @return true if its responsive, false otherwise.
     */
    public boolean isResponsive() {
        return responsive;
    }

    /**
     * Sets the responsive option for this chart.
     *
     * @param responsive the responsive option value.
     */
    public void setResponsive(final boolean responsive) {
        this.responsive = responsive;
    }

    public Scales getScales() {
        return scales;
    }

    public void setScales(final Scales scales) {
        this.scales = scales;
    }
}
