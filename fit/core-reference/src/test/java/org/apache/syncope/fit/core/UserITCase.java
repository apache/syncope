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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.BulkActionResult.Status;
import org.apache.syncope.common.lib.to.ConnObjectTO;
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
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.StatusPatchType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.core.reference.TestAccountRuleConf;
import org.apache.syncope.fit.core.reference.TestPasswordRuleConf;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.FlowableDetector;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.Assume;
import org.junit.Test;

public class UserITCase extends AbstractITCase {

    private boolean getBooleanAttribute(final ConnObjectTO connObjectTO, final String attrName) {
        return Boolean.parseBoolean(connObjectTO.getAttr(attrName).get().getValues().get(0));
    }

    public static UserTO getUniqueSampleTO(final String email) {
        return getSampleTO(getUUIDString() + email);
    }

    public static UserTO getSampleTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword("password123");
        userTO.setUsername(email);

        userTO.getPlainAttrs().add(attrTO("fullname", email));
        userTO.getPlainAttrs().add(attrTO("firstname", email));
        userTO.getPlainAttrs().add(attrTO("surname", "surname"));
        userTO.getPlainAttrs().add(attrTO("ctype", "a type"));
        userTO.getPlainAttrs().add(attrTO("userId", email));
        userTO.getPlainAttrs().add(attrTO("email", email));
        userTO.getPlainAttrs().add(
                attrTO("loginDate", DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date())));

        return userTO;
    }

    @Test
    public void createUserWithNoPropagation() {
        // create a new user
        UserTO userTO = getUniqueSampleTO("xxx@xxx.xxx");
        userTO.setPassword("password123");
        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION);

        userTO = createUser(userTO).getEntity();

        // get the propagation task just created
        PagedResult<PropagationTaskTO> tasks = taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).
                anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        PropagationTaskTO taskTO = tasks.getResult().get(0);
        assertNotNull(taskTO);
        assertFalse(taskTO.getExecutions().isEmpty());
        assertEquals(PropagationTaskExecStatus.NOT_ATTEMPTED.name(), taskTO.getExecutions().get(0).getStatus());
    }

    @Test
    public void enforceMandatoryCondition() {
        UserTO userTO = getUniqueSampleTO("enforce@apache.org");
        userTO.getResources().add(RESOURCE_NAME_WS2);
        userTO.setPassword("newPassword12");

        AttrTO type = null;
        for (AttrTO attr : userTO.getPlainAttrs()) {
            if ("ctype".equals(attr.getSchema())) {
                type = attr;
            }
        }
        assertNotNull(type);
        userTO.getPlainAttrs().remove(type);

        try {
            userTO = createUser(userTO).getEntity();
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userTO.getPlainAttrs().add(type);
        userTO = createUser(userTO).getEntity();
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
            UserTO userTO = getUniqueSampleTO("syncope222@apache.org");
            userTO.getResources().add(resourceTO.getKey());
            userTO.setPassword("newPassword12");

            try {
                userTO = createUser(userTO).getEntity();
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
            }

            userTO.getAuxClasses().add("csv");

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);
            assertEquals(Collections.singleton(resourceTO.getKey()), userTO.getResources());
        } finally {
            resourceService.delete(resourceTO.getKey());
        }
    }

    @Test
    public void createUserWithDbPropagation() {
        UserTO userTO = getUniqueSampleTO("yyy@yyy.yyy");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithInvalidPassword() {
        UserTO userTO = getSampleTO("invalidpasswd@syncope.apache.org");
        userTO.setPassword("pass");
        createUser(userTO);
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithInvalidUsername() {
        UserTO userTO = getSampleTO("invalidusername@syncope.apache.org");
        userTO.setUsername("us");
        userTO.setRealm("/odd");

        createUser(userTO);
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithInvalidPasswordByRes() {
        UserTO userTO = getSampleTO("invalidPwdByRes@passwd.com");

        // configured to be minLength=16
        userTO.setPassword("password1");
        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION);
        createUser(userTO);
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithInvalidPasswordByGroup() {
        UserTO userTO = getSampleTO("invalidPwdByGroup@passwd.com");

        // configured to be minLength=16
        userTO.setPassword("password1");

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        createUser(userTO);
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithException() {
        UserTO newUserTO = new UserTO();
        newUserTO.getPlainAttrs().add(attrTO("userId", "userId@nowhere.org"));
        createUser(newUserTO);
    }

    @Test
    public void create() {
        // get task list
        PagedResult<PropagationTaskTO> tasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        String maxKey = tasks.getResult().iterator().next().getKey();
        PropagationTaskTO taskTO = taskService.read(maxKey, true);

        assertNotNull(taskTO);
        int maxTaskExecutions = taskTO.getExecutions().size();

        UserTO userTO = getUniqueSampleTO("a.b@c.com");

        // add a membership
        userTO.getMemberships().add(new MembershipTO.Builder().
                group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        // add an attribute with a non-existing schema: must be ignored
        AttrTO attrWithInvalidSchemaTO = attrTO("invalid schema", "a value");
        userTO.getPlainAttrs().add(attrWithInvalidSchemaTO);

        // add an attribute with null value: must be ignored
        userTO.getPlainAttrs().add(attrTO("activationDate", null));

        // 1. create user
        UserTO newUserTO = createUser(userTO).getEntity();

        assertNotNull(newUserTO);

        // issue SYNCOPE-15
        assertNotNull(newUserTO.getCreationDate());
        assertNotNull(newUserTO.getCreator());
        assertNotNull(newUserTO.getLastChangeDate());
        assertNotNull(newUserTO.getLastModifier());
        assertEquals(newUserTO.getCreationDate(), newUserTO.getLastChangeDate());

        assertFalse(newUserTO.getPlainAttrs().contains(attrWithInvalidSchemaTO));

        // check for changePwdDate
        assertNotNull(newUserTO.getCreationDate());

        // get the new task list
        tasks = taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        String newMaxKey = tasks.getResult().iterator().next().getKey();

        // default configuration for ws-target-resource2:
        // only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxKey, maxKey);

        // get last task
        taskTO = taskService.read(newMaxKey, true);

        assertNotNull(taskTO);
        assertEquals(maxTaskExecutions, taskTO.getExecutions().size());

        // 3. verify password
        try {
            Pair<Map<String, Set<String>>, UserTO> self =
                    clientFactory.create(newUserTO.getUsername(), "password123").self();
            assertNotNull(self);
        } catch (AccessControlException e) {
            fail("Credentials should be valid and not cause AccessControlException");
        }

        try {
            clientFactory.create(newUserTO.getUsername(), "passwordXX").getService(UserSelfService.class);
            fail("Credentials are invalid, thus request should raise AccessControlException");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. try (and fail) to create another user with same (unique) values
        userTO = getSampleTO(userTO.getUsername());
        AttrTO userIdAttr = userTO.getPlainAttr("userId").get();
        userIdAttr.getValues().clear();
        userIdAttr.getValues().add("a.b@c.com");

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void createWithRequiredValueMissing() {
        UserTO userTO = getUniqueSampleTO("a.b@c.it");

        AttrTO type = userTO.getPlainAttr("ctype").get();
        userTO.getPlainAttrs().remove(type);

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        // 1. create user without type (mandatory by UserSchema)
        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userTO.getPlainAttrs().add(attrTO("ctype", "F"));

        AttrTO surname = userTO.getPlainAttr("surname").get();
        userTO.getPlainAttrs().remove(surname);

        // 2. create user without surname (mandatory when type == 'F')
        try {
            createUser(userTO);
            fail();
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

        UserTO userTO = getSampleTO("qqgf.z@nn.com");

        // specify a propagation
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO).getEntity();

        String key = userTO.getKey();

        ProvisioningResult<UserTO> result = deleteUser(key);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(key, userTO.getKey());
        assertTrue(userTO.getPlainAttrs().isEmpty());

        // check for propagation result
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

        try {
            userService.delete(userTO.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void deleteByUsername() {
        UserTO userTO = getSampleTO("delete.by.username@apache.org");

        // specify a propagation
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO).getEntity();

        String key = userTO.getKey();
        userTO = userService.read(key);

        ProvisioningResult<UserTO> result = deleteUser(userTO.getKey());
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(key, userTO.getKey());
        assertTrue(userTO.getPlainAttrs().isEmpty());

        // check for propagation result
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

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
        UserTO userTO = getUniqueSampleTO("updatewithout@password.com");

        userTO = createUser(userTO).getEntity();

        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().operation(PatchOperation.DELETE).
                attrTO(new AttrTO.Builder().schema("ctype").build()).
                build());

        userTO = updateUser(userPatch).getEntity();

        assertNotNull(userTO);
        assertFalse(userTO.getPlainAttr("ctype").isPresent());
    }

    @Test(expected = SyncopeClientException.class)
    public void updateInvalidPassword() {
        UserTO userTO = getSampleTO("updateinvalid@password.com");

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("pass").build());

        userService.update(userPatch);
    }

    @Test(expected = SyncopeClientException.class)
    public void updateSamePassword() {
        UserTO userTO = getUniqueSampleTO("updatesame@password.com");
        userTO.setRealm("/even/two");

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("password123").build());

        userService.update(userPatch);
    }

    @Test
    public void update() {
        UserTO userTO = getUniqueSampleTO("g.h@t.com");

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        userTO = createUser(userTO).getEntity();

        assertFalse(userTO.getDerAttrs().isEmpty());
        assertEquals(1, userTO.getMemberships().size());

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("new2Password").build());

        String newUserId = getUUIDString() + "t.w@spre.net";
        userPatch.getPlainAttrs().add(attrAddReplacePatch("userId", newUserId));

        String newFullName = getUUIDString() + "g.h@t.com";
        userPatch.getPlainAttrs().add(attrAddReplacePatch("fullname", newFullName));

        userPatch.getMemberships().add(new MembershipPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());
        userPatch.getMemberships().add(new MembershipPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                group(userTO.getMemberships().get(0).getGroupKey()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);

        // issue SYNCOPE-15
        assertNotNull(userTO.getCreationDate());
        assertNotNull(userTO.getCreator());
        assertNotNull(userTO.getLastChangeDate());
        assertNotNull(userTO.getLastModifier());
        assertTrue(userTO.getCreationDate().before(userTO.getLastChangeDate()));

        assertEquals(1, userTO.getMemberships().size());
        assertFalse(userTO.getDerAttrs().isEmpty());

        AttrTO userIdAttr = userTO.getPlainAttr("userId").get();
        assertEquals(Collections.singletonList(newUserId), userIdAttr.getValues());

        AttrTO fullNameAttr = userTO.getPlainAttr("fullname").get();
        assertEquals(Collections.singletonList(newFullName), fullNameAttr.getValues());

        // update by username
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getUsername());
        String newUsername = UUID.randomUUID().toString();
        userPatch.setUsername(new StringReplacePatchItem.Builder().value(newUsername).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertEquals(newUsername, userTO.getUsername());
    }

    @Test
    public void updatePasswordOnly() {
        int beforeTasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();
        assertFalse(beforeTasks <= 0);

        UserTO userTO = getUniqueSampleTO("pwdonly@t.com");
        userTO.getMemberships().add(new MembershipTO.Builder().group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        userTO = createUser(userTO).getEntity();

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").resource(RESOURCE_NAME_WS2).build());

        userTO = updateUser(userPatch).getEntity();

        // check for changePwdDate
        assertNotNull(userTO.getChangePwdDate());

        int afterTasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();
        assertFalse(afterTasks <= 0);

        assertTrue(beforeTasks < afterTasks);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyTaskRegistration() {
        // get task list
        PagedResult<PropagationTaskTO> tasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        String maxKey = tasks.getResult().iterator().next().getKey();

        // --------------------------------------
        // Create operation
        // --------------------------------------
        UserTO userTO = getUniqueSampleTO("t@p.mode");

        // add a membership
        userTO.getMemberships().add(new MembershipTO.Builder().
                group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        // 1. create user
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // get the new task list
        tasks = taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());
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
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        userPatch.getPlainAttrs().add(attrAddReplacePatch("surname", "surname2"));

        userTO = updateUser(userPatch).getEntity();

        assertNotNull(userTO);

        // get the new task list
        tasks = taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());

        // default configuration for ws-target-resource2 during update:
        // all update executions have to be registered
        newMaxKey = tasks.getResult().iterator().next().getKey();

        PropagationTaskTO taskTO = taskService.read(newMaxKey, true);

        assertNotNull(taskTO);
        assertEquals(1, taskTO.getExecutions().size());

        // --------------------------------------
        // Delete operation
        // --------------------------------------
        userService.delete(userTO.getKey());

        // get the new task list
        tasks = taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build());

        maxKey = newMaxKey;
        newMaxKey = tasks.getResult().iterator().next().getKey();

        // default configuration for ws-target-resource2: no delete executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxKey, maxKey);
    }

    @Test
    public void createActivate() {
        Assume.assumeTrue(FlowableDetector.isFlowableEnabledForUsers(syncopeService));

        UserTO userTO = getUniqueSampleTO("createActivate@syncope.apache.org");

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("268fed79-f440-4390-9435-b273768eb5d6").build());

        userTO = createUser(userTO).getEntity();

        assertNotNull(userTO);
        assertNotNull(userTO.getToken());
        assertNotNull(userTO.getTokenExpireTime());

        assertEquals("created", userTO.getStatus());

        StatusPatch statusPatch = new StatusPatch();
        statusPatch.setKey(userTO.getKey());
        statusPatch.setType(StatusPatchType.ACTIVATE);
        statusPatch.setToken(userTO.getToken());
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();

        assertNotNull(userTO);
        assertNull(userTO.getToken());
        assertNull(userTO.getTokenExpireTime());
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void suspendReactivate() {
        UserTO userTO = getUniqueSampleTO("suspendReactivate@syncope.apache.org");

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("bf825fe1-7320-4a54-bd64-143b5c18ab97").build());

        userTO = createUser(userTO).getEntity();

        assertNotNull(userTO);
        assertEquals(FlowableDetector.isFlowableEnabledForUsers(syncopeService)
                ? "active"
                : "created", userTO.getStatus());

        StatusPatch statusPatch = new StatusPatch();
        statusPatch.setKey(userTO.getKey());
        statusPatch.setType(StatusPatchType.SUSPEND);
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        statusPatch = new StatusPatch();
        statusPatch.setKey(userTO.getKey());
        statusPatch.setType(StatusPatchType.REACTIVATE);
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
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
        UserTO userTO = getUniqueSampleTO("suspreactonresource@syncope.apache.org");
        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertEquals(FlowableDetector.isFlowableEnabledForUsers(syncopeService)
                ? "active"
                : "created", userTO.getStatus());
        String userKey = userTO.getKey();

        // Suspend with effect on syncope, ldap and db => user should be suspended in syncope and all resources
        StatusPatch statusPatch = new StatusPatch();
        statusPatch.setKey(userKey);
        statusPatch.setType(StatusPatchType.SUSPEND);
        statusPatch.setOnSyncope(true);
        statusPatch.getResources().add(RESOURCE_NAME_TESTDB);
        statusPatch.getResources().add(RESOURCE_NAME_LDAP);
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userKey);
        assertFalse(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userKey);
        assertNotNull(connObjectTO);

        // Suspend and reactivate only on ldap => db and syncope should still show suspended
        statusPatch = new StatusPatch();
        statusPatch.setKey(userKey);
        statusPatch.setType(StatusPatchType.SUSPEND);
        statusPatch.setOnSyncope(false);
        statusPatch.getResources().add(RESOURCE_NAME_LDAP);
        userService.status(statusPatch);
        statusPatch.setType(StatusPatchType.REACTIVATE);
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userKey);
        assertFalse(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));

        // Reactivate on syncope and db => syncope and db should show the user as active
        statusPatch = new StatusPatch();
        statusPatch.setKey(userKey);
        statusPatch.setType(StatusPatchType.REACTIVATE);
        statusPatch.setOnSyncope(true);
        statusPatch.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userKey);
        assertTrue(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));
    }

    @Test
    public void updateMultivalueAttribute() {
        UserTO userTO = getUniqueSampleTO("multivalue@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getVirAttrs().clear();

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        AttrTO loginDate = userTO.getPlainAttr("loginDate").get();
        assertNotNull(loginDate);
        assertEquals(1, loginDate.getValues().size());

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        loginDate.getValues().add("2000-01-01");
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().
                operation(PatchOperation.ADD_REPLACE).attrTO(loginDate).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);

        loginDate = userTO.getPlainAttr("loginDate").get();
        assertNotNull(loginDate);
        assertEquals(2, loginDate.getValues().size());
    }

    private void verifyAsyncResult(final List<PropagationStatus> statuses) {
        assertEquals(3, statuses.size());

        Map<String, PropagationStatus> byResource = new HashMap<>(3);
        statuses.forEach(status -> {
            byResource.put(status.getResource(), status);
        });
        assertEquals(PropagationTaskExecStatus.SUCCESS, byResource.get(RESOURCE_NAME_LDAP).getStatus());
        assertTrue(byResource.get(RESOURCE_NAME_TESTDB).getStatus() == PropagationTaskExecStatus.CREATED
                || byResource.get(RESOURCE_NAME_TESTDB).getStatus() == PropagationTaskExecStatus.SUCCESS);
        assertTrue(byResource.get(RESOURCE_NAME_TESTDB2).getStatus() == PropagationTaskExecStatus.CREATED
                || byResource.get(RESOURCE_NAME_TESTDB2).getStatus() == PropagationTaskExecStatus.SUCCESS);
    }

    @Test
    public void async() {
        SyncopeClient asyncClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        UserService asyncService = asyncClient.nullPriorityAsync(asyncClient.getService(UserService.class), true);

        UserTO user = getUniqueSampleTO("async@syncope.apache.org");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user.getResources().add(RESOURCE_NAME_TESTDB2);
        user.getResources().add(RESOURCE_NAME_LDAP);

        ProvisioningResult<UserTO> result = asyncService.create(user).readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        });
        assertNotNull(result);
        verifyAsyncResult(result.getPropagationStatuses());

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(result.getEntity().getKey());
        userPatch.setPassword(new PasswordPatch.Builder().
                onSyncope(true).resources(RESOURCE_NAME_LDAP, RESOURCE_NAME_TESTDB, RESOURCE_NAME_TESTDB2).
                value("password321").build());

        result = asyncService.update(userPatch).readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        });
        assertNotNull(result);
        verifyAsyncResult(result.getPropagationStatuses());

        result = asyncService.delete(result.getEntity().getKey()).readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        });
        assertNotNull(result);
        verifyAsyncResult(result.getPropagationStatuses());
    }

    @Test
    public void groupAttrPropagation() {
        UserTO userTO = getUniqueSampleTO("checkGroupAttrPropagation@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();

        userTO.getAuxClasses().add("csv");

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("37d15e4c-cdc1-460b-a591-8505c8133806").build());

        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(actual.getDerAttr("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("sx-dx", connObjectTO.getAttr("THEIRGROUP").get().getValues().get(0));
    }

    @Test
    public void customPolicyRules() {
        // Using custom policy rules with application/xml requires to overwrite
        // org.apache.syncope.common.lib.policy.AbstractAccountRuleConf's and / or
        // org.apache.syncope.common.lib.policy.AbstractPasswordRuleConf's
        // @XmlSeeAlso - the power of JAXB :-/
        Assume.assumeTrue(MediaType.APPLICATION_JSON_TYPE.equals(clientFactory.getContentType().getMediaType()));

        AccountPolicyTO accountPolicy = new AccountPolicyTO();
        accountPolicy.setDescription("Account Policy with custom rules");
        accountPolicy.getRuleConfs().add(new TestAccountRuleConf());
        accountPolicy = createPolicy(accountPolicy);
        assertNotNull(accountPolicy);

        PasswordPolicyTO passwordPolicy = new PasswordPolicyTO();
        passwordPolicy.setDescription("Password Policy with custom rules");
        passwordPolicy.getRuleConfs().add(new TestPasswordRuleConf());
        passwordPolicy = createPolicy(passwordPolicy);
        assertNotNull(passwordPolicy);

        RealmTO realm = realmService.list("/even/two").get(0);
        String oldAccountPolicy = realm.getAccountPolicy();
        realm.setAccountPolicy(accountPolicy.getKey());
        String oldPasswordPolicy = realm.getPasswordPolicy();
        realm.setPasswordPolicy(passwordPolicy.getKey());
        realmService.update(realm);

        try {
            UserTO user = getUniqueSampleTO("custompolicyrules@syncope.apache.org");
            user.setRealm(realm.getFullPath());
            try {
                createUser(user);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidPassword"));
            }

            user.setPassword(user.getPassword() + "XXX");
            try {
                createUser(user);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidUsername"));
            }

            user.setUsername("YYY" + user.getUsername());
            user = createUser(user).getEntity();
            assertNotNull(user);
        } finally {
            realm.setAccountPolicy(oldAccountPolicy);
            realm.setPasswordPolicy(oldPasswordPolicy);
            realmService.update(realm);

            policyService.delete(passwordPolicy.getKey());
            policyService.delete(accountPolicy.getKey());
        }
    }

    @Test
    public void mappingPurpose() {
        UserTO userTO = getUniqueSampleTO("mpurpose@apache.org");
        userTO.getAuxClasses().add("csv");

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertFalse(connObjectTO.getAttr("email").isPresent());
    }

    @Test
    public void bulkActions() {
        BulkAction bulkAction = new BulkAction();

        for (int i = 0; i < 10; i++) {
            UserTO userTO = getUniqueSampleTO("bulk_" + i + "@apache.org");
            bulkAction.getTargets().add(String.valueOf(createUser(userTO).getEntity().getKey()));
        }

        // check for a fail
        bulkAction.getTargets().add(String.valueOf(Long.MAX_VALUE));

        assertEquals(11, bulkAction.getTargets().size());

        bulkAction.setType(BulkAction.Type.SUSPEND);
        BulkActionResult res = userService.bulk(bulkAction).readEntity(BulkActionResult.class);
        assertEquals(10, res.getResultByStatus(Status.SUCCESS).size());
        assertEquals(1, res.getResultByStatus(Status.FAILURE).size());
        assertEquals("suspended", userService.read(res.getResultByStatus(Status.SUCCESS).get(3)).getStatus());

        bulkAction.setType(BulkAction.Type.REACTIVATE);
        res = userService.bulk(bulkAction).readEntity(BulkActionResult.class);
        assertEquals(10, res.getResultByStatus(Status.SUCCESS).size());
        assertEquals(1, res.getResultByStatus(Status.FAILURE).size());
        assertEquals("active", userService.read(res.getResultByStatus(Status.SUCCESS).get(3)).getStatus());

        bulkAction.setType(BulkAction.Type.DELETE);
        res = userService.bulk(bulkAction).readEntity(BulkActionResult.class);
        assertEquals(10, res.getResultByStatus(Status.SUCCESS).size());
        assertEquals(1, res.getResultByStatus(Status.FAILURE).size());
    }

    @Test
    public void unlink() {
        UserTO userTO = getUniqueSampleTO("unlink@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
        deassociationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void link() {
        UserTO userTO = getUniqueSampleTO("link@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setAction(ResourceAssociationAction.LINK);
        associationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void unassign() {
        UserTO userTO = getUniqueSampleTO("unassign@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.UNASSIGN);
        deassociationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void assign() {
        UserTO userTO = getUniqueSampleTO("assign@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setValue("password");
        associationPatch.setAction(ResourceAssociationAction.ASSIGN);
        associationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void deprovision() {
        UserTO userTO = getUniqueSampleTO("deprovision@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.DEPROVISION);
        deassociationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void provision() {
        UserTO userTO = getUniqueSampleTO("provision@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setValue("password");
        associationPatch.setAction(ResourceAssociationAction.PROVISION);
        associationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void deprovisionUnlinked() {
        UserTO userTO = getUniqueSampleTO("provision@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setValue("password");
        associationPatch.setAction(ResourceAssociationAction.PROVISION);
        associationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.DEPROVISION);
        deassociationPatch.getResources().add(RESOURCE_NAME_CSV);

        assertNotNull(userService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void restResource() {
        UserTO userTO = getUniqueSampleTO("rest@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getResources().add("rest-target-resource");

        // 1. create
        ProvisioningResult<UserTO> result = userService.create(userTO).readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        });
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals("rest-target-resource", result.getPropagationStatuses().get(0).getResource());
        assertEquals("surname", userTO.getPlainAttr("surname").get().getValues().get(0));

        // verify user exists on the backend REST service
        WebClient webClient = WebClient.create(
                "http://localhost:9080/syncope-fit-build-tools/cxf/rest/users/" + result.getEntity().getKey());
        Response response = webClient.get();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());

        // 2. update
        UserPatch patch = new UserPatch();
        patch.setKey(result.getEntity().getKey());
        patch.getPlainAttrs().add(new AttrPatch.Builder().
                attrTO(new AttrTO.Builder().schema("surname").value("surname2").build()).build());
        result = userService.update(patch).readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        });
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals("rest-target-resource", result.getPropagationStatuses().get(0).getResource());
        assertEquals("surname2", result.getEntity().getPlainAttr("surname").get().getValues().get(0));

        // verify user still exists on the backend REST service
        response = webClient.get();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());

        // 3. delete
        result = userService.delete(result.getEntity().getKey()).readEntity(
                new GenericType<ProvisioningResult<UserTO>>() {
        });
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals("rest-target-resource", result.getPropagationStatuses().get(0).getResource());

        // verify user was removed by the backend REST service
        assertEquals(404, webClient.get().getStatus());
    }
}
