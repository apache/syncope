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

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.core.persistence.api.entity.policy.InboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jInboundPolicy.NODE)
public class Neo4jInboundPolicy extends Neo4jPolicy implements InboundPolicy {

    private static final long serialVersionUID = -6090413855809521279L;

    public static final String NODE = "InboundPolicy";

    @NotNull
    private ConflictResolutionAction conflictResolutionAction;

    @Relationship(type = "INBOUND_POLICY", direction = Relationship.Direction.INCOMING)
    private List<Neo4jInboundCorrelationRuleEntity> correlationRules = new ArrayList<>();

    @Override
    public ConflictResolutionAction getConflictResolutionAction() {
        return conflictResolutionAction;
    }

    @Override
    public void setConflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    @Override
    public boolean add(final InboundCorrelationRuleEntity filter) {
        checkType(filter, Neo4jInboundCorrelationRuleEntity.class);
        return this.correlationRules.add((Neo4jInboundCorrelationRuleEntity) filter);
    }

    @Override
    public Optional<? extends InboundCorrelationRuleEntity> getCorrelationRule(final String anyType) {
        return correlationRules.stream().
                filter(rule -> anyType != null && anyType.equals(rule.getAnyType().getKey())).findFirst();
    }

    @Override
    public List<? extends InboundCorrelationRuleEntity> getCorrelationRules() {
        return correlationRules;
    }
}
