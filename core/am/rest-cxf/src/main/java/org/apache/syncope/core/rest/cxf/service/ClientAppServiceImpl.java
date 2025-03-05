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
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ClientAppService;
import org.apache.syncope.core.logic.ClientAppLogic;

public class ClientAppServiceImpl extends AbstractService implements ClientAppService {

    protected final ClientAppLogic logic;

    public ClientAppServiceImpl(final ClientAppLogic logic) {
        this.logic = logic;
    }

    @Override
    public Response create(final ClientAppType type, final ClientAppTO clientAppTO) {
        ClientAppTO created = logic.create(type, clientAppTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public <T extends ClientAppTO> List<T> list(final ClientAppType type) {
        return logic.list(type);
    }

    @Override
    public <T extends ClientAppTO> T read(final ClientAppType type, final String key) {
        return logic.read(type, key);
    }

    @Override
    public void update(final ClientAppType type, final ClientAppTO clientAppTO) {
        logic.update(type, clientAppTO);
    }

    @Override
    public void delete(final ClientAppType type, final String key) {
        logic.delete(type, key);
    }
}
