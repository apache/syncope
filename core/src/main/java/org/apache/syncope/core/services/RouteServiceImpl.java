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
package org.apache.syncope.core.services;

import static org.apache.syncope.core.services.AbstractServiceImpl.OPTIONS_ALLOW;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.services.RouteService;
import org.apache.syncope.common.to.RouteTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.core.provisioning.camel.CamelDetector;
import org.apache.syncope.core.rest.controller.RouteController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RouteServiceImpl extends AbstractServiceImpl implements RouteService {
    
    @Override
    public Response getOptions(final SubjectType subject) {
        String key;
        String value;
        if (subject == SubjectType.USER) {
            key = RESTHeaders.CAMEL_USER_PROVISIONING_MANAGER;
            value = Boolean.toString(CamelDetector.isCamelEnabledForUsers());
        } else {
            key = RESTHeaders.CAMEL_ROLE_PROVISIONING_MANAGER;
            value = Boolean.toString(CamelDetector.isCamelEnabledForRoles());
        }
        Response.ResponseBuilder builder = Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW);
        if (key != null && value != null) {
            builder.header(key, value);
        }
        return builder.build();
    }

    @Autowired
    private RouteController controller;

    @Override
    public List<RouteTO> getRoutes(SubjectType subject) {

        return controller.listRoutes(subject);
    }

    @Override
    public RouteTO getRoute(Long id) {

        return controller.readRoute(id);
    }
    
    @Override
    public RouteTO getRoute(SubjectType subject, Long Id) {
        return controller.readRoute(Id, subject);
    }

    @Override
    public void importRoute(Long id, RouteTO route) {
        controller.updateRoute(route);
    }

    @Override
    public void importRoute(SubjectType kind, Long id, RouteTO route) {
        controller.updateRoute(route);
    }

}
