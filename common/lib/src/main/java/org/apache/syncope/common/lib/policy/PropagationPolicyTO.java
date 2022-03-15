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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.syncope.common.lib.types.BackOffStrategy;

@Schema(allOf = { PolicyTO.class })
public class PropagationPolicyTO extends PolicyTO {

    private static final long serialVersionUID = 10604950933449L;

    private BackOffStrategy backOffStrategy = BackOffStrategy.FIXED;

    private String backOffParams = BackOffStrategy.FIXED.getDefaultBackOffParams();

    private int maxAttempts = 3;

    @XmlTransient
    @JsonProperty("@class")
    @Schema(name = "@class", required = true, example = "org.apache.syncope.common.lib.policy.PropagationPolicyTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public BackOffStrategy getBackOffStrategy() {
        return backOffStrategy;
    }

    public void setBackOffStrategy(final BackOffStrategy backOffStrategy) {
        this.backOffStrategy = backOffStrategy;
        this.backOffParams = backOffStrategy.getDefaultBackOffParams();
    }

    public String getBackOffParams() {
        return backOffParams;
    }

    public void setBackOffParams(final String backOffParams) {
        this.backOffParams = backOffParams;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
