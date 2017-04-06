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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.migration.MigrationPullActions;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:migrationEnv.xml" })
public class MigrationITCase extends AbstractTaskITCase {

    private static final String CONNINSTANCE_DISPLAY_NAME = "syncope12DB";

    private static final String RESOURCE_KEY = "Syncope 1.2";

    private static final String MIGRATION_CIPHER_ALGORITHM = "migrationCipherAlgorithm";

    private static final String MIGRATION_RESOURCES_SCHEMA = "migrationResources";

    private static final String MIGRATION_MEMBERSHIPS_SCHEMA = "migrationMemberships";

    private static final String MIGRATION_ANYTYPE_CLASS = "migration";

    private static final String MIGRATION_REALM = "migration";

    private static final String PULL_TASK_NAME = "Syncope 1.2 migration";

    private static String basedir;

    private static String connectorServerLocation;

    private static String connIdDbVersion;

    @BeforeClass
    public static void setup() throws IOException {
        InputStream propStream = null;
        try {
            Properties props = new Properties();
            propStream = MigrationITCase.class.getResourceAsStream("/test.properties");
            props.load(propStream);
            IOUtils.closeQuietly(propStream);
            propStream = MigrationITCase.class.getResourceAsStream("/connid.properties");
            props.load(propStream);

            basedir = props.getProperty("basedir");

            for (String location : props.getProperty("connid.locations").split(",")) {
                if (!location.startsWith("file")) {
                    connectorServerLocation = location;
                }
            }

            connIdDbVersion = props.getProperty("connid.database.version");
        } catch (Exception e) {
            LOG.error("Could not load /connid.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }

        assertNotNull(basedir);
        assertNotNull(connectorServerLocation);
        assertNotNull(connIdDbVersion);
    }

    @Autowired
    private DriverManagerDataSource syncope12DataSource;

    private String setupAnyTypeClass() {
        PlainSchemaTO cipherAlgorithm = new PlainSchemaTO();
        cipherAlgorithm.setKey(MIGRATION_CIPHER_ALGORITHM);
        cipherAlgorithm.setType(AttrSchemaType.String);
        cipherAlgorithm.setReadonly(true);
        cipherAlgorithm = createSchema(SchemaType.PLAIN, cipherAlgorithm);

        PlainSchemaTO migrationResources = new PlainSchemaTO();
        migrationResources.setKey(MIGRATION_RESOURCES_SCHEMA);
        migrationResources.setType(AttrSchemaType.String);
        migrationResources.setMultivalue(true);
        migrationResources.setReadonly(true);
        migrationResources = createSchema(SchemaType.PLAIN, migrationResources);

        PlainSchemaTO migrationMemberships = new PlainSchemaTO();
        migrationMemberships.setKey(MIGRATION_MEMBERSHIPS_SCHEMA);
        migrationMemberships.setType(AttrSchemaType.String);
        migrationMemberships.setMultivalue(true);
        migrationMemberships.setReadonly(true);
        migrationMemberships = createSchema(SchemaType.PLAIN, migrationMemberships);

        AnyTypeClassTO migration = new AnyTypeClassTO();
        migration.setKey(MIGRATION_ANYTYPE_CLASS);
        migration.getPlainSchemas().add(cipherAlgorithm.getKey());
        migration.getPlainSchemas().add(migrationResources.getKey());
        migration.getPlainSchemas().add(migrationMemberships.getKey());

        Response response = anyTypeClassService.create(migration);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());

        return MIGRATION_ANYTYPE_CLASS;
    }

    private String setupConnector() {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setLocation(connectorServerLocation);
        connInstanceTO.setConnectorName("net.tirasa.connid.bundles.db.scriptedsql.ScriptedSQLConnector");
        connInstanceTO.setBundleName("net.tirasa.connid.bundles.db.scriptedsql");
        connInstanceTO.setVersion(connIdDbVersion);
        connInstanceTO.setDisplayName(CONNINSTANCE_DISPLAY_NAME);

        ConnConfPropSchema schema = new ConnConfPropSchema();
        schema.setName("user");
        schema.setType(String.class.getName());
        ConnConfProperty property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add(syncope12DataSource.getUsername());
        connInstanceTO.getConf().add(property);

        schema = new ConnConfPropSchema();
        schema.setName("password");
        schema.setType(GuardedString.class.getName());
        property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add(syncope12DataSource.getPassword());
        connInstanceTO.getConf().add(property);

        schema = new ConnConfPropSchema();
        schema.setName("jdbcDriver");
        property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add("org.h2.Driver");
        connInstanceTO.getConf().add(property);

        schema = new ConnConfPropSchema();
        schema.setName("jdbcUrlTemplate");
        property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add(syncope12DataSource.getUrl());
        connInstanceTO.getConf().add(property);

        schema = new ConnConfPropSchema();
        schema.setName("testScriptFileName");
        property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add(basedir + "/../../core/migration/src/main/resources/scripted/TestScript.groovy");
        connInstanceTO.getConf().add(property);

        schema = new ConnConfPropSchema();
        schema.setName("schemaScriptFileName");
        property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add(basedir + "/../../core/migration/src/main/resources/scripted/SchemaScript.groovy");
        connInstanceTO.getConf().add(property);

        schema = new ConnConfPropSchema();
        schema.setName("searchScriptFileName");
        property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add(basedir + "/../../core/migration/src/main/resources/scripted/SearchScript.groovy");
        connInstanceTO.getConf().add(property);

        schema = new ConnConfPropSchema();
        schema.setName("syncScriptFileName");
        property = new ConnConfProperty();
        property.setSchema(schema);
        property.getValues().add(basedir + "/../../core/migration/src/main/resources/scripted/SyncScript.groovy");
        connInstanceTO.getConf().add(property);

        connInstanceTO.getCapabilities().add(ConnectorCapability.SEARCH);
        connInstanceTO.getCapabilities().add(ConnectorCapability.SYNC);

        Response response = connectorService.create(connInstanceTO);
        connInstanceTO = getObject(response.getLocation(), ConnectorService.class, ConnInstanceTO.class);

        try {
            connectorService.check(connInstanceTO);
        } catch (Exception e) {
            fail("Unexpected exception:\n" + ExceptionUtils2.getFullStackTrace(e));
        }

        return connInstanceTO.getKey();
    }

    private void setupResource(final String connectorKey, final String anyTypeClass) {
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setKey(RESOURCE_KEY);
        resourceTO.setConnector(connectorKey);

        // USER
        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        provisionTO.getAuxClasses().add(anyTypeClass);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("username");
        item.setExtAttrName("username");
        item.setMandatoryCondition("true");
        item.setPurpose(MappingPurpose.PULL);
        mapping.setConnObjectKeyItem(item);

        item = new MappingItemTO();
        item.setPassword(true);
        item.setIntAttrName("password");
        item.setExtAttrName("__PASSWORD__");
        item.setMandatoryCondition("true");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName(MIGRATION_CIPHER_ALGORITHM);
        item.setExtAttrName("cipherAlgorithm");
        item.setMandatoryCondition("true");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("surname");
        item.setExtAttrName("surname");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("email");
        item.setExtAttrName("email");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("firstname");
        item.setExtAttrName("firstname");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("ctype");
        item.setExtAttrName("type");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("gender");
        item.setExtAttrName("gender");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("loginDate");
        item.setExtAttrName("loginDate");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName(MIGRATION_RESOURCES_SCHEMA);
        item.setExtAttrName("__RESOURCES__");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        // GROUP
        provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.GROUP.name());
        provisionTO.setObjectClass(ObjectClass.GROUP_NAME);
        provisionTO.getAuxClasses().add(anyTypeClass);
        resourceTO.getProvisions().add(provisionTO);

        mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        item = new MappingItemTO();
        item.setIntAttrName("name");
        item.setExtAttrName("name");
        item.setMandatoryCondition("true");
        item.setPurpose(MappingPurpose.PULL);
        mapping.setConnObjectKeyItem(item);

        item = new MappingItemTO();
        item.setIntAttrName("show");
        item.setExtAttrName("show");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("title");
        item.setExtAttrName("title");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName("icon");
        item.setExtAttrName("icon");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName(MIGRATION_RESOURCES_SCHEMA);
        item.setExtAttrName("__RESOURCES__");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        item = new MappingItemTO();
        item.setIntAttrName(MIGRATION_MEMBERSHIPS_SCHEMA);
        item.setExtAttrName("__MEMBERSHIPS__");
        item.setMandatoryCondition("false");
        item.setPurpose(MappingPurpose.PULL);
        mapping.add(item);

        resourceService.create(resourceTO);
    }

    private void setupRealm() {
        try {
            realmService.list("/" + MIGRATION_REALM);
        } catch (SyncopeClientException e) {
            LOG.error("{} not found? Let's attempt to re-create...", MIGRATION_REALM, e);

            RealmTO realm = new RealmTO();
            realm.setName(MIGRATION_REALM);
            realmService.create("/", realm);
        }
    }

    private String setupPullTask() {
        PullTaskTO task = new PullTaskTO();
        task.setActive(true);
        task.setName(PULL_TASK_NAME);
        task.setResource(RESOURCE_KEY);
        task.setPerformCreate(true);
        task.setSyncStatus(true);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.setDestinationRealm("/" + MIGRATION_REALM);
        task.getActionsClassNames().add(MigrationPullActions.class.getName());

        UserTO user = new UserTO();
        user.getPlainAttrs().add(new AttrTO.Builder().schema("userId").value("'12' + username + '@syncope.apache.org'").
                build());
        user.getPlainAttrs().add(new AttrTO.Builder().schema("fullname").value("username").build());
        task.getTemplates().put(AnyTypeKind.USER.name(), user);

        Response response = taskService.create(task);
        task = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);

        return task.getKey();
    }

    @Test
    public void migrateFromSyncope12() throws InterruptedException {
        // 1. cleanup
        try {
            for (AbstractTaskTO task : taskService.list(
                    new TaskQuery.Builder(TaskType.PULL).resource(RESOURCE_KEY).build()).getResult()) {

                if (PULL_TASK_NAME.equals(PullTaskTO.class.cast(task).getName())) {
                    taskService.delete(task.getKey());
                }
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            resourceService.delete(RESOURCE_KEY);
        } catch (Exception e) {
            // ignore
        }
        try {
            for (ConnInstanceTO connInstance : connectorService.list(null)) {
                if (CONNINSTANCE_DISPLAY_NAME.equals(connInstance.getDisplayName())) {
                    connectorService.delete(connInstance.getKey());
                }
            }
        } catch (Exception e) {
            // ignore
        }
        try {
            schemaService.delete(SchemaType.PLAIN, MIGRATION_CIPHER_ALGORITHM);
            schemaService.delete(SchemaType.PLAIN, MIGRATION_MEMBERSHIPS_SCHEMA);
            schemaService.delete(SchemaType.PLAIN, MIGRATION_RESOURCES_SCHEMA);
            anyTypeClassService.delete(MIGRATION_ANYTYPE_CLASS);
        } catch (Exception e) {
            // ignore
        }

        BulkAction bulkAction = new BulkAction();
        bulkAction.setType(BulkAction.Type.DELETE);

        for (UserTO user : userService.search(new AnyQuery.Builder().fiql("username==*12").build()).getResult()) {
            bulkAction.getTargets().add(user.getKey());
        }
        userService.bulk(bulkAction);

        bulkAction.getTargets().clear();
        for (GroupTO group : groupService.search(new AnyQuery.Builder().fiql("name==*12").build()).getResult()) {
            bulkAction.getTargets().add(group.getKey());
        }
        groupService.bulk(bulkAction);

        // 2. setup
        setupResource(setupConnector(), setupAnyTypeClass());
        setupRealm();
        String pullTaskKey = setupPullTask();

        // 3. execute pull task
        execProvisioningTask(taskService, pullTaskKey, 50, false);

        // 4. verify
        UserTO user = null;

        int i = 0;
        boolean membershipFound = false;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            try {
                user = userService.read("rossini12");
                assertNotNull(user);

                membershipFound = IterableUtils.matchesAny(user.getMemberships(), new Predicate<MembershipTO>() {

                    @Override
                    public boolean evaluate(final MembershipTO object) {
                        return "1 root12".equals(object.getGroupName());
                    }
                });
            } catch (Exception e) {
                // ignore
            }

            i++;
        } while (!membershipFound && i < 50);
        assertNotNull(user);
        assertTrue(membershipFound);

        assertEquals("/" + MIGRATION_REALM, user.getRealm());
        GroupTO group = groupService.read("12 aRoleForPropagation12");
        assertNotNull(group);
        assertEquals("/" + MIGRATION_REALM, group.getRealm());

        // 4a. user plain attrs
        assertEquals("Gioacchino", user.getPlainAttrMap().get("firstname").getValues().get(0));
        assertEquals("Rossini", user.getPlainAttrMap().get("surname").getValues().get(0));

        // 4b. user resources
        assertTrue(user.getResources().contains(RESOURCE_NAME_TESTDB2));

        // 4c. user password
        assertNotNull(clientFactory.create("bellini12", ADMIN_PWD).self());

        // 4d. group plain attrs
        assertEquals("r12", group.getPlainAttrMap().get("title").getValues().get(0));

        // 4e. group resources
        assertTrue(group.getResources().contains(RESOURCE_NAME_CSV));
    }
}
