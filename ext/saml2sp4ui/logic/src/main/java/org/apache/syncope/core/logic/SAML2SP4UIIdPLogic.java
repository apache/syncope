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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SAML2SP4UIEntitlement;
import org.apache.syncope.core.logic.saml2.SAML2ClientCache;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.provisioning.api.data.SAML2SP4UIIdPDataBinder;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class SAML2SP4UIIdPLogic extends AbstractSAML2SP4UILogic {

    protected final SAML2ClientCache saml2ClientCacheLogin;

    protected final SAML2ClientCache saml2ClientCacheLogout;

    protected final SAML2SP4UIIdPDataBinder binder;

    protected final SAML2SP4UIIdPDAO idpDAO;

    public SAML2SP4UIIdPLogic(
            final SAML2SP4UIProperties props,
            final ResourcePatternResolver resourceResolver,
            final SAML2ClientCache saml2ClientCacheLogin,
            final SAML2ClientCache saml2ClientCacheLogout,
            final SAML2SP4UIIdPDataBinder binder,
            final SAML2SP4UIIdPDAO idpDAO) {

        super(props, resourceResolver);
        this.saml2ClientCacheLogin = saml2ClientCacheLogin;
        this.saml2ClientCacheLogout = saml2ClientCacheLogout;
        this.binder = binder;
        this.idpDAO = idpDAO;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<SAML2SP4UIIdPTO> list() {
        return idpDAO.findAll().stream().map(binder::getIdPTO).toList();
    }

    @PreAuthorize("hasRole('" + SAML2SP4UIEntitlement.IDP_READ + "')")
    @Transactional(readOnly = true)
    public SAML2SP4UIIdPTO read(final String key) {
        SAML2SP4UIIdP idp = idpDAO.findById(key).
                orElseThrow(() -> new NotFoundException("SAML 2.0 IdP " + key));

        return binder.getIdPTO(idp);
    }

    @PreAuthorize("hasRole('" + SAML2SP4UIEntitlement.IDP_IMPORT + "')")
    public String importFromMetadata(final InputStream input) {
        try {
            SAML2SP4UIIdPTO idpTO = SAML2ClientCache.importMetadata(input, newSAML2Configuration());
            SAML2SP4UIIdP idp = binder.create(idpTO);

            return idp.getKey();
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error while importing IdP metadata", e);
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidEntity);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + SAML2SP4UIEntitlement.IDP_UPDATE + "')")
    public void update(final SAML2SP4UIIdPTO saml2IdpTO) {
        SAML2SP4UIIdP idp = idpDAO.findById(saml2IdpTO.getKey()).
                orElseThrow(() -> new NotFoundException("SAML 2.0 IdP " + saml2IdpTO.getKey()));

        idp = binder.update(idp, saml2IdpTO);
        saml2ClientCacheLogin.removeAll(idp.getEntityID());
        saml2ClientCacheLogout.removeAll(idp.getEntityID());
    }

    @PreAuthorize("hasRole('" + SAML2SP4UIEntitlement.IDP_DELETE + "')")
    public void delete(final String key) {
        SAML2SP4UIIdP idp = idpDAO.findById(key).
                orElseThrow(() -> new NotFoundException("SAML 2.0 IdP " + key));

        idpDAO.deleteById(key);
        saml2ClientCacheLogin.removeAll(idp.getEntityID());
        saml2ClientCacheLogout.removeAll(idp.getEntityID());
    }

    @Override
    protected SAML2SP4UIIdPTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof final String s) {
                    key = s;
                } else if (args[i] instanceof final SAML2SP4UIIdPTO saml2SP4UIIdPTO) {
                    key = saml2SP4UIIdPTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getIdPTO(idpDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
