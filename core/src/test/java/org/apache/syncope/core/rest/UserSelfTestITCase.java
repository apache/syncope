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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.Map;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.UserSelfService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class UserSelfTestITCase extends AbstractTest {

    @Test
    public void selfRegistrationAllowed() {
        assertTrue(clientFactory.createAnonymous().isSelfRegistrationAllowed());
    }

    @Test
    public void create() {
        // 1. self-registration as admin: failure
        try {
            userSelfService.create(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"));
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 2. self-registration as anonymous: works
        SyncopeClient anonClient = clientFactory.createAnonymous();
        UserTO self = anonClient.getService(UserSelfService.class).
                create(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org")).
                readEntity(UserTO.class);
        assertNotNull(self);
        assertEquals("createApproval", self.getStatus());
    }

    @Test
    public void createAndApprove() {
        // self-create user with membership: goes 'createApproval' with resources and membership but no propagation
        UserTO userTO = UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org");
        MembershipTO membership = new MembershipTO();
        membership.setRoleId(3L);
        userTO.getMemberships().add(membership);
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        SyncopeClient anonClient = clientFactory.createAnonymous();
        userTO = anonClient.getService(UserSelfService.class).
                create(userTO).
                readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());
        assertFalse(userTO.getMemberships().isEmpty());
        assertFalse(userTO.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, userTO.getId());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // now approve and verify that propagation has happened
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getId());
        form = userWorkflowService.claimForm(form.getTaskId());
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.setProperties(props.values());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, userTO.getId()));
    }

    @Test
    public void read() {
        UserService userService2 = clientFactory.create("rossini", ADMIN_PWD).getService(UserService.class);

        try {
            userService2.read(1L);
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        UserSelfService userSelfService2 = clientFactory.create("rossini", ADMIN_PWD).getService(UserSelfService.class);
        UserTO userTO = userSelfService2.read();
        assertEquals("rossini", userTO.getUsername());
    }

    @Test
    public void updateWithoutApproval() {
        // 1. create user as admin
        UserTO created = createUser(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"));
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username) - works
        UserMod userMod = new UserMod();
        userMod.setUsername(created.getUsername() + "XX");

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(created.getId(), userMod).
                readEntity(UserTO.class);
        assertNotNull(updated);
        assertEquals("active", updated.getStatus());
        assertTrue(updated.getUsername().endsWith("XX"));
    }

    @Test
    public void updateWitApproval() {
        // 1. create user as admin
        UserTO created = createUser(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"));
        assertNotNull(created);
        assertFalse(created.getUsername().endsWith("XX"));

        // 2. self-update (username + memberships + resource) - works but needs approval
        MembershipMod membershipMod = new MembershipMod();
        membershipMod.setRole(7L);
        AttributeMod testAttributeMod = new AttributeMod();
        testAttributeMod.setSchema("testAttribute");
        testAttributeMod.getValuesToBeAdded().add("a value");
        membershipMod.getAttrsToUpdate().add(testAttributeMod);

        UserMod userMod = new UserMod();
        userMod.setUsername(created.getUsername() + "XX");
        userMod.getMembershipsToAdd().add(membershipMod);
        userMod.getResourcesToAdd().add(RESOURCE_NAME_TESTDB);
        userMod.setPassword("newPassword123");
        StatusMod statusMod = new StatusMod();
        statusMod.setOnSyncope(false);
        statusMod.getResourceNames().add(RESOURCE_NAME_TESTDB);
        userMod.setPwdPropRequest(statusMod);

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO updated = authClient.getService(UserSelfService.class).update(created.getId(), userMod).
                readEntity(UserTO.class);
        assertNotNull(updated);
        assertEquals("updateApproval", updated.getStatus());
        assertFalse(updated.getUsername().endsWith("XX"));
        assertTrue(updated.getMemberships().isEmpty());

        // no propagation happened
        assertTrue(updated.getResources().isEmpty());
        try {
            resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, updated.getId());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        // 3. approve self-update as admin
        WorkflowFormTO form = userWorkflowService.getFormForUser(updated.getId());
        form = userWorkflowService.claimForm(form.getTaskId());
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.setProperties(props.values());
        updated = userWorkflowService.submitForm(form);
        assertNotNull(updated);
        assertEquals("active", updated.getStatus());
        assertTrue(updated.getUsername().endsWith("XX"));
        assertEquals(1, updated.getMemberships().size());

        // check that propagation also happened
        assertTrue(updated.getResources().contains(RESOURCE_NAME_TESTDB));
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_TESTDB, AttributableType.USER, updated.getId()));
    }

    @Test
    public void delete() {
        UserTO created = createUser(UserTestITCase.getUniqueSampleTO("anonymous@syncope.apache.org"));
        assertNotNull(created);

        SyncopeClient authClient = clientFactory.create(created.getUsername(), "password123");
        UserTO deleted = authClient.getService(UserSelfService.class).delete().readEntity(UserTO.class);
        assertNotNull(deleted);
        assertEquals("deleteApproval", deleted.getStatus());
    }

    @Test
    public void issueSYNCOPE373() {
        UserTO userTO = userSelfService.read();
        assertEquals(ADMIN_UNAME, userTO.getUsername());
    }

}
