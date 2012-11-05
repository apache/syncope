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

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Resources services.
 */
@Component
public class EntitlementRestClient extends AbstractBaseRestClient {

    /**
     * Get all Entitlements.
     *
     * @return List<String>
     */
    public List<String> getAllEntitlements() {
        List<String> entitlements = null;

        try {
            entitlements = Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "auth/allentitlements.json", String[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading all the entitlements", e);
        }

        return entitlements;
    }
}
