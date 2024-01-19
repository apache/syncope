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

import com.nimbusds.oauth2.sdk.ParseException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.OIDCC4UIEntitlement;
import org.apache.syncope.core.logic.oidc.OIDCClientCache;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.provisioning.api.data.OIDCC4UIProviderDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class OIDCC4UIProviderLogic extends AbstractTransactionalLogic<OIDCC4UIProviderTO> {

    protected final OIDCClientCache oidcClientCacheLogin;

    protected final OIDCClientCache oidcClientCacheLogout;

    protected final OIDCC4UIProviderDAO opDAO;

    protected final OIDCC4UIProviderDataBinder binder;

    public OIDCC4UIProviderLogic(
            final OIDCClientCache oidcClientCacheLogin,
            final OIDCClientCache oidcClientCacheLogout,
            final OIDCC4UIProviderDAO opDAO,
            final OIDCC4UIProviderDataBinder binder) {

        this.oidcClientCacheLogin = oidcClientCacheLogin;
        this.oidcClientCacheLogout = oidcClientCacheLogout;
        this.opDAO = opDAO;
        this.binder = binder;
    }

    @PreAuthorize("hasRole('" + OIDCC4UIEntitlement.OP_CREATE + "')")
    public String createFromDiscovery(final OIDCC4UIProviderTO opTO) {
        try {
            OIDCClientCache.importMetadata(opTO);

            return create(opTO);
        } catch (IOException | InterruptedException | ParseException e) {
            LOG.error("While getting the Discovery Document for {}", opTO.getIssuer(), e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Unknown);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + OIDCC4UIEntitlement.OP_CREATE + "')")
    public String create(final OIDCC4UIProviderTO opTO) {
        if (opTO.getConnObjectKeyItem() == null) {
            Item connObjectKeyItem = new Item();
            connObjectKeyItem.setIntAttrName("username");
            connObjectKeyItem.setExtAttrName("email");
            opTO.setConnObjectKeyItem(connObjectKeyItem);
        }

        OIDCC4UIProvider provider = binder.create(opTO);

        return provider.getKey();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<OIDCC4UIProviderTO> list() {
        return opDAO.findAll().stream().map(binder::getOIDCProviderTO).toList();
    }

    @PreAuthorize("hasRole('" + OIDCC4UIEntitlement.OP_READ + "')")
    @Transactional(readOnly = true)
    public OIDCC4UIProviderTO read(final String key) {
        OIDCC4UIProvider op = opDAO.findById(key).
                orElseThrow(() -> new NotFoundException("OIDC Provider "));

        return binder.getOIDCProviderTO(op);
    }

    @PreAuthorize("hasRole('" + OIDCC4UIEntitlement.OP_UPDATE + "')")
    public void update(final OIDCC4UIProviderTO opTO) {
        OIDCC4UIProvider op = opDAO.findById(opTO.getKey()).
                orElseThrow(() -> new NotFoundException("OIDC Provider " + opTO.getKey()));

        if (!op.getIssuer().equals(opTO.getIssuer())) {
            LOG.error("Issuers do not match: expected {}, found {}",
                    op.getIssuer(), opTO.getIssuer());
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidEntity);
            sce.getElements().add("Issuers do not match");
            throw sce;
        }

        binder.update(op, opTO);
        oidcClientCacheLogin.removeAll(op.getName());
        oidcClientCacheLogout.removeAll(op.getName());
    }

    @PreAuthorize("hasRole('" + OIDCC4UIEntitlement.OP_DELETE + "')")
    public void delete(final String key) {
        OIDCC4UIProvider op = opDAO.findById(key).
                orElseThrow(() -> new NotFoundException("OIDC Provider " + key));

        opDAO.deleteById(key);
        oidcClientCacheLogin.removeAll(op.getName());
        oidcClientCacheLogout.removeAll(op.getName());
    }

    @Override
    protected OIDCC4UIProviderTO resolveReference(
            final Method method, final Object... args) throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof OIDCC4UIProviderTO oIDCC4UIProviderTO) {
                    key = oIDCC4UIProviderTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getOIDCProviderTO(opDAO.findById(key).orElseThrow());
            } catch (final Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
