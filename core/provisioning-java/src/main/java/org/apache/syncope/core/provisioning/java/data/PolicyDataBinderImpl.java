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
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.SyncPolicy;
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
    private RealmDAO realmDAO;

    @Autowired
    private EntityFactory entityFactory;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends AbstractPolicyTO> T getPolicyTO(final Policy policy) {
        final T policyTO;

        switch (policy.getType()) {
            case PASSWORD:
                policyTO = (T) new PasswordPolicyTO();
                ((PasswordPolicyTO) policyTO).setSpecification(policy.getSpecification(PasswordPolicySpec.class));
                break;

            case ACCOUNT:
                policyTO = (T) new AccountPolicyTO();
                ((AccountPolicyTO) policyTO).setSpecification(policy.getSpecification(AccountPolicySpec.class));
                ((AccountPolicyTO) policyTO).getResources().addAll(((AccountPolicy) policy).getResourceNames());
                break;

            case SYNC:
            default:
                policyTO = (T) new SyncPolicyTO();
                ((SyncPolicyTO) policyTO).setSpecification(policy.getSpecification(SyncPolicySpec.class));
        }

        policyTO.setKey(policy.getKey());
        policyTO.setDescription(policy.getDescription());

        for (ExternalResource resource : resourceDAO.findByPolicy(policy)) {
            policyTO.getUsedByResources().add(resource.getKey());
        }
        for (Realm realm : realmDAO.findByPolicy(policy)) {
            policyTO.getUsedByRealms().add(realm.getFullPath());
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
    public <T extends Policy> T getPolicy(final T policy, final AbstractPolicyTO policyTO) {
        if (policy != null && policy.getType() != policyTO.getType()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
            sce.getElements().add(String.format("Cannot update %s from %s", policy.getType(), policyTO.getType()));
            throw sce;
        }

        T result = policy;
        switch (policyTO.getType()) {
            case PASSWORD:
                if (!(policyTO instanceof PasswordPolicyTO)) {
                    throw new ClassCastException("Expected " + PasswordPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (result == null) {
                    result = (T) entityFactory.newEntity(PasswordPolicy.class);
                }
                result.setSpecification(((PasswordPolicyTO) policyTO).getSpecification());
                break;

            case ACCOUNT:
                if (!(policyTO instanceof AccountPolicyTO)) {
                    throw new ClassCastException("Expected " + AccountPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (result == null) {
                    result = (T) entityFactory.newEntity(AccountPolicy.class);
                }
                result.setSpecification(((AccountPolicyTO) policyTO).getSpecification());

                if (((AccountPolicy) result).getResources() != null
                        && !((AccountPolicy) result).getResources().isEmpty()) {
                    ((AccountPolicy) result).getResources().clear();
                }
                for (String resourceName : ((AccountPolicyTO) policyTO).getResources()) {
                    ExternalResource resource = getResource(resourceName);

                    if (resource != null) {
                        ((AccountPolicy) result).addResource(resource);
                    }
                }
                break;

            case SYNC:
            default:
                if (!(policyTO instanceof SyncPolicyTO)) {
                    throw new ClassCastException("Expected " + SyncPolicyTO.class.getName()
                            + ", found " + policyTO.getClass().getName());
                }
                if (result == null) {
                    result = (T) entityFactory.newEntity(SyncPolicy.class);
                }
                result.setSpecification(((SyncPolicyTO) policyTO).getSpecification());
        }

        result.setDescription(policyTO.getDescription());

        return result;
    }
}
