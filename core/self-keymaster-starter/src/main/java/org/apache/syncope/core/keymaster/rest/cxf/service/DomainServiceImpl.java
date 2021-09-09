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
package org.apache.syncope.core.keymaster.rest.cxf.service;

import java.net.URI;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.rest.api.service.DomainService;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.logic.DomainLogic;
import org.springframework.stereotype.Service;

@Service
public class DomainServiceImpl implements DomainService {

    private static final long serialVersionUID = -375255764389240615L;

    @Context
    protected UriInfo uriInfo;

    protected final DomainLogic logic;

    public DomainServiceImpl(final DomainLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<Domain> list() {
        return logic.list();
    }

    @Override
    public Domain read(final String key) {
        return logic.read(key);
    }

    @Override
    public Response create(final Domain domain) {
        Domain created = logic.create(domain);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public Response changeAdminPassword(
            final String key, final String password, final CipherAlgorithm cipherAlgorithm) {

        logic.changeAdminPassword(key, password, cipherAlgorithm);
        return Response.noContent().build();
    }

    @Override
    public Response adjustPoolSize(final String key, final int poolMaxActive, final int poolMinIdle) {
        logic.adjustPoolSize(key, poolMaxActive, poolMinIdle);
        return Response.noContent().build();
    }

    @Override
    public Response delete(final String key) {
        logic.delete(key);
        return Response.noContent().build();
    }
}
