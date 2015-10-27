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
package org.apache.syncope.client.cli.commands.resource;

import java.util.List;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.rest.api.service.ResourceService;

public class ResourceSyncopeOperations {

    private final ResourceService resourceService = SyncopeServices.get(ResourceService.class);

    public void delete(final String name) {
        resourceService.delete(name);
    }

    public List<ResourceTO> list() {
        return resourceService.list();
    }

    public ResourceTO read(final String name) {
        return resourceService.read(name);
    }

    public boolean exists(final String name) {
        try {
            read(name);
            return true;
        } catch (final SyncopeClientException ex) {
            return false;
        }
    }
}
