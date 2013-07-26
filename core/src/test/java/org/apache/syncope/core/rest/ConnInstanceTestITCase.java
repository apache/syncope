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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnIdObjectClassTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ConnectorCapability;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.core.util.ConnIdBundleManager;
import org.identityconnectors.common.security.GuardedString;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@FixMethodOrder(MethodSorters.JVM)
public class ConnInstanceTestITCase extends AbstractTest {

    private static String connidSoapVersion;

    private static String connidDbTableVersion;

    @BeforeClass
    public static void setUpConnIdBundles() throws IOException {
        InputStream propStream = null;
        try {
            Properties props = new Properties();
            propStream = ConnInstanceTestITCase.class.getResourceAsStream(ConnIdBundleManager.CONNID_PROPS);
            props.load(propStream);

            connidSoapVersion = props.getProperty("connid.soap.version");
            connidDbTableVersion = props.getProperty("connid.db.table.version");
        } catch (Exception e) {
            LOG.error("Could not load {}", ConnIdBundleManager.CONNID_PROPS, e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
        assertNotNull(connidSoapVersion);
        assertNotNull(connidDbTableVersion);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithException() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        Response response = connectorService.create(connectorTO);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            throw (RuntimeException) clientExceptionMapper.fromResponse(response);
        }
    }

    @Test
    public void create() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        connectorTO.setLocation(connectorService.read(100L).getLocation());

        // set connector version
        connectorTO.setVersion(connidSoapVersion);

        // set connector name
        connectorTO.setConnectorName("org.connid.bundles.soap.WebServiceConnector");

        // set bundle name
        connectorTO.setBundleName("org.connid.bundles.soap");

        connectorTO.setDisplayName("Display name");

        connectorTO.setConnRequestTimeout(15);

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.getValues().add("http://localhost:8888/wssample/services");

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        endpoint.getValues().add("Provisioning");

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.getConfiguration().addAll(conf);

        // set connector capabilities
        connectorTO.getCapabilities().add(ConnectorCapability.TWO_PHASES_CREATE);
        connectorTO.getCapabilities().add(ConnectorCapability.ONE_PHASE_CREATE);
        connectorTO.getCapabilities().add(ConnectorCapability.TWO_PHASES_UPDATE);

        Response response = connectorService.create(connectorTO);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            throw (RuntimeException) clientExceptionMapper.fromResponse(response);
        }

        ConnInstanceTO actual = getObject(response, ConnInstanceTO.class, connectorService);

        assertNotNull(actual);

        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
        assertEquals("Display name", actual.getDisplayName());
        assertEquals(Integer.valueOf(15), actual.getConnRequestTimeout());
        assertEquals(connectorTO.getCapabilities(), actual.getCapabilities());

        Throwable t = null;

        // check for the updating
        connectorTO.setId(actual.getId());

        connectorTO.getCapabilities().remove(ConnectorCapability.TWO_PHASES_UPDATE);
        actual = null;
        try {
            connectorService.update(connectorTO.getId(), connectorTO);
            actual = connectorService.read(connectorTO.getId());
        } catch (HttpStatusCodeException e) {
            LOG.error("update failed", e);
            t = e;
        }

        assertNull(t);
        assertNotNull(actual);
        assertEquals(connectorTO.getCapabilities(), actual.getCapabilities());

        // check also for the deletion of the created object
        try {
            connectorService.delete(actual.getId());
        } catch (HttpStatusCodeException e) {
            LOG.error("delete failed", e);
            t = e;
        }

        assertNull(t);

        // check the non existence
        try {
            connectorService.read(actual.getId());
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
        connectorTO.setConnectorName("org.connid.bundles.soap.WebServiceConnector");

        // set bundle name
        connectorTO.setBundleName("org.connid.bundles.soap");

        connectorTO.setConnRequestTimeout(20);

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.getValues().add("http://localhost:8888/wssample/services");
        conf.add(endpoint);

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        servicename.getValues().add("Provisioning");
        conf.add(servicename);

        // set connector configuration
        connectorTO.getConfiguration().addAll(conf);

        connectorService.update(connectorTO.getId(), connectorTO);
        ConnInstanceTO actual = connectorService.read(connectorTO.getId());

        assertNotNull(actual);

        actual = connectorService.read(actual.getId());

        assertNotNull(actual);
        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
        assertEquals(Integer.valueOf(20), actual.getConnRequestTimeout());
    }

    @Test
    public void issueSYNCOPE10() {
        // ----------------------------------
        // Copy resource and connector in order to create new objects.
        // ----------------------------------
        // Retrieve a connector instance template.
        ConnInstanceTO connInstanceTO = connectorService.read(103L);

        assertNotNull(connInstanceTO);

        // check for resource
        List<ResourceTO> resources = resourceService.list(Long.valueOf(103));

        assertEquals(4, resources.size());

        // Retrieve a resource TO template.
        ResourceTO resourceTO = resources.get(0);

        // Make it new.
        resourceTO.setName("newAbout103");

        // Make it new.
        connInstanceTO.setId(0);
        connInstanceTO.setDisplayName("newDisplayName" + getUUIDString());
        // ----------------------------------

        // ----------------------------------
        // Create a new connector instance.
        // ----------------------------------
        Response response = connectorService.create(connInstanceTO);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            throw (RuntimeException) clientExceptionMapper.fromResponse(response);
        }

        connInstanceTO = getObject(response, ConnInstanceTO.class, connectorService);

        assertNotNull(connInstanceTO);
        assertTrue(connInstanceTO.getCapabilities().isEmpty());

        long connId = connInstanceTO.getId();

        // Link resourceTO to the new connector instance.
        resourceTO.setConnectorId(connId);
        // ----------------------------------

        // ----------------------------------
        // Check for connector instance update after resource creation.
        // ----------------------------------
        response = resourceService.create(resourceTO);
        resourceTO = getObject(response, ResourceTO.class, resourceService);

        assertNotNull(resourceTO);

        resources = resourceService.list(connId);

        assertEquals(1, resources.size());
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean.
        // ----------------------------------
        ConnInstanceTO connInstanceBean = connectorService.readByResource(resourceTO.getName());

        assertNotNull(connInstanceBean);
        assertTrue(connInstanceBean.getCapabilities().isEmpty());
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean update after connector instance update.
        // ----------------------------------
        connInstanceTO.getCapabilities().add(ConnectorCapability.SEARCH);

        connectorService.update(connInstanceTO.getId(), connInstanceTO);
        ConnInstanceTO actual = connectorService.read(connInstanceTO.getId());

        assertNotNull(actual);
        assertFalse(connInstanceTO.getCapabilities().isEmpty());

        // check for spring bean update
        connInstanceBean = connectorService.readByResource(resourceTO.getName());

        assertFalse(connInstanceBean.getCapabilities().isEmpty());
        // ----------------------------------
    }

    @Test
    public void deleteWithException() {
        try {
            connectorService.delete(0L);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        List<ConnInstanceTO> connectorInstanceTOs = connectorService.list(null);
        assertNotNull(connectorInstanceTOs);
        assertFalse(connectorInstanceTOs.isEmpty());
        for (ConnInstanceTO instance : connectorInstanceTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void read() {
        ConnInstanceTO connectorInstanceTO = connectorService.read(100L);
        assertNotNull(connectorInstanceTO);
    }

    @Test
    public void getBundles() {
        List<ConnBundleTO> bundles = connectorService.getBundles(null);
        assertNotNull(bundles);
        assertFalse(bundles.isEmpty());
        for (ConnBundleTO bundle : bundles) {
            assertNotNull(bundle);
        }
    }

    @Test
    public void getConnectorConfiguration() {
        List<ConnConfProperty> props = connectorService.getConfigurationProperties(104L);
        assertNotNull(props);
        assertFalse(props.isEmpty());
    }

    @Test
    public void checkHiddenProperty() {
        ConnInstanceTO connInstanceTO = connectorService.read(100L);

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
        List<ConnInstanceTO> connectorInstanceTOs = connectorService.list("it");

        Map<String, ConnConfProperty> instanceConfMap;
        for (ConnInstanceTO instance : connectorInstanceTOs) {
            if ("org.connid.bundles.db.table".equals(instance.getBundleName())) {

                instanceConfMap = instance.getConfigurationMap();
                assertEquals("Utente", instanceConfMap.get("user").getSchema().getDisplayName());
            }
        }

        // 2. Check English (default)
        connectorInstanceTOs = connectorService.list(null);

        for (ConnInstanceTO instance : connectorInstanceTOs) {
            if ("org.connid.bundles.db.table".equals(instance.getBundleName())) {

                instanceConfMap = instance.getConfigurationMap();
                assertEquals("User", instanceConfMap.get("user").getSchema().getDisplayName());
            }
        }
    }

    @Test
    public void validate() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        for (URI location : ConnIdBundleManager.getConnManagers().keySet()) {
            if (!location.getScheme().equals("file")) {
                connectorTO.setLocation(location.toString());
            }
        }

        // set connector version
        connectorTO.setVersion(connidDbTableVersion);

        // set connector name
        connectorTO.setConnectorName("org.connid.bundles.db.table.DatabaseTableConnector");

        // set bundle name
        connectorTO.setBundleName("org.connid.bundles.db.table");

        connectorTO.setDisplayName("H2Test");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        ConnConfPropSchema jdbcDriverSchema = new ConnConfPropSchema();
        jdbcDriverSchema.setName("jdbcDriver");
        jdbcDriverSchema.setType(String.class.getName());
        jdbcDriverSchema.setRequired(true);
        ConnConfProperty jdbcDriver = new ConnConfProperty();
        jdbcDriver.setSchema(jdbcDriverSchema);
        jdbcDriver.getValues().add("org.h2.Driver");
        conf.add(jdbcDriver);

        ConnConfPropSchema jdbcUrlTemplateSchema = new ConnConfPropSchema();
        jdbcUrlTemplateSchema.setName("jdbcUrlTemplate");
        jdbcUrlTemplateSchema.setType(String.class.getName());
        jdbcUrlTemplateSchema.setRequired(true);
        ConnConfProperty jdbcUrlTemplate = new ConnConfProperty();
        jdbcUrlTemplate.setSchema(jdbcUrlTemplateSchema);
        jdbcUrlTemplate.getValues().add("jdbc:h2:tcp://localhost:9092/testdb");
        conf.add(jdbcUrlTemplate);

        ConnConfPropSchema userSchema = new ConnConfPropSchema();
        userSchema.setName("user");
        userSchema.setType(String.class.getName());
        userSchema.setRequired(false);
        ConnConfProperty user = new ConnConfProperty();
        user.setSchema(userSchema);
        user.getValues().add("sa");
        conf.add(user);

        ConnConfPropSchema passwordSchema = new ConnConfPropSchema();
        passwordSchema.setName("password");
        passwordSchema.setType(GuardedString.class.getName());
        passwordSchema.setRequired(true);
        ConnConfProperty password = new ConnConfProperty();
        password.setSchema(passwordSchema);
        password.getValues().add("sa");
        conf.add(password);
        
        ConnConfPropSchema tableSchema = new ConnConfPropSchema();
        tableSchema.setName("table");
        tableSchema.setType(String.class.getName());
        tableSchema.setRequired(true);
        ConnConfProperty table = new ConnConfProperty();
        table.setSchema(tableSchema);
        table.getValues().add("test");
        conf.add(table);

        ConnConfPropSchema keyColumnSchema = new ConnConfPropSchema();
        keyColumnSchema.setName("keyColumn");
        keyColumnSchema.setType(String.class.getName());
        keyColumnSchema.setRequired(true);
        ConnConfProperty keyColumn = new ConnConfProperty();
        keyColumn.setSchema(keyColumnSchema);
        keyColumn.getValues().add("id");
        conf.add(keyColumn);

        ConnConfPropSchema passwordColumnSchema = new ConnConfPropSchema();
        passwordColumnSchema.setName("passwordColumn");
        passwordColumnSchema.setType(String.class.getName());
        passwordColumnSchema.setRequired(true);
        ConnConfProperty passwordColumn = new ConnConfProperty();
        passwordColumn.setSchema(passwordColumnSchema);
        passwordColumn.getValues().add("password");
        conf.add(passwordColumn);

        // set connector configuration
        connectorTO.getConfiguration().addAll(conf);

        assertTrue(connectorService.check(connectorTO));

        conf.remove(password);
        password.getValues().clear();
        password.getValues().add("password");
        conf.add(password);

        assertFalse(connectorService.check(connectorTO));
    }

    @Test
    public void getSchemaNames() {
        ConnInstanceTO conn = connectorService.read(101L);

        List<SchemaTO> schemaNames = connectorService.getSchemaNames(conn.getId(), conn, true);
        assertNotNull(schemaNames);
        assertFalse(schemaNames.isEmpty());
        assertNotNull(schemaNames.get(0).getName());
        assertNull(schemaNames.get(0).getEnumerationValues());

        schemaNames = connectorService.getSchemaNames(conn.getId(), conn, false);

        assertNotNull(schemaNames);
        assertEquals(0, schemaNames.size());

        conn = connectorService.read(104L);

        // to be used with overridden properties
        conn.getConfiguration().clear();

        schemaNames = connectorService.getSchemaNames(conn.getId(), conn, true);
        assertNotNull(schemaNames);
        assertFalse(schemaNames.isEmpty());
    }

    @Test
    public void getSupportedObjectClasses() {
        ConnInstanceTO ldap = connectorService.read(105L);
        assertNotNull(ldap);

        List<ConnIdObjectClassTO> objectClasses = connectorService.getSupportedObjectClasses(ldap.getId(), ldap);
        assertNotNull(objectClasses);
        assertEquals(2, objectClasses.size());
        assertTrue(objectClasses.contains(ConnIdObjectClassTO.ACCOUNT));
        assertTrue(objectClasses.contains(ConnIdObjectClassTO.GROUP));

        ConnInstanceTO csv = connectorService.read(104L);
        assertNotNull(csv);

        objectClasses = connectorService.getSupportedObjectClasses(csv.getId(), csv);
        assertNotNull(objectClasses);
        assertEquals(1, objectClasses.size());
        assertTrue(objectClasses.contains(ConnIdObjectClassTO.ACCOUNT));
    }

    @Test
    public void issueSYNCOPE112() {
        // ----------------------------------------
        // Create a new connector
        // ----------------------------------------
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        connectorTO.setLocation(connectorService.read(100L).getLocation());

        // set connector version
        connectorTO.setVersion(connidSoapVersion);

        // set connector name
        connectorTO.setConnectorName("org.connid.bundles.soap.WebServiceConnector");

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
        endpoint.getValues().add("http://localhost:9080/does_not_work");
        endpoint.setOverridable(true);

        ConnConfPropSchema keyColumnSchema = new ConnConfPropSchema();
        keyColumnSchema.setName("servicename");
        keyColumnSchema.setType(String.class.getName());
        keyColumnSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(keyColumnSchema);
        servicename.getValues().add("org.connid.bundles.soap.provisioning.interfaces.Provisioning");
        servicename.setOverridable(false);

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.getConfiguration().addAll(conf);

        try {
            assertFalse(connectorService.check(connectorTO));

            Response response = connectorService.create(connectorTO);
            if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
                throw (RuntimeException) clientExceptionMapper.fromResponse(response);
            }

            connectorTO = getObject(response, ConnInstanceTO.class, connectorService);
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
            endpoint.getValues().clear();
            endpoint.getValues().add("http://localhost:9080/wssample/services/provisioning");
            conf.add(endpoint);

            resourceTO.getConnConfProperties().addAll(conf);

            MappingTO mapping = new MappingTO();
            resourceTO.setUmapping(mapping);

            MappingItemTO mapItem = new MappingItemTO();
            mapItem.setExtAttrName("uid");
            mapItem.setIntAttrName("userId");
            mapItem.setIntMappingType(IntMappingType.UserSchema);
            mapItem.setAccountid(true);
            mapping.setAccountIdItem(mapItem);
            // ----------------------------------------

            // ----------------------------------------
            // Check connection without saving the resource ....
            // ----------------------------------------
            assertTrue(resourceService.check(resourceTO));
            // ----------------------------------------
        } finally {
            // Remove connector from db to make test re-runnable
            connectorService.delete(connectorTO.getId());
        }
    }

    @Test
    public void reload() {
        connectorService.reload();
    }

    @Test
    public void bulkAction() {
        final BulkAction bulkAction = new BulkAction();
        bulkAction.setOperation(BulkAction.Type.DELETE);

        ConnInstanceTO conn = connectorService.read(101L);

        conn.setId(0);
        conn.setDisplayName("forBulk1");

        bulkAction.addTarget(String.valueOf(
                getObject(connectorService.create(conn), ConnInstanceTO.class, connectorService).getId()));

        conn.setDisplayName("forBulk2");

        bulkAction.addTarget(String.valueOf(
                getObject(connectorService.create(conn), ConnInstanceTO.class, connectorService).getId()));


        Iterator<String> iter = bulkAction.getTargets().iterator();

        assertNotNull(connectorService.read(Long.valueOf(iter.next())));
        assertNotNull(connectorService.read(Long.valueOf(iter.next())));

        connectorService.bulkAction(bulkAction);

        iter = bulkAction.getTargets().iterator();

        try {
            connectorService.read(Long.valueOf(iter.next()));
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
        }

        try {
            connectorService.read(Long.valueOf(iter.next()));
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
        }
    }
}
