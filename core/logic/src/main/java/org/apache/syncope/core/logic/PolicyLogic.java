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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class PolicyLogic extends AbstractTransactionalLogic<AbstractPolicyTO> {

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private PolicyDataBinder binder;

    @PreAuthorize("hasRole('" + StandardEntitlement.POLICY_CREATE + "')")
    public <T extends AbstractPolicyTO> T create(final T policyTO) {
        return binder.getPolicyTO(policyDAO.save(binder.create(policyTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.POLICY_UPDATE + "')")
    public AbstractPolicyTO update(final AbstractPolicyTO policyTO) {
        Policy policy = policyDAO.find(policyTO.getKey());
        return binder.getPolicyTO(policyDAO.save(binder.update(policy, policyTO)));
    }

    private Class<? extends Policy> getPolicyClass(final PolicyType policyType) {
        switch (policyType) {
            case ACCOUNT:
                return AccountPolicy.class;

            case PASSWORD:
                return PasswordPolicy.class;

            case PULL:
                return PullPolicy.class;

            case PUSH:
            default:
                return PushPolicy.class;
        }
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.POLICY_LIST + "')")
    public <T extends AbstractPolicyTO> List<T> list(final PolicyType type) {
        return CollectionUtils.collect(policyDAO.find(getPolicyClass(type)), new Transformer<Policy, T>() {

            @Override
            public T transform(final Policy input) {
                return binder.getPolicyTO(input);
            }
        }, new ArrayList<T>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.POLICY_READ + "')")
    public <T extends AbstractPolicyTO> T read(final String key) {
        Policy policy = policyDAO.find(key);
        if (policy == null) {
            throw new NotFoundException("Policy " + key + " not found");
        }

        return binder.getPolicyTO(policy);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.POLICY_DELETE + "')")
    public <T extends AbstractPolicyTO> T delete(final String key) {
        Policy policy = policyDAO.find(key);
        if (policy == null) {
            throw new NotFoundException("Policy " + key + " not found");
        }

        T policyToDelete = binder.getPolicyTO(policy);
        policyDAO.delete(policy);

        return policyToDelete;
    }

    @Override
    protected AbstractPolicyTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AbstractPolicyTO) {
                    key = ((AbstractPolicyTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getPolicyTO(policyDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
