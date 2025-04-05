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

import static org.assertj.core.api.Assumptions.assumeThatCollection;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.SyncReplInboundActions;
import org.apache.syncope.core.provisioning.java.pushpull.SyncReplLiveSyncDeltaMapper;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.function.ThrowingConsumer;

class SyncReplITCase extends AbstractITCase {

    private static final String RESOURCE_NAME_OPENLDAP = "resource-openldap";

    private static final int OPENLDAP_PORT = 1389;

    private static final String ADMIN_PASSWORD = "adminpassword";

    private static final String ADMIN_DN = "cn=admin,o=isp";

    @BeforeAll
    public static void syncReplSetup() {
        assumeThatCollection(System.getProperties().keySet()).anyMatch("OPENLDAP_IP"::equals);

        // LiveSyncDeltaMapper
        ImplementationTO liveSDM = null;
        try {
            liveSDM = IMPLEMENTATION_SERVICE.read(
                    IdMImplementationType.LIVE_SYNC_DELTA_MAPPER, SyncReplLiveSyncDeltaMapper.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                liveSDM = new ImplementationTO();
                liveSDM.setKey(SyncReplLiveSyncDeltaMapper.class.getSimpleName());
                liveSDM.setEngine(ImplementationEngine.JAVA);
                liveSDM.setType(IdMImplementationType.LIVE_SYNC_DELTA_MAPPER);
                liveSDM.setBody(SyncReplLiveSyncDeltaMapper.class.getName());
                Response response = IMPLEMENTATION_SERVICE.create(liveSDM);
                liveSDM = IMPLEMENTATION_SERVICE.read(
                        liveSDM.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(liveSDM);
            }
        }
        assertNotNull(liveSDM);

        ImplementationTO syncReplInboundActions = null;
        try {
            syncReplInboundActions = IMPLEMENTATION_SERVICE.read(
                    IdMImplementationType.INBOUND_ACTIONS, SyncReplInboundActions.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                syncReplInboundActions = new ImplementationTO();
                syncReplInboundActions.setKey(SyncReplInboundActions.class.getSimpleName());
                syncReplInboundActions.setEngine(ImplementationEngine.JAVA);
                syncReplInboundActions.setType(IdMImplementationType.INBOUND_ACTIONS);
                syncReplInboundActions.setBody(SyncReplInboundActions.class.getName());
                Response response = IMPLEMENTATION_SERVICE.create(syncReplInboundActions);
                syncReplInboundActions = IMPLEMENTATION_SERVICE.read(
                        syncReplInboundActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(syncReplInboundActions);
            }
        }
        assertNotNull(syncReplInboundActions);
        ResourceTO resource = null;
        try {
            resource = RESOURCE_SERVICE.read(RESOURCE_NAME_OPENLDAP);
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                // PlainSchema
                PlainSchemaTO userEntryUUID = new PlainSchemaTO();
                userEntryUUID.setKey("userEntryUUID");
                userEntryUUID.setType(AttrSchemaType.String);
                userEntryUUID.setMandatoryCondition("false");
                userEntryUUID.setReadonly(true);
                createSchema(SchemaType.PLAIN, userEntryUUID);

                AnyTypeClassTO minimalUser = ANY_TYPE_CLASS_SERVICE.read("minimal user");
                minimalUser.getPlainSchemas().add(userEntryUUID.getKey());
                ANY_TYPE_CLASS_SERVICE.update(minimalUser);

                PlainSchemaTO groupEntryUUID = new PlainSchemaTO();
                groupEntryUUID.setKey("groupEntryUUID");
                groupEntryUUID.setType(AttrSchemaType.String);
                groupEntryUUID.setMandatoryCondition("false");
                groupEntryUUID.setReadonly(true);
                createSchema(SchemaType.PLAIN, groupEntryUUID);

                AnyTypeClassTO minimalGroup = ANY_TYPE_CLASS_SERVICE.read("minimal group");
                minimalGroup.getPlainSchemas().add(groupEntryUUID.getKey());
                ANY_TYPE_CLASS_SERVICE.update(minimalGroup);

                // ConnInstance
                ConnIdBundle bundle = CONNECTOR_SERVICE.getBundles(null).stream().
                        filter(b -> "net.tirasa.connid.bundles.ldup.LdUpConnector".equals(b.getConnectorName())).
                        findFirst().
                        orElseThrow();

                ConnInstanceTO connector = new ConnInstanceTO();
                connector.setAdminRealm(SyncopeConstants.ROOT_REALM);
                connector.setLocation(bundle.getLocation());
                connector.setVersion(bundle.getVersion());
                connector.setConnectorName(bundle.getConnectorName());
                connector.setBundleName(bundle.getBundleName());
                connector.setDisplayName(bundle.getDisplayName());
                connector.setConnRequestTimeout(15);
                connector.getCapabilities().add(ConnectorCapability.LIVE_SYNC);

                ConnConfPropSchema schema = bundle.getProperties().stream().
                        filter(s -> "url".equals(s.getName())).findFirst().orElseThrow();
                ConnConfProperty prop = new ConnConfProperty();
                prop.setSchema(schema);
                prop.getValues().add("ldap://" + System.getProperty("OPENLDAP_IP") + ":" + OPENLDAP_PORT);
                connector.getConf().add(prop);

                schema = bundle.getProperties().stream().
                        filter(s -> "bindDn".equals(s.getName())).findFirst().orElseThrow();
                prop = new ConnConfProperty();
                prop.setSchema(schema);
                prop.getValues().add(ADMIN_DN);
                connector.getConf().add(prop);

                schema = bundle.getProperties().stream().
                        filter(s -> "bindPassword".equals(s.getName())).findFirst().orElseThrow();
                prop = new ConnConfProperty();
                prop.setSchema(schema);
                prop.getValues().add(ADMIN_PASSWORD);
                connector.getConf().add(prop);

                schema = bundle.getProperties().stream().
                        filter(s -> "baseDn".equals(s.getName())).findFirst().orElseThrow();
                prop = new ConnConfProperty();
                prop.setSchema(schema);
                prop.getValues().add("o=isp");
                connector.getConf().add(prop);

                schema = bundle.getProperties().stream().
                        filter(s -> "groupObjectClass".equals(s.getName())).findFirst().orElseThrow();
                prop = new ConnConfProperty();
                prop.setSchema(schema);
                prop.getValues().add("groupOfNames");
                connector.getConf().add(prop);

                schema = bundle.getProperties().stream().
                        filter(s -> "groupMemberAttribute".equals(s.getName())).findFirst().orElseThrow();
                prop = new ConnConfProperty();
                prop.setSchema(schema);
                prop.getValues().add("member");
                connector.getConf().add(prop);

                schema = bundle.getProperties().stream().
                        filter(s -> "legacyCompatibilityMode".equals(s.getName())).findFirst().orElseThrow();
                prop = new ConnConfProperty();
                prop.setSchema(schema);
                prop.getValues().add(true);
                connector.getConf().add(prop);

                ConnPoolConf cpc = new ConnPoolConf();
                cpc.setMaxObjects(10);
                connector.setPoolConf(cpc);

                Response response = CONNECTOR_SERVICE.create(connector);
                if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                    throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
                }

                // ExternalResource
                resource = new ResourceTO();
                resource.setKey(RESOURCE_NAME_OPENLDAP);
                resource.setConnector(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

                Provision user = new Provision();
                user.setAnyType(AnyTypeKind.USER.name());
                user.setObjectClass("inetOrgPerson");
                user.setIgnoreCaseMatch(true);
                user.setUidOnCreate(userEntryUUID.getKey());
                resource.getProvisions().add(user);

                Mapping mapping = new Mapping();
                user.setMapping(mapping);

                mapping.setConnObjectLink("'cn=' + username + ',ou=People,o=isp'");

                Item item = new Item();
                item.setIntAttrName(userEntryUUID.getKey());
                item.setExtAttrName(userEntryUUID.getKey());
                item.setPurpose(MappingPurpose.PULL);
                mapping.setConnObjectKeyItem(item);

                item = new Item();
                item.setIntAttrName("fullname");
                item.setExtAttrName("cn");
                item.setPurpose(MappingPurpose.PULL);
                mapping.add(item);

                item = new Item();
                item.setIntAttrName("username");
                item.setExtAttrName("uid");
                item.setPurpose(MappingPurpose.PULL);
                mapping.add(item);

                item = new Item();
                item.setIntAttrName("surname");
                item.setExtAttrName("sn");
                item.setPurpose(MappingPurpose.PULL);
                mapping.add(item);

                item = new Item();
                item.setIntAttrName("firstname");
                item.setExtAttrName("givenName");
                item.setPurpose(MappingPurpose.PULL);
                mapping.add(item);

                item = new Item();
                item.setIntAttrName("email");
                item.setExtAttrName("mail");
                item.setPurpose(MappingPurpose.PULL);
                mapping.add(item);

                item = new Item();
                item.setIntAttrName("userId");
                item.setExtAttrName("mail");
                item.setPurpose(MappingPurpose.PULL);
                mapping.add(item);

                Provision group = new Provision();
                group.setAnyType(AnyTypeKind.GROUP.name());
                group.setObjectClass("groupOfNames");
                group.setIgnoreCaseMatch(true);
                group.setUidOnCreate(groupEntryUUID.getKey());
                resource.getProvisions().add(group);

                mapping = new Mapping();
                group.setMapping(mapping);

                mapping.setConnObjectLink("'cn=' + name + ',ou=Groups,o=isp'");

                item = new Item();
                item.setIntAttrName(groupEntryUUID.getKey());
                item.setExtAttrName(groupEntryUUID.getKey());
                item.setPurpose(MappingPurpose.PULL);
                mapping.setConnObjectKeyItem(item);

                item = new Item();
                item.setIntAttrName("name");
                item.setExtAttrName("cn");
                item.setPurpose(MappingPurpose.PULL);
                mapping.add(item);

                resource = createResource(resource);
            }
        }
        assertNotNull(resource);
    }

    private static void execOnOpenLDAP(final ThrowingConsumer<LDAPConnection> op) throws LDAPException {
        try (LDAPConnection ldapConn = new LDAPConnection(
                System.getProperty("OPENLDAP_IP"), OPENLDAP_PORT, ADMIN_DN, ADMIN_PASSWORD)) {

            op.accept(ldapConn);
        }
    }

    @Test
    void liveSync() throws LDAPException {
        assumeThatCollection(System.getProperties().keySet()).anyMatch("OPENLDAP_IP"::equals);

        // 0. complete the predefined test users with missing attributes
        execOnOpenLDAP(ldapConn -> {
            ldapConn.modify("cn=user01,ou=People,o=isp",
                    new Modification(ModificationType.ADD, "mail", "user01@syncope.apache.org"),
                    new Modification(ModificationType.ADD, "givenName", "User01"));
            ldapConn.modify("cn=user02,ou=People,o=isp",
                    new Modification(ModificationType.ADD, "mail", "user02@syncope.apache.org"),
                    new Modification(ModificationType.ADD, "givenName", "User02"));
        });

        // 1. create and execute the live sync task
        LiveSyncTaskTO task = new LiveSyncTaskTO();
        task.setName("OpenLDAP SyncRepl");
        task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        task.setResource(RESOURCE_NAME_OPENLDAP);
        task.setLiveSyncDeltaMapper(SyncReplLiveSyncDeltaMapper.class.getSimpleName());
        task.getActions().add(SyncReplInboundActions.class.getSimpleName());
        task.getActions().add(LDAPMembershipPullActions.class.getSimpleName());
        task.setPerformCreate(true);
        task.setPerformUpdate(true);
        task.setPerformDelete(true);

        Response response = TASK_SERVICE.create(TaskType.LIVE_SYNC, task);
        LiveSyncTaskTO actual = getObject(response.getLocation(), TaskService.class, LiveSyncTaskTO.class);
        assertNotNull(actual);

        task = TASK_SERVICE.read(TaskType.LIVE_SYNC, actual.getKey(), true);
        assertNotNull(task);
        assertEquals(actual.getKey(), task.getKey());
        assertNotNull(actual.getJobDelegate());
        assertEquals(actual.getLiveSyncDeltaMapper(), task.getLiveSyncDeltaMapper());

        TASK_SERVICE.execute(new ExecSpecs.Builder().key(task.getKey()).build());

        try {
            // 2. check preexisting
            List<GroupTO> pregroups = await().
                    atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {

                try {
                    return GROUP_SERVICE.<GroupTO>search(
                            new AnyQuery.Builder().fiql("name==readers").build()).getResult();
                } catch (SyncopeClientException e) {
                    return List.of();
                }
            }, match -> match.size() == 1);
            assertEquals(1, pregroups.size());

            List<UserTO> preusers = await().
                    atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {

                try {
                    return USER_SERVICE.<UserTO>search(
                            new AnyQuery.Builder().fiql("username==user0*").build()).getResult();
                } catch (SyncopeClientException e) {
                    return List.of();
                }
            }, match -> match.size() == 2);
            assertEquals(2, preusers.size());
            assertTrue(preusers.stream().anyMatch(u -> "user01".equals(u.getUsername()) && "user01@syncope.apache.org".
                    equals(u.getPlainAttr("email").orElseThrow().getValues().getFirst())));
            assertTrue(preusers.stream().anyMatch(u -> "user02".equals(u.getUsername()) && "user02@syncope.apache.org".
                    equals(u.getPlainAttr("email").orElseThrow().getValues().getFirst())));
            assertTrue(preusers.stream().allMatch(u -> u.getPlainAttr("userEntryUUID").isPresent()));
            assertTrue(preusers.stream().allMatch(u -> u.getMembership(pregroups.getFirst().getKey()).isPresent()));

            // 3. ldap update
            execOnOpenLDAP(ldapConn -> ldapConn.modify("cn=user01,ou=People,o=isp",
                    new Modification(ModificationType.REPLACE, "mail", "user01_new@syncope.apache.org")));

            // 4. check that change is now in Syncope
            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                    () -> USER_SERVICE.read("user01").getPlainAttr("email").orElseThrow().getValues().getFirst(),
                    "user01_new@syncope.apache.org"::equals);

            // 5. ldap create
            execOnOpenLDAP(ldapConn -> {
                List<Attribute> attrs = List.of(
                        new Attribute("objectClass", "inetOrgPerson"),
                        new Attribute("uid", "jdoe"),
                        new Attribute("sn", "Doe"),
                        new Attribute("givenName", "John"),
                        new Attribute("cn", "John Doe"),
                        new Attribute("mail", "john.doe@syncope.apache.org"));
                ldapConn.add(new AddRequest("uid=jdoe,ou=People,o=isp", attrs));
            });

            // 6. check that the new user is now in Syncope
            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                try {
                    return USER_SERVICE.read("jdoe");
                } catch (SyncopeClientException e) {
                    return null;
                }
            }, jdoe -> jdoe.getPlainAttr("userEntryUUID").isPresent());

            // 7. ldap delete
            execOnOpenLDAP(ldapConn -> ldapConn.delete("uid=jdoe,ou=People,o=isp"));

            // 8. check that the new user was removed from Syncope
            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).
                    failFast(() -> USER_SERVICE.read("jdoe"));
        } finally {
            // finally stop live syncing
            TASK_SERVICE.actionJob(task.getKey(), JobAction.STOP);
        }
    }
}
