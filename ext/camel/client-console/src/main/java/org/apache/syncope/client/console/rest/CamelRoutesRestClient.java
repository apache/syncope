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

import static org.apache.syncope.client.console.rest.BaseRestClient.getService;

import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.to.CamelMetrics;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.service.CamelRouteService;

public class CamelRoutesRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2018208424159468912L;

    public List<CamelRouteTO> list(final AnyTypeKind anyTypeKind) {
        return isCamelEnabledFor(anyTypeKind)
                ? getService(CamelRouteService.class).list(anyTypeKind)
                : Collections.<CamelRouteTO>emptyList();
    }

    public CamelRouteTO read(final String key) {
        return getService(CamelRouteService.class).read(key);
    }

    public void update(final CamelRouteTO routeTO) {
        getService(CamelRouteService.class).update(routeTO);
    }

    public boolean isCamelEnabledFor(final AnyTypeKind anyTypeKind) {
        return anyTypeKind == AnyTypeKind.USER
                ? SyncopeConsoleSession.get().getPlatformInfo().getUserProvisioningManager().contains("Camel")
                : anyTypeKind == AnyTypeKind.ANY_OBJECT
                        ? SyncopeConsoleSession.get().getPlatformInfo().
                                getAnyObjectProvisioningManager().contains("Camel")
                        : SyncopeConsoleSession.get().getPlatformInfo().
                                getGroupProvisioningManager().contains("Camel");

    }

    public CamelMetrics metrics() {
        return getService(CamelRouteService.class).metrics();
    }
}
