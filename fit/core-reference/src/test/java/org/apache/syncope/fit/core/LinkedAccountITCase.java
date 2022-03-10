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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.naming.NamingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
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
import org.apache.syncope.fit.core.reference.LinkedAccountSamplePullCorrelationRule;
import org.apache.syncope.fit.core.reference.LinkedAccountSamplePullCorrelationRuleConf;
import org.junit.jupiter.api.Test;

public class LinkedAccountITCase extends AbstractITCase {

    @Test
    public void createWithLinkedAccountThenUpdateThenRemove() throws NamingException {
        // 1. create user with linked account
        UserCR userCR = UserITCase.getSample(
                "linkedAccount" + RandomStringUtils.randomNumeric(5) + "@syncope.apache.org");
        String connObjectKeyValue = "uid=" + userCR.getUsername() + ",ou=People,o=isp";
        String privilege = applicationService.read("mightyApp").getPrivileges().get(0).getKey();

        LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_LDAP, connObjectKeyValue).build();
        account.getPlainAttrs().add(attr("surname", "LINKED_SURNAME"));
        account.getPrivileges().add(privilege);
        userCR.getLinkedAccounts().add(account);

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user.getKey());
        assertEquals(privilege, user.getLinkedAccounts().get(0).getPrivileges().iterator().next());

        // 2. verify that propagation task was generated and that account is found on resource
        PagedResult<PropagationTaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(1, tasks.getTotalCount());
        assertEquals(connObjectKeyValue, tasks.getResult().get(0).getConnObjectKey());
        assertEquals(ResourceOperation.CREATE, tasks.getResult().get(0).getOperation());
        assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().get(0).getLatestExecStatus());

        ConnObjectTO ldapObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), connObjectKeyValue);
        assertNotNull(ldapObj);
        assertEquals(user.getPlainAttr("email").get().getValues(), ldapObj.getAttr("mail").get().getValues());
        assertEquals("LINKED_SURNAME", ldapObj.getAttr("sn").get().getValues().get(0));

        // 3. remove linked account from user
        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());

        account.getPlainAttrs().clear();
        account.getPlainAttrs().add(attr("email", "UPDATED_EMAIL@syncope.apache.org"));
        account.getPlainAttrs().add(attr("surname", "UPDATED_SURNAME"));
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());

        user = updateUser(userUR).getEntity();
        assertEquals(1, user.getLinkedAccounts().size());

        // 4 verify that account was updated on resource
        ldapObj = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), connObjectKeyValue);
        assertNotNull(ldapObj);

        assertTrue(ldapObj.getAttr("mail").get().getValues().contains("UPDATED_EMAIL@syncope.apache.org"));
        assertEquals("UPDATED_SURNAME", ldapObj.getAttr("sn").get().getValues().get(0));

        // 5. remove linked account from user
        userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                operation(PatchOperation.DELETE).
                linkedAccountTO(user.getLinkedAccounts().get(0)).build());

        user = updateUser(userUR).getEntity();
        assertTrue(user.getLinkedAccounts().isEmpty());

        // 6. verify that propagation task was generated and that account is not any more on resource
        tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(3, tasks.getTotalCount());

        Optional<PropagationTaskTO> deletTask =
                tasks.getResult().stream().filter(task -> task.getOperation() == ResourceOperation.DELETE).findFirst();
        assertTrue(deletTask.isPresent());
        assertEquals(connObjectKeyValue, deletTask.get().getConnObjectKey());
        assertEquals(ExecStatus.SUCCESS.name(), deletTask.get().getLatestExecStatus());

        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, connObjectKeyValue));
    }

    @Test
    public void createWithLinkedAccountThenUpdateUsingPutThenRemove() throws NamingException {
        // 1. create user with linked account
        UserCR userCR = UserITCase.getSample(
                "linkedAccount" + RandomStringUtils.randomNumeric(5) + "@syncope.apache.org");
        String connObjectKeyValue = "uid=" + userCR.getUsername() + ",ou=People,o=isp";
        String privilege = applicationService.read("mightyApp").getPrivileges().get(0).getKey();

        LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_LDAP, connObjectKeyValue).build();
        account.setUsername("LinkedUsername");
        account.getPlainAttrs().add(attr("surname", "LINKED_SURNAME"));
        account.getPrivileges().add(privilege);
        userCR.getLinkedAccounts().add(account);

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user.getKey());
        assertEquals(1, user.getLinkedAccounts().size());
        assertEquals(privilege, user.getLinkedAccounts().get(0).getPrivileges().iterator().next());
        assertEquals("LinkedUsername", user.getLinkedAccounts().get(0).getUsername());
        assertEquals("LINKED_SURNAME", account.getPlainAttr("surname").get().getValues().get(0));

        // 2. update linked account
        account.getPlainAttrs().clear();
        account.setUsername("LinkedUsernameUpdated");
        account.getPlainAttrs().add(attr("email", "UPDATED_EMAIL@syncope.apache.org"));
        account.getPlainAttrs().add(attr("surname", "UPDATED_SURNAME"));
        user.getLinkedAccounts().clear();
        user.getLinkedAccounts().add(account);

        user = updateUser(user).getEntity();
        assertEquals(1, user.getLinkedAccounts().size());
        assertEquals("LinkedUsernameUpdated", user.getLinkedAccounts().get(0).getUsername());
        assertEquals("UPDATED_SURNAME", account.getPlainAttr("surname").get().getValues().get(0));

        // 3. remove linked account from user
        user.getLinkedAccounts().clear();
        user = updateUser(user).getEntity();
        assertTrue(user.getLinkedAccounts().isEmpty());
    }

    @Test
    public void createWithoutLinkedAccountThenAdd() throws NamingException {
        // 1. create user without linked account
        UserCR userCR = UserITCase.getSample(
                "linkedAccount" + RandomStringUtils.randomNumeric(5) + "@syncope.apache.org");
        String connObjectKeyValue = "uid=" + userCR.getUsername() + ",ou=People,o=isp";

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user.getKey());
        assertTrue(user.getLinkedAccounts().isEmpty());

        PagedResult<PropagationTaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(0, tasks.getTotalCount());

        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, connObjectKeyValue));

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
        tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_LDAP).
                        anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
        assertEquals(1, tasks.getTotalCount());
        assertEquals(connObjectKeyValue, tasks.getResult().get(0).getConnObjectKey());
        assertEquals(ResourceOperation.CREATE, tasks.getResult().get(0).getOperation());
        assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().get(0).getLatestExecStatus());

        ConnObjectTO ldapObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), connObjectKeyValue);
        assertNotNull(ldapObj);
        assertEquals(user.getPlainAttr("email").get().getValues(), ldapObj.getAttr("mail").get().getValues());
        assertEquals("LINKED_SURNAME", ldapObj.getAttr("sn").get().getValues().get(0));
    }

    @Test
    public void createWithoutLinkedAccountThenAddAndUpdatePassword() throws NamingException {
        // 1. set the return value parameter to true
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "return.password.value", true);

        // 2. create user without linked account
        UserCR userCR = UserITCase.getSample(
                "linkedAccount" + RandomStringUtils.randomNumeric(5) + "@syncope.apache.org");
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
        assertNull(user.getLinkedAccounts().get(0).getPassword());

        // 4. update linked account with adding a password
        account.setPassword("Password123");
        userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());
        user = updateUser(userUR).getEntity();
        assertNotNull(user.getLinkedAccounts().get(0).getPassword());

        // 5. update linked account  password
        String beforeUpdatePassword = user.getLinkedAccounts().get(0).getPassword();
        account.setPassword("Password123Updated");
        userUR = new UserUR();
        userUR.setKey(user.getKey());

        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());
        user = updateUser(userUR).getEntity();
        assertNotNull(user.getLinkedAccounts().get(0).getPassword());
        assertNotEquals(beforeUpdatePassword, user.getLinkedAccounts().get(0).getPassword());

        // 6. set linked account password to null
        account.setPassword(null);
        userUR = new UserUR();
        userUR.setKey(user.getKey());

        userUR.getLinkedAccounts().add(new LinkedAccountUR.Builder().linkedAccountTO(account).build());
        user = updateUser(userUR).getEntity();
        assertNull(user.getLinkedAccounts().get(0).getPassword());

        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "return.password.value", false);
    }

    @Test
    public void push() {
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
                    "linkedAccount" + RandomStringUtils.randomNumeric(5) + "@syncope.apache.org");

            LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_REST, connObjectKeyValue).build();
            userCR.getLinkedAccounts().add(account);

            UserTO user = createUser(userCR).getEntity();
            userKey = user.getKey();
            assertNotNull(userKey);
            assertNotEquals(userKey, connObjectKeyValue);

            // 2. verify that account is found on resource
            PagedResult<PropagationTaskTO> tasks = taskService.search(
                    new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_REST).
                            anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(1, tasks.getTotalCount());
            assertEquals(connObjectKeyValue, tasks.getResult().get(0).getConnObjectKey());
            assertEquals(ResourceOperation.CREATE, tasks.getResult().get(0).getOperation());
            assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().get(0).getLatestExecStatus());

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

            response = taskService.create(TaskType.PUSH, sendUser);
            sendUser = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
            assertNotNull(sendUser);

            // 5. execute PushTask
            AbstractTaskITCase.execProvisioningTask(
                    taskService, TaskType.PUSH, sendUser.getKey(), MAX_WAIT_SECONDS, false);

            TaskTO task = taskService.read(TaskType.PUSH, sendUser.getKey(), true);
            assertEquals(1, task.getExecutions().size());
            assertEquals(ExecStatus.SUCCESS.name(), task.getExecutions().get(0).getStatus());

            tasks = taskService.search(
                    new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_REST).
                            anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(3, tasks.getTotalCount());

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

                userService.delete(userKey);
            }
        }
    }

    @Test
    public void pull() {
        // -----------------------------
        // Add a custom policy with correlation rule
        // -----------------------------
        ResourceTO restResource = resourceService.read(RESOURCE_NAME_REST);
        if (restResource.getPullPolicy() == null) {
            ImplementationTO rule = null;
            try {
                rule = implementationService.read(
                        IdMImplementationType.PULL_CORRELATION_RULE, "LinkedAccountSamplePullCorrelationRule");
            } catch (SyncopeClientException e) {
                if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                    rule = new ImplementationTO();
                    rule.setKey("LinkedAccountSamplePullCorrelationRule");
                    rule.setEngine(ImplementationEngine.JAVA);
                    rule.setType(IdMImplementationType.PULL_CORRELATION_RULE);
                    rule.setBody(POJOHelper.serialize(new LinkedAccountSamplePullCorrelationRuleConf()));
                    Response response = implementationService.create(rule);
                    rule = implementationService.read(
                            rule.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                    assertNotNull(rule.getKey());
                }
            }
            assertNotNull(rule);

            PullPolicyTO policy = new PullPolicyTO();
            policy.setName("Linked Account sample Pull policy");
            policy.getCorrelationRules().put(AnyTypeKind.USER.name(), rule.getKey());
            Response response = policyService.create(PolicyType.PULL, policy);
            policy = policyService.read(PolicyType.PULL, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
            assertNotNull(policy.getKey());

            restResource.setPullPolicy(policy.getKey());
            resourceService.update(restResource);
        }

        // -----------------------------
        // -----------------------------
        // Add a pull task
        // -----------------------------
        String pullTaskKey;

        PagedResult<PullTaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PULL).resource(RESOURCE_NAME_REST).build());
        if (tasks.getTotalCount() > 0) {
            pullTaskKey = tasks.getResult().get(0).getKey();
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

            Response response = taskService.create(TaskType.PULL, task);
            task = taskService.read(TaskType.PULL, response.getHeaderString(RESTHeaders.RESOURCE_KEY), false);
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
            List<LinkedAccountTO> accounts = userService.read("vivaldi").getLinkedAccounts();
            assertTrue(accounts.isEmpty());

            ExecTO exec = AbstractTaskITCase.execProvisioningTask(
                    taskService, TaskType.PULL, pullTaskKey, MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));
            assertTrue(exec.getMessage().contains("Accounts created"));

            accounts = userService.read("vivaldi").getLinkedAccounts();
            assertEquals(3, accounts.size());

            Optional<LinkedAccountTO> firstAccount = accounts.stream().
                    filter(account -> user1Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertTrue(firstAccount.isPresent());
            assertFalse(firstAccount.get().isSuspended());
            assertEquals(RESOURCE_NAME_REST, firstAccount.get().getResource());
            assertEquals("linkedaccount1", firstAccount.get().getUsername());
            assertEquals("Pasquale", firstAccount.get().getPlainAttr("firstname").get().getValues().get(0));

            Optional<LinkedAccountTO> secondAccount = accounts.stream().
                    filter(account -> user2Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertTrue(secondAccount.isPresent());
            assertFalse(secondAccount.get().isSuspended());
            assertEquals(RESOURCE_NAME_REST, secondAccount.get().getResource());
            assertNull(secondAccount.get().getUsername());
            assertEquals("Giovannino", secondAccount.get().getPlainAttr("firstname").get().getValues().get(0));

            Optional<LinkedAccountTO> thirdAccount = accounts.stream().
                    filter(account -> user3Key.equals(account.getConnObjectKeyValue())).
                    filter(account -> "not.vivaldi".equals(account.getUsername())).
                    findFirst();
            assertTrue(thirdAccount.isPresent());
            assertFalse(thirdAccount.get().isSuspended());
            assertEquals(RESOURCE_NAME_REST, thirdAccount.get().getResource());
            assertEquals("not.vivaldi", thirdAccount.get().getUsername());

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
            exec = AbstractTaskITCase.execProvisioningTask(
                    taskService, TaskType.PULL, pullTaskKey, MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));
            assertTrue(exec.getMessage().contains("Accounts updated"));
            assertTrue(exec.getMessage().contains("Accounts deleted"));

            accounts = userService.read("vivaldi").getLinkedAccounts();
            assertEquals(2, accounts.size());

            firstAccount = accounts.stream().
                    filter(account -> user1Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertFalse(firstAccount.isPresent());

            secondAccount = accounts.stream().
                    filter(account -> user2Key.equals(account.getConnObjectKeyValue())).
                    findFirst();
            assertTrue(secondAccount.isPresent());
            assertFalse(secondAccount.get().isSuspended());
            assertEquals(user2Key, secondAccount.get().getConnObjectKeyValue());
            assertEquals("linkedaccount2", secondAccount.get().getUsername());

            thirdAccount = accounts.stream().
                    filter(account -> "not.vivaldi".equals(account.getUsername())).
                    findFirst();
            assertTrue(thirdAccount.isPresent());
            assertTrue(thirdAccount.get().isSuspended());
            assertEquals(user3Key, thirdAccount.get().getConnObjectKeyValue());
        } finally {
            // clean up
            UserUR patch = new UserUR();
            patch.setKey(LinkedAccountSamplePullCorrelationRule.VIVALDI_KEY);
            patch.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                    operation(PatchOperation.DELETE).
                    linkedAccountTO(new LinkedAccountTO.Builder(RESOURCE_NAME_REST, user2Key).build()).
                    build());
            patch.getLinkedAccounts().add(new LinkedAccountUR.Builder().
                    operation(PatchOperation.DELETE).
                    linkedAccountTO(new LinkedAccountTO.Builder(RESOURCE_NAME_REST, user3Key).build()).
                    build());
            userService.update(patch);

            webClient.replacePath(user2Key).delete();
            webClient.replacePath(user3Key).delete();
        }
    }
}
