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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.to.OIDCProviderTO;
import org.apache.syncope.common.rest.api.service.OIDCProviderService;

public class OIDCProviderRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4006712447589576324L;

    public static List<OIDCProviderTO> list() {
        return getService(OIDCProviderService.class).list();
    }

    public static void create(final OIDCProviderTO op) {
        SyncopeConsoleSession.get().getService(OIDCProviderService.class).create(op);
    }

    public static void createFromDiscovery(final OIDCProviderTO op) {
        SyncopeConsoleSession.get().getService(OIDCProviderService.class).createFromDiscovery(op);
    }

    public static OIDCProviderTO read(final String key) {
        return getService(OIDCProviderService.class).read(key);
    }

    public static void update(final OIDCProviderTO op) {
        getService(OIDCProviderService.class).update(op);
    }

    public static void delete(final String key) {
        getService(OIDCProviderService.class).delete(key);
    }
}
