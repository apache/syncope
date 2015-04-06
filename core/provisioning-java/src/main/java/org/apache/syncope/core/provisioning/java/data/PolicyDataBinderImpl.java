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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.AccountPolicyTO;
import org.apache.syncope.common.lib.to.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.SyncPolicyTO;
import org.apache.syncope.common.lib.types.AccountPolicySpec;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PasswordPolicySpec;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.SyncPolicy;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PolicyDataBinderImpl implements PolicyDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PolicyDataBinder.class);

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private EntityFactory entityFactory;

    /**
     * Get policy TO from policy bean.
     *
     * @param policy bean.
     * @return policy TO.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractPolicyTO> T getPolicyTO(final Policy policy) {
        final T policyTO;

        switch (policy.getType()) {
            case GLOBAL_PASSWORD:
            case PASSWORD:
                policyTO = (T) new PasswordPolicyTO(policy.getType().isGlobal());
                ((PasswordPolicyTO) policyTO).setSpecification(policy.getSpecification(PasswordPolicySpec.class));
                break;

            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                policyTO = (T) new AccountPolicyTO(policy.getType().isGlobal());
                ((AccountPolicyTO) policyTO).setSpecification(policy.getSpecification(AccountPolicySpec.class));
                ((AccountPolicyTO) policyTO).getResources().addAll(((AccountPolicy) policy).getResourceNames());
                break;

            case GLOBAL_SYNC:
            case SYNC:
            default:
                policyTO = (T) new SyncPolicyTO(policy.getType().isGlobal());
                ((SyncPolicyTO) policyTO).setSpecification(policy.getSpecification(SyncPolicySpec.class));
        }

        policyTO.setKey(policy.getKey());
        policyTO.setDescription(policy.getDescription());

        for (ExternalResource resource : resourceDAO.findByPolicy(policy)) {
            policyTO.getUsedByResources().add(resource.getKey());
        }
        if (policy.getType().isGlobal()) {
            for (ExternalResource resource : resourceDAO.findWithoutPolicy(policy.getType())) {
                policyTO.getUsedByResources().add(resource.getKey());
            }
        }
        for (Group group : groupDAO.findByPolicy(policy)) {
            policyTO.getUsedByGroups().add(group.getKey());
        }
        if (policy.getType().isGlobal()) {
            for (Group group : groupDAO.findWithoutPolicy(policy.getType())) {
                policyTO.getUsedByGroups().add(group.getKey());
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
    @Override
    public <T extends Policy> T getPolicy(T policy, final AbstractPolicyTO policyTO) {
        if (policy != null && policy.getType() != policyTO.getType()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
            sce.getElements().add(String.format("Cannot update %s from %s", policy.getType(), policyTO.getType()));
            throw sce;
        }

        switch (policyTO.getType()) {
            case GLOBAL_PASSWORD:
            case PASSWORD:
                if (!(policyTO instanceof PasswordPolicyTO)) {
                    throw new ClassCastException("Expected " + PasswordPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (policy == null) {
                    policy = (T) entityFactory.newPolicy(PasswordPolicy.class, policyTO.getType().isGlobal());
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
                    policy = (T) entityFactory.newPolicy(AccountPolicy.class, policyTO.getType().isGlobal());
                }
                policy.setSpecification(((AccountPolicyTO) policyTO).getSpecification());

                if (((AccountPolicy) policy).getResources() != null
                        && !((AccountPolicy) policy).getResources().isEmpty()) {
                    ((AccountPolicy) policy).getResources().clear();
                }
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
                    policy = (T) entityFactory.newPolicy(SyncPolicy.class, policyTO.getType().isGlobal());
                }
                policy.setSpecification(((SyncPolicyTO) policyTO).getSpecification());
        }

        policy.setDescription(policyTO.getDescription());

        return policy;
    }
}
