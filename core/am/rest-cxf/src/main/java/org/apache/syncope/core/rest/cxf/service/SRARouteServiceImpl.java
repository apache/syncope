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
package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.apache.syncope.core.logic.SRARouteLogic;

public class SRARouteServiceImpl extends AbstractService implements SRARouteService {

    protected final SRARouteLogic logic;

    public SRARouteServiceImpl(final SRARouteLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<SRARouteTO> list() {
        return logic.list();
    }

    @Override
    public Response create(final SRARouteTO routeTO) {
        SRARouteTO createdRoute = logic.create(routeTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(createdRoute.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, createdRoute.getKey()).
                build();
    }

    @Override
    public SRARouteTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public void update(final SRARouteTO routeTO) {
        logic.update(routeTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public void pushToSRA() {
        logic.pushToSRA();
    }
}
