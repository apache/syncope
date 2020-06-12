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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.entity.auth.OIDCJWKS;
import org.apache.syncope.core.provisioning.api.data.OIDCJWKSDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

@Component
public class OIDCJWKSLogic extends AbstractTransactionalLogic<OIDCJWKSTO> {

    @Autowired
    private OIDCJWKSDataBinder binder;

    @Autowired
    private OIDCJWKSDAO dao;

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_READ + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public OIDCJWKSTO get() {
        OIDCJWKS jwks = dao.get();
        if (jwks != null) {
            return binder.get(jwks);
        }
        throw new NotFoundException("OIDC JWKS not found");
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_CREATE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public OIDCJWKSTO set() {
        OIDCJWKS jwks = dao.get();
        if (jwks == null) {
            return binder.get(dao.save(binder.create()));
        }
        throw SyncopeClientException.build(ClientExceptionType.EntityExists);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_UPDATE + "')")
    public OIDCJWKSTO update(final OIDCJWKSTO jwksTO) {
        OIDCJWKS jwks = dao.get();
        if (jwks == null) {
            throw SyncopeClientException.build(ClientExceptionType.NotFound);
        }
        return binder.get(dao.save(binder.update(jwks, jwksTO)));
    }

    @Override
    protected OIDCJWKSTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        OIDCJWKS jwks = dao.get();
        if (jwks == null) {
            throw new UnresolvedReferenceException();
        }
        return binder.get(jwks);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.OIDC_JWKS_DELETE + "')")
    public void delete() {
         dao.delete();
    }
}
