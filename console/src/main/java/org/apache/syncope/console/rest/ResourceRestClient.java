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
package org.apache.syncope.console.rest;

import java.util.List;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.wrap.SubjectId;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Resources services.
 */
@Component
public class ResourceRestClient extends BaseRestClient {

    private static final long serialVersionUID = -6898907679835668987L;

    public List<String> getPropagationActionsClasses() {
        List<String> actions = null;

        try {
            actions = CollectionWrapper.unwrap(getService(ResourceService.class).getPropagationActionsClasses());
        } catch (SyncopeClientException e) {
            LOG.error("While getting all propagation actions classes", e);
        }
        return actions;
    }

    public List<ResourceTO> getAllResources() {
        List<ResourceTO> resources = null;

        try {
            resources = getService(ResourceService.class).list();
        } catch (SyncopeClientException e) {
            LOG.error("While reading all resources", e);
        }

        return resources;
    }

    public void create(final ResourceTO resourceTO) {
        getService(ResourceService.class).create(resourceTO);
    }

    public ResourceTO read(final String name) {
        ResourceTO resourceTO = null;

        try {
            resourceTO = getService(ResourceService.class).read(name);
        } catch (SyncopeClientException e) {
            LOG.error("While reading a resource", e);
        }
        return resourceTO;
    }

    public void update(final ResourceTO resourceTO) {
        getService(ResourceService.class).update(resourceTO.getName(), resourceTO);
    }

    public void delete(final String name) {
        getService(ResourceService.class).delete(name);
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(ResourceService.class).bulk(action);
    }

    public BulkActionResult bulkAssociationAction(
            final String resourceName, final Class<? extends AbstractAttributableTO> typeRef,
            final ResourceDeassociationActionType type, final List<SubjectId> subjtectIds) {

        return getService(ResourceService.class).bulkDeassociation(resourceName,
                UserTO.class.isAssignableFrom(typeRef) ? AttributableType.USER : AttributableType.ROLE,
                type, subjtectIds);
    }
}
