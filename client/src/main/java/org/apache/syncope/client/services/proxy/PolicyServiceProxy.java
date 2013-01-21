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
package org.apache.syncope.client.services.proxy;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("unchecked")
public class PolicyServiceProxy extends SpringServiceProxy implements PolicyService {

    public PolicyServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public <T extends PolicyTO> T create(final PolicyType type, final T policyTO) {
        return (T) getRestTemplate().postForObject(baseUrl + "policy/{kind}/create", policyTO, policyTO.getClass(),
                typeToUrl(policyTO.getType()));
    }

    @Override
    public <T extends PolicyTO> T delete(final PolicyType type, final Long policyId) {
        return (T) getRestTemplate().getForObject(baseUrl + "policy/delete/{id}", getTOClass(type), policyId);
    }

    @Override
    public <T extends PolicyTO> List<T> listByType(final PolicyType type) {
        switch (type) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                return (List<T>) Arrays.asList(getRestTemplate().getForObject(
                        baseUrl + "policy/{kind}/list", AccountPolicyTO[].class,
                        type));

            case PASSWORD:
            case GLOBAL_PASSWORD:
                return (List<T>) Arrays.asList(getRestTemplate().getForObject(
                        baseUrl + "policy/{kind}/list", PasswordPolicyTO[].class,
                        type));

            case SYNC:
            case GLOBAL_SYNC:
                return (List<T>) Arrays.asList(getRestTemplate().getForObject(
                        baseUrl + "policy/{kind}/list", SyncPolicyTO[].class,
                        type));

            default:
                throw new IllegalArgumentException("Policy Type not supported: " + type);
        }
    }

    @Override
    public <T extends PolicyTO> T read(final PolicyType type, final Long policyId) {
        return (T) getRestTemplate().getForObject(baseUrl + "policy/read/{id}", getTOClass(type), policyId);
    }

    @Override
    public <T extends PolicyTO> T readGlobal(final PolicyType type) {
        return (T) getRestTemplate().getForObject(baseUrl + "policy/{kind}/global/read", getTOClass(type),
                typeToUrl(type));
    }

    @Override
    public <T extends PolicyTO> T update(final PolicyType type, final Long policyId, final T policyTO) {
        @SuppressWarnings("unchecked")
        T result = (T) getRestTemplate().postForObject(baseUrl + "policy/{kind}/update", policyTO, policyTO.getClass(),
                typeToUrl(policyTO.getType()));
        return result;
    }

    private Class<? extends PolicyTO> getTOClass(final PolicyType type) {
        switch (type) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                return AccountPolicyTO.class;

            case PASSWORD:
            case GLOBAL_PASSWORD:
                return PasswordPolicyTO.class;

            case SYNC:
            case GLOBAL_SYNC:
                return SyncPolicyTO.class;

            default:
                throw new IllegalArgumentException("Policy Type not supported: " + type);
        }
    }

    private String typeToUrl(final PolicyType type) {
        String url = type.name().toLowerCase();
        int index = url.indexOf("_");
        if (index != -1) {
            return url.substring(index + 1);
        } else {
            return url;
        }
    }
}
