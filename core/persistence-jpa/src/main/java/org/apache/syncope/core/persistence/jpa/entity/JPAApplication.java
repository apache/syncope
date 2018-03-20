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
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.jpa.validation.entity.ApplicationCheck;

@Entity
@Table(name = JPAApplication.TABLE)
@ApplicationCheck
public class JPAApplication extends AbstractProvidedKeyEntity implements Application {

    private static final long serialVersionUID = -5951400197744722305L;

    public static final String TABLE = "Application";

    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "application")
    private List<JPAPrivilege> privileges = new ArrayList<>();

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean add(final Privilege privilege) {
        checkType(privilege, JPAPrivilege.class);
        return privileges.contains((JPAPrivilege) privilege) || privileges.add((JPAPrivilege) privilege);
    }

    @Override
    public Optional<? extends Privilege> getPrivilege(final String key) {
        return privileges.stream().filter(privilege -> privilege.getKey().equals(key)).findFirst();
    }

    @Override
    public List<? extends Privilege> getPrivileges() {
        return privileges;
    }

}
