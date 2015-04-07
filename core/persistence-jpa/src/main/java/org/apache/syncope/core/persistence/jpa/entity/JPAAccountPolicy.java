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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.Valid;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;

@Entity
@DiscriminatorValue("AccountPolicy")
public class JPAAccountPolicy extends JPAPolicy implements AccountPolicy {

    private static final long serialVersionUID = -2767606675667839060L;

    /**
     * Resources for alternative user authentication: if empty, only internal storage will be used.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "account_policy_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_name"))
    @Valid
    private Set<JPAExternalResource> resources;

    public JPAAccountPolicy() {
        this(false);
        this.resources = new HashSet<>();
    }

    public JPAAccountPolicy(final boolean global) {
        super();

        this.type = global
                ? PolicyType.GLOBAL_ACCOUNT
                : PolicyType.ACCOUNT;
    }

    @Override
    public boolean addResource(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return resources.add((JPAExternalResource) resource);
    }

    @Override
    public boolean removeResource(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return resources.remove((JPAExternalResource) resource);
    }

    @Override
    public Set<? extends ExternalResource> getResources() {
        return resources;
    }

    @Override
    public Set<String> getResourceNames() {
        return CollectionUtils.collect(getResources(), new Transformer<ExternalResource, String>() {

            @Override
            public String transform(final ExternalResource input) {
                return input.getKey();
            }
        }, new HashSet<String>());
    }
}
