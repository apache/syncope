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

import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jPushCorrelationRuleEntity.NODE)
public class Neo4jPushCorrelationRuleEntity extends AbstractCorrelationRuleEntity implements PushCorrelationRuleEntity {

    private static final long serialVersionUID = 4276417265524083919L;

    public static final String NODE = "PushCorrelationRuleEntity";

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jPushPolicy pushPolicy;

    @Override
    protected String getImplementationType() {
        return IdMImplementationType.PUSH_CORRELATION_RULE;
    }

    @Override
    public PushPolicy getPushPolicy() {
        return pushPolicy;
    }

    @Override
    public void setPushPolicy(final PushPolicy pushPolicy) {
        checkType(pushPolicy, Neo4jPushPolicy.class);
        this.pushPolicy = (Neo4jPushPolicy) pushPolicy;
    }
}
