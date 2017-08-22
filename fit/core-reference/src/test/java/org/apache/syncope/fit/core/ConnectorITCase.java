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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceHistoryConfTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConnectorITCase extends AbstractITCase {

    private static String connectorServerLocation;

    private static String connIdSoapVersion;

    private static String connIdDbVersion;

    private static String testJDBCURL;

    @BeforeClass
    public static void setUpConnIdBundles() throws IOException {
        InputStream propStream = null;
        try {
            Properties props = new Properties();
            propStream = ConnectorITCase.class.getResourceAsStream("/connid.properties");
            props.load(propStream);

            for (String location : props.getProperty("connid.locations").split(",")) {
                if (!location.startsWith("file")) {
                    connectorServerLocation = location;
                }
            }

            connIdSoapVersion = props.getProperty("connid.soap.version");
            connIdDbVersion = props.getProperty("connid.database.version");

            testJDBCURL = props.getProperty("testdb.url");
        } catch (Exception e) {
            LOG.error("Could not load /connid.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }
        assertNotNull(connectorServerLocation);
        assertNotNull(connIdSoapVersion);
        assertNotNull(connIdDbVersion);
        assertNotNull(testJDBCURL);
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithException() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();

        Response response = connectorService.create(connectorTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
        }
    }

    @Test
    public void create() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();
        connectorTO.setAdminRealm(SyncopeConstants.ROOT_REALM);
        connectorTO.setLocation(connectorService.read(
                "88a7a819-dab5-46b4-9b90-0b9769eabdb8", Locale.ENGLISH.getLanguage()).getLocation());
        connectorTO.setVersion(connIdSoapVersion);
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
        ConnPoolConfTO cpc = new ConnPoolConfTO();
        cpc.setMaxObjects(1534);
        connectorTO.setPoolConf(cpc);

        Response response = connectorService.create(connectorTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
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
        assertEquals(1534, actual.getPoolConf().getMaxObjects(), 0);
        assertEquals(10, actual.getPoolConf().getMaxIdle(), 0);

        Throwable t = null;

        // check update
        actual.getCapabilities().remove(ConnectorCapability.UPDATE);
        actual.getPoolConf().setMaxObjects(null);

        try {
            connectorService.update(actual);
            actual = connectorService.read(actual.getKey(), Locale.ENGLISH.getLanguage());
        } catch (SyncopeClientException e) {
            LOG.error("update failed", e);
            t = e;
        }

        assertNull(t);
        assertNotNull(actual);
        assertEquals(EnumSet.of(ConnectorCapability.CREATE), actual.getCapabilities());
        assertEquals(10, actual.getPoolConf().getMaxObjects(), 0);

        // check also for the deletion of the created object
        try {
            connectorService.delete(actual.getKey());
        } catch (SyncopeClientException e) {
            LOG.error("delete failed", e);
            t = e;
        }

        assertNull(t);

        // check the non existence
        try {
            connectorService.read(actual.getKey(), Locale.ENGLISH.getLanguage());
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
        connectorTO.setVersion(connIdSoapVersion);

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

        connectorService.update(connectorTO);
        ConnInstanceTO actual = connectorService.read(connectorTO.getKey(), Locale.ENGLISH.getLanguage());

        assertNotNull(actual);

        actual = connectorService.read(actual.getKey(), Locale.ENGLISH.getLanguage());

        assertNotNull(actual);
        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
        assertEquals(Integer.valueOf(20), actual.getConnRequestTimeout());
    }

    @Test
    public void reload() {
        connectorService.reload();
    }

    @Test
    public void deleteWithException() {
        try {
            connectorService.delete(UUID.randomUUID().toString());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        List<ConnInstanceTO> connectorInstanceTOs = connectorService.list(null);
        assertNotNull(connectorInstanceTOs);
        assertFalse(connectorInstanceTOs.isEmpty());
        connectorInstanceTOs.forEach(instance -> assertNotNull(instance));
    }

    @Test
    public void read() {
        ConnInstanceTO connectorInstanceTO = connectorService.read(
                "88a7a819-dab5-46b4-9b90-0b9769eabdb8", Locale.ENGLISH.getLanguage());
        assertNotNull(connectorInstanceTO);
    }

    @Test
    public void getBundles() {
        List<ConnBundleTO> bundles = connectorService.getBundles(Locale.ENGLISH.getLanguage());
        assertNotNull(bundles);
        assertFalse(bundles.isEmpty());
        bundles.forEach(bundle -> assertNotNull(bundle));
    }

    @Test
    public void getConnectorConfiguration() {
        List<ConnConfProperty> props = connectorService.read(
                "6c2acf1b-b052-46f0-8c56-7a8ad6905edf", Locale.ENGLISH.getLanguage()).getConf();
        assertNotNull(props);
        assertFalse(props.isEmpty());
    }

    @Test
    public void checkHiddenProperty() {
        ConnInstanceTO connInstanceTO = connectorService.read(
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
        List<ConnInstanceTO> connectorInstanceTOs = connectorService.list("it");

        for (ConnInstanceTO instance : connectorInstanceTOs) {
            if ("net.tirasa.connid.bundles.db.table".equals(instance.getBundleName())) {
                assertEquals("Utente", instance.getConf("user").get().getSchema().getDisplayName());
            }
        }

        // 2. Check English (default)
        connectorInstanceTOs = connectorService.list(null);

        for (ConnInstanceTO instance : connectorInstanceTOs) {
            if ("net.tirasa.connid.bundles.db.table".equals(instance.getBundleName())) {
                assertEquals("User", instance.getConf("user").get().getSchema().getDisplayName());
            }
        }
    }

    @Test
    public void validate() {
        ConnInstanceTO connectorTO = new ConnInstanceTO();
        connectorTO.setAdminRealm(SyncopeConstants.ROOT_REALM);
        connectorTO.setLocation(connectorServerLocation);
        connectorTO.setVersion(connIdDbVersion);
        connectorTO.setConnectorName("net.tirasa.connid.bundles.db.table.DatabaseTableConnector");
        connectorTO.setBundleName("net.tirasa.connid.bundles.db.table");
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
        jdbcUrlTemplate.getValues().add(testJDBCURL);
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
            connectorService.check(connectorTO);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }

        conf.remove(password);
        password.getValues().clear();
        password.getValues().add("password");
        conf.add(password);

        try {
            connectorService.check(connectorTO);
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void buildObjectClassInfo() {
        ConnInstanceTO ws = connectorService.read(
                "5ffbb4ac-a8c3-4b44-b699-11b398a1ba08", Locale.ENGLISH.getLanguage());
        assertNotNull(ws);

        List<ConnIdObjectClassTO> objectClassInfo = connectorService.buildObjectClassInfo(ws, true);
        assertNotNull(objectClassInfo);
        assertEquals(1, objectClassInfo.size());
        assertEquals(ObjectClass.ACCOUNT_NAME, objectClassInfo.get(0).getType());
        assertTrue(objectClassInfo.get(0).getAttributes().contains("promoThirdPartyDisclaimer"));

        ConnInstanceTO ldap = connectorService.read(
                "74141a3b-0762-4720-a4aa-fc3e374ef3ef", Locale.ENGLISH.getLanguage());
        assertNotNull(ldap);

        objectClassInfo = connectorService.buildObjectClassInfo(ldap, true);
        assertNotNull(objectClassInfo);

        Collection<String> objectClasses = objectClassInfo.stream().
                map(info -> info.getType()).collect(Collectors.toSet());
        assertTrue(objectClasses.contains(ObjectClass.ACCOUNT_NAME));
        assertTrue(objectClasses.contains(ObjectClass.GROUP_NAME));
    }

    @Test
    public void history() {
        List<ConnInstanceHistoryConfTO> history = connectorHistoryService.list("74141a3b-0762-4720-a4aa-fc3e374ef3ef");
        assertNotNull(history);
        int pre = history.size();

        ConnInstanceTO ldapConn = connectorService.read("74141a3b-0762-4720-a4aa-fc3e374ef3ef", null);
        String originalDisplayName = ldapConn.getDisplayName();
        Set<ConnectorCapability> originalCapabilities = new HashSet<>(ldapConn.getCapabilities());
        ConnConfProperty originalConfProp = SerializationUtils.clone(
                ldapConn.getConf("maintainPosixGroupMembership").get());
        assertEquals(1, originalConfProp.getValues().size());
        assertEquals("false", originalConfProp.getValues().get(0));

        ldapConn.setDisplayName(originalDisplayName + " modified");
        ldapConn.getCapabilities().clear();
        ldapConn.getConf("maintainPosixGroupMembership").get().getValues().set(0, "true");
        connectorService.update(ldapConn);

        ldapConn = connectorService.read("74141a3b-0762-4720-a4aa-fc3e374ef3ef", null);
        assertNotEquals(originalDisplayName, ldapConn.getDisplayName());
        assertNotEquals(originalCapabilities, ldapConn.getCapabilities());
        assertNotEquals(originalConfProp, ldapConn.getConf("maintainPosixGroupMembership"));

        history = connectorHistoryService.list("74141a3b-0762-4720-a4aa-fc3e374ef3ef");
        assertEquals(pre + 1, history.size());

        connectorHistoryService.restore(history.get(0).getKey());

        ldapConn = connectorService.read("74141a3b-0762-4720-a4aa-fc3e374ef3ef", null);
        assertEquals(originalDisplayName, ldapConn.getDisplayName());
        assertEquals(originalCapabilities, ldapConn.getCapabilities());
        assertEquals(originalConfProp, ldapConn.getConf("maintainPosixGroupMembership").get());
    }

    @Test
    public void authorizations() {
        SyncopeClient puccini = clientFactory.create("puccini", ADMIN_PWD);
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
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        // 3. read and upate a connector in the realm for which entitlements are owned: succeed
        try {
            ConnInstanceTO scriptedsql = pcs.read("a6d017fd-a705-4507-bb7c-6ab6a6745997", null);
            ConnConfProperty reloadScriptOnExecution = scriptedsql.getConf("reloadScriptOnExecution").get();
            assertEquals("true", reloadScriptOnExecution.getValues().get(0).toString());

            reloadScriptOnExecution.getValues().set(0, "false");
            pcs.update(scriptedsql);

            scriptedsql = pcs.read(scriptedsql.getKey(), null);
            reloadScriptOnExecution = scriptedsql.getConf("reloadScriptOnExecution").get();
            assertEquals("false", reloadScriptOnExecution.getValues().get(0).toString());
        } finally {
            ConnInstanceTO scriptedsql = connectorService.read("a6d017fd-a705-4507-bb7c-6ab6a6745997", null);
            ConnConfProperty reloadScriptOnExecution = scriptedsql.getConf("reloadScriptOnExecution").get();
            reloadScriptOnExecution.getValues().set(0, "true");
            connectorService.update(scriptedsql);
        }
    }

    @Test
    public void issueSYNCOPE10() {
        // ----------------------------------
        // Copy resource and connector in order to create new objects.
        // ----------------------------------
        // Retrieve a connector instance template.
        ConnInstanceTO connInstanceTO = connectorService.read(
                "fcf9f2b0-f7d6-42c9-84a6-61b28255a42b", Locale.ENGLISH.getLanguage());
        assertNotNull(connInstanceTO);

        // check for resource
        List<ResourceTO> resources = resourceService.list().stream().
                filter(resource -> "fcf9f2b0-f7d6-42c9-84a6-61b28255a42b".equals(resource.getConnector())).
                collect(Collectors.toList());
        assertEquals(4, resources.size());

        // Retrieve a resource TO template.
        ResourceTO resourceTO = resources.get(0);

        // Make it new.
        resourceTO.setKey("newAbout103" + getUUIDString());

        // Make it new.
        connInstanceTO.setKey(null);
        connInstanceTO.setDisplayName("newDisplayName" + getUUIDString());
        // ----------------------------------

        // ----------------------------------
        // Create a new connector instance.
        // ----------------------------------
        Response response = connectorService.create(connInstanceTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
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
        response = resourceService.create(resourceTO);
        resourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

        assertNotNull(resourceTO);

        resources = resourceService.list().stream().
                filter(resource -> connKey.equals(resource.getConnector())).collect(Collectors.toList());
        assertEquals(1, resources.size());
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean.
        // ----------------------------------
        ConnInstanceTO connInstanceBean = connectorService.readByResource(
                resourceTO.getKey(), Locale.ENGLISH.getLanguage());
        assertNotNull(connInstanceBean);
        assertFalse(connInstanceBean.getCapabilities().contains(ConnectorCapability.AUTHENTICATE));
        // ----------------------------------

        // ----------------------------------
        // Check for spring bean update after connector instance update.
        // ----------------------------------
        connInstanceTO.getCapabilities().add(ConnectorCapability.AUTHENTICATE);

        connectorService.update(connInstanceTO);
        ConnInstanceTO actual = connectorService.read(connInstanceTO.getKey(), Locale.ENGLISH.getLanguage());
        assertNotNull(actual);
        assertTrue(connInstanceTO.getCapabilities().contains(ConnectorCapability.AUTHENTICATE));

        // check for spring bean update
        connInstanceBean = connectorService.readByResource(resourceTO.getKey(), Locale.ENGLISH.getLanguage());
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

        connectorTO.setLocation(connectorService.read(
                "88a7a819-dab5-46b4-9b90-0b9769eabdb8", Locale.ENGLISH.getLanguage()).getLocation());

        // set connector version
        connectorTO.setVersion(connIdSoapVersion);

        // set connector name
        connectorTO.setConnectorName("net.tirasa.connid.bundles.soap.WebServiceConnector");

        // set bundle name
        connectorTO.setBundleName("net.tirasa.connid.bundles.soap");

        // set display name
        connectorTO.setDisplayName("WSSoap");

        // set the connector configuration using PropertyTO
        Set<ConnConfProperty> conf = new HashSet<>();

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
                connectorService.check(connectorTO);
                fail();
            } catch (Exception e) {
                assertNotNull(e);
            }

            Response response = connectorService.create(connectorTO);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
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

            conf = new HashSet<>();
            endpoint.getValues().clear();
            endpoint.getValues().add("http://localhost:9080/syncope-fit-build-tools/cxf/soap/provisioning");
            conf.add(endpoint);

            resourceTO.getConfOverride().addAll(conf);

            ProvisionTO provisionTO = new ProvisionTO();
            provisionTO.setAnyType(AnyTypeKind.USER.name());
            provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resourceTO.getProvisions().add(provisionTO);

            MappingTO mapping = new MappingTO();
            provisionTO.setMapping(mapping);

            ItemTO mapItem = new ItemTO();
            mapItem.setExtAttrName("uid");
            mapItem.setIntAttrName("userId");
            mapItem.setConnObjectKey(true);
            mapping.setConnObjectKeyItem(mapItem);
            // ----------------------------------------

            // ----------------------------------------
            // Check connection without saving the resource ....
            // ----------------------------------------
            try {
                resourceService.check(resourceTO);
            } catch (Exception e) {
                fail(ExceptionUtils.getStackTrace(e));
            }
            // ----------------------------------------
        } finally {
            // Remove connector from db to make test re-runnable
            connectorService.delete(connectorTO.getKey());
        }
    }

    @Test
    public void issueSYNCOPE605() {
        ConnInstanceTO connectorInstanceTO = connectorService.read(
                "fcf9f2b0-f7d6-42c9-84a6-61b28255a42b", Locale.ENGLISH.getLanguage());
        assertTrue(connectorInstanceTO.getCapabilities().isEmpty());

        connectorInstanceTO.getCapabilities().add(ConnectorCapability.SEARCH);
        connectorService.update(connectorInstanceTO);

        ConnInstanceTO updatedCapabilities = connectorService.read(
                connectorInstanceTO.getKey(), Locale.ENGLISH.getLanguage());
        assertNotNull(updatedCapabilities.getCapabilities());
        assertTrue(updatedCapabilities.getCapabilities().size() == 1);
    }
}
