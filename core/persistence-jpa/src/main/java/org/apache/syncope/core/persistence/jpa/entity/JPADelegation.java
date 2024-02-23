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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.validation.DelegationCheck;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;

@Entity
@Table(name = JPADelegation.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "delegating_id", "delegated_id" }))
@Cacheable
@DelegationCheck
public class JPADelegation extends AbstractGeneratedKeyEntity implements Delegation {

    private static final long serialVersionUID = 17988340419552L;

    public static final String TABLE = "Delegation";

    @ManyToOne(optional = false)
    private JPAUser delegating;

    @ManyToOne(optional = false)
    private JPAUser delegated;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    @OneToMany
    private Set<JPARole> roles = new HashSet<>();

    @Override
    public User getDelegating() {
        return delegating;
    }

    @Override
    public void setDelegating(final User delegating) {
        checkType(delegating, JPAUser.class);
        this.delegating = (JPAUser) delegating;
    }

    @Override
    public User getDelegated() {
        return delegated;
    }

    @Override
    public void setDelegated(final User delegated) {
        checkType(delegated, JPAUser.class);
        this.delegated = (JPAUser) delegated;
    }

    @Override
    public OffsetDateTime getStart() {
        return startDate;
    }

    @Override
    public void setStart(final OffsetDateTime start) {
        this.startDate = start;
    }

    @Override
    public OffsetDateTime getEnd() {
        return endDate;
    }

    @Override
    public void setEnd(final OffsetDateTime end) {
        this.endDate = end;
    }

    @Override
    public boolean add(final Role role) {
        checkType(role, JPARole.class);
        return roles.add((JPARole) role);
    }

    @Override
    public Set<? extends Role> getRoles() {
        return roles;
    }
}
