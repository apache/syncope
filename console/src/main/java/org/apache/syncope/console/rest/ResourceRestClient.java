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

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.to.ResourceTO;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Resources services.
 */
@Component
public class ResourceRestClient extends AbstractBaseRestClient {

    public List<String> getPropagationActionsClasses() {
        List<String> actions = null;

        try {
            actions = Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "resource/propagationActionsClasses.json", String[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting all propagation actions classes", e);
        }
        return actions;
    }

    public List<ResourceTO> getAllResources() {
        List<ResourceTO> resources = null;

        try {
            resources = Arrays.asList(SyncopeSession.get().getRestTemplate().
                    getForObject(baseURL + "resource/list.json", ResourceTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading all resources", e);
        }

        return resources;
    }

    public void create(final ResourceTO resourceTO) {
        SyncopeSession.get().getRestTemplate().postForObject(baseURL + "resource/create", resourceTO, ResourceTO.class);
    }

    public ResourceTO read(final String name) {
        ResourceTO resourceTO = null;

        try {
            resourceTO = SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "resource/read/" + name + ".json", ResourceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a resource", e);
        }
        return resourceTO;
    }

    public void update(final ResourceTO resourceTO) {
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "resource/update.json", resourceTO, ResourceTO.class);
    }

    public ResourceTO delete(final String name) {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "resource/delete/{resourceName}.json", ResourceTO.class, name);
    }
}
