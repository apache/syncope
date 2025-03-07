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
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.RESTHeaders;
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

    private static AuditEventTO queryWithFailure(final AuditQuery query, final int maxWaitSeconds) {
        List<AuditEventTO> results = query(query, maxWaitSeconds);
        if (results.isEmpty()) {
            fail(() -> "Timeout when executing query for key " + query.getEntityKey());
            return null;
        }
        return results.getFirst();
    }

    @Test
    public void userReadAndSearchYieldsNoAudit() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder().entityKey(userTO.getKey()).build();
        int entriesBefore = query(query, MAX_WAIT_SECONDS).size();

        PagedResult<UserTO> usersTOs = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().
                                is("username").equalTo(userTO.getUsername()).query()).
                        build());
        assertNotNull(usersTOs);
        assertFalse(usersTOs.getResult().isEmpty());

        int entriesAfter = query(query, MAX_WAIT_SECONDS).size();
        assertEquals(entriesBefore, entriesAfter);
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
                orderBy("when desc").
                build();
        AuditEventTO entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        USER_SERVICE.delete(userTO.getKey());
    }

    @Test
    public void findByUserAndOther() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit-2@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder().
                entityKey(userTO.getKey()).
                orderBy("when desc").
                page(1).
                size(1).
                type(OpEvent.CategoryType.LOGIC).
                category(UserLogic.class.getSimpleName()).
                op("create").
                outcome(OpEvent.Outcome.SUCCESS).
                after(OffsetDateTime.now().minusSeconds(30)).
                build();
        AuditEventTO entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        USER_SERVICE.delete(userTO.getKey());
    }

    @Test
    public void findByGroup() {
        GroupTO groupTO = createGroup(GroupITCase.getBasicSample("AuditGroup")).getEntity();
        assertNotNull(groupTO.getKey());

        AuditQuery query = new AuditQuery.Builder().entityKey(groupTO.getKey()).orderBy("when desc").
                page(1).size(1).build();
        AuditEventTO entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        GROUP_SERVICE.delete(groupTO.getKey());
    }

    @Test
    public void groupReadAndSearchYieldsNoAudit() {
        GroupTO groupTO = createGroup(GroupITCase.getBasicSample("AuditGroupSearch")).getEntity();
        assertNotNull(groupTO.getKey());

        AuditQuery query = new AuditQuery.Builder().entityKey(groupTO.getKey()).build();
        int entriesBefore = query(query, MAX_WAIT_SECONDS).size();

        PagedResult<GroupTO> groups = GROUP_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo(groupTO.getName()).query()).
                build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());

        int entriesAfter = query(query, MAX_WAIT_SECONDS).size();
        assertEquals(entriesBefore, entriesAfter);
    }

    @Test
    public void findByAnyObject() {
        AnyObjectTO anyObjectTO = createAnyObject(AnyObjectITCase.getSample("Italy")).getEntity();
        assertNotNull(anyObjectTO.getKey());
        AuditQuery query = new AuditQuery.Builder().entityKey(anyObjectTO.getKey()).
                orderBy("when desc").page(1).size(1).build();
        AuditEventTO entry = queryWithFailure(query, MAX_WAIT_SECONDS);
        assertNotNull(entry);
        ANY_OBJECT_SERVICE.delete(anyObjectTO.getKey());
    }

    @Test
    public void anyObjectReadAndSearchYieldsNoAudit() {
        AnyObjectTO anyObjectTO = createAnyObject(AnyObjectITCase.getSample("USA")).getEntity();
        assertNotNull(anyObjectTO);

        AuditQuery query = new AuditQuery.Builder().entityKey(anyObjectTO.getKey()).build();
        int entriesBefore = query(query, MAX_WAIT_SECONDS).size();

        PagedResult<AnyObjectTO> anyObjects = ANY_OBJECT_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(anyObjectTO.getType()).query()).
                        build());
        assertNotNull(anyObjects);
        assertFalse(anyObjects.getResult().isEmpty());

        int entriesAfter = query(query, MAX_WAIT_SECONDS).size();
        assertEquals(entriesBefore, entriesAfter);
    }

    @Test
    public void findByConnector() throws JsonProcessingException {
        String connectorKey = "74141a3b-0762-4720-a4aa-fc3e374ef3ef";

        AuditQuery query = new AuditQuery.Builder().
                entityKey(connectorKey).
                orderBy("when desc").
                type(OpEvent.CategoryType.LOGIC).
                category(ConnectorLogic.class.getSimpleName()).
                op("update").
                outcome(OpEvent.Outcome.SUCCESS).
                build();
        List<AuditEventTO> entries = AUDIT_SERVICE.search(query).getResult();
        int pre = entries.size();

        ConnInstanceTO ldapConn = CONNECTOR_SERVICE.read(connectorKey, null);
        String originalDisplayName = ldapConn.getDisplayName();
        Set<ConnectorCapability> originalCapabilities = new HashSet<>(ldapConn.getCapabilities());
        ConnConfProperty originalConfProp = SerializationUtils.clone(
                ldapConn.getConf("maintainPosixGroupMembership").get());
        assertEquals(1, originalConfProp.getValues().size());
        assertEquals("false", originalConfProp.getValues().getFirst());

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

        ConnInstanceTO restore = JSON_MAPPER.readValue(entries.getFirst().getBefore(), ConnInstanceTO.class);
        CONNECTOR_SERVICE.update(restore);

        ldapConn = CONNECTOR_SERVICE.read(connectorKey, null);
        assertEquals(originalDisplayName, ldapConn.getDisplayName());
        assertEquals(originalCapabilities, ldapConn.getCapabilities());
        assertEquals(originalConfProp, ldapConn.getConf("maintainPosixGroupMembership").get());
    }

    @Test
    public void enableDisable() {
        OpEvent opEvent = new OpEvent(
                OpEvent.CategoryType.LOGIC,
                ReportLogic.class.getSimpleName(),
                null,
                "deleteExecution",
                OpEvent.Outcome.FAILURE);

        List<AuditConfTO> audits = AUDIT_SERVICE.confs();
        assertFalse(audits.stream().anyMatch(a -> a.getKey().equals(opEvent.toString())));

        AuditConfTO audit = new AuditConfTO();
        audit.setKey(opEvent.toString());
        audit.setActive(true);
        AUDIT_SERVICE.setConf(audit);

        audits = AUDIT_SERVICE.confs();
        assertTrue(audits.stream().anyMatch(a -> a.getKey().equals(opEvent.toString())));

        AUDIT_SERVICE.deleteConf(audit.getKey());

        audits = AUDIT_SERVICE.confs();
        assertFalse(audits.stream().anyMatch(a -> a.getKey().equals(opEvent.toString())));
    }

    @Test
    public void listOpEvents() {
        List<OpEvent> opEvents = AUDIT_SERVICE.events();

        assertTrue(opEvents.stream().
                filter(opEvent -> UserLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                anyMatch(opEvent -> "create".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> UserLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                anyMatch(opEvent -> "search".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> UserLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                noneMatch(opEvent -> "doCreate".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> UserLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                noneMatch(opEvent -> "setStatusOnWfAdapter".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> UserLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                noneMatch(opEvent -> "resolveReference".equals(opEvent.getOp())));

        assertTrue(opEvents.stream().
                filter(opEvent -> GroupLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                anyMatch(opEvent -> "create".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> GroupLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                anyMatch(opEvent -> "search".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> GroupLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                noneMatch(opEvent -> "resolveReference".equals(opEvent.getOp())));

        assertTrue(opEvents.stream().
                filter(opEvent -> ResourceLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                anyMatch(opEvent -> "create".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> ResourceLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                anyMatch(opEvent -> "read".equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> ResourceLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                anyMatch(opEvent -> ResourceOperation.DELETE.name().toLowerCase().equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> ResourceLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())).
                noneMatch(opEvent -> "resolveReference".equals(opEvent.getOp())));

        assertTrue(opEvents.stream().
                filter(opEvent -> AnyTypeKind.USER.name().equals(opEvent.getCategory())
                && RESOURCE_NAME_LDAP.equals(opEvent.getSubcategory())
                && OpEvent.CategoryType.PULL == opEvent.getType()).
                anyMatch(opEvent -> ResourceOperation.DELETE.name().toLowerCase().equals(opEvent.getOp())));

        assertTrue(opEvents.stream().
                filter(opEvent -> AnyTypeKind.USER.name().equals(opEvent.getCategory())
                && RESOURCE_NAME_CSV.equals(opEvent.getSubcategory())
                && OpEvent.CategoryType.PROPAGATION == opEvent.getType()).
                anyMatch(opEvent -> ResourceOperation.CREATE.name().toLowerCase().equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> AnyTypeKind.USER.name().equals(opEvent.getCategory())
                && RESOURCE_NAME_CSV.equals(opEvent.getSubcategory())
                && OpEvent.CategoryType.PROPAGATION == opEvent.getType()).
                anyMatch(opEvent -> ResourceOperation.UPDATE.name().toLowerCase().equals(opEvent.getOp())));
        assertTrue(opEvents.stream().
                filter(opEvent -> AnyTypeKind.USER.name().equals(opEvent.getCategory())
                && RESOURCE_NAME_CSV.equals(opEvent.getSubcategory())
                && OpEvent.CategoryType.PROPAGATION == opEvent.getType()).
                anyMatch(opEvent -> ResourceOperation.DELETE.name().toLowerCase().equals(opEvent.getOp())));

        assertTrue(opEvents.stream().
                anyMatch(opEvent -> OpEvent.CategoryType.TASK == opEvent.getType()
                && "PullJobDelegate".equals(opEvent.getCategory())));
    }

    @Test
    public void saveAuditEvent() {
        AuditEventTO auditEvent = new AuditEventTO();
        auditEvent.setOpEvent(new OpEvent(
                OpEvent.CategoryType.WA,
                OpEvent.AUTHENTICATION_CATEGORY,
                null,
                "validate",
                OpEvent.Outcome.SUCCESS));
        auditEvent.setWho("syncope-user " + UUID.randomUUID());
        auditEvent.setWhen(OffsetDateTime.now());
        auditEvent.setBefore(UUID.randomUUID().toString());
        auditEvent.setOutput(UUID.randomUUID().toString());
        assertDoesNotThrow(() -> AUDIT_SERVICE.create(auditEvent));

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<AuditEventTO> events = AUDIT_SERVICE.search(new AuditQuery.Builder().
                size(1).
                type(auditEvent.getOpEvent().getType()).
                category(auditEvent.getOpEvent().getCategory()).
                subcategory(auditEvent.getOpEvent().getSubcategory()).
                op(auditEvent.getOpEvent().getOp()).
                outcome(auditEvent.getOpEvent().getOutcome()).
                build());
        assertNotNull(events);
        assertEquals(1, events.getSize());
    }

    @Test
    public void saveAuthEvent() {
        AuditEventTO auditEvent = new AuditEventTO();
        auditEvent.setOpEvent(new OpEvent(
                OpEvent.CategoryType.WA,
                null,
                "AuthenticationEvent",
                "auth",
                OpEvent.Outcome.SUCCESS));
        auditEvent.setWho("syncope-user " + UUID.randomUUID());
        auditEvent.setWhen(OffsetDateTime.now());
        auditEvent.setBefore(UUID.randomUUID().toString());
        auditEvent.setOutput(UUID.randomUUID().toString());
        assertDoesNotThrow(() -> AUDIT_SERVICE.create(auditEvent));

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<AuditEventTO> events = AUDIT_SERVICE.search(new AuditQuery.Builder().
                size(1).
                type(auditEvent.getOpEvent().getType()).
                category(auditEvent.getOpEvent().getCategory()).
                subcategory(auditEvent.getOpEvent().getSubcategory()).
                op(auditEvent.getOpEvent().getOp()).
                outcome(auditEvent.getOpEvent().getOutcome()).
                build());
        assertNotNull(events);
        assertEquals(1, events.getSize());
    }

    @Test
    public void auditEventProcessor() throws IOException, InterruptedException {
        try (InputStream propStream = getClass().getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            Path auditFilePath = Path.of(props.getProperty("test.log.dir") + File.separator + "audit_for_Master_file");

            // check that resource update is transformed and logged onto an audit file.
            ResourceTO resource = RESOURCE_SERVICE.read(RESOURCE_NAME_CSV);
            assertNotNull(resource);
            resource.setPropagationPriority(100);
            RESOURCE_SERVICE.update(resource);

            ConnInstanceTO connector = CONNECTOR_SERVICE.readByResource(RESOURCE_NAME_CSV, null);
            assertNotNull(connector);
            connector.setPoolConf(new ConnPoolConf());
            CONNECTOR_SERVICE.update(connector);

            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                try {
                    return Files.readString(auditFilePath, StandardCharsets.UTF_8).contains(
                            "{\"type\":\"LOGIC\",\"category\":\"ConnectorLogic\",\"subcategory\":null,"
                            + "\"op\":\"update\",\"outcome\":\"SUCCESS\"}");
                } catch (Exception e) {
                    LOG.error("Could not check content of {}", auditFilePath, e);
                    return false;
                }
            });
        } catch (IOException e) {
            fail("Unable to read/write custom event processor output file", e);
        }
    }

    @Test
    public void issueSYNCOPE976() {
        List<OpEvent> opEvents = AUDIT_SERVICE.events();
        assertNotNull(opEvents);

        assertEquals(1, opEvents.stream().
                filter(opEvent -> UserLogic.class.getSimpleName().equals(opEvent.getCategory())
                && OpEvent.CategoryType.LOGIC.equals(opEvent.getType())
                && "create".equals(opEvent.getOp())
                && OpEvent.Outcome.SUCCESS == opEvent.getOutcome()).count());
    }

    @Test
    public void issueSYNCOPE1446() {
        OpEvent createSuccess = new OpEvent(
                OpEvent.CategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "create",
                OpEvent.Outcome.SUCCESS);
        OpEvent createFailure = new OpEvent(
                OpEvent.CategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "create",
                OpEvent.Outcome.FAILURE);
        OpEvent updateSuccess = new OpEvent(
                OpEvent.CategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "update",
                OpEvent.Outcome.SUCCESS);
        OpEvent updateFailure = new OpEvent(
                OpEvent.CategoryType.PROPAGATION,
                AnyTypeKind.ANY_OBJECT.name(),
                RESOURCE_NAME_DBSCRIPTED,
                "update",
                OpEvent.Outcome.FAILURE);
        try {
            // 1. setup audit for propagation
            AuditConfTO audit = new AuditConfTO();
            audit.setKey(createSuccess.toString());
            audit.setActive(true);
            AUDIT_SERVICE.setConf(audit);

            audit.setKey(createFailure.toString());
            AUDIT_SERVICE.setConf(audit);

            audit.setKey(updateSuccess.toString());
            AUDIT_SERVICE.setConf(audit);

            audit.setKey(updateFailure.toString());
            AUDIT_SERVICE.setConf(audit);

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
                AUDIT_SERVICE.deleteConf(createSuccess.toString());
            } catch (Exception e) {
                // ignore
            }
            try {
                AUDIT_SERVICE.deleteConf(createFailure.toString());
            } catch (Exception e) {
                // ignore
            }
            try {
                AUDIT_SERVICE.deleteConf(updateSuccess.toString());
            } catch (Exception e) {
                // ignore
            }
            try {
                AUDIT_SERVICE.deleteConf(updateFailure.toString());
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1695() {
        // add audit conf for pull
        AUDIT_SERVICE.setConf(
                buildAuditConf("[PULL]:[USER]:[resource-ldap]:[matchingrule_update]:[SUCCESS]", true));
        AUDIT_SERVICE.setConf(
                buildAuditConf("[PULL]:[USER]:[resource-ldap]:[unmatchingrule_assign]:[SUCCESS]", true));
        AUDIT_SERVICE.setConf(
                buildAuditConf("[PULL]:[USER]:[resource-ldap]:[unmatchingrule_provision]:[SUCCESS]", true));

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
            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            assertEquals(1, AUDIT_SERVICE.search(new AuditQuery.Builder().
                    entityKey(pullFromLDAP.getKey()).
                    page(1).
                    size(10).
                    outcome(OpEvent.Outcome.SUCCESS).
                    build()).getResult().stream().
                    filter(e -> Set.of("matchingrule_update", "unmatchingrule_assign", "unmatchingrule_provision").
                    contains(e.getOpEvent().getOp())).count());
        } finally {
            if (pullFromLDAP != null) {
                USER_SERVICE.deassociate(new ResourceDR.Builder()
                        .key(pullFromLDAP.getKey())
                        .resource(RESOURCE_NAME_LDAP)
                        .action(ResourceDeassociationAction.UNLINK)
                        .build());
                USER_SERVICE.delete(pullFromLDAP.getKey());

                // restore previous audit
                AUDIT_SERVICE.setConf(buildAuditConf(
                        "[PULL]:[USER]:[resource-ldap]:[matchingrule_update]:[SUCCESS]", false));
                AUDIT_SERVICE.setConf(buildAuditConf(
                        "[PULL]:[USER]:[resource-ldap]:[unmatchingrule_assign]:[SUCCESS]", false));
                AUDIT_SERVICE.setConf(buildAuditConf(
                        "[PULL]:[USER]:[resource-ldap]:[unmatchingrule_provision]:[SUCCESS]", false));
            }
        }
    }

    @Test
    public void issueSYNCOPE1791() throws IOException {
        ImplementationTO logicActions;
        try {
            logicActions = IMPLEMENTATION_SERVICE.read(
                    IdRepoImplementationType.LOGIC_ACTIONS, "CustomAuditLogicActions");
        } catch (SyncopeClientException e) {
            logicActions = new ImplementationTO();
            logicActions.setKey("CustomAuditLogicActions");
            logicActions.setEngine(ImplementationEngine.GROOVY);
            logicActions.setType(IdRepoImplementationType.LOGIC_ACTIONS);
            logicActions.setBody(IOUtils.toString(
                    getClass().getResourceAsStream("/CustomAuditLogicActions.groovy"), StandardCharsets.UTF_8));
            Response response = IMPLEMENTATION_SERVICE.create(logicActions);
            logicActions = IMPLEMENTATION_SERVICE.read(
                    logicActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        }
        assertNotNull(logicActions);

        RealmTO root = getRealm(SyncopeConstants.ROOT_REALM).orElseThrow();
        root.getActions().add(logicActions.getKey());
        REALM_SERVICE.update(root);

        AuditQuery query = new AuditQuery.Builder().type(OpEvent.CategoryType.CUSTOM).build();
        int before = query(query, MAX_WAIT_SECONDS).size();
        try {
            AUDIT_SERVICE.setConf(buildAuditConf("[CUSTOM]:[]:[]:[MY_EVENT]:[SUCCESS]", true));

            AnyObjectTO printer = createAnyObject(AnyObjectITCase.getSample("syncope-1791")).getEntity();
            updateAnyObject(new AnyObjectUR.Builder(printer.getKey()).
                    plainAttr(attrAddReplacePatch("location", "new" + getUUIDString())).
                    build());

            int after = query(query, MAX_WAIT_SECONDS).size();
            assertEquals(before + 1, after);
        } finally {
            AUDIT_SERVICE.setConf(buildAuditConf("[CUSTOM]:[]:[]:[MY_EVENT]:[SUCCESS]", false));

            root.getActions().remove(logicActions.getKey());
            REALM_SERVICE.update(root);
        }
    }
}
