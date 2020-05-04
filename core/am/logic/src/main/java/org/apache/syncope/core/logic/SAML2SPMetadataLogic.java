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
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPMetadataDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2SPMetadata;
import org.apache.syncope.core.provisioning.api.data.SAML2SPMetadataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

@Component
public class SAML2SPMetadataLogic extends AbstractTransactionalLogic<SAML2SPMetadataTO> {

    @Autowired
    private SAML2SPMetadataBinder binder;

    @Autowired
    private SAML2SPMetadataDAO saml2SPMetadataDAO;

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_METADATA_READ + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2SPMetadataTO read(final String key) {
        final SAML2SPMetadata metadata = saml2SPMetadataDAO.find(key);
        if (metadata == null) {
            throw new NotFoundException(key + " not found");
        }
        return binder.getSAML2SPMetadataTO(metadata);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_METADATA_READ + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2SPMetadataTO get(final String name) {
        final SAML2SPMetadata metadata = saml2SPMetadataDAO.findByOwner(name);
        if (metadata == null) {
            throw new NotFoundException("SAML2 SP Metadata owned by " + name + " not found");
        }

        return binder.getSAML2SPMetadataTO(metadata);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_METADATA_CREATE + "') "
        + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2SPMetadataTO set(final SAML2SPMetadataTO metadataTO) {
        SAML2SPMetadata metadata = saml2SPMetadataDAO.findByOwner(metadataTO.getOwner());
        if (metadata == null) {
            return binder.getSAML2SPMetadataTO(saml2SPMetadataDAO.save(binder.create(metadataTO)));
        }
        throw SyncopeClientException.build(ClientExceptionType.EntityExists);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_METADATA_UPDATE + "')")
    public SAML2SPMetadataTO update(final SAML2SPMetadataTO metadataTO) {
        final SAML2SPMetadata metadata = saml2SPMetadataDAO.find(metadataTO.getKey());
        if (metadata == null) {
            throw new NotFoundException(metadataTO.getKey() + " not found");
        }
        return binder.getSAML2SPMetadataTO(saml2SPMetadataDAO.save(binder.update(metadata, metadataTO)));
    }

    @Override
    protected SAML2SPMetadataTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String name = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; name == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    name = (String) args[i];
                } else if (args[i] instanceof SAML2SPMetadataTO) {
                    name = ((SAML2SPMetadataTO) args[i]).getKey();
                }
            }
        }

        if (name != null) {
            try {
                return binder.getSAML2SPMetadataTO(saml2SPMetadataDAO.findByOwner(name));
            } catch (final Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }
        throw new UnresolvedReferenceException();
    }
}
