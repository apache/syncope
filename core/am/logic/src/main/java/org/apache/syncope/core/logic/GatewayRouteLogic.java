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
package org.apache.syncope.core.logic;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.core.persistence.api.dao.GatewayRouteDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.GatewayRoute;
import org.apache.syncope.core.provisioning.api.data.GatewayRouteDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class GatewayRouteLogic extends AbstractTransactionalLogic<GatewayRouteTO> {

    @Autowired
    private GatewayRouteDAO routeDAO;

    @Autowired
    private GatewayRouteDataBinder binder;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private ServiceOps serviceOps;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Resource(name = "anonymousKey")
    private String anonymousKey;

    @PreAuthorize("isAuthenticated()")
    public List<GatewayRouteTO> list() {
        return routeDAO.findAll().stream().map(binder::getGatewayRouteTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GATEWAY_ROUTE_CREATE + "')")
    public GatewayRouteTO create(final GatewayRouteTO routeTO) {
        GatewayRoute route = entityFactory.newEntity(GatewayRoute.class);
        binder.getGatewayRoute(route, routeTO);

        return binder.getGatewayRouteTO(routeDAO.save(route));
    }

    @PreAuthorize("isAuthenticated()")
    public GatewayRouteTO read(final String key) {
        GatewayRoute route = routeDAO.find(key);
        if (route == null) {
            throw new NotFoundException("GatewayRoute " + key);
        }
        return binder.getGatewayRouteTO(route);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GATEWAY_ROUTE_UPDATE + "')")
    public GatewayRouteTO update(final GatewayRouteTO routeTO) {
        GatewayRoute route = routeDAO.find(routeTO.getKey());
        if (route == null) {
            throw new NotFoundException("GatewayRoute " + routeTO.getKey());
        }

        binder.getGatewayRoute(route, routeTO);

        return binder.getGatewayRouteTO(routeDAO.save(route));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GATEWAY_ROUTE_DELETE + "')")
    public GatewayRouteTO delete(final String key) {
        GatewayRoute route = routeDAO.find(key);
        if (route == null) {
            throw new NotFoundException("GatewayRoute " + key);
        }

        GatewayRouteTO deleted = binder.getGatewayRouteTO(route);
        routeDAO.delete(route);
        return deleted;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.GATEWAY_ROUTE_PUSH + "')")
    public void pushToSRA() {
        try {
            NetworkService sra = serviceOps.get(NetworkService.Type.SRA);
            HttpClient.newBuilder().build().send(
                    HttpRequest.newBuilder(URI.create(
                            StringUtils.appendIfMissing(sra.getAddress(), "/") + "management/routes/refresh")).
                            header(HttpHeaders.AUTHORIZATION,
                                    DefaultBasicAuthSupplier.getBasicAuthHeader(anonymousUser, anonymousKey)).
                            POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (KeymasterException e) {
            throw new NotFoundException("Could not find any SRA instance", e);
        } catch (IOException | InterruptedException e) {
            throw new InternalServerErrorException("Errors while communicating with SRA instance", e);
        }
    }

    @Override
    protected GatewayRouteTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args) && ("create".equals(method.getName())
                || "update".equals(method.getName())
                || "delete".equals(method.getName()))) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof GatewayRouteTO) {
                    key = ((GatewayRouteTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getGatewayRouteTO(routeDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
