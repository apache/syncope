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

import java.net.URI;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.RegisteredClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.RegisteredClientAppService;
import org.apache.syncope.core.logic.RegisteredClientAppLogic;

@Service
public class RegisteredClientAppServiceImpl extends AbstractServiceImpl implements RegisteredClientAppService {

    @Autowired
    private RegisteredClientAppLogic logic;

    @Override
    public List<RegisteredClientAppTO> list() {
        return logic.list();
    }

    @Override
    public RegisteredClientAppTO read(final Long clientAppId) {
        RegisteredClientAppTO registeredClientAppTO = logic.read(clientAppId);
        if (registeredClientAppTO == null) {
            throw new NotFoundException("Client app with clientApp ID " + clientAppId + " not found");
        }
        return registeredClientAppTO;
    }

    @Override
    public RegisteredClientAppTO read(final Long clientAppId, final ClientAppType type) {
        RegisteredClientAppTO registeredClientAppTO = logic.read(clientAppId, type);
        if (registeredClientAppTO == null) {
            throw new NotFoundException("Client app with clientApp ID " + clientAppId
                    + " with type " + type + " not found");
        }
        return registeredClientAppTO;
    }

    @Override
    public RegisteredClientAppTO read(final String name) {
        RegisteredClientAppTO registeredClientAppTO = logic.read(name);
        if (registeredClientAppTO == null) {
            throw new NotFoundException("Client app with name " + name + " not found");
        }
        return registeredClientAppTO;
    }

    @Override
    public RegisteredClientAppTO read(final String name, final ClientAppType type) {
        RegisteredClientAppTO registeredClientAppTO = logic.read(name, type);
        if (registeredClientAppTO == null) {
            throw new NotFoundException("Client app with name " + name + " with type " + type + " not found");
        }
        return registeredClientAppTO;
    }

    @Override
    public Response create(final RegisteredClientAppTO registeredClientAppTO) {
        RegisteredClientAppTO appTO = logic.create(registeredClientAppTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(appTO.getClientAppTO().getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, appTO.getClientAppTO().getKey()).
                build();
    }

    @Override
    public boolean delete(final String name) {
        return logic.delete(name);
    }

}
