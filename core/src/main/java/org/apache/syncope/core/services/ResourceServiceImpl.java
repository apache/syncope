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

import javax.ws.rs.core.Response;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.BulkAssociationAction;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationActionClassTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.ResourceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl extends AbstractServiceImpl implements ResourceService, ContextAware {

    @Autowired
    private ResourceController controller;

    @Override
    public Response create(final ResourceTO resourceTO) {
        ResourceTO resource = controller.create(resourceTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(resource.getName()).build();
        return Response.created(location)
                .header(SyncopeConstants.REST_HEADER_ID, resource.getName())
                .build();
    }

    @Override
    public void update(final String resourceName, final ResourceTO resourceTO) {
        controller.update(resourceTO);
    }

    @Override
    public void delete(final String resourceName) {
        controller.delete(resourceName);
    }

    @Override
    public ResourceTO read(final String resourceName) {
        return controller.read(resourceName);
    }

    @Override
    public Set<PropagationActionClassTO> getPropagationActionsClasses() {
        return CollectionWrapper.wrapPropagationActionClasses(controller.getPropagationActionsClasses());
    }

    @Override
    public List<ResourceTO> list() {
        return controller.list(null);
    }

    @Override
    public List<ResourceTO> list(final Long connInstanceId) {
        return controller.list(connInstanceId);
    }

    @Override
    public ConnObjectTO getConnectorObject(final String resourceName, final AttributableType type, final Long id) {
        return controller.getConnectorObject(resourceName, type, id);
    }

    @Override
    public boolean check(final ResourceTO resourceTO) {
        return controller.check(resourceTO);
    }

    @Override
    public BulkActionRes bulkAction(final BulkAction bulkAction) {
        return controller.bulkAction(bulkAction);
    }

    @Override
    public BulkActionRes usersBulkAssociationAction(final String resourceName, final BulkAssociationAction bulkAction) {
        return controller.usersBulkAssociationAction(resourceName, bulkAction);
    }

    @Override
    public BulkActionRes rolesBulkAssociationAction(final String resourceName, final BulkAssociationAction bulkAction) {
        return controller.rolesBulkAssociationAction(resourceName, bulkAction);
    }
}
