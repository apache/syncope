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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.audit.EventCategory;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.core.logic.ConnectorLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.ReportLogic;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class AuditITCase extends AbstractITCase {

    private static AuditConfTO buildAuditConf(final String auditLoggerName, final boolean active) {
        AuditConfTO auditConfTO = new AuditConfTO();
        auditConfTO.setActive(active);
        auditConfTO.setKey(auditLoggerName);
        return auditConfTO;
    }

    private static AuditEntry queryWithFailure(final AuditQuery query, final int maxWaitSeconds) {
        List<AuditEntry> results = query(query, maxWaitSeconds);
        if (results.isEmpty()) {
            fail("Timeout when executing query for key " + query.getEntityKey());
            return null;
        }
        return results.get(0);
    }

    @Test
    public void userReadAndSearchYieldsNoAudit() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder().entityKey(userTO.getKey()).build();
        List<AuditEntry> entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());

        PagedResult<UserTO> usersTOs = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalTo(userTO.getUsername()).query()).
                        build());
        assertNotNull(usersTOs);
        assertFalse(usersTOs.getResult().isEmpty());

        entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());
    }

    @Test
    public void findByUser() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder().
                entityKey(userTO.getKey()).
                before(OffsetDateTime.now().plusSeconds(30)).
                page(1).
                size(1).
                orderBy("event_date desc").
                build();
        AuditEntry entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        USER_SERVICE.delete(userTO.getKey());
    }

    @Test
    public void findByUserAndOther() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit-2@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder().
                entityKey(userTO.getKey()).
                orderBy("event_date desc").
                page(1).
                size(1).
                type(AuditElements.EventCategoryType.LOGIC).
                category(UserLogic.class.getSimpleName()).
                event("create").
                result(AuditElements.Result.SUCCESS).
                after(OffsetDateTime.now().minusSeconds(30)).
                build();
        AuditEntry entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        USER_SERVICE.delete(userTO.getKey());
    }

    @Test
    public void findByGroup() {
        GroupTO groupTO = createGroup(GroupITCase.getBasicSample("AuditGroup")).getEntity();
        assertNotNull(groupTO.getKey());

        AuditQuery query = new AuditQuery.Builder().entityKey(groupTO.getKey()).orderBy("event_date desc").
                page(1).size(1).build();
        AuditEntry entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        GROUP_SERVICE.delete(groupTO.getKey());
    }

    @Test
    public void groupReadAndSearchYieldsNoAudit() {
        GroupTO groupTO = createGroup(GroupITCase.getBasicSample("AuditGroupSearch")).getEntity();
        assertNotNull(groupTO.getKey());

        AuditQuery query = new AuditQuery.Builder().entityKey(groupTO.getKey()).build();
        List<AuditEntry> entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());

        PagedResult<GroupTO> groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo(groupTO.getName()).query()).
                build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());

        entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());
    }

    @Test
    public void findByAnyObject() {
        AnyObjectTO anyObjectTO = createAnyObject(AnyObjectITCase.getSample("Italy")).getEntity();
        assertNotNull(anyObjectTO.getKey());
        AuditQuery query = new AuditQuery.Builder().entityKey(anyObjectTO.getKey()).
                orderBy("event_date desc").page(1).size(1).build();
        AuditEntry entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        ANY_OBJECT_SERVICE.delete(anyObjectTO.getKey());
    }

    @Test
    public void anyObjectReadAndSearchYieldsNoAudit() {
        AnyObjectTO anyObjectTO = createAnyObject(AnyObjectITCase.getSample("USA")).getEntity();
        assertNotNull(anyObjectTO);

        AuditQuery query = new AuditQuery.Builder().entityKey(anyObjectTO.getKey()).build();
        List<AuditEntry> entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());

        PagedResult<AnyObjectTO> anyObjects = ANY_OBJECT_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(anyObjectTO.getType()).query()).
                        build());
        assertNotNull(anyObjects);
        assertFalse(anyObjects.getResult().isEmpty());

        entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());
    }

    @Test
    public void findByConnector() throws JsonProcessingException {
        String connectorKey = "74141a3b-0762-4720-a4aa-fc3e374ef3ef";

        AuditQuery query = new AuditQuery.Builder().
                entityKey(connectorKey).
                orderBy("event_date desc").
                type(AuditElements.EventCategoryType.LOGIC).
                category(ConnectorLogic.class.getSimpleName()).
                event("update").
                result(AuditElements.Result.SUCCESS).
                build();
        List<AuditEntry> entries = AUDIT_SERVICE.search(query).getResult();
        int pre = entries.size();

        ConnInstanceTO ldapConn = CONNECTOR_SERVICE.read(connectorKey, null);
        String originalDisplayName = ldapConn.getDisplayName();
        Set<ConnectorCapability> originalCapabilities = new HashSet<>(ldapConn.getCapabilities());
        ConnConfProperty originalConfProp = SerializationUtils.clone(
                ldapConn.getConf("maintainPosixGroupMembership").get());
        assertEquals(1, originalConfProp.getValues().size());
        assertEquals("false", originalConfProp.getValues().get(0));

        ldapConn.setDisplayName(originalDisplayName + " modified");
        ldapConn.getCapabilities().clear();
        ldapConn.getConf("maintainPosixGroupMembership").get().getValues().set(0, "true");
        CONNECTOR_SERVICE.update(ldapConn);

        ldapConn = CONNECTOR_SERVICE.read(connectorKey, null);
        assertNotEquals(originalDisplayName, ldapConn.getDisplayName());
        assertNotEquals(originalCapabilities, ldapConn.getCapabilities());
        assertNotEquals(originalConfProp, ldapConn.getConf("maintainPosixGroupMembership"));

        entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(pre + 1, entries.size());

        ConnInstanceTO restore = JSON_MAPPER.readValue(entries.get(0).getBefore(), ConnInstanceTO.class);
        CONNECTOR_SERVICE.update(restore);

        ldapConn = CONNECTOR_SERVICE.read(connectorKey, null);
        assertEquals(originalDisplayName, ldapConn.getDisplayName());
        assertEquals(originalCapabilities, ldapConn.getCapabilities());
        assertEquals(originalConfProp, ldapConn.getConf("maintainPosixGroupMembership").get());
    }

    @Test
    public void enableDisable() {
        AuditLoggerName auditLoggerName = new AuditLoggerName(
                AuditElements.EventCategoryType.LOGIC,
                ReportLogic.class.getSimpleName(),
                null,
                "deleteExecution",
                AuditElements.Result.FAILURE);

        List<AuditConfTO> audits = AUDIT_SERVICE.list();
        assertFalse(audits.stream().anyMatch(a -> a.getKey().equals(auditLoggerName.toAuditKey())));

        AuditConfTO audit = new AuditConfTO();
        audit.setKey(auditLoggerName.toAuditKey());
        audit.setActive(true);
        AUDIT_SERVICE.set(audit);

        audits = AUDIT_SERVICE.list();
        assertTrue(audits.stream().anyMatch(a -> a.getKey().equals(auditLoggerName.toAuditKey())));

        AUDIT_SERVICE.delete(audit.getKey());

        audits = AUDIT_SERVICE.list();
        assertFalse(audits.stream().anyMatch(a -> a.getKey().equals(auditLoggerName.toAuditKey())));
    }

    @Test
    public void listAuditEvents() {
        List<EventCategory> events = AUDIT_SERVICE.events();

        boolean found = false;

        for (EventCategory eventCategoryTO : events) {
            if (UserLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(AuditElements.EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("search"));
                assertFalse(eventCategoryTO.getEvents().contains("doCreate"));
                assertFalse(eventCategoryTO.getEvents().contains("setStatusOnWfAdapter"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
            if (GroupLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(AuditElements.EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("search"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
            if (ResourceLogic.class.getSimpleName().equals(eventCategoryTO.getCategory())) {
                assertEquals(AuditElements.EventCategoryType.LOGIC, eventCategoryTO.getType());
                assertTrue(eventCategoryTO.getEvents().contains("create"));
                assertTrue(eventCategoryTO.getEvents().contains("read"));
                assertTrue(eventCategoryTO.getEvents().contains("delete"));
                assertFalse(eventCategoryTO.getEvents().contains("resolveReference"));
                found = true;
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
            if (AnyTypeKind.USER.name().equals(eventCategoryTO.getCategory())) {
                if (RESOURCE_NAME_LDAP.equals(eventCategoryTO.getSubcategory())
                        && AuditElements.EventCategoryType.PULL == eventCategoryTO.getType()) {

                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.DELETE.name().toLowerCase()));
                    found = true;
                }
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
            if (AnyTypeKind.USER.name().equals(eventCategoryTO.getCategory())) {
                if (RESOURCE_NAME_CSV.equals(eventCategoryTO.getSubcategory())
                        && AuditElements.EventCategoryType.PROPAGATION == eventCategoryTO.getType()) {

                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.CREATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.UPDATE.name().toLowerCase()));
                    assertTrue(eventCategoryTO.getEvents().contains(ResourceOperation.DELETE.name().toLowerCase()));
                    found = true;
                }
            }
        }
        assertTrue(found);

        found = false;
        for (EventCategory eventCategoryTO : events) {
            if (AuditElements.EventCategoryType.TASK == eventCategoryTO.getType()
                    && "PullJobDelegate".equals(eventCategoryTO.getCategory())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    private static void checkLogFileFor(
            final Path path,
            final Function<String, Boolean> checker,
            final int maxWaitSeconds)
            throws IOException {

        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return checker.apply(Files.readString(path, StandardCharsets.UTF_8));
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    public void saveAuditEvent() {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setWho("syncope-user " + UUID.randomUUID().toString());
        auditEntry.setLogger(new AuditLoggerName(
                AuditElements.EventCategoryType.WA,
                null,
                AuditElements.AUTHENTICATION_CATEGORY.toUpperCase(),
                "validate",
                AuditElements.Result.SUCCESS));
        auditEntry.setDate(OffsetDateTime.now());
        auditEntry.setBefore(UUID.randomUUID().toString());
        auditEntry.setOutput(UUID.randomUUID().toString());
        assertDoesNotThrow(() -> AUDIT_SERVICE.create(auditEntry));

        if (IS_ELASTICSEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<AuditEntry> events = AUDIT_SERVICE.search(new AuditQuery.Builder().
                size(1).
                type(auditEntry.getLogger().getType()).
                category(auditEntry.getLogger().getCategory()).
                subcategory(auditEntry.getLogger().getSubcategory()).
                event(auditEntry.getLogger().getEvent()).
                result(auditEntry.getLogger().getResult()).
                build());
        assertNotNull(events);
        assertEquals(1, events.getSize());
    }

    @Test
    public void saveAuthEvent() {
        AuditEntry auditEntry = new AuditEntry();
        auditEntry.setWho("syncope-user " + UUID.randomUUID().toString());
        auditEntry.setLogger(new AuditLoggerName(
                AuditElements.EventCategoryType.WA,
                null,
                "AuthenticationEvent",
                "auth",
                AuditElements.Result.SUCCESS));
        auditEntry.setDate(OffsetDateTime.now());
        auditEntry.setBefore(UUID.randomUUID().toString());
        auditEntry.setOutput(UUID.randomUUID().toString());
        assertDoesNotThrow(() -> AUDIT_SERVICE.create(auditEntry));

        if (IS_ELASTICSEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<AuditEntry> events = AUDIT_SERVICE.search(new AuditQuery.Builder().
                size(1).
                type(auditEntry.getLogger().getType()).
                category(auditEntry.getLogger().getCategory()).
                subcategory(auditEntry.getLogger().getSubcategory()).
                event(auditEntry.getLogger().getEvent()).
                result(auditEntry.getLogger().getResult()).
                build());
        assertNotNull(events);
        assertEquals(1, events.getSize());
    }

    @Test
    public void customAuditAppender() throws IOException, InterruptedException {
        try (InputStream propStream = getClass().getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            Path auditFilePath = Paths.get(props.getProperty("test.log.dir")
                    + File.separator + "audit_for_Master_file.log");
            Files.write(auditFilePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);

            Path auditNoRewriteFilePath = Paths.get(props.getProperty("test.log.dir")
                    + File.separator + "audit_for_Master_norewrite_file.log");
            Files.write(auditNoRewriteFilePath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);

            // check that resource update is transformed and logged onto an audit file.
            ResourceTO resource = RESOURCE_SERVICE.read(RESOURCE_NAME_CSV);
            assertNotNull(resource);
            resource.setPropagationPriority(100);
            RESOURCE_SERVICE.update(resource);

            ConnInstanceTO connector = CONNECTOR_SERVICE.readByResource(RESOURCE_NAME_CSV, null);
            assertNotNull(connector);
            connector.setPoolConf(new ConnPoolConfTO());
            CONNECTOR_SERVICE.update(connector);

            // check audit_for_Master_file.log, it should contain only a static message
            checkLogFileFor(
                    auditFilePath,
                    content -> content.contains(
                            "DEBUG Master.syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]"
                            + " - This is a static test message"),
                    10);

            // nothing expected in audit_for_Master_norewrite_file.log instead
            checkLogFileFor(
                    auditNoRewriteFilePath,
                    content -> !content.contains(
                            "DEBUG Master.syncope.audit.[LOGIC]:[ResourceLogic]:[]:[update]:[SUCCESS]"
                            + " - This is a static test message"),
                    10);
        } catch (IOException e) {
            fail("Unable to read/write log files", e);
        }
    }

    @Test
    public void issueSYNCOPE976() {
        List<EventCategory> events = AUDIT_SERVICE.events();
        assertNotNull(events);

        EventCategory userLogic = events.stream().
                filter(object -> "UserLogic".equals(object.getCategory())).findAny().get();
        assertNotNull(userLogic);
        assertEquals(1, userLogic.getEvents().stream().filter("create"::equals).count());
    }

    @Test
    public void issueSYNCOPE1446() {
        AuditLoggerName createSuccess = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "create",
                AuditElements.Result.SUCCESS);
        AuditLoggerName createFailure = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "create",
                AuditElements.Result.FAILURE);
        AuditLoggerName updateSuccess = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "update",
                AuditElements.Result.SUCCESS);
        AuditLoggerName updateFailure = new AuditLoggerName(
                AuditElements.EventCategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "update",
                AuditElements.Result.FAILURE);
        try {
            // 1. setup audit for propagation
            AuditConfTO audit = new AuditConfTO();
            audit.setKey(createSuccess.toAuditKey());
            audit.setActive(true);
            AUDIT_SERVICE.set(audit);

            audit.setKey(createFailure.toAuditKey());
            AUDIT_SERVICE.set(audit);

            audit.setKey(updateSuccess.toAuditKey());
            AUDIT_SERVICE.set(audit);

            audit.setKey(updateFailure.toAuditKey());
            AUDIT_SERVICE.set(audit);

            // 2. push on resource
            PushTaskTO pushTask = new PushTaskTO();
            pushTask.setPerformCreate(true);
            pushTask.setPerformUpdate(true);
            pushTask.setUnmatchingRule(UnmatchingRule.PROVISION);
            pushTask.setMatchingRule(MatchingRule.UPDATE);
            RECONCILIATION_SERVICE.push(new ReconQuery.Builder(PRINTER, RESOURCE_NAME_DBSCRIPTED).
                    anyKey("fc6dbc3a-6c07-4965-8781-921e7401a4a5").build(), pushTask);
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            fail(e::getMessage);
        } finally {
            try {
                AUDIT_SERVICE.delete(createSuccess.toAuditKey());
            } catch (Exception e) {
                // ignore
            }
            try {
                AUDIT_SERVICE.delete(createFailure.toAuditKey());
            } catch (Exception e) {
                // ignore
            }
            try {
                AUDIT_SERVICE.delete(updateSuccess.toAuditKey());
            } catch (Exception e) {
                // ignore
            }
            try {
                AUDIT_SERVICE.delete(updateFailure.toAuditKey());
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1695() {
        // add audit conf for pull
        AUDIT_SERVICE.set(
                buildAuditConf("syncope.audit.[PULL]:[USER]:[resource-ldap]:[matchingrule_update]:[SUCCESS]", true));
        AUDIT_SERVICE.set(
                buildAuditConf("syncope.audit.[PULL]:[USER]:[resource-ldap]:[unmatchingrule_assign]:[SUCCESS]", true));
        AUDIT_SERVICE.set(
                buildAuditConf("syncope.audit.[PULL]:[USER]:[resource-ldap]:[unmatchingrule_provision]:[SUCCESS]",
                        true));
        UserTO pullFromLDAP = null;
        try {
            // pull from resource-ldap -> generates an audit entry
            PullTaskTO pullTaskTO = new PullTaskTO();
            pullTaskTO.setPerformCreate(true);
            pullTaskTO.setPerformUpdate(true);
            pullTaskTO.getActions().add("LDAPMembershipPullActions");
            pullTaskTO.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            pullTaskTO.setMatchingRule(MatchingRule.UPDATE);
            pullTaskTO.setUnmatchingRule(UnmatchingRule.ASSIGN);
            RECONCILIATION_SERVICE.pull(new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).
                    fiql("uid==pullFromLDAP").build(), pullTaskTO);

            // update pullTaskTO -> another audit entry
            pullFromLDAP = updateUser(new UserUR.Builder(USER_SERVICE.read("pullFromLDAP").getKey()).
                    plainAttr(new AttrPatch.Builder(new Attr.Builder("ctype").value("abcdef").build()).build()).
                    build()).getEntity();

            // search by empty type and category events and get both events on testfromLDAP
            if (IS_ELASTICSEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            assertEquals(2, AUDIT_SERVICE.search(new AuditQuery.Builder().
                    entityKey(pullFromLDAP.getKey()).
                    page(1).
                    size(10).
                    events(List.of(
                            "create", "update", "matchingrule_update", "unmatchingrule_assign",
                            "unmatchingrule_provision")).
                    result(AuditElements.Result.SUCCESS).
                    build()).getTotalCount());
        } finally {
            if (pullFromLDAP != null) {
                USER_SERVICE.deassociate(new ResourceDR.Builder()
                        .key(pullFromLDAP.getKey())
                        .resource(RESOURCE_NAME_LDAP)
                        .action(ResourceDeassociationAction.UNLINK)
                        .build());
                USER_SERVICE.delete(pullFromLDAP.getKey());

                // restore previous audit
                AUDIT_SERVICE.set(
                        buildAuditConf("syncope.audit.[PULL]:[USER]:[resource-ldap]:[matchingrule_update]:[SUCCESS]",
                                false));
                AUDIT_SERVICE.set(
                        buildAuditConf("syncope.audit.[PULL]:[USER]:[resource-ldap]:[unmatchingrule_assign]:[SUCCESS]",
                                false));
                AUDIT_SERVICE.set(buildAuditConf(
                        "syncope.audit.[PULL]:[USER]:[resource-ldap]:[unmatchingrule_provision]:[SUCCESS]",
                        false));
            }
        }
    }
}
