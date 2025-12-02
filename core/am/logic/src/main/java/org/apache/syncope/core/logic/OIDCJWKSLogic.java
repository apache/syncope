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
import java.util.List;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.OIDCJWKS;
import org.apache.syncope.core.persistence.api.entity.am.WAConfigEntry;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class OIDCJWKSLogic extends AbstractTransactionalLogic<OIDCJWKSTO> {

    protected final OIDCJWKSDataBinder binder;

    protected final OIDCJWKSDAO oidcJWKSDAO;

    protected final WAConfigDAO waConfigDAO;

    protected final EntityFactory entityFactory;

    public OIDCJWKSLogic(
            final OIDCJWKSDataBinder binder,
            final OIDCJWKSDAO oidcJWKSDAO,
            final WAConfigDAO waConfigDAO,
            final EntityFactory entityFactory) {

        this.binder = binder;
        this.oidcJWKSDAO = oidcJWKSDAO;
        this.waConfigDAO = waConfigDAO;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public OIDCJWKSTO get() {
        return oidcJWKSDAO.get().
                map(binder::getOIDCJWKSTO).
                orElseThrow(() -> new NotFoundException("OIDC JWKS not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_GENERATE + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCJWKSTO generate(final String jwksKeyId, final String jwksType, final int jwksKeySize) {
        if (oidcJWKSDAO.get().isEmpty()) {
            OIDCJWKSTO oidcJWKSTO = binder.getOIDCJWKSTO(
                    oidcJWKSDAO.save(binder.create(jwksKeyId, jwksType, jwksKeySize)));

            WAConfigEntry jwksKeyIdConfig = entityFactory.newEntity(WAConfigEntry.class);
            jwksKeyIdConfig.setKey("cas.authn.oidc.jwks.core.jwks-key-id");
            jwksKeyIdConfig.setValues(List.of(jwksKeyId));
            waConfigDAO.save(jwksKeyIdConfig);

            WAConfigEntry jwksTypeConfig = entityFactory.newEntity(WAConfigEntry.class);
            jwksTypeConfig.setKey("cas.authn.oidc.jwks.core.jwks-type");
            jwksTypeConfig.setValues(List.of(jwksType));
            waConfigDAO.save(jwksTypeConfig);

            WAConfigEntry jwksKeySizeConfig = entityFactory.newEntity(WAConfigEntry.class);
            jwksKeySizeConfig.setKey("cas.authn.oidc.jwks.core.jwks-key-size");
            jwksKeySizeConfig.setValues(List.of(String.valueOf(jwksKeySize)));
            waConfigDAO.save(jwksKeySizeConfig);

            return oidcJWKSTO;
        }

        throw new DuplicateException("OIDC JWKS already set");
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCJWKSTO set(final OIDCJWKSTO entityTO) {
        OIDCJWKS jwks = oidcJWKSDAO.get().orElseGet(() -> entityFactory.newEntity(OIDCJWKS.class));
        jwks.setJson(entityTO.getJson());
        return binder.getOIDCJWKSTO(oidcJWKSDAO.save(jwks));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_DELETE + "')")
    public void delete() {
        oidcJWKSDAO.delete();
    }

    @Override
    protected OIDCJWKSTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        OIDCJWKS jwks = oidcJWKSDAO.get().orElseThrow(UnresolvedReferenceException::new);
        return binder.getOIDCJWKSTO(jwks);
    }
}
