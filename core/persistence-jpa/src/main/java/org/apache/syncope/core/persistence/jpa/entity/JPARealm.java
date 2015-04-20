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

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.validation.entity.RealmCheck;

@Entity
@Table(name = JPARealm.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "name", "parent_id" }))
@Cacheable
@RealmCheck
public class JPARealm extends AbstractEntity<Long> implements Realm {

    private static final long serialVersionUID = 5533247460239909964L;

    public static final String TABLE = "Realm";

    @Id
    private Long id;

    @Size(min = 1)
    private String name;

    @ManyToOne
    private JPARealm parent;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAPasswordPolicy passwordPolicy;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAAccountPolicy accountPolicy;

    @Override
    public Long getKey() {
        return id;
    }

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

}
