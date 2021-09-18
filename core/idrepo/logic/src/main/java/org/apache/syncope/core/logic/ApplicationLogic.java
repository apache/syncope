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
import org.apache.syncope.common.lib.to.ApplicationTO;
import org.apache.syncope.common.lib.to.PrivilegeTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.provisioning.api.data.ApplicationDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ApplicationLogic extends AbstractTransactionalLogic<ApplicationTO> {

    protected final ApplicationDataBinder binder;

    protected final ApplicationDAO applicationDAO;

    public ApplicationLogic(final ApplicationDataBinder binder, final ApplicationDAO applicationDAO) {
        this.binder = binder;
        this.applicationDAO = applicationDAO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.APPLICATION_READ + "')")
    @Transactional(readOnly = true)
    public ApplicationTO read(final String key) {
        Application application = applicationDAO.find(key);
        if (application == null) {
            LOG.error("Could not find application '" + key + '\'');

            throw new NotFoundException(key);
        }

        return binder.getApplicationTO(application);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.APPLICATION_READ + "')")
    @Transactional(readOnly = true)
    public PrivilegeTO readPrivilege(final String key) {
        Privilege privilege = applicationDAO.findPrivilege(key);
        if (privilege == null) {
            LOG.error("Could not find privilege '" + key + '\'');

            throw new NotFoundException(key);
        }

        return binder.getPrivilegeTO(privilege);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.APPLICATION_LIST + "')")
    @Transactional(readOnly = true)
    public List<ApplicationTO> list() {
        return applicationDAO.findAll().stream().map(binder::getApplicationTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.APPLICATION_CREATE + "')")
    public ApplicationTO create(final ApplicationTO applicationTO) {
        return binder.getApplicationTO(applicationDAO.save(binder.create(applicationTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.APPLICATION_UPDATE + "')")
    public ApplicationTO update(final ApplicationTO applicationTO) {
        Application application = applicationDAO.find(applicationTO.getKey());
        if (application == null) {
            LOG.error("Could not find application '" + applicationTO.getKey() + '\'');
            throw new NotFoundException(applicationTO.getKey());
        }

        return binder.getApplicationTO(applicationDAO.save(binder.update(application, applicationTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.APPLICATION_DELETE + "')")
    public ApplicationTO delete(final String key) {
        Application application = applicationDAO.find(key);
        if (application == null) {
            LOG.error("Could not find application '" + key + '\'');

            throw new NotFoundException(key);
        }

        ApplicationTO deleted = binder.getApplicationTO(application);
        applicationDAO.delete(key);
        return deleted;
    }

    @Override
    protected ApplicationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof ApplicationTO) {
                    key = ((ApplicationTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getApplicationTO(applicationDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
