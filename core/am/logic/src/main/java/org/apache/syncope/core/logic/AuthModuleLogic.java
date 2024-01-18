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
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class AuthModuleLogic extends AbstractTransactionalLogic<AuthModuleTO> {

    protected final AuthModuleDataBinder binder;

    protected final AuthModuleDAO authModuleDAO;

    public AuthModuleLogic(final AuthModuleDataBinder binder, final AuthModuleDAO authModuleDAO) {
        this.binder = binder;
        this.authModuleDAO = authModuleDAO;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_CREATE + "')")
    public AuthModuleTO create(final AuthModuleTO authModuleTO) {
        return binder.getAuthModuleTO(authModuleDAO.save(binder.create(authModuleTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_UPDATE + "')")
    public AuthModuleTO update(final AuthModuleTO authModuleTO) {
        AuthModule authModule = authModuleDAO.findById(authModuleTO.getKey()).
                orElseThrow(() -> new NotFoundException("AuthModule " + authModuleTO.getKey()));

        return binder.getAuthModuleTO(authModuleDAO.save(binder.update(authModule, authModuleTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<AuthModuleTO> list() {
        return authModuleDAO.findAll().stream().map(binder::getAuthModuleTO).toList();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_READ + "')")
    @Transactional(readOnly = true)
    public AuthModuleTO read(final String key) {
        AuthModule authModule = authModuleDAO.findById(key).
                orElseThrow(() -> new NotFoundException("AuthModule " + key));

        return binder.getAuthModuleTO(authModule);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_DELETE + "')")
    public AuthModuleTO delete(final String key) {
        AuthModule authModule = authModuleDAO.findById(key).
                orElseThrow(() -> new NotFoundException("AuthModule " + key));

        AuthModuleTO deleted = binder.getAuthModuleTO(authModule);
        authModuleDAO.delete(authModule);

        return deleted;
    }

    @Override
    protected AuthModuleTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        if (ArrayUtils.isEmpty(args)) {
            throw new UnresolvedReferenceException();
        }

        final String key;

        if (args[0] instanceof String string) {
            key = string;
        } else if (args[0] instanceof AuthModuleTO authModuleTO) {
            key = authModuleTO.getKey();
        } else {
            throw new UnresolvedReferenceException();
        }

        try {
            return binder.getAuthModuleTO(authModuleDAO.findById(key).orElseThrow());
        } catch (Throwable ignore) {
            LOG.debug("Unresolved reference", ignore);
            throw new UnresolvedReferenceException(ignore);
        }
    }
}
