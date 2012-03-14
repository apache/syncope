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
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.LoggerTO;
import org.syncope.types.SyncopeLoggerLevel;

@Component
public class ConfigurationRestClient extends AbstractBaseRestClient {

    /**
     * Get all stored configurations.
     *
     * @return ConfigurationTOs
     */
    public List<ConfigurationTO> getAllConfigurations() {
        return Arrays.asList(restTemplate.getForObject(baseURL + "configuration/list.json", ConfigurationTO[].class));
    }

    public ConfigurationTO readConfiguration(final String key) {
        return restTemplate.getForObject(baseURL + "configuration/read/{key}.json", ConfigurationTO.class, key);
    }

    /**
     * Create a new configuration.
     *
     * @param configurationTO
     */
    public void createConfiguration(ConfigurationTO configurationTO) {
        restTemplate.postForObject(baseURL + "configuration/create", configurationTO, ConfigurationTO.class);
    }

    /**
     * Update an existing configuration.
     *
     * @param configurationTO
     */
    public void updateConfiguration(final ConfigurationTO configurationTO) {
        restTemplate.postForObject(baseURL + "configuration/update", configurationTO, ConfigurationTO.class);
    }

    /**
     * Deelete a configuration by key.
     */
    public void deleteConfiguration(final String key) {
        restTemplate.delete(baseURL + "configuration/delete/{key}.json", key);
    }

    public List<LoggerTO> listLogs() {
        return Arrays.asList(restTemplate.getForObject(baseURL + "logger/log/list", LoggerTO[].class));
    }

    public void setLogLevel(final String name, final SyncopeLoggerLevel level) {
        restTemplate.postForObject(baseURL + "logger/log/{name}/{level}", null, LoggerTO.class, name, level);
    }
}
