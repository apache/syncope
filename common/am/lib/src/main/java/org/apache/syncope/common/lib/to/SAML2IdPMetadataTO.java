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

import javax.ws.rs.PathParam;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "saml2idpMetadata")
@XmlType
public class SAML2IdPMetadataTO extends BaseBean implements EntityTO {

    private static final long serialVersionUID = 7215073386484048953L;

    private String key;

    private String metadata;

    private String signingCertificate;

    private String signingKey;

    private String encryptionCertificate;

    private String encryptionKey;

    private String appliesTo;

    public static class Builder {

        private final SAML2IdPMetadataTO instance = new SAML2IdPMetadataTO();

        public Builder metadata(final String metadata) {
            instance.setMetadata(metadata);
            return this;
        }

        public Builder signingCertificate(final String signingCertificate) {
            instance.setSigningCertificate(signingCertificate);
            return this;
        }

        public Builder signingKey(final String signingKey) {
            instance.setSigningKey(signingKey);
            return this;
        }

        public Builder encryptionCertificate(final String encryptionCertificate) {
            instance.setEncryptionCertificate(encryptionCertificate);
            return this;
        }

        public Builder encryptionKey(final String encryptionKey) {
            instance.setEncryptionKey(encryptionKey);
            return this;
        }

        public Builder appliesTo(final String appliesTo) {
            instance.setAppliesTo(appliesTo);
            return this;
        }

        public SAML2IdPMetadataTO build() {
            return instance;
        }
    }

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

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

    public String getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(final String appliesTo) {
        this.appliesTo = appliesTo;
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
        SAML2IdPMetadataTO other = (SAML2IdPMetadataTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(metadata, other.metadata).
                append(encryptionCertificate, other.encryptionCertificate).
                append(encryptionKey, other.encryptionKey).
                append(signingCertificate, other.signingCertificate).
                append(signingKey, other.signingKey).
                append(appliesTo, other.appliesTo).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(metadata).
                append(encryptionCertificate).
                append(encryptionKey).
                append(signingCertificate).
                append(signingKey).
                append(appliesTo).
                build();
    }

}
