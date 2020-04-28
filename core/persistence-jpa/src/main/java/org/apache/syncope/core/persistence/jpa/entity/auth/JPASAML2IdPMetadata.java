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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPMetadata;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPASAML2IdPMetadata.TABLE)
public class JPASAML2IdPMetadata extends AbstractGeneratedKeyEntity implements SAML2IdPMetadata {

    public static final String TABLE = "SAML2IdPMetadata";

    private static final long serialVersionUID = 57352617217394093L;

    @Column(unique = true)
    private String appliesTo;

    @Lob
    @Column
    private String metadata;

    @Lob
    @Column
    private String signingCertificate;

    @Lob
    @Column
    private String signingKey;

    @Lob
    @Column
    private String encryptionCertificate;

    @Lob
    @Column
    private String encryptionKey;

    @Override
    public String getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getSigningCertificate() {
        return signingCertificate;
    }

    @Override
    public void setSigningCertificate(final String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    @Override
    public String getSigningKey() {
        return signingKey;
    }

    @Override
    public void setSigningKey(final String signingKey) {
        this.signingKey = signingKey;
    }

    @Override
    public String getEncryptionCertificate() {
        return encryptionCertificate;
    }

    @Override
    public void setEncryptionCertificate(final String encryptionCertificate) {
        this.encryptionCertificate = encryptionCertificate;
    }

    @Override
    public String getEncryptionKey() {
        return encryptionKey;
    }

    @Override
    public void setEncryptionKey(final String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public String getAppliesTo() {
        return appliesTo;
    }

    @Override
    public void setAppliesTo(final String appliesTo) {
        this.appliesTo = appliesTo;
    }

}
