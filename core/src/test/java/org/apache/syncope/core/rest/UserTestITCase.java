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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.services.UserSelfService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.reqres.BulkActionResult.Status;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.PasswordPolicyTO;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.PropagationTaskTO;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.TaskType;
import org.apache.syncope.common.util.AttributableOperations;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.naming.NamingException;
import javax.ws.rs.core.EntityTag;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.mod.ResourceAssociationMod;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.Preference;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class UserTestITCase extends AbstractTest {

    private String getStringAttribute(final ConnObjectTO connObjectTO, final String attrName) {
        return connObjectTO.getAttrMap().get(attrName).getValues().get(0);
    }

    private boolean getBooleanAttribute(final ConnObjectTO connObjectTO, final String attrName) {
        return Boolean.parseBoolean(getStringAttribute(connObjectTO, attrName));
    }

    private long getMaxTaskId(final List<PropagationTaskTO> tasks) {
        long newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }
        return newMaxId;
    }

    public static UserTO getUniqueSampleTO(final String email) {
        return getSampleTO(getUUIDString() + email);
    }

    public static UserTO getSampleTO(final String email) {
        String uid = email;
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername(uid);

        userTO.getAttrs().add(attributeTO("fullname", uid));
        userTO.getAttrs().add(attributeTO("firstname", uid));
        userTO.getAttrs().add(attributeTO("surname", "surname"));
        userTO.getAttrs().add(attributeTO("type", "a type"));
        userTO.getAttrs().add(attributeTO("userId", uid));
        userTO.getAttrs().add(attributeTO("email", uid));
        userTO.getAttrs().add(attributeTO("loginDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
        userTO.getDerAttrs().add(attributeTO("cn", null));
        userTO.getVirAttrs().add(attributeTO("virtualdata", "virtualvalue"));
        return userTO;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createUserWithNoPropagation() {
        // get task list
        PagedResult<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        long maxId = getMaxTaskId(tasks.getResult());

        // create a new user
        UserTO userTO = getUniqueSampleTO("xxx@xxx.xxx");

        userTO.setPassword("password123");
        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION);

        createUser(userTO);

        // get the new task list
        tasks = taskService.list(TaskType.PROPAGATION);
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        long newMaxId = getMaxTaskId(tasks.getResult());

        assertTrue(newMaxId > maxId);

        // get last task
        PropagationTaskTO taskTO = taskService.read(newMaxId);

        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void issue172() {
        List<PasswordPolicyTO> policies = policyService.list(PolicyType.GLOBAL_PASSWORD);
        for (PasswordPolicyTO policyTO : policies) {
            policyService.delete(policyTO.getId());
        }

        try {
            UserTO userTO = getUniqueSampleTO("issue172@syncope.apache.org");
            createUser(userTO);
        } finally {
            for (PasswordPolicyTO policyTO : policies) {
                Response response = policyService.create(policyTO);
                PasswordPolicyTO cPolicyTO = getObject(
                        response.getLocation(), PolicyService.class, PasswordPolicyTO.class);
                assertNotNull(cPolicyTO);
            }
        }
    }

    @Test
    public void issue186() {
        // 1. create an user with strict mandatory attributes only
        UserTO userTO = new UserTO();
        String userId = getUUIDString() + "issue186@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password");

        userTO.getAttrs().add(attributeTO("userId", userId));
        userTO.getAttrs().add(attributeTO("fullname", userId));
        userTO.getAttrs().add(attributeTO("surname", userId));

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource forcing mandatory constraints: must fail with RequiredValuesMissing
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.getResourcesToAdd().add(RESOURCE_NAME_WS2);

        try {
            userTO = updateUser(userMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        // 3. update assigning a resource NOT forcing mandatory constraints
        // AND primary: must fail with PropagationException
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.getResourcesToAdd().add(RESOURCE_NAME_WS1);

        userTO = updateUser(userMod);
        assertNotNull(userTO.getPropagationStatusTOs().get(0).getFailureReason());

        // 4. update assigning a resource NOT forcing mandatory constraints
        // BUT not primary: must succeed
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.getResourcesToAdd().add("resource-db");

        try {
            updateUser(userMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSyncopeUser, e.getType());
        }
    }

    @Test
    public void enforceMandatoryCondition() {
        UserTO userTO = getUniqueSampleTO("enforce@apache.org");
        userTO.getResources().add(RESOURCE_NAME_WS2);
        userTO.setPassword("newPassword");

        AttributeTO type = null;
        for (AttributeTO attr : userTO.getAttrs()) {
            if ("type".equals(attr.getSchema())) {
                type = attr;
            }
        }
        assertNotNull(type);
        userTO.getAttrs().remove(type);

        try {
            userTO = createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userTO.getAttrs().add(type);
        userTO = createUser(userTO);
        assertNotNull(userTO);
    }

    @Test
    public void enforceMandatoryConditionOnDerived() {
        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_CSV);
        assertNotNull(resourceTO);
        resourceTO.setName("resource-csv-enforcing");
        resourceTO.setEnforceMandatoryCondition(true);

        Response response = resourceService.create(resourceTO);
        resourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
        assertNotNull(resourceTO);

        UserTO userTO = getUniqueSampleTO("syncope222@apache.org");
        userTO.getResources().add(resourceTO.getName());
        userTO.setPassword("newPassword");

        try {
            userTO = createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals(Collections.singleton("resource-csv-enforcing"), userTO.getResources());
    }

    @Test
    public void createUserWithDbPropagation() {
        UserTO userTO = getUniqueSampleTO("yyy@yyy.yyy");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals(1, userTO.getPropagationStatusTOs().size());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
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

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);

        userTO.getMemberships().add(membershipTO);

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
    public void createWithInvalidPasswordByRole() {
        UserTO userTO = getSampleTO("invalidPwdByRole@passwd.com");

        // configured to be minLength=16
        userTO.setPassword("password1");

        final MembershipTO membership = new MembershipTO();
        membership.setRoleId(8L);

        userTO.getMemberships().add(membership);

        createUser(userTO);
    }

    @Test(expected = SyncopeClientException.class)
    public void createWithException() {
        UserTO newUserTO = new UserTO();
        newUserTO.getAttrs().add(attributeTO("userId", "userId@nowhere.org"));
        createUser(newUserTO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void create() {
        // get task list
        PagedResult<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        long maxId = getMaxTaskId(tasks.getResult());
        PropagationTaskTO taskTO = taskService.read(maxId);

        assertNotNull(taskTO);
        int maxTaskExecutions = taskTO.getExecutions().size();

        UserTO userTO = getUniqueSampleTO("a.b@c.com");

        // add a membership
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.getMemberships().add(membershipTO);

        // add an attribute with no values: must be ignored
        membershipTO.getAttrs().add(attributeTO("subscriptionDate", null));

        // add an attribute with a non-existing schema: must be ignored
        AttributeTO attrWithInvalidSchemaTO = attributeTO("invalid schema", "a value");
        userTO.getAttrs().add(attrWithInvalidSchemaTO);

        // add an attribute with null value: must be ignored
        userTO.getAttrs().add(attributeTO("activationDate", null));

        // 1. create user
        UserTO newUserTO = createUser(userTO);

        assertNotNull(newUserTO);

        // issue SYNCOPE-15
        assertNotNull(newUserTO.getCreationDate());
        assertNotNull(newUserTO.getCreator());
        assertNotNull(newUserTO.getLastChangeDate());
        assertNotNull(newUserTO.getLastModifier());
        assertEquals(newUserTO.getCreationDate(), newUserTO.getLastChangeDate());

        assertFalse(newUserTO.getAttrs().contains(attrWithInvalidSchemaTO));

        // check for changePwdDate
        assertNotNull(newUserTO.getCreationDate());

        // 2. check for virtual attribute value
        newUserTO = userService.read(newUserTO.getId());
        assertNotNull(newUserTO);

        assertNotNull(newUserTO.getVirAttrMap());
        assertNotNull(newUserTO.getVirAttrMap().get("virtualdata").getValues());
        assertFalse(newUserTO.getVirAttrMap().get("virtualdata").getValues().isEmpty());
        assertEquals("virtualvalue", newUserTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // get the new task list
        tasks = taskService.list(TaskType.PROPAGATION);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        long newMaxId = getMaxTaskId(tasks.getResult());

        // default configuration for ws-target-resource2:
        // only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);

        // get last task
        taskTO = taskService.read(newMaxId);

        assertNotNull(taskTO);
        assertEquals(maxTaskExecutions, taskTO.getExecutions().size());

        // 3. verify password
        UserSelfService userSelfService1 = clientFactory.create(
                newUserTO.getUsername(), "password123").getService(UserSelfService.class);
        try {
            UserTO user = userSelfService1.read();
            assertNotNull(user);
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
        AttributeTO userIdAttr = userTO.getAttrMap().get("userId");
        userIdAttr.getValues().clear();
        userIdAttr.getValues().add("a.b@c.com");

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DataIntegrityViolation, e.getType());
        }
    }

    @Test
    public void createWithRequiredValueMissing() {
        UserTO userTO = getSampleTO("a.b@c.it");

        AttributeTO type = userTO.getAttrMap().get("type");
        userTO.getAttrs().remove(type);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.getMemberships().add(membershipTO);

        // 1. create user without type (mandatory by UserSchema)
        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        userTO.getAttrs().add(attributeTO("type", "F"));

        AttributeTO surname = userTO.getAttrMap().get("surname");
        userTO.getAttrs().remove(surname);

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
            userService.delete(0L);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        UserTO userTO = getSampleTO("qqgf.z@nn.com");

        // specify a propagation
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO);

        long id = userTO.getId();

        userTO = deleteUser(id);

        assertNotNull(userTO);
        assertEquals(id, userTO.getId());
        assertTrue(userTO.getAttrs().isEmpty());

        // check for propagation result
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());

        try {
            userService.delete(userTO.getId());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void deleteByUsername() {
        UserTO userTO = getSampleTO("delete.by.username@apache.org");

        // specify a propagation
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO);

        long id = userTO.getId();
        userTO = userService.read(id);
        userTO = deleteUser(userTO.getId());

        assertNotNull(userTO);
        assertEquals(id, userTO.getId());
        assertTrue(userTO.getAttrs().isEmpty());

        // check for propagation result
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());

        try {
            userService.read(userTO.getId());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        PagedResult<UserTO> users = userService.list();
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());

        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }
    }

    @Test
    public void paginatedList() {
        PagedResult<UserTO> users = userService.list(1, 2);
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());
        assertEquals(2, users.getResult().size());

        for (UserTO user : users.getResult()) {
            assertNotNull(user);
        }

        users = userService.list(2, 2);
        assertNotNull(users);
        assertFalse(users.getResult().isEmpty());
        assertEquals(2, users.getResult().size());

        users = userService.list(100, 2);
        assertNotNull(users);
        assertTrue(users.getResult().isEmpty());
    }

    @Test
    public void read() {
        UserTO userTO = userService.read(1L);

        assertNotNull(userTO);
        assertNotNull(userTO.getAttrs());
        assertFalse(userTO.getAttrs().isEmpty());
    }

    @Test
    public void readWithMailAddressAsUserName() {
        UserTO userTO = createUser(getUniqueSampleTO("mail@domain.org"));
        userTO = userService.read(userTO.getId());
        assertNotNull(userTO);
    }

    @Test
    public void updateWithouPassword() {
        UserTO userTO = getUniqueSampleTO("updatewithout@password.com");

        userTO = createUser(userTO);

        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getDerAttrsToRemove().add("cn");

        userTO = updateUser(userMod);

        assertNotNull(userTO);
        assertNotNull(userTO.getDerAttrMap());
        assertFalse(userTO.getDerAttrMap().containsKey("cn"));
    }

    @Test(expected = SyncopeClientException.class)
    public void updateInvalidPassword() {
        UserTO userTO = getSampleTO("updateinvalid@password.com");

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("pass");

        userService.update(userMod.getId(), userMod);
    }

    @Test(expected = SyncopeClientException.class)
    public void updateSamePassword() {
        UserTO userTO = getSampleTO("updatesame@password.com");

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("password123");

        userService.update(userMod.getId(), userMod);
    }

    @Test
    public void update() {
        UserTO userTO = getUniqueSampleTO("g.h@t.com");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        membershipTO.getAttrs().add(attributeTO("subscriptionDate", "2009-08-18T16:33:12.203+0200"));
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);

        assertFalse(userTO.getDerAttrs().isEmpty());
        assertEquals(1, userTO.getMemberships().size());

        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(8L);
        membershipMod.getAttrsToUpdate().add(attributeMod("subscriptionDate", "2010-08-18T16:33:12.203+0200"));

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("new2Password");

        userMod.getAttrsToRemove().add("userId");
        String newUserId = getUUIDString() + "t.w@spre.net";
        userMod.getAttrsToUpdate().add(attributeMod("userId", newUserId));

        userMod.getAttrsToRemove().add("fullname");
        String newFullName = getUUIDString() + "g.h@t.com";
        userMod.getAttrsToUpdate().add(attributeMod("fullname", newFullName));

        userMod.getDerAttrsToAdd().add("cn");
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.getMembershipsToRemove().add(userTO.getMemberships().iterator().next().getId());

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        // issue SYNCOPE-15
        assertNotNull(userTO.getCreationDate());
        assertNotNull(userTO.getCreator());
        assertNotNull(userTO.getLastChangeDate());
        assertNotNull(userTO.getLastModifier());
        assertTrue(userTO.getCreationDate().before(userTO.getLastChangeDate()));

        SyncopeUser passwordTestUser = new SyncopeUser();
        passwordTestUser.setPassword("new2Password", CipherAlgorithm.SHA, 0);
        assertEquals(passwordTestUser.getPassword(), userTO.getPassword());

        assertEquals(1, userTO.getMemberships().size());
        assertEquals(1, userTO.getMemberships().iterator().next().getAttrs().size());
        assertFalse(userTO.getDerAttrs().isEmpty());

        AttributeTO userIdAttr = userTO.getAttrMap().get("userId");
        assertEquals(Collections.singletonList(newUserId), userIdAttr.getValues());

        AttributeTO fullNameAttr = userTO.getAttrMap().get("fullname");
        assertEquals(Collections.singletonList(newFullName), fullNameAttr.getValues());
    }

    @Test
    public void updatePasswordOnly() {
        int beforeTasks = taskService.list(TaskType.PROPAGATION, 1, 1).getTotalCount();
        assertFalse(beforeTasks <= 0);

        UserTO userTO = getUniqueSampleTO("pwdonly@t.com");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        membershipTO.getAttrs().add(attributeTO("subscriptionDate", "2009-08-18T16:33:12.203+0200"));
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword123");

        userTO = updateUser(userMod);

        // check for changePwdDate
        assertNotNull(userTO.getChangePwdDate());

        int afterTasks = taskService.list(TaskType.PROPAGATION, 1, 1).getTotalCount();
        assertFalse(beforeTasks <= 0);

        assertTrue(beforeTasks < afterTasks);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifyTaskRegistration() {
        // get task list
        PagedResult<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        long maxId = getMaxTaskId(tasks.getResult());

        // --------------------------------------
        // Create operation
        // --------------------------------------
        UserTO userTO = getUniqueSampleTO("t@p.mode");

        // add a membership
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.getMemberships().add(membershipTO);

        // 1. create user
        userTO = createUser(userTO);
        assertNotNull(userTO);

        // get the new task list
        tasks = taskService.list(TaskType.PROPAGATION);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        long newMaxId = getMaxTaskId(tasks.getResult());

        // default configuration for ws-target-resource2:
        // only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);

        // --------------------------------------
        // Update operation
        // --------------------------------------
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        userMod.getAttrsToUpdate().add(attributeMod("surname", "surname"));

        userTO = updateUser(userMod);

        assertNotNull(userTO);

        // get the new task list
        tasks = taskService.list(TaskType.PROPAGATION);

        maxId = newMaxId;
        newMaxId = getMaxTaskId(tasks.getResult());

        // default configuration for ws-target-resource2:
        // all update executions have to be registered
        assertTrue(newMaxId > maxId);

        final PropagationTaskTO taskTO = taskService.read(newMaxId);

        assertNotNull(taskTO);
        assertEquals(1, taskTO.getExecutions().size());

        // --------------------------------------
        // Delete operation
        // --------------------------------------
        userService.delete(userTO.getId());

        // get the new task list
        tasks = taskService.list(TaskType.PROPAGATION);

        maxId = newMaxId;
        newMaxId = getMaxTaskId(tasks.getResult());

        // default configuration for ws-target-resource2: no delete executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);
    }

    @Test
    public void createActivate() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers());

        UserTO userTO = getUniqueSampleTO("createActivate@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(11L);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);

        assertNotNull(userTO);
        assertNotNull(userTO.getToken());
        assertNotNull(userTO.getTokenExpireTime());

        assertEquals("created", userTO.getStatus());

        StatusMod statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.ACTIVATE);
        statusMod.setToken(userTO.getToken());
        userTO = userService.status(userTO.getId(), statusMod).readEntity(UserTO.class);

        assertNotNull(userTO);
        assertNull(userTO.getToken());
        assertNull(userTO.getTokenExpireTime());
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void suspendReactivate() {
        UserTO userTO = getUniqueSampleTO("suspendReactivate@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);

        assertNotNull(userTO);
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers()
                ? "active"
                : "created", userTO.getStatus());

        StatusMod statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.SUSPEND);
        userTO = userService.status(userTO.getId(), statusMod).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        userTO = userService.status(userTO.getId(), statusMod).readEntity(UserTO.class);
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
        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers()
                ? "active"
                : "created", userTO.getStatus());
        long userId = userTO.getId();

        // Suspend with effect on syncope, ldap and db => user should be suspended in syncope and all resources
        StatusMod statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.SUSPEND);
        statusMod.setOnSyncope(true);
        statusMod.getResourceNames().add(RESOURCE_NAME_TESTDB);
        statusMod.getResourceNames().add(RESOURCE_NAME_LDAP);
        userTO = userService.status(userId, statusMod).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, userId);
        assertFalse(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_LDAP, AttributableType.USER, userId);
        assertNotNull(connObjectTO);

        // Suspend and reactivate only on ldap => db and syncope should still show suspended
        statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.SUSPEND);
        statusMod.setOnSyncope(false);
        statusMod.getResourceNames().add(RESOURCE_NAME_LDAP);
        userService.status(userId, statusMod);
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        userTO = userService.status(userId, statusMod).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, userId);
        assertFalse(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));

        // Reactivate on syncope and db => syncope and db should show the user as active
        statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        statusMod.setOnSyncope(true);
        statusMod.getResourceNames().add(RESOURCE_NAME_TESTDB);

        userTO = userService.status(userId, statusMod).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, userId);
        assertTrue(getBooleanAttribute(connObjectTO, OperationalAttributes.ENABLE_NAME));
    }

    public void updateMultivalueAttribute() {
        UserTO userTO = getSampleTO("multivalue@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();

        userTO = createUser(userTO);
        assertNotNull(userTO);

        AttributeTO loginDate = userTO.getAttrMap().get("loginDate");
        assertNotNull(loginDate);
        assertEquals(1, loginDate.getValues().size());

        UserMod userMod = new UserMod();

        AttributeMod loginDateMod = new AttributeMod();
        loginDateMod.getValuesToBeAdded().add("2000-01-01");

        userMod.setId(userTO.getId());
        userMod.getAttrsToUpdate().add(loginDateMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        loginDate = userTO.getAttrMap().get("loginDate");
        assertNotNull(loginDate);
        assertEquals(2, loginDate.getValues().size());
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void issue213() {
        UserTO userTO = getUniqueSampleTO("issue213@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals(1, userTO.getResources().size());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String username = jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", String.class,
                userTO.getUsername());

        assertEquals(userTO.getUsername(), username);

        UserMod userMod = new UserMod();

        userMod.setId(userTO.getId());
        userMod.getResourcesToRemove().add(RESOURCE_NAME_TESTDB);

        userTO = updateUser(userMod);
        assertTrue(userTO.getResources().isEmpty());

        jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
    }

    @Test
    public void issue234() {
        UserTO inUserTO = getUniqueSampleTO("issue234@syncope.apache.org");
        inUserTO.getResources().add(RESOURCE_NAME_LDAP);

        UserTO userTO = createUser(inUserTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();

        userMod.setId(userTO.getId());
        userMod.setUsername("1" + userTO.getUsername());

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertEquals("1" + inUserTO.getUsername(), userTO.getUsername());
    }

    @Test
    public void issue270() {
        // 1. create a new user without virtual attributes
        UserTO original = getUniqueSampleTO("issue270@syncope.apache.org");
        // be sure to remove all virtual attributes
        original.getVirAttrs().clear();

        original = createUser(original);

        assertNotNull(original);

        assertTrue(original.getVirAttrs().isEmpty());

        UserTO toBeUpdated = userService.read(original.getId());

        AttributeTO virtual = attributeTO("virtualdata", "virtualvalue");
        toBeUpdated.getVirAttrs().add(virtual);

        // 2. try to update by adding a resource, but no password: must fail
        UserMod userMod = AttributableOperations.diff(toBeUpdated, original);
        assertNotNull(userMod);

        toBeUpdated = updateUser(userMod);
        assertNotNull(toBeUpdated);

        assertFalse(toBeUpdated.getVirAttrs().isEmpty());
        assertNotNull(toBeUpdated.getVirAttrs().get(0));

        assertEquals(virtual.getSchema(), toBeUpdated.getVirAttrs().get(0).getSchema());
    }

    @Test
    public final void issue280() {
        UserTO userTO = getUniqueSampleTO("issue280@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("123password");
        userMod.getResourcesToAdd().add(RESOURCE_NAME_TESTDB);

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        final List<PropagationStatus> propagations = userTO.getPropagationStatusTOs();

        assertNotNull(propagations);
        assertEquals(1, propagations.size());

        final PropagationTaskExecStatus status = propagations.get(0).getStatus();
        final String resource = propagations.get(0).getResource();

        assertNotNull(status);
        assertEquals(RESOURCE_NAME_TESTDB, resource);
        assertTrue(status.isSuccessful());
    }

    @Test
    public void issue281() {
        UserTO userTO = getUniqueSampleTO("issue281@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        final List<PropagationStatus> propagations = userTO.getPropagationStatusTOs();

        assertNotNull(propagations);
        assertEquals(1, propagations.size());

        final PropagationTaskExecStatus status = propagations.get(0).getStatus();
        final String resource = propagations.get(0).getResource();

        assertNotNull(status);
        assertEquals(RESOURCE_NAME_CSV, resource);
        assertFalse(status.isSuccessful());
    }

    @Test
    public void issue288() {
        UserTO userTO = getSampleTO("issue288@syncope.apache.org");
        userTO.getAttrs().add(attributeTO("aLong", "STRING"));

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidValues, e.getType());
        }
    }

    @Test
    public void roleAttrPropagation() {
        UserTO userTO = getUniqueSampleTO("checkRoleAttrPropagation@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();

        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);

        userTO.getMemberships().add(membershipTO);

        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertNotNull(actual.getDerAttrMap().get("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
        assertNotNull(connObjectTO);
        assertEquals("sx-dx", connObjectTO.getAttrMap().get("ROLE").getValues().get(0));
    }

    @Test
    public void membershipAttrPropagation() {
        UserTO userTO = getUniqueSampleTO("checkMembAttrPropagation@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);
        membershipTO.getAttrs().add(attributeTO("mderived_sx", "sx"));
        membershipTO.getAttrs().add(attributeTO("mderived_dx", "dx"));
        membershipTO.getDerAttrs().add(attributeTO("mderToBePropagated", null));
        userTO.getMemberships().add(membershipTO);

        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertNotNull(actual.getDerAttrMap().get("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
        assertNotNull(connObjectTO);
        assertEquals("sx-dx", connObjectTO.getAttrMap().get("MEMBERSHIP").getValues().get(0));
    }

    @Test
    public void noContent() throws IOException {
        SyncopeClient noContentclient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        UserService noContentService = noContentclient.prefer(UserService.class, Preference.RETURN_NO_CONTENT);

        UserTO user = getUniqueSampleTO("nocontent@syncope.apache.org");

        Response response = noContentService.create(user);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));

        user = getObject(response.getLocation(), UserService.class, UserTO.class);
        assertNotNull(user);

        UserMod userMod = new UserMod();
        userMod.setPassword("password321");

        response = noContentService.update(user.getId(), userMod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));

        response = noContentService.delete(user.getId());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));
    }

    @Test
    public void issueSYNCOPE108() {
        UserTO userTO = getUniqueSampleTO("syncope108@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        MembershipTO memb12 = new MembershipTO();
        memb12.setRoleId(12L);

        userTO.getMemberships().add(memb12);

        MembershipTO memb13 = new MembershipTO();
        memb13.setRoleId(13L);

        userTO.getMemberships().add(memb13);

        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertEquals(2, actual.getMemberships().size());
        assertEquals(1, actual.getResources().size());

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
        assertNotNull(connObjectTO);

        // -----------------------------------
        // Remove the first membership: de-provisioning shouldn't happen
        // -----------------------------------
        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());

        userMod.getMembershipsToRemove().add(actual.getMemberships().get(0).getId());

        actual = updateUser(userMod);
        assertNotNull(actual);
        assertEquals(1, actual.getMemberships().size());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the resource assigned directly: de-provisioning shouldn't happen
        // -----------------------------------
        userMod = new UserMod();
        userMod.setId(actual.getId());

        userMod.getResourcesToRemove().add(actual.getResources().iterator().next());

        actual = updateUser(userMod);
        assertNotNull(actual);
        assertEquals(1, actual.getMemberships().size());
        assertFalse(actual.getResources().isEmpty());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the first membership: de-provisioning should happen
        // -----------------------------------
        userMod = new UserMod();
        userMod.setId(actual.getId());

        userMod.getMembershipsToRemove().add(actual.getMemberships().get(0).getId());

        actual = updateUser(userMod);
        assertNotNull(actual);
        assertTrue(actual.getMemberships().isEmpty());
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
            fail("Read should not succeeed");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE111() {
        UserTO userTO = getUniqueSampleTO("syncope111@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        MembershipTO memb12 = new MembershipTO();
        memb12.setRoleId(12L);
        memb12.getAttrs().add(attributeTO("postalAddress", "postalAddress"));
        userTO.getMemberships().add(memb12);

        MembershipTO memb13 = new MembershipTO();
        memb13.setRoleId(13L);
        userTO.getMemberships().add(memb13);

        userTO.getResources().add(RESOURCE_NAME_LDAP);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertEquals(2, actual.getMemberships().size());

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, AttributableType.USER, actual.getId());
        assertNotNull(connObjectTO);

        AttributeTO postalAddress = connObjectTO.getAttrMap().get("postalAddress");
        assertNotNull(postalAddress);
        assertEquals(1, postalAddress.getValues().size());
        assertEquals("postalAddress", postalAddress.getValues().get(0));

        AttributeTO title = connObjectTO.getAttrMap().get("title");
        assertNotNull(title);
        assertEquals(2, title.getValues().size());
        assertTrue(title.getValues().contains("r12") && title.getValues().contains("r13"));

        // -----------------------------------
        // Remove the first membership and check for membership attr propagation and role attr propagation
        // -----------------------------------
        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());

        MembershipTO membershipTO = actual.getMemberships().get(0).getRoleId() == 12L
                ? actual.getMemberships().get(0)
                : actual.getMemberships().get(1);

        userMod.getMembershipsToRemove().add(membershipTO.getId());

        actual = updateUser(userMod);
        assertNotNull(actual);
        assertEquals(1, actual.getMemberships().size());

        connObjectTO = resourceService.getConnectorObject(RESOURCE_NAME_LDAP, AttributableType.USER, actual.getId());
        assertNotNull(connObjectTO);

        postalAddress = connObjectTO.getAttrMap().get("postalAddress");
        assertTrue(postalAddress == null || postalAddress.getValues().isEmpty()
                || StringUtils.isNotBlank(postalAddress.getValues().get(0)));

        title = connObjectTO.getAttrMap().get("title");
        assertNotNull(title);
        assertEquals(1, title.getValues().size());
        assertTrue(title.getValues().contains("r13"));
        // -----------------------------------
    }

    @Test
    public void issueSYNCOPE185() {
        // 1. create user with LDAP resource, succesfully propagated
        UserTO userTO = getSampleTO("syncope185@syncope.apache.org");
        userTO.getVirAttrs().clear();
        userTO.getResources().add(RESOURCE_NAME_LDAP);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_LDAP, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, userTO.getPropagationStatusTOs().get(0).getStatus());

        // 2. delete this user
        userService.delete(userTO.getId());

        // 3. try (and fail) to find this user on the external LDAP resource
        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, AttributableType.USER, userTO.getId());
            fail("This entry should not be present on this resource");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test()
    public void issueSYNCOPE51() {
        ConfigurationTO defaultConfigurationTO = configurationService.read("password.cipher.algorithm");

        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("password.cipher.algorithm");
        configurationTO.setValue("MD5");

        configurationService.update(configurationTO.getKey(), configurationTO);
        ConfigurationTO newConfTO = configurationService.read(configurationTO.getKey());

        assertEquals(configurationTO, newConfTO);

        UserTO userTO = getSampleTO("syncope51@syncope.apache.org");
        userTO.setPassword("password");

        try {
            createUser(userTO);
            fail("Create user should not succeed");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getElements().iterator().next().toString().contains("MD5"));
        }

        configurationService.update(defaultConfigurationTO.getKey(), defaultConfigurationTO);
        ConfigurationTO oldConfTO = configurationService.read(defaultConfigurationTO.getKey());

        assertEquals(defaultConfigurationTO, oldConfTO);
    }

    @Test
    public void issueSYNCOPE267() {
        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserTO userTO = getUniqueSampleTO("syncope267@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_DBVIRATTR, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_DBVIRATTR, AttributableType.USER, userTO.getId());
        assertNotNull(connObjectTO);
        assertEquals("virtualvalue", connObjectTO.getAttrMap().get("USERNAME").getValues().get(0));
        // ----------------------------------

        userTO = userService.read(userTO.getId());

        assertNotNull(userTO);
        assertEquals(1, userTO.getVirAttrs().size());
        assertEquals("virtualvalue", userTO.getVirAttrs().get(0).getValues().get(0));
    }

    @Test
    public void issueSYNCOPE266() {
        UserTO userTO = getUniqueSampleTO("syncope266@apache.org");
        userTO.getResources().clear();

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        // this resource has not a mapping for Password
        userMod.getResourcesToAdd().add(RESOURCE_NAME_UPDATE);

        userTO = updateUser(userMod);
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE279() {
        UserTO userTO = getUniqueSampleTO("syncope279@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_TIMEOUT);
        userTO = createUser(userTO);
        assertEquals(RESOURCE_NAME_TIMEOUT, userTO.getPropagationStatusTOs().get(0).getResource());
        assertNotNull(userTO.getPropagationStatusTOs().get(0).getFailureReason());
        assertEquals(PropagationTaskExecStatus.UNSUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE122() {
        // 1. create user on testdb and testdb2
        UserTO userTO = getUniqueSampleTO("syncope122@apache.org");
        userTO.getResources().clear();

        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        userTO.getResources().add(RESOURCE_NAME_TESTDB2);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB2));

        final String pwdOnSyncope = userTO.getPassword();

        ConnObjectTO userOnDb = resourceService.getConnectorObject(
                RESOURCE_NAME_TESTDB, AttributableType.USER, userTO.getId());
        final AttributeTO pwdOnTestDbAttr = userOnDb.getAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDbAttr);
        assertNotNull(pwdOnTestDbAttr.getValues());
        assertFalse(pwdOnTestDbAttr.getValues().isEmpty());
        final String pwdOnTestDb = pwdOnTestDbAttr.getValues().iterator().next();

        ConnObjectTO userOnDb2 = resourceService.getConnectorObject(
                RESOURCE_NAME_TESTDB2, AttributableType.USER, userTO.getId());
        final AttributeTO pwdOnTestDb2Attr = userOnDb2.getAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDb2Attr);
        assertNotNull(pwdOnTestDb2Attr.getValues());
        assertFalse(pwdOnTestDb2Attr.getValues().isEmpty());
        final String pwdOnTestDb2 = pwdOnTestDb2Attr.getValues().iterator().next();

        // 2. request to change password only on testdb (no Syncope, no testdb2)
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword(getUUIDString());
        StatusMod pwdPropRequest = new StatusMod();
        pwdPropRequest.setOnSyncope(false);
        pwdPropRequest.getResourceNames().add(RESOURCE_NAME_TESTDB);
        userMod.setPwdPropRequest(pwdPropRequest);

        userTO = updateUser(userMod);

        // 3a. Chech that only a single propagation took place
        assertNotNull(userTO.getPropagationStatusTOs());
        assertEquals(1, userTO.getPropagationStatusTOs().size());
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getPropagationStatusTOs().iterator().next().getResource());

        // 3b. verify that password hasn't changed on Syncope
        assertEquals(pwdOnSyncope, userTO.getPassword());

        // 3c. verify that password *has* changed on testdb
        userOnDb = resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, userTO.getId());
        final AttributeTO pwdOnTestDbAttrAfter = userOnDb.getAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDbAttrAfter);
        assertNotNull(pwdOnTestDbAttrAfter.getValues());
        assertFalse(pwdOnTestDbAttrAfter.getValues().isEmpty());
        assertNotEquals(pwdOnTestDb, pwdOnTestDbAttrAfter.getValues().iterator().next());

        // 3d. verify that password hasn't changed on testdb2
        userOnDb2 = resourceService.getConnectorObject(RESOURCE_NAME_TESTDB2, AttributableType.USER, userTO.getId());
        final AttributeTO pwdOnTestDb2AttrAfter = userOnDb2.getAttrMap().get(OperationalAttributes.PASSWORD_NAME);
        assertNotNull(pwdOnTestDb2AttrAfter);
        assertNotNull(pwdOnTestDb2AttrAfter.getValues());
        assertFalse(pwdOnTestDb2AttrAfter.getValues().isEmpty());
        assertEquals(pwdOnTestDb2, pwdOnTestDb2AttrAfter.getValues().iterator().next());
    }

    @Test
    public void isseSYNCOPE136AES() {
        // 1. read configured cipher algorithm in order to be able to restore it at the end of test
        ConfigurationTO pwdCipherAlgo = configurationService.read("password.cipher.algorithm");
        final String origpwdCipherAlgo = pwdCipherAlgo.getValue();

        // 2. set AES password cipher algorithm
        pwdCipherAlgo.setValue("AES");
        configurationService.update(pwdCipherAlgo.getKey(), pwdCipherAlgo);

        // 3. create user with no resources
        UserTO userTO = getUniqueSampleTO("syncope136_AES@apache.org");
        userTO.getResources().clear();

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 4. update user, assign a propagation primary resource but don't provide any password
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getResourcesToAdd().add(RESOURCE_NAME_WS1);

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        // 5. verify that propagation was successful
        List<PropagationStatus> props = userTO.getPropagationStatusTOs();
        assertNotNull(props);
        assertEquals(1, props.size());
        PropagationStatus prop = props.iterator().next();
        assertNotNull(prop);
        assertEquals(RESOURCE_NAME_WS1, prop.getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, prop.getStatus());

        // 6. restore initial cipher algorithm
        pwdCipherAlgo.setValue(origpwdCipherAlgo);
        configurationService.update(pwdCipherAlgo.getKey(), pwdCipherAlgo);
    }

    @Test
    public void isseSYNCOPE136Random() {
        // 1. create user with no resources
        UserTO userTO = getUniqueSampleTO("syncope136_Random@apache.org");
        userTO.getResources().clear();

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 2. update user, assign a propagation primary resource but don't provide any password
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getResourcesToAdd().add(RESOURCE_NAME_LDAP);

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        // 3. verify that propagation was successful
        List<PropagationStatus> props = userTO.getPropagationStatusTOs();
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

        AttributeTO csvuserid = new AttributeTO();
        csvuserid.setSchema("csvuserid");
        userTO.getDerAttrs().add(csvuserid);

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
        assertNull(connObjectTO.getAttrMap().get("email"));
    }

    @Test
    public void issueSYNCOPE265() {
        for (long i = 1; i <= 5; i++) {
            UserMod userMod = new UserMod();
            userMod.setId(i);

            AttributeMod attributeMod = new AttributeMod();
            attributeMod.setSchema("type");
            attributeMod.getValuesToBeAdded().add("a type");

            userMod.getAttrsToRemove().add("type");
            userMod.getAttrsToUpdate().add(attributeMod);

            UserTO userTO = updateUser(userMod);
            assertEquals("a type", userTO.getAttrMap().get("type").getValues().get(0));
        }
    }

    @Test
    public void bulkActions() {
        final BulkAction bulkAction = new BulkAction();

        for (int i = 0; i < 10; i++) {
            UserTO userTO = getUniqueSampleTO("bulk_" + i + "@apache.org");
            bulkAction.getTargets().add(String.valueOf(createUser(userTO).getId()));
        }

        // check for a fail
        bulkAction.getTargets().add(String.valueOf(Long.MAX_VALUE));

        assertEquals(11, bulkAction.getTargets().size());

        bulkAction.setOperation(BulkAction.Type.SUSPEND);
        BulkActionResult res = userService.bulk(bulkAction);
        assertEquals(10, res.getResultByStatus(Status.SUCCESS).size());
        assertEquals(1, res.getResultByStatus(Status.FAILURE).size());
        assertEquals("suspended", userService.read(
                Long.parseLong(res.getResultByStatus(Status.SUCCESS).get(3).toString())).getStatus());

        bulkAction.setOperation(BulkAction.Type.REACTIVATE);
        res = userService.bulk(bulkAction);
        assertEquals(10, res.getResultByStatus(Status.SUCCESS).size());
        assertEquals(1, res.getResultByStatus(Status.FAILURE).size());
        assertEquals("active", userService.read(
                Long.parseLong(res.getResultByStatus(Status.SUCCESS).get(3).toString())).getStatus());

        bulkAction.setOperation(BulkAction.Type.DELETE);
        res = userService.bulk(bulkAction);
        assertEquals(10, res.getResultByStatus(Status.SUCCESS).size());
        assertEquals(1, res.getResultByStatus(Status.FAILURE).size());
    }

    @Test
    public void issueSYNCOPE354() {
        // change resource-ldap role mapping for including uniqueMember (need for assertions below)
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        for (MappingItemTO item : ldap.getRmapping().getItems()) {
            if ("description".equals(item.getExtAttrName())) {
                item.setExtAttrName("uniqueMember");
            }
        }
        resourceService.update(ldap.getName(), ldap);

        // 1. create role with LDAP resource
        RoleTO roleTO = new RoleTO();
        roleTO.setName("SYNCOPE354-" + getUUIDString());
        roleTO.setParent(8L);
        roleTO.getResources().add(RESOURCE_NAME_LDAP);

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);

        // 2. create user with LDAP resource and membership of the above role
        UserTO userTO = getUniqueSampleTO("syncope354@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(roleTO.getId());
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));

        // 3. read role on resource, check that user DN is included in uniqueMember
        ConnObjectTO connObj = resourceService.getConnectorObject(
                RESOURCE_NAME_LDAP, AttributableType.ROLE, roleTO.getId());
        assertNotNull(connObj);
        assertTrue(connObj.getAttrMap().get("uniqueMember").getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 4. remove membership
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getMembershipsToRemove().add(userTO.getMemberships().iterator().next().getId());

        userTO = updateUser(userMod);
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));

        // 5. read role on resource, check that user DN was removed from uniqueMember
        connObj = resourceService.getConnectorObject(RESOURCE_NAME_LDAP, AttributableType.ROLE, roleTO.getId());
        assertNotNull(connObj);
        assertFalse(connObj.getAttrMap().get("uniqueMember").getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 6. restore original resource-ldap role mapping
        for (MappingItemTO item : ldap.getRmapping().getItems()) {
            if ("uniqueMember".equals(item.getExtAttrName())) {
                item.setExtAttrName("description");
            }
        }
        resourceService.update(ldap.getName(), ldap);
    }

    @Test
    public void issueSYNCOPE357() {
        // 1. create role with LDAP resource
        RoleTO roleTO = new RoleTO();
        roleTO.setName("SYNCOPE357-" + getUUIDString());
        roleTO.setParent(8L);
        roleTO.getResources().add(RESOURCE_NAME_LDAP);

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);

        // 2. create user with membership of the above role
        UserTO userTO = getUniqueSampleTO("syncope357@syncope.apache.org");
        userTO.getAttrs().add(attributeTO("obscure", "valueToBeObscured"));
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(roleTO.getId());
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));
        assertNotNull(userTO.getAttrMap().get("obscure"));

        // 3. read user on resource
        ConnObjectTO connObj = resourceService.getConnectorObject(
                RESOURCE_NAME_LDAP, AttributableType.USER, userTO.getId());
        assertNotNull(connObj);
        AttributeTO registeredAddress = connObj.getAttrMap().get("registeredAddress");
        assertNotNull(registeredAddress);
        assertEquals(userTO.getAttrMap().get("obscure").getValues(), registeredAddress.getValues());

        // 4. remove role
        roleService.delete(roleTO.getId());

        // 5. try to read user on resource: fail
        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, AttributableType.USER, userTO.getId());
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
        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 2. assign resource without specifying a new pwd and check propagation failure
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getResourcesToAdd().add(RESOURCE_NAME_TESTDB);
        userTO = updateUser(userMod);
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertFalse(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
        assertNotNull(userTO.getPropagationStatusTOs().get(0).getFailureReason());

        // 3. request to change password only on testdb
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword(getUUIDString());
        StatusMod pwdPropRequest = new StatusMod();
        pwdPropRequest.getResourceNames().add(RESOURCE_NAME_TESTDB);
        userMod.setPwdPropRequest(pwdPropRequest);

        userTO = updateUser(userMod);
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
    }

    @Test
    public void issueSYNCOPE402() {
        // 1. create an user with strict mandatory attributes only
        UserTO userTO = new UserTO();
        String userId = getUUIDString() + "syncope402@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password");

        userTO.getAttrs().add(attributeTO("userId", userId));
        userTO.getAttrs().add(attributeTO("fullname", userId));
        userTO.getAttrs().add(attributeTO("surname", userId));

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        //2. update assigning a resource NOT forcing mandatory constraints
        // AND primary: must fail with PropagationException
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");

        userMod.getResourcesToAdd().add(RESOURCE_NAME_WS1);
        userMod.getResourcesToAdd().add(RESOURCE_NAME_TESTDB);
        userTO = updateUser(userMod);

        List<PropagationStatus> propagationStatuses = userTO.getPropagationStatusTOs();
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
        assertEquals(PropagationTaskExecStatus.UNSUBMITTED, ws1PropagationStatus.getStatus());
    }

    @Test
    public void unlink() {
        UserTO userTO = getUniqueSampleTO("unlink@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId()));

        assertNotNull(userService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.UNLINK,
                CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId()));
    }

    @Test
    public void link() {
        UserTO userTO = getUniqueSampleTO("link@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        final ResourceAssociationMod associationMod = new ResourceAssociationMod();
        associationMod.getTargetResources().addAll(CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class));

        assertNotNull(userService.bulkAssociation(
                actual.getId(), ResourceAssociationActionType.LINK, associationMod).readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
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
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId()));

        assertNotNull(userService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.UNASSIGN,
                CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
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
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        final ResourceAssociationMod associationMod = new ResourceAssociationMod();
        associationMod.getTargetResources().addAll(CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class));
        associationMod.setChangePwd(true);
        associationMod.setPassword("password");

        assertNotNull(userService.bulkAssociation(actual.getId(), ResourceAssociationActionType.ASSIGN, associationMod)
                .readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId()));
    }

    @Test
    public void deprovision() {
        UserTO userTO = getUniqueSampleTO("deprovision@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));
        userTO.getResources().add(RESOURCE_NAME_CSV);

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId()));

        assertNotNull(userService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void provision() {
        UserTO userTO = getUniqueSampleTO("provision@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        final ResourceAssociationMod associationMod = new ResourceAssociationMod();
        associationMod.getTargetResources().addAll(CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class));
        associationMod.setChangePwd(true);
        associationMod.setPassword("password");

        assertNotNull(userService.bulkAssociation(actual.getId(), ResourceAssociationActionType.PROVISION,
                associationMod)
                .readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId()));
    }

    @Test
    public void deprovisionUnlinked() {
        UserTO userTO = getUniqueSampleTO("provision@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerAttrs().clear();
        userTO.getVirAttrs().clear();
        userTO.getDerAttrs().add(attributeTO("csvuserid", null));

        UserTO actual = createUser(userTO);
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        final ResourceAssociationMod associationMod = new ResourceAssociationMod();
        associationMod.getTargetResources().addAll(CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class));
        associationMod.setChangePwd(true);
        associationMod.setPassword("password");

        assertNotNull(userService.bulkAssociation(actual.getId(), ResourceAssociationActionType.PROVISION,
                associationMod)
                .readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId()));

        assertNotNull(userService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_CSV, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_CSV, AttributableType.USER, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void issueSYNCOPE420() {
        UserTO userTO = getUniqueSampleTO("syncope420@syncope.apache.org");
        userTO.getAttrs().add(attributeTO("makeItDouble", "3"));

        userTO = createUser(userTO);
        assertEquals("6", userTO.getAttrMap().get("makeItDouble").getValues().get(0));

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.getAttrsToRemove().add("makeItDouble");
        userMod.getAttrsToUpdate().add(attributeMod("makeItDouble", "7"));

        userTO = updateUser(userMod);
        assertEquals("14", userTO.getAttrMap().get("makeItDouble").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE426() {
        UserTO userTO = getUniqueSampleTO("syncope426@syncope.apache.org");
        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setPassword("anotherPassword123");
        userTO = userService.update(userTO.getId(), userMod).readEntity(UserTO.class);
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE435() {
        // 1. try to create user without password - fail
        UserTO userTO = getUniqueSampleTO("syncope435@syncope.apache.org");
        userTO.setPassword(null);

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSyncopeUser, e.getType());
        }

        userTO.setPassword("password123");
        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 2. try to update user by subscribing a resource - works but propagation is not even attempted
        UserMod userMod = new UserMod();
        userMod.getResourcesToAdd().add(RESOURCE_NAME_WS1);

        userTO = userService.update(userTO.getId(), userMod).readEntity(UserTO.class);
        assertEquals(Collections.singleton(RESOURCE_NAME_WS1), userTO.getResources());
        assertFalse(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getFailureReason().
                startsWith("Not attempted because there are mandatory attributes without value(s): [__PASSWORD__]"));
    }

    @Test
    public void ifMatch() {
        UserTO userTO = userService.create(getUniqueSampleTO("ifmatch@syncope.apache.org")).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertNotNull(userTO.getId());

        EntityTag etag = adminClient.getLatestEntityTag(userService);
        assertNotNull(etag);
        assertTrue(StringUtils.isNotBlank(etag.getValue()));

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setUsername(userTO.getUsername() + "XX");
        userTO = userService.update(userMod.getId(), userMod).readEntity(UserTO.class);
        assertTrue(userTO.getUsername().endsWith("XX"));
        EntityTag etag1 = adminClient.getLatestEntityTag(userService);
        assertFalse(etag.getValue().equals(etag1.getValue()));

        UserService ifMatchService = adminClient.ifMatch(UserService.class, etag);
        userMod.setUsername(userTO.getUsername() + "YY");
        try {
            ifMatchService.update(userMod.getId(), userMod);
            fail();
        } catch (WebServiceException e) {
            assertTrue(e.getMessage().endsWith(Response.Status.PRECONDITION_FAILED.name()));
        }

        userTO = userService.read(userTO.getId());
        assertTrue(userTO.getUsername().endsWith("XX"));
    }

    @Test
    public void issueSYNCOPE454() throws NamingException {
        // 1. create user with LDAP resource (with 'Generate password if missing' enabled)
        UserTO userTO = getUniqueSampleTO("syncope454@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 2. read resource configuration for LDAP binding
        ConnObjectTO connObject =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, AttributableType.USER, userTO.getId());

        // 3. try (and succeed) to perform simple LDAP binding with provided password ('password123')
        assertNotNull(getLdapRemoteObject(
                connObject.getAttrMap().get(Name.NAME).getValues().get(0),
                "password123",
                connObject.getAttrMap().get(Name.NAME).getValues().get(0)));

        // 4. update user without any password change request
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPwdPropRequest(new StatusMod());
        userMod.getAttrsToUpdate().add(attributeMod("surname", "surname2"));

        userService.update(userTO.getId(), userMod);

        // 5. try (and succeed again) to perform simple LDAP binding: password has not changed
        assertNotNull(getLdapRemoteObject(
                connObject.getAttrMap().get(Name.NAME).getValues().get(0),
                "password123",
                connObject.getAttrMap().get(Name.NAME).getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE493() {
        // 1.  create user and check that firstname is not propagated on resource with mapping for firstname set to NONE
        UserTO userTO = getUniqueSampleTO("issueSYNCOPE493@test.org");
        userTO.getResources().add(RESOURCE_NAME_WS1);
        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals(1, userTO.getPropagationStatusTOs().size());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());

        final ConnObjectTO actual = resourceService.getConnectorObject(RESOURCE_NAME_WS1, AttributableType.USER, userTO.
                getId());
        assertNotNull(actual);
        // check if mapping attribute with purpose NONE really hasn't been propagated
        assertNull(actual.getAttrMap().get("NAME"));

        // 2.  update resource ws-target-resource-1
        ResourceTO ws1 = resourceService.read(RESOURCE_NAME_WS1);
        assertNotNull(ws1);

        MappingTO ws1NewUMapping = ws1.getUmapping();
        // change purpose from NONE to BOTH
        for (MappingItemTO itemTO : ws1NewUMapping.getItems()) {
            if ("firstname".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.BOTH);
            }
        }

        ws1.setUmapping(ws1NewUMapping);
        ws1.setRmapping(ws1.getRmapping());

        resourceService.update(RESOURCE_NAME_WS1, ws1);
        ResourceTO newWs1 = resourceService.read(ws1.getName());
        assertNotNull(newWs1);

        // check for existence
        Collection<MappingItemTO> mapItems = newWs1.getUmapping().getItems();
        assertNotNull(mapItems);
        assertEquals(7, mapItems.size());

        // 3.  update user and check firstname propagation        
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPwdPropRequest(new StatusMod());
        userMod.getAttrsToUpdate().add(attributeMod("firstname", "firstnameNew"));

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertEquals(1, userTO.getPropagationStatusTOs().size());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());

        final ConnObjectTO newUser = resourceService.getConnectorObject(RESOURCE_NAME_WS1, AttributableType.USER,
                userTO.getId());

        assertNotNull(newUser.getAttrMap().get("NAME"));
        assertEquals("firstnameNew", newUser.getAttrMap().get("NAME").getValues().get(0));

        // 4.  restore resource ws-target-resource-1 mapping
        ws1NewUMapping = newWs1.getUmapping();
        // restore purpose from BOTH to NONE
        for (MappingItemTO itemTO : ws1NewUMapping.getItems()) {
            if ("firstname".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.NONE);
            }
        }

        newWs1.setUmapping(ws1NewUMapping);
        newWs1.setRmapping(newWs1.getRmapping());

        resourceService.update(RESOURCE_NAME_WS1, newWs1);
    }
}
