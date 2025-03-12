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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAttributable;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.spring.security.AuthContextUtils;

@Entity
@Table(name = JPALinkedAccount.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "connObjectKeyValue", "resource_id" }))
@EntityListeners({ JSONLinkedAccountListener.class })
public class JPALinkedAccount extends AbstractAttributable implements LinkedAccount {

    private static final long serialVersionUID = -5141654998687601522L;

    public static final String TABLE = "LinkedAccount";

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

    private String plainAttrs;

    @Transient
    private final List<PlainAttr> plainAttrsList = new ArrayList<>();

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

    protected String encode(final String value) throws Exception {
        return ApplicationContextProvider.getApplicationContext().getBean(EncryptorManager.class).getInstance().encode(
                value,
                Optional.ofNullable(cipherAlgorithm).
                        orElseGet(() -> CipherAlgorithm.valueOf(
                        ApplicationContextProvider.getBeanFactory().getBean(ConfParamOps.class).get(
                                AuthContextUtils.getDomain(),
                                "password.cipher.algorithm",
                                CipherAlgorithm.AES.name(),
                                String.class))));
    }

    @Override
    public void setPassword(final String password) {
        try {
            this.password = encode(password);
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
    public List<PlainAttr> getPlainAttrsList() {
        return plainAttrsList;
    }

    @Override
    public String getPlainAttrsJSON() {
        return plainAttrs;
    }

    @Override
    public void setPlainAttrsJSON(final String plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    @Override
    public boolean add(final PlainAttr attr) {
        return plainAttrsList.add(attr);
    }

    @Override
    public boolean remove(final PlainAttr attr) {
        return plainAttrsList.removeIf(jsonAttr -> jsonAttr.getSchema().equals(attr.getSchema()));
    }

    @Override
    public Optional<PlainAttr> getPlainAttr(final String plainSchema) {
        return plainAttrsList.stream().
                filter(attr -> plainSchema.equals(attr.getSchema())).
                findFirst();
    }

    @Override
    public List<PlainAttr> getPlainAttrs() {
        return plainAttrsList.stream().toList();
    }
}
