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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.validation.entity.RealmCheck;

@Entity
@Table(name = JPARealm.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "id", "parent_id" }))
@Cacheable
@RealmCheck
public class JPARealm extends AbstractGeneratedKeyEntity implements Realm {

    private static final long serialVersionUID = 5533247460239909964L;

    public static final String TABLE = "Realm";

    @Size(min = 1)
    private String name;

    @ManyToOne
    private JPARealm parent;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAPasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAccountPolicy accountPolicy;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "actionClassName")
    @CollectionTable(name = "Realm_actionsClassNames",
            joinColumns =
            @JoinColumn(name = "realm_id", referencedColumnName = "id"))
    private Set<String> actionsClassNames = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "realm")
    private List<JPAAnyTemplateRealm> templates = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "realm_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"))
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
        return getParent() == null
                ? SyncopeConstants.ROOT_REALM
                : StringUtils.appendIfMissing(getParent().getFullPath(), "/") + getName();
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
    public Set<String> getActionsClassNames() {
        return actionsClassNames;
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
    public boolean add(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return resources.add((JPAExternalResource) resource);
    }

    @Override
    public List<String> getResourceKeys() {
        return getResources().stream().map(resource -> resource.getKey()).collect(Collectors.toList());
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return resources;
    }
}
