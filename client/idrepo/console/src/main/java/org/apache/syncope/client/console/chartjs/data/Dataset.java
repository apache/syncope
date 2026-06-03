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
package org.apache.syncope.client.console.chartjs.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dataset implements Serializable {

    private static final long serialVersionUID = -9143409945075593686L;

    private final List<Number> data = new ArrayList<>();

    private final List<String> label = new ArrayList<>();

    private final List<String> backgroundColor = new ArrayList<>();

    private final List<String> borderColor = new ArrayList<>();

    private Boolean hidden;

    private Double tension;

    public Double getTension() {
        return tension;
    }

    public void setTension(final Double tension) {
        this.tension = tension;
    }

    public List<Number> getData() {
        return data;
    }

    public List<String> getLabel() {
        return label;
    }

    public List<String> getBackgroundColor() {
        return backgroundColor;
    }

    public List<String> getBorderColor() {
        return borderColor;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(final Boolean hidden) {
        this.hidden = hidden;
    }
}
