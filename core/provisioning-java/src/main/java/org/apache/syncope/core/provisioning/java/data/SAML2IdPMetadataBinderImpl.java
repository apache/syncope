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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPMetadata;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPMetadataBinder;

@Component
public class SAML2IdPMetadataBinderImpl implements SAML2IdPMetadataBinder {

    @Autowired
    private EntityFactory entityFactory;

    private SAML2IdPMetadata getSAML2IdPMetadata(
            final SAML2IdPMetadata saml2IdPMetadata,
            final SAML2IdPMetadataTO saml2IdPMetadataTO) {

        SAML2IdPMetadata saml2IdPMetadataResult = saml2IdPMetadata;
        if (saml2IdPMetadataResult == null) {
            saml2IdPMetadataResult = entityFactory.newEntity(SAML2IdPMetadata.class);
        }

        saml2IdPMetadataResult.setEncryptionCertificate(saml2IdPMetadataTO.getEncryptionCertificate());
        saml2IdPMetadataResult.setEncryptionKey(saml2IdPMetadataTO.getEncryptionKey());
        saml2IdPMetadataResult.setMetadata(saml2IdPMetadataTO.getMetadata());
        saml2IdPMetadataResult.setSigningCertificate(saml2IdPMetadataTO.getSigningCertificate());
        saml2IdPMetadataResult.setSigningKey(saml2IdPMetadataTO.getSigningKey());
        saml2IdPMetadataResult.setAppliesTo(saml2IdPMetadataTO.getAppliesTo());

        return saml2IdPMetadataResult;
    }

    @Override
    public SAML2IdPMetadata create(final SAML2IdPMetadataTO saml2IdPMetadataTO) {
        return update(entityFactory.newEntity(SAML2IdPMetadata.class), saml2IdPMetadataTO);
    }

    @Override
    public SAML2IdPMetadata update(
            final SAML2IdPMetadata saml2IdPMetadata,
            final SAML2IdPMetadataTO saml2IdPMetadataTO) {

        return getSAML2IdPMetadata(saml2IdPMetadata, saml2IdPMetadataTO);
    }

    @Override
    public SAML2IdPMetadataTO getSAML2IdPMetadataTO(final SAML2IdPMetadata saml2IdPMetadata) {
        SAML2IdPMetadataTO saml2IdPMetadataTO = new SAML2IdPMetadataTO();

        saml2IdPMetadataTO.setKey(saml2IdPMetadata.getKey());
        saml2IdPMetadataTO.setMetadata(saml2IdPMetadata.getMetadata());
        saml2IdPMetadataTO.setEncryptionCertificate(saml2IdPMetadata.getEncryptionCertificate());
        saml2IdPMetadataTO.setEncryptionKey(saml2IdPMetadata.getEncryptionKey());
        saml2IdPMetadataTO.setSigningCertificate(saml2IdPMetadata.getSigningCertificate());
        saml2IdPMetadataTO.setSigningKey(saml2IdPMetadata.getSigningKey());
        saml2IdPMetadataTO.setAppliesTo(saml2IdPMetadata.getAppliesTo());

        return saml2IdPMetadataTO;
    }
}
