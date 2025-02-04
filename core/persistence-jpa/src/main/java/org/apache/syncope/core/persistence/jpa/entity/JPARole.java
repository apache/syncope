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

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPARole.TABLE)
@Cacheable
public class JPARole extends AbstractProvidedKeyEntity implements Role {

    private static final long serialVersionUID = -7657701119422588832L;

    public static final String TABLE = "SyncopeRole";

    protected static final TypeReference<Set<String>> TYPEREF = new TypeReference<Set<String>>() {
    };

    @Lob
    private String entitlements;

    @Transient
    private Set<String> entitlementsSet = new HashSet<>();

    private String dynMembershipCond;

    @Lob
    private String anyLayout;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "realm_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "role_id", "realm_id" }))
    @Valid
    private List<JPARealm> realms = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "dynamicRealm_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "role_id", "dynamicRealm_id" }))
    @Valid
    private List<JPADynRealm> dynRealms = new ArrayList<>();

    @Override
    public Set<String> getEntitlements() {
        return entitlementsSet;
    }

    @Override
    public String getDynMembershipCond() {
        return dynMembershipCond;
    }

    @Override
    public void setDynMembershipCond(final String dynMembershipCond) {
        this.dynMembershipCond = dynMembershipCond;
    }

    @Override
    public boolean add(final Realm realm) {
        checkType(realm, JPARealm.class);
        return realms.contains((JPARealm) realm) || realms.add((JPARealm) realm);
    }

    @Override
    public List<? extends Realm> getRealms() {
        return realms;
    }

    @Override
    public boolean add(final DynRealm dynamicRealm) {
        checkType(dynamicRealm, JPADynRealm.class);
        return dynRealms.contains((JPADynRealm) dynamicRealm) || dynRealms.add((JPADynRealm) dynamicRealm);
    }

    @Override
    public List<? extends DynRealm> getDynRealms() {
        return dynRealms;
    }

    @Override
    public String getAnyLayout() {
        return anyLayout;
    }

    @Override
    public void setAnyLayout(final String anyLayout) {
        this.anyLayout = anyLayout;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getEntitlements().clear();
        }
        if (entitlements != null) {
            getEntitlements().addAll(POJOHelper.deserialize(entitlements, TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
        entitlements = POJOHelper.serialize(getEntitlements());
    }
}
