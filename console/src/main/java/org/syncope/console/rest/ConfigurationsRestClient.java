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

import java.io.UnsupportedEncodingException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.ConfigurationTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Connectors services.
 */
public class ConfigurationsRestClient {

    RestClient restClient;

    /**
     * Get all stored configurations.
     * @return ConfigurationTOs
     */
    public ConfigurationTOs getAllConfigurations() {

        ConfigurationTOs configurations = restClient.getRestTemplate().getForObject(
                restClient.getBaseURL() + "configuration/list.json",
                ConfigurationTOs.class);

        return configurations;

    }

   /**
     * Load an existent configuration.
     * @return ConfigurationTO object if the configuration exists, null otherwise
     */
    public ConfigurationTO readConfiguration() {

        ConfigurationTO configurationTO;
        try {
            configurationTO = restClient.getRestTemplate().getForObject(
                    restClient.getBaseURL() + "configuration/read/{confKey}.json",
                    ConfigurationTO.class, "users.attributes.view");
        } catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
            return null;
        }

        return configurationTO;
    }

    /**
     * Create a new configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean createConfiguration(ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO = restClient.getRestTemplate().postForObject(
                restClient.getBaseURL() + "configuration/create",
                configurationTO, ConfigurationTO.class);

        return (configurationTO.equals(newConfigurationTO)) ? true : false;
    }

    /**
     * Update an existent configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean updateConfiguration(ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO = restClient.getRestTemplate().postForObject(
                restClient.getBaseURL() + "configuration/update",
                configurationTO, ConfigurationTO.class);

        return (configurationTO.equals(newConfigurationTO)) ? true : false;
    }

    /**
     * Deelete a configuration by confKey
     * @throws UnsupportedEncodingException
     */
    public void deleteConfiguration(String confKey) throws UnsupportedEncodingException {
        try {
            restClient.getRestTemplate().delete( restClient.getBaseURL() + "configuration/delete/{confKey}.json",
                    confKey);
        } catch (HttpStatusCodeException e) {
            //assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
            throw e;
        }

    }

    /**
     * Getter for restClient attribute.
     * @return RestClient instance
     */
    public RestClient getRestClient() {
        return restClient;
    }

    /**
     * Setter for restClient attribute.
     * @param restClient instance
     */
    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
}