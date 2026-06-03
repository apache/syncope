/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.chartjs.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Scale implements Serializable {

    private static final long serialVersionUID = 1870456183966939178L;

    private Boolean display;

    private Number min;

    private Number max;

    private Boolean beginAtZero;

    private String type;

    public Boolean getDisplay() {
        return display;
    }

    public void setDisplay(final Boolean display) {
        this.display = display;
    }

    public Number getMin() {
        return min;
    }

    public void setMin(final Number min) {
        this.min = min;
    }

    public Number getMax() {
        return max;
    }

    public void setMax(final Number max) {
        this.max = max;
    }

    public Boolean getBeginAtZero() {
        return beginAtZero;
    }

    public void setBeginAtZero(final Boolean beginAtZero) {
        this.beginAtZero = beginAtZero;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }
}
