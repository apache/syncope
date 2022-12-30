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
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPEntity;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;

@Entity
@Table(name = JPASAML2SPEntity.TABLE)
public class JPASAML2SPEntity extends AbstractProvidedKeyEntity implements SAML2SPEntity {

    public static final String TABLE = "SAML2SPEntity";

    private static final long serialVersionUID = 12342617217394093L;

    @Column(nullable = true)
    @Lob
    private byte[] keystore;

    @Column(nullable = true)
    @Lob
    private byte[] metadata;

    @Override
    public byte[] getKeystore() {
        return keystore;
    }

    @Override
    public void setKeystore(final byte[] keystore) {
        this.keystore = ArrayUtils.clone(keystore);
    }

    @Override
    public byte[] getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(final byte[] metadata) {
        this.metadata = ArrayUtils.clone(metadata);
    }
}
