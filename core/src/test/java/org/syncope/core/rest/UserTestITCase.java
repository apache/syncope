/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.SyncopeUserCond;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.ResourceCond;
import org.syncope.client.to.PasswordPolicyTO;
import org.syncope.client.to.PolicyTO;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowFormPropertyTO;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.client.util.AttributableOperations;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.types.CipherAlgorithm;
import org.syncope.types.PropagationTaskExecStatus;
import org.syncope.types.SyncopeClientExceptionType;

public class UserTestITCase extends AbstractTest {

    public static UserTO getSampleTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername(email);

        AttributeTO fullnameTO = new AttributeTO();
        fullnameTO.setSchema("fullname");
        fullnameTO.addValue(email);
        userTO.addAttribute(fullnameTO);

        AttributeTO firstnameTO = new AttributeTO();
        firstnameTO.setSchema("firstname");
        firstnameTO.addValue(email);
        userTO.addAttribute(firstnameTO);

        AttributeTO surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("Surname");
        userTO.addAttribute(surnameTO);

        AttributeTO typeTO = new AttributeTO();
        typeTO.setSchema("type");
        typeTO.addValue("a type");
        userTO.addAttribute(typeTO);

        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue(email);
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue(email);
        userTO.addAttribute(emailTO);

        AttributeTO loginDateTO = new AttributeTO();
        loginDateTO.setSchema("loginDate");
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        loginDateTO.addValue(sdf.format(new Date()));
        userTO.addAttribute(loginDateTO);

        // add a derived attribute
        AttributeTO cnTO = new AttributeTO();
        cnTO.setSchema("cn");
        userTO.addDerivedAttribute(cnTO);

        // add a virtual attribute
        AttributeTO virtualdata = new AttributeTO();
        virtualdata.setSchema("virtualdata");
        virtualdata.addValue("virtualvalue");
        userTO.addVirtualAttribute(virtualdata);

        return userTO;
    }

    @Test
    public final void createUserWithNoPropagation() {
        // get task list
        List<PropagationTaskTO> tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        // get max task id
        long maxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > maxId) {
                maxId = task.getId();
            }
        }

        // create a new user
        UserTO userTO = new UserTO();
        userTO.setUsername("xxx@xxx.xxx");

        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("firstname");
        attributeTO.addValue("xxx");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("surname");
        attributeTO.addValue("xxx");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("xxx@xxx.xxx");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("fullname");
        attributeTO.addValue("xxx");
        userTO.addAttribute(attributeTO);

        userTO.setPassword("password123");
        userTO.addResource("ws-target-resource-nopropagation");

        restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);

        // get the new task list
        tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        // get max task id
        long newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }

        assertTrue(newMaxId > maxId);

        // get last task
        PropagationTaskTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", PropagationTaskTO.class,
                newMaxId);

        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    /* This test has been introduced to verify and solve the following issue:
     * http://code.google.com/p/syncope/issues/detail?id=172.
     * Creations of a new user without having a global password policy stored
     * into the local repository used to fail with a null pointer exception.
     * This bug has been fixed introducing a simple control.
     */
    public final void issue172() {
        PolicyTO policyTO = restTemplate.getForObject(
                BASE_URL + "policy/read/{id}", PasswordPolicyTO.class, 2L);

        assertNotNull(policyTO);

        restTemplate.delete(BASE_URL + "policy/delete/{id}", 2L);

        UserTO userTO = new UserTO();
        userTO.setUsername("issue172@syncope-idm.org");
        userTO.setPassword("password");

        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("firstname");
        attributeTO.addValue("issue172");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("surname");
        attributeTO.addValue("issue172");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("issue172@syncope-idm.org");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("fullname");
        attributeTO.addValue("issue172");
        userTO.addAttribute(attributeTO);

        restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);

        policyTO = restTemplate.postForObject(
                BASE_URL + "policy/password/create",
                policyTO, PasswordPolicyTO.class);

        assertNotNull(policyTO);
    }

    @Test
    public final void issue186() {
        // 1. create an user with strict mandatory attributes only
        UserTO userTO = new UserTO();
        userTO.setUsername("issue186@syncope-idm.org");
        userTO.setPassword("password");

        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("issue186@syncope-idm.org");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("fullname");
        attributeTO.addValue("issue186");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("surname");
        attributeTO.addValue("issue186");
        userTO.addAttribute(attributeTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assignign a resource forcing mandatory constraints: must
        // fail with RequiredValuesMissing
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.addResourceToBeAdded("ws-target-resource-2");

        SyncopeClientException sce = null;
        try {
            userTO = restTemplate.postForObject(
                    BASE_URL + "user/update", userMod, UserTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            sce = scce.getException(
                    SyncopeClientExceptionType.RequiredValuesMissing);
        }
        assertNotNull(sce);

        // 3. update assignign a resource NOT forcing mandatory constraints
        // AND primary: must fail with PropagationException
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.addResourceToBeAdded("ws-target-resource-1");

        sce = null;
        try {
            userTO = restTemplate.postForObject(BASE_URL + "user/update",
                    userMod, UserTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            sce = scce.getException(SyncopeClientExceptionType.Propagation);
        }
        assertNotNull(sce);

        // 4. update assignign a resource NOT forcing mandatory constraints
        // BUT not primary: must succeed
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.addResourceToBeAdded("resource-db");

        sce = null;
        try {
            userTO = restTemplate.postForObject(
                    BASE_URL + "user/update", userMod, UserTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            sce = scce.getException(SyncopeClientExceptionType.Propagation);
        }
        assertNull(sce);
    }

    @Test
    public final void issue147() {
        // 1. create an user wihtout role nor resources
        UserTO userTO = getSampleTO("issue147@syncope-idm.org");

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. try to update by adding a resource, but no password: must fail
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.addResourceToBeAdded("ws-target-resource-2");

        SyncopeClientException sce = null;
        try {
            userTO = restTemplate.postForObject(
                    BASE_URL + "user/update", userMod, UserTO.class);
        } catch (SyncopeClientCompositeErrorException scce) {
            sce = scce.getException(
                    SyncopeClientExceptionType.RequiredValuesMissing);
        }
        assertNotNull(sce);

        // 3. provide password: now update must work
        userMod.setPassword("newPassword");
        userTO = restTemplate.postForObject(
                BASE_URL + "user/update", userMod, UserTO.class);
        assertNotNull(userTO);
        assertEquals(1, userTO.getResources().size());
    }

    @Test
    public final void createUserWithDbPropagation() {
        UserTO userTO = new UserTO();
        userTO.setPassword("password");
        userTO.setUsername("yyy@yyy.yyy");

        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("firstname");
        attributeTO.addValue("yyy");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("surname");
        attributeTO.addValue("yyy");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("yyy@yyy.yyy");
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("fullname");
        attributeTO.addValue("yyy");
        userTO.addAttribute(attributeTO);

        userTO.addResource("resource-testdb");

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);
        assertEquals(1, userTO.getPropagationStatusMap().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS,
                userTO.getPropagationStatusMap().get("resource-testdb"));
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createWithInvalidPassword() {
        UserTO userTO = getSampleTO("invalidpasswd@syncope-idm.org");
        userTO.setPassword("pass");

        restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createWithInvalidUsername() {
        UserTO userTO = getSampleTO("invalidusername@syncope-idm.org");
        userTO.setUsername("us");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);

        userTO.addMembership(membershipTO);

        restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createWithInvalidPasswordByRes() {
        UserTO userTO = getSampleTO("invalidPwdByRes@passwd.com");

        // configured to be minLength=16
        userTO.setPassword("password1");

        userTO.setResources(
                Collections.singleton("ws-target-resource-nopropagation"));

        restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createWithInvalidPasswordByRole() {
        UserTO userTO = getSampleTO("invalidPwdByRole@passwd.com");

        // configured to be minLength=16
        userTO.setPassword("password1");

        final MembershipTO membership = new MembershipTO();
        membership.setRoleId(8L);

        userTO.addMembership(membership);

        restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void createWithException() {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("userId@nowhere.org");

        UserTO newUserTO = new UserTO();
        newUserTO.addAttribute(attributeTO);

        restTemplate.postForObject(BASE_URL + "user/create",
                newUserTO, UserTO.class);
    }

    @Test
    public final void create() {
        // get task list
        List<PropagationTaskTO> tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        // get max task id
        long maxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > maxId) {
                maxId = task.getId();
            }
        }
        PropagationTaskTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", PropagationTaskTO.class,
                maxId);
        assertNotNull(taskTO);
        int maxTaskExecutions = taskTO.getExecutions().size();

        UserTO userTO = getSampleTO("a.b@c.com");

        // add a membership
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        // add an attribute with no values: must be ignored
        AttributeTO nullValueAttributeTO = new AttributeTO();
        nullValueAttributeTO.setSchema("subscriptionDate");
        nullValueAttributeTO.setValues(null);
        membershipTO.addAttribute(nullValueAttributeTO);

        // add an attribute with a non-existing schema: must be ignored
        AttributeTO attrWithInvalidSchemaTO = new AttributeTO();
        attrWithInvalidSchemaTO.setSchema("invalid schema");
        attrWithInvalidSchemaTO.addValue("a value");
        userTO.addAttribute(attrWithInvalidSchemaTO);

        // add an attribute with null value: must be ignored
        nullValueAttributeTO = new AttributeTO();
        nullValueAttributeTO.setSchema("activationDate");
        nullValueAttributeTO.addValue(null);
        userTO.addAttribute(nullValueAttributeTO);

        // 1. create user
        UserTO newUserTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);

        assertNotNull(newUserTO);
        assertFalse(newUserTO.getAttributes().contains(
                attrWithInvalidSchemaTO));

        // check for changePwdDate
        assertNotNull(newUserTO.getCreationDate());

        // 2. check for virtual attribute value
        newUserTO = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json",
                UserTO.class, newUserTO.getId());
        assertNotNull(newUserTO);

        assertNotNull(newUserTO.getVirtualAttributeMap());
        assertNotNull(newUserTO.getVirtualAttributeMap().get("virtualdata").
                getValues());
        assertFalse(newUserTO.getVirtualAttributeMap().get("virtualdata").
                getValues().isEmpty());
        assertEquals("virtualvalue",
                newUserTO.getVirtualAttributeMap().get("virtualdata").
                getValues().get(0));

        // get the new task list
        tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        // get max task id
        long newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }

        // default configuration for ws-target-resource2:
        //             only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);

        // get last task
        taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", PropagationTaskTO.class,
                newMaxId);

        assertNotNull(taskTO);
        assertEquals(maxTaskExecutions, taskTO.getExecutions().size());

        // 3. verify password
        Boolean verify = restTemplate.getForObject(
                BASE_URL + "user/verifyPassword/{userId}?password=password123",
                Boolean.class, newUserTO.getId());
        assertTrue(verify);
        verify = restTemplate.getForObject(
                BASE_URL + "user/verifyPassword/{userId}?password=passwordXX",
                Boolean.class, newUserTO.getId());
        assertFalse(verify);

        // 4. try (and fail) to create another user with same (unique) values
        userTO = getSampleTO("pippo@c.com");
        for (AttributeTO attr : userTO.getAttributes()) {
            if ("userId".equals(attr.getSchema())) {
                attr.getValues().clear();
                attr.addValue("a.b@c.com");
            }
        }

        SyncopeClientException sce = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/create",
                    userTO, UserTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            sce = e.getException(
                    SyncopeClientExceptionType.DuplicateUniqueValue);
        }
        assertNotNull(sce);
    }

    @Test
    public final void createWithRequiredValueMissing() {
        UserTO userTO = getSampleTO("a.b@c.it");

        AttributeTO type = null;
        for (AttributeTO attr : userTO.getAttributes()) {
            if ("type".equals(attr.getSchema())) {
                type = attr;
            }
        }
        assertNotNull(type);

        userTO.removeAttribute(type);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        SyncopeClientCompositeErrorException ex = null;
        try {
            // 1. create user without type (mandatory by UserSchema)
            restTemplate.postForObject(BASE_URL + "user/create",
                    userTO, UserTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNotNull(ex.getException(
                SyncopeClientExceptionType.RequiredValuesMissing));

        AttributeTO fType = new AttributeTO();
        fType.setSchema("type");
        fType.addValue("F");
        userTO.addAttribute(fType);

        AttributeTO surname = null;
        for (AttributeTO attributeTO : userTO.getAttributes()) {
            if ("surname".equals(attributeTO.getSchema())) {
                surname = attributeTO;
            }
        }
        userTO.removeAttribute(surname);

        // 2. create user without surname (mandatory when type == 'F')
        ex = null;
        try {
            restTemplate.postForObject(BASE_URL + "user/create",
                    userTO, UserTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNotNull(ex.getException(
                SyncopeClientExceptionType.RequiredValuesMissing));
    }

    @Test
    public final void createWithReject() {
        UserTO userTO = getSampleTO("createWithReject@syncope-idm.org");

        // User with role 9 are defined in workflow as subject to approval
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(9L);
        userTO.addMembership(membershipTO);

        // 1. create user with role 9
        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(9, userTO.getMemberships().get(0).getRoleId());
        assertEquals("createApproval", userTO.getStatus());

        // 2. request if there is any pending task for user just created
        WorkflowFormTO form = restTemplate.getForObject(
                BASE_URL + "user/workflow/form/{userId}",
                WorkflowFormTO.class, userTO.getId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNull(form.getOwner());

        // 3. claim task from user1, not in role 7 (designated for 
        // approval in workflow definition): fail
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                "user1", "password"));

        SyncopeClientException sce = null;
        try {
            restTemplate.getForObject(
                    BASE_URL + "user/workflow/form/claim/{taskId}",
                    WorkflowFormTO.class, form.getTaskId());
        } catch (SyncopeClientCompositeErrorException scce) {
            sce = scce.getException(SyncopeClientExceptionType.Workflow);
        }
        assertNotNull(sce);

        // 4. claim task from user4, in to role 7
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                "user4", "password"));

        form = restTemplate.getForObject(
                BASE_URL + "user/workflow/form/claim/{taskId}",
                WorkflowFormTO.class, form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getOwner());

        // 5. reject user
        Map<String, WorkflowFormPropertyTO> props = form.getPropertiesAsMap();
        props.get("approve").setValue(Boolean.FALSE.toString());
        props.get("rejectReason").setValue("I don't like him.");
        form.setProperties(props.values());
        userTO = restTemplate.postForObject(
                BASE_URL + "user/workflow/form/submit",
                form, UserTO.class);
        assertNotNull(userTO);
        assertEquals("rejected", userTO.getStatus());

        // reset admin credentials for restTemplate
        super.setupRestTemplate();
    }

    @Test
    public final void createWithApproval() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        UserTO userTO = getSampleTO("createWithApproval@syncope-idm.org");
        userTO.addResource("resource-testdb");

        // User with role 9 are defined in workflow as subject to approval
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(9L);
        userTO.addMembership(membershipTO);

        // 1. create user with role 9 (and verify that no propagation occurred)
        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(9, userTO.getMemberships().get(0).getRoleId());
        assertEquals("createApproval", userTO.getStatus());
        assertEquals(Collections.singleton("resource-testdb"),
                userTO.getResources());
        assertTrue(userTO.getPropagationStatusMap().isEmpty());
        Exception exception = null;
        try {
            jdbcTemplate.queryForInt(
                    "SELECT id FROM test WHERE id=?", userTO.getId());
        } catch (EmptyResultDataAccessException e) {
            exception = e;
        }
        assertNotNull(exception);

        // 2. request if there is any pending task for user just created
        WorkflowFormTO form = restTemplate.getForObject(
                BASE_URL + "user/workflow/form/{userId}",
                WorkflowFormTO.class, userTO.getId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNull(form.getOwner());

        // 4. claim task (from admin)
        form = restTemplate.getForObject(
                BASE_URL + "user/workflow/form/claim/{taskId}",
                WorkflowFormTO.class, form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getOwner());

        // 5. approve user (and verify that propagation occurred)
        Map<String, WorkflowFormPropertyTO> props = form.getPropertiesAsMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.setProperties(props.values());
        userTO = restTemplate.postForObject(
                BASE_URL + "user/workflow/form/submit",
                form, UserTO.class);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
        assertEquals(Collections.singleton("resource-testdb"),
                userTO.getResources());
        assertFalse(userTO.getPropagationStatusMap().isEmpty());
        exception = null;
        try {
            int id = jdbcTemplate.queryForInt(
                    "SELECT id FROM test WHERE id=?", userTO.getId());
            assertEquals(userTO.getId(), id);
        } catch (EmptyResultDataAccessException e) {
            exception = e;
        }
        assertNull(exception);

        // 6. update user
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("anotherPassword123");

        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);
        assertNotNull(userTO);
    }

    @Test
    public final void delete() {
        try {
            restTemplate.delete(BASE_URL + "user/delete/{userId}", 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        UserTO userTO = getSampleTO("qqgf.z@nn.com");

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);

        restTemplate.delete(BASE_URL + "user/delete/{userId}", userTO.getId());
        try {
            restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                    UserTO.class, userTO.getId());
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public final void count() {
        Integer count = restTemplate.getForObject(
                BASE_URL + "user/count.json", Integer.class);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public final void searchCount() {
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");
        NodeCond searchCond = NodeCond.getLeafCond(isNullCond);

        Integer count = restTemplate.postForObject(
                BASE_URL + "user/search/count.json", searchCond, Integer.class);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public final void list() {
        List<UserTO> users = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "user/list.json", UserTO[].class));
        assertNotNull(users);
        assertFalse(users.isEmpty());
        for (UserTO user : users) {
            assertNotNull(user);
        }
    }

    @Test
    public final void paginatedList() {
        List<UserTO> users = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "user/list/{page}/{size}.json",
                UserTO[].class, 1, 2));

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        for (UserTO user : users) {
            assertNotNull(user);
        }

        users = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "user/list/{page}/{size}.json",
                UserTO[].class, 2, 2));

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        users = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "user/list/{page}/{size}.json",
                UserTO[].class, 100, 2));

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public final void read() {
        UserTO userTO = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json", UserTO.class, 1);

        assertNotNull(userTO);
        assertNotNull(userTO.getAttributes());
        assertFalse(userTO.getAttributes().isEmpty());
    }

    @Test
    public final void search() {
        // LIKE
        AttributeCond fullnameLeafCond1 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");

        AttributeCond fullnameLeafCond2 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");

        NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getLeafCond(fullnameLeafCond1),
                NodeCond.getLeafCond(fullnameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        List<UserTO> matchedUsers = Arrays.asList(
                restTemplate.postForObject(
                BASE_URL + "user/search",
                searchCondition, UserTO[].class));
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        for (UserTO user : matchedUsers) {
            assertNotNull(user);
        }

        // ISNULL
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");
        searchCondition = NodeCond.getLeafCond(isNullCond);

        matchedUsers = Arrays.asList(
                restTemplate.postForObject(BASE_URL + "user/search",
                searchCondition, UserTO[].class));
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertTrue(userIds.contains(2L));
        assertTrue(userIds.contains(3L));
    }

    @Test
    public final void searchByUsernameAndId() {
        final SyncopeUserCond usernameLeafCond =
                new SyncopeUserCond(SyncopeUserCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("user1");

        final SyncopeUserCond idRightCond =
                new SyncopeUserCond(SyncopeUserCond.Type.LT);
        idRightCond.setSchema("id");
        idRightCond.setExpression("2");

        final NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(idRightCond));

        assertTrue(searchCondition.checkValidity());

        final List<UserTO> matchingUsers = Arrays.asList(
                restTemplate.postForObject(
                BASE_URL + "user/search",
                searchCondition, UserTO[].class));

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.size());
        assertEquals("user1", matchingUsers.iterator().next().getUsername());
        assertEquals(1L, matchingUsers.iterator().next().getId());
    }

    @Test
    public final void searchUserByResourceName() {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-2");

        NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getNotLeafCond(ws2),
                NodeCond.getLeafCond(ws1));

        assertTrue(searchCondition.checkValidity());

        List<UserTO> matchedUsers = Arrays.asList(
                restTemplate.postForObject(
                BASE_URL + "user/search",
                searchCondition, UserTO[].class));
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());

        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }

        assertEquals(1, userIds.size());
        assertTrue(userIds.contains(2L));
    }

    @Test
    public final void paginatedSearch() {
        // LIKE
        AttributeCond fullnameLeafCond1 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");

        AttributeCond fullnameLeafCond2 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");

        NodeCond searchCondition = NodeCond.getAndCond(
                NodeCond.getLeafCond(fullnameLeafCond1),
                NodeCond.getLeafCond(fullnameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        List<UserTO> matchedUsers = Arrays.asList(restTemplate.postForObject(
                BASE_URL + "user/search/{page}/{size}",
                searchCondition, UserTO[].class, 1, 2));
        assertNotNull(matchedUsers);

        assertFalse(matchedUsers.isEmpty());
        for (UserTO user : matchedUsers) {
            assertNotNull(user);
        }

        // ISNULL
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");
        searchCondition = NodeCond.getLeafCond(isNullCond);

        matchedUsers = Arrays.asList(restTemplate.postForObject(
                BASE_URL + "user/search/{page}/{size}",
                searchCondition, UserTO[].class, 1, 2));

        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertEquals(2, userIds.size());
    }

    @Test
    public final void updateWithouPassword() {
        UserTO userTO = getSampleTO("updatewithout@password.com");

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);

        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.addDerivedAttributeToBeRemoved("cn");

        userTO = restTemplate.postForObject(
                BASE_URL + "user/update", userMod, UserTO.class);

        assertNotNull(userTO);
        assertNotNull(userTO.getDerivedAttributeMap());
        assertFalse(userTO.getDerivedAttributeMap().containsKey("cn"));
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void updateInvalidPassword() {
        UserTO userTO = getSampleTO("updateinvalid@password.com");

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("pass");

        restTemplate.postForObject(
                BASE_URL + "user/update", userMod, UserTO.class);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public final void updateSamePassword() {
        UserTO userTO = getSampleTO("updatesame@password.com");

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("password123");

        restTemplate.postForObject(
                BASE_URL + "user/update", userMod, UserTO.class);
    }

    @Test
    public final void update() {
        UserTO userTO = getSampleTO("g.h@t.com");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        AttributeTO membershipAttr = new AttributeTO();
        membershipAttr.setSchema("subscriptionDate");
        membershipAttr.addValue("2009-08-18T16:33:12.203+0200");
        membershipTO.addAttribute(membershipAttr);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);

        assertFalse(userTO.getDerivedAttributes().isEmpty());
        assertEquals(1, userTO.getMemberships().size());

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema("subscriptionDate");
        attributeMod.addValueToBeAdded("2010-08-18T16:33:12.203+0200");

        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(8L);
        membershipMod.addAttributeToBeUpdated(attributeMod);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");

        userMod.addAttributeToBeRemoved("userId");
        attributeMod = new AttributeMod();
        attributeMod.setSchema("userId");
        attributeMod.addValueToBeAdded("t.w@spre.net");
        userMod.addAttributeToBeUpdated(attributeMod);

        userMod.addAttributeToBeRemoved("fullname");
        attributeMod = new AttributeMod();
        attributeMod.setSchema("fullname");
        attributeMod.addValueToBeAdded("g.h@t.com");
        userMod.addAttributeToBeUpdated(attributeMod);

        userMod.addDerivedAttributeToBeAdded("cn");
        userMod.addMembershipToBeAdded(membershipMod);
        userMod.addMembershipToBeRemoved(
                userTO.getMemberships().iterator().next().getId());

        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);
        assertNotNull(userTO);

        SyncopeUser passwordTestUser = new SyncopeUser();
        passwordTestUser.setPassword("newPassword", CipherAlgorithm.MD5, 0);
        assertEquals(passwordTestUser.getPassword(), userTO.getPassword());

        assertEquals(1, userTO.getMemberships().size());
        assertEquals(1, userTO.getMemberships().iterator().next().
                getAttributes().size());
        assertFalse(userTO.getDerivedAttributes().isEmpty());
        boolean userIdFound = false;
        boolean fullnameFound = false;
        for (AttributeTO attributeTO : userTO.getAttributes()) {
            if ("userId".equals(attributeTO.getSchema())) {
                userIdFound = true;

                assertEquals(Collections.singletonList("t.w@spre.net"),
                        attributeTO.getValues());
            }
            if ("fullname".equals(attributeTO.getSchema())) {
                fullnameFound = true;

                assertEquals(Collections.singletonList("g.h@t.com"),
                        attributeTO.getValues());
            }
        }
        assertTrue(userIdFound);
        assertTrue(fullnameFound);
    }

    @Test
    public void updatePasswordOnly() {
        List<PropagationTaskTO> beforeTasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));
        assertNotNull(beforeTasks);
        assertFalse(beforeTasks.isEmpty());

        UserTO userTO = getSampleTO("pwdonly@t.com");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        AttributeTO membershipAttr = new AttributeTO();
        membershipAttr.setSchema("subscriptionDate");
        membershipAttr.addValue("2009-08-18T16:33:12.203+0200");
        membershipTO.addAttribute(membershipAttr);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");

        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);

        // check for changePwdDate
        assertNotNull(userTO.getChangePwdDate());

        SyncopeUser passwordTestUser = new SyncopeUser();
        passwordTestUser.setPassword("newPassword", CipherAlgorithm.MD5, 0);
        assertEquals(passwordTestUser.getPassword(), userTO.getPassword());

        List<PropagationTaskTO> afterTasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));
        assertNotNull(afterTasks);
        assertFalse(afterTasks.isEmpty());

        assertTrue(beforeTasks.size() < afterTasks.size());
    }

    @Test
    public final void verifyTaskRegistration() {
        // get task list
        List<PropagationTaskTO> tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        // get max task id
        long maxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > maxId) {
                maxId = task.getId();
            }
        }

        // --------------------------------------
        // Create operation
        // --------------------------------------

        UserTO userTO = getSampleTO("task@propagation.mode");

        // add a membership
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        // 1. create user
        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);

        // get the new task list
        tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        // get max task id
        long newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }

        // default configuration for ws-target-resource2:
        //             only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);

        // --------------------------------------
        // Update operation
        // --------------------------------------
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema("surname");
        attributeMod.addValueToBeAdded("surname");
        userMod.addAttributeToBeUpdated(attributeMod);

        userTO = restTemplate.postForObject(
                BASE_URL + "user/update", userMod, UserTO.class);

        assertNotNull(userTO);

        // get the new task list
        tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        // get max task id
        maxId = newMaxId;
        newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }

        // default configuration for ws-target-resource2:
        //             all update executions have to be registered
        assertTrue(newMaxId > maxId);

        // --------------------------------------
        // Delete operation
        // --------------------------------------
        restTemplate.delete(
                BASE_URL + "user/delete/{userId}", userTO.getId());

        // get the new task list
        tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));

        // get max task id
        maxId = newMaxId;
        newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }

        // default configuration for ws-target-resource2:
        //             no delete executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);
    }

    @Test
    public final void suspendReactivate() {
        UserTO userTO = getSampleTO("suspendReactivate@syncope-idm.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        userTO = restTemplate.getForObject(
                BASE_URL + "user/suspend/" + userTO.getId(), UserTO.class);

        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        userTO = restTemplate.getForObject(
                BASE_URL + "user/reactivate/" + userTO.getId(), UserTO.class);

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @ExpectedException(EmptyResultDataAccessException.class)
    @Test
    public void issue213() {
        UserTO userTO = getSampleTO("issue213@syncope-idm.org");
        userTO.addResource("resource-testdb");

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        int id = jdbcTemplate.queryForInt(
                "SELECT id FROM test WHERE id=?", userTO.getId());
        assertEquals(userTO.getId(), id);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.addResourceToBeRemoved("resource-testdb");

        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);
        assertTrue(userTO.getResources().isEmpty());

        jdbcTemplate.queryForInt(
                "SELECT id FROM test WHERE id=?", userTO.getId());
    }

    @Test
    public void issue234() {
        UserTO userTO = getSampleTO("issue234@syncope-idm.org");
        userTO.addResource("resource-ldap");

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setUsername("1" + userTO.getUsername());

        userTO = restTemplate.postForObject(BASE_URL + "user/update",
                userMod, UserTO.class);
        assertNotNull(userTO);
        assertEquals("1issue234@syncope-idm.org", userTO.getUsername());
    }

    @Test
    public final void issue270() {
        // 1. create a new user without virtual attributes
        UserTO original = getSampleTO("issue270@syncope-idm.org");
        // be sure to remove all virtual attributes
        original.setVirtualAttributes(Collections.EMPTY_LIST);

        original = restTemplate.postForObject(
                BASE_URL + "user/create", original, UserTO.class);

        assertNotNull(original);
        assertTrue(original.getVirtualAttributes().isEmpty());

        UserTO toBeUpdated = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json",
                UserTO.class, original.getId());

        AttributeTO virtual = new AttributeTO();
        virtual.setSchema("virtualdata");
        virtual.addValue("virtualvalue");

        toBeUpdated.addVirtualAttribute(virtual);

        // 2. try to update by adding a resource, but no password: must fail
        UserMod userMod = AttributableOperations.diff(toBeUpdated, original);

        assertNotNull(userMod);

        toBeUpdated = restTemplate.postForObject(
                BASE_URL + "user/update", userMod, UserTO.class);

        assertNotNull(toBeUpdated);
        assertFalse(toBeUpdated.getVirtualAttributes().isEmpty());
        assertNotNull(toBeUpdated.getVirtualAttributes().get(0));

        assertEquals(virtual.getSchema(),
                toBeUpdated.getVirtualAttributes().get(0).getSchema());
    }

    @Test
    public final void issue280() {
        UserTO userTO = getSampleTO("issue280@syncope-idm.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerivedAttributes().clear();

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("123password");
        userMod.addResourceToBeAdded("resource-testdb");

        userTO = restTemplate.postForObject(
                BASE_URL + "user/update", userMod, UserTO.class);
        assertNotNull(userTO);

        final Map<String, PropagationTaskExecStatus> propStatusMap =
                userTO.getPropagationStatusMap();

        assertNotNull(propStatusMap);
        assertEquals(1, propStatusMap.size());
        assertEquals(1, propStatusMap.size());

        final PropagationTaskExecStatus exec =
                propStatusMap.values().iterator().next();

        assertNotNull(exec);
        assertEquals("resource-testdb", propStatusMap.keySet().iterator().next());
        assertTrue(exec.isSuccessful());
    }
}
