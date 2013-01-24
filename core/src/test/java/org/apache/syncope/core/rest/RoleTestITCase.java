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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@FixMethodOrder(MethodSorters.JVM)
public class RoleTestITCase extends AbstractTest {

    // Enable running test more than once with parameters
    public RoleTestITCase(String contentType) {
        super(contentType);
    }

    private RoleTO buildBasicRoleTO(final String name) {
        RoleTO roleTO = new RoleTO();
        roleTO.setName(name + getUUIDString());
        roleTO.setParent(8L);
        return roleTO;
    }

    private RoleTO buildRoleTO(final String name) {
        RoleTO roleTO = buildBasicRoleTO(name);

        // verify inheritance password and account policies
        roleTO.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        roleTO.setAccountPolicy(6L);

        roleTO.setInheritPasswordPolicy(true);
        // inherited so setter execution should be ignored
        roleTO.setPasswordPolicy(2L);

        roleTO.addAttribute(attributeTO("icon", "anIcon"));

        roleTO.addResource("resource-ldap");
        return roleTO;
    }

    @Test
    public void createWithException() {
        RoleTO newRoleTO = new RoleTO();
        newRoleTO.addAttribute(attributeTO("attr1", "value1"));

        Throwable t = null;
        try {
            roleService.create(newRoleTO);
            fail();
        } catch (SyncopeClientCompositeErrorException sccee) {
            t = sccee.getException(SyncopeClientExceptionType.InvalidSyncopeRole);
        }
        assertNotNull(t);
    }

    @Test
    public void create() {
        RoleTO roleTO = buildRoleTO("lastRole");
        roleTO.addVirtualAttribute(attributeTO("rvirtualdata", "rvirtualvalue"));
        roleTO.setRoleOwner(8L);

        roleTO = roleService.create(roleTO);
        assertNotNull(roleTO);

        assertNotNull(roleTO.getVirtualAttributeMap());
        assertNotNull(roleTO.getVirtualAttributeMap().get("rvirtualdata").getValues());
        assertFalse(roleTO.getVirtualAttributeMap().get("rvirtualdata").getValues().isEmpty());
        assertEquals("rvirtualvalue", roleTO.getVirtualAttributeMap().get("rvirtualdata").getValues().get(0));

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(4L, (long) roleTO.getPasswordPolicy());

        assertTrue(roleTO.getResources().contains("resource-ldap"));

        ConnObjectTO connObjectTO = resourceService.getConnector("resource-ldap", AttributableType.ROLE,
                roleTO.getName());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getAttributeMap().get("owner"));
    }

    @Test
    public void createWithPasswordPolicy() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("roleWithPassword" + getUUIDString());
        roleTO.setParent(8L);
        roleTO.setPasswordPolicy(4L);

        RoleTO actual = roleService.create(roleTO);
        assertNotNull(actual);

        actual = roleService.read(actual.getId());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void delete() {
        try {
            roleService.delete(0L);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        RoleTO roleTO = new RoleTO();
        roleTO.setName("toBeDeleted");
        roleTO.setParent(8L);

        roleTO.addResource("resource-ldap");

        roleTO = roleService.create(roleTO);
        assertNotNull(roleTO);

        RoleTO deletedRole = roleService.delete(roleTO.getId());
        assertNotNull(deletedRole);

        try {
            roleService.read(deletedRole.getId());
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
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

    @Test
    public void parent() {
        RoleTO roleTO = roleService.parent(7L);

        assertNotNull(roleTO);
        assertEquals(roleTO.getId(), 6L);
    }

    @Test
    public void read() {
        RoleTO roleTO = roleService.read(1L);

        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttributes());
        assertFalse(roleTO.getAttributes().isEmpty());
    }

    @Test
    public void selfRead() {
        UserTO userTO = userService.read(1L);
        assertNotNull(userTO);

        assertTrue(userTO.getMembershipMap().containsKey(1L));
        assertFalse(userTO.getMembershipMap().containsKey(3L));

        super.setupRestTemplate("user1", "password");

        SyncopeClientException exception = null;
        try {
            roleService.selfRead(3L);
            fail();
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        RoleTO roleTO = roleService.selfRead(1L);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttributes());
        assertFalse(roleTO.getAttributes().isEmpty());

        // restore admin authentication
        super.resetRestTemplate();
    }

    @Test
    public void update() {
        RoleTO roleTO = buildRoleTO("latestRole");
        roleTO = roleService.create(roleTO);

        assertEquals(1, roleTO.getAttributes().size());

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(4L, (long) roleTO.getPasswordPolicy());

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        String modName = "finalRole" + getUUIDString();
        roleMod.setName(modName);
        roleMod.addAttributeToBeUpdated(attributeMod("show", "FALSE"));

        // change password policy inheritance
        roleMod.setInheritPasswordPolicy(Boolean.FALSE);

        roleTO = roleService.update(roleMod.getId(), roleMod);

        assertEquals(modName, roleTO.getName());
        assertEquals(2, roleTO.getAttributes().size());

        // changes ignored because not requested (null ReferenceMod)
        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        // password policy null because not inherited
        assertNull(roleTO.getPasswordPolicy());
    }

    @Test
    public void updateRemovingVirAttribute() {
        RoleTO roleTO = buildBasicRoleTO("withvirtual");
        roleTO.addVirtualAttribute(attributeTO("rvirtualdata", null));

        roleTO = roleService.create(roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getVirtualAttributes().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.addVirtualAttributeToBeRemoved("rvirtualdata");

        roleTO = roleService.update(roleMod.getId(), roleMod);

        assertNotNull(roleTO);
        assertTrue(roleTO.getVirtualAttributes().isEmpty());
    }

    @Test
    public void updateRemovingDerAttribute() {
        RoleTO roleTO = buildBasicRoleTO("withderived");
        roleTO.addDerivedAttribute(attributeTO("rderivedschema", null));

        roleTO = roleService.create(roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getDerivedAttributes().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.addDerivedAttributeToBeRemoved("rderivedschema");

        roleTO = roleService.update(roleMod.getId(), roleMod);

        assertNotNull(roleTO);
        assertTrue(roleTO.getDerivedAttributes().isEmpty());
    }

    @Test
    public void updateAsRoleOwner() {
        // 1. read role as admin
        RoleTO roleTO = roleService.read(7L);

        // 2. prepare update
        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setName("Managing Director");

        // 3. try to update as user3, not owner of role 7 - fail
        setupRestTemplate("user2", "password");

        try {
            roleService.update(roleMod.getId(), roleMod);
            fail();
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }

        // 4. update as user5, owner of role 7 because owner of role 6 with
        // inheritance - success
        super.setupRestTemplate("user5", ADMIN_PWD);

        roleTO = roleService.update(roleMod.getId(), roleMod);
        assertEquals("Managing Director", roleTO.getName());

        // restore admin authentication
        super.resetRestTemplate();
    }

    /**
     * Role rename used to fail in case of parent null.
     *
     * http://code.google.com/p/syncope/issues/detail?id=178
     */
    @Test
    public void issue178() {
        RoleTO roleTO = new RoleTO();
        String roleName = "torename" + getUUIDString();
        roleTO.setName(roleName);

        RoleTO actual = roleService.create(roleTO);

        assertNotNull(actual);
        assertEquals(roleName, actual.getName());
        assertEquals(0L, actual.getParent());

        RoleMod roleMod = new RoleMod();
        roleMod.setId(actual.getId());
        String renamedRole = "renamed" + getUUIDString();
        roleMod.setName(renamedRole);

        actual = roleService.update(roleMod.getId(), roleMod);

        assertNotNull(actual);
        assertEquals(renamedRole, actual.getName());
        assertEquals(0L, actual.getParent());
    }

    @Test
    public void issueSYNCOPE228() {
        RoleTO roleTO = buildRoleTO("issueSYNCOPE228");
        roleTO.addEntitlement("USER_READ");
        roleTO.addEntitlement("SCHEMA_READ");

        roleTO = roleService.create(roleTO);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getEntitlements());
        assertFalse(roleTO.getEntitlements().isEmpty());

        List<String> entitlements = roleTO.getEntitlements();

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setInheritDerivedAttributes(Boolean.TRUE);

        roleTO = roleService.update(roleMod.getId(), roleMod);
        assertNotNull(roleTO);
        assertEquals(entitlements, roleTO.getEntitlements());

        roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setEntitlements(new ArrayList<String>());

        roleTO = roleService.update(roleMod.getId(), roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().isEmpty());
    }
}
