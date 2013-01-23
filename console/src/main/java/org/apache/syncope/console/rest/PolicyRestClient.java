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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.List;

import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Policy services.
 */
@Component
public class PolicyRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    @SuppressWarnings("unchecked")
    public <T extends PolicyTO> T getGlobalPolicy(final PolicyType type) {

        T policy = null;

        try {
            policy = getService(PolicyService.class).readGlobal(type);
        } catch (Exception e) {
            LOG.warn("No global " + type + " policy found", e);
            switch (type) {
            case GLOBAL_ACCOUNT:
                policy = (T) new AccountPolicyTO();
                break;
            case GLOBAL_PASSWORD:
                policy = (T) new PasswordPolicyTO();
                break;
            case GLOBAL_SYNC:
                policy = (T) new SyncPolicyTO();
                break;
            default:
                LOG.warn("Invalid policy type");
            }
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    public <T extends PolicyTO> List<T> getPolicies(final PolicyType type, final boolean includeGlobal) {
        final List<T> res = new ArrayList<T>();
        List<T> policies = new ArrayList<T>();

        try {
            policies = (List<T>) getService(PolicyService.class).list(type);
            res.addAll(policies);
        } catch (Exception ignore) {
            LOG.debug("No policy found", ignore);
        }

        if (includeGlobal) {
            try {
                PolicyTO globalPolicy = getGlobalPolicy(type);
                res.add(0, (T) globalPolicy);
            } catch (Exception ignore) {
                LOG.warn("No global policy found", ignore);
            }
        }

        return res;
    }

    public <T extends PolicyTO> void createPolicy(final T policy) {
        getService(PolicyService.class).create(policy.getType(), policy);
    }

    public <T extends PolicyTO> void updatePolicy(final T policy) {
        getService(PolicyService.class).update(policy.getType(), policy.getId(), policy);
    }

    public void delete(final Long id, final Class<? extends PolicyTO> policyClass) {
        getService(PolicyService.class).delete(getPolicyType(policyClass), id);
    }

    private PolicyType getPolicyType(final Class<? extends PolicyTO> clazz) {
        if (AccountPolicyTO.class.equals(clazz)) {
            return PolicyType.ACCOUNT;
        } else if (PasswordPolicyTO.class.equals(clazz)) {
            return PolicyType.PASSWORD;
        } else if (SyncPolicyTO.class.equals(clazz)) {
            return PolicyType.SYNC;
        } else {
            throw new IllegalArgumentException("Policy Type not supported");
        }
    }
}
