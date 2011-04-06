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
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

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
        List<ConnInstanceTO> connectors = null;

        connectors = Arrays.asList(restTemplate.getForObject(
                baseURL + "connector/list.json",
                ConnInstanceTO[].class));

        return connectors;
    }

    /**
     * Create new connector.
     * @param schemaTO
     */
    public void createConnector(ConnInstanceTO connectorTO) {
        ConnInstanceTO actual = null;

        try {
            actual = restTemplate.postForObject(baseURL
                    + "connector/create.json", connectorTO,
                    ConnInstanceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While creating a connector", e);
        }
    }

    /**
     * Load an already existent connector by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public ConnInstanceTO readConnector(String name) {
        ConnInstanceTO schema = null;

        try {
            schema = restTemplate.getForObject(
                    baseURL + "connector/read/" + name + ".json",
                    ConnInstanceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a connector", e);
        }

        return schema;
    }

    /**
     * Update an already existent connector.
     * @param schemaTO updated
     */
    public void updateConnector(ConnInstanceTO connectorTO) {
        try {
            restTemplate.postForObject(
                    baseURL + "connector/update.json",
                    connectorTO,
                    ConnInstanceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a connector", e);
        }
    }

    /**
     * Delete an already existent connector by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteConnector(Long id) {
        try {
            restTemplate.delete(baseURL
                    + "connector/delete/{connectorId}.json", id.toString());
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While deleting a connector", e);
        }
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
}
