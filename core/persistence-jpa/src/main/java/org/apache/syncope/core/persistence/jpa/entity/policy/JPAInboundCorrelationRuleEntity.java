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

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.persistence.api.entity.policy.InboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;

@Entity
@Table(name = JPAInboundCorrelationRuleEntity.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "inboundPolicy_id", "anyType_id" }))
public class JPAInboundCorrelationRuleEntity
        extends AbstractCorrelationRuleEntity
        implements InboundCorrelationRuleEntity {

    private static final long serialVersionUID = 4276417265524083919L;

    public static final String TABLE = "InboundCorrelationRuleEntity";

    @ManyToOne(optional = false)
    private JPAInboundPolicy inboundPolicy;

    @Override
    protected String getImplementationType() {
        return IdMImplementationType.INBOUND_CORRELATION_RULE;
    }

    @Override
    public InboundPolicy getInboundPolicy() {
        return inboundPolicy;
    }

    @Override
    public void setInboundPolicy(final InboundPolicy inboundPolicy) {
        checkType(inboundPolicy, JPAInboundPolicy.class);
        this.inboundPolicy = (JPAInboundPolicy) inboundPolicy;
    }
}
