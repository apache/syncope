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
package org.apache.syncope.core.persistence.jpa.entity.user;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;

@Entity
@Table(name = JPADynRoleMembership.TABLE)
public class JPADynRoleMembership extends AbstractUDynMembership implements DynRoleMembership {

    private static final long serialVersionUID = -7336814163949640354L;

    public static final String TABLE = "DynRoleMembership";

    @OneToOne
    private JPARole role;

    @ManyToMany
    @JoinTable(name = TABLE + "_User", joinColumns =
            @JoinColumn(name = "dynRoleMembership_id"),
            inverseJoinColumns =
            @JoinColumn(name = "user_id"))
    private List<JPAUser> users = new ArrayList<>();

    @Override
    protected List<JPAUser> internalGetUsers() {
        return users;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public void setRole(final Role role) {
        checkType(role, JPARole.class);
        this.role = (JPARole) role;
    }

}
