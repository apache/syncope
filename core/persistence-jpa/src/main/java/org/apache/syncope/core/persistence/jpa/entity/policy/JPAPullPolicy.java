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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.policy.PullCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;

@Entity
@Table(name = JPAPullPolicy.TABLE)
public class JPAPullPolicy extends AbstractProvisioningPolicy implements PullPolicy {

    private static final long serialVersionUID = -6090413855809521279L;

    public static final String TABLE = "PullPolicy";

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "pullPolicy")
    private List<JPAPullCorrelationRuleEntity> correlationRules = new ArrayList<>();

    @Override
    public boolean add(final PullCorrelationRuleEntity filter) {
        checkType(filter, JPAPullCorrelationRuleEntity.class);
        return this.correlationRules.add((JPAPullCorrelationRuleEntity) filter);
    }

    @Override
    public Optional<? extends PullCorrelationRuleEntity> getCorrelationRule(final String anyType) {
        return correlationRules.stream().
                filter(rule -> anyType != null && anyType.equals(rule.getAnyType().getKey())).findFirst();
    }

    @Override
    public List<? extends PullCorrelationRuleEntity> getCorrelationRules() {
        return correlationRules;
    }
}
