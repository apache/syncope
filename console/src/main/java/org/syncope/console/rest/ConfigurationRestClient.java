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
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.LoggerTO;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Connectors services.
 */
@Component
public class ConfigurationRestClient extends AbstractBaseRestClient {

    public String dbContentAsXml()
            throws SyncopeClientCompositeErrorException {

        return restTemplate.getForObject(baseURL
                + "configuration/dbexport.json", String.class);
    }

    /**
     * Get all stored configurations.
     * @return ConfigurationTOs
     */
    public List<ConfigurationTO> getAllConfigurations()
            throws SyncopeClientCompositeErrorException {

        List<ConfigurationTO> configurations = null;

        configurations = Arrays.asList(
                restTemplate.getForObject(baseURL
                + "configuration/list.json", ConfigurationTO[].class));

        return configurations;
    }

    /**
     * Load an existent configuration.
     * @return ConfigurationTO object if the configuration exists,
     * null otherwise
     */
    public ConfigurationTO readConfiguration(String key)
            throws SyncopeClientCompositeErrorException {

        ConfigurationTO configurationTO =
                restTemplate.getForObject(baseURL
                + "configuration/read/{key}.json", ConfigurationTO.class,
                key);


        return configurationTO;
    }

    /**
     * Create a new configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean createConfiguration(ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO =
                restTemplate.postForObject(baseURL
                + "configuration/create",
                configurationTO, ConfigurationTO.class);

        return configurationTO.equals(newConfigurationTO);
    }

    /**
     * Update an existent configuration.
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean updateConfiguration(ConfigurationTO configurationTO) {
        ConfigurationTO newConfigurationTO = null;

        try {
            newConfigurationTO = restTemplate.postForObject(baseURL
                    + "configuration/update", configurationTO,
                    ConfigurationTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a configuration", e);
            return false;
        }
        return (configurationTO.equals(newConfigurationTO)) ? true : false;
    }

    /**
     * Deelete a configuration by key.
     * @throws UnsupportedEncodingException
     */
    public void deleteConfiguration(String key)
            throws SyncopeClientCompositeErrorException {

        restTemplate.delete(baseURL
                + "configuration/delete/{key}.json",
                key);
    }

    public WorkflowDefinitionTO getWorkflowDefinition()
            throws SyncopeClientCompositeErrorException {

        return restTemplate.getForObject(baseURL
                + "configuration/workflow/definition.json",
                WorkflowDefinitionTO.class);
    }

    public void updateWorkflowDefinition(final WorkflowDefinitionTO workflowDef)
            throws SyncopeClientCompositeErrorException {

        restTemplate.put(baseURL
                + "configuration/workflow/definition.json", workflowDef);
    }

    /**
     * Get all loggers.
     * @return LoggerTOs
     */
    public List<LoggerTO> getLoggers()
            throws SyncopeClientCompositeErrorException {

        List<LoggerTO> loggers = Arrays.asList(
                restTemplate.getForObject(
                baseURL + "log/controller/list", LoggerTO[].class));

        return loggers;
    }

    public boolean setLoggerLevel(final String name, final String level) {
        boolean result;
        try {
            restTemplate.postForObject(
                    baseURL + "log/controller/{name}/{level}",
                    null, LoggerTO.class, name, level);
            result = true;
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While setting a logger's level", e);
            result = false;
        }

        return result;
    }
}
