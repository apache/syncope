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

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.core.logic.RootLogic;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.ext.scimv2.api.data.ResourceType;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.ServiceProviderConfig;
import org.apache.syncope.ext.scimv2.api.service.RootService;

public class RootServiceImpl extends AbstractSCIMService<SCIMResource> implements RootService {

    private RootLogic rootLogic;

    protected RootLogic rootLogic() {
        synchronized (this) {
            if (rootLogic == null) {
                rootLogic = ApplicationContextProvider.getApplicationContext().getBean(RootLogic.class);
            }
        }
        return rootLogic;
    }

    @Override
    public ServiceProviderConfig serviceProviderConfig() {
        return rootLogic().serviceProviderConfig();
    }

    @Override
    public List<ResourceType> resourceTypes() {
        return rootLogic().resourceTypes(uriInfo.getAbsolutePathBuilder());
    }

    @Override
    public ResourceType resourceType(final String type) {
        return rootLogic().resourceType(uriInfo.getAbsolutePathBuilder(), type);
    }

    @Override
    public Response schemas() {
        return Response.ok(rootLogic().schemas()).build();
    }

    @Override
    public Response schema(final String schema) {
        return Response.ok(rootLogic().schema(schema)).build();
    }

}
