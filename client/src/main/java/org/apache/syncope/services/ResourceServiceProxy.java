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

package org.apache.syncope.services;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.syncope.client.to.ConnObjectTO;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.types.AttributableType;
import org.springframework.web.client.RestTemplate;

public class ResourceServiceProxy extends SpringServiceProxy implements ResourceService {

    public ResourceServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public ResourceTO create(ResourceTO resourceTO) {
        return restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);
    }

    @Override
    public ResourceTO update(String resourceName, ResourceTO resourceTO) {
        return restTemplate.postForObject(BASE_URL + "resource/update.json", resourceTO, ResourceTO.class);
    }

    @Override
    public ResourceTO delete(String resourceName) {
        return restTemplate.getForObject(BASE_URL + "resource/delete/{resourceName}.json", ResourceTO.class,
                resourceName);
    }

    @Override
    public ResourceTO read(String resourceName) {
        return restTemplate.getForObject(BASE_URL + "resource/read/{resourceName}.json", ResourceTO.class,
                resourceName);
    }

    @Override
    public Set<String> getPropagationActionsClasses() {
        return new HashSet<String>(Arrays.asList(restTemplate.getForObject(BASE_URL
                + "resource/propagationActionsClasses.json", String[].class)));
    }

    @Override
    public List<ResourceTO> list(Long connInstanceId) {
        String query = (connInstanceId != null)
                ? query = "?connInstanceId=" + connInstanceId.toString()
                : "";

        return Arrays.asList(restTemplate.getForObject(BASE_URL + "resource/list.json" + query,
                ResourceTO[].class, connInstanceId));
    }

    @Override
    public ConnObjectTO getConnector(String resourceName, AttributableType type, String objectId) {
        return restTemplate.getForObject(BASE_URL + "resource/{resourceName}/read/{type}/{objectId}.json",
                ConnObjectTO.class, resourceName, type, objectId);
    }

    @Override
    public boolean check(ResourceTO resourceTO) {
        return restTemplate.postForObject(BASE_URL + "resource/check.json", resourceTO, Boolean.class)
                .booleanValue();
    }

}
