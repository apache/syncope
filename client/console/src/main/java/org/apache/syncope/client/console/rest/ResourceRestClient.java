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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.ResourceDeassociationPatch;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Resources services.
 */
@Component
public class ResourceRestClient extends BaseRestClient {

    private static final long serialVersionUID = -6898907679835668987L;

    public List<String> getPropagationActionsClasses() {
        return new ArrayList<>(SyncopeConsoleSession.get().getSyncopeTO().getPropagationActions());
    }

    public List<ResourceTO> getAll() {
        List<ResourceTO> resources = null;

        try {
            resources = getService(ResourceService.class).list();
        } catch (SyncopeClientException e) {
            LOG.error("While reading all resources", e);
        }

        return resources;
    }

    public ResourceTO create(final ResourceTO resourceTO) {
        final ResourceService service = getService(ResourceService.class);
        final Response response = service.create(resourceTO);
        return getObject(service, response.getLocation(), ResourceTO.class);
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
        getService(ResourceService.class).update(resourceTO);
    }

    public void delete(final String name) {
        getService(ResourceService.class).delete(name);
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(ResourceService.class).bulk(action);
    }

    public BulkActionResult bulkAssociationAction(
            final String resourceName, final String anyTypeName,
            final ResourceDeassociationAction action, final List<Long> anyKeys) {

        ResourceDeassociationPatch patch = new ResourceDeassociationPatch();
        patch.setKey(resourceName);
        patch.setAnyTypeKey(anyTypeName);
        patch.setAction(action);
        patch.getAnyKyes().addAll(anyKeys);

        return getService(ResourceService.class).bulkDeassociation(patch);
    }
}
