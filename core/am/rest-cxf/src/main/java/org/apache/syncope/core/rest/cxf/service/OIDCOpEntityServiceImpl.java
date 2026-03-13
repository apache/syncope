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
import org.apache.syncope.common.lib.to.OIDCOpEntityTO;
import org.apache.syncope.common.rest.api.service.OIDCOpEntityService;
import org.apache.syncope.core.logic.OIDCOpEntityLogic;

public class OIDCOpEntityServiceImpl extends AbstractService implements OIDCOpEntityService {

    protected final OIDCOpEntityLogic logic;

    public OIDCOpEntityServiceImpl(final OIDCOpEntityLogic logic) {
        this.logic = logic;
    }

    @Override
    public OIDCOpEntityTO get() {
        return logic.get();
    }

    @Override
    public void set(final OIDCOpEntityTO oidcOpEntityTO) {
        logic.set(oidcOpEntityTO);
    }

    @Override
    public Response generate(final String jwksKeyId, final String jwksType, final int jwksKeySize) {
        OIDCOpEntityTO jwks = logic.generate(jwksKeyId, jwksType, jwksKeySize);
        URI location = uriInfo.getAbsolutePathBuilder().build();
        return Response.created(location).entity(jwks).build();
    }

    @Override
    public void delete() {
        logic.delete();
    }
}
