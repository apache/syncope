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

import static org.apache.syncope.fit.AbstractITCase.getObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.common.lib.request.LinkedAccountUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.AbstractITCase;
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

        LdapContext ldapObj = (LdapContext) getLdapRemoteObject(
                RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, connObjectKeyValue);
        assertNotNull(ldapObj);

        Attributes ldapAttrs = ldapObj.getAttributes("");
        assertEquals(
                user.getPlainAttr("email").get().getValues().get(0),
                ldapAttrs.get("mail").getAll().next().toString());
        assertEquals("LINKED_SURNAME", ldapAttrs.get("sn").getAll().next().toString());

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
        ldapObj = (LdapContext) getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, connObjectKeyValue);
        assertNotNull(ldapObj);

        ldapAttrs = ldapObj.getAttributes("");
        assertEquals("UPDATED_EMAIL@syncope.apache.org", ldapAttrs.get("mail").getAll().next().toString());
        assertEquals("UPDATED_SURNAME", ldapAttrs.get("sn").getAll().next().toString());

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

        LdapContext ldapObj = (LdapContext) getLdapRemoteObject(
                RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, connObjectKeyValue);
        assertNotNull(ldapObj);

        Attributes ldapAttrs = ldapObj.getAttributes("");
        assertEquals(
                user.getPlainAttr("email").get().getValues().get(0),
                ldapAttrs.get("mail").getAll().next().toString());
        assertEquals("LINKED_SURNAME", ldapAttrs.get("sn").getAll().next().toString());
    }

    @Test
    public void push() {
        // 0a. read configured cipher algorithm in order to be able to restore it at the end of test
        String origpwdCipherAlgo = confParamOps.get(SyncopeConstants.MASTER_DOMAIN,
                "password.cipher.algorithm", null, String.class);

        // 0b. set AES password cipher algorithm
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", "AES");

        try {
            // 1. create user with linked account
            UserCR userCR = UserITCase.getSample(
                    "linkedAccount" + RandomStringUtils.randomNumeric(5) + "@syncope.apache.org");
            String connObjectKeyValue = UUID.randomUUID().toString();

            LinkedAccountTO account = new LinkedAccountTO.Builder(RESOURCE_NAME_REST, connObjectKeyValue).build();
            userCR.getLinkedAccounts().add(account);

            UserTO user = createUser(userCR).getEntity();
            String userKey = user.getKey();
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
            AbstractTaskITCase.execProvisioningTask(taskService, TaskType.PUSH, sendUser.getKey(), 50, false);

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
        }
    }
}
