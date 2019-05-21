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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;

@Entity
@Table(name = JPAAccountPolicy.TABLE)
public class JPAAccountPolicy extends AbstractPolicy implements AccountPolicy {

    private static final long serialVersionUID = -2767606675667839060L;

    public static final String TABLE = "AccountPolicy";

    @NotNull
    private Boolean propagateSuspension = false;

    private int maxAuthenticationAttempts;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Rule",
            joinColumns =
            @JoinColumn(name = "policy_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "policy_id", "implementation_id" }))
    private List<JPAImplementation> rules = new ArrayList<>();

    /**
     * Resources for alternative user authentication: if empty, only internal storage will be used.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "accountPolicy_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "accountPolicy_id", "resource_id" }))
    private Set<JPAExternalResource> resources = new HashSet<>();

    @Override
    public boolean isPropagateSuspension() {
        return propagateSuspension;
    }

    @Override
    public void setPropagateSuspension(final boolean propagateSuspension) {
        this.propagateSuspension = propagateSuspension;
    }

    @Override
    public int getMaxAuthenticationAttempts() {
        return maxAuthenticationAttempts;
    }

    @Override
    public void setMaxAuthenticationAttempts(final int maxAuthenticationAttempts) {
        this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    @Override
    public boolean add(final Implementation rule) {
        checkType(rule, JPAImplementation.class);
        checkImplementationType(rule, IdRepoImplementationType.ACCOUNT_RULE);
        return rules.contains((JPAImplementation) rule) || rules.add((JPAImplementation) rule);
    }

    @Override
    public List<? extends Implementation> getRules() {
        return rules;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return resources.contains((JPAExternalResource) resource) || resources.add((JPAExternalResource) resource);
    }

    @Override
    public Set<? extends ExternalResource> getResources() {
        return resources;
    }
}
