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

import java.util.Base64;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.SAML2IdPEntity;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPEntityDataBinder;

public class SAML2IdPEntityDataBinderImpl implements SAML2IdPEntityDataBinder {

    protected final EntityFactory entityFactory;

    public SAML2IdPEntityDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public SAML2IdPEntity create(final SAML2IdPEntityTO entityTO) {
        SAML2IdPEntity entity = entityFactory.newEntity(SAML2IdPEntity.class);
        entity.setKey(entityTO.getKey());
        return update(entity, entityTO);
    }

    @Override
    public SAML2IdPEntity update(final SAML2IdPEntity entity, final SAML2IdPEntityTO entityTO) {
        entity.setEncryptionCertificate(entityTO.getEncryptionCertificate() == null
                ? null
                : Base64.getDecoder().decode(entityTO.getEncryptionCertificate()));
        entity.setEncryptionKey(entityTO.getEncryptionKey() == null
                ? null
                : Base64.getDecoder().decode(entityTO.getEncryptionKey()));
        entity.setMetadata(entityTO.getMetadata() == null
                ? null
                : Base64.getDecoder().decode(entityTO.getMetadata()));
        entity.setSigningCertificate(entityTO.getSigningCertificate() == null
                ? null
                : Base64.getDecoder().decode(entityTO.getSigningCertificate()));
        entity.setSigningKey(entityTO.getSigningKey() == null
                ? null
                : Base64.getDecoder().decode(entityTO.getSigningKey()));
        return entity;
    }

    @Override
    public SAML2IdPEntityTO getSAML2IdPEntityTO(final SAML2IdPEntity entity) {
        SAML2IdPEntityTO entityTO = new SAML2IdPEntityTO();
        entityTO.setKey(entity.getKey());
        entityTO.setMetadata(Base64.getEncoder().encodeToString(entity.getMetadata()));
        if (entity.getEncryptionCertificate() != null) {
            entityTO.setEncryptionCertificate(
                    Base64.getEncoder().encodeToString(entity.getEncryptionCertificate()));
        }
        if (entity.getEncryptionKey() != null) {
            entityTO.setEncryptionKey(
                    Base64.getEncoder().encodeToString(entity.getEncryptionKey()));
        }
        if (entity.getSigningCertificate() != null) {
            entityTO.setSigningCertificate(
                    Base64.getEncoder().encodeToString(entity.getSigningCertificate()));
        }
        if (entity.getSigningKey() != null) {
            entityTO.setSigningKey(Base64.getEncoder().encodeToString(entity.getSigningKey()));
        }
        return entityTO;
    }
}
