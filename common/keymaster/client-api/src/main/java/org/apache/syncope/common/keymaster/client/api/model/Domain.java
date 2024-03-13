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
package org.apache.syncope.common.keymaster.client.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class", "key" })
public abstract class Domain implements Serializable {

    private static final long serialVersionUID = -5881851479361505961L;

    private static final Logger LOG = LoggerFactory.getLogger(Domain.class);

    protected abstract static class Builder<D extends Domain, B extends Builder<D, B>> {

        protected final D domain;

        Builder(final D domain, final String key) {
            this.domain = domain;
            this.domain.key = key;
        }

        @SuppressWarnings("unchecked")
        public B adminPassword(final String adminPassword) {
            this.domain.adminPassword = adminPassword;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B adminCipherAlgorithm(final CipherAlgorithm adminCipherAlgorithm) {
            this.domain.adminCipherAlgorithm = adminCipherAlgorithm;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B content(final String content) {
            this.domain.content = content;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B keymasterConfParams(final String keymasterConfParams) {
            this.domain.keymasterConfParams = keymasterConfParams;
            return (B) this;
        }

        public D build() {
            return this.domain;
        }
    }

    protected static String read(final String filename) {
        String read = null;
        try {
            read = IOUtils.toString(Domain.class.getResourceAsStream('/' + filename));
        } catch (IOException e) {
            LOG.error("Could not read {}", filename, e);
        }

        return read;
    }

    protected String key;

    protected String adminPassword;

    protected CipherAlgorithm adminCipherAlgorithm = CipherAlgorithm.SHA512;

    protected String content;

    protected String keymasterConfParams;

    protected boolean deployed = false;

    @JsonProperty("_class")
    public String getDiscriminator() {
        return getClass().getName();
    }

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    public String getKey() {
        return key;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public CipherAlgorithm getAdminCipherAlgorithm() {
        return adminCipherAlgorithm;
    }

    public void setAdminCipherAlgorithm(final CipherAlgorithm adminCipherAlgorithm) {
        this.adminCipherAlgorithm = adminCipherAlgorithm;
    }

    protected abstract String defaultContentFile();

    public String getContent() {
        if (content == null) {
            content = read(defaultContentFile());
        }

        return content;
    }

    public String getKeymasterConfParams() {
        if (keymasterConfParams == null) {
            keymasterConfParams = read("defaultKeymasterConfParams.json");
        }

        return keymasterConfParams;
    }

    public boolean isDeployed() {
        return deployed;
    }

    public void setDeployed(final boolean deployed) {
        this.deployed = deployed;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(adminPassword).
                append(adminCipherAlgorithm).
                append(content).
                append(keymasterConfParams).
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
        final Domain other = (Domain) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(adminPassword, other.adminPassword).
                append(adminCipherAlgorithm, other.adminCipherAlgorithm).
                append(content, other.content).
                append(keymasterConfParams, other.keymasterConfParams).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(key).
                append(adminPassword).
                append(adminCipherAlgorithm).
                append(content).
                append(keymasterConfParams).
                build();
    }
}
