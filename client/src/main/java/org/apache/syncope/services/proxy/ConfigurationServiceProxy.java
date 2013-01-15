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

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.syncope.client.to.ConfigurationTO;
import org.apache.syncope.services.ConfigurationService;
import org.springframework.web.client.RestTemplate;

public class ConfigurationServiceProxy extends SpringServiceProxy implements ConfigurationService {

    public ConfigurationServiceProxy(String baseUrl, RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public ConfigurationTO create(ConfigurationTO configurationTO) {
        return getRestTemplate()
                .postForObject(baseUrl + "configuration/create", configurationTO, ConfigurationTO.class);
    }

    @Override
    public ConfigurationTO delete(String key) {
        return getRestTemplate().getForObject(baseUrl + "configuration/delete/{key}.json", ConfigurationTO.class, key);
    }

    @Override
    public List<ConfigurationTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "configuration/list.json",
                ConfigurationTO[].class));
    }

    @Override
    public ConfigurationTO read(String key) {
        return getRestTemplate().getForObject(baseUrl + "configuration/read/{key}.json", ConfigurationTO.class, key);
    }

    @Override
    public ConfigurationTO update(String key, ConfigurationTO configurationTO) {
        return getRestTemplate()
                .postForObject(baseUrl + "configuration/update", configurationTO, ConfigurationTO.class);
    }

    @Override
    public Set<String> getValidators() {
        return new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "configuration/validators.json", String[].class)));
    }

    @Override
    public Set<String> getMailTemplates() {
        return new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "configuration/mailTemplates.json", String[].class)));
    }

    @Override
    public Response dbExport() {
        return Response.ok(getRestTemplate().getForObject(baseUrl + "configuration/dbexport", InputStream.class))
                .build();
    }

}
