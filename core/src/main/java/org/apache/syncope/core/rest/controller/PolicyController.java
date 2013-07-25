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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.PolicySubCategory;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.audit.AuditManager;
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
public class PolicyController extends AbstractController {

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyDataBinder binder;

    @PreAuthorize("hasRole('POLICY_CREATE')")
    public <T extends AbstractPolicyTO> T create(final T policyTO) {
        LOG.debug("Creating policy " + policyTO);

        final Policy policy = binder.getPolicy(null, policyTO);

        auditManager.audit(Category.policy, PolicySubCategory.create, Result.success,
                "Successfully created " + policy.getType().toString() + " policy: " + policy.getId());

        return binder.getPolicyTO(policyDAO.save(policy));
    }

    private <T extends AbstractPolicyTO, K extends Policy> T update(final T policyTO, final K policy) {
        LOG.debug("Updating policy " + policyTO);

        binder.getPolicy(policy, policyTO);
        K savedPolicy = policyDAO.save(policy);

        auditManager.audit(Category.policy, PolicySubCategory.update, Result.success,
                "Successfully updated policy (" + savedPolicy.getType() + "): " + savedPolicy.getId());

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

        auditManager.audit(Category.policy, PolicySubCategory.list, Result.success,
                "Successfully listed all policies (" + type + "): " + policyTOs.size());

        return policyTOs;
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public PasswordPolicyTO getGlobalPasswordPolicy() {
        LOG.debug("Reading global password policy");

        PasswordPolicy policy = policyDAO.getGlobalPasswordPolicy();
        if (policy == null) {
            throw new NotFoundException("No password policy found");
        }

        auditManager.audit(Category.policy, PolicySubCategory.read, Result.success,
                "Successfully read global password policy: " + policy.getId());

        return (PasswordPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public AccountPolicyTO getGlobalAccountPolicy() {
        LOG.debug("Reading global account policy");

        AccountPolicy policy = policyDAO.getGlobalAccountPolicy();
        if (policy == null) {
            throw new NotFoundException("No account policy found");
        }

        auditManager.audit(Category.policy, PolicySubCategory.read, Result.success,
                "Successfully read global account policy: " + policy.getId());

        return (AccountPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public SyncPolicyTO getGlobalSyncPolicy() {
        LOG.debug("Reading global sync policy");

        SyncPolicy policy = policyDAO.getGlobalSyncPolicy();
        if (policy == null) {
            throw new NotFoundException("No sync policy found");
        }

        auditManager.audit(Category.policy, PolicySubCategory.read, Result.success,
                "Successfully read global sync policy: " + policy.getId());

        return (SyncPolicyTO) binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('POLICY_READ')")
    public <T extends AbstractPolicyTO> T read(final Long id) {
        LOG.debug("Reading policy with id {}", id);

        Policy policy = policyDAO.find(id);
        if (policy == null) {
            throw new NotFoundException("Policy " + id + " not found");
        }

        auditManager.audit(Category.policy, PolicySubCategory.read, Result.success,
                "Successfully read policy (" + policy.getType() + "): " + policy.getId());

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

        auditManager.audit(Category.policy, PolicySubCategory.delete, Result.success,
                "Successfully deleted policy: " + id);

        return policyToDelete;
    }

    @PreAuthorize("hasRole('POLICY_LIST')")
    public Set<String> getSyncCorrelationRuleClasses() {
        final Set<String> correlationRules =
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.SYNC_CORRELATION_RULES);

        auditManager.audit(Category.policy, AuditElements.PolicySubCategory.getCorrelationRuleClasses,
                Result.success, "Successfully listed all correlation rule classes: " + correlationRules.size());

        return correlationRules;
    }
}
