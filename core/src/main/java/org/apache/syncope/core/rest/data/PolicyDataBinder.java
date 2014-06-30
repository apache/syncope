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
package org.apache.syncope.core.rest.data;

import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.AccountPolicySpec;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.PasswordPolicySpec;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.beans.AccountPolicy;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PolicyDataBinder {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(PolicyDataBinder.class);

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private RoleDAO roleDAO;

    private boolean isGlobalPolicy(final PolicyType policyType) {
        boolean isGlobal;
        switch (policyType) {
            case GLOBAL_PASSWORD:
            case GLOBAL_ACCOUNT:
            case GLOBAL_SYNC:
                isGlobal = true;
                break;

            default:
                isGlobal = false;
        }

        return isGlobal;
    }

    /**
     * Get policy TO from policy bean.
     *
     * @param policy bean.
     * @return policy TO.
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicyTO> T getPolicyTO(final Policy policy) {
        final T policyTO;

        final boolean isGlobal = isGlobalPolicy(policy.getType());

        switch (policy.getType()) {
            case GLOBAL_PASSWORD:
            case PASSWORD:
                if (!(policy.getSpecification() instanceof PasswordPolicySpec)) {
                    throw new ClassCastException("Expected " + PasswordPolicySpec.class.getName()
                            + ", found " + policy.getSpecification().getClass().getName());
                }
                policyTO = (T) new PasswordPolicyTO(isGlobal);
                ((PasswordPolicyTO) policyTO).setSpecification((PasswordPolicySpec) policy.getSpecification());
                break;

            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                if (!(policy.getSpecification() instanceof AccountPolicySpec)) {
                    throw new ClassCastException("Expected " + AccountPolicySpec.class.getName()
                            + ", found " + policy.getSpecification().getClass().getName());
                }
                policyTO = (T) new AccountPolicyTO(isGlobal);
                ((AccountPolicyTO) policyTO).setSpecification((AccountPolicySpec) policy.getSpecification());
                ((AccountPolicyTO) policyTO).getResources().addAll(((AccountPolicy) policy).getResourceNames());
                break;

            case GLOBAL_SYNC:
            case SYNC:
            default:
                if (!(policy.getSpecification() instanceof SyncPolicySpec)) {
                    throw new ClassCastException("Expected " + SyncPolicySpec.class.getName()
                            + ", found " + policy.getSpecification().getClass().getName());

                }
                policyTO = (T) new SyncPolicyTO(isGlobal);
                ((SyncPolicyTO) policyTO).setSpecification((SyncPolicySpec) policy.getSpecification());
        }

        policyTO.setId(policy.getId());
        policyTO.setDescription(policy.getDescription());

        for (ExternalResource resource : resourceDAO.findByPolicy(policy)) {
            policyTO.getUsedByResources().add(resource.getName());
        }
        if (isGlobal) {
            for (ExternalResource resource : resourceDAO.findWithoutPolicy(policy.getType())) {
                policyTO.getUsedByResources().add(resource.getName());
            }
        }
        for (SyncopeRole role : roleDAO.findByPolicy(policy)) {
            policyTO.getUsedByRoles().add(role.getId());
        }
        if (isGlobal) {
            for (SyncopeRole role : roleDAO.findWithoutPolicy(policy.getType())) {
                policyTO.getUsedByRoles().add(role.getId());
            }
        }

        return policyTO;
    }

    private ExternalResource getResource(final String resourceName) {
        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            LOG.debug("Ignoring invalid resource {} ", resourceName);
        }

        return resource;
    }

    @SuppressWarnings("unchecked")
    public <T extends Policy> T getPolicy(T policy, final AbstractPolicyTO policyTO) {
        if (policy != null && policy.getType() != policyTO.getType()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
            sce.getElements().add(String.format("Cannot update %s from %s", policy.getType(), policyTO.getType()));
            throw sce;
        }

        final boolean isGlobal = isGlobalPolicy(policyTO.getType());

        switch (policyTO.getType()) {
            case GLOBAL_PASSWORD:
            case PASSWORD:
                if (!(policyTO instanceof PasswordPolicyTO)) {
                    throw new ClassCastException("Expected " + PasswordPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (policy == null) {
                    policy = (T) new PasswordPolicy(isGlobal);
                }
                policy.setSpecification(((PasswordPolicyTO) policyTO).getSpecification());
                break;

            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                if (!(policyTO instanceof AccountPolicyTO)) {
                    throw new ClassCastException("Expected " + AccountPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (policy == null) {
                    policy = (T) new AccountPolicy(isGlobal);
                }
                policy.setSpecification(((AccountPolicyTO) policyTO).getSpecification());

                ((AccountPolicy) policy).getResources().clear();
                for (String resourceName : ((AccountPolicyTO) policyTO).getResources()) {
                    ExternalResource resource = getResource(resourceName);

                    if (resource != null) {
                        ((AccountPolicy) policy).addResource(resource);
                    }
                }
                break;

            case GLOBAL_SYNC:
            case SYNC:
            default:
                if (!(policyTO instanceof SyncPolicyTO)) {
                    throw new ClassCastException("Expected " + SyncPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (policy == null) {
                    policy = (T) new SyncPolicy(isGlobal);
                }
                policy.setSpecification(((SyncPolicyTO) policyTO).getSpecification());
        }

        policy.setDescription(policyTO.getDescription());

        return policy;
    }
}
