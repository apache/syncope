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
import java.util.Set;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.jpa.entity.user.JPADynRoleMembership;
import org.apache.syncope.core.persistence.jpa.validation.entity.RoleCheck;

@Entity
@Table(name = JPARole.TABLE)
@Cacheable
@RoleCheck
public class JPARole extends AbstractProvidedKeyEntity implements Role {

    private static final long serialVersionUID = -7657701119422588832L;

    public static final String TABLE = "SyncopeRole";

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "entitlement")
    @CollectionTable(name = "SyncopeRole_entitlements",
            joinColumns =
            @JoinColumn(name = "role_id", referencedColumnName = "id"))
    private Set<String> entitlements = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "realm_id"))
    @Valid
    private List<JPARealm> realms = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "role")
    @Valid
    private JPADynRoleMembership dynMembership;

    @Lob
    private String consoleLayoutInfo;

    @Override
    public Set<String> getEntitlements() {
        return entitlements;
    }

    @Override
    public boolean add(final Realm realm) {
        checkType(realm, JPARealm.class);
        return realms.add((JPARealm) realm);
    }

    @Override
    public List<? extends Realm> getRealms() {
        return realms;
    }

    @Override
    public DynRoleMembership getDynMembership() {
        return dynMembership;
    }

    @Override
    public void setDynMembership(final DynRoleMembership dynMembership) {
        checkType(dynMembership, JPADynRoleMembership.class);
        this.dynMembership = (JPADynRoleMembership) dynMembership;
    }

    @Override
    public String getConsoleLayoutInfo() {
        return consoleLayoutInfo;
    }

    @Override
    public void setConsoleLayoutInfo(final String consoleLayoutInfo) {
        this.consoleLayoutInfo = consoleLayoutInfo;
    }

}
