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
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPEntity;
import org.apache.syncope.core.provisioning.api.data.SAML2SPEntityDataBinder;

public class SAML2SPEntityDataBinderImpl implements SAML2SPEntityDataBinder {

    protected final EntityFactory entityFactory;

    public SAML2SPEntityDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public SAML2SPEntity create(final SAML2SPEntityTO entityTO) {
        SAML2SPEntity entity = entityFactory.newEntity(SAML2SPEntity.class);
        entity.setKey(entityTO.getKey());
        return update(entity, entityTO);
    }

    @Override
    public SAML2SPEntity update(final SAML2SPEntity entity, final SAML2SPEntityTO entityTO) {
        entity.setKeystore(entityTO.getKeystore() == null
                ? null
                : Base64.getDecoder().decode(entityTO.getKeystore()));
        entity.setMetadata(entityTO.getMetadata() == null
                ? null
                : Base64.getDecoder().decode(entityTO.getMetadata()));
        return entity;
    }

    @Override
    public SAML2SPEntityTO getSAML2SPEntityTO(final SAML2SPEntity entity) {
        SAML2SPEntityTO entityTO = new SAML2SPEntityTO();
        entityTO.setKey(entity.getKey());
        if (entity.getKeystore() != null) {
            entityTO.setKeystore(Base64.getEncoder().encodeToString(entity.getKeystore()));
        }
        if (entity.getMetadata() != null) {
            entityTO.setMetadata(Base64.getEncoder().encodeToString(entity.getMetadata()));
        }
        return entityTO;
    }
}
