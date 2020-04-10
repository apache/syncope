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
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.core.logic.AuthModuleLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthModuleServiceImpl extends AbstractServiceImpl implements AuthModuleService {

    @Autowired
    private AuthModuleLogic logic;

    @Override
    public Response create(final AuthModuleTO authModuleTO) {
        AuthModuleTO authModule = logic.create(authModuleTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(authModule.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, authModule.getKey()).
                build();
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public List<AuthModuleTO> list() {
        return logic.list();
    }

    @Override
    public AuthModuleTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public void update(final AuthModuleTO authModuleTO) {
        logic.update(authModuleTO);
    }
}
