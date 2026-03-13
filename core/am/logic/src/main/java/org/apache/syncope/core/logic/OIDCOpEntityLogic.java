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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCOpEntityDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOpEntity;
import org.apache.syncope.core.provisioning.api.data.OIDCOpEntityDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class OIDCOpEntityLogic extends AbstractTransactionalLogic<OIDCOpEntityTO> {

    protected final OIDCOpEntityDataBinder binder;

    protected final OIDCOpEntityDAO oidcOpEntityDAO;

    protected final EntityFactory entityFactory;

    public OIDCOpEntityLogic(
            final OIDCOpEntityDataBinder binder,
            final OIDCOpEntityDAO oidcOpEntityDAO,
            final EntityFactory entityFactory) {

        this.binder = binder;
        this.oidcOpEntityDAO = oidcOpEntityDAO;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_GENERATE + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCOpEntityTO generate(final String jwksKeyId, final String jwksType, final int jwksKeySize) {
        OIDCOpEntity oidcOpEntity = oidcOpEntityDAO.get().orElseGet(() -> entityFactory.newEntity(OIDCOpEntity.class));
        oidcOpEntity.setJWKS(binder.generateJWKS(jwksKeyId, jwksType, jwksKeySize));

        return binder.getOIDCOpEntityTO(oidcOpEntityDAO.save(oidcOpEntity));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_OP_ENTITY_GET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public OIDCOpEntityTO get() {
        return oidcOpEntityDAO.get().
                map(binder::getOIDCOpEntityTO).
                orElseThrow(() -> new NotFoundException("OIDC OP not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_OP_ENTITY_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCOpEntityTO set(final OIDCOpEntityTO oidcOpEntityTO) {
        OIDCOpEntity oidcOpEntity = oidcOpEntityDAO.get().orElseGet(() -> entityFactory.newEntity(OIDCOpEntity.class));
        binder.update(oidcOpEntity, oidcOpEntityTO);

        return binder.getOIDCOpEntityTO(oidcOpEntityDAO.save(oidcOpEntity));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_OP_ENTITY_DELETE + "')")
    public void delete() {
        oidcOpEntityDAO.delete();
    }

    @Override
    protected OIDCOpEntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        OIDCOpEntity oidcOp = oidcOpEntityDAO.get().orElseThrow(UnresolvedReferenceException::new);
        return binder.getOIDCOpEntityTO(oidcOp);
    }
}
