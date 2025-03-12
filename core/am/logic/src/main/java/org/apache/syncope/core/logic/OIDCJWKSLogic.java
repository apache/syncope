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
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.OIDCJWKS;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class OIDCJWKSLogic extends AbstractTransactionalLogic<OIDCJWKSTO> {

    protected final OIDCJWKSDataBinder binder;

    protected final OIDCJWKSDAO dao;

    protected final EntityFactory entityFactory;

    public OIDCJWKSLogic(final OIDCJWKSDataBinder binder, final OIDCJWKSDAO dao, final EntityFactory entityFactory) {
        this.binder = binder;
        this.dao = dao;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public OIDCJWKSTO get() {
        return dao.get().
                map(binder::getOIDCJWKSTO).
                orElseThrow(() -> new NotFoundException("OIDC JWKS not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_GENERATE + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCJWKSTO generate(final String jwksKeyId, final String jwksType, final int jwksKeySize) {
        if (dao.get().isEmpty()) {
            return binder.getOIDCJWKSTO(dao.save(binder.create(jwksKeyId, jwksType, jwksKeySize)));
        }
        throw new DuplicateException("OIDC JWKS already set");
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCJWKSTO set(final OIDCJWKSTO entityTO) {
        OIDCJWKS jwks = dao.get().orElse(null);
        if (jwks == null) {
            jwks = entityFactory.newEntity(OIDCJWKS.class);
        }
        jwks.setJson(entityTO.getJson());
        return binder.getOIDCJWKSTO(dao.save(jwks));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_DELETE + "')")
    public void delete() {
        dao.delete();
    }

    @Override
    protected OIDCJWKSTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        OIDCJWKS jwks = dao.get().orElseThrow(UnresolvedReferenceException::new);
        return binder.getOIDCJWKSTO(jwks);
    }
}
