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
package org.apache.syncope.core.persistence.neo4j.entity.policy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jPropagationPolicy.NODE)
public class Neo4jPropagationPolicy extends Neo4jPolicy implements PropagationPolicy {

    private static final long serialVersionUID = 17400846199535L;

    public static final String NODE = "PropagationPolicy";

    @NotNull
    private Boolean fetchAroundProvisioning = true;

    @NotNull
    private Boolean updateDelta = false;

    @NotNull
    private BackOffStrategy backOffStrategy;

    private String backOffParams;

    @Min(1)
    @NotNull
    private Integer maxAttempts = 3;

    @Override
    public boolean isFetchAroundProvisioning() {
        return fetchAroundProvisioning;
    }

    @Override
    public void setFetchAroundProvisioning(final boolean fetchAroundProvisioning) {
        this.fetchAroundProvisioning = fetchAroundProvisioning;
    }

    @Override
    public boolean isUpdateDelta() {
        return updateDelta;
    }

    @Override
    public void setUpdateDelta(final boolean updateDelta) {
        this.updateDelta = updateDelta;
    }

    @Override
    public BackOffStrategy getBackOffStrategy() {
        return backOffStrategy;
    }

    @Override
    public void setBackOffStrategy(final BackOffStrategy backOffStrategy) {
        this.backOffStrategy = backOffStrategy;
    }

    @Override
    public String getBackOffParams() {
        return backOffParams;
    }

    @Override
    public void setBackOffParams(final String backOffParams) {
        this.backOffParams = backOffParams;
    }

    @Override
    public int getMaxAttempts() {
        return Optional.ofNullable(maxAttempts).orElse(3);
    }

    @Override
    public void setMaxAttempts(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
