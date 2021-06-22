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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
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
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "startDate")
    private Date start;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "endDate")
    private Date end;

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
    public Date getStart() {
        return start == null
                ? null
                : new Date(start.getTime());
    }

    @Override
    public void setStart(final Date start) {
        this.start = start == null
                ? null
                : new Date(start.getTime());
    }

    @Override
    public Date getEnd() {
        return end == null
                ? null
                : new Date(end.getTime());
    }

    @Override
    public void setEnd(final Date end) {
        this.end = end == null
                ? null
                : new Date(end.getTime());
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
