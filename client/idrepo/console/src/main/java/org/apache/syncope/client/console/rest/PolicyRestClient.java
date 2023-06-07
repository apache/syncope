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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.service.PolicyService;

/**
 * Console client for invoking Rest Policy services.
 */
public class PolicyRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    protected static final Comparator<PolicyTO> COMPARATOR = Comparator.comparing(PolicyTO::getName);

    public <T extends PolicyTO> T read(final PolicyType type, final String key) {
        T policy = null;
        try {
            policy = getService(PolicyService.class).read(type, key);
        } catch (Exception e) {
            LOG.warn("No policy found for type {} and key {}", type, key, e);
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    public <T extends PolicyTO> List<T> list(final PolicyType type) {
        try {
            return getService(PolicyService.class).<T>list(type).stream().
                    sorted(COMPARATOR).
                    collect(Collectors.toList());
        } catch (Exception ignore) {
            LOG.debug("No policy found", ignore);
            return List.of();
        }
    }

    public <T extends PolicyTO> void create(final PolicyType type, final T policy) {
        getService(PolicyService.class).create(type, policy);
    }

    public <T extends PolicyTO> void update(final PolicyType type, final T policy) {
        getService(PolicyService.class).update(type, policy);
    }

    public void delete(final PolicyType type, final String key) {
        getService(PolicyService.class).delete(type, key);
    }
}
