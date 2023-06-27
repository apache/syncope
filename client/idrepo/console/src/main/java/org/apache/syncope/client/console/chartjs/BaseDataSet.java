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
import java.util.List;

/**
 * Provides the simplest kind of a dataset.
 * Besides the list of data itself, it contains default values for fill - and stroke color.
 */
public abstract class BaseDataSet implements Serializable {

    private static final long serialVersionUID = 1581171902504828797L;

    private String backgroundColor = "rgba(220,220,220,0.5)";

    private String borderColor = "rgba(220,220,220,1)";

    /** The data. */
    private final List<? extends Number> data;

    /**
     * Instantiates a new abstract base data set.
     *
     * @param data the data values
     */
    public BaseDataSet(final List<? extends Number> data) {
        this.data = data;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public BaseDataSet setBackgroundColor(final String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public BaseDataSet setBorderColor(final String borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    public List<? extends Number> getData() {
        return data;
    }
}
