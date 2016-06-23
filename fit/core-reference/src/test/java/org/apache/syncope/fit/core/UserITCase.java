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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.naming.NamingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.BulkActionResult.Status;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
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
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.provisioning.java.propagation.DBPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPPasswordPropagationActions;
import org.apache.syncope.fit.core.reference.DoubleValueLogicActions;
import org.apache.syncope.fit.core.reference.TestAccountRuleConf;
import org.apache.syncope.fit.core.reference.TestPasswordRuleConf;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ActivitiDetector;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class UserITCase extends AbstractITCase {

    private static final FastDateFormat DATE_FORMAT = DateFormatUtils.ISO_DATE_FORMAT;

    private boolean getBooleanAttribute(final ConnObjectTO connObjectTO, final String attrName) {
        return Boolean.parseBoolean(connObjectTO.getPlainAttrMap().get(attrName).getValues().get(0));
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
        userTO.getPlainAttrs().add(attrTO("loginDate", DATE_FORMAT.format(new Date())));
        userTO.getDerAttrs().add(attrTO("cn", null));
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
                anyTypeKind(AnyTypeKind.USER).anyTypeKey(userTO.getKey()).page(1).size(1).build());
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        PropagationTaskTO taskTO = tasks.getResult().get(0);
        assertNotNull(taskTO);
        assertFalse(taskTO.getExecutions().isEmpty());
        assertEquals(PropagationTaskExecStatus.NOT_ATTEMPTED.name(), taskTO.getExecutions().get(0).getStatus());
    }

    @Test
    public void issue186() {
        // 1. create an user with strict mandatory attributes only
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "issue186@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password123");

        userTO.getPlainAttrs().add(attrTO("userId", userId));
        userTO.getPlainAttrs().add(attrTO("fullname", userId));
        userTO.getPlainAttrs().add(attrTO("surname", userId));

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource forcing mandatory constraints: must fail with RequiredValuesMissing
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS2).build());

        try {
            userTO = updateUser(userPatch).getEntity();
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        // 3. update assigning a resource NOT forcing mandatory constraints
        // AND priority: must fail with PropagationException
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        userTO = result.getEntity();

        // 4. update assigning a resource NOT forcing mandatory constraints
        // BUT not priority: must succeed
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123456").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_CSV).build());

        updateUser(userPatch);
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
            userTO.getDerAttrs().add(new AttrTO.Builder().schema("csvuserid").build());

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

        UserSelfService userSelfService2 = clientFactory.create(
                newUserTO.getUsername(), "passwordXX").getService(UserSelfService.class);
        try {
            userSelfService2.read();
            fail("Credentials are invalid, thus request should raise AccessControlException");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. try (and fail) to create another user with same (unique) values
        userTO = getSampleTO(userTO.getUsername());
        AttrTO userIdAttr = userTO.getPlainAttrMap().get("userId");
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

        AttrTO type = userTO.getPlainAttrMap().get("ctype");
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

        AttrTO surname = userTO.getPlainAttrMap().get("surname");
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
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build());
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());

        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }
    }

    @Test
    public void paginatedList() {
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
    public void readWithMailAddressAsUserName() {
        UserTO userTO = createUser(getUniqueSampleTO("mail@domain.org")).getEntity();
        userTO = userService.read(userTO.getKey());
        assertNotNull(userTO);
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
        assertNotNull(userTO.getDerAttrMap());
        assertFalse(userTO.getPlainAttrMap().containsKey("ctype"));
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

        AttrTO userIdAttr = userTO.getPlainAttrMap().get("userId");
        assertEquals(Collections.singletonList(newUserId), userIdAttr.getValues());

        AttrTO fullNameAttr = userTO.getPlainAttrMap().get("fullname");
        assertEquals(Collections.singletonList(newFullName), fullNameAttr.getValues());
    }

    @Test
    public void updatePasswordOnly() {
        int beforeTasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();
        assertFalse(beforeTasks <= 0);

        UserTO userTO = getUniqueSampleTO("pwdonly@t.com");
        userTO.getMemberships().add(new MembershipTO.Builder().
                group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        userTO = createUser(userTO).getEntity();

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").resource(RESOURCE_NAME_WS2).build());

        userTO = updateUser(userPatch).getEntity();

        // check for changePwdDate
        assertNotNull(userTO.getChangePwdDate());

        int afterTasks = taskService.list(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();
        assertFalse(beforeTasks <= 0);

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
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers(syncopeService));

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
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers(syncopeService)
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
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers(syncopeService)
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
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        AttrTO loginDate = userTO.getPlainAttrMap().get("loginDate");
        assertNotNull(loginDate);
        assertEquals(1, loginDate.getValues().size());

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        loginDate.getValues().add("2000-01-01");
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().
                operation(PatchOperation.ADD_REPLACE).attrTO(loginDate).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);

        loginDate = userTO.getPlainAttrMap().get("loginDate");
        assertNotNull(loginDate);
        assertEquals(2, loginDate.getValues().size());
    }

    private void verifyAsyncResult(final List<PropagationStatus> statuses) {
        assertEquals(3, statuses.size());

        Map<String, PropagationStatus> byResource = new HashMap<>(3);
        MapUtils.populateMap(byResource, statuses, new Transformer<PropagationStatus, String>() {

            @Override
            public String transform(final PropagationStatus status) {
                return status.getResource();
            }
        });
        assertEquals(PropagationTaskExecStatus.SUCCESS, byResource.get(RESOURCE_NAME_LDAP).getStatus());
        assertTrue(byResource.get(RESOURCE_NAME_TESTDB).getStatus() == PropagationTaskExecStatus.CREATED
                || byResource.get(RESOURCE_NAME_TESTDB).getStatus() == PropagationTaskExecStatus.SUCCESS);
        assertTrue(byResource.get(RESOURCE_NAME_TESTDB2).getStatus() == PropagationTaskExecStatus.CREATED
                || byResource.get(RESOURCE_NAME_TESTDB2).getStatus() == PropagationTaskExecStatus.SUCCESS);
    }

    @Test
    public void async() {
        UserService asyncService =
                clientFactory.create(ADMIN_UNAME, ADMIN_PWD).nullPriorityAsync(UserService.class, true);

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

    @Test(expected = EmptyResultDataAccessException.class)
    public void issue213() {
        UserTO userTO = getUniqueSampleTO("issue213@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getResources().size());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String username = jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", String.class,
                userTO.getUsername());

        assertEquals(userTO.getUsername(), username);

        UserPatch userPatch = new UserPatch();

        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(RESOURCE_NAME_TESTDB).build());

        userTO = updateUser(userPatch).getEntity();
        assertTrue(userTO.getResources().isEmpty());

        jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
    }

    @Test
    public void issue234() {
        UserTO inUserTO = getUniqueSampleTO("issue234@syncope.apache.org");
        inUserTO.getResources().add(RESOURCE_NAME_LDAP);

        UserTO userTO = createUser(inUserTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();

        userPatch.setKey(userTO.getKey());
        userPatch.setUsername(new StringReplacePatchItem.Builder().value("1" + userTO.getUsername()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertEquals("1" + inUserTO.getUsername(), userTO.getUsername());
    }

    @Test
    public final void issue280() {
        UserTO userTO = getUniqueSampleTO("issue280@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).
                resource(RESOURCE_NAME_TESTDB).value("123password").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);

        List<PropagationStatus> propagations = result.getPropagationStatuses();
        assertNotNull(propagations);
        assertEquals(1, propagations.size());

        assertEquals(PropagationTaskExecStatus.SUCCESS, propagations.get(0).getStatus());

        String resource = propagations.get(0).getResource();
        assertEquals(RESOURCE_NAME_TESTDB, resource);
    }

    @Test
    public void issue281() {
        UserTO userTO = getUniqueSampleTO("issue281@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getResources().add(RESOURCE_NAME_CSV);

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);

        List<PropagationStatus> propagations = result.getPropagationStatuses();
        assertNotNull(propagations);
        assertEquals(1, propagations.size());
        assertNotEquals(PropagationTaskExecStatus.SUCCESS, propagations.get(0).getStatus());

        String resource = propagations.get(0).getResource();
        assertEquals(RESOURCE_NAME_CSV, resource);
    }

    @Test
    public void issue288() {
        UserTO userTO = getSampleTO("issue288@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO("aLong", "STRING"));

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidValues, e.getType());
        }
    }

    @Test
    public void groupAttrPropagation() {
        UserTO userTO = getUniqueSampleTO("checkGroupAttrPropagation@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();

        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("37d15e4c-cdc1-460b-a591-8505c8133806").build());

        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(actual.getDerAttrMap().get("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("sx-dx", connObjectTO.getPlainAttrMap().get("THEIRGROUP").getValues().get(0));
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
    public void issueSYNCOPE108() {
        UserTO userTO = getUniqueSampleTO("syncope108@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("0626100b-a4ba-4e00-9971-86fad52a6216").build());
        userTO.getMemberships().add(new MembershipTO.Builder().
                group("ba9ed509-b1f5-48ab-a334-c8530a6422dc").build());

        userTO.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertEquals(2, userTO.getMemberships().size());
        assertEquals(1, userTO.getResources().size());

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // -----------------------------------
        // Remove the first membership: de-provisioning shouldn't happen
        // -----------------------------------
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        userPatch.getMemberships().add(new MembershipPatch.Builder().
                operation(PatchOperation.DELETE).group(userTO.getMemberships().get(0).getGroupKey()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the resource assigned directly: de-provisioning shouldn't happen
        // -----------------------------------
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        userPatch.getResources().add(new StringPatchItem.Builder().operation(PatchOperation.DELETE).
                value(userTO.getResources().iterator().next()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertFalse(userTO.getResources().isEmpty());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the first membership: de-provisioning should happen
        // -----------------------------------
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        userPatch.getMemberships().add(new MembershipPatch.Builder().
                operation(PatchOperation.DELETE).group(userTO.getMemberships().get(0).getGroupKey()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getMemberships().isEmpty());
        assertTrue(userTO.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
            fail("Read should not succeeed");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE185() {
        // 1. create user with LDAP resource, succesfully propagated
        UserTO userTO = getSampleTO("syncope185@syncope.apache.org");
        userTO.getVirAttrs().clear();
        userTO.getResources().add(RESOURCE_NAME_LDAP);

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        // 2. delete this user
        userService.delete(userTO.getKey());

        // 3. try (and fail) to find this user on the external LDAP resource
        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
            fail("This entry should not be present on this resource");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test()
    public void issueSYNCOPE51() {
        AttrTO defaultCA = configurationService.get("password.cipher.algorithm");
        final String originalCAValue = defaultCA.getValues().get(0);
        defaultCA.getValues().set(0, "MD5");
        configurationService.set(defaultCA);

        AttrTO newCA = configurationService.get(defaultCA.getSchema());
        assertEquals(defaultCA, newCA);

        UserTO userTO = getSampleTO("syncope51@syncope.apache.org");
        userTO.setPassword("password");

        try {
            createUser(userTO);
            fail("Create user should not succeed");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getElements().iterator().next().contains("MD5"));
        }

        defaultCA.getValues().set(0, originalCAValue);
        configurationService.set(defaultCA);

        AttrTO oldCA = configurationService.get(defaultCA.getSchema());
        assertEquals(defaultCA, oldCA);
    }

    @Test
    public void issueSYNCOPE267() {
        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserTO userTO = getUniqueSampleTO("syncope267@apache.org");
        userTO.getVirAttrs().add(attrTO("virtualdata", "virtualvalue"));
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_DBVIRATTR, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_DBVIRATTR, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertEquals("virtualvalue", connObjectTO.getPlainAttrMap().get("USERNAME").getValues().get(0));
        // ----------------------------------

        userTO = userService.read(userTO.getKey());

        assertNotNull(userTO);
        assertEquals(1, userTO.getVirAttrs().size());
        assertEquals("virtualvalue", userTO.getVirAttrs().iterator().next().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE266() {
        UserTO userTO = getUniqueSampleTO("syncope266@apache.org");
        userTO.getResources().clear();

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        // this resource has not a mapping for Password
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_UPDATE).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE279() {
        UserTO userTO = getUniqueSampleTO("syncope279@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_TIMEOUT);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertEquals(RESOURCE_NAME_TIMEOUT, result.getPropagationStatuses().get(0).getResource());
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        assertEquals(PropagationTaskExecStatus.FAILURE, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE122() {
        // 1. create user on testdb and testdb2
        UserTO userTO = getUniqueSampleTO("syncope122@apache.org");
        userTO.getResources().clear();

        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        userTO.getResources().add(RESOURCE_NAME_TESTDB2);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB2));

        final String pwdOnSyncope = userTO.getPassword();

        ConnObjectTO userOnDb = resourceService.readConnObject(
                RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDbAttr = userOnDb.getPlainAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDbAttr);
        assertNotNull(pwdOnTestDbAttr.getValues());
        assertFalse(pwdOnTestDbAttr.getValues().isEmpty());
        final String pwdOnTestDb = pwdOnTestDbAttr.getValues().iterator().next();

        ConnObjectTO userOnDb2 = resourceService.readConnObject(
                RESOURCE_NAME_TESTDB2, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDb2Attr = userOnDb2.getPlainAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDb2Attr);
        assertNotNull(pwdOnTestDb2Attr.getValues());
        assertFalse(pwdOnTestDb2Attr.getValues().isEmpty());
        final String pwdOnTestDb2 = pwdOnTestDb2Attr.getValues().iterator().next();

        // 2. request to change password only on testdb (no Syncope, no testdb2)
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value(getUUIDString()).onSyncope(false).
                resource(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        userTO = result.getEntity();

        // 3a. Chech that only a single propagation took place
        assertNotNull(result.getPropagationStatuses());
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_TESTDB, result.getPropagationStatuses().iterator().next().getResource());

        // 3b. verify that password hasn't changed on Syncope
        assertEquals(pwdOnSyncope, userTO.getPassword());

        // 3c. verify that password *has* changed on testdb
        userOnDb = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDbAttrAfter = userOnDb.getPlainAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDbAttrAfter);
        assertNotNull(pwdOnTestDbAttrAfter.getValues());
        assertFalse(pwdOnTestDbAttrAfter.getValues().isEmpty());
        assertNotEquals(pwdOnTestDb, pwdOnTestDbAttrAfter.getValues().iterator().next());

        // 3d. verify that password hasn't changed on testdb2
        userOnDb2 = resourceService.readConnObject(RESOURCE_NAME_TESTDB2, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDb2AttrAfter = userOnDb2.getPlainAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDb2AttrAfter);
        assertNotNull(pwdOnTestDb2AttrAfter.getValues());
        assertFalse(pwdOnTestDb2AttrAfter.getValues().isEmpty());
        assertEquals(pwdOnTestDb2, pwdOnTestDb2AttrAfter.getValues().iterator().next());
    }

    @Test
    public void issueSYNCOPE136AES() {
        // 1. read configured cipher algorithm in order to be able to restore it at the end of test
        AttrTO pwdCipherAlgo = configurationService.get("password.cipher.algorithm");
        final String origpwdCipherAlgo = pwdCipherAlgo.getValues().get(0);

        // 2. set AES password cipher algorithm
        pwdCipherAlgo.getValues().set(0, "AES");
        configurationService.set(pwdCipherAlgo);

        UserTO userTO = null;
        try {
            // 3. create user with no resources
            userTO = getUniqueSampleTO("syncope136_AES@apache.org");
            userTO.getResources().clear();

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);

            // 4. update user, assign a propagation priority resource but don't provide any password
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userTO.getKey());
            userPatch.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
            userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

            ProvisioningResult<UserTO> result = updateUser(userPatch);
            assertNotNull(result);
            userTO = result.getEntity();
            assertNotNull(userTO);

            // 5. verify that propagation was successful
            List<PropagationStatus> props = result.getPropagationStatuses();
            assertNotNull(props);
            assertEquals(1, props.size());
            PropagationStatus prop = props.iterator().next();
            assertNotNull(prop);
            assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
            assertEquals(PropagationTaskExecStatus.SUCCESS, prop.getStatus());
        } finally {
            // restore initial cipher algorithm
            pwdCipherAlgo.getValues().set(0, origpwdCipherAlgo);
            configurationService.set(pwdCipherAlgo);

            if (userTO != null) {
                deleteUser(userTO.getKey());
            }
        }
    }

    @Test
    public void isseSYNCOPE136Random() {
        // 1. create user with no resources
        UserTO userTO = getUniqueSampleTO("syncope136_Random@apache.org");
        userTO.getResources().clear();
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 2. update user, assign a propagation priority resource but don't provide any password
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);

        // 3. verify that propagation was successful
        List<PropagationStatus> props = result.getPropagationStatuses();
        assertNotNull(props);
        assertEquals(1, props.size());
        PropagationStatus prop = props.iterator().next();
        assertNotNull(prop);
        assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, prop.getStatus());
    }

    @Test
    public void mappingPurpose() {
        UserTO userTO = getUniqueSampleTO("mpurpose@apache.org");
        userTO.getAuxClasses().add("csv");

        AttrTO csvuserid = new AttrTO();
        csvuserid.setSchema("csvuserid");
        userTO.getDerAttrs().add(csvuserid);

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNull(connObjectTO.getPlainAttrMap().get("email"));
    }

    @Test
    public void issueSYNCOPE265() {
        String[] userKeys = new String[] {
            "1417acbe-cbf6-4277-9372-e75e04f97000",
            "74cd8ece-715a-44a4-a736-e17b46c4e7e6",
            "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee",
            "c9b2dec2-00a7-4855-97c0-d854842b4b24",
            "823074dc-d280-436d-a7dd-07399fae48ec" };

        for (String userKey : userKeys) {
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userKey);
            userPatch.getPlainAttrs().add(attrAddReplacePatch("ctype", "a type"));
            UserTO userTO = updateUser(userPatch).getEntity();
            assertEquals("a type", userTO.getPlainAttrMap().get("ctype").getValues().get(0));
        }
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
    public void issueSYNCOPE354() {
        // change resource-ldap group mapping for including uniqueMember (need for assertions below)
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        for (MappingItemTO item : ldap.getProvision(AnyTypeKind.GROUP.name()).getMapping().getItems()) {
            if ("description".equals(item.getExtAttrName())) {
                item.setExtAttrName("uniqueMember");
            }
        }
        resourceService.update(ldap);

        // 1. create group with LDAP resource
        GroupTO groupTO = new GroupTO();
        groupTO.setName("SYNCOPE354-" + getUUIDString());
        groupTO.setRealm("/");
        groupTO.getResources().add(RESOURCE_NAME_LDAP);

        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);

        // 2. create user with LDAP resource and membership of the above group
        UserTO userTO = getUniqueSampleTO("syncope354@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO.getMemberships().add(new MembershipTO.Builder().group(groupTO.getKey()).build());

        userTO = createUser(userTO).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));

        // 3. read group on resource, check that user DN is included in uniqueMember
        ConnObjectTO connObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObj);
        assertTrue(connObj.getPlainAttrMap().get("uniqueMember").getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 4. remove membership
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getMemberships().add(new MembershipPatch.Builder().operation(PatchOperation.DELETE).
                group(userTO.getMemberships().get(0).getGroupKey()).build());

        userTO = updateUser(userPatch).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));

        // 5. read group on resource, check that user DN was removed from uniqueMember
        connObj = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObj);
        assertFalse(connObj.getPlainAttrMap().get("uniqueMember").getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 6. restore original resource-ldap group mapping
        for (MappingItemTO item : ldap.getProvision(AnyTypeKind.GROUP.name()).getMapping().getItems()) {
            if ("uniqueMember".equals(item.getExtAttrName())) {
                item.setExtAttrName("description");
            }
        }
        resourceService.update(ldap);
    }

    @Test
    public void issueSYNCOPE357() throws IOException {
        // 1. create group with LDAP resource
        GroupTO groupTO = new GroupTO();
        groupTO.setName("SYNCOPE357-" + getUUIDString());
        groupTO.setRealm("/");
        groupTO.getResources().add(RESOURCE_NAME_LDAP);

        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);

        // 2. create user with membership of the above group
        UserTO userTO = getUniqueSampleTO("syncope357@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO("obscure", "valueToBeObscured"));
        userTO.getPlainAttrs().add(attrTO("photo",
                Base64Utility.encode(IOUtils.readBytesFromStream(getClass().getResourceAsStream("/favicon.jpg")))));
        userTO.getMemberships().add(new MembershipTO.Builder().group(groupTO.getKey()).build());

        userTO = createUser(userTO).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));
        assertNotNull(userTO.getPlainAttrMap().get("obscure"));
        assertNotNull(userTO.getPlainAttrMap().get("photo"));

        // 3. read user on resource
        ConnObjectTO connObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObj);
        AttrTO registeredAddress = connObj.getPlainAttrMap().get("registeredAddress");
        assertNotNull(registeredAddress);
        assertEquals(userTO.getPlainAttrMap().get("obscure").getValues(), registeredAddress.getValues());
        AttrTO jpegPhoto = connObj.getPlainAttrMap().get("jpegPhoto");
        assertNotNull(jpegPhoto);
        assertEquals(userTO.getPlainAttrMap().get("photo").getValues(), jpegPhoto.getValues());

        // 4. remove group
        groupService.delete(groupTO.getKey());

        // 5. try to read user on resource: fail
        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE383() {
        // 1. create user without resources
        UserTO userTO = getUniqueSampleTO("syncope383@apache.org");
        userTO.getResources().clear();
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 2. assign resource without specifying a new pwd and check propagation failure
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertNotEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        userTO = result.getEntity();

        // 3. request to change password only on testdb
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(
                new PasswordPatch.Builder().value(getUUIDString() + "abbcbcbddd123").resource(RESOURCE_NAME_TESTDB).
                build());

        result = updateUser(userPatch);
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE402() {
        // 1. create an user with strict mandatory attributes only
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "syncope402@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password123");

        userTO.getPlainAttrs().add(attrTO("userId", userId));
        userTO.getPlainAttrs().add(attrTO("fullname", userId));
        userTO.getPlainAttrs().add(attrTO("surname", userId));

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource NOT forcing mandatory constraints
        // AND priority: must fail with PropagationException
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());
        ProvisioningResult<UserTO> result = updateUser(userPatch);

        List<PropagationStatus> propagationStatuses = result.getPropagationStatuses();
        PropagationStatus ws1PropagationStatus = null;
        if (propagationStatuses != null) {
            for (PropagationStatus propStatus : propagationStatuses) {
                if (RESOURCE_NAME_WS1.equals(propStatus.getResource())) {
                    ws1PropagationStatus = propStatus;
                    break;
                }
            }
        }
        assertNotNull(ws1PropagationStatus);
        assertEquals(RESOURCE_NAME_WS1, ws1PropagationStatus.getResource());
        assertNotNull(ws1PropagationStatus.getFailureReason());
        assertEquals(PropagationTaskExecStatus.FAILURE, ws1PropagationStatus.getStatus());
    }

    @Test
    public void unlink() {
        UserTO userTO = getUniqueSampleTO("unlink@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));
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
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

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
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));
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
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

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
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));
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
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

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
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

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
    public void issueSYNCOPE420() {
        RealmTO realm = realmService.list("/even/two").iterator().next();
        assertNotNull(realm);
        realm.getActionsClassNames().add(DoubleValueLogicActions.class.getName());
        realmService.update(realm);

        UserTO userTO = getUniqueSampleTO("syncope420@syncope.apache.org");
        userTO.setRealm(realm.getFullPath());
        userTO.getPlainAttrs().add(attrTO("makeItDouble", "3"));

        userTO = createUser(userTO).getEntity();
        assertEquals("6", userTO.getPlainAttrMap().get("makeItDouble").getValues().get(0));

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("makeItDouble", "7"));

        userTO = updateUser(userPatch).getEntity();
        assertEquals("14", userTO.getPlainAttrMap().get("makeItDouble").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE426() {
        UserTO userTO = getUniqueSampleTO("syncope426@syncope.apache.org");
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("anotherPassword123").build());
        userTO = userService.update(userPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE435() {
        // 1. create user without password
        UserTO userTO = getUniqueSampleTO("syncope435@syncope.apache.org");
        userTO.setPassword(null);
        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);

        // 2. try to update user by subscribing a resource - works but propagation is not even attempted
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(Collections.singleton(RESOURCE_NAME_WS1), userTO.getResources());
        assertNotEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertTrue(result.getPropagationStatuses().get(0).getFailureReason().
                startsWith("Not attempted because there are mandatory attributes without value(s): [__PASSWORD__]"));
    }

    @Test
    public void issueSYNCOPE454() throws NamingException {
        // 1. create user with LDAP resource (with 'Generate password if missing' enabled)
        UserTO userTO = getUniqueSampleTO("syncope454@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 2. read resource configuration for LDAP binding
        ConnObjectTO connObject =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());

        // 3. try (and succeed) to perform simple LDAP binding with provided password ('password123')
        assertNotNull(getLdapRemoteObject(
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0),
                "password123",
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));

        // 4. update user without any password change request
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("surname", "surname2"));

        userService.update(userPatch);

        // 5. try (and succeed again) to perform simple LDAP binding: password has not changed
        assertNotNull(getLdapRemoteObject(
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0),
                "password123",
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE493() {
        // 1.  create user and check that firstname is not propagated on resource with mapping for firstname set to NONE
        UserTO userTO = getUniqueSampleTO("493@test.org");
        userTO.getResources().add(RESOURCE_NAME_WS1);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(userTO);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        ConnObjectTO actual =
                resourceService.readConnObject(RESOURCE_NAME_WS1, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(actual);
        // check if mapping attribute with purpose NONE really hasn't been propagated
        assertNull(actual.getPlainAttrMap().get("NAME"));

        // 2.  update resource ws-target-resource-1
        ResourceTO ws1 = resourceService.read(RESOURCE_NAME_WS1);
        assertNotNull(ws1);

        MappingTO ws1NewUMapping = ws1.getProvision(AnyTypeKind.USER.name()).getMapping();
        // change purpose from NONE to BOTH
        for (MappingItemTO itemTO : ws1NewUMapping.getItems()) {
            if ("firstname".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.BOTH);
            }
        }

        ws1.getProvision(AnyTypeKind.USER.name()).setMapping(ws1NewUMapping);

        resourceService.update(ws1);
        ResourceTO newWs1 = resourceService.read(ws1.getKey());
        assertNotNull(newWs1);

        // check for existence
        Collection<MappingItemTO> mapItems = newWs1.getProvision(AnyTypeKind.USER.name()).getMapping().getItems();
        assertNotNull(mapItems);
        assertEquals(7, mapItems.size());

        // 3.  update user and check firstname propagation        
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("firstname", "firstnameNew"));

        result = updateUser(userPatch);
        assertNotNull(userTO);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        ConnObjectTO newUser =
                resourceService.readConnObject(RESOURCE_NAME_WS1, AnyTypeKind.USER.name(), userTO.getKey());

        assertNotNull(newUser.getPlainAttrMap().get("NAME"));
        assertEquals("firstnameNew", newUser.getPlainAttrMap().get("NAME").getValues().get(0));

        // 4.  restore resource ws-target-resource-1 mapping
        ws1NewUMapping = newWs1.getProvision(AnyTypeKind.USER.name()).getMapping();
        // restore purpose from BOTH to NONE
        for (MappingItemTO itemTO : ws1NewUMapping.getItems()) {
            if ("firstname".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.NONE);
            }
        }

        newWs1.getProvision(AnyTypeKind.USER.name()).setMapping(ws1NewUMapping);

        resourceService.update(newWs1);
    }

    @Test
    public void issueSYNCOPE505DB() throws Exception {
        // 1. create user
        UserTO user = UserITCase.getUniqueSampleTO("syncope505-db@syncope.apache.org");
        user.setPassword("security123");
        user = createUser(user).getEntity();
        assertNotNull(user);
        assertTrue(user.getResources().isEmpty());

        // 2. Add DBPasswordPropagationActions
        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().add(DBPasswordPropagationActions.class.getName());
        resourceService.update(resourceTO);

        // 3. Add a db resource to the User
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_TESTDB).build());

        user = updateUser(userPatch).getEntity();
        assertNotNull(user);
        assertEquals(1, user.getResources().size());

        // 4. Check that the DB resource has the correct password
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = jdbcTemplate.queryForObject(
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("security123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 5. Remove DBPasswordPropagationActions
        resourceTO = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().remove(DBPasswordPropagationActions.class.getName());
        resourceService.update(resourceTO);
    }

    @Test
    public void issueSYNCOPE505LDAP() throws Exception {
        // 1. create user
        UserTO user = UserITCase.getUniqueSampleTO("syncope505-ldap@syncope.apache.org");
        user.setPassword("security123");
        user = createUser(user).getEntity();
        assertNotNull(user);
        assertTrue(user.getResources().isEmpty());

        // 2. Add LDAPPasswordPropagationActions
        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().add(LDAPPasswordPropagationActions.class.getName());
        resourceTO.setRandomPwdIfNotProvided(false);
        resourceService.update(resourceTO);

        // 3. Add a resource to the User
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());

        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

        user = updateUser(userPatch).getEntity();
        assertNotNull(user);
        assertEquals(1, user.getResources().size());

        // 4. Check that the LDAP resource has the correct password
        ConnObjectTO connObject =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());

        assertNotNull(getLdapRemoteObject(
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0),
                "security123",
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));

        // 5. Remove LDAPPasswordPropagationActions
        resourceTO = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().remove(LDAPPasswordPropagationActions.class.getName());
        resourceTO.setRandomPwdIfNotProvided(true);
        resourceService.update(resourceTO);
    }

    @Test
    public void issueSYNCOPE391() {
        // 1. create user on Syncope with null password
        UserTO userTO = getUniqueSampleTO("syncope391@syncope.apache.org");
        userTO.setPassword(null);

        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);
        assertNull(userTO.getPassword());

        // 2. create existing user on csv and check that password on Syncope is null and that password on resource
        // doesn't change
        userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword(null);
        userTO.setUsername("syncope391@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO("fullname", "fullname"));
        userTO.getPlainAttrs().add(attrTO("firstname", "nome0"));
        userTO.getPlainAttrs().add(attrTO("surname", "cognome0"));
        userTO.getPlainAttrs().add(attrTO("userId", "syncope391@syncope.apache.org"));
        userTO.getPlainAttrs().add(attrTO("email", "syncope391@syncope.apache.org"));
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);
        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has not changed
        assertEquals("password0", connObjectTO.getPlainAttrMap().
                get(OperationalAttributes.PASSWORD_NAME).getValues().get(0));
        assertNull(userTO.getPassword());

        // 3. create user with not null password and propagate onto resource-csv, specify not to save password on
        // Syncope local storage
        userTO = getUniqueSampleTO("syncope391@syncope.apache.org");
        userTO.setPassword("passwordTESTNULL1");
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

        userTO.getResources().add(RESOURCE_NAME_CSV);
        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);

        connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has been propagated and that saved userTO's password is null
        assertEquals("passwordTESTNULL1", connObjectTO.getPlainAttrMap().
                get(OperationalAttributes.PASSWORD_NAME).getValues().get(0));
        assertNull(userTO.getPassword());

        // 4. create user and propagate password on resource-csv and on Syncope local storage
        userTO = getUniqueSampleTO("syncope391@syncope.apache.org");
        userTO.setPassword("passwordTESTNULL1");
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

        userTO.getResources().add(RESOURCE_NAME_CSV);
        // storePassword true by default
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has been correctly propagated on Syncope and resource-csv as usual
        assertEquals("passwordTESTNULL1", connObjectTO.getPlainAttrMap().
                get(OperationalAttributes.PASSWORD_NAME).getValues().get(0));
        Pair<Map<String, Set<String>>, UserTO> self =
                clientFactory.create(userTO.getUsername(), "passwordTESTNULL1").self();
        assertNotNull(self);

        // 4. add password policy to resource with passwordNotStore to false --> must store password
        ResourceTO csv = resourceService.read(RESOURCE_NAME_CSV);
        assertNotNull(csv);
        try {
            csv.setPasswordPolicy("55e5de0b-c79c-4e66-adda-251b6fb8579a");
            resourceService.update(csv);
            csv = resourceService.read(RESOURCE_NAME_CSV);
            assertEquals("55e5de0b-c79c-4e66-adda-251b6fb8579a", csv.getPasswordPolicy());

            userTO = getUniqueSampleTO("syncope391@syncope.apache.org");
            userTO.setPassword(null);
            userTO.getDerAttrs().clear();
            userTO.getVirAttrs().clear();
            userTO.getAuxClasses().add("csv");
            userTO.getDerAttrs().add(attrTO("csvuserid", null));

            userTO.getResources().add(RESOURCE_NAME_CSV);
            createUser(userTO, false);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertTrue(e.getMessage().contains("Password mandatory"));
        } finally {
            // resource csv with null password policy
            csv.setPasswordPolicy(null);
            resourceService.update(csv);
        }
    }

    @Test
    public void issueSYNCOPE647() {
        UserTO userTO = getUniqueSampleTO("syncope647@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");
        userTO.getDerAttrs().add(attrTO("csvuserid", null));

        userTO.getAuxClasses().add("generic membership");
        userTO.getPlainAttrs().add(attrTO("postalAddress", "postalAddress"));

        userTO.getResources().add(RESOURCE_NAME_LDAP);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(actual.getDerAttrMap().get("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("postalAddress", connObjectTO.getPlainAttrMap().get("postalAddress").getValues().get(0));

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(actual.getKey());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("postalAddress", "newPostalAddress"));

        actual = updateUser(userPatch).getEntity();

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("newPostalAddress", connObjectTO.getPlainAttrMap().get("postalAddress").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE626() {
        PasswordPolicyTO passwordPolicy = new PasswordPolicyTO();
        passwordPolicy.setDescription("Password Policy for SYNCOPE-626");

        DefaultPasswordRuleConf ruleConf = new DefaultPasswordRuleConf();
        ruleConf.setUsernameAllowed(false);
        passwordPolicy.getRuleConfs().add(ruleConf);

        passwordPolicy = createPolicy(passwordPolicy);
        assertNotNull(passwordPolicy);

        RealmTO realm = realmService.list("/even/two").get(0);
        String oldPasswordPolicy = realm.getPasswordPolicy();
        realm.setPasswordPolicy(passwordPolicy.getKey());
        realmService.update(realm);

        try {
            UserTO user = getUniqueSampleTO("syncope626@syncope.apache.org");
            user.setRealm(realm.getFullPath());
            user.setPassword(user.getUsername());
            try {
                createUser(user);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidPassword"));
            }

            user.setPassword("password123");
            user = createUser(user).getEntity();
            assertNotNull(user);
        } finally {
            realm.setPasswordPolicy(oldPasswordPolicy);
            realmService.update(realm);

            policyService.delete(passwordPolicy.getKey());
        }

    }

    @Test
    public void issueSYNCOPE686() {
        // 1. read configured cipher algorithm in order to be able to restore it at the end of test
        AttrTO pwdCipherAlgo = configurationService.get("password.cipher.algorithm");
        String origpwdCipherAlgo = pwdCipherAlgo.getValues().get(0);

        // 2. set AES password cipher algorithm
        pwdCipherAlgo.getValues().set(0, "AES");
        configurationService.set(pwdCipherAlgo);

        try {
            // 3. create group with LDAP resource assigned
            GroupTO group = GroupITCase.getBasicSampleTO("syncope686");
            group.getResources().add(RESOURCE_NAME_LDAP);
            group = createGroup(group).getEntity();
            assertNotNull(group);

            // 4. create user with no resources
            UserTO userTO = getUniqueSampleTO("syncope686@apache.org");
            userTO.getResources().clear();

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);

            // 5. update user with the new group, and don't provide any password
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userTO.getKey());
            userPatch.getMemberships().add(new MembershipPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                    group(group.getKey()).build());

            ProvisioningResult<UserTO> result = updateUser(userPatch);
            assertNotNull(result);

            // 5. verify that propagation was successful
            List<PropagationStatus> props = result.getPropagationStatuses();
            assertNotNull(props);
            assertEquals(1, props.size());
            PropagationStatus prop = props.iterator().next();
            assertNotNull(prop);
            assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
            assertEquals(PropagationTaskExecStatus.SUCCESS, prop.getStatus());
        } finally {
            // restore initial cipher algorithm
            pwdCipherAlgo.getValues().set(0, origpwdCipherAlgo);
            configurationService.set(pwdCipherAlgo);
        }
    }

    @Test
    public void issueSYNCOPE710() {
        // 1. create groups for indirect resource assignment
        GroupTO ldapGroup = GroupITCase.getBasicSampleTO("syncope710.ldap");
        ldapGroup.getResources().add(RESOURCE_NAME_LDAP);
        ldapGroup = createGroup(ldapGroup).getEntity();

        GroupTO dbGroup = GroupITCase.getBasicSampleTO("syncope710.db");
        dbGroup.getResources().add(RESOURCE_NAME_TESTDB);
        dbGroup = createGroup(dbGroup).getEntity();

        // 2. create user with memberships for the groups created above
        UserTO userTO = getUniqueSampleTO("syncope710@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getMemberships().add(new MembershipTO.Builder().group(ldapGroup.getKey()).build());
        userTO.getMemberships().add(new MembershipTO.Builder().group(dbGroup.getKey()).build());

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertEquals(2, result.getPropagationStatuses().size());
        userTO = result.getEntity();

        // 3. request to propagate passwod only to db
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().
                onSyncope(false).resource(RESOURCE_NAME_TESTDB).value("newpassword123").build());

        result = updateUser(userPatch);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_TESTDB, result.getPropagationStatuses().get(0).getResource());
    }
}
