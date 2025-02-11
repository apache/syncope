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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;

public class DeletingLinkedAccount implements LinkedAccount {

    private static final long serialVersionUID = -6828106363047119713L;

    private final User user;

    private final ExternalResource resource;

    private final String connObjectKeyValue;

    public DeletingLinkedAccount(final User user, final ExternalResource resource, final String connObjectKeyValue) {
        this.user = user;
        this.resource = resource;
        this.connObjectKeyValue = connObjectKeyValue;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getConnObjectKeyValue() {
        return connObjectKeyValue;
    }

    @Override
    public void setConnObjectKeyValue(final String connObjectKeyValue) {
        // unsupported
    }

    @Override
    public User getOwner() {
        return user;
    }

    @Override
    public void setOwner(final User owner) {
        // unsupported
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        // unsupported
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public void setUsername(final String username) {
        // unsupported
    }

    @Override
    public CipherAlgorithm getCipherAlgorithm() {
        return null;
    }

    @Override
    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        // unsupported
    }

    @Override
    public boolean canDecodeSecrets() {
        return false;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public void setEncodedPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        // unsupported
    }

    @Override
    public void setPassword(final String password) {
        // unsupported
    }

    @Override
    public Boolean isSuspended() {
        return null;
    }

    @Override
    public void setSuspended(final Boolean suspended) {
        //
    }

    @Override
    public boolean add(final PlainAttr attr) {
        return false;
    }

    @Override
    public boolean remove(final PlainAttr attr) {
        return false;
    }

    @Override
    public Optional<PlainAttr> getPlainAttr(final String plainSchema) {
        return Optional.empty();
    }

    @Override
    public List<PlainAttr> getPlainAttrs() {
        return List.of();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(user.getKey()).
                append(resource).
                append(connObjectKeyValue).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DeletingLinkedAccount other = (DeletingLinkedAccount) obj;
        return new EqualsBuilder().
                append(user.getKey(), other.user.getKey()).
                append(resource, other.resource).
                append(connObjectKeyValue, other.connObjectKeyValue).
                build();
    }
}
