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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.policy.CorrelationRule;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;

@Entity
@Table(name = JPAPullPolicy.TABLE)
public class JPAPullPolicy extends AbstractPolicy implements PullPolicy {

    private static final long serialVersionUID = -6090413855809521279L;

    public static final String TABLE = "PullPolicy";

    @Enumerated(EnumType.STRING)
    @NotNull
    private ConflictResolutionAction conflictResolutionAction;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "pullPolicy")
    private List<JPACorrelationRule> correlationRules = new ArrayList<>();

    @Override
    public ConflictResolutionAction getConflictResolutionAction() {
        return conflictResolutionAction;
    }

    @Override
    public void setConflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    @Override
    public boolean add(final CorrelationRule filter) {
        checkType(filter, JPACorrelationRule.class);
        return this.correlationRules.add((JPACorrelationRule) filter);
    }

    @Override
    public Optional<? extends CorrelationRule> getCorrelationRule(final AnyType anyType) {
        return correlationRules.stream().
                filter(rule -> anyType != null && anyType.equals(rule.getAnyType())).findFirst();
    }

    @Override
    public List<? extends CorrelationRule> getCorrelationRules() {
        return correlationRules;
    }
}
