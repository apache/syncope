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

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.CorrelationRuleEntity;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyType;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@MappedSuperclass
abstract class AbstractCorrelationRuleEntity extends AbstractGeneratedKeyEntity implements CorrelationRuleEntity {

    private static final long serialVersionUID = 4017405130146577834L;

    @ManyToOne(optional = false)
    private JPAAnyType anyType;

    @ManyToOne(optional = false)
    private JPAImplementation implementation;

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.anyType = (JPAAnyType) anyType;
    }

    @Override
    public Implementation getImplementation() {
        return implementation;
    }

    protected abstract String getImplementationType();

    @Override
    public void setImplementation(final Implementation implementation) {
        checkType(implementation, JPAImplementation.class);
        checkImplementationType(implementation, getImplementationType());
        this.implementation = (JPAImplementation) implementation;
    }
}
