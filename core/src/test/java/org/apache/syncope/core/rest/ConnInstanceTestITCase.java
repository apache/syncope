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
package org.apache.syncope.core.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.connid.bundles.soap.WebServiceConnector;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.databasetable.DatabaseTableConnector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.apache.syncope.client.to.ConnBundleTO;
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.client.to.ResourceTO;
import org.apache.syncope.client.to.SchemaMappingTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.types.ConnConfPropSchema;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.ConnectorCapability;
import org.apache.syncope.types.IntMappingType;

public class ConnInstanceTestITCase extends AbstractTest {

    private static String connidSoapVersion;

    private static String connidDbTableVersion;

    private static String bundlesDirectory;

    @BeforeClass
    public static void init() {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = ConnInstanceTestITCase.class.getResourceAsStream("/bundles.properties");
            props.load(propStream);
            connidSoapVersion = props.getProperty("connid.soap.version");
            connidDbTableVersion = props.getProperty("connid.db.table.version");
            bundlesDirectory = props.getProperty("bundles.directory");
        } catch (Exception e) {
            LOG.error("Could not load bundles.properties", e);
        } finally {
            if (propStream != null) {
                try {
                    propStream.close();
                } catch (IOException e) {
                    LOG.error("While reading bundles.properties", e);
                }
            }
        }
        assertNotNull(connidSoapVersion);
        assertNotNull(bundlesDirectory);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithException() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        restTemplate.postForObject(BASE_URL + "connector/create.json", connectorTO, ConnInstanceTO.class);
    }

    @Test
    public void create() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        // set connector version
        connectorTO.setVersion(connidSoapVersion);

        // set connector name
        connectorTO.setConnectorName(WebServiceConnector.class.getName());

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
        endpoint.setValues(Collections.singletonList("http://localhost:8888/wssample/services"));

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        servicename.setValues(Collections.singletonList("Provisioning"));

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        // set connector capabilities
        connectorTO.addCapability(ConnectorCapability.TWO_PHASES_CREATE);
        connectorTO.addCapability(ConnectorCapability.ONE_PHASE_CREATE);
        connectorTO.addCapability(ConnectorCapability.TWO_PHASES_UPDATE);

        ConnInstanceTO actual = restTemplate.postForObject(BASE_URL + "connector/create.json", connectorTO,
                ConnInstanceTO.class);

        assertNotNull(actual);

        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
        assertEquals("Display name", actual.getDisplayName());
        assertEquals(connectorTO.getCapabilities(), actual.getCapabilities());

        Throwable t = null;

        // check for the updating
        connectorTO.setId(actual.getId());

        connectorTO.removeCapability(ConnectorCapability.TWO_PHASES_UPDATE);
        actual = null;
        try {
            actual = restTemplate.postForObject(BASE_URL + "connector/update.json", connectorTO, ConnInstanceTO.class);
        } catch (HttpStatusCodeException e) {
            LOG.error("update failed", e);
            t = e;
        }

        assertNull(t);
        assertNotNull(actual);
        assertEquals(connectorTO.getCapabilities(), actual.getCapabilities());

        // check also for the deletion of the created object
        try {
            ConnInstanceTO deletedConn = restTemplate.getForObject(
                    BASE_URL + "connector/delete/{connectorId}.json",
                    ConnInstanceTO.class,
                    String.valueOf(actual.getId()));
            assertNotNull(deletedConn);
        } catch (HttpStatusCodeException e) {
            LOG.error("delete failed", e);
            t = e;
        }

        assertNull(t);

        // check the non existence
        try {
            restTemplate.getForObject(BASE_URL + "connector/read/{connectorId}", ConnInstanceTO.class,
                    String.valueOf(actual.getId()));
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void update() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        // set connector instance id
        connectorTO.setId(103L);

        // set connector version
        connectorTO.setVersion(connidSoapVersion);

        // set connector name
        connectorTO.setConnectorName(WebServiceConnector.class.getName());

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
        endpoint.setValues(Collections.singletonList("http://localhost:8888/wssample/services"));

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        servicename.setValues(Collections.singletonList("Provisioning"));

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        ConnInstanceTO actual = (ConnInstanceTO) restTemplate.postForObject(BASE_URL + "connector/update.json",
                connectorTO, ConnInstanceTO.class);

        assertNotNull(actual);

        actual = restTemplate.getForObject(BASE_URL + "connector/read/{connectorId}", ConnInstanceTO.class, String.
                valueOf(actual.getId()));

        assertNotNull(actual);
        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
    }

    @Test
    public void issueSYNCOPE10() {
        // ----------------------------------
        // Copy resource and connector in order to create new objects.
        // ----------------------------------
        // Retrieve a connector instance template.
        ConnInstanceTO connInstanceTO =
                restTemplate.getForObject(BASE_URL + "connector/read/{connectorId}", ConnInstanceTO.class, 103L);

        assertNotNull(connInstanceTO);

        // check for resource
        List<ResourceTO> resources = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "resource/list.json?connInstanceId=103", ResourceTO[].class));

        assertEquals(4, resources.size());

        // Retrieve a resource TO template.
        ResourceTO resourceTO = resources.get(0);

        // Make it new.
        resourceTO.setName("newAbout103");

        // Make it new.
        connInstanceTO.setId(0);
        connInstanceTO.setDisplayName("newDisplayName");
        // ----------------------------------

        // ----------------------------------
        // Create a new connector instance.
        // ----------------------------------
        connInstanceTO =
                restTemplate.postForObject(BASE_URL + "connector/create.json", connInstanceTO, ConnInstanceTO.class);

        assertNotNull(connInstanceTO);
        assertTrue(connInstanceTO.getCapabilities().isEmpty());

        long connId = connInstanceTO.getId();

        // Link resourceTO to the new connector instance.
        resourceTO.setConnectorId(connId);
        // ----------------------------------

        // ----------------------------------
        // Check for connector instance update after resource creation.
        // ----------------------------------
        resourceTO = restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO, ResourceTO.class);

        assertNotNull(resourceTO);

        resources = Arrays.asList(restTemplate.getForObject(BASE_URL + "resource/list.json?connInstanceId=" + connId,
                ResourceTO[].class));

        assertEquals(1, resources.size());
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean.
        // ----------------------------------
        ConnInstanceTO connInstanceBean = restTemplate.getForObject(
                BASE_URL + "connector/{resourceName}/connectorBean", ConnInstanceTO.class, resourceTO.getName());

        assertNotNull(connInstanceBean);
        assertTrue(connInstanceBean.getCapabilities().isEmpty());
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean update after connector instance update.
        // ----------------------------------
        connInstanceTO.addCapability(ConnectorCapability.SEARCH);

        ConnInstanceTO actual = (ConnInstanceTO) restTemplate.postForObject(BASE_URL + "connector/update.json",
                connInstanceTO, ConnInstanceTO.class);

        assertNotNull(actual);
        assertFalse(connInstanceTO.getCapabilities().isEmpty());

        // check for spring bean update
        connInstanceBean = restTemplate.getForObject(BASE_URL + "connector/{resourceName}/connectorBean",
                ConnInstanceTO.class, resourceTO.getName());

        assertFalse(connInstanceBean.getCapabilities().isEmpty());
        // ----------------------------------
    }

    @Test
    public void deleteWithException() {
        try {
            restTemplate.getForObject(
                    BASE_URL + "connector/delete/{connectorId}.json", ConnInstanceTO.class, "0");
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        List<ConnInstanceTO> connectorInstanceTOs = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "connector/list.json", ConnInstanceTO[].class));
        assertNotNull(connectorInstanceTOs);
        assertFalse(connectorInstanceTOs.isEmpty());
        for (ConnInstanceTO instance : connectorInstanceTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void read() {
        ConnInstanceTO connectorInstanceTO = restTemplate.getForObject(BASE_URL + "connector/read/{connectorId}.json",
                ConnInstanceTO.class, "100");

        assertNotNull(connectorInstanceTO);
    }

    @Test
    public void getBundles() {
        List<ConnBundleTO> bundles = Arrays.asList(restTemplate.getForObject(BASE_URL + "connector/bundle/list",
                ConnBundleTO[].class));
        assertNotNull(bundles);
        assertFalse(bundles.isEmpty());
        for (ConnBundleTO bundle : bundles) {
            assertNotNull(bundle);
        }
    }

    @Test
    public void getConnectorConfiguration() {
        List<ConnConfProperty> props = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "connector/{connectorId}/configurationProperty/list", ConnConfProperty[].class, 104));
        assertNotNull(props);
        assertFalse(props.isEmpty());
    }

    @Test
    public void checkHiddenProperty() {
        ConnInstanceTO connInstanceTO = restTemplate.getForObject(BASE_URL + "connector/read/{connectorId}.json",
                ConnInstanceTO.class, "100");

        boolean check = false;

        for (ConnConfProperty prop : connInstanceTO.getConfiguration()) {
            if ("receiveTimeout".equals(prop.getSchema().getName())) {
                check = true;
            }
        }
        assertTrue(check);
    }

    @Test
    public void checkSelectedLanguage() {
        // 1. Check Italian
        List<ConnInstanceTO> connectorInstanceTOs = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "connector/list.json?lang=it", ConnInstanceTO[].class));

        Map<String, ConnConfProperty> instanceConfMap;
        for (ConnInstanceTO instance : connectorInstanceTOs) {
            if ("org.connid.bundles.db.table".equals(instance.getBundleName())) {

                instanceConfMap = instance.getConfigurationMap();
                assertEquals("Utente", instanceConfMap.get("user").getSchema().getDisplayName());
            }
        }

        // 2. Check English (default)
        connectorInstanceTOs = Arrays.asList(restTemplate.getForObject(BASE_URL + "connector/list.json",
                ConnInstanceTO[].class));

        for (ConnInstanceTO instance : connectorInstanceTOs) {
            if ("org.connid.bundles.db.table".equals(instance.getBundleName())) {

                instanceConfMap = instance.getConfigurationMap();
                assertEquals("User", instanceConfMap.get("user").getSchema().getDisplayName());
            }
        }
    }

    @Test
    public void check() {

        ConnInstanceTO connectorTO = new ConnInstanceTO();

        // set connector version
        connectorTO.setVersion(connidDbTableVersion);

        // set connector name
        connectorTO.setConnectorName(DatabaseTableConnector.class.getName());

        // set bundle name
        connectorTO.setBundleName("org.connid.bundles.db.table");

        connectorTO.setDisplayName("H2Test");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema userSchema = new ConnConfPropSchema();
        userSchema.setName("user");
        userSchema.setType(String.class.getName());
        userSchema.setRequired(false);
        ConnConfProperty user = new ConnConfProperty();
        user.setSchema(userSchema);
        user.setValues(Collections.singletonList("sa"));

        ConnConfPropSchema keyColumnSchema = new ConnConfPropSchema();
        keyColumnSchema.setName("keyColumn");
        keyColumnSchema.setType(String.class.getName());
        keyColumnSchema.setRequired(true);
        ConnConfProperty keyColumn = new ConnConfProperty();
        keyColumn.setSchema(keyColumnSchema);
        keyColumn.setValues(Collections.singletonList("id"));

        ConnConfPropSchema jdbcUrlTemplateSchema = new ConnConfPropSchema();
        jdbcUrlTemplateSchema.setName("jdbcUrlTemplate");
        jdbcUrlTemplateSchema.setType(String.class.getName());
        jdbcUrlTemplateSchema.setRequired(true);
        ConnConfProperty jdbcUrlTemplate = new ConnConfProperty();
        jdbcUrlTemplate.setSchema(jdbcUrlTemplateSchema);
        jdbcUrlTemplate.setValues(Collections.singletonList("jdbc:h2:tcp://localhost:9092/testdb"));

        ConnConfPropSchema passwordColumnSchema = new ConnConfPropSchema();
        passwordColumnSchema.setName("passwordColumn");
        passwordColumnSchema.setType(String.class.getName());
        passwordColumnSchema.setRequired(true);
        ConnConfProperty passwordColumn = new ConnConfProperty();
        passwordColumn.setSchema(passwordColumnSchema);
        passwordColumn.setValues(Collections.singletonList("password"));

        ConnConfPropSchema tableSchema = new ConnConfPropSchema();
        tableSchema.setName("table");
        tableSchema.setType(String.class.getName());
        tableSchema.setRequired(true);
        ConnConfProperty table = new ConnConfProperty();
        table.setSchema(tableSchema);
        table.setValues(Collections.singletonList("test"));

        ConnConfPropSchema passwordSchema = new ConnConfPropSchema();
        passwordSchema.setName("password");
        passwordSchema.setType(GuardedString.class.getName());
        passwordSchema.setRequired(true);
        ConnConfProperty password = new ConnConfProperty();
        password.setSchema(passwordSchema);
        password.setValues(Collections.singletonList("sa"));

        ConnConfPropSchema jdbcDriverSchema = new ConnConfPropSchema();
        jdbcDriverSchema.setName("jdbcDriver");
        jdbcDriverSchema.setType(String.class.getName());
        jdbcDriverSchema.setRequired(true);
        ConnConfProperty jdbcDriver = new ConnConfProperty();
        jdbcDriver.setSchema(jdbcDriverSchema);
        jdbcDriver.setValues(Collections.singletonList("org.h2.Driver"));

        conf.add(user);
        conf.add(keyColumn);
        conf.add(jdbcUrlTemplate);
        conf.add(passwordColumn);
        conf.add(table);
        conf.add(password);
        conf.add(jdbcDriver);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        Boolean verify = restTemplate.postForObject(BASE_URL + "connector/check.json", connectorTO, Boolean.class);

        assertTrue(verify);

        conf.remove(password);
        password.setValues(Collections.singletonList("password"));
        conf.add(password);

        verify = restTemplate.postForObject(BASE_URL + "connector/check.json", connectorTO, Boolean.class);

        assertFalse(verify);
    }

    @Test
    public void getSchemaNames() {
        ConnInstanceTO conn = restTemplate.getForObject(BASE_URL + "connector/read/{connectorId}.json",
                ConnInstanceTO.class, "101");

        List<String> schemaNames = Arrays.asList(restTemplate.postForObject(BASE_URL
                + "connector/schema/list?showall=true", conn, String[].class));
        assertNotNull(schemaNames);
        assertFalse(schemaNames.isEmpty());

        schemaNames = Arrays.asList(restTemplate.postForObject(BASE_URL + "connector/schema/list", conn, String[].class));
        assertNotNull(schemaNames);
        assertEquals(0, schemaNames.size());

        conn = restTemplate.getForObject(BASE_URL + "connector/read/{connectorId}.json", ConnInstanceTO.class, "104");

        // to be used with overridden properties
        conn.getConfiguration().clear();

        schemaNames = Arrays.asList(restTemplate.postForObject(BASE_URL + "connector//schema/list?showall=true", conn,
                String[].class, conn));
        assertNotNull(schemaNames);
        assertFalse(schemaNames.isEmpty());
    }

    @Test
    public void issueSYNCOPE112() {

        // ----------------------------------------
        // Create a new connector
        // ----------------------------------------
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        // set connector version
        connectorTO.setVersion(connidSoapVersion);

        // set connector name
        connectorTO.setConnectorName(WebServiceConnector.class.getName());

        // set bundle name
        connectorTO.setBundleName("org.connid.bundles.soap");

        // set display name
        connectorTO.setDisplayName("WSSoap");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema userSchema = new ConnConfPropSchema();
        userSchema.setName("endpoint");
        userSchema.setType(String.class.getName());
        userSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(userSchema);
        endpoint.setValues(Collections.singletonList("http://localhost:9080/does_not_work"));
        endpoint.setOverridable(true);

        ConnConfPropSchema keyColumnSchema = new ConnConfPropSchema();
        keyColumnSchema.setName("servicename");
        keyColumnSchema.setType(String.class.getName());
        keyColumnSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(keyColumnSchema);
        servicename.setValues(
                Collections.singletonList("org.connid.bundles.soap.provisioning.interfaces.Provisioning"));
        servicename.setOverridable(false);

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        assertFalse(restTemplate.postForObject(BASE_URL + "connector/check.json", connectorTO, Boolean.class));

        connectorTO = restTemplate.postForObject(BASE_URL + "connector/create.json", connectorTO, ConnInstanceTO.class);
        assertNotNull(connectorTO);
        // ----------------------------------------

        // ----------------------------------------
        // create a resourceTO
        // ----------------------------------------
        String resourceName = "checkForPropOverriding";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(connectorTO.getId());

        conf = new HashSet<ConnConfProperty>();
        endpoint.setValues(Collections.singletonList("http://localhost:9080/wssample/services/provisioning"));
        conf.add(endpoint);

        resourceTO.setConnectorConfigurationProperties(conf);

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setExtAttrName("uid");
        schemaMappingTO.setIntAttrName("userId");
        schemaMappingTO.setIntMappingType(IntMappingType.UserSchema);
        schemaMappingTO.setAccountid(true);
        resourceTO.addMapping(schemaMappingTO);
        // ----------------------------------------

        // ----------------------------------------
        // Check connection without saving the resource ....
        // ----------------------------------------
        assertTrue(restTemplate.postForObject(BASE_URL + "resource/check.json", resourceTO, Boolean.class));
        // ----------------------------------------
    }
}
