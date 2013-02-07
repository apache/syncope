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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.to.ConnBundleTO;
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.types.ConnConfProperty;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

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
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "connector/list.json?lang=" + SyncopeSession.get().getLocale(), ConnInstanceTO[].class));
    }

    /**
     * Create new connector.
     *
     * @param schemaTO
     */
    public void create(final ConnInstanceTO connectorTO) {
        connectorTO.setConfiguration(filterProperties(connectorTO.getConfiguration()));
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "connector/create.json", connectorTO, ConnInstanceTO.class);
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
            connectorTO = SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "connector/read/" + connectorInstanceId, ConnInstanceTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a connector", e);
        }

        return connectorTO;
    }

    public void update(final ConnInstanceTO connectorTO) {
        connectorTO.setConfiguration(filterProperties(connectorTO.getConfiguration()));
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "connector/update.json", connectorTO, ConnInstanceTO.class);
    }

    public ConnInstanceTO delete(Long id) {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "connector/delete/{connectorId}.json", ConnInstanceTO.class, id.toString());
    }

    public List<ConnBundleTO> getAllBundles() {
        List<ConnBundleTO> bundles = null;

        try {
            bundles = Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                    baseURL + "connector/bundle/list?lang=" + SyncopeSession.get().getLocale(), ConnBundleTO[].class));
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
    public List<ConnConfProperty> getConnectorProperties(final Long connectorId) {
        List<ConnConfProperty> properties = null;

        try {
            properties = Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(baseURL
                    + "connector/{connectorId}/configurationProperty/list", ConnConfProperty[].class, connectorId));

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
     * Test connector connection.
     *
     * @param connectorTO connector.
     * @return Connection status.
     */
    public boolean check(final ConnInstanceTO connectorTO) {
        ConnInstanceTO toBeChecked = new ConnInstanceTO();
        BeanUtils.copyProperties(connectorTO, toBeChecked, new String[]{"configuration"});
        toBeChecked.setConfiguration(filterProperties(connectorTO.getConfiguration()));

        boolean check = false;
        try {
            check = SyncopeSession.get().getRestTemplate().postForObject(
                    baseURL + "connector/check.json", toBeChecked, Boolean.class);
        } catch (Exception e) {
            LOG.error("While checking {}", toBeChecked, e);
        }

        return check;
    }

    /**
     * Test resource connection.
     *
     * @param connectorTO connector.
     * @return Connection status.
     */
    public boolean check(final ResourceTO resourceTO) {
        boolean check = false;
        try {
            check = SyncopeSession.get().getRestTemplate().postForObject(
                    baseURL + "resource/check.json", resourceTO, Boolean.class);
        } catch (Exception e) {
            LOG.error("Connector not found {}", resourceTO.getConnectorId(), e);
        }

        return check;
    }

    public List<String> getSchemaNames(final ConnInstanceTO connectorTO) {
        List<String> schemaNames = null;

        try {
            schemaNames = Arrays.asList(SyncopeSession.get().getRestTemplate().postForObject(
                    baseURL + "connector/schema/list", connectorTO, String[].class));

            // re-order schema names list
            Collections.sort(schemaNames);
        } catch (Exception e) {
            LOG.error("While getting resource schema names", e);
        }

        return schemaNames;
    }
}
