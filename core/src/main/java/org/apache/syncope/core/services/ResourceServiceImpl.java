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
import javax.ws.rs.core.Response;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.wrap.PropagationActionClass;
import org.apache.syncope.common.wrap.SubjectId;
import org.apache.syncope.core.rest.controller.AbstractResourceAssociator;
import org.apache.syncope.core.rest.controller.ResourceController;
import org.apache.syncope.core.rest.controller.RoleController;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl extends AbstractServiceImpl implements ResourceService {

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
                header(RESTHeaders.RESOURCE_ID, created.getName()).
                build();
    }

    @Override
    public void update(final String resourceName, final ResourceTO resourceTO) {
        resourceTO.setName(resourceName);
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
    public List<PropagationActionClass> getPropagationActionsClasses() {
        return CollectionWrapper.wrap(controller.getPropagationActionsClasses(), PropagationActionClass.class);
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
    public ConnObjectTO getConnectorObject(final String resourceName, final SubjectType type, final Long id) {
        return controller.getConnectorObject(resourceName, type, id);
    }

    @Override
    public boolean check(final ResourceTO resourceTO) {
        return controller.check(resourceTO);
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }

    @Override
    public BulkActionResult bulkDeassociation(final String resourceName, final SubjectType subjectType,
            final ResourceDeassociationActionType type, final List<SubjectId> subjectIds) {

        AbstractResourceAssociator<? extends AbstractAttributableTO> associator = subjectType == SubjectType.USER
                ? userController
                : roleController;

        final BulkActionResult res = new BulkActionResult();

        for (SubjectId id : subjectIds) {
            final Set<String> resources = Collections.singleton(resourceName);
            try {
                switch (type) {
                    case DEPROVISION:
                        associator.deprovision(id.getElement(), resources);
                        break;

                    case UNASSIGN:
                        associator.unassign(id.getElement(), resources);
                        break;

                    case UNLINK:
                        associator.unlink(id.getElement(), resources);
                        break;

                    default:
                }

                res.add(id, BulkActionResult.Status.SUCCESS);
            } catch (Exception e) {
                LOG.warn("While executing {} on {} {}", type, subjectType, id.getElement(), e);
                res.add(id, BulkActionResult.Status.FAILURE);
            }
        }

        return res;
    }

}
