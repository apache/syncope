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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.core.persistence.api.entity.GatewayRoute;
import org.apache.syncope.core.provisioning.api.data.GatewayRouteDataBinder;
import org.springframework.stereotype.Component;

@Component
public class GatewayRouteDataBinderImpl implements GatewayRouteDataBinder {

    @Override
    public void getGatewayRoute(final GatewayRoute route, final GatewayRouteTO routeTO) {
        route.setName(routeTO.getName());
        route.setTarget(routeTO.getTarget());
        route.setFilters(routeTO.getFilters());
        route.setPredicates(routeTO.getPredicates());
        route.setStatus(routeTO.getStatus());
    }

    @Override
    public GatewayRouteTO getGatewayRouteTO(final GatewayRoute route) {
        GatewayRouteTO routeTO = new GatewayRouteTO();
        routeTO.setKey(route.getKey());
        routeTO.setName(route.getName());
        routeTO.setTarget(route.getTarget());
        routeTO.getFilters().addAll(route.getFilters());
        routeTO.getPredicates().addAll(route.getPredicates());
        routeTO.setStatus(route.getStatus());

        return routeTO;
    }
}
