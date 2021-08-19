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

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.entity.SRARoute;
import org.apache.syncope.core.provisioning.api.data.SRARouteDataBinder;

public class SRARouteDataBinderImpl implements SRARouteDataBinder {

    @Override
    public void getSRARoute(final SRARoute route, final SRARouteTO routeTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
        if (StringUtils.isBlank(routeTO.getName())) {
            sce.getElements().add("name");
        }
        if (routeTO.getTarget() == null) {
            sce.getElements().add("target");
        }
        if (!sce.isEmpty()) {
            throw sce;
        }

        route.setName(routeTO.getName());
        route.setTarget(routeTO.getTarget());
        route.setError(routeTO.getError());
        route.setType(routeTO.getType());
        route.setLogout(routeTO.isLogout());
        route.setPostLogout(routeTO.getPostLogout());
        route.setCsrf(routeTO.isCsrf());
        route.setOrder(routeTO.getOrder());
        route.setFilters(routeTO.getFilters());
        route.setPredicates(routeTO.getPredicates());
    }

    @Override
    public SRARouteTO getSRARouteTO(final SRARoute route) {
        SRARouteTO routeTO = new SRARouteTO();
        routeTO.setKey(route.getKey());
        routeTO.setName(route.getName());
        routeTO.setTarget(route.getTarget());
        routeTO.setError(route.getError());
        routeTO.setType(route.getType());
        routeTO.setLogout(route.isLogout());
        routeTO.setPostLogout(route.getPostLogout());
        routeTO.setCsrf(route.isCsrf());
        routeTO.setOrder(route.getOrder());
        routeTO.getFilters().addAll(route.getFilters());
        routeTO.getPredicates().addAll(route.getPredicates());

        return routeTO;
    }
}
