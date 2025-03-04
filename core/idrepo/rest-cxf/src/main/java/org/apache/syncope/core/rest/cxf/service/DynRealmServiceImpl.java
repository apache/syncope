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
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.core.logic.DynRealmLogic;

public class DynRealmServiceImpl extends AbstractService implements DynRealmService {

    protected final DynRealmLogic logic;

    public DynRealmServiceImpl(final DynRealmLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<DynRealmTO> list() {
        return logic.list();
    }

    @Override
    public DynRealmTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response create(final DynRealmTO roleTO) {
        DynRealmTO created = logic.create(roleTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public void update(final DynRealmTO roleTO) {
        logic.update(roleTO);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

}
