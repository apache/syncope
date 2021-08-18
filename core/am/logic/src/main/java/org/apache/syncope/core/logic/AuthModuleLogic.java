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
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModule;
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
        AuthModule authModule = authModuleDAO.find(authModuleTO.getKey());
        if (authModule == null) {
            throw new NotFoundException("AuthModule " + authModuleTO.getKey() + " not found");
        }

        return binder.getAuthModuleTO(authModuleDAO.save(binder.update(authModule, authModuleTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<AuthModuleTO> list() {
        return authModuleDAO.findAll().stream().map(binder::getAuthModuleTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_READ + "')")
    @Transactional(readOnly = true)
    public AuthModuleTO read(final String key) {
        AuthModule authModule = authModuleDAO.find(key);
        if (authModule == null) {
            throw new NotFoundException("AuthModule " + key + " not found");
        }

        return binder.getAuthModuleTO(authModule);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.AUTH_MODULE_DELETE + "')")
    public AuthModuleTO delete(final String key) {
        AuthModule authModule = authModuleDAO.find(key);
        if (authModule == null) {
            throw new NotFoundException("AuthModule " + key + " not found");
        }

        AuthModuleTO deleted = binder.getAuthModuleTO(authModule);
        authModuleDAO.delete(authModule);

        return deleted;
    }

    @Override
    protected AuthModuleTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AuthModuleTO) {
                    key = ((AuthModuleTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getAuthModuleTO(authModuleDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
