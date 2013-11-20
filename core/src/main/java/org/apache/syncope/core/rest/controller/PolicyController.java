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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.rest.data.PolicyDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class PolicyController extends AbstractTransactionalController<AbstractPolicyTO> {

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyDataBinder binder;

    @PreAuthorize("hasRole('POLICY_CREATE')")
    public <T extends AbstractPolicyTO> T create(final T policyTO) {
        LOG.debug("Creating policy " + policyTO);
        return binder.getPolicyTO(policyDAO.save(binder.getPolicy(null, policyTO)));
    }

    private <T extends AbstractPolicyTO, K extends Policy> T update(final T policyTO, final K policy) {
        LOG.debug("Updating policy " + policyTO);
        binder.getPolicy(policy, policyTO);
        K savedPolicy = policyDAO.save(policy);
        return binder.getPolicyTO(savedPolicy);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    public PasswordPolicyTO update(final PasswordPolicyTO policyTO) {
        Policy policy = policyDAO.find(policyTO.getId());
        if (!(policy instanceof PasswordPolicy)) {
            throw new NotFoundException("PasswordPolicy with id " + policyTO.getId());
        }

        return update(policyTO, policy);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    public AccountPolicyTO update(final AccountPolicyTO policyTO) {
        Policy policy = policyDAO.find(policyTO.getId());
        if (!(policy instanceof AccountPolicy)) {
            throw new NotFoundException("AccountPolicy with id " + policyTO.getId());
        }

        return update(policyTO, policy);
    }

    @PreAuthorize("hasRole('POLICY_UPDATE')")
    public SyncPolicyTO update(final SyncPolicyTO policyTO) {
        Policy policy = policyDAO.find(policyTO.getId());
        if (!(policy instanceof SyncPolicy)) {
            throw new NotFoundException("SyncPolicy with id " + policyTO.getId());
        }

        return update(policyTO, policy);
    }

    @PreAuthorize("hasRole('POLICY_LIST')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicyTO> List<T> list(final PolicyType type) {
        LOG.debug("Listing policies");

        List<? extends Policy> policies = policyDAO.find(type);

        final List<T> policyTOs = new ArrayList<T>();
        for (Policy policy : policies) {
            policyTOs.add((T) binder.getPolicyTO(policy));
        }

        return policyTOs;
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public PasswordPolicyTO getGlobalPasswordPolicy() {
        LOG.debug("Reading global password policy");

        PasswordPolicy policy = policyDAO.getGlobalPasswordPolicy();
        if (policy == null) {
            throw new NotFoundException("No password policy found");
        }

        return (PasswordPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public AccountPolicyTO getGlobalAccountPolicy() {
        LOG.debug("Reading global account policy");

        AccountPolicy policy = policyDAO.getGlobalAccountPolicy();
        if (policy == null) {
            throw new NotFoundException("No account policy found");
        }

        return (AccountPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public SyncPolicyTO getGlobalSyncPolicy() {
        LOG.debug("Reading global sync policy");

        SyncPolicy policy = policyDAO.getGlobalSyncPolicy();
        if (policy == null) {
            throw new NotFoundException("No sync policy found");
        }

        return (SyncPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public <T extends AbstractPolicyTO> T read(final Long id) {
        LOG.debug("Reading policy with id {}", id);

        Policy policy = policyDAO.find(id);
        if (policy == null) {
            throw new NotFoundException("Policy " + id + " not found");
        }

        return binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_DELETE')")
    public <T extends AbstractPolicyTO> T delete(final Long id) {
        Policy policy = policyDAO.find(id);
        if (policy == null) {
            throw new NotFoundException("Policy " + id + " not found");
        }

        T policyToDelete = binder.getPolicyTO(policy);
        policyDAO.delete(policy);

        return policyToDelete;
    }

    @PreAuthorize("hasRole('POLICY_LIST')")
    public Set<String> getSyncCorrelationRuleClasses() {
        return classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.SYNC_CORRELATION_RULES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPolicyTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof AbstractPolicyTO) {
                    id = ((AbstractPolicyTO) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return binder.getPolicyTO(policyDAO.find(id));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
