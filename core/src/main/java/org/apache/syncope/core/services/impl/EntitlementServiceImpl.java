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
package org.apache.syncope.core.services.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.to.EntitlementTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.AuthenticationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntitlementServiceImpl implements EntitlementService {

    @Autowired
    private AuthenticationController authenticationController;

    @Override
    public Set<EntitlementTO> getAllEntitlements() {
        Set<String> entitlements = new HashSet<String>(authenticationController.listEntitlements());
        return CollectionWrapper.wrap(entitlements);
    }

    @Override
    public Set<EntitlementTO> getMyEntitlements() {
        Set<String> entitlements = authenticationController.getEntitlements();
        return CollectionWrapper.wrap(entitlements);
    }
}
