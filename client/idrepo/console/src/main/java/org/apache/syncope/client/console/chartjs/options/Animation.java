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
public class Animation implements Serializable {

    private static final long serialVersionUID = 3958509378965197343L;

    private Integer duration;

    private String easing;

    private Boolean loop;

    private AnimationCallback onProgress;

    private AnimationCallback onComplete;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(final Integer duration) {
        this.duration = duration;
    }

    public String getEasing() {
        return easing;
    }

    public void setEasing(final String easing) {
        this.easing = easing;
    }

    public Boolean getLoop() {
        return loop;
    }

    public void setLoop(final Boolean loop) {
        this.loop = loop;
    }

    public AnimationCallback getOnProgress() {
        return onProgress;
    }

    public void setOnProgress(final AnimationCallback onProgress) {
        this.onProgress = onProgress;
    }

    public AnimationCallback getOnComplete() {
        return onComplete;
    }

    public void setOnComplete(final AnimationCallback onComplete) {
        this.onComplete = onComplete;
    }
}
