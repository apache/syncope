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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPKeystoreDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPKeystore;
import org.apache.syncope.core.provisioning.api.data.SAML2SPKeystoreBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

@Component
public class SAML2SPKeystoreLogic extends AbstractTransactionalLogic<SAML2SPKeystoreTO> {

    @Autowired
    private SAML2SPKeystoreBinder binder;

    @Autowired
    private SAML2SPKeystoreDAO saml2SPKeystoreDAO;

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_KEYSTORE_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2SPKeystoreTO read(final String key) {
        return Optional.ofNullable(saml2SPKeystoreDAO.find(key)).
                map(binder::getSAML2SPKeystoreTO).
                orElseThrow(() -> new NotFoundException(key + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_KEYSTORE_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2SPKeystoreTO get(final String name) {
        return Optional.ofNullable(saml2SPKeystoreDAO.findByOwner(name)).
                map(binder::getSAML2SPKeystoreTO).
                orElseThrow(() -> new NotFoundException("SAML2 SP keystore owned by " + name + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_KEYSTORE_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2SPKeystoreTO set(final SAML2SPKeystoreTO keystoreTO) {
        SAML2SPKeystore keystore = saml2SPKeystoreDAO.findByOwner(keystoreTO.getOwner());
        if (keystore == null) {
            return binder.getSAML2SPKeystoreTO(saml2SPKeystoreDAO.save(binder.create(keystoreTO)));
        }
        throw SyncopeClientException.build(ClientExceptionType.EntityExists);
    }

    @Override
    protected SAML2SPKeystoreTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        String name = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; name == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    name = (String) args[i];
                } else if (args[i] instanceof SAML2SPKeystoreTO) {
                    name = ((SAML2SPKeystoreTO) args[i]).getOwner();
                }
            }
        }

        if (name != null) {
            try {
                return binder.getSAML2SPKeystoreTO(saml2SPKeystoreDAO.findByOwner(name));
            } catch (final Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }
        throw new UnresolvedReferenceException();
    }
}
