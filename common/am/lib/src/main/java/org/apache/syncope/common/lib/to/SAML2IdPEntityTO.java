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
package org.apache.syncope.common.lib.to;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SAML2IdPEntityTO extends SAML2EntityTO {

    private static final long serialVersionUID = 7215073386484048953L;

    public static class Builder extends SAML2EntityTO.Builder<SAML2IdPEntityTO, Builder> {

        @Override
        protected SAML2IdPEntityTO newInstance() {
            return new SAML2IdPEntityTO();
        }

        public Builder metadata(final String metadata) {
            getInstance().setMetadata(metadata);
            return this;
        }

        public Builder signingCertificate(final String signingCertificate) {
            getInstance().setSigningCertificate(signingCertificate);
            return this;
        }

        public Builder signingKey(final String signingKey) {
            getInstance().setSigningKey(signingKey);
            return this;
        }

        public Builder encryptionCertificate(final String encryptionCertificate) {
            getInstance().setEncryptionCertificate(encryptionCertificate);
            return this;
        }

        public Builder encryptionKey(final String encryptionKey) {
            getInstance().setEncryptionKey(encryptionKey);
            return this;
        }
    }

    private String metadata;

    private String signingCertificate;

    private String signingKey;

    private String encryptionCertificate;

    private String encryptionKey;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(final String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public String getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(final String signingKey) {
        this.signingKey = signingKey;
    }

    public String getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public void setEncryptionCertificate(final String encryptionCertificate) {
        this.encryptionCertificate = encryptionCertificate;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(final String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(metadata).
                append(encryptionCertificate).
                append(encryptionKey).
                append(signingCertificate).
                append(signingKey).
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
        SAML2IdPEntityTO other = (SAML2IdPEntityTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(metadata, other.metadata).
                append(encryptionCertificate, other.encryptionCertificate).
                append(encryptionKey, other.encryptionKey).
                append(signingCertificate, other.signingCertificate).
                append(signingKey, other.signingKey).
                build();
    }
}
