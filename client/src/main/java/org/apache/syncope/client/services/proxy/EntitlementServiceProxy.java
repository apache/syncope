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
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.to.EntitlementTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.web.client.RestTemplate;

public class EntitlementServiceProxy extends SpringServiceProxy implements EntitlementService {

    public EntitlementServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public Set<EntitlementTO> getAllEntitlements() {
        Set<String> entitlements = new HashSet<String>(Arrays.asList(new RestTemplate().getForObject(
                baseUrl + "auth/allentitlements.json", String[].class)));
        return CollectionWrapper.wrap(entitlements);
    }

    @Override
    public Set<EntitlementTO> getMyEntitlements() {
        Set<String> entitlements = new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "auth/entitlements.json", String[].class)));
        return CollectionWrapper.wrap(entitlements);
    }
}
