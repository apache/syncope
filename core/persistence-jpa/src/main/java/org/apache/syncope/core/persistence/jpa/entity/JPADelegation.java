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

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.persistence.jpa.validation.entity.DelegationCheck;

@Entity
@Table(name = JPADelegation.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "delegating_id", "delegated_id" }))
@Cacheable
@DelegationCheck
public class JPADelegation extends AbstractGeneratedKeyEntity implements Delegation {

    public static final String TABLE = "Delegation";

    private static final long serialVersionUID = 17988340419552L;

    @ManyToOne(optional = false)
    private JPAUser delegating;

    @ManyToOne(optional = false)
    private JPAUser delegated;

    @NotNull
    @Column(name = "startDate", nullable = false)
    private OffsetDateTime start;

    @Column(name = "endDate")
    private OffsetDateTime end;

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
        return start;
    }

    @Override
    public void setStart(final OffsetDateTime start) {
        this.start = start;
    }

    @Override
    public OffsetDateTime getEnd() {
        return end;
    }

    @Override
    public void setEnd(final OffsetDateTime end) {
        this.end = end;
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
