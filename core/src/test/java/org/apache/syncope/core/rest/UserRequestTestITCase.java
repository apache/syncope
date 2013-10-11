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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.List;

import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;

import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.AttributeCond;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.types.UserRequestType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class UserRequestTestITCase extends AbstractTest {

    private Response createUserRequest(final UserRequestService service, final UserRequestTO userRequestTO) {
        Response response = service.create(userRequestTO);
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
        }
        return response;
    }

    @Test
    public void create() throws InvalidSearchConditionException {
        // 1. set create request not allowed
        ConfigurationTO configurationTO = new ConfigurationTO();
        configurationTO.setKey("createRequest.allowed");
        configurationTO.setValue("false");

        Response response = configurationService.create(configurationTO);
        assertNotNull(response);
        assertEquals(HttpStatus.SC_CREATED, response.getStatus());
        configurationTO =
                adminClient.getObject(response.getLocation(), ConfigurationService.class, ConfigurationTO.class);
        assertNotNull(configurationTO);

        UserTO userTO = UserTestITCase.getUniqueSampleTO("selfcreate@syncope.apache.org");

        // 2. get unauthorized when trying to request user create
        UserRequestService anonymousUserRequestService =
                clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(UserRequestService.class);
        try {
            createUserRequest(anonymousUserRequestService, new UserRequestTO(userTO));
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertNotNull(e.getException(SyncopeClientExceptionType.UnauthorizedRole));
        }

        // 3. set create request allowed
        configurationTO.setValue("true");

        response = configurationService.create(configurationTO);
        assertNotNull(response);
        assertEquals(HttpStatus.SC_CREATED, response.getStatus());
        configurationTO =
                adminClient.getObject(response.getLocation(), ConfigurationService.class, ConfigurationTO.class);
        assertNotNull(configurationTO);

        // 4. as anonymous, request user create works
        createUserRequest(anonymousUserRequestService, new UserRequestTO(userTO));

        // 5. try to find user
        AttributeCond attrCond = new AttributeCond(AttributeCond.Type.EQ);
        attrCond.setSchema("userId");
        attrCond.setExpression(userTO.getUsername());

        final List<UserTO> matchingUsers = userService.search(NodeCond.getLeafCond(attrCond));
        assertTrue(matchingUsers.isEmpty());

        // 6. actually create user
        userTO = createUser(userTO);
        assertNotNull(userTO);
    }

    @Test
    public void update() {
        // 1. create an user (as admin)
        UserTO userTO = UserTestITCase.getUniqueSampleTO("selfupdate@syncope.apache.org");
        String initialPassword = userTO.getPassword();

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());
        userMod.setPassword(initialPassword);

        // 2. try to request user update as admin: failure
        try {
            createUserRequest(userRequestService, new UserRequestTO(userMod));
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertNotNull(e.getException(SyncopeClientExceptionType.UnauthorizedRole));
        }

        // 3. auth as user just created
        UserRequestService userRequestService2 =
                clientFactory.create(userTO.getUsername(), initialPassword).getService(UserRequestService.class);

        // 4. update with same password: not matching password policy
        try {
            createUserRequest(userRequestService2, new UserRequestTO(userMod));
            fail();
        } catch (SyncopeClientCompositeException scce) {
            assertNotNull(scce.getException(SyncopeClientExceptionType.InvalidSyncopeUser));
        }

        // 5. now request user update works
        userMod.setPassword("new" + initialPassword);
        createUserRequest(userRequestService2, new UserRequestTO(userMod));

        // 6. user password has not changed yet
        UserService userService1 =
                clientFactory.create(userTO.getUsername(), userMod.getPassword()).getService(UserService.class);
        try {
            userService1.readSelf();
            fail("Credentials are not updated yet, thus request should raise AccessControlException");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 7. actually update user
        userTO = userService.update(userMod.getId(), userMod);
        assertNotNull(userTO);

        // 8. user password has now changed
        UserService userService2 =
                clientFactory.create(userTO.getUsername(), userMod.getPassword()).getService(UserService.class);
        try {
            UserTO user = userService2.readSelf();
            assertNotNull(user);
        } catch (AccessControlException e) {
            fail("Credentials should be valid and not cause AccessControlException");
        }
    }

    @Test
    public void delete() {
        // 1. create an user (as admin)
        UserTO userTO = UserTestITCase.getUniqueSampleTO("selfdelete@syncope.apache.org");
        String initialPassword = userTO.getPassword();

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 2. try to request user delete as admin: failure
        try {
            createUserRequest(userRequestService, new UserRequestTO(userTO.getId()));
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertNotNull(e.getException(SyncopeClientExceptionType.UnauthorizedRole));
        }

        // 3. auth as user just created
        UserRequestService userRequestService2 =
                clientFactory.create(userTO.getUsername(), initialPassword).getService(UserRequestService.class);

        // 4. now request user delete works
        createUserRequest(userRequestService2, new UserRequestTO(userTO.getId()));

        // 5. user still exists
        UserTO actual = userService.read(userTO.getId());
        assertNotNull(actual);

        // 6. actually delete user
        userService.delete(userTO.getId());

        // 7. user does not exist any more
        try {
            userService.read(userTO.getId());
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void execute() {
        final String USERNAME = "ex.create@syncope.apache.org";
        UserTO userTO = UserTestITCase.getUniqueSampleTO(USERNAME);
        final String initialPassword = userTO.getPassword();

        UserRequestService selfservice =
                clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(UserRequestService.class);
        Response response = createUserRequest(selfservice, new UserRequestTO(userTO));

        UserRequestTO userRequest =
                adminClient.getObject(response.getLocation(), UserRequestService.class, UserRequestTO.class);
        assertNotNull(userRequest);
        assertEquals(UserRequestType.CREATE, userRequest.getType());
        assertTrue(userRequest.getUsername().endsWith(USERNAME));
        assertNotNull(userRequest.getCreationDate());
        assertNull(userRequest.getClaimDate());
        assertNull(userRequest.getExecutionDate());

        try {
            userService.read(userTO.getUsername());
            fail();
        } catch (Exception ignore) {
            assertNotNull(ignore);
        }

        assertFalse(userRequestService.read(userRequest.getId()).isExecuted());
        userRequest = userRequestService.claim(userRequest.getId());
        assertEquals(ADMIN_UNAME, userRequest.getOwner());
        assertTrue(userRequest.getUsername().endsWith(USERNAME));
        assertNotNull(userRequest.getCreationDate());
        assertNotNull(userRequest.getClaimDate());
        assertNull(userRequest.getExecutionDate());

        assertNotNull(userRequestService.executeCreate(userRequest.getId(), userTO));

        userRequest = userRequestService.read(userRequest.getId());
        assertTrue(userRequest.isExecuted());
        assertTrue(userRequest.getUsername().endsWith(USERNAME));
        assertNotNull(userRequest.getCreationDate());
        assertNotNull(userRequest.getClaimDate());
        assertNotNull(userRequest.getExecutionDate());

        for (UserRequestTO userRequestTO : userRequestService.list()) {
            assertFalse(userRequestTO.isExecuted());
        }

        userTO = userService.read(userTO.getUsername());
        assertNotNull(userTO);

        UserMod userMod = new UserMod();
        userMod.setId(userTO.getId());

        selfservice = clientFactory.create(userTO.getUsername(), initialPassword).getService(UserRequestService.class);

        userMod.setPassword("new" + initialPassword);
        response = createUserRequest(selfservice, new UserRequestTO(userMod));

        userRequest = adminClient.getObject(response.getLocation(), UserRequestService.class, UserRequestTO.class);
        assertNotNull(userRequest);
        assertEquals(UserRequestType.UPDATE, userRequest.getType());
        assertTrue(userRequest.getUsername().endsWith(USERNAME));

        final String newpwd = "new" + initialPassword + "!";

        UserMod furtherChanges = new UserMod();
        furtherChanges.setId(userMod.getId());
        furtherChanges.setPassword(newpwd);

        assertFalse(userRequestService.read(userRequest.getId()).isExecuted());
        userRequest = userRequestService.claim(userRequest.getId());
        assertEquals("admin", userRequest.getOwner());
        assertNotNull(userRequestService.executeUpdate(userRequest.getId(), furtherChanges));
        assertTrue(userRequestService.read(userRequest.getId()).isExecuted());

        for (UserRequestTO userRequestTO : userRequestService.list()) {
            assertFalse(userRequestTO.isExecuted());
        }

        assertNotNull(userService.read(userTO.getUsername()));

        try {
            clientFactory.create(userTO.getUsername(), "new" + initialPassword).
                    getService(UserService.class).readSelf();
            fail("Credentials are not updated yet, thus request should raise AccessControlException");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        assertNotNull(clientFactory.create(userTO.getUsername(), newpwd).getService(UserService.class).readSelf());

        selfservice =
                clientFactory.create(userTO.getUsername(), newpwd).getService(UserRequestService.class);
        response = createUserRequest(selfservice, new UserRequestTO(userTO.getId()));

        userRequest = adminClient.getObject(response.getLocation(), UserRequestService.class, UserRequestTO.class);
        assertNotNull(userRequest);
        assertEquals(UserRequestType.DELETE, userRequest.getType());
        assertTrue(userRequest.getUsername().endsWith(USERNAME));

        assertFalse(userRequestService.read(userRequest.getId()).isExecuted());
        userRequest = userRequestService.claim(userRequest.getId());
        assertEquals("admin", userRequest.getOwner());
        userRequestService.executeDelete(userRequest.getId());
        assertTrue(userRequestService.read(userRequest.getId()).isExecuted());

        for (UserRequestTO userRequestTO : userRequestService.list()) {
            assertFalse(userRequestTO.isExecuted());
        }

        try {
            userService.read(userTO.getUsername());
            fail();
        } catch (Exception ignore) {
            assertNotNull(ignore);
        }

        assertEquals(3, userRequestService.listByUsername(userTO.getUsername()).size());
    }

    @Test(expected = SyncopeClientCompositeException.class)
    public void executeNoClaim() {
        UserTO userTO = UserTestITCase.getUniqueSampleTO("reqnoclaim@syncope.apache.org");

        final UserRequestService selfservice =
                clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(UserRequestService.class);

        final UserRequestTO userRequest = adminClient.getObject(
                createUserRequest(selfservice, new UserRequestTO(userTO)).getLocation(),
                UserRequestService.class, UserRequestTO.class);
        assertNotNull(userRequest);

        userRequestService.executeCreate(userRequest.getId(), userTO);
    }
}
