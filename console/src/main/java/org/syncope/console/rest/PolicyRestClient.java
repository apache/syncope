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
package org.syncope.console.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.AccountPolicyTO;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.SyncPolicyTO;
import org.syncope.types.PolicyType;

/**
 * Console client for invoking Rest Policy services.
 */
@Component
public class PolicyRestClient extends AbstractBaseRestClient {

    public <T extends PolicyTO> T getGlobalPolicy(final PolicyType type) {

        T policy = null;

        try {

            switch (type) {
                case GLOBAL_ACCOUNT:
                    try {
                        policy = (T) restTemplate.getForObject(
                                baseURL + "policy/account/global/read",
                                AccountPolicyTO.class);
                    } catch (Exception e) {
                        LOG.debug("No account policy found", e);
                        policy = (T) new AccountPolicyTO();
                    }
                    break;
                case GLOBAL_PASSWORD:
                    try {
                        policy = (T) restTemplate.getForObject(
                                baseURL + "policy/password/global/read",
                                PasswordPolicyTO.class);
                    } catch (Exception e) {
                        LOG.debug("No password policy found", e);
                        policy = (T) new PasswordPolicyTO();
                    }
                    break;
                case GLOBAL_SYNC:
                    try {
                        policy = (T) restTemplate.getForObject(
                                baseURL + "policy/sync/global/read",
                                SyncPolicyTO.class);
                    } catch (Exception e) {
                        LOG.debug("No password policy found", e);
                        policy = (T) new SyncPolicyTO();
                    }
                    break;
                default:
                    throw new Exception("Invalid policy type");
            }

        } catch (Exception ignore) {
            LOG.error("Invalid policy type", ignore);
        }

        return policy;
    }

    public <T extends PolicyTO> List<T> getPolicies(final PolicyType type) {
        final List<T> res = new ArrayList<T>();

        T[] policies = null;

        final Class reference;
        final Class globalReference;
        final String policy;

        try {

            switch (type) {
                case ACCOUNT:
                    reference = AccountPolicyTO[].class;
                    globalReference = AccountPolicyTO.class;
                    policy = "account";
                    break;
                case PASSWORD:
                    reference = PasswordPolicyTO[].class;
                    globalReference = PasswordPolicyTO.class;
                    policy = "password";
                    break;
                case SYNC:
                    reference = SyncPolicyTO[].class;
                    globalReference = SyncPolicyTO.class;
                    policy = "sync";
                    break;
                default:
                    throw new Exception("Invalid policy type");
            }

            try {
                policies = (T[]) restTemplate.getForObject(
                        baseURL + "policy/" + policy + "/list",
                        reference);
            } catch (Exception ignore) {
                LOG.debug("No policy found", ignore);
            }

            if (policies != null) {
                res.addAll(Arrays.asList(policies));
            }

            PolicyTO globalPolicy = null;

            try {
                globalPolicy = (T) restTemplate.getForObject(
                        baseURL + "policy/" + policy + "/global/read",
                        globalReference);
            } catch (Exception ignore) {
                LOG.warn("No global policy found", ignore);
            }

            if (globalPolicy != null) {
                res.add(0, (T) globalPolicy);
            }

        } catch (Exception ignore) {
            LOG.error("No policy found", ignore);
        }

        return res;
    }

    public <T extends PolicyTO> T createPolicy(final T policy)
            throws InvalidPolicyType {

        switch (policy.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                return (T) restTemplate.postForObject(
                        baseURL + "policy/account/create",
                        policy, AccountPolicyTO.class);
            case GLOBAL_PASSWORD:
            case PASSWORD:
                return (T) restTemplate.postForObject(
                        baseURL + "policy/password/create",
                        policy, PasswordPolicyTO.class);
            case GLOBAL_SYNC:
            case SYNC:
                return (T) restTemplate.postForObject(
                        baseURL + "policy/sync/create",
                        policy, SyncPolicyTO.class);
            default:
                throw new InvalidPolicyType("Invalid type " + policy.getType());
        }
    }

    public <T extends PolicyTO> T updatePolicy(final T policy)
            throws InvalidPolicyType {

        switch (policy.getType()) {
            case GLOBAL_ACCOUNT:
            case ACCOUNT:
                return (T) restTemplate.postForObject(
                        baseURL + "policy/account/update",
                        policy, AccountPolicyTO.class);
            case GLOBAL_PASSWORD:
            case PASSWORD:
                return (T) restTemplate.postForObject(
                        baseURL + "policy/password/update",
                        policy, PasswordPolicyTO.class);
            case GLOBAL_SYNC:
            case SYNC:
                return (T) restTemplate.postForObject(
                        baseURL + "policy/sync/update",
                        policy, SyncPolicyTO.class);
            default:
                throw new InvalidPolicyType("Invalid type " + policy.getType());
        }
    }

    public void delete(final Long id) {
        restTemplate.delete(baseURL + "policy/delete/" + id);
    }
}
