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

import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPMetadata;
import org.apache.syncope.core.provisioning.api.data.SAML2SPMetadataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SAML2SPMetadataBinderImpl implements SAML2SPMetadataBinder {

    @Autowired
    private EntityFactory entityFactory;

    private SAML2SPMetadata getSAML2SPMetadata(
        final SAML2SPMetadata metadata,
        final SAML2SPMetadataTO metadataTO) {

        SAML2SPMetadata result = metadata;
        if (result == null) {
            result = entityFactory.newEntity(SAML2SPMetadata.class);
        }

        result.setMetadata(metadataTO.getMetadata());
        result.setOwner(metadataTO.getOwner());

        return result;
    }

    @Override
    public SAML2SPMetadata create(final SAML2SPMetadataTO metadataTO) {
        return update(entityFactory.newEntity(SAML2SPMetadata.class), metadataTO);
    }

    @Override
    public SAML2SPMetadata update(
        final SAML2SPMetadata metadata,
        final SAML2SPMetadataTO metadataTO) {

        return getSAML2SPMetadata(metadata, metadataTO);
    }

    @Override
    public SAML2SPMetadataTO getSAML2SPMetadataTO(final SAML2SPMetadata saml2IdPMetadata) {
        SAML2SPMetadataTO metadataTO = new SAML2SPMetadataTO();

        metadataTO.setKey(saml2IdPMetadata.getKey());
        metadataTO.setMetadata(saml2IdPMetadata.getMetadata());
        metadataTO.setOwner(saml2IdPMetadata.getOwner());

        return metadataTO;
    }
}
