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

    private Boolean showScale;

    /** The scale override. */
    private Boolean scaleOverride;

    // ** The next three are required if scaleOverride is true **
    /** The scale steps. */
    private Integer scaleSteps;

    /** The scale step width. */
    private Integer scaleStepWidth;

    /** The scale start value. */
    private Integer scaleStartValue;

    /** The scale line color. */
    private String scaleLineColor;

    /** The scale line width. */
    private Integer scaleLineWidth;

    /** The scale show labels. */
    private Boolean scaleShowLabels;

    /** The scale label. */
    private String scaleLabel;

    /** The scale font family. */
    private String scaleFontFamily;

    /** The scale font size. */
    private Integer scaleFontSize;

    /** The scale font style. */
    private String scaleFontStyle;

    /** The scale font color. */
    private String scaleFontColor;

    private Boolean scaleIntegersOnly;

    private Boolean scaleBeginAtZero;

    private Boolean responsive;

    private Boolean maintainAspectRatio;

    private Boolean showTooltips;

    private String[] tooltipEvents = new String[] { "mousemove", "touchstart", "touchmove" };

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

    /**
     * Gets the scale override.
     *
     * @return the scale override
     */
    public Boolean getScaleOverride() {
        return scaleOverride;
    }

    /**
     * Sets the scale override.
     *
     * @param scaleOverride decides if you want to override with a hard coded scale
     */
    public void setScaleOverride(final Boolean scaleOverride) {
        this.scaleOverride = scaleOverride;
    }

    /**
     * Gets the scale steps.
     *
     * @return the scale steps
     */
    public Integer getScaleSteps() {
        return scaleSteps;
    }

    /**
     * Sets the scale steps.
     *
     * @param scaleSteps the number of steps in a hard coded scale (required if scaleOverride == true, default is null).
     */
    public void setScaleSteps(final Integer scaleSteps) {
        this.scaleSteps = scaleSteps;
    }

    /**
     * Gets the scale step width.
     *
     * @return the scale step width
     */
    public Integer getScaleStepWidth() {
        return scaleStepWidth;
    }

    /**
     * Sets the scale step width.
     *
     * @param scaleStepWidth the value jump in the hard coded scale (required if scaleOverride == true, default is
     * null).
     */
    public void setScaleStepWidth(final Integer scaleStepWidth) {
        this.scaleStepWidth = scaleStepWidth;
    }

    /**
     * Gets the scale start value.
     *
     * @return the scale start value
     */
    public Integer getScaleStartValue() {
        return scaleStartValue;
    }

    /**
     * Sets the scale start value.
     *
     * @param scaleStartValue the scale starting value (required if scaleOverride == true, default is null).
     */
    public void setScaleStartValue(final Integer scaleStartValue) {
        this.scaleStartValue = scaleStartValue;
    }

    /**
     * Gets the scale line color.
     *
     * @return the scale line color
     */
    public String getScaleLineColor() {
        return scaleLineColor;
    }

    /**
     * Sets the scale line color.
     *
     * @param scaleLineColor
     * color of the scale line
     */
    public void setScaleLineColor(final String scaleLineColor) {
        this.scaleLineColor = scaleLineColor;
    }

    /**
     * Gets the scale line width.
     *
     * @return the scale line width
     */
    public Integer getScaleLineWidth() {
        return scaleLineWidth;
    }

    /**
     * Sets the scale line width.
     *
     * @param scaleLineWidth the pixel width of the scale line
     */
    public void setScaleLineWidth(final Integer scaleLineWidth) {
        this.scaleLineWidth = scaleLineWidth;
    }

    /**
     * Gets the scale show labels.
     *
     * @return the scale show labels
     */
    public Boolean getScaleShowLabels() {
        return scaleShowLabels;
    }

    /**
     * Sets the scale show labels.
     *
     * @param scaleShowLabels decides whether to show labels on the scale
     */
    public void setScaleShowLabels(final Boolean scaleShowLabels) {
        this.scaleShowLabels = scaleShowLabels;
    }

    /**
     * Gets the scale label.
     *
     * @return the scale label
     */
    public String getScaleLabel() {
        return scaleLabel;
    }

    /**
     * Sets the scale label.
     *
     * @param scaleLabel an interpolated js string that can access value.
     */
    public void setScaleLabel(final String scaleLabel) {
        this.scaleLabel = scaleLabel;
    }

    /**
     * Gets the scale font family.
     *
     * @return the scale font family
     */
    public String getScaleFontFamily() {
        return scaleFontFamily;
    }

    /**
     * Sets the scale font family.
     *
     * @param scaleFontFamily scale label font declaration for the scale label (default is
     * "'Arial'").
     */
    public void setScaleFontFamily(final String scaleFontFamily) {
        this.scaleFontFamily = scaleFontFamily;
    }

    /**
     * Gets the scale font size.
     *
     * @return the scale font size
     */
    public Integer getScaleFontSize() {
        return scaleFontSize;
    }

    /**
     * Sets the scale font size.
     *
     * @param scaleFontSize the scale label font size in pixels
     */
    public void setScaleFontSize(final Integer scaleFontSize) {
        this.scaleFontSize = scaleFontSize;
    }

    /**
     * Gets the scale font style.
     *
     * @return the scale font style
     */
    public String getScaleFontStyle() {
        return scaleFontStyle;
    }

    /**
     * Sets the scale font style.
     *
     * @param scaleFontStyle the scale label font weight style (default is "normal").
     */
    public void setScaleFontStyle(final String scaleFontStyle) {
        this.scaleFontStyle = scaleFontStyle;
    }

    /**
     * Gets the scale font color.
     *
     * @return the scale font color
     */
    public String getScaleFontColor() {
        return scaleFontColor;
    }

    /**
     * Sets the scale font color.
     *
     * @param scaleFontColor the scale label font color (default is "#666").
     */
    public void setScaleFontColor(final String scaleFontColor) {
        this.scaleFontColor = scaleFontColor;
    }

    public Boolean getShowScale() {
        return showScale;
    }

    public void setShowScale(final Boolean showScale) {
        this.showScale = showScale;
    }

    public Boolean getScaleIntegersOnly() {
        return scaleIntegersOnly;
    }

    public void setScaleIntegersOnly(final Boolean scaleIntegersOnly) {
        this.scaleIntegersOnly = scaleIntegersOnly;
    }

    public Boolean getScaleBeginAtZero() {
        return scaleBeginAtZero;
    }

    public void setScaleBeginAtZero(final Boolean scaleBeginAtZero) {
        this.scaleBeginAtZero = scaleBeginAtZero;
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
}
