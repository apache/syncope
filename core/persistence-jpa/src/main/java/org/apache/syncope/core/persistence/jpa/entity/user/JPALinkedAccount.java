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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAPrivilege;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.Encryptor;

@Entity
@Table(name = JPALinkedAccount.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "connObjectKeyValue", "resource_id" }))
public class JPALinkedAccount extends AbstractGeneratedKeyEntity implements LinkedAccount {

    private static final long serialVersionUID = -5141654998687601522L;

    public static final String TABLE = "LinkedAccount";

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @NotNull
    private String connObjectKeyValue;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAUser owner;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAExternalResource resource;

    private String username;

    @Enumerated(EnumType.STRING)
    private CipherAlgorithm cipherAlgorithm;

    @Column(nullable = true)
    private String password;

    private Boolean suspended = false;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "account")
    @Valid
    private List<JPALAPlainAttr> plainAttrs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "linked_account_id"),
            inverseJoinColumns =
            @JoinColumn(name = "privilege_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "linked_account_id", "privilege_id" }))
    @Valid
    private Set<JPAPrivilege> privileges = new HashSet<>();

    @Override
    public String getConnObjectKeyValue() {
        return connObjectKeyValue;
    }

    @Override
    public void setConnObjectKeyValue(final String connObjectKeyValue) {
        this.connObjectKeyValue = connObjectKeyValue;
    }

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        checkType(owner, JPAUser.class);
        this.owner = (JPAUser) owner;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        this.resource = (JPAExternalResource) resource;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    @Override
    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        if (this.cipherAlgorithm == null || cipherAlgorithm == null) {
            this.cipherAlgorithm = cipherAlgorithm;
        } else {
            throw new IllegalArgumentException("Cannot override existing cipher algorithm");
        }
    }
    
    @Override
    public boolean canDecodeSecrets() {
        return this.cipherAlgorithm != null && this.cipherAlgorithm.isInvertible();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setEncodedPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        this.password = password;
        this.cipherAlgorithm = cipherAlgoritm;
    }

    @Override
    public void setPassword(final String password) {
        try {
            this.password = ENCRYPTOR.encode(password, cipherAlgorithm == null
                    ? CipherAlgorithm.valueOf(ApplicationContextProvider.getBeanFactory().getBean(ConfParamOps.class).
                    get(AuthContextUtils.getDomain(), "password.cipher.algorithm", CipherAlgorithm.AES.name(),
                            String.class))
                    : cipherAlgorithm);
        } catch (Exception e) {
            LOG.error("Could not encode password", e);
            this.password = null;
        }
    }

    @Override
    public void setSuspended(final Boolean suspended) {
        this.suspended = suspended;
    }

    @Override
    public Boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean add(final LAPlainAttr attr) {
        checkType(attr, JPALAPlainAttr.class);
        return plainAttrs.add((JPALAPlainAttr) attr);
    }

    @Override
    public boolean remove(final LAPlainAttr attr) {
        checkType(attr, JPALAPlainAttr.class);
        return plainAttrs.remove((JPALAPlainAttr) attr);
    }

    @Override
    public Optional<? extends LAPlainAttr> getPlainAttr(final String plainSchema) {
        return getPlainAttrs().stream().filter(plainAttr
                -> plainAttr != null && plainAttr.getSchema() != null
                && plainSchema.equals(plainAttr.getSchema().getKey())).findFirst();
    }

    @Override
    public List<? extends LAPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean add(final Privilege privilege) {
        checkType(privilege, JPAPrivilege.class);
        return privileges.add((JPAPrivilege) privilege);
    }

    @Override
    public Set<? extends Privilege> getPrivileges() {
        return privileges;
    }
}
