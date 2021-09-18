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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.SAML2IdPEntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.api.entity.auth.SAML2IdPEntity;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2IdPEntityDAO;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPEntityDataBinder;

public class SAML2IdPEntityLogic extends AbstractTransactionalLogic<SAML2IdPEntityTO> {

    protected final SAML2IdPEntityDataBinder binder;

    protected final SAML2IdPEntityDAO entityDAO;

    public SAML2IdPEntityLogic(final SAML2IdPEntityDataBinder binder, final SAML2IdPEntityDAO entityDAO) {
        this.binder = binder;
        this.entityDAO = entityDAO;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_ENTITY_LIST + "')")
    @Transactional(readOnly = true)
    public List<SAML2IdPEntityTO> list() {
        return entityDAO.findAll().stream().
                map(binder::getSAML2IdPEntityTO).
                collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_ENTITY_GET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2IdPEntityTO get(final String key) {
        return Optional.ofNullable(entityDAO.find(key)).
                map(binder::getSAML2IdPEntityTO).
                orElseThrow(() -> new NotFoundException(key + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_IDP_ENTITY_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2IdPEntityTO set(final SAML2IdPEntityTO entityTO) {
        SAML2IdPEntity entity = Optional.ofNullable(entityDAO.find(entityTO.getKey())).
                map(metadata -> binder.update(metadata, entityTO)).
                orElseGet(() -> binder.create(entityTO));
        return binder.getSAML2IdPEntityTO(entityDAO.save(entity));
    }

    @Override
    protected SAML2IdPEntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof SAML2IdPEntityTO) {
                    key = ((SAML2IdPEntityTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getSAML2IdPEntityTO(entityDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
