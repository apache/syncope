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
package org.apache.syncope.services.proxy;

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.client.to.ConnBundleTO;
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.services.ConnectorService;
import org.apache.syncope.types.ConnConfProperty;
import org.springframework.web.client.RestTemplate;

public class ConnectorServiceProxy extends SpringServiceProxy implements ConnectorService {

    public ConnectorServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public ConnInstanceTO create(ConnInstanceTO connectorTO) {
        return getRestTemplate().postForObject(baseUrl + "connector/create.json", connectorTO,
                ConnInstanceTO.class);
    }

    @Override
    public ConnInstanceTO update(Long connectorId, ConnInstanceTO connectorTO) {
        return getRestTemplate().postForObject(baseUrl + "connector/update.json", connectorTO,
                ConnInstanceTO.class);
    }

    @Override
    public ConnInstanceTO delete(Long connectorId) {
        return getRestTemplate().getForObject(baseUrl + "connector/delete/{connectorId}.json",
                ConnInstanceTO.class, connectorId);
    }

    @Override
    public List<ConnInstanceTO> list(String lang) {
        String param = (lang != null)
                ? "?lang=" + lang
                : "";

        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "connector/list.json" + param,
                ConnInstanceTO[].class));
    }

    @Override
    public ConnInstanceTO read(Long connectorId) {
        return getRestTemplate().getForObject(baseUrl + "connector/read/{connectorId}", ConnInstanceTO.class,
                connectorId);
    }

    @Override
    public List<ConnBundleTO> getBundles(String lang) {
        String param = (lang != null)
                ? "?lang=" + lang
                : "";

        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "connector/bundle/list.json" + param,
                ConnBundleTO[].class));
    }

    @Override
    public List<String> getSchemaNames(Long connectorId, ConnInstanceTO connectorTO, boolean showall) {
        String param = (showall)
                ? "?showall=true"
                : "?showall=false";

        return Arrays.asList(getRestTemplate().postForObject(baseUrl + "connector/schema/list" + param, connectorTO,
                String[].class));
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(Long connectorId) {
        return Arrays.asList(getRestTemplate()
                .getForObject(baseUrl + "connector/{connectorId}/configurationProperty/list",
                        ConnConfProperty[].class, connectorId));
    }

    @Override
    public boolean validate(ConnInstanceTO connectorTO) {
        return getRestTemplate().postForObject(baseUrl + "connector/check.json", connectorTO, Boolean.class);
    }

    @Override
    public ConnInstanceTO readConnectorBean(String resourceName) {
        return getRestTemplate().getForObject(baseUrl + "connector/{resourceName}/connectorBean",
                ConnInstanceTO.class, resourceName);
    }

}
