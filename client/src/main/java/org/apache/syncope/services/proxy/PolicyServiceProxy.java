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
package org.apache.syncope.services.proxy;

import java.util.List;

import org.apache.syncope.client.to.AccountPolicyTO;
import org.apache.syncope.client.to.PasswordPolicyTO;
import org.apache.syncope.client.to.PolicyTO;
import org.apache.syncope.client.to.SyncPolicyTO;
import org.apache.syncope.services.PolicyService;
import org.apache.syncope.types.PolicyType;

public class PolicyServiceProxy extends SpringServiceProxy implements PolicyService {

    public PolicyServiceProxy(String baseUrl, SpringRestTemplate callback) {
        super(baseUrl, callback);
    }

    @Override
    public <T extends PolicyTO> T create(PolicyType type, final T policyTO) {
        @SuppressWarnings("unchecked")
        T result = (T) getRestTemplate().postForObject(baseUrl + "policy/{kind}/create", policyTO, policyTO.getClass(),
                typeToUrl(policyTO.getType()));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> T delete(PolicyType type, Long policyId) {
        T result = (T) getRestTemplate().getForObject(baseUrl + "policy/delete/{id}", getTOClass(type), policyId);
        return result;
    }

    private Class<? extends PolicyTO> getTOClass(PolicyType type) {
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
            throw new IllegalArgumentException("Policy Type not supported");
        }
    }

    @Override
    public <T extends PolicyTO> List<T> listByType(PolicyType type) {
        @SuppressWarnings("unchecked")
        List<T> result = getRestTemplate().getForObject(baseUrl + "policy/{kind}/list", List.class, typeToUrl(type));
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> T read(PolicyType type, Long policyId) {
        T result = (T) getRestTemplate().getForObject(baseUrl + "policy/read/{id}", getTOClass(type), policyId);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PolicyTO> T readGlobal(PolicyType type) {
        T result = (T) getRestTemplate().getForObject(baseUrl + "policy/{kind}/global/read", getTOClass(type),
                typeToUrl(type));
        return result;
    }

    private String typeToUrl(PolicyType type) {
        String url = type.name().toLowerCase();
        int index = url.indexOf("_");
        if (index != -1) {
            return url.substring(index + 1);
        } else {
            return url;
        }
    }

    @Override
    public <T extends PolicyTO> T update(PolicyType type, Long policyId, T policyTO) {
        @SuppressWarnings("unchecked")
        T result = (T) getRestTemplate().postForObject(baseUrl + "policy/{kind}/update", policyTO, policyTO.getClass(),
                typeToUrl(policyTO.getType()));
        return result;
    }
}
