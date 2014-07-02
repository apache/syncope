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
package org.apache.syncope.core.persistence.beans;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.types.AbstractPolicySpec;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.persistence.validation.entity.PolicyCheck;
import org.apache.syncope.core.util.POJOHelper;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@PolicyCheck
public abstract class Policy extends AbstractBaseBean {

    private static final long serialVersionUID = -5844833125843247458L;

    @Id
    private Long id;

    @NotNull
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    protected PolicyType type;

    @Lob
    private String specification;

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PolicyType getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicySpec> T getSpecification(final Class<T> reference) {
        return POJOHelper.deserialize(specification, reference);
    }

    public <T extends AbstractPolicySpec> void setSpecification(final T policy) {
        this.specification = POJOHelper.serialize(policy);
    }
}
