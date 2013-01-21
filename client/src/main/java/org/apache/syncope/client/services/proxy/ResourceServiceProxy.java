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
package org.apache.syncope.client.services.proxy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.springframework.web.client.RestTemplate;

public class ResourceServiceProxy extends SpringServiceProxy implements ResourceService {

    public ResourceServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public ResourceTO create(final ResourceTO resourceTO) {
        return getRestTemplate().postForObject(baseUrl + "resource/create.json", resourceTO, ResourceTO.class);
    }

    @Override
    public ResourceTO update(final String resourceName, final ResourceTO resourceTO) {
        return getRestTemplate().postForObject(baseUrl + "resource/update.json", resourceTO, ResourceTO.class);
    }

    @Override
    public ResourceTO delete(final String resourceName) {
        return getRestTemplate().getForObject(baseUrl + "resource/delete/{resourceName}.json", ResourceTO.class,
                resourceName);
    }

    @Override
    public ResourceTO read(final String resourceName) {
        return getRestTemplate().getForObject(baseUrl + "resource/read/{resourceName}.json", ResourceTO.class,
                resourceName);
    }

    @Override
    public Set<String> getPropagationActionsClasses() {
        return new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(baseUrl
                + "resource/propagationActionsClasses.json", String[].class)));
    }

    @Override
    public List<ResourceTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "resource/list.json", ResourceTO[].class));
    }

    @Override
    public List<ResourceTO> list(final Long connInstanceId) {
        if (connInstanceId == null) {
            return list();
        }

        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "resource/list.json?connInstanceId={connId}",
                ResourceTO[].class, connInstanceId));
    }

    @Override
    public ConnObjectTO getConnector(final String resourceName, final AttributableType type, final String objectId) {
        return getRestTemplate().getForObject(baseUrl + "resource/{resourceName}/read/{type}/{objectId}.json",
                ConnObjectTO.class, resourceName, type.name(), objectId);
    }

    @Override
    public boolean check(final ResourceTO resourceTO) {
        return getRestTemplate().postForObject(baseUrl + "resource/check.json", resourceTO, Boolean.class).
                booleanValue();
    }
}
