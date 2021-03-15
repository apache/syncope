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
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.SAML2IdPMetadataTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2IdPMetadataDAO;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPMetadata;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPMetadataDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SAML2IdPMetadataLogic extends AbstractTransactionalLogic<SAML2IdPMetadataTO> {

    @Autowired
    private SAML2IdPMetadataDataBinder binder;

    @Autowired
    private SAML2IdPMetadataDAO saml2IdPMetadataDAO;

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_METADATA_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2IdPMetadataTO read(final String key) {
        return Optional.ofNullable(saml2IdPMetadataDAO.find(key)).
                map(binder::getSAML2IdPMetadataTO).
                orElseThrow(() -> new NotFoundException("SAML2 IdP Metadata " + key + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_METADATA_READ + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2IdPMetadataTO readFor(final String appliesTo) {
        return Optional.ofNullable(saml2IdPMetadataDAO.findByOwner(appliesTo)).
                map(binder::getSAML2IdPMetadataTO).
                orElseThrow(() -> new NotFoundException("SAML2 IdP Metadata owned by " + appliesTo + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_METADATA_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2IdPMetadataTO set(final SAML2IdPMetadataTO saml2IdPMetadataTO) {
        SAML2IdPMetadata saml2IdPMetadata = saml2IdPMetadataDAO.findByOwner(saml2IdPMetadataTO.getAppliesTo());
        if (saml2IdPMetadata == null) {
            return binder.getSAML2IdPMetadataTO(saml2IdPMetadataDAO.save(binder.create(saml2IdPMetadataTO)));
        }
        throw new DuplicateException("SAML 2.0 IdP metadata for " + saml2IdPMetadataTO.getAppliesTo());
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
                    appliesTo = ((SAML2IdPMetadataTO) args[i]).getAppliesTo();
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
