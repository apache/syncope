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
import java.util.stream.Collectors;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;

@Entity
@Table(name = JPAAccountPolicy.TABLE)
public class JPAAccountPolicy extends AbstractPolicy implements AccountPolicy {

    private static final long serialVersionUID = -2767606675667839060L;

    public static final String TABLE = "AccountPolicy";

    @Basic
    @Min(0)
    @Max(1)
    private Integer propagateSuspension;

    private int maxAuthenticationAttempts;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "accountPolicy")
    private List<JPAAccountRuleConfInstance> ruleConfs = new ArrayList<>();

    /**
     * Resources for alternative user authentication: if empty, only internal storage will be used.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "accountPolicy_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"))
    private Set<JPAExternalResource> resources = new HashSet<>();

    @Override
    public boolean isPropagateSuspension() {
        return isBooleanAsInteger(propagateSuspension);
    }

    @Override
    public void setPropagateSuspension(final boolean propagateSuspension) {
        this.propagateSuspension = getBooleanAsInteger(propagateSuspension);
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
    public boolean add(final AccountRuleConf accountRuleConf) {
        if (accountRuleConf == null) {
            return false;
        }

        JPAAccountRuleConfInstance instance = new JPAAccountRuleConfInstance();
        instance.setAccountPolicy(this);
        instance.setInstance(accountRuleConf);

        return ruleConfs.add(instance);
    }

    @Override
    public void removeAllRuleConfs() {
        ruleConfs.clear();
    }

    @Override
    public List<AccountRuleConf> getRuleConfs() {
        return ruleConfs.stream().map(input -> input.getInstance()).collect(Collectors.toList());
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

    @Override
    public Set<String> getResourceKeys() {
        return getResources().stream().map(resource -> resource.getKey()).collect(Collectors.toSet());
    }
}
