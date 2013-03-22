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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.to.AccountPolicyTO;
import org.apache.syncope.common.to.CorrelationRuleClassTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PolicyTO;
import org.apache.syncope.common.to.SyncPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("unchecked")
public class PolicyServiceProxy extends SpringServiceProxy implements PolicyService {

    public PolicyServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public <T extends PolicyTO> Response create(final PolicyType type, final T policyTO) {
        PolicyTO policy = getRestTemplate().postForObject(baseUrl + "policy/{kind}/create", policyTO,
                policyTO.getClass(), typeToUrl(policyTO.getType()));

        return Response.created(URI.create(baseUrl + "policy/read/" + policy.getId() + ".json")).build();
    }

    @Override
    public <T extends PolicyTO> void delete(final PolicyType type, final Long policyId) {
        getRestTemplate().getForObject(baseUrl + "policy/delete/{id}", getTOClass(type), policyId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PolicyTO> List<T> list(final PolicyType type) {
        List<T> result = null;

        switch (type) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                result = (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "policy/{kind}/list",
                        AccountPolicyTO[].class, type));
                break;

            case PASSWORD:
            case GLOBAL_PASSWORD:
                result = (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "policy/{kind}/list",
                        PasswordPolicyTO[].class, type));
                break;

            case SYNC:
            case GLOBAL_SYNC:
                result = (List<T>) Arrays.asList(getRestTemplate().getForObject(baseUrl + "policy/{kind}/list",
                        SyncPolicyTO[].class, type));
                break;

            default:
                throw new IllegalArgumentException("Policy Type not supported: " + type);
        }

        return result;
    }

    @Override
    public <T extends PolicyTO> T read(final PolicyType type, final Long policyId) {
        return (T) getRestTemplate().getForObject(baseUrl + "policy/read/{id}.json", getTOClass(type), policyId);
    }

    @Override
    public <T extends PolicyTO> T readGlobal(final PolicyType type) {
        return (T) getRestTemplate().getForObject(baseUrl + "policy/{kind}/global/read.json", getTOClass(type),
                typeToUrl(type));
    }

    @Override
    public <T extends PolicyTO> void update(final PolicyType type, final Long policyId, final T policyTO) {
        getRestTemplate().postForObject(baseUrl + "policy/{kind}/update", policyTO, policyTO.getClass(),
                typeToUrl(policyTO.getType()));
    }

    @SuppressWarnings("unchecked")
    private <T extends PolicyTO> Class<T> getTOClass(final PolicyType type) {
        Class<T> result = null;

        switch (type) {
            case ACCOUNT:
            case GLOBAL_ACCOUNT:
                result = (Class<T>) AccountPolicyTO.class;
                break;

            case PASSWORD:
            case GLOBAL_PASSWORD:
                result = (Class<T>) PasswordPolicyTO.class;
                break;

            case SYNC:
            case GLOBAL_SYNC:
                result = (Class<T>) SyncPolicyTO.class;
                break;

            default:
                throw new IllegalArgumentException("Policy Type not supported: " + type);
        }

        return result;
    }

    private String typeToUrl(final PolicyType type) {
        String url = type.name().toLowerCase();
        int index = url.indexOf('_');
        return index == -1 ? url : url.substring(index + 1);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Set<CorrelationRuleClassTO> getSyncCorrelationRuleClasses(final PolicyType type) {
        Set<CorrelationRuleClassTO> result = Collections.<CorrelationRuleClassTO>emptySet();

        switch (type) {
            case SYNC:
            case GLOBAL_SYNC:
                result = CollectionWrapper.wrapSyncCorrelationRuleClasses(
                        handlePossiblyEmptyStringCollection(baseUrl + "policy/syncCorrelationRuleClasses.json"));
                break;

            default:
                throw new NotFoundException();
        }

        return result;
    }
}
