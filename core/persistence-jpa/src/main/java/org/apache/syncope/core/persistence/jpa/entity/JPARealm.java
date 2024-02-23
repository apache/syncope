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

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.common.validation.RealmCheck;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccessPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAttrReleasePolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAuthPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPATicketExpirationPolicy;

@Entity
@Table(name = JPARealm.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "name", "parent_id" }))
@Cacheable
@RealmCheck
public class JPARealm extends AbstractGeneratedKeyEntity implements Realm {

    private static final long serialVersionUID = 5533247460239909964L;

    public static final String TABLE = "Realm";

    @Size(min = 1)
    private String name;

    @ManyToOne
    private JPARealm parent;

    @Column(nullable = false, unique = true)
    private String fullPath;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAPasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAccountPolicy accountPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAuthPolicy authPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAccessPolicy accessPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAttrReleasePolicy attrReleasePolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPATicketExpirationPolicy ticketExpirationPolicy;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Action",
            joinColumns =
            @JoinColumn(name = "realm_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "realm_id", "implementation_id" }))
    private List<JPAImplementation> actions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "realm")
    private List<JPAAnyTemplateRealm> templates = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "realm_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "realm_id", "resource_id" }))
    private List<JPAExternalResource> resources = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Realm getParent() {
        return parent;
    }

    @Override
    public String getFullPath() {
        return fullPath;
    }

    @Override
    public AccountPolicy getAccountPolicy() {
        return accountPolicy == null && getParent() != null ? getParent().getAccountPolicy() : accountPolicy;
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy == null && getParent() != null ? getParent().getPasswordPolicy() : passwordPolicy;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public void setParent(final Realm parent) {
        checkType(parent, JPARealm.class);
        this.parent = (JPARealm) parent;
    }

    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    @Override
    public void setAccountPolicy(final AccountPolicy accountPolicy) {
        checkType(accountPolicy, JPAAccountPolicy.class);
        this.accountPolicy = (JPAAccountPolicy) accountPolicy;
    }

    @Override
    public void setPasswordPolicy(final PasswordPolicy passwordPolicy) {
        checkType(passwordPolicy, JPAPasswordPolicy.class);
        this.passwordPolicy = (JPAPasswordPolicy) passwordPolicy;
    }

    @Override
    public AuthPolicy getAuthPolicy() {
        return authPolicy;
    }

    @Override
    public void setAuthPolicy(final AuthPolicy authPolicy) {
        checkType(authPolicy, JPAAuthPolicy.class);
        this.authPolicy = (JPAAuthPolicy) authPolicy;
    }

    @Override
    public AccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    @Override
    public void setAccessPolicy(final AccessPolicy accessPolicy) {
        checkType(accessPolicy, JPAAccessPolicy.class);
        this.accessPolicy = (JPAAccessPolicy) accessPolicy;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, JPAImplementation.class);
        checkImplementationType(action, IdRepoImplementationType.LOGIC_ACTIONS);
        return actions.contains((JPAImplementation) action) || actions.add((JPAImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return actions;
    }

    @Override
    public boolean add(final AnyTemplateRealm template) {
        checkType(template, JPAAnyTemplateRealm.class);
        return this.templates.add((JPAAnyTemplateRealm) template);
    }

    @Override
    public Optional<? extends AnyTemplateRealm> getTemplate(final AnyType anyType) {
        return templates.stream().
                filter(template -> anyType != null && anyType.equals(template.getAnyType())).
                findFirst();
    }

    @Override
    public List<? extends AnyTemplateRealm> getTemplates() {
        return templates;
    }

    @Override
    public void setAttrReleasePolicy(final AttrReleasePolicy policy) {
        checkType(policy, JPAAttrReleasePolicy.class);
        this.attrReleasePolicy = (JPAAttrReleasePolicy) policy;
    }

    @Override
    public AttrReleasePolicy getAttrReleasePolicy() {
        return this.attrReleasePolicy;
    }

    @Override
    public TicketExpirationPolicy getTicketExpirationPolicy() {
        return this.ticketExpirationPolicy;
    }

    @Override
    public void setTicketExpirationPolicy(final TicketExpirationPolicy policy) {
        checkType(policy, JPATicketExpirationPolicy.class);
        this.ticketExpirationPolicy = (JPATicketExpirationPolicy) policy;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return resources.contains((JPAExternalResource) resource) || resources.add((JPAExternalResource) resource);
    }

    @Override
    public List<String> getResourceKeys() {
        return getResources().stream().map(ExternalResource::getKey).toList();
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return resources;
    }
}
