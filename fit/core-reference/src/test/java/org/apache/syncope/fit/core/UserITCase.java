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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.HaveIBeenPwnedPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.ResourceAR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.FlowableDetector;
import org.apache.syncope.fit.core.reference.TestAccountRuleConf;
import org.apache.syncope.fit.core.reference.TestPasswordRuleConf;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.jupiter.api.Test;

public class UserITCase extends AbstractITCase {

    private static boolean getBooleanAttribute(final ConnObjectTO connObjectTO, final String attrName) {
        return Boolean.parseBoolean(connObjectTO.getAttr(attrName).get().getValues().get(0));
    }

    public static UserCR getUniqueSample(final String email) {
        return getSample(getUUIDString() + email);
    }

    public static UserCR getSample(final String email) {
        return new UserCR.Builder(SyncopeConstants.ROOT_REALM, email).
                password("password123").
                plainAttr(attr("fullname", email)).
                plainAttr(attr("firstname", email)).
                plainAttr(attr("surname", "surname")).
                plainAttr(attr("ctype", "a type")).
                plainAttr(attr("userId", email)).
                plainAttr(attr("email", email)).
                plainAttr(attr("loginDate", DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()))).
                build();
    }

    @Test
    public void readPrivileges() {
        Set<String> privileges = userService.read("rossini").getPrivileges();
        assertNotNull(privileges);
        assertEquals(1, privileges.size());
    }

    @Test
    public void createUserWithNoPropagation() {
        // create a new user
        UserCR req = getUniqueSample("xxx@xxx.xxx");
        req.setPassword("password123");
        req.getResources().add(RESOURCE_NAME_NOPROPAGATION);

        UserTO userTO = createUser(req).getEntity();

        // get the propagation task just created
        PagedResult<PropagationTaskTO> tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        PropagationTaskTO taskTO = tasks.getResult().get(0);
        assertNotNull(taskTO);
        assertFalse(taskTO.getExecutions().isEmpty());
        assertEquals(ExecStatus.NOT_ATTEMPTED.name(), taskTO.getExecutions().get(0).getStatus());
    }

    @Test
    public void enforceMandatoryCondition() {
        UserCR userCR = getUniqueSample("enforce@apache.org");
        userCR.getResources().add(RESOURCE_NAME_WS2);
        userCR.setPassword("newPassword12");

        Attr type = null;
        for (Attr attr : userCR.getPlainAttrs()) {
            if ("ctype".equals(attr.getSchema())) {
                type = attr;
            }
        }
        assertNotNull(type);
        userCR.getPlainAttrs().remove(type);

        try {
            createUser(userCR).getEntity();
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userCR.getPlainAttrs().add(type);
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void enforceMandatoryConditionOnDerived() {
        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_CSV);
        assertNotNull(resourceTO);
        resourceTO.setKey("resource-csv-enforcing");
        resourceTO.setEnforceMandatoryCondition(true);

        Response response = resourceService.create(resourceTO);
        resourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(resourceTO);

        try {
            UserCR userCR = getUniqueSample("syncope222@apache.org");
            userCR.getResources().add(resourceTO.getKey());
            userCR.setPassword("newPassword12");

            try {
                createUser(userCR).getEntity();
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
            }

            userCR.getAuxClasses().add("csv");

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);
            assertEquals(Set.of(resourceTO.getKey()), userTO.getResources());
        } finally {
            resourceService.delete(resourceTO.getKey());
        }
    }

    @Test
    public void createUserWithDbPropagation() {
        UserCR userCR = getUniqueSample("yyy@yyy.yyy");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        ProvisioningResult<UserTO> result = createUser(userCR);
        assertNotNull(result);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void createWithInvalidPassword() {
        assertThrows(SyncopeClientException.class, () -> {
            UserCR userCR = getSample("invalidpasswd@syncope.apache.org");
            userCR.setPassword("pass");
            createUser(userCR);
        });
    }

    @Test
    public void createWithInvalidUsername() {
        assertThrows(SyncopeClientException.class, () -> {
            UserCR userCR = getSample("invalidusername@syncope.apache.org");
            userCR.setUsername("us");
            userCR.setRealm("/odd");

            createUser(userCR);
        });
    }

    @Test
    public void createWithInvalidPasswordByRes() {
        assertThrows(SyncopeClientException.class, () -> {
            UserCR userCR = getSample("invalidPwdByRes@passwd.com");

            // configured to be minLength=16
            userCR.setPassword("password1");
            userCR.getResources().add(RESOURCE_NAME_NOPROPAGATION);
            createUser(userCR);
        });
    }

    @Test
    public void createWithInvalidPasswordByGroup() {
        assertThrows(SyncopeClientException.class, () -> {
            UserCR userCR = getSample("invalidPwdByGroup@passwd.com");

            // configured to be minLength=16
            userCR.setPassword("password1");

            userCR.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

            createUser(userCR);
        });
    }

    @Test
    public void createWithException() {
        assertThrows(SyncopeClientException.class, () -> {
            UserCR userCR = new UserCR();
            userCR.getPlainAttrs().add(attr("userId", "userId@nowhere.org"));
            createUser(userCR);
        });
    }

    @Test
    public void create() {
        // get task list
        PagedResult<PropagationTaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        String maxKey = tasks.getResult().iterator().next().getKey();
        PropagationTaskTO taskTO = taskService.read(TaskType.PROPAGATION, maxKey, true);

        assertNotNull(taskTO);
        int maxTaskExecutions = taskTO.getExecutions().size();

        UserCR userCR = getUniqueSample("a.b@c.com");

        // add a membership
        userCR.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        // add an attribute with a non-existing schema: must be ignored
        Attr attrWithInvalidSchemaTO = attr("invalid schema", "a value");
        userCR.getPlainAttrs().add(attrWithInvalidSchemaTO);

        // add an attribute with null value: must be ignored
        userCR.getPlainAttrs().add(attr("activationDate", null));

        // 1. create user
        UserTO userTO = createUser(userCR).getEntity();

        assertNotNull(userTO);

        // issue SYNCOPE-15
        assertNotNull(userTO.getCreationDate());
        assertNotNull(userTO.getCreator());
        assertNotNull(userTO.getLastChangeDate());
        assertNotNull(userTO.getLastModifier());
        assertTrue(userTO.getLastChangeDate().getTime() - userTO.getCreationDate().getTime() < 3000);

        assertFalse(userTO.getPlainAttrs().contains(attrWithInvalidSchemaTO));

        // check for changePwdDate
        assertNotNull(userTO.getCreationDate());

        // get the new task list
        tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        String newMaxKey = tasks.getResult().iterator().next().getKey();

        // default configuration for ws-target-resource2:
        // only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxKey, maxKey);

        // get last task
        taskTO = taskService.read(TaskType.PROPAGATION, newMaxKey, true);

        assertNotNull(taskTO);
        assertEquals(maxTaskExecutions, taskTO.getExecutions().size());

        // 3. verify password
        try {
            Triple<Map<String, Set<String>>, List<String>, UserTO> self =
                    clientFactory.create(userTO.getUsername(), "password123").self();
            assertNotNull(self);
        } catch (AccessControlException e) {
            fail("Credentials should be valid and not cause AccessControlException");
        }

        try {
            clientFactory.create(userTO.getUsername(), "passwordXX").getService(UserSelfService.class);
            fail("Credentials are invalid, thus request should raise AccessControlException");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. try (and fail) to create another user with same (unique) values
        userCR = getSample(userTO.getUsername());
        Attr userIdAttr = userTO.getPlainAttr("userId").get();
        userIdAttr.getValues().clear();
        userIdAttr.getValues().add("a.b@c.com");

        try {
            createUser(userCR);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void createWithRequiredValueMissing() {
        UserCR userCR = getUniqueSample("a.b@c.it");

        Attr type = userCR.getPlainAttr("ctype").get();
        userCR.getPlainAttrs().remove(type);

        userCR.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        // 1. create user without type (mandatory by UserSchema)
        try {
            createUser(userCR);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userCR.getPlainAttrs().add(attr("ctype", "F"));

        Attr surname = userCR.getPlainAttr("surname").get();
        userCR.getPlainAttrs().remove(surname);

        // 2. create user without surname (mandatory when type == 'F')
        try {
            createUser(userCR);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }
    }

    @Test
    public void delete() {
        try {
            userService.delete(UUID.randomUUID().toString());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        UserCR userCR = getSample("qqgf.z@nn.com");

        // specify a propagation
        userCR.getResources().add(RESOURCE_NAME_TESTDB);

        UserTO userTO = createUser(userCR).getEntity();

        String key = userTO.getKey();

        ProvisioningResult<UserTO> result = deleteUser(key);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(key, userTO.getKey());
        assertTrue(userTO.getPlainAttrs().isEmpty());

        // check for propagation result
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

        try {
            userService.delete(userTO.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void deleteByUsername() {
        UserCR userCR = getSample("delete.by.username@apache.org");

        // specify a propagation
        userCR.getResources().add(RESOURCE_NAME_TESTDB);

        UserTO userTO = createUser(userCR).getEntity();

        String key = userTO.getKey();
        userTO = userService.read(key);

        ProvisioningResult<UserTO> result = deleteUser(userTO.getKey());
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(key, userTO.getKey());
        assertTrue(userTO.getPlainAttrs().isEmpty());

        // check for propagation result
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

        try {
            userService.read(userTO.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        PagedResult<UserTO> users = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).page(1).size(2).build());
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());
        assertEquals(2, users.getResult().size());

        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }

        users = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                page(2).size(2).build());
        assertNotNull(users);
        assertEquals(2, users.getPage());
        assertEquals(2, users.getResult().size());

        users = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                page(100).size(2).build());
        assertNotNull(users);
        assertTrue(users.getResult().isEmpty());
    }

    @Test
    public void read() {
        UserTO userTO = userService.read("1417acbe-cbf6-4277-9372-e75e04f97000");

        assertNotNull(userTO);
        assertNull(userTO.getPassword());
        assertNotNull(userTO.getPlainAttrs());
        assertFalse(userTO.getPlainAttrs().isEmpty());
    }

    @Test
    public void updateWithoutPassword() {
        UserCR userCR = getUniqueSample("updatewithout@password.com");

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        UserUR userUR = new UserUR.Builder(userTO.getKey()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("ctype").build()).
                        operation(PatchOperation.DELETE).
                        build()).build();

        userTO = updateUser(userUR).getEntity();

        assertNotNull(userTO);
        assertFalse(userTO.getPlainAttr("ctype").isPresent());
    }

    @Test
    public void updateInvalidPassword() {
        assertThrows(SyncopeClientException.class, () -> {
            UserCR userCR = getSample("updateinvalid@password.com");

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);

            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            userUR.setPassword(new PasswordPatch.Builder().value("pass").build());

            userService.update(userUR);
        });
    }

    @Test
    public void updateSamePassword() {
        assertThrows(SyncopeClientException.class, () -> {
            UserCR userCR = getUniqueSample("updatesame@password.com");
            userCR.setRealm("/even/two");

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);

            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            userUR.setPassword(new PasswordPatch.Builder().value("password123").build());

            userService.update(userUR);
        });
    }

    @Test
    public void update() {
        UserCR userCR = getUniqueSample("g.h@t.com");

        userCR.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        UserTO userTO = createUser(userCR).getEntity();

        assertFalse(userTO.getDerAttrs().isEmpty());
        assertEquals(1, userTO.getMemberships().size());

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("new2Password").build());

        String newUserId = getUUIDString() + "t.w@spre.net";
        userUR.getPlainAttrs().add(attrAddReplacePatch("userId", newUserId));

        String newFullName = getUUIDString() + "g.h@t.com";
        userUR.getPlainAttrs().add(attrAddReplacePatch("fullname", newFullName));

        userUR.getMemberships().add(new MembershipUR.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").
                operation(PatchOperation.ADD_REPLACE).build());
        userUR.getMemberships().add(new MembershipUR.Builder(userTO.getMemberships().get(0).getGroupKey()).
                operation(PatchOperation.ADD_REPLACE).build());

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);

        // issue SYNCOPE-15
        assertNotNull(userTO.getCreationDate());
        assertNotNull(userTO.getCreator());
        assertNotNull(userTO.getLastChangeDate());
        assertNotNull(userTO.getLastModifier());
        assertTrue(userTO.getCreationDate().before(userTO.getLastChangeDate()));

        assertEquals(1, userTO.getMemberships().size());
        assertFalse(userTO.getDerAttrs().isEmpty());

        Attr userIdAttr = userTO.getPlainAttr("userId").get();
        assertEquals(List.of(newUserId), userIdAttr.getValues());

        Attr fullNameAttr = userTO.getPlainAttr("fullname").get();
        assertEquals(List.of(newFullName), fullNameAttr.getValues());

        // update by username
        userUR = new UserUR();
        userUR.setKey(userTO.getUsername());
        String newUsername = UUID.randomUUID().toString();
        userUR.setUsername(new StringReplacePatchItem.Builder().value(newUsername).build());

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);
        assertEquals(newUsername, userTO.getUsername());
    }

    @Test
    public void updatePasswordOnly() {
        int beforeTasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();
        assertFalse(beforeTasks <= 0);

        UserCR userCR = getUniqueSample("pwdonly@t.com");
        userCR.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        UserTO userTO = createUser(userCR).getEntity();

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("newPassword123").resource(RESOURCE_NAME_WS2).build());

        userTO = updateUser(userUR).getEntity();

        // check for changePwdDate
        assertNotNull(userTO.getChangePwdDate());

        int afterTasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();
        assertFalse(afterTasks <= 0);

        assertTrue(beforeTasks < afterTasks);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyTaskRegistration() {
        // get task list
        PagedResult<PropagationTaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        String maxKey = tasks.getResult().iterator().next().getKey();

        // --------------------------------------
        // Create operation
        // --------------------------------------
        UserCR userCR = getUniqueSample("t@p.mode");

        // add a membership
        userCR.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        // 1. create user
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // get the new task list
        tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        String newMaxKey = tasks.getResult().iterator().next().getKey();

        // default configuration for ws-target-resource2 during create:
        // only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxKey, maxKey);

        // --------------------------------------
        // Update operation
        // --------------------------------------
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());

        userUR.getPlainAttrs().add(attrAddReplacePatch("surname", "surname2"));

        userTO = updateUser(userUR).getEntity();

        assertNotNull(userTO);

        // get the new task list
        tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());

        // default configuration for ws-target-resource2 during update:
        // all update executions have to be registered
        newMaxKey = tasks.getResult().iterator().next().getKey();

        PropagationTaskTO taskTO = taskService.read(TaskType.PROPAGATION, newMaxKey, true);

        assertNotNull(taskTO);
        assertEquals(1, taskTO.getExecutions().size());

        // --------------------------------------
        // Delete operation
        // --------------------------------------
        userService.delete(userTO.getKey());

        // get the new task list
        tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());

        maxKey = newMaxKey;
        newMaxKey = tasks.getResult().iterator().next().getKey();

        // default configuration for ws-target-resource2: no delete executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxKey, maxKey);
    }

    @Test
    public void createActivate() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform()));

        UserCR userCR = getUniqueSample("createActivate@syncope.apache.org");

        userCR.getMemberships().add(new MembershipTO.Builder("268fed79-f440-4390-9435-b273768eb5d6").build());

        UserTO userTO = createUser(userCR).getEntity();

        assertNotNull(userTO);
        assertNotNull(userTO.getToken());
        assertNotNull(userTO.getTokenExpireTime());

        assertEquals("created", userTO.getStatus());

        StatusR statusR = new StatusR.Builder().key(userTO.getKey()).
                type(StatusRType.ACTIVATE).token(userTO.getToken()).build();

        userTO = userService.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();

        assertNotNull(userTO);
        assertNull(userTO.getToken());
        assertNull(userTO.getTokenExpireTime());
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void suspendReactivate() {
        UserCR userCR = getUniqueSample("suspendReactivate@syncope.apache.org");

        userCR.getMemberships().add(new MembershipTO.Builder("bf825fe1-7320-4a54-bd64-143b5c18ab97").build());

        UserTO userTO = createUser(userCR).getEntity();

        assertNotNull(userTO);
        assertEquals(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform())
                ? "active"
                : "created", userTO.getStatus());

        StatusR statusR = new StatusR.Builder().key(userTO.getKey()).
                type(StatusRType.SUSPEND).token(userTO.getToken()).build();

        userTO = userService.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        statusR = new StatusR.Builder().key(userTO.getKey()).type(StatusRType.REACTIVATE).build();

        userTO = userService.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void suspendReactivateOnResource() {
        // Assert resources are present
        ResourceTO dbTable = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(dbTable);
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(ldap);

        // Create user with reference to resources
        UserCR userCR = getUniqueSample("suspreactonresource@syncope.apache.org");
        userCR.getMemberships().clear();
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        userCR.getResources().add(RESOURCE_NAME_LDAP);
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertEquals(FlowableDetector.isFlowableEnabledForUserWorkflow(adminClient.platform())
                ? "active"
                : "created", userTO.getStatus());
        String userKey = userTO.getKey();

        // Suspend with effect on syncope, ldap and db => user should be suspended in syncope and all resources
        StatusR statusR = new StatusR.Builder().key(userKey).
                type(StatusRType.SUSPEND).
                onSyncope(true).
                resources(RESOURCE_NAME_TESTDB, RESOURCE_NAME_LDAP).
                build();
        userTO = userService.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userKey);
        assertFalse(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userKey);
        assertNotNull(connObjectTO);

        // Suspend and reactivate only on ldap => db and syncope should still show suspended
        statusR = new StatusR.Builder().key(userKey).
                type(StatusRType.SUSPEND).
                onSyncope(false).
                resources(RESOURCE_NAME_LDAP).
                build();
        userService.status(statusR);
        statusR.setType(StatusRType.REACTIVATE);
        userTO = userService.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userKey);
        assertFalse(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));

        // Reactivate on syncope and db => syncope and db should show the user as active
        statusR = new StatusR.Builder().key(userKey).
                type(StatusRType.REACTIVATE).
                onSyncope(true).
                resources(RESOURCE_NAME_TESTDB).
                build();
        userTO = userService.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userKey);
        assertTrue(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));
    }

    @Test
    public void updateMultivalueAttribute() {
        UserCR userCR = getUniqueSample("multivalue@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getVirAttrs().clear();

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        Attr loginDate = userTO.getPlainAttr("loginDate").get();
        assertNotNull(loginDate);
        assertEquals(1, loginDate.getValues().size());

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());

        loginDate.getValues().add("2000-01-01");
        userUR.getPlainAttrs().add(new AttrPatch.Builder(loginDate).
                operation(PatchOperation.ADD_REPLACE).build());

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);

        loginDate = userTO.getPlainAttr("loginDate").get();
        assertNotNull(loginDate);
        assertEquals(2, loginDate.getValues().size());
    }

    private static void verifyAsyncResult(final List<PropagationStatus> statuses) {
        assertEquals(3, statuses.size());

        Map<String, PropagationStatus> byResource = statuses.stream().collect(
                Collectors.toMap(PropagationStatus::getResource, Function.identity()));
        assertEquals(ExecStatus.SUCCESS, byResource.get(RESOURCE_NAME_LDAP).getStatus());
        assertTrue(byResource.get(RESOURCE_NAME_TESTDB).getStatus() == ExecStatus.CREATED
                || byResource.get(RESOURCE_NAME_TESTDB).getStatus() == ExecStatus.SUCCESS);
        assertTrue(byResource.get(RESOURCE_NAME_TESTDB2).getStatus() == ExecStatus.CREATED
                || byResource.get(RESOURCE_NAME_TESTDB2).getStatus() == ExecStatus.SUCCESS);
    }

    @Test
    public void async() {
        SyncopeClient asyncClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        UserService asyncService = SyncopeClient.nullPriorityAsync(asyncClient.getService(UserService.class), true);

        UserCR userCR = getUniqueSample("async@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        userCR.getResources().add(RESOURCE_NAME_TESTDB2);
        userCR.getResources().add(RESOURCE_NAME_LDAP);

        ProvisioningResult<UserTO> result = asyncService.create(userCR).readEntity(
            new GenericType<>() {
            });
        assertNotNull(result);
        verifyAsyncResult(result.getPropagationStatuses());

        UserUR userUR = new UserUR();
        userUR.setKey(result.getEntity().getKey());
        userUR.setPassword(new PasswordPatch.Builder().
                onSyncope(true).resources(RESOURCE_NAME_LDAP, RESOURCE_NAME_TESTDB, RESOURCE_NAME_TESTDB2).
                value("password321").build());

        result = asyncService.update(userUR).readEntity(
            new GenericType<>() {
            });
        assertNotNull(result);
        verifyAsyncResult(result.getPropagationStatuses());

        result = asyncService.delete(result.getEntity().getKey()).readEntity(
            new GenericType<>() {
            });
        assertNotNull(result);
        verifyAsyncResult(result.getPropagationStatuses());
    }

    @Test
    public void groupAttrPropagation() {
        UserCR userCR = getUniqueSample("checkGroupAttrPropagation@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();

        userCR.getAuxClasses().add("csv");

        userCR.getMemberships().add(new MembershipTO.Builder("37d15e4c-cdc1-460b-a591-8505c8133806").build());

        userCR.getResources().add(RESOURCE_NAME_CSV);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertNotNull(userTO.getDerAttr("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertEquals("sx-dx", connObjectTO.getAttr("THEIRGROUP").get().getValues().get(0));
    }

    @Test
    public void customPolicyRules() {
        ImplementationTO implementationTO = new ImplementationTO();
        implementationTO.setKey("TestAccountRuleConf" + UUID.randomUUID().toString());
        implementationTO.setEngine(ImplementationEngine.JAVA);
        implementationTO.setType(IdRepoImplementationType.ACCOUNT_RULE);
        implementationTO.setBody(POJOHelper.serialize(new TestAccountRuleConf()));
        Response response = implementationService.create(implementationTO);
        implementationTO.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        AccountPolicyTO accountPolicy = new AccountPolicyTO();
        accountPolicy.setName("Account Policy with custom rules");
        accountPolicy.getRules().add(implementationTO.getKey());
        accountPolicy = createPolicy(PolicyType.ACCOUNT, accountPolicy);
        assertNotNull(accountPolicy);

        implementationTO = new ImplementationTO();
        implementationTO.setKey("TestPasswordRuleConf" + UUID.randomUUID().toString());
        implementationTO.setEngine(ImplementationEngine.JAVA);
        implementationTO.setType(IdRepoImplementationType.PASSWORD_RULE);
        implementationTO.setBody(POJOHelper.serialize(new TestPasswordRuleConf()));
        response = implementationService.create(implementationTO);
        implementationTO.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        PasswordPolicyTO passwordPolicy = new PasswordPolicyTO();
        passwordPolicy.setName("Password Policy with custom rules");
        passwordPolicy.getRules().add(implementationTO.getKey());
        passwordPolicy = createPolicy(PolicyType.PASSWORD, passwordPolicy);
        assertNotNull(passwordPolicy);

        RealmTO realm = realmService.search(new RealmQuery.Builder().keyword("two").build()).getResult().get(0);
        String oldAccountPolicy = realm.getAccountPolicy();
        realm.setAccountPolicy(accountPolicy.getKey());
        String oldPasswordPolicy = realm.getPasswordPolicy();
        realm.setPasswordPolicy(passwordPolicy.getKey());
        realmService.update(realm);

        try {
            UserCR userCR = getUniqueSample("custompolicyrules@syncope.apache.org");
            userCR.setRealm(realm.getFullPath());
            try {
                createUser(userCR);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidPassword"));
            }

            userCR.setPassword(userCR.getPassword() + "XXX");
            try {
                createUser(userCR);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidUsername"));
            }

            userCR.setUsername("YYY" + userCR.getUsername());
            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);
        } finally {
            realm.setAccountPolicy(oldAccountPolicy);
            realm.setPasswordPolicy(oldPasswordPolicy);
            realmService.update(realm);

            policyService.delete(PolicyType.PASSWORD, passwordPolicy.getKey());
            policyService.delete(PolicyType.ACCOUNT, accountPolicy.getKey());
        }
    }

    @Test
    public void mappingPurpose() {
        UserCR userCR = getUniqueSample("mpurpose@apache.org");
        userCR.getAuxClasses().add("csv");

        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_CSV);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertFalse(connObjectTO.getAttr("email").isPresent());
    }

    @Test
    public void batch() throws IOException {
        List<String> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserCR userCR = getUniqueSample("batch_" + i + "@apache.org");
            users.add(createUser(userCR).getEntity().getKey());
        }

        // check for a fail
        users.add(UUID.randomUUID().toString());

        assertEquals(11, users.size());

        BatchRequest batchRequest = adminClient.batch();

        UserService batchUserService = batchRequest.getService(UserService.class);
        users.forEach(user -> batchUserService.status(new StatusR.Builder().key(user).type(StatusRType.SUSPEND).
                onSyncope(true).
                build()));
        List<BatchResponseItem> batchResponseItems = parseBatchResponse(batchRequest.commit().getResponse());
        assertEquals(10, batchResponseItems.stream().
                filter(item -> Response.Status.OK.getStatusCode() == item.getStatus()).count());
        assertEquals(1, batchResponseItems.stream().
                filter(item -> Response.Status.NOT_FOUND.getStatusCode() == item.getStatus()).count());
        assertEquals("suspended", userService.read(users.get(3)).getStatus());

        UserService batchUserService2 = batchRequest.getService(UserService.class);
        users.forEach(user -> batchUserService2.status(new StatusR.Builder().key(user).type(StatusRType.REACTIVATE).
                onSyncope(true).
                build()));
        batchResponseItems = parseBatchResponse(batchRequest.commit().getResponse());
        assertEquals(10, batchResponseItems.stream().
                filter(item -> Response.Status.OK.getStatusCode() == item.getStatus()).count());
        assertEquals(1, batchResponseItems.stream().
                filter(item -> Response.Status.NOT_FOUND.getStatusCode() == item.getStatus()).count());
        assertEquals("active", userService.read(users.get(3)).getStatus());

        UserService batchUserService3 = batchRequest.getService(UserService.class);
        users.forEach(batchUserService3::delete);
        batchResponseItems = parseBatchResponse(batchRequest.commit().getResponse());
        assertEquals(10, batchResponseItems.stream().
                filter(item -> Response.Status.OK.getStatusCode() == item.getStatus()).count());
        assertEquals(1, batchResponseItems.stream().
                filter(item -> Response.Status.NOT_FOUND.getStatusCode() == item.getStatus()).count());

        try {
            userService.read(users.get(3));
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void unlink() throws IOException {
        UserCR userCR = getUniqueSample("unlink@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");
        userCR.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        ResourceDR resourceDR = new ResourceDR.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(resourceDR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void link() throws IOException {
        UserCR userCR = getUniqueSample("link@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        ResourceAR resourceAR = new ResourceAR.Builder().key(actual.getKey()).
                action(ResourceAssociationAction.LINK).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(resourceAR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void unassign() throws IOException {
        UserCR userCR = getUniqueSample("unassign@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");
        userCR.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        ResourceDR resourceDR = new ResourceDR.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.UNASSIGN).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(resourceDR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void assign() throws IOException {
        UserCR userCR = getUniqueSample("assign@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        ResourceAR resourceAR = new ResourceAR.Builder().key(actual.getKey()).
                value("password").action(ResourceAssociationAction.ASSIGN).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(resourceAR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void deprovision() throws IOException {
        UserCR userCR = getUniqueSample("deprovision@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");
        userCR.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        ResourceDR resourceDR = new ResourceDR.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.DEPROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(resourceDR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void provision() throws IOException {
        UserCR userCR = getUniqueSample("provision@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        ResourceAR resourceAR = new ResourceAR.Builder().key(actual.getKey()).
                value("password").action(ResourceAssociationAction.PROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(resourceAR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void deprovisionUnlinked() throws IOException {
        UserCR userCR = getUniqueSample("provision@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        ResourceAR resourceAR = new ResourceAR.Builder().key(actual.getKey()).
                value("password").action(ResourceAssociationAction.PROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(resourceAR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        ResourceDR resourceDR = new ResourceDR.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.DEPROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(resourceDR)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void restResource() {
        UserCR userCR = getUniqueSample("rest@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_REST);

        // 1. create
        ProvisioningResult<UserTO> result = createUser(userCR);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals(RESOURCE_NAME_REST, result.getPropagationStatuses().get(0).getResource());
        assertEquals("surname", result.getEntity().getPlainAttr("surname").get().getValues().get(0));

        // verify user exists on the backend REST service
        WebClient webClient = WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/users/" + result.getEntity().getKey());
        Response response = webClient.get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());

        // 2. update
        UserUR userUR = new UserUR.Builder(result.getEntity().getKey()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("surname").value("surname2").build()).build()).
                build();
        result = updateUser(userUR);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals(RESOURCE_NAME_REST, result.getPropagationStatuses().get(0).getResource());
        assertEquals("surname2", result.getEntity().getPlainAttr("surname").get().getValues().get(0));

        // verify user still exists on the backend REST service
        response = webClient.get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());

        // 3. delete
        result = deleteUser(result.getEntity().getKey());
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals(RESOURCE_NAME_REST, result.getPropagationStatuses().get(0).getResource());

        // verify user was removed by the backend REST service
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), webClient.get().getStatus());
    }

    @Test
    public void haveIBeenPwned() {
        ImplementationTO rule = new ImplementationTO();
        rule.setKey("HaveIBeenPwnedPasswordRuleConf" + getUUIDString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(IdRepoImplementationType.PASSWORD_RULE);
        rule.setBody(POJOHelper.serialize(new HaveIBeenPwnedPasswordRuleConf()));
        Response response = implementationService.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        PasswordPolicyTO pwdPolicy = new PasswordPolicyTO();
        pwdPolicy.setName("Have I Been Pwned?");
        pwdPolicy.getRules().add(rule.getKey());
        pwdPolicy = createPolicy(PolicyType.PASSWORD, pwdPolicy);
        assertNotNull(pwdPolicy.getKey());

        RealmTO realm = new RealmTO();
        realm.setName("hibp");
        realm.setPasswordPolicy(pwdPolicy.getKey());
        realmService.create(SyncopeConstants.ROOT_REALM, realm);

        UserCR userCR = getUniqueSample("hibp@syncope.apache.org");
        userCR.setRealm("/hibp");
        userCR.setPassword("password");
        try {
            createUser(userCR);
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertEquals("InvalidPassword: Password pwned", e.getElements().iterator().next());
        }

        userCR.setPassword('1' + RandomStringUtils.randomAlphanumeric(10));
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO.getKey());
    }
}
