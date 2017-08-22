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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.springframework.beans.BeanUtils;

/**
 * Console client for invoking Rest Connectors services.
 */
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
        List<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConf());
        connectorTO.getConf().clear();
        connectorTO.getConf().addAll(filteredConf);

        ConnectorService service = getService(ConnectorService.class);
        Response response = service.create(connectorTO);

        return getObject(service, response.getLocation(), ConnInstanceTO.class);
    }

    public List<String> getObjectClasses(final String connectorKey) {
        List<String> result = new ArrayList<>();

        ConnectorService service = getService(ConnectorService.class);
        ConnInstanceTO connInstance = service.read(connectorKey, SyncopeConsoleSession.get().getLocale().getLanguage());
        if (connInstance != null) {
            result.addAll(service.buildObjectClassInfo(connInstance, true).stream().
                    map(input -> input.getType()).collect(Collectors.toList()));
        }

        return result;
    }

    public List<String> getExtAttrNames(
            final String objectClass, final String connectorKey, final Collection<ConnConfProperty> conf) {

        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setKey(connectorKey);
        connInstanceTO.getConf().addAll(conf);

        // SYNCOPE-156: use provided info to give schema names (and type!) by ObjectClass
        Optional<ConnIdObjectClassTO> connIdObjectClass = buildObjectClassInfo(connInstanceTO, false).stream().
                filter(object -> object.getType().equalsIgnoreCase(objectClass)).
                findAny();

        return connIdObjectClass.isPresent() ? connIdObjectClass.get().getAttributes() : new ArrayList<>();
    }

    /**
     * Load an already existent connector by its name.
     *
     * @param key the id
     * @return ConnInstanceTO
     */
    public ConnInstanceTO read(final String key) {
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
        List<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConf());
        connectorTO.getConf().clear();
        connectorTO.getConf().addAll(filteredConf);
        getService(ConnectorService.class).update(connectorTO);
    }

    public ConnInstanceTO delete(final String key) {
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

    private List<ConnConfProperty> filterProperties(final Collection<ConnConfProperty> properties) {
        List<ConnConfProperty> newProperties = new ArrayList<>();

        properties.stream().map(property -> {
            ConnConfProperty prop = new ConnConfProperty();
            prop.setSchema(property.getSchema());
            prop.setOverridable(property.isOverridable());
            final List<Object> parsed = new ArrayList<>();
            if (property.getValues() != null) {
                property.getValues().stream().
                        filter(obj -> (obj != null && !obj.toString().isEmpty())).
                        forEachOrdered((obj) -> {
                            parsed.add(obj);
                        });
            }
            prop.getValues().addAll(parsed);
            return prop;
        }).forEachOrdered(prop -> {
            newProperties.add(prop);
        });
        return newProperties;
    }

    public Pair<Boolean, String> check(final ConnInstanceTO connectorTO) {
        ConnInstanceTO toBeChecked = new ConnInstanceTO();
        BeanUtils.copyProperties(connectorTO, toBeChecked, new String[] { "configuration", "configurationMap" });
        toBeChecked.getConf().addAll(filterProperties(connectorTO.getConf()));

        boolean check = false;
        String errorMessage = null;
        try {
            getService(ConnectorService.class).check(toBeChecked);
            check = true;
        } catch (Exception e) {
            LOG.error("While checking {}", toBeChecked, e);
            errorMessage = e.getMessage();
        }

        return Pair.of(check, errorMessage);
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
}
