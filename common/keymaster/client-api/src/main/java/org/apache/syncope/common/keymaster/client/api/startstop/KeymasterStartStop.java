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
package org.apache.syncope.common.keymaster.client.api.startstop;

import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

abstract class KeymasterStartStop {

    @Autowired
    protected ServiceOps serviceOps;

    protected final NetworkService.Type networkServiceType;

    protected KeymasterStartStop(final NetworkService.Type networkServiceType) {
        this.networkServiceType = networkServiceType;
    }

    @Value("${service.discovery.address}")
    private String address;

    protected NetworkService getNetworkService() {
        NetworkService ns = new NetworkService();
        ns.setType(networkServiceType);
        ns.setAddress(address);
        return ns;
    }
}
