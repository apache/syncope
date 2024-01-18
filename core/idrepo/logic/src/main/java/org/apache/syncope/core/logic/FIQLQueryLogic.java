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
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.FIQLQueryTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.provisioning.api.data.FIQLQueryDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class FIQLQueryLogic extends AbstractTransactionalLogic<FIQLQueryTO> {

    protected final FIQLQueryDataBinder binder;

    protected final FIQLQueryDAO fiqlQueryDAO;

    protected final UserDAO userDAO;

    protected final SecurityProperties securityProperties;

    public FIQLQueryLogic(
            final FIQLQueryDataBinder binder,
            final FIQLQueryDAO fiqlQueryDAO,
            final UserDAO userDAO,
            final SecurityProperties securityProperties) {

        this.binder = binder;
        this.fiqlQueryDAO = fiqlQueryDAO;
        this.userDAO = userDAO;
        this.securityProperties = securityProperties;
    }

    protected boolean skip() {
        return securityProperties.getAdminUser().equals(AuthContextUtils.getUsername())
                || securityProperties.getAnonymousUser().equals(AuthContextUtils.getUsername());
    }

    protected void securityChecks(final String owner) {
        if (!AuthContextUtils.getUsername().equals(owner)) {
            throw new DelegatedAdministrationException(SyncopeConstants.ROOT_REALM, AnyTypeKind.USER.name(), owner);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public FIQLQueryTO read(final String key) {
        if (skip()) {
            throw new NotFoundException("FIQLQuery " + key);
        }

        FIQLQuery fiqlQuery = fiqlQueryDAO.findById(key).
                orElseThrow(() -> new NotFoundException("FIQLQuery " + key));

        securityChecks(fiqlQuery.getOwner().getUsername());

        return binder.getFIQLQueryTO(fiqlQuery);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<FIQLQueryTO> list(final String target) {
        if (skip()) {
            return List.of();
        }

        return fiqlQueryDAO.findByOwner(
                userDAO.findByUsername(AuthContextUtils.getUsername()).
                        orElseThrow(() -> new NotFoundException("User " + AuthContextUtils.getUsername())), target).
                stream().
                map(binder::getFIQLQueryTO).toList();
    }

    @PreAuthorize("isAuthenticated()")
    public FIQLQueryTO create(final FIQLQueryTO fiqlQueryTO) {
        if (skip()) {
            throw new NotFoundException("FIQLQuery " + fiqlQueryTO.getKey());
        }

        return binder.getFIQLQueryTO(fiqlQueryDAO.save(binder.create(fiqlQueryTO)));
    }

    @PreAuthorize("isAuthenticated()")
    public FIQLQueryTO update(final FIQLQueryTO fiqlQueryTO) {
        if (skip()) {
            throw new NotFoundException("FIQLQuery " + fiqlQueryTO.getKey());
        }

        FIQLQuery fiqlQuery = fiqlQueryDAO.findById(fiqlQueryTO.getKey()).
                orElseThrow(() -> new NotFoundException("FIQLQuery " + fiqlQueryTO.getKey()));

        securityChecks(fiqlQuery.getOwner().getUsername());

        return binder.getFIQLQueryTO(fiqlQueryDAO.save(binder.update(fiqlQuery, fiqlQueryTO)));
    }

    @PreAuthorize("isAuthenticated()")
    public FIQLQueryTO delete(final String key) {
        if (skip()) {
            throw new NotFoundException("FIQLQuery " + key);
        }

        FIQLQuery fiqlQuery = fiqlQueryDAO.findById(key).
                orElseThrow(() -> new NotFoundException("FIQLQuery " + key));

        securityChecks(fiqlQuery.getOwner().getUsername());

        FIQLQueryTO deleted = binder.getFIQLQueryTO(fiqlQuery);
        fiqlQueryDAO.deleteById(key);
        return deleted;
    }

    @Override
    protected FIQLQueryTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof FIQLQueryTO fIQLQueryTO) {
                    key = fIQLQueryTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getFIQLQueryTO(fiqlQueryDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
