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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnIdObjectClass;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ConnectorITCase extends AbstractITCase {

    private static String CONNECTOR_SERVER_LOCATION;

    private static String CONNID_SOAP_VERSION;

    private static String CONNID_DB_VERSION;

    private static String TEST_JDBC_URL;

    @BeforeAll
    public static void setUpConnIdBundles() throws IOException {
        try (InputStream propStream = ConnectorITCase.class.getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            CONNID_SOAP_VERSION = props.getProperty("connid.soap.version");
            CONNID_DB_VERSION = props.getProperty("connid.db.version");

            TEST_JDBC_URL = props.getProperty("testdb.url");
        } catch (Exception e) {
            LOG.error("Could not load /test.properties", e);
        }

        try (InputStream propStream = ConnectorITCase.class.getResourceAsStream("/core-embedded.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            for (String location : props.getProperty("provisioning.connIdLocation").split(",")) {
                if (!location.startsWith("file")) {
                    CONNECTOR_SERVER_LOCATION = location;
                }
            }
        } catch (Exception e) {
            LOG.error("Could not load /core-embedded.properties", e);
        }

        assertNotNull(CONNECTOR_SERVER_LOCATION);
        assertNotNull(CONNID_SOAP_VERSION);
        assertNotNull(CONNID_DB_VERSION);
        assertNotNull(TEST_JDBC_URL);
    }

    @Test
    public void createWithException() {
        assertThrows(SyncopeClientException.class, () -> {
            ConnInstanceTO connectorTO = new ConnInstanceTO();

            Response response = CONNECTOR_SERVICE.create(connectorTO);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            }
        });
    }

    @Test
    public void create() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();
        connectorTO.setAdminRealm(SyncopeConstants.ROOT_REALM);
        connectorTO.setLocation(CONNECTOR_SERVICE.read(
                "88a7a819-dab5-46b4-9b90-0b9769eabdb8", Locale.ENGLISH.getLanguage()).getLocation());
        connectorTO.setVersion(CONNID_SOAP_VERSION);
        connectorTO.setConnectorName("net.tirasa.connid.bundles.soap.WebServiceConnector");
        connectorTO.setBundleName("net.tirasa.connid.bundles.soap");
        connectorTO.setDisplayName("Display name");
        connectorTO.setConnRequestTimeout(15);

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.getValues().add("http://localhost:8888/syncope-fit-build-tools/cxf/soap");
        endpoint.getValues().add("Provisioning");
        conf.add(endpoint);

        ConnConfPropSchema servicenameSchema = new ConnConfPropSchema();
        servicenameSchema.setName("servicename");
        servicenameSchema.setType(String.class.getName());
        servicenameSchema.setRequired(true);
        ConnConfProperty servicename = new ConnConfProperty();
        servicename.setSchema(servicenameSchema);
        conf.add(servicename);

        // set connector configuration
        connectorTO.getConf().addAll(conf);

        // set connector capabilities
        connectorTO.getCapabilities().add(ConnectorCapability.CREATE);
        connectorTO.getCapabilities().add(ConnectorCapability.UPDATE);

        // set connector pool conf
        ConnPoolConf cpc = new ConnPoolConf();
        cpc.setMaxObjects(1534);
        connectorTO.setPoolConf(cpc);

        Response response = CONNECTOR_SERVICE.create(connectorTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
        }

        ConnInstanceTO actual = getObject(
                response.getLocation(), ConnectorService.class, ConnInstanceTO.class);
        assertNotNull(actual);

        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
        assertEquals("Display name", actual.getDisplayName());
        assertEquals(Integer.valueOf(15), actual.getConnRequestTimeout());
        assertEquals(connectorTO.getCapabilities(), actual.getCapabilities());
        assertNotNull(actual.getPoolConf());
        assertEquals(1534, actual.getPoolConf().getMaxObjects().intValue());
        assertEquals(10, actual.getPoolConf().getMaxIdle().intValue());

        Throwable t = null;

        // check update
        actual.getCapabilities().remove(ConnectorCapability.UPDATE);
        actual.getPoolConf().setMaxObjects(null);

        try {
            CONNECTOR_SERVICE.update(actual);
            actual = CONNECTOR_SERVICE.read(actual.getKey(), Locale.ENGLISH.getLanguage());
        } catch (SyncopeClientException e) {
            LOG.error("update failed", e);
            t = e;
        }

        assertNull(t);
        assertNotNull(actual);
        assertEquals(EnumSet.of(ConnectorCapability.CREATE), actual.getCapabilities());
        assertEquals(10, actual.getPoolConf().getMaxObjects().intValue());

        // check also for the deletion of the created object
        try {
            CONNECTOR_SERVICE.delete(actual.getKey());
        } catch (SyncopeClientException e) {
            LOG.error("delete failed", e);
            t = e;
        }

        assertNull(t);

        // check the non existence
        try {
            CONNECTOR_SERVICE.read(actual.getKey(), Locale.ENGLISH.getLanguage());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void update() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();
        connectorTO.setAdminRealm(SyncopeConstants.ROOT_REALM);

        // set connector instance key
        connectorTO.setKey("fcf9f2b0-f7d6-42c9-84a6-61b28255a42b");

        // set connector version
        connectorTO.setVersion(CONNID_SOAP_VERSION);

        // set connector name
        connectorTO.setConnectorName("net.tirasa.connid.bundles.soap.WebServiceConnector");

        // set bundle name
        connectorTO.setBundleName("net.tirasa.connid.bundles.soap");

        connectorTO.setConnRequestTimeout(20);

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<>();

        ConnConfPropSchema endpointSchema = new ConnConfPropSchema();
        endpointSchema.setName("endpoint");
        endpointSchema.setType(String.class.getName());
        endpointSchema.setRequired(true);
        ConnConfProperty endpoint = new ConnConfProperty();
        endpoint.setSchema(endpointSchema);
        endpoint.getValues().add("http://localhost:8888/syncope-fit-build-tools/cxf/soap");
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
        connectorTO.getConf().addAll(conf);

        CONNECTOR_SERVICE.update(connectorTO);
        ConnInstanceTO actual = CONNECTOR_SERVICE.read(connectorTO.getKey(), Locale.ENGLISH.getLanguage());

        assertNotNull(actual);

        actual = CONNECTOR_SERVICE.read(actual.getKey(), Locale.ENGLISH.getLanguage());

        assertNotNull(actual);
        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
        assertEquals(Integer.valueOf(20), actual.getConnRequestTimeout());
    }

    @Test
    public void reload() {
        CONNECTOR_SERVICE.reload();
    }

    @Test
    public void deleteWithException() {
        try {
            CONNECTOR_SERVICE.delete(UUID.randomUUID().toString());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        List<ConnInstanceTO> connInstances = CONNECTOR_SERVICE.list(null);
        assertNotNull(connInstances);
        assertFalse(connInstances.isEmpty());
        connInstances.forEach(Assertions::assertNotNull);
    }

    @Test
    public void read() {
        ConnInstanceTO connInstance = CONNECTOR_SERVICE.read(
                "88a7a819-dab5-46b4-9b90-0b9769eabdb8", Locale.ENGLISH.getLanguage());
        assertNotNull(connInstance);
        assertFalse(connInstance.isErrored());
        assertNotNull(connInstance.getLocation());
        assertFalse(connInstance.getConf().isEmpty());

        connInstance = CONNECTOR_SERVICE.read(
                "413bf072-678a-41d3-9d20-8c453b3a39d1", Locale.ENGLISH.getLanguage());
        assertNotNull(connInstance);
        assertTrue(connInstance.isErrored());
        assertNotNull(connInstance.getLocation());
        assertTrue(connInstance.getConf().isEmpty());
    }

    @Test
    public void getBundles() {
        List<ConnIdBundle> bundles = CONNECTOR_SERVICE.getBundles(Locale.ENGLISH.getLanguage());
        assertNotNull(bundles);
        assertFalse(bundles.isEmpty());
        bundles.forEach(Assertions::assertNotNull);
    }

    @Test
    public void getConnectorConfiguration() {
        List<ConnConfProperty> props = CONNECTOR_SERVICE.read(
                "6c2acf1b-b052-46f0-8c56-7a8ad6905edf", Locale.ENGLISH.getLanguage()).getConf();
        assertNotNull(props);
        assertFalse(props.isEmpty());
    }

    @Test
    public void checkHiddenProperty() {
        ConnInstanceTO connInstanceTO = CONNECTOR_SERVICE.read(
                "88a7a819-dab5-46b4-9b90-0b9769eabdb8", Locale.ENGLISH.getLanguage());

        boolean check = false;

        for (ConnConfProperty prop : connInstanceTO.getConf()) {
            if ("receiveTimeout".equals(prop.getSchema().getName())) {
                check = true;
            }
        }
        assertTrue(check);
    }

    @Test
    public void checkSelectedLanguage() {
        // 1. Check Italian
        assertTrue(CONNECTOR_SERVICE.list("it").stream().
                filter(i -> "net.tirasa.connid.bundles.db.table.DatabaseTableConnector".equals(i.getConnectorName())).
                allMatch(i -> "Utente".equals(i.getConf("user").get().getSchema().getDisplayName())));

        // 2. Check English (default)
        assertTrue(CONNECTOR_SERVICE.list(null).stream().
                filter(i -> "net.tirasa.connid.bundles.db.table.DatabaseTableConnector".equals(i.getConnectorName())).
                allMatch(i -> "User".equals(i.getConf("user").get().getSchema().getDisplayName())));
    }

    @Test
    public void validate() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();
        connectorTO.setAdminRealm(SyncopeConstants.ROOT_REALM);
        connectorTO.setLocation(CONNECTOR_SERVER_LOCATION);
        connectorTO.setVersion(CONNID_DB_VERSION);
        connectorTO.setConnectorName("net.tirasa.connid.bundles.db.table.DatabaseTableConnector");
        connectorTO.setBundleName("net.tirasa.connid.bundles.db");
        connectorTO.setDisplayName("H2Test");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<>();

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
        jdbcUrlTemplate.getValues().add(TEST_JDBC_URL);
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
        connectorTO.getConf().addAll(conf);

        try {
            CONNECTOR_SERVICE.check(connectorTO);
        } catch (Exception e) {
            fail(() -> ExceptionUtils.getStackTrace(e));
        }

        conf.remove(password);
        password.getValues().clear();
        password.getValues().add("password");
        conf.add(password);

        try {
            CONNECTOR_SERVICE.check(connectorTO);
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void buildObjectClassInfo() {
        ConnInstanceTO db = CONNECTOR_SERVICE.read(
                "5aa5b8be-7521-481a-9651-c557aea078c1", Locale.ENGLISH.getLanguage());
        assertNotNull(db);

        List<ConnIdObjectClass> objectClassInfo = CONNECTOR_SERVICE.buildObjectClassInfo(db, true);
        assertNotNull(objectClassInfo);
        assertEquals(1, objectClassInfo.size());
        assertEquals(ObjectClass.ACCOUNT_NAME, objectClassInfo.getFirst().getType());
        assertTrue(objectClassInfo.getFirst().getAttributes().stream().
            anyMatch(schema -> "ID".equals(schema.getKey())));

        ConnInstanceTO ldap = CONNECTOR_SERVICE.read(
                "74141a3b-0762-4720-a4aa-fc3e374ef3ef", Locale.ENGLISH.getLanguage());
        assertNotNull(ldap);

        objectClassInfo = CONNECTOR_SERVICE.buildObjectClassInfo(ldap, true);
        assertNotNull(objectClassInfo);

        Collection<String> objectClasses = objectClassInfo.stream().
                map(ConnIdObjectClass::getType).collect(Collectors.toSet());
        assertTrue(objectClasses.contains(ObjectClass.ACCOUNT_NAME));
        assertTrue(objectClasses.contains(ObjectClass.GROUP_NAME));
    }

    @Test
    public void authorizations() {
        SyncopeClient puccini = CLIENT_FACTORY.create("puccini", ADMIN_PWD);
        ConnectorService pcs = puccini.getService(ConnectorService.class);

        // 1. list connectors: get only the ones allowed
        List<ConnInstanceTO> connInstances = pcs.list(null);
        assertEquals(2, connInstances.size());

        assertTrue(connInstances.stream().allMatch(connInstance
                -> "a6d017fd-a705-4507-bb7c-6ab6a6745997".equals(connInstance.getKey())
                || "44c02549-19c3-483c-8025-4919c3283c37".equals(connInstance.getKey())));

        // 2. attempt to read a connector with a different admin realm: fail
        try {
            pcs.read("88a7a819-dab5-46b4-9b90-0b9769eabdb8", null);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        // 3. read and upate a connector in the realm for which entitlements are owned: succeed
        try {
            ConnInstanceTO scriptedsql = pcs.read("a6d017fd-a705-4507-bb7c-6ab6a6745997", null);
            ConnConfProperty reloadScriptOnExecution = scriptedsql.getConf("reloadScriptOnExecution").get();
            assertEquals("true", reloadScriptOnExecution.getValues().getFirst().toString());

            reloadScriptOnExecution.getValues().set(0, "false");
            pcs.update(scriptedsql);

            scriptedsql = pcs.read(scriptedsql.getKey(), null);
            reloadScriptOnExecution = scriptedsql.getConf("reloadScriptOnExecution").get();
            assertEquals("false", reloadScriptOnExecution.getValues().getFirst().toString());
        } finally {
            ConnInstanceTO scriptedsql = CONNECTOR_SERVICE.read("a6d017fd-a705-4507-bb7c-6ab6a6745997", null);
            ConnConfProperty reloadScriptOnExecution = scriptedsql.getConf("reloadScriptOnExecution").get();
            reloadScriptOnExecution.getValues().set(0, "true");
            CONNECTOR_SERVICE.update(scriptedsql);
        }
    }

    @Test
    public void issueSYNCOPE10() {
        // ----------------------------------
        // Copy resource and connector in order to create new objects.
        // ----------------------------------
        // Retrieve a connector instance template.
        ConnInstanceTO connInstanceTO = CONNECTOR_SERVICE.read(
                "fcf9f2b0-f7d6-42c9-84a6-61b28255a42b", Locale.ENGLISH.getLanguage());
        assertNotNull(connInstanceTO);

        // check for resource
        List<ResourceTO> resources = RESOURCE_SERVICE.list().stream().
                filter(resource -> "fcf9f2b0-f7d6-42c9-84a6-61b28255a42b".equals(resource.getConnector())).
                toList();
        assertEquals(4, resources.size());

        // Retrieve a resource TO template.
        ResourceTO resourceTO = resources.getFirst();

        // Make it new.
        resourceTO.setKey("newAbout103" + getUUIDString());

        // Make it new.
        connInstanceTO.setKey(null);
        connInstanceTO.setDisplayName("newDisplayName" + getUUIDString());
        // ----------------------------------

        // ----------------------------------
        // Create a new connector instance.
        // ----------------------------------
        Response response = CONNECTOR_SERVICE.create(connInstanceTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
        }

        connInstanceTO = getObject(response.getLocation(), ConnectorService.class, ConnInstanceTO.class);
        assertNotNull(connInstanceTO);
        assertFalse(connInstanceTO.getCapabilities().contains(ConnectorCapability.AUTHENTICATE));

        final String connKey = connInstanceTO.getKey();

        // Link resourceTO to the new connector instance.
        resourceTO.setConnector(connKey);
        // ----------------------------------

        // ----------------------------------
        // Check for connector instance update after resource creation.
        // ----------------------------------
        response = RESOURCE_SERVICE.create(resourceTO);
        resourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(resourceTO);

        resources = RESOURCE_SERVICE.list().stream().
                filter(resource -> connKey.equals(resource.getConnector())).toList();
        assertEquals(1, resources.size());
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean.
        // ----------------------------------
        ConnInstanceTO connInstanceBean = CONNECTOR_SERVICE.readByResource(
                resourceTO.getKey(), Locale.ENGLISH.getLanguage());
        assertNotNull(connInstanceBean);
        assertFalse(connInstanceBean.getCapabilities().contains(ConnectorCapability.AUTHENTICATE));
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean update after connector instance update.
        // ----------------------------------
        connInstanceTO.getCapabilities().add(ConnectorCapability.AUTHENTICATE);

        CONNECTOR_SERVICE.update(connInstanceTO);
        ConnInstanceTO actual = CONNECTOR_SERVICE.read(connInstanceTO.getKey(), Locale.ENGLISH.getLanguage());
        assertNotNull(actual);
        assertTrue(connInstanceTO.getCapabilities().contains(ConnectorCapability.AUTHENTICATE));

        // check for spring bean update
        connInstanceBean = CONNECTOR_SERVICE.readByResource(resourceTO.getKey(), Locale.ENGLISH.getLanguage());
        assertTrue(connInstanceBean.getCapabilities().contains(ConnectorCapability.AUTHENTICATE));
        // ----------------------------------
    }

    @Test
    public void issueSYNCOPE112() {
        // ----------------------------------------
        // Create a new connector
        // ----------------------------------------
        ConnInstanceTO connectorTO = new ConnInstanceTO();
        connectorTO.setAdminRealm(SyncopeConstants.ROOT_REALM);

        connectorTO.setLocation(CONNECTOR_SERVICE.read(
                "88a7a819-dab5-46b4-9b90-0b9769eabdb8", Locale.ENGLISH.getLanguage()).getLocation());

        // set connector version
        connectorTO.setVersion(CONNID_SOAP_VERSION);

        // set connector name
        connectorTO.setConnectorName("net.tirasa.connid.bundles.soap.WebServiceConnector");

        // set bundle name
        connectorTO.setBundleName("net.tirasa.connid.bundles.soap");

        // set display name
        connectorTO.setDisplayName("WSSoap");

        // set the connector configuration using PropertyTO
        List<ConnConfProperty> conf = new ArrayList<>();

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
        servicename.getValues().add("net.tirasa.connid.bundles.soap.provisioning.interfaces.Provisioning");
        servicename.setOverridable(false);

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.getConf().addAll(conf);

        try {
            try {
                CONNECTOR_SERVICE.check(connectorTO);
                fail("This should not happen");
            } catch (Exception e) {
                assertNotNull(e);
            }

            Response response = CONNECTOR_SERVICE.create(connectorTO);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            }

            connectorTO = getObject(response.getLocation(), ConnectorService.class, ConnInstanceTO.class);
            assertNotNull(connectorTO);
            // ----------------------------------------

            // ----------------------------------------
            // create a resourceTO
            // ----------------------------------------
            String resourceName = "checkForPropOverriding";
            ResourceTO resourceTO = new ResourceTO();

            resourceTO.setKey(resourceName);
            resourceTO.setConnector(connectorTO.getKey());

            conf = new ArrayList<>();
            endpoint.getValues().clear();
            endpoint.getValues().add(BUILD_TOOLS_ADDRESS + "/soap/provisioning");
            conf.add(endpoint);

            resourceTO.setConfOverride(Optional.of(conf));

            Provision provisionTO = new Provision();
            provisionTO.setAnyType(AnyTypeKind.USER.name());
            provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resourceTO.getProvisions().add(provisionTO);

            Mapping mapping = new Mapping();
            provisionTO.setMapping(mapping);

            Item mapItem = new Item();
            mapItem.setExtAttrName("uid");
            mapItem.setIntAttrName("userId");
            mapItem.setConnObjectKey(true);
            mapping.setConnObjectKeyItem(mapItem);
            // ----------------------------------------

            // ----------------------------------------
            // Check connection without saving the resource ....
            // ----------------------------------------
            try {
                RESOURCE_SERVICE.check(resourceTO);
            } catch (Exception e) {
                fail(() -> ExceptionUtils.getStackTrace(e));
            }
            // ----------------------------------------
        } finally {
            // Remove connector from db to make test re-runnable
            CONNECTOR_SERVICE.delete(connectorTO.getKey());
        }
    }

    @Test
    public void issueSYNCOPE605() {
        ConnInstanceTO connInstance = CONNECTOR_SERVICE.read(
                "fcf9f2b0-f7d6-42c9-84a6-61b28255a42b", Locale.ENGLISH.getLanguage());
        assertTrue(connInstance.getCapabilities().isEmpty());

        connInstance.getCapabilities().add(ConnectorCapability.SEARCH);
        CONNECTOR_SERVICE.update(connInstance);

        ConnInstanceTO updatedCapabilities = CONNECTOR_SERVICE.read(
                connInstance.getKey(), Locale.ENGLISH.getLanguage());
        assertNotNull(updatedCapabilities.getCapabilities());
        assertTrue(updatedCapabilities.getCapabilities().size() == 1);
    }
}
