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
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PasswordManagementDAO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.provisioning.api.data.PasswordManagementDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class PasswordManagementLogic extends AbstractTransactionalLogic<PasswordManagementTO> {

    protected final PasswordManagementDataBinder passwordManagementDataBinder;

    protected final PasswordManagementDAO passwordManagementDAO;

    public PasswordManagementLogic(final PasswordManagementDataBinder passwordManagementDataBinder,
            final PasswordManagementDAO passwordManagementDAO) {
        this.passwordManagementDataBinder = passwordManagementDataBinder;
        this.passwordManagementDAO = passwordManagementDAO;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MANAGEMENT_CREATE + "')")
    public PasswordManagementTO create(final PasswordManagementTO passwordManagementTO) {
        return passwordManagementDataBinder.getPasswordManagementTO(
                passwordManagementDAO.save(passwordManagementDataBinder.create(passwordManagementTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MANAGEMENT_UPDATE + "')")
    public PasswordManagementTO update(final PasswordManagementTO passwordManagementTO) {
        PasswordManagement passwordManagement = passwordManagementDAO.findById(passwordManagementTO.getKey()).
                orElseThrow(() -> new NotFoundException("PasswordManagement " + passwordManagementTO.getKey()));

        return passwordManagementDataBinder.getPasswordManagementTO(
                passwordManagementDAO.save(passwordManagementDataBinder
                        .update(passwordManagement, passwordManagementTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MANAGEMENT_LIST + "') or hasRole('"
            + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<PasswordManagementTO> list() {
        return passwordManagementDAO.findAll().stream()
                .map(passwordManagementDataBinder::getPasswordManagementTO).toList();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MANAGEMENT_READ + "')")
    @Transactional(readOnly = true)
    public PasswordManagementTO read(final String key) {
        PasswordManagement passwordManagement = passwordManagementDAO.findById(key).
                orElseThrow(() -> new NotFoundException("PasswordManagement " + key));

        return passwordManagementDataBinder.getPasswordManagementTO(passwordManagement);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MANAGEMENT_DELETE + "')")
    public PasswordManagementTO delete(final String key) {
        PasswordManagement passwordManagement = passwordManagementDAO.findById(key).
                orElseThrow(() -> new NotFoundException("PasswordManagement " + key));

        PasswordManagementTO deleted = passwordManagementDataBinder.getPasswordManagementTO(passwordManagement);
        passwordManagementDAO.delete(passwordManagement);

        return deleted;
    }

    @Override
    protected PasswordManagementTO resolveReference(final Method method, final Object... args)
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
            return passwordManagementDataBinder.getPasswordManagementTO(passwordManagementDAO.findById(key)
                    .orElseThrow());
        } catch (Throwable ignore) {
            LOG.debug("Unresolved reference", ignore);
            throw new UnresolvedReferenceException(ignore);
        }
    }
}
