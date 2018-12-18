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

import java.util.List;

/**
 * Provides some additional point color and point stroke color information.
 */
public abstract class PointColorDataSet extends BaseDataSet {

    private static final long serialVersionUID = 1581171902504828797L;

    /** The point color. */
    private String pointColor = "rgba(220,220,220,1)";

    /** The point stroke color. */
    private String pointStrokeColor = "#fff";

    /**
     * Instantiates a new abstract point color data set.
     *
     * @param data the values
     */
    public PointColorDataSet(final List<? extends Number> data) {
        super(data);
    }

    /**
     * Gets the point color.
     *
     * @return the point color
     */
    public String getPointColor() {
        return pointColor;
    }

    /**
     * Sets the point color.
     *
     * @param pointColor the point color
     */
    public void setPointColor(final String pointColor) {
        this.pointColor = pointColor;
    }

    /**
     * Gets the point stroke color.
     *
     * @return the point stroke color
     */
    public String getPointStrokeColor() {
        return pointStrokeColor;
    }

    /**
     * Sets the point stroke color.
     *
     * @param pointStrokeColor the point stroke color
     */
    public void setPointStrokeColor(final String pointStrokeColor) {
        this.pointStrokeColor = pointStrokeColor;
    }
}
