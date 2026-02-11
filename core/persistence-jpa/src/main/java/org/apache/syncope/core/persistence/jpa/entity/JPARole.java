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
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.jpa.converters.StringSetConverter;

@Entity
@Table(name = JPARole.TABLE)
@Cacheable
public class JPARole extends AbstractProvidedKeyEntity implements Role {

    private static final long serialVersionUID = -7657701119422588832L;

    public static final String TABLE = "SyncopeRole";

    @Convert(converter = StringSetConverter.class)
    @Lob
    private Set<String> entitlements = new HashSet<>();

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

    @Override
    public Set<String> getEntitlements() {
        return entitlements;
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
    public String getAnyLayout() {
        return anyLayout;
    }

    @Override
    public void setAnyLayout(final String anyLayout) {
        this.anyLayout = anyLayout;
    }
}
