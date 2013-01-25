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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationActionClassTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.core.rest.controller.ResourceController;
import org.apache.syncope.core.util.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl implements ResourceService, ContextAware {

    @Autowired
    private ResourceController resourceController;

    private UriInfo uriInfo;

    @Override
    public Response create(final ResourceTO resourceTO) {
        try {
            ResourceTO resource = resourceController.create(new DummyHTTPServletResponse(), resourceTO);
            URI location = uriInfo.getAbsolutePathBuilder().path(resource.getName()).build();
            return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, resource.getName()).build();
        } catch (SyncopeClientCompositeErrorException e) {
            throw new BadRequestException(e);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void update(final String resourceName, final ResourceTO resourceTO) {
        try {
            resourceController.update(resourceTO);
        } catch (SyncopeClientCompositeErrorException e) {
            throw new BadRequestException(e);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void delete(final String resourceName) {
        try {
            resourceController.delete(resourceName);
        } catch (SyncopeClientCompositeErrorException e) {
            throw new BadRequestException(e);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public ResourceTO read(final String resourceName) {
        try {
            return resourceController.read(resourceName);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public Set<PropagationActionClassTO> getPropagationActionsClasses() {
        @SuppressWarnings("unchecked")
        Set<String> classes = (Set<String>) resourceController.getPropagationActionsClasses().getModel().values()
                .iterator().next();
        return CollectionWrapper.wrapPropagationActionClasses(classes);
    }

    @Override
    public List<ResourceTO> list() {
        try {
            return resourceController.list(null);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public List<ResourceTO> list(final Long connInstanceId) {
        try {
            return resourceController.list(connInstanceId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public ConnObjectTO getConnector(final String resourceName, final AttributableType type, final String objectId) {
        try {
            return resourceController.getObject(resourceName, type, objectId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public boolean check(final ResourceTO resourceTO) {
        try {
            return (Boolean) resourceController.check(new DummyHTTPServletResponse(), resourceTO).getModel().values()
                    .iterator().next();
        } catch (SyncopeClientCompositeErrorException e) {
            throw new BadRequestException(e);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void setUriInfo(UriInfo ui) {
        this.uriInfo = ui;
    }

}
