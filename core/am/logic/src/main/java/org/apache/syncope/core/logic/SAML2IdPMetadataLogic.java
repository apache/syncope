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

import static org.apache.syncope.core.logic.AbstractLogic.LOG;

import java.lang.reflect.Method;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2IdPMetadataDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPMetadata;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPMetadataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SAML2IdPMetadataLogic extends AbstractTransactionalLogic<SAML2IdPMetadataTO> {

    @Autowired
    private SAML2IdPMetadataBinder binder;

    @Autowired
    private SAML2IdPMetadataDAO saml2IdPMetadataDAO;

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_METADATA_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2IdPMetadataTO read(final String key) {
        SAML2IdPMetadata sAML2IdPMetadata = saml2IdPMetadataDAO.find(key);
        if (sAML2IdPMetadata == null) {
            throw new NotFoundException("AuthModule " + key + " not found");
        }

        return binder.getSAML2IdPMetadataTO(sAML2IdPMetadata);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_METADATA_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2IdPMetadataTO get(final String appliesTo) {
        SAML2IdPMetadata saml2IdPMetadata = saml2IdPMetadataDAO.findByOwner(appliesTo);
        if (saml2IdPMetadata == null) {
            throw new NotFoundException("SAML2 IdP Metadata owned by " + appliesTo + " not found");
        }

        return binder.getSAML2IdPMetadataTO(saml2IdPMetadata);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_METADATA_CREATE + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2IdPMetadataTO set(final SAML2IdPMetadataTO saml2IdPMetadataTO) {
        SAML2IdPMetadata saml2IdPMetadata = saml2IdPMetadataDAO.findByOwner(saml2IdPMetadataTO.getAppliesTo());
        if (saml2IdPMetadata == null) {
            return binder.getSAML2IdPMetadataTO(saml2IdPMetadataDAO.save(binder.create(saml2IdPMetadataTO)));
        }

        throw SyncopeClientException.build(ClientExceptionType.EntityExists);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_METADATA_UPDATE + "')")
    public SAML2IdPMetadataTO update(final SAML2IdPMetadataTO saml2IdPMetadataTO) {
        SAML2IdPMetadata authModule = saml2IdPMetadataDAO.findByOwner(saml2IdPMetadataTO.getAppliesTo());
        if (authModule == null) {
            throw new NotFoundException("AuthModule " + saml2IdPMetadataTO.getKey() + " not found");
        }

        return binder.getSAML2IdPMetadataTO(saml2IdPMetadataDAO.save(binder.update(authModule, saml2IdPMetadataTO)));
    }

    @Override
    protected SAML2IdPMetadataTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String appliesTo = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; appliesTo == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    appliesTo = (String) args[i];
                } else if (args[i] instanceof SAML2IdPMetadataTO) {
                    appliesTo = ((SAML2IdPMetadataTO) args[i]).getKey();
                }
            }
        }

        if (appliesTo != null) {
            try {
                return binder.getSAML2IdPMetadataTO(saml2IdPMetadataDAO.findByOwner(appliesTo));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
