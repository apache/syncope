/* 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Resources services.
 */
@Component
public class ResourceRestClient extends AbstractBaseRestClient {

    public List<String> getSchemaNames(final String resourceName) {
        List<String> schemaNames = null;

        try {
            schemaNames = Arrays.asList(restTemplate.getForObject(
                    baseURL + "resource/schema/{resourceName}/list",
                    String[].class, resourceName));

            // re-order schema names list
            Collections.sort(schemaNames);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting resource schema names", e);
        }

        return schemaNames;
    }

    public List<ResourceTO> getAllResources() {
        List<ResourceTO> resources = null;

        try {
            resources = Arrays.asList(restTemplate.getForObject(
                    baseURL + "resource/list.json",
                    ResourceTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading all resources", e);
        }

        return resources;
    }

    public void create(final ResourceTO resourceTO) {
        restTemplate.postForObject(baseURL
                + "resource/create", resourceTO, ResourceTO.class);
    }

    public ResourceTO read(final String name) {
        ResourceTO resourceTO = null;

        try {
            resourceTO = restTemplate.getForObject(
                    baseURL + "resource/read/" + name + ".json",
                    ResourceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a resource", e);
        }
        return resourceTO;
    }

    public void update(final ResourceTO resourceTO) {
        restTemplate.postForObject(
                baseURL + "resource/update.json", resourceTO,
                ResourceTO.class);
    }

    public void delete(final String name) {
        restTemplate.delete(baseURL
                + "resource/delete/{resourceName}.json", name);
    }
}
