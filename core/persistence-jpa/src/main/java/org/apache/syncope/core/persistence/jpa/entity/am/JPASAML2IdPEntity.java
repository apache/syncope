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
package org.apache.syncope.core.persistence.jpa.entity.am;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.core.persistence.api.entity.am.SAML2IdPEntity;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;

@Entity
@Table(name = JPASAML2IdPEntity.TABLE)
public class JPASAML2IdPEntity extends AbstractProvidedKeyEntity implements SAML2IdPEntity {

    public static final String TABLE = "SAML2IdPEntity";

    private static final long serialVersionUID = 57352617217394093L;

    @Column(nullable = false)
    @Lob
    private byte[] metadata;

    @Lob
    private byte[] signingCertificate;

    @Lob
    private byte[] signingKey;

    @Lob
    private byte[] encryptionCertificate;

    @Lob
    private byte[] encryptionKey;

    @Override
    public byte[] getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(final byte[] metadata) {
        this.metadata = ArrayUtils.clone(metadata);
    }

    @Override
    public byte[] getSigningCertificate() {
        return signingCertificate;
    }

    @Override
    public void setSigningCertificate(final byte[] signingCertificate) {
        this.signingCertificate = ArrayUtils.clone(signingCertificate);
    }

    @Override
    public byte[] getSigningKey() {
        return signingKey;
    }

    @Override
    public void setSigningKey(final byte[] signingKey) {
        this.signingKey = ArrayUtils.clone(signingKey);
    }

    @Override
    public byte[] getEncryptionCertificate() {
        return encryptionCertificate;
    }

    @Override
    public void setEncryptionCertificate(final byte[] encryptionCertificate) {
        this.encryptionCertificate = ArrayUtils.clone(encryptionCertificate);
    }

    @Override
    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    @Override
    public void setEncryptionKey(final byte[] encryptionKey) {
        this.encryptionKey = ArrayUtils.clone(encryptionKey);
    }
}
