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

import java.util.List;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.rest.api.service.OIDCC4UIProviderService;

public class OIDCProviderRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4006712447589576324L;

    public List<OIDCC4UIProviderTO> list() {
        return getService(OIDCC4UIProviderService.class).list();
    }

    public void create(final OIDCC4UIProviderTO op) {
        getService(OIDCC4UIProviderService.class).create(op);
    }

    public void createFromDiscovery(final OIDCC4UIProviderTO op) {
        getService(OIDCC4UIProviderService.class).createFromDiscovery(op);
    }

    public OIDCC4UIProviderTO read(final String key) {
        return getService(OIDCC4UIProviderService.class).read(key);
    }

    public void update(final OIDCC4UIProviderTO op) {
        getService(OIDCC4UIProviderService.class).update(op);
    }

    public void delete(final String key) {
        getService(OIDCC4UIProviderService.class).delete(key);
    }
}
