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
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AuthProfileService;
import org.apache.syncope.core.logic.AuthProfileLogic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public class AuthProfileServiceImpl extends AbstractService implements AuthProfileService {

    protected final AuthProfileLogic logic;

    public AuthProfileServiceImpl(final AuthProfileLogic logic) {
        this.logic = logic;
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public AuthProfileTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response create(final AuthProfileTO authProfileTO) {
        AuthProfileTO created = logic.create(authProfileTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public void update(final AuthProfileTO authProfileTO) {
        logic.update(authProfileTO);
    }

    @Override
    public PagedResult<AuthProfileTO> list(final int page, final int size) {
        Page<AuthProfileTO> result = logic.list(PageRequest.of(page < 1 ? 0 : page - 1, size < 1 ? 1 : size));
        return buildPagedResult(result);
    }
}
