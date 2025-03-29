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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.InboundPolicyTO;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.LinkedAccountSampleInboundCorrelationRule;
import org.apache.syncope.fit.core.reference.LinkedAccountSampleInboundCorrelationRuleConf;
import org.junit.jupiter.api.Test;

public class LinkedAccountITCase extends AbstractITCase {

    @Test
    public void createWithLinkedAccountThenUpdateThenRemove() {
        // 1. create user with linked account
        UserCR userCR = UserITCase.getSample(
                "linkedAccount" + RandomStringUtils.insecure().nextNumeric(5) + "@syncope.apache.org");
        String connObjectKeyValue = "firstAccountOf" + userCR.getUsername();

        LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_LDAP, connObjectKeyValue).build();
        account.getPlainAttrs().add(attr("surname", "LINKED_SURNAME"));
        userCR.getLinkedAccounts().add(account);

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user.getKey());

        // 2. verify that propagation task was generated and that account is found on resource
        PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(1, tasks.getTotalCount());
        assertEquals(connObjectKeyValue, tasks.getResult().getFirst().getConnObjectKey());
        assertEquals(ResourceOperation.CREATE, tasks.getResult().getFirst().getOperation());
        assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().getFirst().getLatestExecStatus());

        ConnObject ldapObj = RESOURCE_SERVICE.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), connObjectKeyValue);
        assertNotNull(ldapObj);
        assertEquals(
                user.getPlainAttr("email").orElseThrow().getValues(),
                ldapObj.getAttr("mail").orElseThrow().getValues());
        assertEquals("LINKED_SURNAME", ldapObj.getAttr("sn").orElseThrow().getValues().getFirst());

        // 3. update linked account
        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());

        account = new LinkedAccountTO.Builder(RESOURCE_NAME_LDAP, connObjectKeyValue).build();
        account.getPlainAttrs().add(attr("email", "UPDATED_EMAIL@syncope.apache.org"));
        account.getPlainAttrs().add(attr("surname", "UPDATED_SURNAME"));
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());

        user = updateUser(userUR).getEntity();
        assertEquals(1, user.getLinkedAccounts().size());
        assertEquals("UPDATED_SURNAME", user.getLinkedAccounts().getFirst().
                getPlainAttr("surname").orElseThrow().getValues().getFirst());
        assertEquals("UPDATED_EMAIL@syncope.apache.org", user.getLinkedAccounts().getFirst().
                getPlainAttr("email").orElseThrow().getValues().getFirst());

        // 4 verify that account was updated on resource
        ldapObj = RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), connObjectKeyValue);
        assertNotNull(ldapObj);

        assertTrue(ldapObj.getAttr("mail").orElseThrow().getValues().contains("UPDATED_EMAIL@syncope.apache.org"));
        assertEquals("UPDATED_SURNAME", ldapObj.getAttr("sn").orElseThrow().getValues().getFirst());

        // 5. remove linked account from user
        userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                operation(PatchOperation.DELETE).
                linkedAccountTO(user.getLinkedAccounts().getFirst()).build());

        user = updateUser(userUR).getEntity();
        assertTrue(user.getLinkedAccounts().isEmpty());

        // 6. verify that propagation task was generated and that account is not any more on resource
        tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(3, tasks.getTotalCount());

        Optional<PropagationTaskTO> deletTask =
                tasks.getResult().stream().filter(task -> task.getOperation() == ResourceOperation.DELETE).findFirst();
        assertTrue(deletTask.isPresent());
        assertEquals(connObjectKeyValue, deletTask.orElseThrow().getConnObjectKey());
        assertEquals(ExecStatus.SUCCESS.name(), deletTask.orElseThrow().getLatestExecStatus());

        assertNull(getLdapRemoteObject("uid=" + connObjectKeyValue + ",ou=People,o=isp"));
    }

    @Test
    public void createWithoutLinkedAccountThenAdd() {
        // 1. create user without linked account
        UserCR userCR = UserITCase.getSample(
                "linkedAccount" + RandomStringUtils.insecure().nextNumeric(5) + "@syncope.apache.org");
        String connObjectKeyValue = "uid=" + userCR.getUsername() + ",ou=People,o=isp";

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user.getKey());
        assertTrue(user.getLinkedAccounts().isEmpty());

        PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(0, tasks.getTotalCount());

        assertNull(getLdapRemoteObject(connObjectKeyValue));

        // 2. add linked account to user
        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());

        LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_LDAP, connObjectKeyValue).build();
        account.getPlainAttrs().add(attr("surname", "LINKED_SURNAME"));
        account.setPassword("Password123");
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());

        user = updateUser(userUR).getEntity();
        assertEquals(1, user.getLinkedAccounts().size());

        // 3. verify that propagation task was generated and that account is found on resource
        tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(1, tasks.getTotalCount());
        assertEquals(connObjectKeyValue, tasks.getResult().getFirst().getConnObjectKey());
        assertEquals(ResourceOperation.CREATE, tasks.getResult().getFirst().getOperation());
        assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().getFirst().getLatestExecStatus());

        ConnObject ldapObj = RESOURCE_SERVICE.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), connObjectKeyValue);
        assertNotNull(ldapObj);
        assertEquals(
                user.getPlainAttr("email").orElseThrow().getValues(),
                ldapObj.getAttr("mail").orElseThrow().getValues());
        assertEquals("LINKED_SURNAME", ldapObj.getAttr("sn").orElseThrow().getValues().getFirst());
    }

    @Test
    public void createWithoutLinkedAccountThenAddAndUpdatePassword() {
        // 1. set the return value parameter to true
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "return.password.value", true);

        // 2. create user without linked account
        UserCR userCR = UserITCase.getSample(
                "linkedAccount" + RandomStringUtils.insecure().nextNumeric(5) + "@syncope.apache.org");
        String connObjectKeyValue = "uid=" + userCR.getUsername() + ",ou=People,o=isp";

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user.getKey());
        assertTrue(user.getLinkedAccounts().isEmpty());

        // 3. add linked account to user without password
        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());

        LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_LDAP, connObjectKeyValue).build();
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());

        user = updateUser(userUR).getEntity();
        assertEquals(1, user.getLinkedAccounts().size());
        assertNull(user.getLinkedAccounts().getFirst().getPassword());

        // 4. update linked account with adding a password
        account.setPassword("Password123");
        userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());

        // 4.1 SYNCOPE-1824 update with a wrong password, a error must be raised
        account.setPassword("password");
        try {
            updateUser(userUR);
            fail("Should not arrive here due to wrong linked account password");
        } catch (SyncopeClientException sce) {
            assertEquals(ClientExceptionType.InvalidUser, sce.getType());
            assertEquals("InvalidUser [InvalidPassword: Password must be 10 or more characters in length.]",
                    sce.getMessage());
        }

        // set a correct password
        account.setPassword("Password123");
        user = updateUser(userUR).getEntity();
        assertNotNull(user.getLinkedAccounts().getFirst().getPassword());

        // 5. update linked account  password
        String beforeUpdatePassword = user.getLinkedAccounts().getFirst().getPassword();
        account.setPassword("Password123Updated");
        userUR = new UserUR();
        userUR.setKey(user.getKey());

        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());
        user = updateUser(userUR).getEntity();
        assertNotNull(user.getLinkedAccounts().getFirst().getPassword());
        assertNotEquals(beforeUpdatePassword, user.getLinkedAccounts().getFirst().getPassword());

        // 6. set linked account password to null
        account.setPassword(null);
        userUR = new UserUR();
        userUR.setKey(user.getKey());

        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());
        user = updateUser(userUR).getEntity();
        assertNull(user.getLinkedAccounts().getFirst().getPassword());

        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "return.password.value", false);
    }

    @Test
    public void push() {
        assumeFalse(IS_EXT_SEARCH_ENABLED);

        // 0a. read configured cipher algorithm in order to be able to restore it at the end of test
        String origpwdCipherAlgo = confParamOps.get(SyncopeConstants.MASTER_DOMAIN,
                "password.cipher.algorithm", null, String.class);

        // 0b. set AES password cipher algorithm
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", "AES");

        String userKey = null;
        String connObjectKeyValue = UUID.randomUUID().toString();
        try {
            // 1. create user with linked account
            UserCR userCR = UserITCase.getSample(
                    "linkedAccount" + RandomStringUtils.insecure().nextNumeric(5) + "@syncope.apache.org");

            LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_REST, connObjectKeyValue).build();
            userCR.getLinkedAccounts().add(account);

            UserTO user = createUser(userCR).getEntity();
            userKey = user.getKey();
            assertNotNull(userKey);
            assertNotEquals(userKey, connObjectKeyValue);

            // 2. verify that account is found on resource
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(
                    new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_REST).
                            anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(1, tasks.getTotalCount());
            assertEquals(connObjectKeyValue, tasks.getResult().getFirst().getConnObjectKey());
            assertEquals(ResourceOperation.CREATE, tasks.getResult().getFirst().getOperation());
            assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().getFirst().getLatestExecStatus());

            WebClient webClient = WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/users/" + connObjectKeyValue).
                    accept(MediaType.APPLICATION_JSON_TYPE);
            Response response = webClient.get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // 3. remove account from resource
            response = webClient.delete();
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

            response = webClient.get();
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

            // 4. create PushTask for the user above
            PushTaskTO sendUser = new PushTaskTO();
            sendUser.setName("Send User " + user.getUsername());
            sendUser.setResource(RESOURCE_NAME_REST);
            sendUser.setUnmatchingRule(UnmatchingRule.PROVISION);
            sendUser.setMatchingRule(MatchingRule.UPDATE);
            sendUser.setSourceRealm(SyncopeConstants.ROOT_REALM);
            sendUser.getFilters().put(AnyTypeKind.USER.name(), "username==" + user.getUsername());
            sendUser.setPerformCreate(true);
            sendUser.setPerformUpdate(true);

            response = TASK_SERVICE.create(TaskType.PUSH, sendUser);
            sendUser = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
            assertNotNull(sendUser);

            // 5. execute PushTask
            AbstractTaskITCase.execSchedTask(
                    TASK_SERVICE, TaskType.PUSH, sendUser.getKey(), MAX_WAIT_SECONDS, false);

            TaskTO task = TASK_SERVICE.read(TaskType.PUSH, sendUser.getKey(), true);
            assertEquals(1, task.getExecutions().size());
            assertEquals(ExecStatus.SUCCESS.name(), task.getExecutions().getFirst().getStatus());

            await().until(() -> TASK_SERVICE.search(
                    new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_REST)
                            .anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build()).getTotalCount() == 3);

            // 6. verify that both user and account are now found on resource
            response = webClient.get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            webClient = WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/users/" + userKey).
                    accept(MediaType.APPLICATION_JSON_TYPE);
            response = webClient.get();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        } finally {
            // restore initial cipher algorithm
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", origpwdCipherAlgo);

            // delete user and accounts
            if (userKey != null) {
                WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/users/" + connObjectKeyValue).delete();
                WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/users/" + userKey).delete();

                USER_SERVICE.delete(userKey);
            }
        }
    }

    @Test
    public void pull() {
        // -----------------------------
        // Add a custom policy with correlation rule
        // -----------------------------
        ResourceTO restResource = RESOURCE_SERVICE.read(RESOURCE_NAME_REST);
        if (restResource.getInboundPolicy() == null) {
            ImplementationTO rule = null;
            try {
                rule = IMPLEMENTATION_SERVICE.read(
                        IdMImplementationType.INBOUND_CORRELATION_RULE,
                        "LinkedAccountSampleInboundCorrelationrrelationRule");
            } catch (SyncopeClientException e) {
                if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                    rule = new ImplementationTO();
                    rule.setKey("LinkedAccountSampleInboundCorrelationrrelationRule");
                    rule.setEngine(ImplementationEngine.JAVA);
                    rule.setType(IdMImplementationType.INBOUND_CORRELATION_RULE);
                    rule.setBody(POJOHelper.serialize(new LinkedAccountSampleInboundCorrelationRuleConf()));
                    Response response = IMPLEMENTATION_SERVICE.create(rule);
                    rule = IMPLEMENTATION_SERVICE.read(
                            rule.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                    assertNotNull(rule.getKey());
                }
            }
            assertNotNull(rule);

            InboundPolicyTO policy = new InboundPolicyTO();
            policy.setName("Linked Account sample inbound policy");
            policy.getCorrelationRules().put(AnyTypeKind.USER.name(), rule.getKey());
            Response response = POLICY_SERVICE.create(PolicyType.INBOUND, policy);
            policy = POLICY_SERVICE.read(PolicyType.INBOUND, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
            assertNotNull(policy.getKey());

            restResource.setInboundPolicy(policy.getKey());
            RESOURCE_SERVICE.update(restResource);
        }

        // -----------------------------
        // -----------------------------
        // Add a pull task
        // -----------------------------
        String pullTaskKey;

        PagedResult<PullTaskTO> tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PULL).resource(RESOURCE_NAME_REST).build());
        if (tasks.getTotalCount() > 0) {
            pullTaskKey = tasks.getResult().getFirst().getKey();
        } else {
            PullTaskTO task = new PullTaskTO();
            task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            task.setName("Linked Account Pull Task");
            task.setActive(true);
            task.setResource(RESOURCE_NAME_REST);
            task.setPullMode(PullMode.INCREMENTAL);
            task.setPerformCreate(true);
            task.setPerformUpdate(true);
            task.setPerformDelete(true);
            task.setSyncStatus(true);

            Response response = TASK_SERVICE.create(TaskType.PULL, task);
            task = TASK_SERVICE.read(TaskType.PULL, response.getHeaderString(RESTHeaders.RESOURCE_KEY), false);
            assertNotNull(task.getKey());
            pullTaskKey = task.getKey();
        }
        assertNotNull(pullTaskKey);
        // -----------------------------

        // 1. create REST users
        WebClient webClient = WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/users").
                accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE);

        ObjectNode user = JSON_MAPPER.createObjectNode();
        user.put("username", "linkedaccount1");
        user.put("password", "Password123");
        user.put("firstName", "Pasquale");
        user.put("surname", "Vivaldi");
        user.put("email", "vivaldi@syncope.org");

        Response response = webClient.post(user.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        String user1Key = StringUtils.substringAfterLast(response.getHeaderString(HttpHeaders.LOCATION), "/");
        assertNotNull(user1Key);

        user = JSON_MAPPER.createObjectNode();
        user.put("username", "vivaldi");
        user.put("password", "Password123");
        user.put("firstName", "Giovannino");
        user.put("surname", "Vivaldi");
        user.put("email", "vivaldi@syncope.org");

        response = webClient.post(user.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        String user2Key = StringUtils.substringAfterLast(response.getHeaderString(HttpHeaders.LOCATION), "/");
        assertNotNull(user2Key);

        user = JSON_MAPPER.createObjectNode();
        user.put("username", "not.vivaldi");
        user.put("password", "Password123");
        user.put("email", "not.vivaldi@syncope.org");

        response = webClient.post(user.toString());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        String user3Key = StringUtils.substringAfterLast(response.getHeaderString(HttpHeaders.LOCATION), "/");
        assertNotNull(user3Key);

        // 2. execute pull task and verify linked accounts were pulled
        try {
            List<LinkedAccountTO> accounts = USER_SERVICE.read("vivaldi").getLinkedAccounts();
            assertTrue(accounts.isEmpty());

            ExecTO exec = AbstractTaskITCase.execSchedTask(
                    TASK_SERVICE, TaskType.PULL, pullTaskKey, MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));
            assertTrue(exec.getMessage().contains("Accounts created"));

            accounts = USER_SERVICE.read("vivaldi").getLinkedAccounts();
            assertEquals(3, accounts.size());

            Optional<LinkedAccountTO> firstAccount = accounts.stream().
                    filter(account -> user1Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertTrue(firstAccount.isPresent());
            assertFalse(firstAccount.orElseThrow().isSuspended());
            assertEquals(RESOURCE_NAME_REST, firstAccount.orElseThrow().getResource());
            assertEquals("linkedaccount1", firstAccount.orElseThrow().getUsername());
            assertEquals(
                    "Pasquale",
                    firstAccount.orElseThrow().getPlainAttr("firstname").orElseThrow().getValues().getFirst());

            Optional<LinkedAccountTO> secondAccount = accounts.stream().
                    filter(account -> user2Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertTrue(secondAccount.isPresent());
            assertFalse(secondAccount.orElseThrow().isSuspended());
            assertEquals(RESOURCE_NAME_REST, secondAccount.orElseThrow().getResource());
            assertNull(secondAccount.orElseThrow().getUsername());
            assertEquals(
                    "Giovannino",
                    secondAccount.orElseThrow().getPlainAttr("firstname").orElseThrow().getValues().getFirst());

            Optional<LinkedAccountTO> thirdAccount = accounts.stream().
                    filter(account -> user3Key.equals(account.getConnObjectKeyValue())).
                    filter(account -> "not.vivaldi".equals(account.getUsername())).
                    findFirst();
            assertTrue(thirdAccount.isPresent());
            assertFalse(thirdAccount.orElseThrow().isSuspended());
            assertEquals(RESOURCE_NAME_REST, thirdAccount.orElseThrow().getResource());
            assertEquals("not.vivaldi", thirdAccount.orElseThrow().getUsername());

            // 3. update / remove REST users
            response = webClient.path(user1Key).delete();
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

            user = JSON_MAPPER.createObjectNode();
            user.put("username", "linkedaccount2");
            response = webClient.replacePath(user2Key).put(user.toString());
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

            user = JSON_MAPPER.createObjectNode();
            user.put("status", "INACTIVE");
            response = webClient.replacePath(user3Key).put(user.toString());
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

            // 4. execute pull task again and verify linked accounts were pulled
            exec = AbstractTaskITCase.execSchedTask(
                    TASK_SERVICE, TaskType.PULL, pullTaskKey, MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));
            assertTrue(exec.getMessage().contains("Accounts updated"));
            assertTrue(exec.getMessage().contains("Accounts deleted"));

            accounts = USER_SERVICE.read("vivaldi").getLinkedAccounts();
            assertEquals(2, accounts.size());

            firstAccount = accounts.stream().
                    filter(account -> user1Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertFalse(firstAccount.isPresent());

            secondAccount = accounts.stream().
                    filter(account -> user2Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertTrue(secondAccount.isPresent());
            assertFalse(secondAccount.orElseThrow().isSuspended());
            assertEquals(user2Key, secondAccount.orElseThrow().getConnObjectKeyValue());
            assertEquals("linkedaccount2", secondAccount.orElseThrow().getUsername());

            thirdAccount = accounts.stream().
                    filter(account -> "not.vivaldi".equals(account.getUsername())).
                    findFirst();
            assertTrue(thirdAccount.isPresent());
            assertTrue(thirdAccount.orElseThrow().isSuspended());
            assertEquals(user3Key, thirdAccount.orElseThrow().getConnObjectKeyValue());
        } finally {
            // clean up
            UserUR patch = new UserUR();
            patch.setKey(LinkedAccountSampleInboundCorrelationRule.VIVALDI_KEY);
            patch.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                    operation(PatchOperation.DELETE).
                    linkedAccountTO(new LinkedAccountTO.Builder(RESOURCE_NAME_REST, user2Key).build()).
                    build());
            patch.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                    operation(PatchOperation.DELETE).
                    linkedAccountTO(new LinkedAccountTO.Builder(RESOURCE_NAME_REST, user3Key).build()).
                    build());
            USER_SERVICE.update(patch);

            webClient.replacePath(user2Key).delete();
            webClient.replacePath(user3Key).delete();
        }
    }
}
