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
package org.syncope.core.rest;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.connid.bundles.soap.WebServiceConnector;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.ConnBundleTO;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.types.ConnConfProperty;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnectorCapability;

public class ConnInstanceTestITCase extends AbstractTest {

    private static String connidSoapVersion;
    private static String bundlesDirectory;

    @Before
    public void init() {
        Properties props = new java.util.Properties();
        try {
            InputStream propStream =
                    getClass().getResourceAsStream(
                    "/bundles.properties");
            props.load(propStream);
            connidSoapVersion = props.getProperty("connid.soap.version");
            bundlesDirectory = props.getProperty("bundles.directory");
        } catch (Throwable t) {
            LOG.error("Could not load bundles.properties", t);
        }
        assertNotNull(connidSoapVersion);
        assertNotNull(bundlesDirectory);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public void createWithException() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        restTemplate.postForObject(BASE_URL + "connector/create.json",
                connectorTO, ConnInstanceTO.class);
    }

    @Test
    public void create() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        // set connector version
        connectorTO.setVersion(connidSoapVersion);

        // set connector name
        connectorTO.setConnectorName(WebServiceConnector.class.getSimpleName());

        // set bundle name
        connectorTO.setBundleName("org.connid.bundles.soap");

        connectorTO.setDisplayName("Display name");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.setValue("http://localhost:8888/wssample/services");

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        servicename.setValue("Provisioning");

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        // set connector capabilities
        connectorTO.addCapability(ConnectorCapability.ASYNC_CREATE);
        connectorTO.addCapability(ConnectorCapability.SYNC_CREATE);
        connectorTO.addCapability(ConnectorCapability.ASYNC_UPDATE);

        ConnInstanceTO actual =
                (ConnInstanceTO) restTemplate.postForObject(
                BASE_URL + "connector/create.json",
                connectorTO, ConnInstanceTO.class);

        assertNotNull(actual);

        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
        assertEquals("Display name", actual.getDisplayName());
        assertEquals(connectorTO.getCapabilities(), actual.getCapabilities());

        Throwable t = null;

        // check for the updating
        connectorTO.setId(actual.getId());
        connectorTO.removeCapability(ConnectorCapability.ASYNC_UPDATE);
        actual = null;
        try {
            actual = restTemplate.postForObject(
                    BASE_URL + "connector/update.json",
                    connectorTO, ConnInstanceTO.class);
        } catch (HttpStatusCodeException e) {
            LOG.error("update failed", e);
            t = e;
        }

        assertNull(t);
        assertNotNull(actual);
        assertEquals(connectorTO.getCapabilities(), actual.getCapabilities());

        // check also for the deletion of the created object
        try {
            restTemplate.delete(
                    BASE_URL + "connector/delete/{connectorId}.json",
                    actual.getId().toString());
        } catch (HttpStatusCodeException e) {
            LOG.error("delete failed", e);
            t = e;
        }

        assertNull(t);

        // check the non existence
        try {
            restTemplate.getForObject(
                    BASE_URL + "connector/read/{connectorId}",
                    ConnInstanceTO.class,
                    actual.getId().toString());
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void update() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        // set connector instance id
        connectorTO.setId(100L);

        // set connector version
        connectorTO.setVersion(connidSoapVersion);

        // set connector name
        connectorTO.setConnectorName(WebServiceConnector.class.getSimpleName());

        // set bundle name
        connectorTO.setBundleName("org.connid.bundles.soap");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.setValue("http://localhost:8888/wssample/services");

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        servicename.setValue("Provisioning");

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        ConnInstanceTO actual =
                (ConnInstanceTO) restTemplate.postForObject(
                BASE_URL + "connector/update.json",
                connectorTO, ConnInstanceTO.class);

        assertNotNull(actual);

        actual = restTemplate.getForObject(
                BASE_URL + "connector/read/{connectorId}",
                ConnInstanceTO.class,
                actual.getId().toString());

        assertNotNull(actual);
        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
    }

    @Test
    public void deleteWithException() {
        try {
            restTemplate.delete(
                    BASE_URL + "connector/delete/{connectorId}.json", "0");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void list() {
        List<ConnInstanceTO> connectorInstanceTOs = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "connector/list.json", ConnInstanceTO[].class));
        assertNotNull(connectorInstanceTOs);
        assertFalse(connectorInstanceTOs.isEmpty());
        for (ConnInstanceTO instance : connectorInstanceTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void read() {
        ConnInstanceTO connectorInstanceTO = restTemplate.getForObject(
                BASE_URL + "connector/read/{connectorId}.json",
                ConnInstanceTO.class, "100");

        assertNotNull(connectorInstanceTO);
    }

    @Test
    public void check() {
        Boolean verify = restTemplate.getForObject(
                BASE_URL + "connector/check/{connectorId}.json",
                Boolean.class, 100L);

        assertTrue(verify);
    }

    @Test
    public void getBundles() {
        List<ConnBundleTO> bundles = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "connector/bundle/list",
                ConnBundleTO[].class));
        assertNotNull(bundles);
        assertFalse(bundles.isEmpty());
        for (ConnBundleTO bundle : bundles) {
            assertNotNull(bundle);
        }
    }
}
