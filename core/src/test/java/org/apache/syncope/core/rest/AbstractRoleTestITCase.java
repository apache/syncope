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

import java.util.List;
import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.apache.syncope.NotFoundException;
import org.apache.syncope.controller.RoleService;
import org.apache.syncope.controller.UnauthorizedRoleException;
import org.apache.syncope.mod.AttributeMod;
import org.apache.syncope.mod.RoleMod;
import org.apache.syncope.to.AttributeTO;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.to.UserTO;
import org.junit.Test;

public abstract class AbstractRoleTestITCase extends AbstractTest {

    protected RoleService roleService;

    @Test
    public void crud() throws NotFoundException, UnauthorizedRoleException {
        String parentRoleName = "testParentRole-" + UUID.randomUUID().toString().substring(0, 12);
        String childRoleName = "testChildRole-" + UUID.randomUUID().toString().substring(0, 12);
        long parentRoleId;
        long childRoleId;

        parentRoleId = create(parentRoleName, null);
        childRoleId = create(childRoleName, parentRoleId);
        try {
            read(parentRoleId);
            update(childRoleId);
            parent(childRoleId, parentRoleId);
            children(childRoleId, parentRoleId);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            delete(childRoleId);
            delete(parentRoleId);
        }
    }

    @Test
    public void createWithException() throws UnauthorizedRoleException {
        AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema("attr1");
        attributeTO.addValue("value1");

        RoleTO newRoleTO = new RoleTO();
        newRoleTO.addAttribute(attributeTO);

        try {
            Response response = roleService.create(newRoleTO);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        } catch (BadRequestException e) {
        }
    }

    public long create(String roleName, Long parentRoleId) throws UnauthorizedRoleException {
        RoleTO roleTO = new RoleTO();
        roleTO.setName(roleName);
        if (parentRoleId != null) {
            roleTO.setParent(parentRoleId.longValue());
        }

        // verify inheritance password and account policies
        roleTO.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        roleTO.setAccountPolicy(6L);

        roleTO.setInheritPasswordPolicy(false);
        // inherited so setter execution should be ignored
        roleTO.setPasswordPolicy(2L);

        AttributeTO icon = new AttributeTO();
        icon.setSchema("icon");
        icon.addValue("anIcon");

        roleTO.addAttribute(icon);

        Response response = roleService.create(roleTO);

        assertNotNull(response);
        assertEquals(org.apache.http.HttpStatus.SC_CREATED, response.getStatus());

        WebClient webClient = WebClient.fromClient(WebClient.client(roleService));
        webClient.to(response.getLocation().toString(), false);
        RoleTO actual = webClient.get(RoleTO.class);

        roleTO.setId(actual.getId());

        roleTO.setPasswordPolicy(2L);

        assertEquals(roleTO, actual);

        return actual.getId();
    }

    public void delete(long roleId) throws UnauthorizedRoleException {

        try {
            roleService.delete(0L);
            fail("You should not be able to delete an unexsisting role.");
        } catch (NotFoundException e) {
        }

        Response response;
        try {
            response = roleService.delete(roleId);
            assertNotNull(response);
            assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus());
        } catch (NotFoundException e1) {
            fail(e1.getMessage());
        }

        try {
            roleService.read(roleId);
            fail("Role should be removed and can not be read afterwards.");
        } catch (NotFoundException e) {
        }
    }

    @Test
    public void list() {
        List<RoleTO> roleTOs = roleService.list();

        assertNotNull(roleTOs);
        assertTrue(roleTOs.size() >= 8);
        for (RoleTO roleTO : roleTOs) {
            assertNotNull(roleTO);
        }
    }

    public void parent(long childRoleId, long parentRoleId) throws NotFoundException,
            UnauthorizedRoleException {

        RoleTO parentRoleTO = roleService.parent(childRoleId);

        assertNotNull(parentRoleTO);
        assertEquals(parentRoleId, parentRoleTO.getId());
    }

    public void children(long childRoleId, long parentRoleId) throws NotFoundException,
            UnauthorizedRoleException {

        List<RoleTO> children = roleService.children(parentRoleId);

        assertNotNull(children);
        assertEquals(1, children.size());
        assertEquals(childRoleId, children.get(0).getId());
    }

    @Test
    public void testChildren() throws Exception {
        List<RoleTO> children = roleService.children(1L);

        assertNotNull(children);
        assertTrue(children.size() > 0);
    }

    public void read(long roleId) throws NotFoundException, UnauthorizedRoleException {
        try {
            roleService.read(0L);
        } catch (NotFoundException e) {
        }

        RoleTO roleTO = roleService.read(roleId);

        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttributes());
        assertFalse(roleTO.getAttributes().isEmpty());
        assertEquals(roleId, roleTO.getId());
    }

    @Test
    public void selfRead() throws NotFoundException, UnauthorizedRoleException {
        WebClient wc = restClientFactory.createWebClient();
        UserTO userTO = wc.path("user/{userId}.json", 1).get(UserTO.class);
        assertNotNull(userTO);

        assertTrue(userTO.getMembershipMap().containsKey(1L));
        assertFalse(userTO.getMembershipMap().containsKey(3L));

        restClientFactory.setUsername("user1");
        RoleService user1RoleService = restClientFactory.create(RoleService.class);

        try {
            user1RoleService.read(3L);
            fail();
        } catch (UnauthorizedRoleException e) {
            assertNotNull(e);
        }

        RoleTO roleTO = user1RoleService.read(1L);

        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttributes());
        assertFalse(roleTO.getAttributes().isEmpty());
    }

    public void update(long roleId) throws NotFoundException, UnauthorizedRoleException {

        AttributeMod attributeMod = new AttributeMod();
        attributeMod.setSchema("show");
        attributeMod.addValueToBeAdded("FALSE");

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleId);
        String newRoleName = "finalRole-" + UUID.randomUUID().toString().substring(0, 8);
        roleMod.setName(newRoleName);
        roleMod.addAttributeToBeUpdated(attributeMod);

        // change password policy inheritance
        roleMod.setInheritPasswordPolicy(Boolean.FALSE);

        RoleTO roleTO = roleService.update(roleId, roleMod);

        assertEquals(newRoleName, roleTO.getName());
        assertEquals(2, roleTO.getAttributes().size());

        // changes ignored because not requested (null ReferenceMod)
        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        // password policy null because not inherited
        assertFalse(roleTO.isInheritPasswordPolicy());
    }

    @Test
    public void updateRemovingVirAttribute() throws UnauthorizedRoleException, NotFoundException {
        String roleName = "withvirtual-" + UUID.randomUUID().toString().substring(0, 8);
        RoleTO roleTO = new RoleTO();
        roleTO.setName(roleName);
        roleTO.setParent(8L);

        final AttributeTO rvirtualdata = new AttributeTO();
        rvirtualdata.setSchema("rvirtualdata");
        roleTO.addVirtualAttribute(rvirtualdata);

        Response response = roleService.create(roleTO);
        try {
            assertNotNull(response);
            assertEquals(org.apache.http.HttpStatus.SC_CREATED, response.getStatus());

            WebClient webClient = WebClient.fromClient(WebClient.client(roleService));
            webClient.to(response.getLocation().toString(), false);
            roleTO = webClient.get(RoleTO.class);

            final RoleMod roleMod = new RoleMod();
            roleMod.setId(roleTO.getId());
            roleMod.addVirtualAttributeToBeRemoved("rvirtualdata");

            roleTO = roleService.update(roleTO.getId(), roleMod);
            assertNotNull(roleTO);
            assertTrue(roleTO.getVirtualAttributes().isEmpty());
        } finally {
            response = roleService.delete(roleTO.getId());
            assertNotNull(response);
            assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus());
        }
    }

    @Test
    public void updateRemovingDerAttribute() throws UnauthorizedRoleException, NotFoundException {
        String roleName = "withderived-" + UUID.randomUUID().toString().substring(0, 8);
        RoleTO roleTO = new RoleTO();
        roleTO.setName(roleName);
        roleTO.setParent(8L);

        final AttributeTO deriveddata = new AttributeTO();
        deriveddata.setSchema("rderivedschema");
        roleTO.addDerivedAttribute(deriveddata);

        Response response = roleService.create(roleTO);
        try {
            assertNotNull(response);
            assertEquals(org.apache.http.HttpStatus.SC_CREATED, response.getStatus());

            WebClient webClient = WebClient.fromClient(WebClient.client(roleService));
            webClient.to(response.getLocation().toString(), false);
            roleTO = webClient.get(RoleTO.class);

            assertNotNull(roleTO);
            assertEquals(1, roleTO.getDerivedAttributes().size());

            final RoleMod roleMod = new RoleMod();
            roleMod.setId(roleTO.getId());
            roleMod.addDerivedAttributeToBeRemoved("rderivedschema");

            roleTO = roleService.update(roleTO.getId(), roleMod);
            assertNotNull(roleTO);
            assertTrue(roleTO.getDerivedAttributes().isEmpty());
        } finally {
            response = roleService.delete(roleTO.getId());
            assertNotNull(response);
            assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus());
        }
    }

    @Test
    public void updateAsRoleOwner() throws NotFoundException, UnauthorizedRoleException {
        // 1. read role as admin
        RoleTO roleTO = roleService.read(7L);

        // 2. prepare update
        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setName("Managing Director");

        // 3. try to update as user3, not owner of role 7 - fail
        RoleService user2RoleService = createServiceInstance(RoleService.class, "user2", roleService);

        try {
            roleTO = user2RoleService.update(roleTO.getId(), roleMod);
            fail();
        } catch (UnauthorizedRoleException e) {
            assertNotNull(e);
        }

        // 4. update as user5, owner of role 7 because owner of role 6 with
        // inheritance - success
        RoleService user5RoleService = createServiceInstance(RoleService.class, "user5", roleService);

        roleTO = user5RoleService.update(roleTO.getId(), roleMod);
        assertEquals("Managing Director", roleTO.getName());
    }

    /**
     * Role rename used to fail in case of parent null.
     *
     * http://code.google.com/p/syncope/issues/detail?id=178
     *
     * @throws UnauthorizedRoleException
     * @throws NotFoundException
     */
    @Test
    public void issue178() throws UnauthorizedRoleException, NotFoundException {
        RoleTO roleTO = new RoleTO();
        String roleName = "torename-" + UUID.randomUUID().toString().substring(0, 8);
        roleTO.setName(roleName);

        Response response = roleService.create(roleTO);
        assertNotNull(response);
        assertEquals(org.apache.http.HttpStatus.SC_CREATED, response.getStatus());

        WebClient webClient = WebClient.fromClient(WebClient.client(roleService));
        webClient.to(response.getLocation().toString(), false);
        RoleTO actual = webClient.get(RoleTO.class);

        assertNotNull(actual);
        assertEquals(roleName, actual.getName());
        assertEquals(0L, actual.getParent());

        RoleMod roleMod = new RoleMod();
        roleMod.setId(actual.getId());
        roleMod.setName("renamed");

        actual = roleService.update(actual.getId(), roleMod);

        assertNotNull(actual);
        assertEquals("renamed", actual.getName());
        assertEquals(0L, actual.getParent());

        response = roleService.delete(actual.getId());
        assertNotNull(response);
        assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    public void issue228() throws UnauthorizedRoleException, NotFoundException {
        RoleTO roleTO = new RoleTO();
        String roleName = "issue228-" + UUID.randomUUID().toString().substring(0, 12);
        roleTO.setName(roleName);
        roleTO.setParent(8L);
        roleTO.setInheritAccountPolicy(false);
        roleTO.setAccountPolicy(6L);
        roleTO.setInheritPasswordPolicy(true);
        roleTO.setPasswordPolicy(2L);

        AttributeTO icon = new AttributeTO();
        icon.setSchema("icon");
        icon.addValue("anIcon");
        roleTO.addAttribute(icon);

        roleTO.addEntitlement("USER_READ");

        Response response = roleService.create(roleTO);
        assertNotNull(response);
        assertEquals(org.apache.http.HttpStatus.SC_CREATED, response.getStatus());

        WebClient webClient = WebClient.fromClient(WebClient.client(roleService));
        webClient.to(response.getLocation().toString(), false);
        roleTO = webClient.get(RoleTO.class);

        assertNotNull(roleTO);
        assertNotNull(roleTO.getEntitlements());
        assertFalse(roleTO.getEntitlements().isEmpty());

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setInheritDerivedAttributes(Boolean.TRUE);
        roleMod.addEntitlementsToBeAdded("SCHEMA_READ");

        roleTO = roleService.update(roleTO.getId(), roleMod);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getEntitlements());
        assertEquals(2, roleTO.getEntitlements().size());

        roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.addEntitlementsToBeRemoved("USER_READ");
        roleMod.addEntitlementsToBeRemoved("SCHEMA_READ");

        roleTO = roleService.update(roleTO.getId(), roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().isEmpty());

        response = roleService.delete(roleTO.getId());
        assertNotNull(response);
        assertEquals(org.apache.http.HttpStatus.SC_OK, response.getStatus());
    }
}
