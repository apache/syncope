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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.ws.rs.BadRequestException;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.BulkAssociationAction;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationActionClassTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.AbstractResourceAssociator;
import org.apache.syncope.core.rest.controller.ResourceController;
import org.apache.syncope.core.rest.controller.RoleController;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl extends AbstractServiceImpl implements ResourceService, ContextAware {

    @Autowired
    private ResourceController controller;

    @Autowired
    private UserController userController;

    @Autowired
    private RoleController roleController;

    @Override
    public Response create(final ResourceTO resourceTO) {
        ResourceTO created = controller.create(resourceTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getName()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID.toString(), created.getName()).
                build();
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
    public List<PropagationActionClassTO> getPropagationActionsClasses() {
        return CollectionWrapper.wrap(controller.getPropagationActionsClasses(), PropagationActionClassTO.class);
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
    public BulkActionRes bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }

    @Override
    public BulkActionRes bulkAssociation(final String resourceName,
            final BulkAssociationAction bulkAssociationAction, final AttributableType type) {

        if (bulkAssociationAction.getOperation() == null || type == AttributableType.MEMBERSHIP) {
            throw new BadRequestException();
        }

        AbstractResourceAssociator<? extends AbstractAttributableTO> associator = type == AttributableType.USER
                ? userController
                : roleController;

        final BulkActionRes res = new BulkActionRes();

        for (Long id : bulkAssociationAction.getTargets()) {
            final Set<String> resources = Collections.singleton(resourceName);
            try {
                switch (bulkAssociationAction.getOperation()) {
                    case DEPROVISION:
                        associator.deprovision(id, resources);
                        break;

                    case UNASSIGN:
                        associator.unassign(id, resources);
                        break;

                    case UNLINK:
                        associator.unlink(id, resources);
                        break;

                    default:
                }

                res.add(id, BulkActionRes.Status.SUCCESS);
            } catch (Exception e) {
                LOG.warn("While executing {} on {} {}", bulkAssociationAction.getOperation(), type, id, e);
                res.add(id, BulkActionRes.Status.FAILURE);
            }
        }

        return res;
    }

}
