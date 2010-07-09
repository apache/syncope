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

import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ConnectorInstanceTOs;
import org.syncope.client.to.ConnectorBundleTOs;

/**
 * Console client for invoking Rest Connectors services.
 */
public class ConnectorsRestClient {

    RestClient restClient;

    /**
     * Get all connectors.
     * @return SchemaTOs
     */
    public ConnectorInstanceTOs getAllConnectors() {
        ConnectorInstanceTOs connectors = null;

        connectors = restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "connector/list.json", ConnectorInstanceTOs.class);

        return connectors;
    }

    /**
     * Create new connector.
     * @param schemaTO
     */
    public void createConnector(ConnectorInstanceTO connectorTO) {
        restClient.getRestTemplate().postForObject(restClient.getBaseURL() +
                "connector/create", connectorTO, ConnectorInstanceTO.class);
    }

    /**
     * Load an already existent connector by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public ConnectorInstanceTO readConnector(String name) {
        ConnectorInstanceTO schema = restClient.getRestTemplate().getForObject(restClient.getBaseURL() + "connector/read/" + name + ".json", ConnectorInstanceTO.class);
        return schema;
    }

    /**
     * Update an already existent connector.
     * @param schemaTO updated
     */
    public void updateConnector(ConnectorInstanceTO connectorTO) {
        ConnectorInstanceTO updatedTO = restClient.getRestTemplate().postForObject(restClient.getBaseURL() + "connector/update.json", connectorTO, ConnectorInstanceTO.class);
    }

    /**
     * Delete an already existent connector by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteConnector(Long id) {
        restClient.getRestTemplate().delete(restClient.getBaseURL() +
                "connector/delete/{connectorId}.json",id.toString());
    }

    public ConnectorBundleTOs getAllBundles() {

        ConnectorBundleTOs bundles = restClient.getRestTemplate().getForObject(
                restClient.getBaseURL() + "connector/getBundles.json",
                ConnectorBundleTOs.class);

        return bundles;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
}