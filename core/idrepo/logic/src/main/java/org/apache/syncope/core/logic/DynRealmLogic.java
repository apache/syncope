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
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class DynRealmLogic extends AbstractTransactionalLogic<DynRealmTO> {

    protected final DynRealmDataBinder binder;

    protected final DynRealmDAO dynRealmDAO;

    public DynRealmLogic(final DynRealmDataBinder binder, final DynRealmDAO dynRealmDAO) {
        this.binder = binder;
        this.dynRealmDAO = dynRealmDAO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DYNREALM_READ + "')")
    @Transactional(readOnly = true)
    public DynRealmTO read(final String key) {
        DynRealm dynRealm = dynRealmDAO.findById(key).
                orElseThrow(() -> new NotFoundException("DynRealm " + key));

        return binder.getDynRealmTO(dynRealm);
    }

    @Transactional(readOnly = true)
    public List<DynRealmTO> list() {
        return dynRealmDAO.findAll().stream().map(binder::getDynRealmTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DYNREALM_CREATE + "')")
    public DynRealmTO create(final DynRealmTO dynRealmTO) {
        return binder.getDynRealmTO(binder.create(dynRealmTO));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DYNREALM_UPDATE + "')")
    public DynRealmTO update(final DynRealmTO dynRealmTO) {
        DynRealm dynRealm = dynRealmDAO.findById(dynRealmTO.getKey()).
                orElseThrow(() -> new NotFoundException("DynRealm " + dynRealmTO.getKey()));

        return binder.getDynRealmTO(binder.update(dynRealm, dynRealmTO));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.DYNREALM_DELETE + "')")
    public DynRealmTO delete(final String key) {
        DynRealm dynRealm = dynRealmDAO.findById(key).
                orElseThrow(() -> new NotFoundException("DynRealm " + key));

        DynRealmTO deleted = binder.getDynRealmTO(dynRealm);
        dynRealmDAO.deleteById(key);
        return deleted;
    }

    @Override
    protected DynRealmTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof DynRealmTO dynRealmTO) {
                    key = dynRealmTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getDynRealmTO(dynRealmDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
