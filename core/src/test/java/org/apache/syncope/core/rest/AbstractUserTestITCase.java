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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.syncope.NotFoundException;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.apache.syncope.core.init.SpringContextInitializer;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.exceptions.InvalidSearchConditionException;
import org.apache.syncope.exceptions.UnauthorizedRoleException;
import org.apache.syncope.mod.AttributeMod;
import org.apache.syncope.mod.MembershipMod;
import org.apache.syncope.mod.StatusMod;
import org.apache.syncope.mod.StatusMod.Status;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.propagation.PropagationException;
import org.apache.syncope.search.AttributeCond;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.search.ResourceCond;
import org.apache.syncope.search.SyncopeUserCond;
import org.apache.syncope.services.UserService;
import org.apache.syncope.to.AttributeTO;
import org.apache.syncope.to.ConfigurationTO;
import org.apache.syncope.to.ConnObjectTO;
import org.apache.syncope.to.MembershipTO;
import org.apache.syncope.to.PasswordPolicyTO;
import org.apache.syncope.to.PolicyTO;
import org.apache.syncope.to.PropagationTO;
import org.apache.syncope.to.PropagationTaskTO;
import org.apache.syncope.to.ResourceTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.to.WorkflowFormPropertyTO;
import org.apache.syncope.to.WorkflowFormTO;
import org.apache.syncope.types.CipherAlgorithm;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.apache.syncope.util.AttributableOperations;
import org.apache.syncope.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.workflow.WorkflowException;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

public abstract class AbstractUserTestITCase extends AbstractTest {

    protected UserService userService;

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
    public void createUserWithNoPropagation() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        // get task list
        List<PropagationTaskTO> tasks = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "task/propagation/list.json", PropagationTaskTO[].class));

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
        UserTO userTO = getSampleTO();

        userTO.setPassword("password123");
        userTO.addResource("ws-target-resource-nopropagation");

        userCreateAndGet(userTO);

        // get the new task list
        tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list.json",
                PropagationTaskTO[].class));

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
        PropagationTaskTO taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}.json",
                PropagationTaskTO.class, newMaxId);

        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    // This test should be moved to PolicyTestITCase
    /*
     * This test has been introduced to verify and solve the following issue:
     * http://code.google.com/p/syncope/issues/detail?id=172. Creations of a new
     * user without having a global password policy stored into the local
     * repository used to fail with a null pointer exception. This bug has been
     * fixed introducing a simple control.
     */
    @Ignore
    public void issue172() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        // This test causes sideeffects to other tests, which depend on policy
        // with the id 2L. ReCreating this policy
        // after deletion is not sufficient, because another ID will be assigned
        PolicyTO policyTO = restTemplate.getForObject(BASE_URL + "policy/read/{id}", PasswordPolicyTO.class,
                2L);

        assertNotNull(policyTO);

        restTemplate.getForObject(BASE_URL + "policy/delete/{id}", PasswordPolicyTO.class, 2L);
        try {
            UserTO userTO = getSampleTO();
            userTO.setPassword("password");

            userCreateAndGet(userTO);

        } finally {
            policyTO = restTemplate.postForObject(BASE_URL + "policy/password/create", policyTO,
                    PasswordPolicyTO.class);
            assertNotNull(policyTO);
        }
    }

    @Test
    public void issue186() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        // 1. create an user with strict mandatory attributes only
        String uid = getSampleEmail();
        UserTO userTO = new UserTO();
        userTO.setUsername(uid);
        userTO.setPassword("password");

        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue(uid);
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("fullname");
        attributeTO.addValue(uid);
        userTO.addAttribute(attributeTO);

        attributeTO = new AttributeTO();
        attributeTO.setSchema("surname");
        attributeTO.addValue(uid);
        userTO.addAttribute(attributeTO);

        userTO = userCreateAndGet(userTO);

        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource forcing mandatory constraints: must
        // fail with RequiredValuesMissing
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.addResourceToBeAdded("ws-target-resource-2");

        try {
            userTO = userService.update(userMod.getId(), userMod);
            fail("SyncopeClientCompositeErrorException expected");
        } catch (SyncopeClientCompositeErrorException scce) {
            assertNotNull(scce.getException(SyncopeClientExceptionType.RequiredValuesMissing));
        }

        // 3. update assigning a resource NOT forcing mandatory constraints
        // AND primary: must fail with PropagationException
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword");
        userMod.addResourceToBeAdded("ws-target-resource-1");

        try {
            userTO = userService.update(userMod.getId(), userMod);
            fail("SyncopeClientCompositeErrorException expected");
        } catch (SyncopeClientCompositeErrorException scce) {
            assertNotNull(scce.getException(SyncopeClientExceptionType.Propagation));
        }

        // Cleanup
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.addResourceToBeRemoved("ws-target-resource-1");
        userTO = userService.update(userMod.getId(), userMod);

        // 4. update assigning a resource NOT forcing mandatory constraints
        // BUT not primary: must succeed
        userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword2");
        userMod.addResourceToBeAdded("resource-db");

        userTO = userService.update(userMod.getId(), userMod);
    }

    /**
     * @param userTO
     *            User to be stored
     * @return Newly created User
     * @throws PropagationException
     * @throws UnauthorizedRoleException
     * @throws WorkflowException
     * @throws NotFoundException
     */
    public UserTO userCreateAndGet(UserTO userTO) throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        Response response = userService.create(userTO);

        return resolve(UserTO.class, response, userService);
    }

    @Test
    public void testEnforceMandatoryCondition() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.addResource("ws-target-resource-2");
        userTO.setPassword("newPassword");

        AttributeTO type = null;
        for (AttributeTO attr : userTO.getAttributes()) {
            if ("type".equals(attr.getSchema())) {
                type = attr;
            }
        }
        assertNotNull(type);
        userTO.removeAttribute(type);

        try {
            userService.create(userTO);
            fail("SyncopeClientCompositeErrorException expected");
        } catch (SyncopeClientCompositeErrorException scce) {
            assertNotNull(scce.getException(SyncopeClientExceptionType.RequiredValuesMissing));
        }

        userTO.addAttribute(type);

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
    }

    @Test
    @Ignore
    public void testEnforceMandatoryConditionOnDerived() throws PropagationException,
            UnauthorizedRoleException, WorkflowException, NotFoundException {
        ResourceTO resourceTO;

        try {
            resourceTO = restTemplate.getForObject(BASE_URL + "/resource/read/{resourceName}.json",
                    ResourceTO.class, "resource-csv-enforcing");
        } catch (Exception e) {
            resourceTO = restTemplate.getForObject(BASE_URL + "/resource/read/{resourceName}.json",
                    ResourceTO.class, "resource-csv");

            resourceTO.setName("resource-csv-enforcing");
            resourceTO.setEnforceMandatoryCondition(true);
            // FIXME create operation fails
            resourceTO = restTemplate.postForObject(BASE_URL + "resource/create.json", resourceTO,
                    ResourceTO.class);
        }

        assertNotNull(resourceTO);

        UserTO userTO = getSampleTO();
        userTO.addResource(resourceTO.getName());
        userTO.setPassword("newPassword");

        try {
            userTO = userCreateAndGet(userTO);
            fail("SyncopeClientExceptionType expected");
        } catch (SyncopeClientCompositeErrorException scce) {
            assertNotNull(scce.getException(SyncopeClientExceptionType.RequiredValuesMissing));
        }

        AttributeTO derAttTO = new AttributeTO();
        derAttTO.setSchema("csvuserid");
        userTO.addDerivedAttribute(derAttTO);

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
        assertEquals(Collections.singleton("resource-csv-enforcing"), userTO.getResources());
    }

    @Test
    public void issue147() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        // 1. create an user without role nor resources
        UserTO userTO = getSampleTO();

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. try to update by adding a resource, but no password: must fail
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.addResourceToBeAdded("ws-target-resource-2");

        try {
            userTO = userService.update(userMod.getId(), userMod);
            fail("SyncopeClientCompositeErrorException expected");
        } catch (SyncopeClientCompositeErrorException scce) {
            assertNotNull(scce.getException(SyncopeClientExceptionType.RequiredValuesMissing));
        }

        // 3. provide password: now update must work
        userMod.setPassword("newPassword");
        userTO = userService.update(userMod.getId(), userMod);
        assertNotNull(userTO);
        assertEquals(1, userTO.getResources().size());
    }

    @Test
    @Ignore
    // Are propagations required after create?
    public void createUserWithDbPropagation() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
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

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
        assertEquals(1, userTO.getPropagationTOs().size());
        assertTrue(userTO.getPropagationTOs().get(0).getStatus().isSuccessful());
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithInvalidPassword() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO("invalidpasswd@syncope.apache.org");
        userTO.setPassword("pass");

        userCreateAndGet(userTO);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithInvalidUsername() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO("invalidusername@syncope.apache.org");
        userTO.setUsername("us");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);

        userTO.addMembership(membershipTO);

        userCreateAndGet(userTO);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithInvalidPasswordByRes() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO("invalidPwdByRes@passwd.com");

        // configured to be minLength=16
        userTO.setPassword("password1");

        userTO.setResources(Collections.singleton("ws-target-resource-nopropagation"));

        userCreateAndGet(userTO);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithInvalidPasswordByRole() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO("invalidPwdByRole@passwd.com");

        // configured to be minLength=16
        userTO.setPassword("password1");

        final MembershipTO membership = new MembershipTO();
        membership.setRoleId(8L);

        userTO.addMembership(membership);

        userCreateAndGet(userTO);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void createWithException() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("userId");
        attributeTO.addValue("userId@nowhere.org");

        UserTO newUserTO = new UserTO();
        newUserTO.addAttribute(attributeTO);

        userCreateAndGet(newUserTO);
    }

    @Test
    public void create() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        // get task list
        List<PropagationTaskTO> tasks = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "task/propagation/list.json", PropagationTaskTO[].class));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        // get max task id
        long maxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > maxId) {
                maxId = task.getId();
            }
        }
        PropagationTaskTO taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}.json",
                PropagationTaskTO.class, maxId);

        assertNotNull(taskTO);
        int maxTaskExecutions = taskTO.getExecutions().size();

        String uniqueId = getSampleEmail();
        UserTO userTO = getSampleTO(uniqueId);
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        // add an attribute with no values: must be ignored
        AttributeTO nullValueAttrTO = new AttributeTO();
        nullValueAttrTO.setSchema("subscriptionDate");
        nullValueAttrTO.setValues(null);
        membershipTO.addAttribute(nullValueAttrTO);

        // add an attribute with a non-existing schema: must be ignored
        AttributeTO attrWithInvalidSchemaTO = new AttributeTO();
        attrWithInvalidSchemaTO.setSchema("invalid schema");
        attrWithInvalidSchemaTO.addValue("a value");
        userTO.addAttribute(attrWithInvalidSchemaTO);

        // add an attribute with null value: must be ignored
        nullValueAttrTO = new AttributeTO();
        nullValueAttrTO.setSchema("activationDate");
        nullValueAttrTO.addValue(null);
        userTO.addAttribute(nullValueAttrTO);

        // 1. create user
        UserTO newUserTO = userCreateAndGet(userTO);

        assertFalse(newUserTO.getAttributes().contains(attrWithInvalidSchemaTO));

        // check for changePwdDate
        assertNotNull(newUserTO.getCreationDate());

        // 2. check for virtual attribute value
        assertNotNull(newUserTO.getVirtualAttributeMap());
        assertNotNull(newUserTO.getVirtualAttributeMap().get("virtualdata").getValues());
        assertFalse(newUserTO.getVirtualAttributeMap().get("virtualdata").getValues().isEmpty());
        assertEquals("virtualvalue", newUserTO.getVirtualAttributeMap().get("virtualdata").getValues().get(0));

        // get the new task list
        tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list.json",
                PropagationTaskTO[].class));

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
        // only failed executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);

        // get last task
        taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}.json", PropagationTaskTO.class,
                newMaxId);

        assertNotNull(taskTO);
        assertEquals(maxTaskExecutions, taskTO.getExecutions().size());

        // 3. verify password
        Boolean verify = userService.verifyPassword(newUserTO.getUsername(), "password123");
        assertTrue(verify);

        verify = userService.verifyPassword(newUserTO.getUsername(), "passwordXX");
        assertFalse(verify);

        // 4. try (and fail) to create another user with same (unique) values
        userTO = getSampleTO();
        for (AttributeTO attr : userTO.getAttributes()) {
            if ("userId".equals(attr.getSchema())) {
                attr.getValues().clear();
                attr.addValue(uniqueId);
            }
        }

        try {
            userCreateAndGet(newUserTO);
            fail("You should not be able to create a user twice with the same unique identifiers!");
        } catch (SyncopeClientCompositeErrorException e) {
            // TODO DataIntegrityViolation Exception Mapping
        }
    }

    @Test
    public void createWithRequiredValueMissing() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();

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

        try {
            // 1. create user without type (mandatory by UserSchema)
            userCreateAndGet(userTO);
        } catch (SyncopeClientCompositeErrorException ex) {
            assertNotNull(ex.getException(SyncopeClientExceptionType.RequiredValuesMissing));
        }

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
        try {
            userCreateAndGet(userTO);
        } catch (SyncopeClientCompositeErrorException ex) {
            assertNotNull(ex.getException(SyncopeClientExceptionType.RequiredValuesMissing));
        }
    }

    @Test
    public void createWithReject() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        Assume.assumeTrue(SpringContextInitializer.isActivitiConfigured());

        UserTO userTO = getSampleTO();

        // User with role 9 are defined in workflow as subject to approval
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(9L);
        userTO.addMembership(membershipTO);

        // 1. create user with role 9
        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(9, userTO.getMemberships().get(0).getRoleId());
        assertEquals("createApproval", userTO.getStatus());

        // 2. request if there is any pending task for user just created
        WorkflowFormTO form = userService.getFormForUser(userTO.getId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNull(form.getOwner());

        // 3. claim task from user1, not in role 7 (designated for approval in
        // workflow definition): fail
        PreemptiveAuthHttpRequestFactory requestFactory = ((PreemptiveAuthHttpRequestFactory) restTemplate
                .getRequestFactory());
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials("user1", "password"));

        // SyncopeClientException sce = null;
        try {
            userService.claimForm(form.getTaskId());
        } catch (SyncopeClientCompositeErrorException scce) {
            assertNotNull(scce.getException(SyncopeClientExceptionType.Workflow));
        }

        // 4. claim task from user4, in role 7
        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials("user4", "password"));

        form = userService.claimForm(form.getTaskId());
        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getOwner());

        // 5. reject user
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.FALSE.toString());
        props.get("rejectReason").setValue("I don't like him.");
        form.setProperties(props.values());
        userTO = userService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("rejected", userTO.getStatus());

        // reset admin credentials for restTemplate
        super.resetRestTemplate();
    }

    @Test
    public void createWithApproval() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        Assume.assumeTrue(SpringContextInitializer.isActivitiConfigured());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        UserTO userTO = getSampleTO();
        userTO.addResource("resource-testdb");

        // User with role 9 are defined in workflow as subject to approval
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(9L);
        userTO.addMembership(membershipTO);

        // 1. create user with role 9 (and verify that no propagation occurred)
        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(9, userTO.getMemberships().get(0).getRoleId());
        assertEquals("createApproval", userTO.getStatus());
        assertEquals(Collections.singleton("resource-testdb"), userTO.getResources());

        assertTrue(userTO.getPropagationTOs().isEmpty());

        try {
            jdbcTemplate.queryForInt("SELECT id FROM test WHERE id=?", userTO.getUsername());
            fail("EmptyResultDataAccessException expected");
        } catch (EmptyResultDataAccessException e) {
            assertNotNull(e);
        }

        // 2. request if there is any pending task for user just created
        WorkflowFormTO form = userService.getFormForUser(userTO.getId());

        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNull(form.getOwner());

        // 4. claim task (from admin)
        form = userService.claimForm(form.getTaskId());

        assertNotNull(form);
        assertNotNull(form.getTaskId());
        assertNotNull(form.getOwner());

        // 5. approve user (and verify that propagation occurred)
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.setProperties(props.values());
        userTO = userService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
        assertEquals(Collections.singleton("resource-testdb"), userTO.getResources());

        try {
            final String username = jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?",
                    String.class, userTO.getUsername());
            assertEquals(userTO.getUsername(), username);
        } catch (EmptyResultDataAccessException e) {
            fail(e.getMessage());
        }

        // 6. update user
        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("anotherPassword123");

        userTO = userService.update(userMod.getId(), userMod);
        assertNotNull(userTO);
    }

    @Test
    public void delete() throws WorkflowException, PropagationException, UnauthorizedRoleException,
            NotFoundException {
        try {
            userService.delete(0L);
            fail("User should not be found");
        } catch (SyncopeClientCompositeErrorException sccee) {
            Throwable t = sccee.getException(SyncopeClientExceptionType.NotFound);
            assertNotNull(t);
        }

        UserTO userTO = getSampleTO();
        // specify a propagation
        userTO.addResource("resource-testdb");

        userTO = userCreateAndGet(userTO);

        long id = userTO.getId();

        Response response = userService.delete(id);

        assertNotNull(response);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        // assertFalse(userTO.getPropagationTOs().isEmpty());
        // assertTrue(userTO.getPropagationTOs().get(0).getStatus().isSuccessful());

        try {
            userService.read(userTO.getId());
            fail("User should be deleted and thus cannot be found");
        } catch (SyncopeClientCompositeErrorException sccee) {
            Throwable t = sccee.getException(SyncopeClientExceptionType.NotFound);
            assertNotNull(t);
        }
    }

    @Test
    public void deleteByUsername() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        String uid = getSampleEmail();
        UserTO userTO = getSampleTO(uid);
        // specify a propagation
        userTO.addResource("resource-testdb");

        userTO = userCreateAndGet(userTO);

        Response response = userService.delete(userTO.getId());

        assertNotNull(response);
        assertEquals(HttpStatus.SC_OK, response.getStatus());

        // check for propagation result
        // assertFalse(userTO.getPropagationTOs().isEmpty());
        // assertTrue(userTO.getPropagationTOs().get(0).getStatus().isSuccessful());

        try {
            userService.read(uid);
            fail("User should have been deleted");
        } catch (SyncopeClientCompositeErrorException sccee) {
            Throwable t = sccee.getException(SyncopeClientExceptionType.NotFound);
            assertNotNull(t);
        }
    }

    public static UserTO getSampleTO() {
        return getSampleTO(getSampleEmail());
    }

    @Test
    public void count() {
        Integer count = userService.count();
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void searchCount() throws InvalidSearchConditionException {
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");
        NodeCond searchCond = NodeCond.getLeafCond(isNullCond);

        Integer count = userService.searchCount(searchCond);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void list() {
        List<UserTO> users = userService.list();
        assertNotNull(users);
        assertFalse(users.isEmpty());
        for (UserTO user : users) {
            assertNotNull(user);
        }
    }

    @Test
    public void paginatedList() {
        List<UserTO> users = userService.list(1, 2);

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        for (UserTO user : users) {
            assertNotNull(user);
        }

        users = userService.list(2, 1);

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());

        users = userService.list(100, 20);

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void read() throws NotFoundException, UnauthorizedRoleException {
        UserTO userTO = userService.read(1L);

        assertNotNull(userTO);
        assertNotNull(userTO.getAttributes());
        assertFalse(userTO.getAttributes().isEmpty());
    }

    @Test
    public void search() throws InvalidSearchConditionException {
        // LIKE
        AttributeCond fullnameLeafCond1 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");

        AttributeCond fullnameLeafCond2 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");

        NodeCond searchCondition = NodeCond.getAndCond(NodeCond.getLeafCond(fullnameLeafCond1),
                NodeCond.getLeafCond(fullnameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        List<UserTO> matchedUsers = userService.search(searchCondition);

        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        for (UserTO user : matchedUsers) {
            assertNotNull(user);
        }

        // ISNULL
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");
        searchCondition = NodeCond.getLeafCond(isNullCond);

        matchedUsers = userService.search(searchCondition);
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
    public void searchByUsernameAndId() throws InvalidSearchConditionException {
        final SyncopeUserCond usernameLeafCond = new SyncopeUserCond(SyncopeUserCond.Type.EQ);
        usernameLeafCond.setSchema("username");
        usernameLeafCond.setExpression("user1");

        final SyncopeUserCond idRightCond = new SyncopeUserCond(SyncopeUserCond.Type.LT);
        idRightCond.setSchema("id");
        idRightCond.setExpression("2");

        final NodeCond searchCondition = NodeCond.getAndCond(NodeCond.getLeafCond(usernameLeafCond),
                NodeCond.getLeafCond(idRightCond));

        assertTrue(searchCondition.checkValidity());

        final List<UserTO> matchingUsers = userService.search(searchCondition);

        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.size());
        assertEquals("user1", matchingUsers.iterator().next().getUsername());
        assertEquals(1L, matchingUsers.iterator().next().getId());
    }

    @Test
    public void searchUserByResourceName() throws InvalidSearchConditionException {
        ResourceCond ws2 = new ResourceCond();
        ws2.setResourceName("ws-target-resource2");

        ResourceCond ws1 = new ResourceCond();
        ws1.setResourceName("ws-target-resource-list-mappings-2");

        NodeCond searchCondition = NodeCond.getAndCond(NodeCond.getNotLeafCond(ws2),
                NodeCond.getLeafCond(ws1));

        assertTrue(searchCondition.checkValidity());

        List<UserTO> matchedUsers = userService.search(searchCondition);
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
    public void paginatedSearch() throws InvalidSearchConditionException {
        // LIKE
        AttributeCond fullnameLeafCond1 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");

        AttributeCond fullnameLeafCond2 = new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");

        NodeCond searchCondition = NodeCond.getAndCond(NodeCond.getLeafCond(fullnameLeafCond1),
                NodeCond.getLeafCond(fullnameLeafCond2));

        assertTrue(searchCondition.checkValidity());

        List<UserTO> matchedUsers = userService.search(searchCondition, 1, 2);
        assertNotNull(matchedUsers);

        assertFalse(matchedUsers.isEmpty());
        for (UserTO user : matchedUsers) {
            assertNotNull(user);
        }

        // ISNULL
        AttributeCond isNullCond = new AttributeCond(AttributeCond.Type.ISNULL);
        isNullCond.setSchema("loginDate");
        searchCondition = NodeCond.getLeafCond(isNullCond);

        matchedUsers = userService.search(searchCondition, 1, 2);

        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertEquals(2, userIds.size());
    }

    @Test
    public void updateWithouPassword() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();

        userTO = userCreateAndGet(userTO);

        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.addDerivedAttributeToBeRemoved("cn");

        userTO = userService.update(userMod.getId(), userMod);

        assertNotNull(userTO);
        assertNotNull(userTO.getDerivedAttributeMap());
        assertFalse(userTO.getDerivedAttributeMap().containsKey("cn"));
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void updateInvalidPassword() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("pass");

        userService.update(userMod.getId(), userMod);
    }

    @Test(expected = SyncopeClientCompositeErrorException.class)
    public void updateSamePassword() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("password123");

        userService.update(userMod.getId(), userMod);
    }

    @Test
    public void update() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        String email = getSampleEmail();
        UserTO userTO = getSampleTO(email);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        AttributeTO membershipAttr = new AttributeTO();
        membershipAttr.setSchema("subscriptionDate");
        membershipAttr.addValue("2009-08-18T16:33:12.203+0200");
        membershipTO.addAttribute(membershipAttr);
        userTO.addMembership(membershipTO);

        userTO = userCreateAndGet(userTO);

        assertFalse(userTO.getDerivedAttributes().isEmpty());
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(1, userTO.getMemberships().iterator().next().getAttributes().size());

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema("subscriptionDate");
        attributeMod.addValueToBeAdded("2010-08-18T16:33:12.203+0200");

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("new2Password");

        String newUserId = getSampleEmail();

        userMod.addAttributeToBeRemoved("userId");
        attributeMod = new AttributeMod();
        attributeMod.setSchema("userId");
        attributeMod.addValueToBeAdded(newUserId);
        userMod.addAttributeToBeUpdated(attributeMod);

        userMod.addAttributeToBeRemoved("fullname");
        attributeMod = new AttributeMod();
        attributeMod.setSchema("fullname");
        attributeMod.addValueToBeAdded(email);
        userMod.addAttributeToBeUpdated(attributeMod);

        userMod.addDerivedAttributeToBeAdded("cn");

        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(8L);
        attributeMod = new AttributeMod();
        attributeMod.setSchema("subscriptionDate");
        attributeMod.addValueToBeAdded("2010-08-18T16:33:12.203+0200");
        membershipMod.addAttributeToBeUpdated(attributeMod);
        userMod.addMembershipToBeAdded(membershipMod);
        userMod.addMembershipToBeRemoved(userTO.getMemberships().get(0).getId());

        userTO = userService.update(userMod.getId(), userMod);

        assertNotNull(userTO);

        SyncopeUser passwordTestUser = new SyncopeUser();
        passwordTestUser.setPassword("new2Password", CipherAlgorithm.SHA1, 0);
        assertEquals(passwordTestUser.getPassword(), userTO.getPassword());

        assertEquals(1, userTO.getMemberships().size());
        assertEquals(1, userTO.getMemberships().iterator().next().getAttributes().size());
        assertFalse(userTO.getDerivedAttributes().isEmpty());

        boolean userIdFound = false;
        boolean fullnameFound = false;

        for (AttributeTO attributeTO : userTO.getAttributes()) {
            if ("userId".equals(attributeTO.getSchema())) {
                userIdFound = true;
                assertEquals(Collections.singletonList(newUserId), attributeTO.getValues());
            }
            if ("fullname".equals(attributeTO.getSchema())) {
                fullnameFound = true;
                assertEquals(Collections.singletonList(email), attributeTO.getValues());
            }
        }
        assertTrue(userIdFound);
        assertTrue(fullnameFound);
    }

    public static String getSampleEmail() {
        String email = "m." + UUID.randomUUID().toString().substring(0, 8) + ".n@apache.com";
        return email;
    }

    @Test
    public void updatePasswordOnly() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        List<PropagationTaskTO> beforeTasks = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "task/propagation/list", PropagationTaskTO[].class));
        assertNotNull(beforeTasks);
        assertFalse(beforeTasks.isEmpty());

        UserTO userTO = getSampleTO();
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        AttributeTO membershipAttr = new AttributeTO();
        membershipAttr.setSchema("subscriptionDate");
        membershipAttr.addValue("2009-08-18T16:33:12.203+0200");
        membershipTO.addAttribute(membershipAttr);
        userTO.addMembership(membershipTO);

        userTO = userCreateAndGet(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("newPassword123");

        userTO = userService.update(userMod.getId(), userMod);
        ;

        // check for changePwdDate
        assertNotNull(userTO.getChangePwdDate());

        SyncopeUser passwordTestUser = new SyncopeUser();
        passwordTestUser.setPassword("newPassword123", CipherAlgorithm.SHA1, 0);
        assertEquals(passwordTestUser.getPassword(), userTO.getPassword());

        List<PropagationTaskTO> afterTasks = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "task/propagation/list", PropagationTaskTO[].class));
        assertNotNull(afterTasks);
        assertFalse(afterTasks.isEmpty());

        assertTrue(beforeTasks.size() < afterTasks.size());
    }

    @Test
    public void verifyTaskRegistration() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        // get task list
        List<PropagationTaskTO> tasks = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "task/propagation/list", PropagationTaskTO[].class));

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

        UserTO userTO = getSampleTO();

        // add a membership
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        // 1. create user
        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);

        // get the new task list
        tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list",
                PropagationTaskTO[].class));

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
        // only failed executions have to be registered
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

        userTO = userService.update(userMod.getId(), userMod);

        assertNotNull(userTO);

        // get the new task list
        tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list",
                PropagationTaskTO[].class));

        // get max task id
        maxId = newMaxId;
        newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }

        // default configuration for ws-target-resource2:
        // all update executions have to be registered
        assertTrue(newMaxId > maxId);

        final PropagationTaskTO taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}.json",
                PropagationTaskTO.class, newMaxId);

        assertNotNull(taskTO);
        assertEquals(1, taskTO.getExecutions().size());

        // --------------------------------------
        // Delete operation
        // --------------------------------------
        userService.delete(userTO.getId());

        // get the new task list
        tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list",
                PropagationTaskTO[].class));

        // get max task id
        maxId = newMaxId;
        newMaxId = Long.MIN_VALUE;
        for (PropagationTaskTO task : tasks) {
            if (task.getId() > newMaxId) {
                newMaxId = task.getId();
            }
        }

        // default configuration for ws-target-resource2:
        // no delete executions have to be registered
        // --> no more tasks/executions should be added
        assertEquals(newMaxId, maxId);
    }

    @Test
    public void createActivate() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        Assume.assumeTrue(SpringContextInitializer.isActivitiConfigured());

        UserTO userTO = getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(11L);
        userTO.addMembership(membershipTO);

        userTO = userCreateAndGet(userTO);

        assertNotNull(userTO);
        assertNotNull(userTO.getToken());
        assertNotNull(userTO.getTokenExpireTime());

        assertEquals("created", userTO.getStatus());

        StatusMod statusMod = new StatusMod(userTO.getId(), Status.ACTIVATE);
        statusMod.setToken(userTO.getToken());

        userTO = userService.setStatus(statusMod.getId(), statusMod);

        assertNotNull(userTO);
        assertNull(userTO.getToken());
        assertNull(userTO.getTokenExpireTime());

        assertEquals("active", userTO.getStatus());
    }

    @Test
    @Ignore
    public void createActivateByUsername() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        Assume.assumeTrue(SpringContextInitializer.isActivitiConfigured());

        UserTO userTO = getSampleTO("createActivateByUsername@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(11L);
        userTO.addMembership(membershipTO);

        userTO = userCreateAndGet(userTO);

        assertNotNull(userTO);
        assertNotNull(userTO.getToken());
        assertNotNull(userTO.getTokenExpireTime());

        assertEquals("created", userTO.getStatus());

        userTO = restTemplate.getForObject(BASE_URL + "user/activateByUsername/{username}.json?token="
                + userTO.getToken(), UserTO.class, userTO.getUsername());

        assertNotNull(userTO);
        assertNull(userTO.getToken());
        assertNull(userTO.getTokenExpireTime());

        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void suspendReactivate() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        userTO.addMembership(membershipTO);

        userTO = userCreateAndGet(userTO);

        assertNotNull(userTO);
        assertEquals(SpringContextInitializer.isActivitiConfigured()
                ? "active"
                : "created", userTO.getStatus());

        StatusMod statusMod = new StatusMod(userTO.getId(), Status.SUSPEND);

        userTO = userService.setStatus(userTO.getId(), statusMod);

        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        statusMod.setStatus(Status.REACTIVATE);
        userTO = userService.setStatus(userTO.getId(), statusMod);

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @Test
    @Ignore
    public void suspendReactivateByUsername() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO("suspendReactivateByUsername@syncope.apache.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        userTO.addMembership(membershipTO);

        userTO = userCreateAndGet(userTO);

        assertNotNull(userTO);
        assertEquals(SpringContextInitializer.isActivitiConfigured()
                ? "active"
                : "created", userTO.getStatus());

        userTO = restTemplate.getForObject(BASE_URL + "user/suspendByUsername/{username}.json", UserTO.class,
                userTO.getUsername());

        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        userTO = restTemplate.getForObject(BASE_URL + "user/reactivateByUsername/{username}.json",
                UserTO.class, userTO.getUsername());

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void suspendReactivateOnResource() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();

        userTO.getMemberships().clear();
        userTO.getResources().clear();

        ResourceTO dbTable = restTemplate.getForObject(BASE_URL + "/resource/read/{resourceName}.json",
                ResourceTO.class, "resource-testdb");

        assertNotNull(dbTable);
        userTO.addResource(dbTable.getName());

        ResourceTO ldap = restTemplate.getForObject(BASE_URL + "/resource/read/{resourceName}.json",
                ResourceTO.class, "resource-ldap");

        assertNotNull(ldap);
        userTO.addResource(ldap.getName());

        userTO = userCreateAndGet(userTO);

        assertNotNull(userTO);
        assertEquals(SpringContextInitializer.isActivitiConfigured()
                ? "active"
                : "created", userTO.getStatus());

        StatusMod statusMod = new StatusMod(userTO.getId(), Status.SUSPEND);
        statusMod.getExcludeResources().add(dbTable.getName());
        statusMod.getExcludeResources().add(ldap.getName());

        userTO = userService.setStatus(userTO.getId(), statusMod);

        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        String dbTableUID = userTO.getUsername();
        assertNotNull(dbTableUID);

        ConnObjectTO connObjectTO = restTemplate.getForObject(BASE_URL
                + "/resource/{resourceName}/read/{objectId}.json", ConnObjectTO.class, dbTable.getName(),
                dbTableUID);

        assertFalse(Boolean.parseBoolean(connObjectTO.getAttributeMap()
                .get(OperationalAttributes.ENABLE_NAME).getValues().get(0)));

        String ldapUID = userTO.getUsername();
        assertNotNull(ldapUID);

        connObjectTO = restTemplate.getForObject(BASE_URL + "/resource/{resourceName}/read/{objectId}.json",
                ConnObjectTO.class, ldap.getName(), ldapUID);

        assertNotNull(connObjectTO);

        statusMod.setStatus(Status.REACTIVATE);
        statusMod.getExcludeResources().clear();
        statusMod.getExcludeResources().add(ldap.getName());
        // check also performLocally
        statusMod.setUpdateInternal(false);

        userTO = userService.setStatus(userTO.getId(), statusMod);

        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = restTemplate.getForObject(BASE_URL + "/resource/{resourceName}/read/{objectId}.json",
                ConnObjectTO.class, dbTable.getName(), dbTableUID);

        assertFalse(Boolean.parseBoolean(connObjectTO.getAttributeMap()
                .get(OperationalAttributes.ENABLE_NAME).getValues().get(0)));

        statusMod.getExcludeResources().clear();
        statusMod.getExcludeResources().add(dbTable.getName());
        // check also performLocally
        statusMod.setUpdateInternal(true);

        userTO = userService.setStatus(userTO.getId(), statusMod);

        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        connObjectTO = restTemplate.getForObject(BASE_URL + "/resource/{resourceName}/read/{objectId}.json",
                ConnObjectTO.class, dbTable.getName(), dbTableUID);

        assertTrue(Boolean.parseBoolean(connObjectTO.getAttributeMap().get(OperationalAttributes.ENABLE_NAME)
                .getValues().get(0)));
    }

    public void updateMultivalueAttribute() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.getResources().clear();
        userTO.getDerivedAttributes().clear();
        userTO.getVirtualAttributes().clear();

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);

        AttributeTO loginDate = userTO.getAttributeMap().get("loginDate");
        assertNotNull(loginDate);
        assertEquals(1, loginDate.getValues().size());

        UserMod userMod = new UserMod();

        AttributeMod loginDateMod = new AttributeMod();
        loginDateMod.addValueToBeAdded("2000-01-01");

        userMod.setId(userTO.getId());
        userMod.addAttributeToBeUpdated(loginDateMod);

        userTO = userService.update(userMod.getId(), userMod);
        assertNotNull(userTO);

        loginDate = userTO.getAttributeMap().get("loginDate");
        assertNotNull(loginDate);
        assertEquals(2, loginDate.getValues().size());
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void issue213() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.addResource("resource-testdb");

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
        assertEquals(1, userTO.getResources().size());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String username = jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", String.class,
                userTO.getUsername());

        assertEquals(userTO.getUsername(), username);

        UserMod userMod = new UserMod();

        userMod.setId(userTO.getId());
        userMod.addResourceToBeRemoved("resource-testdb");

        userTO = userService.update(userMod.getId(), userMod);

        assertTrue(userTO.getResources().isEmpty());

        jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
    }

    @Test
    public void issue234() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        String uid = getSampleEmail();
        UserTO userTO = getSampleTO(uid);
        userTO.addResource("resource-ldap");

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();

        userMod.setId(userTO.getId());
        userMod.setUsername("1" + userTO.getUsername());

        userTO = userService.update(userMod.getId(), userMod);

        assertNotNull(userTO);

        assertEquals("1" + uid, userTO.getUsername());
    }

    @Test
    public void issue270() throws NotFoundException, UnauthorizedRoleException, PropagationException,
            WorkflowException {
        // 1. create a new user without virtual attributes
        UserTO original = getSampleTO();
        // be sure to remove all virtual attributes
        original.setVirtualAttributes(new ArrayList<AttributeTO>());

        original = userCreateAndGet(original);

        assertNotNull(original);

        assertTrue(original.getVirtualAttributes().isEmpty());

        UserTO toBeUpdated = userService.read(original.getId());

        AttributeTO virtual = new AttributeTO();

        virtual.setSchema("virtualdata");
        virtual.addValue("virtualvalue");

        toBeUpdated.addVirtualAttribute(virtual);

        // 2. try to update by adding a resource, but no password: must fail
        UserMod userMod = AttributableOperations.diff(toBeUpdated, original);

        assertNotNull(userMod);

        toBeUpdated = userService.update(userMod.getId(), userMod);

        assertNotNull(toBeUpdated);

        assertFalse(toBeUpdated.getVirtualAttributes().isEmpty());
        assertNotNull(toBeUpdated.getVirtualAttributes().get(0));

        assertEquals(virtual.getSchema(), toBeUpdated.getVirtualAttributes().get(0).getSchema());
    }

    @Test
    public final void issue280() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerivedAttributes().clear();

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword("123password");
        userMod.addResourceToBeAdded("resource-testdb");

        userTO = userService.update(userMod.getId(), userMod);

        final List<PropagationTO> propagations = userTO.getPropagationTOs();

        assertNotNull(propagations);
        assertEquals(1, propagations.size());

        final PropagationTaskExecStatus status = propagations.get(0).getStatus();
        final String resource = propagations.get(0).getResourceName();

        assertNotNull(status);
        assertEquals("resource-testdb", resource);
        assertTrue(status.isSuccessful());
    }

    @Test
    @Ignore
    // Are propagations required after create?
    public void issue281() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerivedAttributes().clear();
        userTO.addResource("resource-csv");

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);

        final List<PropagationTO> propagations = userTO.getPropagationTOs();

        assertNotNull(propagations);
        assertEquals(1, propagations.size());

        final PropagationTaskExecStatus status = propagations.get(0).getStatus();
        final String resource = propagations.get(0).getResourceName();

        assertNotNull(status);
        assertEquals("resource-csv", resource);
        assertFalse(status.isSuccessful());
    }

    @Test
    public void issue288() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        UserTO userTO = getSampleTO();

        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("aLong");
        attributeTO.addValue("STRING");
        userTO.addAttribute(attributeTO);

        try {
            userCreateAndGet(userTO);
            fail("SyncopeClientCompositeErrorException expected");
        } catch (SyncopeClientCompositeErrorException sccee) {
            assertNotNull(sccee.getException(SyncopeClientExceptionType.InvalidValues));
        }
    }

    @Test
    public void roleAttrPropagation() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerivedAttributes().clear();
        userTO.getVirtualAttributes().clear();

        AttributeTO csvuserid = new AttributeTO();
        csvuserid.setSchema("csvuserid");
        userTO.addDerivedAttribute(csvuserid);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);

        userTO.addMembership(membershipTO);

        userTO.addResource("resource-csv");

        UserTO actual = userCreateAndGet(userTO);

        assertNotNull(actual);
        assertNotNull(actual.getDerivedAttributeMap().get("csvuserid"));

        ConnObjectTO connObjectTO = restTemplate.getForObject(BASE_URL
                + "/resource/{resourceName}/read/{objectId}.json", ConnObjectTO.class, "resource-csv", actual
                .getDerivedAttributeMap().get("csvuserid").getValues().get(0));

        assertNotNull(connObjectTO);

        assertEquals("sx-dx", connObjectTO.getAttributeMap().get("ROLE").getValues().get(0));
    }

    @Test
    public void membershipAttrPropagation() throws PropagationException, UnauthorizedRoleException,
            WorkflowException, NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerivedAttributes().clear();
        userTO.getVirtualAttributes().clear();

        AttributeTO csvuserid = new AttributeTO();
        csvuserid.setSchema("csvuserid");
        userTO.addDerivedAttribute(csvuserid);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(1L);

        AttributeTO mderived_sx = new AttributeTO();
        mderived_sx.setSchema("mderived_sx");
        mderived_sx.setValues(Collections.singletonList("sx"));
        membershipTO.addAttribute(mderived_sx);

        AttributeTO mderived_dx = new AttributeTO();
        mderived_dx.setSchema("mderived_dx");
        mderived_dx.setValues(Collections.singletonList("dx"));
        membershipTO.addAttribute(mderived_dx);

        AttributeTO mderiveddata = new AttributeTO();
        mderiveddata.setSchema("mderToBePropagated");
        membershipTO.addDerivedAttribute(mderiveddata);

        userTO.addMembership(membershipTO);

        userTO.addResource("resource-csv");

        UserTO actual = userCreateAndGet(userTO);

        assertNotNull(actual);
        assertNotNull(actual.getDerivedAttributeMap().get("csvuserid"));

        ConnObjectTO connObjectTO = restTemplate.getForObject(BASE_URL
                + "/resource/{resourceName}/read/{objectId}.json", ConnObjectTO.class, "resource-csv", actual
                .getDerivedAttributeMap().get("csvuserid").getValues().get(0));

        assertNotNull(connObjectTO);

        assertEquals("sx-dx", connObjectTO.getAttributeMap().get("MEMBERSHIP").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE16() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        UserTO userTO = getSampleTO();

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTO.addMembership(membershipTO);

        // 1. create user
        UserTO actual = userCreateAndGet(userTO);
        assertNotNull(actual);

        // 2. check for virtual attribute value
        assertEquals("virtualvalue", actual.getVirtualAttributeMap().get("virtualdata").getValues().get(0));

        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());

        AttributeMod virtualdata = new AttributeMod();
        virtualdata.setSchema("virtualdata");
        virtualdata.addValueToBeAdded("virtualupdated");

        userMod.addVirtualAttributeToBeRemoved("virtualdata");
        userMod.addVirtualAttributeToBeUpdated(virtualdata);

        // 3. update virtual attribute
        actual = userService.update(userMod.getId(), userMod);
        assertNotNull(actual);

        // 4. check for virtual attribute value
        actual = userService.read(actual.getId());
        assertNotNull(actual);
        assertEquals("virtualupdated", actual.getVirtualAttributeMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE108() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerivedAttributes().clear();
        userTO.getVirtualAttributes().clear();

        AttributeTO csvuserid = new AttributeTO();
        csvuserid.setSchema("csvuserid");
        userTO.addDerivedAttribute(csvuserid);

        MembershipTO memb12 = new MembershipTO();
        memb12.setRoleId(12L);

        userTO.addMembership(memb12);

        MembershipTO memb13 = new MembershipTO();
        memb13.setRoleId(13L);

        userTO.addMembership(memb13);

        userTO.addResource("resource-csv");

        UserTO actual = userCreateAndGet(userTO);

        assertNotNull(actual);
        assertEquals(2, actual.getMemberships().size());
        assertEquals(1, actual.getResources().size());

        ConnObjectTO connObjectTO = restTemplate.getForObject(BASE_URL
                + "/resource/{resourceName}/read/{objectId}.json", ConnObjectTO.class, "resource-csv", actual
                .getDerivedAttributeMap().get("csvuserid").getValues().get(0));

        assertNotNull(connObjectTO);

        // -----------------------------------
        // Remove the first membership: de-provisioning shouldn't happen
        // -----------------------------------
        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());

        userMod.addMembershipToBeRemoved(actual.getMemberships().get(0).getId());

        actual = userService.update(userMod.getId(), userMod);
        assertNotNull(actual);
        assertEquals(1, actual.getMemberships().size());

        connObjectTO = restTemplate.getForObject(BASE_URL + "/resource/{resourceName}/read/{objectId}.json",
                ConnObjectTO.class, "resource-csv", actual.getDerivedAttributeMap().get("csvuserid")
                        .getValues().get(0));

        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the resource assigned directly: de-provisioning shouldn't
        // happen
        // -----------------------------------
        userMod = new UserMod();
        userMod.setId(actual.getId());

        userMod.addResourceToBeRemoved(actual.getResources().iterator().next());

        actual = userService.update(userMod.getId(), userMod);
        assertNotNull(actual);
        assertEquals(1, actual.getMemberships().size());
        assertFalse(actual.getResources().isEmpty());

        connObjectTO = restTemplate.getForObject(BASE_URL + "/resource/{resourceName}/read/{objectId}.json",
                ConnObjectTO.class, "resource-csv", actual.getDerivedAttributeMap().get("csvuserid")
                        .getValues().get(0));

        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the first membership: de-provisioning should happen
        // -----------------------------------
        userMod = new UserMod();
        userMod.setId(actual.getId());

        userMod.addMembershipToBeRemoved(actual.getMemberships().get(0).getId());

        actual = userService.update(userMod.getId(), userMod);
        assertNotNull(actual);
        assertTrue(actual.getMemberships().isEmpty());
        assertTrue(actual.getResources().isEmpty());

        try {
            restTemplate.getForObject(BASE_URL + "/resource/{resourceName}/read/{objectId}.json",
                    ConnObjectTO.class, "resource-csv", actual.getDerivedAttributeMap().get("csvuserid")
                            .getValues().get(0));
            fail("SyncopeClientCompositeErrorException expected");
        } catch (SyncopeClientCompositeErrorException e) {
            assertNotNull(e.getException(SyncopeClientExceptionType.NotFound));
        }

        // -----------------------------------
    }

    @Test
    public void issueSYNCOPE111() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        UserTO userTO = getSampleTO();
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getDerivedAttributes().clear();
        userTO.getVirtualAttributes().clear();

        AttributeTO csvuserid = new AttributeTO();
        csvuserid.setSchema("csvuserid");
        userTO.addDerivedAttribute(csvuserid);

        MembershipTO memb12 = new MembershipTO();
        memb12.setRoleId(12L);

        AttributeTO postalAddress = new AttributeTO();
        postalAddress.setSchema("postalAddress");
        postalAddress.addValue("postalAddress");

        memb12.addAttribute(postalAddress);

        userTO.addMembership(memb12);

        MembershipTO memb13 = new MembershipTO();
        memb13.setRoleId(13L);

        userTO.addMembership(memb13);

        userTO.addResource("resource-ldap");

        UserTO actual = userCreateAndGet(userTO);
        assertNotNull(actual);
        assertEquals(2, actual.getMemberships().size());

        ConnObjectTO connObjectTO = restTemplate.getForObject(BASE_URL
                + "/resource/{resourceName}/read/{objectId}.json", ConnObjectTO.class, "resource-ldap",
                userTO.getUsername());

        assertNotNull(connObjectTO);

        postalAddress = connObjectTO.getAttributeMap().get("postalAddress");
        assertNotNull(postalAddress);
        assertEquals(1, postalAddress.getValues().size());
        assertEquals("postalAddress", postalAddress.getValues().get(0));

        AttributeTO title = connObjectTO.getAttributeMap().get("title");
        assertNotNull(title);
        assertEquals(2, title.getValues().size());
        assertTrue(title.getValues().contains("r12") && title.getValues().contains("r13"));

        // -----------------------------------
        // Remove the first membership and check for membership attr propagation
        // and role attr propagation
        // -----------------------------------
        UserMod userMod = new UserMod();
        userMod.setId(actual.getId());

        MembershipTO membershipTO = actual.getMemberships().get(0).getRoleId() == 12L
                ? actual.getMemberships().get(0)
                : actual.getMemberships().get(1);

        userMod.addMembershipToBeRemoved(membershipTO.getId());

        actual = userService.update(userMod.getId(), userMod);
        assertNotNull(actual);
        assertEquals(1, actual.getMemberships().size());

        connObjectTO = restTemplate.getForObject(BASE_URL + "/resource/{resourceName}/read/{objectId}.json",
                ConnObjectTO.class, "resource-ldap", userTO.getUsername());

        assertNotNull(connObjectTO);

        postalAddress = connObjectTO.getAttributeMap().get("postalAddress");
        assertTrue(postalAddress == null || postalAddress.getValues().isEmpty()
                || StringUtils.hasText(postalAddress.getValues().get(0)));

        title = connObjectTO.getAttributeMap().get("title");
        assertNotNull(title);
        assertEquals(1, title.getValues().size());
        assertTrue(title.getValues().contains("r13"));
        // -----------------------------------
    }

    @Test
    public void issueSYNCOPE185() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        // 1. create user with LDAP resource, succesfully propagated
        UserTO userTO = getSampleTO();
        userTO.getVirtualAttributes().clear();
        userTO.addResource("resource-ldap");

        userTO = userCreateAndGet(userTO);
        assertNotNull(userTO);
        // assertFalse(userTO.getPropagationTOs().isEmpty());
        // assertEquals("resource-ldap",
        // userTO.getPropagationTOs().get(0).getResourceName());
        // assertEquals(PropagationTaskExecStatus.SUCCESS,
        // userTO.getPropagationTOs().get(0).getStatus());

        // 2. delete this user
        userService.delete(userTO.getId());

        // 3. try (and fail) to find this user on the external LDAP resource
        try {
            restTemplate.getForObject(BASE_URL + "resource/{resourceName}/read/{objectId}.json",
                    ConnObjectTO.class, "resource-ldap", userTO.getUsername());
            fail("This entry should not be present on this resource");
        } catch (SyncopeClientCompositeErrorException sccee) {
            assertNotNull(sccee.getException(SyncopeClientExceptionType.NotFound));
        }
    }

    @Test
    @Ignore
    public void issueSYNCOPE51() throws PropagationException, UnauthorizedRoleException, WorkflowException,
            NotFoundException {
        ConfigurationTO defaultConfigurationTO = restTemplate.getForObject(BASE_URL
                + "configuration/read/{key}.json", ConfigurationTO.class, "password.cipher.algorithm");

        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("password.cipher.algorithm");
        // FIXME MD5 Support
        configurationTO.setValue("MD5");

        WebClient wc = createWebClient("configuration/update.json");
        ConfigurationTO newConfTO = wc.post(configurationTO, ConfigurationTO.class);

        try {
            assertEquals(configurationTO, newConfTO);

            UserTO userTO = getSampleTO();
            userTO.setPassword("password");

            try {
                userCreateAndGet(userTO);
                fail();
            } catch (SyncopeClientCompositeErrorException e) {
                assertTrue(e.getException(SyncopeClientExceptionType.NotFound).getElements().iterator()
                        .next().contains("MD5"));
            }
        } finally {
            ConfigurationTO oldConfTO = wc.post(defaultConfigurationTO, ConfigurationTO.class);
            assertEquals(defaultConfigurationTO, oldConfTO);
        }
    }
}
