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

import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AuthProfileService;
import org.apache.syncope.core.logic.AuthProfileLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Service
public class AuthProfileServiceImpl extends AbstractServiceImpl implements AuthProfileService {

    @Autowired
    private AuthProfileLogic logic;

    @Override
    public Response deleteByKey(final String key) {
        logic.deleteByKey(key);
        return Response.noContent().build();
    }

    @Override
    public Response deleteByOwner(final String owner) {
        logic.deleteByOwner(owner);
        return Response.noContent().build();
    }

    @Override
    public AuthProfileTO findByOwner(final String owner) {
        return logic.findByOwner(owner);
    }

    @Override
    public AuthProfileTO findByKey(final String key) {
        return logic.findByKey(key);
    }

    @Override
    public List<AuthProfileTO> list() {
        return logic.list();
    }

    @Override
    public Response create(final AuthProfileTO authProfileTO) {
        AuthProfileTO profileTO = logic.create(authProfileTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(profileTO.getKey()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, profileTO.getKey()).
            build();
    }
}
