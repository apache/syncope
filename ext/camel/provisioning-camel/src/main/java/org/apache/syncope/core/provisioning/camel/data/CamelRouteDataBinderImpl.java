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
package org.apache.syncope.core.provisioning.camel.data;

import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.core.persistence.api.dao.CamelRouteDAO;
import org.apache.syncope.core.persistence.api.entity.CamelRoute;
import org.apache.syncope.core.provisioning.api.data.CamelRouteDataBinder;

public class CamelRouteDataBinderImpl implements CamelRouteDataBinder {

    protected final CamelRouteDAO routeDAO;

    public CamelRouteDataBinderImpl(final CamelRouteDAO routeDAO) {
        this.routeDAO = routeDAO;
    }

    @Override
    public CamelRouteTO getRouteTO(final CamelRoute route) {
        CamelRouteTO routeTO = new CamelRouteTO();
        routeTO.setKey(route.getKey());
        routeTO.setAnyTypeKind(route.getAnyTypeKind());
        routeTO.setContent(route.getContent());

        return routeTO;
    }

    @Override
    public void update(final CamelRoute route, final CamelRouteTO routeTO) {
        route.setContent(routeTO.getContent());
        routeDAO.save(route);
    }
}
