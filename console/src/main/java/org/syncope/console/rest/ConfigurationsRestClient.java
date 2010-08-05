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

import org.syncope.client.to.ConfigurationTO;

/**
 * Console client for invoking Rest Connectors services.
 */
public class ConfigurationsRestClient {

    RestClient restClient;

    /**
     * Create a new configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean createConfiguration(ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO = restClient.getRestTemplate().postForObject(
                restClient.getBaseURL() + "configuration/create",
                configurationTO, ConfigurationTO.class);

        return (configurationTO.equals(newConfigurationTO))?true:false;
    }

    /**
     * Update an existent configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean udpateConfiguration(ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO = restClient.getRestTemplate().postForObject(
                 restClient.getBaseURL() + "configuration/update",
                 configurationTO, ConfigurationTO.class);

        return (configurationTO.equals(newConfigurationTO))?true:false;
    }
}