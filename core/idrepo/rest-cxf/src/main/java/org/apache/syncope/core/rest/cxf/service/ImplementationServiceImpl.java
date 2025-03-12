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
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ImplementationService;
import org.apache.syncope.core.logic.ImplementationLogic;

public class ImplementationServiceImpl extends AbstractService implements ImplementationService {

    protected final ImplementationLogic logic;

    public ImplementationServiceImpl(final ImplementationLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<ImplementationTO> list(final String type) {
        return logic.list(type);
    }

    @Override
    public ImplementationTO read(final String type, final String key) {
        return logic.read(type, key);
    }

    @Override
    public Response create(final ImplementationTO implementationTO) {
        ImplementationTO created = logic.create(implementationTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.getKey())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public Response update(final ImplementationTO implementationTO) {
        logic.update(implementationTO);
        return Response.noContent().build();
    }

    @Override
    public Response delete(final String type, final String key) {
        logic.delete(type, key);
        return Response.noContent().build();
    }
}
