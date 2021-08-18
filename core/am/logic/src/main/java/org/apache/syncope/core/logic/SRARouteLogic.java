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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.core.persistence.api.dao.SRARouteDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.SRARoute;
import org.apache.syncope.core.provisioning.api.data.SRARouteDataBinder;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.security.access.prepost.PreAuthorize;

public class SRARouteLogic extends AbstractTransactionalLogic<SRARouteTO> {

    protected final SRARouteDAO routeDAO;

    protected final SRARouteDataBinder binder;

    protected final EntityFactory entityFactory;

    protected final ServiceOps serviceOps;

    protected final SecurityProperties securityProperties;

    public SRARouteLogic(
            final SRARouteDAO routeDAO,
            final SRARouteDataBinder binder,
            final EntityFactory entityFactory,
            final ServiceOps serviceOps,
            final SecurityProperties securityProperties) {

        this.routeDAO = routeDAO;
        this.binder = binder;
        this.entityFactory = entityFactory;
        this.serviceOps = serviceOps;
        this.securityProperties = securityProperties;
    }

    @PreAuthorize("isAuthenticated()")
    public List<SRARouteTO> list() {
        return routeDAO.findAll().stream().map(binder::getSRARouteTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SRA_ROUTE_CREATE + "')")
    public SRARouteTO create(final SRARouteTO routeTO) {
        SRARoute route = entityFactory.newEntity(SRARoute.class);
        binder.getSRARoute(route, routeTO);

        return binder.getSRARouteTO(routeDAO.save(route));
    }

    @PreAuthorize("isAuthenticated()")
    public SRARouteTO read(final String key) {
        SRARoute route = routeDAO.find(key);
        if (route == null) {
            throw new NotFoundException("SRARoute " + key);
        }
        return binder.getSRARouteTO(route);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SRA_ROUTE_UPDATE + "')")
    public SRARouteTO update(final SRARouteTO routeTO) {
        SRARoute route = routeDAO.find(routeTO.getKey());
        if (route == null) {
            throw new NotFoundException("SRARoute " + routeTO.getKey());
        }

        binder.getSRARoute(route, routeTO);

        return binder.getSRARouteTO(routeDAO.save(route));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SRA_ROUTE_DELETE + "')")
    public SRARouteTO delete(final String key) {
        SRARoute route = routeDAO.find(key);
        if (route == null) {
            throw new NotFoundException("SRARoute " + key);
        }

        SRARouteTO deleted = binder.getSRARouteTO(route);
        routeDAO.delete(route);
        return deleted;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.SRA_ROUTE_PUSH + "')")
    public void pushToSRA() {
        try {
            NetworkService sra = serviceOps.get(NetworkService.Type.SRA);
            HttpClient.newBuilder().build().send(
                    HttpRequest.newBuilder(URI.create(
                            StringUtils.appendIfMissing(sra.getAddress(), "/") + "actuator/gateway/refresh")).
                            header(HttpHeaders.AUTHORIZATION, DefaultBasicAuthSupplier.getBasicAuthHeader(
                                    securityProperties.getAnonymousUser(), securityProperties.getAnonymousKey())).
                            POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (KeymasterException e) {
            throw new NotFoundException("Could not find any SRA instance", e);
        } catch (IOException | InterruptedException e) {
            throw new InternalServerErrorException("Errors while communicating with SRA instance", e);
        }
    }

    @Override
    protected SRARouteTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args) && ("create".equals(method.getName())
                || "update".equals(method.getName())
                || "delete".equals(method.getName()))) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof SRARouteTO) {
                    key = ((SRARouteTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getSRARouteTO(routeDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
