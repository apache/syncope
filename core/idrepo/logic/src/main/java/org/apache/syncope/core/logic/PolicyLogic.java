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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtils;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class PolicyLogic extends AbstractTransactionalLogic<PolicyTO> {

    protected final PolicyDAO policyDAO;

    protected final PolicyDataBinder binder;

    protected final PolicyUtilsFactory policyUtilsFactory;

    public PolicyLogic(
            final PolicyDAO policyDAO,
            final PolicyDataBinder binder,
            final PolicyUtilsFactory policyUtilsFactory) {

        this.policyDAO = policyDAO;
        this.binder = binder;
        this.policyUtilsFactory = policyUtilsFactory;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.POLICY_CREATE + "')")
    public <T extends PolicyTO> T create(final PolicyType type, final T policyTO) {
        PolicyUtils policyUtils = policyUtilsFactory.getInstance(policyTO);
        if (policyUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + policyUtils.getType());
            throw sce;
        }

        return binder.getPolicyTO(policyDAO.save(binder.create(policyTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.POLICY_UPDATE + "')")
    public PolicyTO update(final PolicyType type, final PolicyTO policyTO) {
        Policy policy = Optional.ofNullable(policyDAO.<Policy>find(policyTO.getKey())).
                orElseThrow(() -> new NotFoundException("Policy " + policyTO.getKey() + " not found"));

        PolicyUtils policyUtils = policyUtilsFactory.getInstance(policy);
        if (policyUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + policyUtils.getType());
            throw sce;
        }

        return binder.getPolicyTO(policyDAO.save(binder.update(policy, policyTO)));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.POLICY_LIST + "')")
    @Transactional(readOnly = true)
    public <T extends PolicyTO> List<T> list(final PolicyType type) {
        PolicyUtils policyUtils = policyUtilsFactory.getInstance(type);

        return policyDAO.find(policyUtils.policyClass()).stream().
                <T>map(binder::getPolicyTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.POLICY_READ + "')")
    @Transactional(readOnly = true)
    public <T extends PolicyTO> T read(final PolicyType type, final String key) {
        Policy policy = Optional.ofNullable(policyDAO.<Policy>find(key)).
                orElseThrow(() -> new NotFoundException("Policy " + key + " not found"));

        PolicyUtils policyUtils = policyUtilsFactory.getInstance(policy);
        if (type != null && policyUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + policyUtils.getType());
            throw sce;
        }

        return binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.POLICY_DELETE + "')")
    public <T extends PolicyTO> T delete(final PolicyType type, final String key) {
        Policy policy = Optional.ofNullable(policyDAO.<Policy>find(key)).
                orElseThrow(() -> new NotFoundException("Policy " + key + " not found"));

        PolicyUtils policyUtils = policyUtilsFactory.getInstance(policy);
        if (type != null && policyUtils.getType() != type) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Found " + type + ", expected " + policyUtils.getType());
            throw sce;
        }

        T deleted = binder.getPolicyTO(policy);
        policyDAO.delete(policy);

        return deleted;
    }

    @Override
    protected PolicyTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        if (ArrayUtils.isEmpty(args) || args.length != 2) {
            throw new UnresolvedReferenceException();
        }

        try {
            final String key;
            final PolicyType type;

            if (args[0] instanceof PolicyType) {
                type = (PolicyType) args[0];
            } else {
                throw new RuntimeException("Invalid Policy type");
            }

            if (args[1] instanceof String) {
                key = (String) args[1];
            } else if (args[1] instanceof PolicyTO) {
                key = ((PolicyTO) args[1]).getKey();
            } else {
                throw new RuntimeException("Invalid ClientApp key");
            }

            return read(type, key);
        } catch (Throwable t) {
            throw new UnresolvedReferenceException();
        }
    }
}
