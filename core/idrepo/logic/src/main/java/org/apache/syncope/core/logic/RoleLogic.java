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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class RoleLogic extends AbstractTransactionalLogic<RoleTO> {

    protected final RoleDataBinder binder;

    protected final RoleDAO roleDAO;

    public RoleLogic(final RoleDataBinder binder, final RoleDAO roleDAO) {
        this.binder = binder;
        this.roleDAO = roleDAO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ROLE_READ + "')")
    @Transactional(readOnly = true)
    public RoleTO read(final String key) {
        Role role = roleDAO.findById(key).orElseThrow(() -> new NotFoundException("Role " + key));

        return binder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ROLE_LIST + "')")
    @Transactional(readOnly = true)
    public List<RoleTO> list() {
        return roleDAO.findAll().stream().map(binder::getRoleTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ROLE_CREATE + "')")
    public RoleTO create(final RoleTO roleTO) {
        return binder.getRoleTO(binder.create(roleTO));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ROLE_UPDATE + "')")
    public RoleTO update(final RoleTO roleTO) {
        Role role = roleDAO.findById(roleTO.getKey()).
                orElseThrow(() -> new NotFoundException("Role " + roleTO.getKey()));

        return binder.getRoleTO(binder.update(role, roleTO));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ROLE_DELETE + "')")
    public RoleTO delete(final String key) {
        if (RoleDAO.GROUP_OWNER_ROLE.equals(key)) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRole);
            sce.getElements().add("This Role cannot be deleted");
            throw sce;
        }

        Role role = roleDAO.findById(key).orElseThrow(() -> new NotFoundException("Role " + key));

        RoleTO deleted = binder.getRoleTO(role);
        roleDAO.delete(role);
        return deleted;
    }

    @PreAuthorize("isAuthenticated()")
    public String getAnyLayout(final String key) {
        Role role = roleDAO.findById(key).orElseThrow(() -> new NotFoundException("Role " + key));

        String consoleLayout = role.getAnyLayout();
        if (StringUtils.isBlank(consoleLayout)) {
            throw new NotFoundException("Console layout for role " + key);
        }

        return consoleLayout;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ROLE_UPDATE + "')")
    public void setAnyLayout(final String key, final String consoleLayout) {
        Role role = roleDAO.findById(key).orElseThrow(() -> new NotFoundException("Role " + key));

        role.setAnyLayout(consoleLayout);
        roleDAO.save(role);
    }

    @Override
    protected RoleTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof RoleTO roleTO) {
                    key = roleTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getRoleTO(roleDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
