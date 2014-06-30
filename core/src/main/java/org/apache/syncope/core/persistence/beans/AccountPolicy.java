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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.validation.Valid;

import org.apache.syncope.common.types.PolicyType;

@Entity
public class AccountPolicy extends Policy {

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
    private Set<ExternalResource> resources;

    public AccountPolicy() {
        this(false);
        this.resources = new HashSet<ExternalResource>();
    }

    public AccountPolicy(final boolean global) {
        super();

        this.type = global
                ? PolicyType.GLOBAL_ACCOUNT
                : PolicyType.ACCOUNT;
    }

    public boolean addResource(final ExternalResource resource) {
        return resources.add(resource);
    }

    public boolean removeResource(final ExternalResource resource) {
        return resources.remove(resource);
    }

    public Set<ExternalResource> getResources() {
        return resources;
    }

    public Set<String> getResourceNames() {
        Set<String> result = new HashSet<String>(resources.size());
        for (ExternalResource resource : resources) {
            result.add(resource.getName());
        }

        return result;
    }

    public void setResources(final Collection<ExternalResource> resources) {
        this.resources.clear();
        if (resources != null) {
            resources.addAll(resources);
        }
    }
}
