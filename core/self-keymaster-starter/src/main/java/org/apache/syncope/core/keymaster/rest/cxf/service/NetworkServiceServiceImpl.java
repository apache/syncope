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
package org.apache.syncope.core.keymaster.rest.cxf.service;

import java.util.List;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.rest.api.service.NetworkServiceService;
import org.apache.syncope.core.logic.NetworkServiceLogic;

public class NetworkServiceServiceImpl implements NetworkServiceService {

    private static final long serialVersionUID = 4160287655489345100L;

    protected final NetworkServiceLogic logic;

    public NetworkServiceServiceImpl(final NetworkServiceLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        return logic.list(serviceType);
    }

    @Override
    public NetworkService get(final NetworkService.Type serviceType) {
        return logic.get(serviceType);
    }

    @Override
    public void action(final NetworkService networkService, final Action action) {
        if (action == Action.unregister) {
            logic.unregister(networkService);
        } else {
            logic.register(networkService);
        }
    }
}
