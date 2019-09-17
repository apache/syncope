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
import org.apache.syncope.common.lib.to.CamelMetrics;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.service.CamelRouteService;

public class CamelRoutesRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2018208424159468912L;

    public static List<CamelRouteTO> list(final AnyTypeKind anyTypeKind) {
        return isCamelEnabledFor(anyTypeKind)
                ? getService(CamelRouteService.class).list(anyTypeKind)
                : List.of();
    }

    public static CamelRouteTO read(final AnyTypeKind anyTypeKind, final String key) {
        return getService(CamelRouteService.class).read(anyTypeKind, key);
    }

    public static void update(final AnyTypeKind anyTypeKind, final CamelRouteTO routeTO) {
        getService(CamelRouteService.class).update(anyTypeKind, routeTO);
    }

    public static boolean isCamelEnabledFor(final AnyTypeKind anyTypeKind) {
        return anyTypeKind == AnyTypeKind.USER
                ? SyncopeConsoleSession.get().getPlatformInfo().
                        getProvisioningInfo().getUserProvisioningManager().contains("Camel")
                : anyTypeKind == AnyTypeKind.ANY_OBJECT
                        ? SyncopeConsoleSession.get().getPlatformInfo().getProvisioningInfo().
                                getAnyObjectProvisioningManager().contains("Camel")
                        : SyncopeConsoleSession.get().getPlatformInfo().getProvisioningInfo().
                                getGroupProvisioningManager().contains("Camel");

    }

    public static CamelMetrics metrics() {
        return getService(CamelRouteService.class).metrics();
    }
}
