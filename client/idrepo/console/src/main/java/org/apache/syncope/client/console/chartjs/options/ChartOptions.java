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
public class ChartOptions implements Serializable {

    private static final long serialVersionUID = -424268454800409829L;

    private Boolean responsive;

    private Boolean maintainAspectRatio;

    private Animation animation;

    private Plugins plugins;

    private Scales scales;

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

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(final Animation animation) {
        this.animation = animation;
    }

    public Plugins getPlugins() {
        return plugins;
    }

    public void setPlugins(final Plugins plugins) {
        this.plugins = plugins;
    }

    public Scales getScales() {
        return scales;
    }

    public void setScales(final Scales scales) {
        this.scales = scales;
    }
}
