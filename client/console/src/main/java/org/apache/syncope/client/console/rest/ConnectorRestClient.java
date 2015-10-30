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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Connectors services.
 */
@Component
public class ConnectorRestClient extends BaseRestClient {

    private static final long serialVersionUID = -6870366819966266617L;

    public List<ConnInstanceTO> getAllConnectors() {
        List<ConnInstanceTO> connectors = Collections.<ConnInstanceTO>emptyList();
        try {
            connectors = getService(ConnectorService.class).list(SyncopeConsoleSession.get().getLocale().toString());
        } catch (Exception e) {
            LOG.error("While reading connectors", e);
        }
        return connectors;
    }

    public ConnInstanceTO create(final ConnInstanceTO connectorTO) {
        Set<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConf());
        connectorTO.getConf().clear();
        connectorTO.getConf().addAll(filteredConf);

        final ConnectorService service = getService(ConnectorService.class);
        final Response response = service.create(connectorTO);

        return getObject(service, response.getLocation(), ConnInstanceTO.class);
    }

    /**
     * Load an already existent connector by its name.
     *
     * @param key the id
     * @return ConnInstanceTO
     */
    public ConnInstanceTO read(final Long key) {
        ConnInstanceTO connectorTO = null;

        try {
            connectorTO = getService(ConnectorService.class).
                    read(key, SyncopeConsoleSession.get().getLocale().toString());
        } catch (SyncopeClientException e) {
            LOG.error("While reading a connector", e);
        }

        return connectorTO;
    }

    public void update(final ConnInstanceTO connectorTO) {
        Set<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConf());
        connectorTO.getConf().clear();
        connectorTO.getConf().addAll(filteredConf);
        getService(ConnectorService.class).update(connectorTO);
    }

    public ConnInstanceTO delete(final Long key) {
        ConnInstanceTO connectorTO = getService(ConnectorService.class).
                read(key, SyncopeConsoleSession.get().getLocale().toString());
        getService(ConnectorService.class).delete(key);
        return connectorTO;
    }

    public List<ConnBundleTO> getAllBundles() {
        List<ConnBundleTO> bundles = Collections.<ConnBundleTO>emptyList();

        try {
            bundles = getService(ConnectorService.class).getBundles(SyncopeConsoleSession.get().getLocale().toString());
        } catch (SyncopeClientException e) {
            LOG.error("While getting connector bundles", e);
        }

        return bundles;
    }

    private Set<ConnConfProperty> filterProperties(final Set<ConnConfProperty> properties) {
        Set<ConnConfProperty> newProperties = new HashSet<>();

        for (ConnConfProperty property : properties) {
            ConnConfProperty prop = new ConnConfProperty();
            prop.setSchema(property.getSchema());
            prop.setOverridable(property.isOverridable());

            final List<Object> parsed = new ArrayList<>();
            if (property.getValues() != null) {
                for (Object obj : property.getValues()) {
                    if (obj != null && !obj.toString().isEmpty()) {
                        parsed.add(obj);
                    }
                }
            }
            prop.getValues().addAll(parsed);
            newProperties.add(prop);
        }
        return newProperties;
    }

    /**
     * Test connector connection.
     *
     * @param connectorTO connector
     * @return Connection status
     */
    public boolean check(final ConnInstanceTO connectorTO) {
        ConnInstanceTO toBeChecked = new ConnInstanceTO();
        BeanUtils.copyProperties(connectorTO, toBeChecked, new String[] { "configuration", "configurationMap" });
        toBeChecked.getConf().addAll(filterProperties(connectorTO.getConf()));

        boolean check = false;
        try {
            getService(ConnectorService.class).check(toBeChecked);
            check = true;
        } catch (Exception e) {
            LOG.error("While checking {}", toBeChecked, e);
        }

        return check;
    }

    public boolean check(final ResourceTO resourceTO) {
        boolean check = false;
        try {
            getService(ResourceService.class).check(resourceTO);
            check = true;
        } catch (Exception e) {
            LOG.error("Connector not found {}", resourceTO.getConnector(), e);
        }

        return check;
    }

    public List<ConnIdObjectClassTO> buildObjectClassInfo(
            final ConnInstanceTO connInstanceTO, final boolean includeSpecial) {

        List<ConnIdObjectClassTO> result = Collections.emptyList();
        try {
            result = getService(ConnectorService.class).buildObjectClassInfo(connInstanceTO, includeSpecial);
        } catch (Exception e) {
            LOG.error("While getting supported object classes", e);
        }

        return result;
    }

    public void reload() {
        getService(ConnectorService.class).reload();
    }

    public BulkActionResult bulkAction(final BulkAction action) {
        return getService(ConnectorService.class).bulk(action);
    }
}
