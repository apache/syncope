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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConnBundleTO;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.SyncopeSession;
import org.syncope.types.ConnConfProperty;

/**
 * Console client for invoking Rest Connectors services.
 */
@Component
public class ConnectorRestClient extends AbstractBaseRestClient {

    /**
     * Get all connectors.
     *
     * @return ConnectorInstanceTOs
     */
    public List<ConnInstanceTO> getAllConnectors() {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "connector/list.json?lang=" + SyncopeSession.get().
                getLocale(), ConnInstanceTO[].class));
    }

    /**
     * Create new connector.
     *
     * @param schemaTO
     */
    public void create(final ConnInstanceTO connectorTO) {
        filterProperties(connectorTO.getConfiguration());
        restTemplate.postForObject(baseURL
                + "connector/create.json", connectorTO, ConnInstanceTO.class);
    }

    /**
     * Load an already existent connector by its name.
     *
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

    public void update(final ConnInstanceTO connectorTO) {
        filterProperties(connectorTO.getConfiguration());
        restTemplate.postForObject(baseURL + "connector/update.json",
                connectorTO, ConnInstanceTO.class);
    }

    public void delete(Long id) {
        restTemplate.delete(baseURL
                + "connector/delete/{connectorId}.json", id.toString());
    }

    public List<ConnBundleTO> getAllBundles() {
        List<ConnBundleTO> bundles = null;

        try {
            bundles = Arrays.asList(restTemplate.getForObject(
                    baseURL + "connector/bundle/list?lang="
                    + SyncopeSession.get().getLocale(),
                    ConnBundleTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While getting connector bundles", e);
        }

        return bundles;
    }

    /**
     * Get all configuration properties for the given connector instance.
     *
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

    private Set<ConnConfProperty> filterProperties(final Set<ConnConfProperty> properties) {

        Set<ConnConfProperty> newProperties = new HashSet<ConnConfProperty>();

        for (ConnConfProperty property : properties) {
            ConnConfProperty prop = new ConnConfProperty();
            prop.setSchema(property.getSchema());
            prop.setOverridable(property.isOverridable());

            final List parsed = new ArrayList();

            for (Object obj : property.getValues()) {
                if (obj != null && !obj.toString().isEmpty()) {
                    parsed.add(obj);
                }
            }
            prop.setValues(parsed);
            newProperties.add(prop);
        }
        return newProperties;
    }

    /**
     * Test connection.
     *
     * @param connectorTO connector.
     * @return Connection status.
     */
    public Boolean check(final ConnInstanceTO connectorTO) {

        ConnInstanceTO connector = new ConnInstanceTO();
        BeanUtils.copyProperties(connectorTO, connector);

        connector.setConfiguration(
                filterProperties(connector.getConfiguration()));

        try {
            return restTemplate.postForObject(
                    baseURL + "connector/check.json", connector, Boolean.class);

        } catch (Exception e) {
            LOG.error("Connector not found {}", connector, e);
            return false;
        }
    }

    public List<String> getSchemaNames(final ConnInstanceTO connectorTO) {
        List<String> schemaNames = null;

        try {
            schemaNames = Arrays.asList(restTemplate.postForObject(
                    baseURL + "connector/schema/list",
                    connectorTO, String[].class));

            // re-order schema names list
            Collections.sort(schemaNames);

        } catch (Exception e) {
            LOG.error("While getting resource schema names", e);
        }

        return schemaNames;
    }
}
