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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.services.ConnectorService;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.ConnConfProperty;
import org.springframework.web.client.RestTemplate;

public class ConnectorServiceProxy extends SpringServiceProxy implements ConnectorService {

    public ConnectorServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public Response create(final ConnInstanceTO connInstanceTO) {
        ConnInstanceTO response = getRestTemplate().postForObject(baseUrl + "connector/create.json", connInstanceTO,
                ConnInstanceTO.class);
        URI location = URI.create(baseUrl + "connector/read/" + response.getId() + ".json");
        return Response.created(location).build();
    }

    @Override
    public void update(final Long connInstanceId, final ConnInstanceTO connInstanceTO) {
        getRestTemplate().postForObject(baseUrl + "connector/update.json", connInstanceTO, ConnInstanceTO.class);
    }

    @Override
    public void delete(final Long connInstanceId) {
        getRestTemplate().getForObject(baseUrl + "connector/delete/{connInstanceId}.json", ConnInstanceTO.class,
                connInstanceId);
    }

    @Override
    public List<ConnInstanceTO> list(final String lang) {
        String param = (lang == null)
                ? ""
                : "?lang=" + lang;

        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "connector/list.json" + param,
                ConnInstanceTO[].class));
    }

    @Override
    public ConnInstanceTO read(final Long connInstanceId) {
        return getRestTemplate().getForObject(baseUrl + "connector/read/{connInstanceId}", ConnInstanceTO.class,
                connInstanceId);
    }

    @Override
    public List<ConnBundleTO> getBundles(final String lang) {
        String param = (lang == null)
                ? ""
                : "?lang=" + lang;

        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "connector/bundle/list.json" + param,
                ConnBundleTO[].class));
    }

    @Override
    public List<SchemaTO> getSchemaNames(final Long connInstanceId, final ConnInstanceTO connInstanceTO,
            final boolean showall) {
        final String queryString = "?showall=" + showall;

        List<String> response = Arrays.asList(getRestTemplate().postForObject(
                baseUrl + "connector/schema/list" + queryString, connInstanceTO, String[].class));
        List<SchemaTO> schemaNames = new ArrayList<SchemaTO>();
        for (String name : response) {
            SchemaTO schemaTO = new SchemaTO();
            schemaTO.setName(name);
            schemaNames.add(schemaTO);
        }
        return schemaNames;
    }

    @Override
    public List<ConnConfProperty> getConfigurationProperties(final Long connInstanceId) {
        return Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "connector/{connInstanceId}/configurationProperty/list", ConnConfProperty[].class,
                connInstanceId));
    }

    @Override
    public boolean check(final ConnInstanceTO connInstanceTO) {
        return getRestTemplate().postForObject(baseUrl + "connector/check.json", connInstanceTO, Boolean.class);
    }

    @Override
    public ConnInstanceTO readByResource(final String resourceName) {
        return getRestTemplate().getForObject(baseUrl + "connector/{resourceName}/readByResource",
                ConnInstanceTO.class, resourceName);
    }

    @Override
    public void reload() {
        getRestTemplate().postForLocation(baseUrl + "connector/reload", null);
    }
}
