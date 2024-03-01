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
package org.apache.syncope.ext.scimv2.cxf.service;

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.SCIMLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.ext.scimv2.api.data.ResourceType;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.ServiceProviderConfig;
import org.apache.syncope.ext.scimv2.api.service.SCIMService;

public class SCIMServiceImpl extends AbstractSCIMService<SCIMResource> implements SCIMService {

    protected final SCIMLogic scimLogic;

    public SCIMServiceImpl(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager,
            final SCIMLogic scimLogic) {

        super(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
        this.scimLogic = scimLogic;
    }

    @Override
    public ServiceProviderConfig serviceProviderConfig() {
        return scimLogic.serviceProviderConfig(uriInfo.getAbsolutePathBuilder());
    }

    @Override
    public List<ResourceType> resourceTypes() {
        return scimLogic.resourceTypes(uriInfo.getAbsolutePathBuilder());
    }

    @Override
    public ResourceType resourceType(final String type) {
        return scimLogic.resourceType(uriInfo.getAbsolutePathBuilder(), type);
    }

    @Override
    public Response schemas() {
        return Response.ok(scimLogic.schemas()).build();
    }

    @Override
    public Response schema(final String schema) {
        return Response.ok(scimLogic.schema(schema)).build();
    }
}
