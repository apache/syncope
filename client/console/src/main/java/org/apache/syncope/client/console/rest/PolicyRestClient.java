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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.service.PolicyService;

/**
 * Console client for invoking Rest Policy services.
 */
public class PolicyRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    public <T extends AbstractPolicyTO> T getPolicy(final String key) {
        T policy = null;
        try {
            policy = getService(PolicyService.class).read(key);
        } catch (Exception e) {
            LOG.warn("No policy found for id {}", key, e);
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicyTO> List<T> getPolicies(final PolicyType type) {
        final List<T> res = new ArrayList<>();

        try {
            res.addAll((List<T>) getService(PolicyService.class).list(type));
            Collections.sort(res, new PolicyComparator());
        } catch (Exception ignore) {
            LOG.debug("No policy found", ignore);
        }

        return res;
    }

    public <T extends AbstractPolicyTO> void createPolicy(final T policy) {
        getService(PolicyService.class).create(policy);
    }

    public <T extends AbstractPolicyTO> void updatePolicy(final T policy) {
        getService(PolicyService.class).update(policy);
    }

    public void delete(final String key) {
        getService(PolicyService.class).delete(key);
    }

    private class PolicyComparator implements Comparator<AbstractPolicyTO>, Serializable {

        private static final long serialVersionUID = -4921433085213223115L;

        @Override
        public int compare(final AbstractPolicyTO left, final AbstractPolicyTO right) {
            return left == null ? -1 : right == null ? 1 : left.getDescription().compareTo(right.getDescription());
        }

    }
}
