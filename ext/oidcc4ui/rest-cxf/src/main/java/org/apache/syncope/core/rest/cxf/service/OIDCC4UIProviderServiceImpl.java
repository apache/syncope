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
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.OIDCC4UIProviderService;
import org.apache.syncope.core.logic.OIDCC4UIProviderLogic;

public class OIDCC4UIProviderServiceImpl extends AbstractService implements OIDCC4UIProviderService {

    protected final OIDCC4UIProviderLogic logic;

    public OIDCC4UIProviderServiceImpl(final OIDCC4UIProviderLogic logic) {
        this.logic = logic;
    }

    @Override
    public Response create(final OIDCC4UIProviderTO oidcProviderTO) {
        String created = logic.create(oidcProviderTO);

        URI location = uriInfo.getAbsolutePathBuilder().path(created).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created).
                build();
    }

    @Override
    public Response createFromDiscovery(final OIDCC4UIProviderTO oidcProviderTO) {
        String created = logic.createFromDiscovery(oidcProviderTO);

        URI location = uriInfo.getAbsolutePathBuilder().path(created).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created).
                build();
    }

    @Override
    public List<OIDCC4UIProviderTO> list() {
        return logic.list();

    }

    @Override
    public OIDCC4UIProviderTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public void update(final OIDCC4UIProviderTO oidcProviderTO) {
        logic.update(oidcProviderTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }
}
