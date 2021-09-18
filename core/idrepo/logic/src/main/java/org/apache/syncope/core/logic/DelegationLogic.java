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
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.DelegationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class DelegationLogic extends AbstractTransactionalLogic<DelegationTO> {

    protected final DelegationDataBinder binder;

    protected final DelegationDAO delegationDAO;

    protected final UserDAO userDAO;

    public DelegationLogic(
            final DelegationDataBinder binder,
            final DelegationDAO delegationDAO,
            final UserDAO userDAO) {

        this.binder = binder;
        this.delegationDAO = delegationDAO;
        this.userDAO = userDAO;
    }

    protected void securityChecks(final String delegating, final String entitlement) {
        if (!AuthContextUtils.getAuthorizations().keySet().contains(entitlement)
                && (delegating == null || !delegating.equals(userDAO.findKey(AuthContextUtils.getUsername())))) {

            throw new DelegatedAdministrationException(
                    SyncopeConstants.ROOT_REALM, AnyTypeKind.USER.name(), delegating);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public DelegationTO read(final String key) {
        Delegation delegation = delegationDAO.find(key);
        if (delegation == null) {
            LOG.error("Could not find delegation '" + key + "'");
            throw new NotFoundException(key);
        }

        securityChecks(delegation.getDelegating().getKey(), IdRepoEntitlement.DELEGATION_READ);

        return binder.getDelegationTO(delegation);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<DelegationTO> list() {
        Stream<DelegationTO> delegations = delegationDAO.findAll().stream().map(binder::getDelegationTO);

        if (!AuthContextUtils.getAuthorizations().keySet().contains(IdRepoEntitlement.DELEGATION_LIST)) {
            String authUserKey = userDAO.findKey(AuthContextUtils.getUsername());
            delegations = delegations.filter(delegation -> delegation.getDelegating().equals(authUserKey));
        }

        return delegations.collect(Collectors.toList());
    }

    @PreAuthorize("isAuthenticated()")
    public DelegationTO create(final DelegationTO delegationTO) {
        if (delegationTO.getDelegating() != null
                && !SyncopeConstants.UUID_PATTERN.matcher(delegationTO.getDelegating()).matches()) {

            delegationTO.setDelegating(userDAO.findKey(delegationTO.getDelegating()));
        }
        if (delegationTO.getDelegated() != null
                && !SyncopeConstants.UUID_PATTERN.matcher(delegationTO.getDelegated()).matches()) {

            delegationTO.setDelegated(userDAO.findKey(delegationTO.getDelegated()));
        }

        securityChecks(delegationTO.getDelegating(), IdRepoEntitlement.DELEGATION_CREATE);

        return binder.getDelegationTO(delegationDAO.save(binder.create(delegationTO)));
    }

    @PreAuthorize("isAuthenticated()")
    public DelegationTO update(final DelegationTO delegationTO) {
        Delegation delegation = delegationDAO.find(delegationTO.getKey());
        if (delegation == null) {
            LOG.error("Could not find delegation '" + delegationTO.getKey() + "'");
            throw new NotFoundException(delegationTO.getKey());
        }

        securityChecks(delegation.getDelegating().getKey(), IdRepoEntitlement.DELEGATION_UPDATE);

        return binder.getDelegationTO(delegationDAO.save(binder.update(delegation, delegationTO)));
    }

    @PreAuthorize("isAuthenticated()")
    public DelegationTO delete(final String key) {
        Delegation delegation = delegationDAO.find(key);
        if (delegation == null) {
            LOG.error("Could not find delegation '" + key + "'");

            throw new NotFoundException(key);
        }

        securityChecks(delegation.getDelegating().getKey(), IdRepoEntitlement.DELEGATION_DELETE);

        DelegationTO deleted = binder.getDelegationTO(delegation);
        delegationDAO.delete(key);
        return deleted;
    }

    @Override
    protected DelegationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof DelegationTO) {
                    key = ((DelegationTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getDelegationTO(delegationDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
