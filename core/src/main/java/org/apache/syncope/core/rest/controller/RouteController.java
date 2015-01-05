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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.common.to.RouteTO;
import org.apache.syncope.core.persistence.beans.CamelRoute;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.RouteDAO;
import org.apache.syncope.core.provisioning.camel.SyncopeCamelContext;
import org.apache.syncope.core.rest.data.RouteDataBinder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RouteController extends AbstractTransactionalController<RouteTO> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RouteDataBinder.class);

    @Autowired
    private RouteDAO routeDao;

    @Autowired
    private RouteDataBinder binder;

    @Autowired
    private SyncopeCamelContext context;

    @PreAuthorize("hasRole('ROUTE_LIST')")
    @Transactional(readOnly = true)
    public List<RouteTO> listRoutes() {
        List<RouteTO> routes = new ArrayList<RouteTO>();
        Iterator it = routeDao.findAll().iterator();
        while (it.hasNext()) {
            routes.add(binder.getRouteTO((CamelRoute) it.next()));
        }
        return routes;
    }

    @PreAuthorize("hasRole('ROUTE_READ')")
    @Transactional(readOnly = true)
    public RouteTO readRoute(Long id) {
        CamelRoute route = routeDao.find(id);
        if (route == null) {
            throw new NotFoundException("Route with id=" + id);
        }

        return binder.getRouteTO(route);
    }

    @PreAuthorize("hasRole('ROUTE_UPDATE')")
    public void updateRoute(RouteTO routeTO) {

        CamelRoute route = routeDao.find(routeTO.getId());
        if (route == null) {
            throw new NotFoundException("Route with id=" + route.getId());
        }
        route.setRouteContent(routeTO.getRouteContent());
        routeDao.save(route);
        LOG.info("UPDATING ROUTE WITH ID {} ", routeTO.getId());
        LOG.info("NEW ROUTE CONTENT {} ", routeTO.getRouteContent());
        context.reloadContext(routeDao, routeTO.getId());
    }

    @Override
    protected RouteTO resolveReference(Method method, Object... args) throws UnresolvedReferenceException {
        throw new UnresolvedReferenceException();
    }

}
