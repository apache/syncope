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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.syncope.common.lib.types.BackOffStrategy;

@Schema(allOf = { PolicyTO.class }, discriminatorProperty = "_class")
public class PropagationPolicyTO extends PolicyTO {

    private static final long serialVersionUID = 10604950933449L;

    private boolean fetchAroundProvisioning = true;

    private boolean updateDelta = false;

    private BackOffStrategy backOffStrategy = BackOffStrategy.FIXED;

    private String backOffParams = BackOffStrategy.FIXED.getDefaultBackOffParams();

    private int maxAttempts = 3;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.policy.PropagationPolicyTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public boolean isFetchAroundProvisioning() {
        return fetchAroundProvisioning;
    }

    public void setFetchAroundProvisioning(final boolean fetchAroundProvisioning) {
        this.fetchAroundProvisioning = fetchAroundProvisioning;
    }

    public boolean isUpdateDelta() {
        return updateDelta;
    }

    public void setUpdateDelta(final boolean updateDelta) {
        this.updateDelta = updateDelta;
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
