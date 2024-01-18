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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.SAML2SPEntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SAML2SPEntityDAO;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPEntity;
import org.apache.syncope.core.provisioning.api.data.SAML2SPEntityDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class SAML2SPEntityLogic extends AbstractTransactionalLogic<SAML2SPEntityTO> {

    protected final SAML2SPEntityDataBinder binder;

    protected final SAML2SPEntityDAO saml2SPEntityDAO;

    public SAML2SPEntityLogic(final SAML2SPEntityDataBinder binder, final SAML2SPEntityDAO saml2SPEntityDAO) {
        this.binder = binder;
        this.saml2SPEntityDAO = saml2SPEntityDAO;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_ENTITY_LIST + "')")
    @Transactional(readOnly = true)
    public List<SAML2SPEntityTO> list() {
        return saml2SPEntityDAO.findAll().stream().
                map(binder::getSAML2SPEntityTO).
                toList();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_ENTITY_GET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public SAML2SPEntityTO read(final String key) {
        return saml2SPEntityDAO.findById(key).
                map(binder::getSAML2SPEntityTO).
                orElseThrow(() -> new NotFoundException(key + " not found"));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_ENTITY_SET + "') "
            + "or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    public SAML2SPEntityTO set(final SAML2SPEntityTO entityTO) {
        SAML2SPEntity entity = saml2SPEntityDAO.findById(entityTO.getKey()).
                map(metadata -> binder.update(metadata, entityTO)).
                orElseGet(() -> binder.create(entityTO));
        return binder.getSAML2SPEntityTO(saml2SPEntityDAO.save(entity));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SAML2_SP_ENTITY_DELETE + "')")
    public void delete(final String key) {
        saml2SPEntityDAO.findById(key).ifPresentOrElse(
                saml2SPEntityDAO::delete,
                () -> new NotFoundException("SAML SP " + key));
    }

    @Override
    protected SAML2SPEntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof SAML2SPEntityTO saml2SPEntityTO) {
                    key = saml2SPEntityTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getSAML2SPEntityTO(saml2SPEntityDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }
        throw new UnresolvedReferenceException();
    }
}
