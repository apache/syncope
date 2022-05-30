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
package org.apache.syncope.core.persistence.jpa.entity.policy;

import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.BackOffStrategy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;

@Entity
@Table(name = JPAPropagationPolicy.TABLE)
public class JPAPropagationPolicy extends AbstractPolicy implements PropagationPolicy {

    private static final long serialVersionUID = 17400846199535L;

    public static final String TABLE = "PropagationPolicy";

    @Enumerated(EnumType.STRING)
    @NotNull
    private BackOffStrategy backOffStrategy;

    private String backOffParams;

    @NotNull
    private Integer maxAttempts = 3;

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
