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
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConnBundleTO;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.ConnConfProperty;

/**
 * Console client for invoking Rest Connectors services.
 */
@Component
public class ConnectorRestClient extends AbstractBaseRestClient {

    /**
     * Get all connectors.
     * @return ConnectorInstanceTOs
     */
    public List<ConnInstanceTO> getAllConnectors() {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "connector/list.json",
                ConnInstanceTO[].class));
    }

    /**
     * Create new connector.
     * @param schemaTO
     */
    public void create(ConnInstanceTO connectorTO) {
        restTemplate.postForObject(baseURL
                + "connector/create.json", connectorTO,
                ConnInstanceTO.class);
    }

    /**
     * Load an already existent connector by its name.
     * @param connectorInstanceId the id
     * @return ConnInstanceTO
     */
    public ConnInstanceTO read(final Long connectorInstanceId) {
        ConnInstanceTO connectorTO = null;

        try {
            connectorTO = restTemplate.getForObject(
                    baseURL + "connector/read/" + connectorInstanceId,
                    ConnInstanceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a connector", e);
        }

        return connectorTO;
    }

    /**
     * Update an already existent connector.
     * @param schemaTO updated
     */
    public void update(ConnInstanceTO connectorTO) {
        restTemplate.postForObject(
                baseURL + "connector/update.json",
                connectorTO,
                ConnInstanceTO.class);
    }

    /**
     * Delete an already existent connector by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void delete(Long id) {
        restTemplate.delete(baseURL
                + "connector/delete/{connectorId}.json", id.toString());
    }

    public List<ConnBundleTO> getAllBundles() {
        List<ConnBundleTO> bundles = null;

        try {
            bundles = Arrays.asList(restTemplate.getForObject(
                    baseURL + "connector/bundle/list",
                    ConnBundleTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting connector bundles", e);
        }

        return bundles;
    }

    public List<String> getSchemaNames(final ResourceTO resourceTO) {
        List<String> schemaNames = null;

        try {
            schemaNames = Arrays.asList(restTemplate.postForObject(
                    baseURL + "connector/schema/list",
                    resourceTO, String[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting resource schema names", e);
        }

        return schemaNames;
    }

    /**
     * Get all configuration properties for the given connector instance.
     * @param connectorId the connector id
     * @return List of ConnConfProperty, or an empty list in case none found
     */
    public List<ConnConfProperty> getConnectorProperties(
            final Long connectorId) {
        List<ConnConfProperty> properties = null;
        try {
            properties = Arrays.asList(restTemplate.getForObject(baseURL
                    + "connector/{connectorId}/configurationProperty/list",
                    ConnConfProperty[].class, connectorId));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting connector configuration properties", e);
        }

        return properties;
    }
}
