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
package org.apache.syncope.client.console.rest;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.service.PolicyService;

/**
 * Console client for invoking Rest Policy services.
 */
public class PolicyRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    private static final PolicyComparator COMPARATOR = new PolicyComparator();

    public static <T extends PolicyTO> T getPolicy(final PolicyType type, final String key) {
        T policy = null;
        try {
            policy = getService(PolicyService.class).read(type, key);
        } catch (Exception e) {
            LOG.warn("No policy found for id {}", key, e);
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PolicyTO> List<T> getPolicies(final PolicyType type) {
        try {
            return getService(PolicyService.class).<T>list(type).stream().
                    sorted(COMPARATOR).
                    collect(Collectors.toList());
        } catch (Exception ignore) {
            LOG.debug("No policy found", ignore);
            return List.of();
        }
    }

    public static <T extends PolicyTO> void createPolicy(final PolicyType type, final T policy) {
        getService(PolicyService.class).create(type, policy);
    }

    public static <T extends PolicyTO> void updatePolicy(final PolicyType type, final T policy) {
        getService(PolicyService.class).update(type, policy);
    }

    public static void delete(final PolicyType type, final String key) {
        getService(PolicyService.class).delete(type, key);
    }

    private static class PolicyComparator implements Comparator<PolicyTO>, Serializable {

        private static final long serialVersionUID = -4921433085213223115L;

        @Override
        public int compare(final PolicyTO left, final PolicyTO right) {
            return Optional.ofNullable(left).map(to -> Optional.ofNullable(right)
                .map(policyTO -> to.getDescription().compareTo(policyTO.getDescription())).orElse(1)).orElse(-1);
        }

    }
}
