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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
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
import org.apache.syncope.common.lib.policy.HaveIBeenPwnedPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.AttrTO;
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
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.StatusPatchType;
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
import org.apache.syncope.fit.core.reference.TestAccountRuleConf;
import org.apache.syncope.fit.core.reference.TestPasswordRuleConf;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.FlowableDetector;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.jupiter.api.Test;

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
    public void readPrivileges() {
        Set<String> privileges = userService.read("rossini").getPrivileges();
        assertNotNull(privileges);
        assertEquals(1, privileges.size());
    }

    @Test
    public void createUserWithNoPropagation() {
        // create a new user
        UserTO userTO = getUniqueSampleTO("xxx@xxx.xxx");
        userTO.setPassword("password123");
        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION);

        userTO = createUser(userTO).getEntity();

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
            fail("This should not happen");
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
                fail("This should not happen");
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
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void createWithInvalidPassword() {
        assertThrows(SyncopeClientException.class, () -> {
            UserTO userTO = getSampleTO("invalidpasswd@syncope.apache.org");
            userTO.setPassword("pass");
            createUser(userTO);
        });
    }

    @Test
    public void createWithInvalidUsername() {
        assertThrows(SyncopeClientException.class, () -> {
            UserTO userTO = getSampleTO("invalidusername@syncope.apache.org");
            userTO.setUsername("us");
            userTO.setRealm("/odd");

            createUser(userTO);
        });
    }

    @Test
    public void createWithInvalidPasswordByRes() {
        assertThrows(SyncopeClientException.class, () -> {
            UserTO userTO = getSampleTO("invalidPwdByRes@passwd.com");

            // configured to be minLength=16
            userTO.setPassword("password1");
            userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION);
            createUser(userTO);
        });
    }

    @Test
    public void createWithInvalidPasswordByGroup() {
        assertThrows(SyncopeClientException.class, () -> {
            UserTO userTO = getSampleTO("invalidPwdByGroup@passwd.com");

            // configured to be minLength=16
            userTO.setPassword("password1");

            userTO.getMemberships().add(new MembershipTO.Builder().
                    group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

            createUser(userTO);
        });
    }

    @Test
    public void createWithException() {
        assertThrows(SyncopeClientException.class, () -> {
            UserTO newUserTO = new UserTO();
            newUserTO.getPlainAttrs().add(attrTO("userId", "userId@nowhere.org"));
            createUser(newUserTO);
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
        assertTrue(newUserTO.getLastChangeDate().getTime() - newUserTO.getCreationDate().getTime() < 3000);

        assertFalse(newUserTO.getPlainAttrs().contains(attrWithInvalidSchemaTO));

        // check for changePwdDate
        assertNotNull(newUserTO.getCreationDate());

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
            fail("This should not happen");
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
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userTO.getPlainAttrs().add(attrTO("ctype", "F"));

        AttrTO surname = userTO.getPlainAttr("surname").get();
        userTO.getPlainAttrs().remove(surname);

        // 2. create user without surname (mandatory when type == 'F')
        try {
            createUser(userTO);
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
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

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

    @Test
    public void updateInvalidPassword() {
        assertThrows(SyncopeClientException.class, () -> {
            UserTO userTO = getSampleTO("updateinvalid@password.com");

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);

            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userTO.getKey());
            userPatch.setPassword(new PasswordPatch.Builder().value("pass").build());

            userService.update(userPatch);
        });
    }

    @Test
    public void updateSamePassword() {
        assertThrows(SyncopeClientException.class, () -> {
            UserTO userTO = getUniqueSampleTO("updatesame@password.com");
            userTO.setRealm("/even/two");

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);

            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userTO.getKey());
            userPatch.setPassword(new PasswordPatch.Builder().value("password123").build());

            userService.update(userPatch);
        });
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
        int beforeTasks = taskService.search(
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

        int afterTasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();
        assertFalse(afterTasks <= 0);

        assertTrue(beforeTasks < afterTasks);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyTaskRegistration() {
        // get task list
        List<PropagationTaskTO> tasks = taskService.<PropagationTaskTO>search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1000).build()).getResult();
        assertFalse(tasks.isEmpty());

        String maxKey = tasks.stream().
                max(Comparator.comparing(PropagationTaskTO::getStart, Comparator.nullsLast(Comparator.naturalOrder()))).
                map(PropagationTaskTO::getKey).orElse(null);
        assertNotNull(maxKey);

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
        tasks = taskService.<PropagationTaskTO>search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1000).build()).getResult();
        assertFalse(tasks.isEmpty());

        String newMaxKey = tasks.stream().
                max(Comparator.comparing(PropagationTaskTO::getStart, Comparator.nullsLast(Comparator.naturalOrder()))).
                map(PropagationTaskTO::getKey).orElse(null);
        assertNotNull(newMaxKey);

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
        tasks = taskService.<PropagationTaskTO>search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1000).build()).getResult();
        assertFalse(tasks.isEmpty());

        // default configuration for ws-target-resource2 during update:
        // all update executions have to be registered
        newMaxKey = tasks.stream().
                max(Comparator.comparing(PropagationTaskTO::getStart, Comparator.nullsLast(Comparator.naturalOrder()))).
                map(PropagationTaskTO::getKey).orElse(null);
        assertNotNull(newMaxKey);

        assertNotNull(taskService.read(TaskType.PROPAGATION, newMaxKey, false));

        // --------------------------------------
        // Delete operation
        // --------------------------------------
        userService.delete(userTO.getKey());

        // get the new task list
        tasks = taskService.<PropagationTaskTO>search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1000).build()).getResult();
        assertFalse(tasks.isEmpty());

        maxKey = newMaxKey;
        newMaxKey = tasks.stream().
                max(Comparator.comparing(PropagationTaskTO::getStart, Comparator.nullsLast(Comparator.naturalOrder()))).
                map(PropagationTaskTO::getKey).orElse(null);
        assertNotNull(newMaxKey);

        // default configuration for ws-target-resource2: no delete executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxKey, maxKey);
    }

    @Test
    public void createActivate() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(syncopeService));

        UserTO userTO = getUniqueSampleTO("createActivate@syncope.apache.org");

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("268fed79-f440-4390-9435-b273768eb5d6").build());

        userTO = createUser(userTO).getEntity();

        assertNotNull(userTO);
        assertNotNull(userTO.getToken());
        assertNotNull(userTO.getTokenExpireTime());

        assertEquals("created", userTO.getStatus());

        StatusPatch statusPatch = new StatusPatch.Builder().key(userTO.getKey()).
                type(StatusPatchType.ACTIVATE).token(userTO.getToken()).build();

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
        assertEquals(FlowableDetector.isFlowableEnabledForUserWorkflow(syncopeService)
                ? "active"
                : "created", userTO.getStatus());

        StatusPatch statusPatch = new StatusPatch.Builder().key(userTO.getKey()).
                type(StatusPatchType.SUSPEND).token(userTO.getToken()).build();

        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        statusPatch = new StatusPatch.Builder().key(userTO.getKey()).type(StatusPatchType.REACTIVATE).build();

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
        assertEquals(FlowableDetector.isFlowableEnabledForUserWorkflow(syncopeService)
                ? "active"
                : "created", userTO.getStatus());
        String userKey = userTO.getKey();

        // Suspend with effect on syncope, ldap and db => user should be suspended in syncope and all resources
        StatusPatch statusPatch = new StatusPatch.Builder().key(userKey).
                type(StatusPatchType.SUSPEND).
                onSyncope(true).
                resources(RESOURCE_NAME_TESTDB, RESOURCE_NAME_LDAP).
                build();
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
        statusPatch = new StatusPatch.Builder().key(userKey).
                type(StatusPatchType.SUSPEND).
                onSyncope(false).
                resources(RESOURCE_NAME_LDAP).
                build();
        userService.status(statusPatch);
        statusPatch.setType(StatusPatchType.REACTIVATE);
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userKey);
        assertFalse(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));

        // Reactivate on syncope and db => syncope and db should show the user as active
        statusPatch = new StatusPatch.Builder().key(userKey).
                type(StatusPatchType.REACTIVATE).
                onSyncope(true).
                resources(RESOURCE_NAME_TESTDB).
                build();
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
        UserService asyncService = asyncClient.nullPriorityAsync(asyncClient.getService(UserService.class), true);

        UserTO user = getUniqueSampleTO("async@syncope.apache.org");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user.getResources().add(RESOURCE_NAME_TESTDB2);
        user.getResources().add(RESOURCE_NAME_LDAP);

        ProvisioningResult<UserTO> result = asyncService.create(user, true).readEntity(
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

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertNotNull(userTO.getDerAttr("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertEquals("sx-dx", connObjectTO.getAttr("THEIRGROUP").get().getValues().get(0));
    }

    @Test
    public void customPolicyRules() {
        // Using custom policy rules with application/xml requires to overwrite
        // org.apache.syncope.common.lib.policy.AbstractAccountRuleConf's and / or
        // org.apache.syncope.common.lib.policy.AbstractPasswordRuleConf's
        // @XmlSeeAlso - the power of JAXB :-/
        assumeTrue(MediaType.APPLICATION_JSON_TYPE.equals(clientFactory.getContentType().getMediaType()));

        ImplementationTO implementationTO = new ImplementationTO();
        implementationTO.setKey("TestAccountRuleConf" + UUID.randomUUID().toString());
        implementationTO.setEngine(ImplementationEngine.JAVA);
        implementationTO.setType(ImplementationType.ACCOUNT_RULE);
        implementationTO.setBody(POJOHelper.serialize(new TestAccountRuleConf()));
        Response response = implementationService.create(implementationTO);
        implementationTO.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        AccountPolicyTO accountPolicy = new AccountPolicyTO();
        accountPolicy.setDescription("Account Policy with custom rules");
        accountPolicy.getRules().add(implementationTO.getKey());
        accountPolicy = createPolicy(PolicyType.ACCOUNT, accountPolicy);
        assertNotNull(accountPolicy);

        implementationTO = new ImplementationTO();
        implementationTO.setKey("TestPasswordRuleConf" + UUID.randomUUID().toString());
        implementationTO.setEngine(ImplementationEngine.JAVA);
        implementationTO.setType(ImplementationType.PASSWORD_RULE);
        implementationTO.setBody(POJOHelper.serialize(new TestPasswordRuleConf()));
        response = implementationService.create(implementationTO);
        implementationTO.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        PasswordPolicyTO passwordPolicy = new PasswordPolicyTO();
        passwordPolicy.setDescription("Password Policy with custom rules");
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
            UserTO user = getUniqueSampleTO("custompolicyrules@syncope.apache.org");
            user.setRealm(realm.getFullPath());
            try {
                createUser(user);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidPassword"));
            }

            user.setPassword(user.getPassword() + "XXX");
            try {
                createUser(user);
                fail("This should not happen");
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

            policyService.delete(PolicyType.PASSWORD, passwordPolicy.getKey());
            policyService.delete(PolicyType.ACCOUNT, accountPolicy.getKey());
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
    public void batch() throws IOException {
        List<String> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserTO userTO = getUniqueSampleTO("batch_" + i + "@apache.org");
            users.add(String.valueOf(createUser(userTO).getEntity().getKey()));
        }

        // check for a fail
        users.add(UUID.randomUUID().toString());

        assertEquals(11, users.size());

        BatchRequest batchRequest = adminClient.batch();

        UserService batchUserService = batchRequest.getService(UserService.class);
        users.forEach(user -> {
            batchUserService.status(
                    new StatusPatch.Builder().key(user).type(StatusPatchType.SUSPEND).onSyncope(true).build());
        });
        List<BatchResponseItem> batchResponseItems = parseBatchResponse(batchRequest.commit().getResponse());
        assertEquals(10, batchResponseItems.stream().
                filter(item -> Response.Status.OK.getStatusCode() == item.getStatus()).count());
        assertEquals(1, batchResponseItems.stream().
                filter(item -> Response.Status.NOT_FOUND.getStatusCode() == item.getStatus()).count());
        assertEquals("suspended", userService.read(users.get(3)).getStatus());

        UserService batchUserService2 = batchRequest.getService(UserService.class);
        users.forEach(user -> {
            batchUserService2.status(
                    new StatusPatch.Builder().key(user).type(StatusPatchType.REACTIVATE).onSyncope(true).build());
        });
        batchResponseItems = parseBatchResponse(batchRequest.commit().getResponse());
        assertEquals(10, batchResponseItems.stream().
                filter(item -> Response.Status.OK.getStatusCode() == item.getStatus()).count());
        assertEquals(1, batchResponseItems.stream().
                filter(item -> Response.Status.NOT_FOUND.getStatusCode() == item.getStatus()).count());
        assertEquals("active", userService.read(users.get(3)).getStatus());

        UserService batchUserService3 = batchRequest.getService(UserService.class);
        users.forEach(user -> {
            batchUserService3.delete(user);
        });
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
        UserTO userTO = getUniqueSampleTO("unlink@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(deassociationPatch)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void link() throws IOException {
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
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch.Builder().key(actual.getKey()).
                action(ResourceAssociationAction.LINK).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(associationPatch)));

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
        UserTO userTO = getUniqueSampleTO("unassign@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.UNASSIGN).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(deassociationPatch)));

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
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch.Builder().key(actual.getKey()).
                value("password123").action(ResourceAssociationAction.ASSIGN).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(associationPatch)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void deprovision() throws IOException {
        UserTO userTO = getUniqueSampleTO("deprovision@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.DEPROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(deassociationPatch)));

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
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch.Builder().key(actual.getKey()).
                value("password").action(ResourceAssociationAction.PROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(associationPatch)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));
    }

    @Test
    public void deprovisionUnlinked() throws IOException {
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
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch.Builder().key(actual.getKey()).
                value("password").action(ResourceAssociationAction.PROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.associate(associationPatch)));

        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.DEPROVISION).resource(RESOURCE_NAME_CSV).build();

        assertNotNull(parseBatchResponse(userService.deassociate(deassociationPatch)));

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
        UserTO userTO = getUniqueSampleTO("rest@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_REST);

        // 1. create
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals(RESOURCE_NAME_REST, result.getPropagationStatuses().get(0).getResource());
        assertEquals("surname", userTO.getPlainAttr("surname").get().getValues().get(0));

        // verify user exists on the backend REST service
        WebClient webClient = WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/users/" + result.getEntity().getKey());
        Response response = webClient.get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());

        // 2. update
        UserPatch patch = new UserPatch();
        patch.setKey(result.getEntity().getKey());
        patch.getPlainAttrs().add(new AttrPatch.Builder().
                attrTO(new AttrTO.Builder().schema("surname").value("surname2").build()).build());
        result = updateUser(patch);
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
        rule.setType(ImplementationType.PASSWORD_RULE);
        rule.setBody(POJOHelper.serialize(new HaveIBeenPwnedPasswordRuleConf()));
        Response response = implementationService.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        PasswordPolicyTO pwdPolicy = new PasswordPolicyTO();
        pwdPolicy.setDescription("Have I Been Pwned?");
        pwdPolicy.getRules().add(rule.getKey());
        pwdPolicy = createPolicy(PolicyType.PASSWORD, pwdPolicy);
        assertNotNull(pwdPolicy.getKey());

        RealmTO realm = new RealmTO();
        realm.setName("hibp");
        realm.setPasswordPolicy(pwdPolicy.getKey());
        realmService.create(SyncopeConstants.ROOT_REALM, realm);

        UserTO user = getUniqueSampleTO("hibp@syncope.apache.org");
        user.setRealm("/hibp");
        user.setPassword("password");
        try {
            createUser(user);
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertEquals("InvalidPassword: Password pwned", e.getElements().iterator().next());
        }

        user.setPassword("1" + RandomStringUtils.randomAlphanumeric(10));
        user = createUser(user).getEntity();
        assertNotNull(user.getKey());
    }
}
