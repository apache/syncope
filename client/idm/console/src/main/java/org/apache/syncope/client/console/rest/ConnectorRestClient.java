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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.springframework.beans.BeanUtils;

/**
 * Console client for invoking Rest Connectors services.
 */
public class ConnectorRestClient extends BaseRestClient {

    private static final long serialVersionUID = -6870366819966266617L;

    public static List<ConnInstanceTO> getAllConnectors() {
        List<ConnInstanceTO> connectors = List.of();
        try {
            connectors = getService(ConnectorService.class).list(SyncopeConsoleSession.get().getLocale().toString());
        } catch (Exception e) {
            LOG.error("While reading connectors", e);
        }
        return connectors;
    }

    public static ConnInstanceTO create(final ConnInstanceTO connectorTO) {
        List<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConf());
        connectorTO.getConf().clear();
        connectorTO.getConf().addAll(filteredConf);

        ConnectorService service = getService(ConnectorService.class);
        Response response = service.create(connectorTO);

        return getObject(service, response.getLocation(), ConnInstanceTO.class);
    }

    public static List<String> getObjectClasses(final String connectorKey) {
        List<String> result = new ArrayList<>();
        try {
            ConnectorService service = getService(ConnectorService.class);
            ConnInstanceTO connInstance = service.read(connectorKey, SyncopeConsoleSession.get().getLocale().
                    getLanguage());
            if (connInstance != null) {
                result.addAll(service.buildObjectClassInfo(connInstance, true).stream().
                        map(ConnIdObjectClassTO::getType).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            LOG.error("While reading object classes for connector {}", connectorKey, e);
        }
        return result;
    }

    public static List<String> getExtAttrNames(
            final String adminRealm,
            final String objectClass,
            final String connectorKey,
            final Collection<ConnConfProperty> conf) {

        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setAdminRealm(adminRealm);
        connInstanceTO.setKey(connectorKey);
        connInstanceTO.getConf().addAll(conf);

        // SYNCOPE-156: use provided info to give schema names (and type!) by ObjectClass
        Optional<ConnIdObjectClassTO> connIdObjectClass = buildObjectClassInfo(connInstanceTO, false).stream().
                filter(object -> object.getType().equalsIgnoreCase(objectClass)).
                findAny();

        return connIdObjectClass.map(connIdObjectClassTO -> connIdObjectClassTO.getAttributes().stream().
            map(PlainSchemaTO::getKey).collect(Collectors.toList())).orElseGet(List::of);
    }

    /**
     * Load an already existent connector by its name.
     *
     * @param key the id
     * @return ConnInstanceTO
     */
    public static ConnInstanceTO read(final String key) {
        ConnInstanceTO connectorTO = null;

        try {
            connectorTO = getService(ConnectorService.class).
                    read(key, SyncopeConsoleSession.get().getLocale().toString());
        } catch (SyncopeClientException e) {
            LOG.error("While reading a connector", e);
        }

        return connectorTO;
    }

    public static void update(final ConnInstanceTO connectorTO) {
        List<ConnConfProperty> filteredConf = filterProperties(connectorTO.getConf());
        connectorTO.getConf().clear();
        connectorTO.getConf().addAll(filteredConf);
        getService(ConnectorService.class).update(connectorTO);
    }

    public static ConnInstanceTO delete(final String key) {
        ConnInstanceTO connectorTO = getService(ConnectorService.class).
                read(key, SyncopeConsoleSession.get().getLocale().toString());
        getService(ConnectorService.class).delete(key);
        return connectorTO;
    }

    public static List<ConnBundleTO> getAllBundles() {
        List<ConnBundleTO> bundles = List.of();

        try {
            bundles = getService(ConnectorService.class).getBundles(SyncopeConsoleSession.get().getLocale().toString());
        } catch (SyncopeClientException e) {
            LOG.error("While getting connector bundles", e);
        }

        return bundles;
    }

    private static List<ConnConfProperty> filterProperties(final Collection<ConnConfProperty> properties) {
        List<ConnConfProperty> newProperties = new ArrayList<>();

        properties.stream().map(property -> {
            ConnConfProperty prop = new ConnConfProperty();
            prop.setSchema(property.getSchema());
            prop.setOverridable(property.isOverridable());
            final List<Object> parsed = new ArrayList<>();
            if (property.getValues() != null) {
                property.getValues().stream().
                        filter(obj -> (obj != null && !obj.toString().isEmpty())).
                        forEachOrdered(parsed::add);
            }
            prop.getValues().addAll(parsed);
            return prop;
        }).forEachOrdered(newProperties::add);
        return newProperties;
    }

    public static boolean check(final String coreAddress, final String domain, final String jwt, final String key)
            throws IOException {

        WebClient client = WebClient.create(coreAddress).path("connectors").
                accept(MediaType.APPLICATION_JSON_TYPE).
                type(MediaType.APPLICATION_JSON_TYPE).
                header(RESTHeaders.DOMAIN, domain).
                authorization("Bearer " + jwt);
        Response response = client.path(key).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            response = client.back(false).path("check").
                    post(IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));
            return response.getStatus() == Response.Status.NO_CONTENT.getStatusCode();
        }
        return false;
    }

    public static Pair<Boolean, String> check(final ConnInstanceTO connectorTO) {
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

    public static List<ConnIdObjectClassTO> buildObjectClassInfo(
            final ConnInstanceTO connInstanceTO, final boolean includeSpecial) {

        List<ConnIdObjectClassTO> result = List.of();
        try {
            result = getService(ConnectorService.class).buildObjectClassInfo(connInstanceTO, includeSpecial);
        } catch (Exception e) {
            LOG.error("While getting supported object classes", e);
        }

        return result;
    }

    public static void reload() {
        getService(ConnectorService.class).reload();
    }
}
