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
import org.apache.syncope.common.lib.to.OIDCOPTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCOPDAO;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOP;
import org.apache.syncope.core.persistence.api.entity.am.WAConfigEntry;
import org.apache.syncope.core.provisioning.api.data.OIDCOPDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class OIDCOPLogic extends AbstractTransactionalLogic<OIDCOPTO> {

    protected final OIDCOPDataBinder binder;

    protected final OIDCOPDAO oidcOPDAO;

    protected final WAConfigDAO waConfigDAO;

    protected final EntityFactory entityFactory;

    public OIDCOPLogic(
            final OIDCOPDataBinder binder,
            final OIDCOPDAO oidcOPDAO,
            final WAConfigDAO waConfigDAO,
            final EntityFactory entityFactory) {

        this.binder = binder;
        this.oidcOPDAO = oidcOPDAO;
        this.waConfigDAO = waConfigDAO;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_OP_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public OIDCOPTO get() {
        return oidcOPDAO.get().
                map(binder::getOIDCOPTO).
                orElseThrow(() -> new NotFoundException("OIDC OP not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_GENERATE + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCOPTO generate(final String jwksKeyId, final String jwksType, final int jwksKeySize) {
        if (oidcOPDAO.get().isEmpty()) {
            OIDCOPTO oidcOPTO = binder.getOIDCOPTO(oidcOPDAO.save(binder.create(jwksKeyId, jwksType, jwksKeySize)));

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

            return oidcOPTO;
        }

        throw new DuplicateException("OIDC JWKS already set");
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_OP_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCOPTO set(final OIDCOPTO oidcOPTO) {
        OIDCOP oidcOp = oidcOPDAO.get().orElseGet(() -> entityFactory.newEntity(OIDCOP.class));
        binder.update(oidcOp, oidcOPTO);
        return binder.getOIDCOPTO(oidcOPDAO.save(oidcOp));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_OP_DELETE + "')")
    public void delete() {
        oidcOPDAO.delete();
    }

    @Override
    protected OIDCOPTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        OIDCOP oidcOp = oidcOPDAO.get().orElseThrow(UnresolvedReferenceException::new);
        return binder.getOIDCOPTO(oidcOp);
    }
}
