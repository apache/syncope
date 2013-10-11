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

import java.security.AccessControlException;
import java.util.List;
import org.apache.http.HttpStatus;

import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationTargetsTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class RoleTestITCase extends AbstractTest {

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

        roleTO.getRAttrTemplates().add("icon");
        roleTO.getAttrs().add(attributeTO("icon", "anIcon"));

        roleTO.getResources().add("resource-ldap");
        return roleTO;
    }

    private ConnObjectTO readConnectorObject(final String resourceName, final Long roleId) {
        return resourceService.getConnectorObject(resourceName, AttributableType.ROLE, roleId);
    }

    @Test
    public void createWithException() {
        RoleTO newRoleTO = new RoleTO();
        newRoleTO.getAttrs().add(attributeTO("attr1", "value1"));

        try {
            createRole(roleService, newRoleTO);
            fail();
        } catch (SyncopeClientCompositeException sccee) {
            assertNotNull(sccee.getException(SyncopeClientExceptionType.InvalidSyncopeRole));
        }
    }

    @Test
    public void create() {
        RoleTO roleTO = buildRoleTO("lastRole");
        roleTO.getRVirAttrTemplates().add("rvirtualdata");
        roleTO.getVirAttrs().add(attributeTO("rvirtualdata", "rvirtualvalue"));
        roleTO.setRoleOwner(8L);

        roleTO = createRole(roleService, roleTO);
        assertNotNull(roleTO);

        assertNotNull(roleTO.getVirAttrMap());
        assertNotNull(roleTO.getVirAttrMap().get("rvirtualdata").getValues());
        assertFalse(roleTO.getVirAttrMap().get("rvirtualdata").getValues().isEmpty());
        assertEquals("rvirtualvalue", roleTO.getVirAttrMap().get("rvirtualdata").getValues().get(0));

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(4L, (long) roleTO.getPasswordPolicy());

        assertTrue(roleTO.getResources().contains("resource-ldap"));

        ConnObjectTO connObjectTO = readConnectorObject("resource-ldap", roleTO.getId());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getAttrMap().get("owner"));
    }

    @Test
    public void createWithPasswordPolicy() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("roleWithPassword" + getUUIDString());
        roleTO.setParent(8L);
        roleTO.setPasswordPolicy(4L);

        RoleTO actual = createRole(roleService, roleTO);
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
        } catch (SyncopeClientCompositeException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
        }

        RoleTO roleTO = new RoleTO();
        roleTO.setName("toBeDeleted" + getUUIDString());
        roleTO.setParent(8L);

        roleTO.getResources().add("resource-ldap");

        roleTO = createRole(roleService, roleTO);
        assertNotNull(roleTO);

        RoleTO deletedRole = roleService.delete(roleTO.getId());
        assertNotNull(deletedRole);

        try {
            roleService.read(deletedRole.getId());
        } catch (SyncopeClientCompositeException e) {
            assertEquals(HttpStatus.SC_NOT_FOUND, e.getStatusCode());
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
        assertNotNull(roleTO.getAttrs());
        assertFalse(roleTO.getAttrs().isEmpty());
    }

    @Test
    public void selfRead() {
        UserTO userTO = userService.read(1L);
        assertNotNull(userTO);

        assertTrue(userTO.getMembershipMap().containsKey(1L));
        assertFalse(userTO.getMembershipMap().containsKey(3L));

        RoleService roleService2 =
                clientFactory.create("rossini", ADMIN_PWD).getService(RoleService.class);

        SyncopeClientException exception = null;
        try {
            roleService2.selfRead(3L);
            fail();
        } catch (SyncopeClientCompositeException e) {
            exception = e.getException(SyncopeClientExceptionType.UnauthorizedRole);
        }
        assertNotNull(exception);

        RoleTO roleTO = roleService2.selfRead(1L);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttrs());
        assertFalse(roleTO.getAttrs().isEmpty());
    }

    @Test
    public void update() {
        RoleTO roleTO = buildRoleTO("latestRole" + getUUIDString());
        roleTO.getRAttrTemplates().add("show");
        roleTO = createRole(roleService, roleTO);

        assertEquals(1, roleTO.getAttrs().size());

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(4L, (long) roleTO.getPasswordPolicy());

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        String modName = "finalRole" + getUUIDString();
        roleMod.setName(modName);
        roleMod.getAttrsToUpdate().add(attributeMod("show", "FALSE"));

        // change password policy inheritance
        roleMod.setInheritPasswordPolicy(Boolean.FALSE);

        roleTO = roleService.update(roleMod.getId(), roleMod);

        assertEquals(modName, roleTO.getName());
        assertEquals(2, roleTO.getAttrs().size());

        // changes ignored because not requested (null ReferenceMod)
        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        // password policy null because not inherited
        assertNull(roleTO.getPasswordPolicy());
    }

    @Test
    public void updateRemovingVirAttribute() {
        RoleTO roleTO = buildBasicRoleTO("withvirtual" + getUUIDString());
        roleTO.getRVirAttrTemplates().add("rvirtualdata");
        roleTO.getVirAttrs().add(attributeTO("rvirtualdata", null));

        roleTO = createRole(roleService, roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getVirAttrs().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.getVirAttrsToRemove().add("rvirtualdata");

        roleTO = roleService.update(roleMod.getId(), roleMod);

        assertNotNull(roleTO);
        assertTrue(roleTO.getVirAttrs().isEmpty());
    }

    @Test
    public void updateRemovingDerAttribute() {
        RoleTO roleTO = buildBasicRoleTO("withderived" + getUUIDString());
        roleTO.getRDerAttrTemplates().add("rderivedschema");
        roleTO.getDerAttrs().add(attributeTO("rderivedschema", null));

        roleTO = createRole(roleService, roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getDerAttrs().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.getDerAttrsToRemove().add("rderivedschema");

        roleTO = roleService.update(roleMod.getId(), roleMod);

        assertNotNull(roleTO);
        assertTrue(roleTO.getDerAttrs().isEmpty());
    }

    @Test
    public void updateAsRoleOwner() {
        // 1. read role as admin
        RoleTO roleTO = roleService.read(7L);

        // issue SYNCOPE-15
        assertNotNull(roleTO.getCreationDate());
        assertNotNull(roleTO.getLastChangeDate());
        assertEquals("admin", roleTO.getCreator());
        assertEquals("admin", roleTO.getLastModifier());

        // 2. prepare update
        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setName("Managing Director");

        // 3. try to update as verdi, not owner of role 7 - fail
        RoleService roleService2 =
                clientFactory.create("verdi", ADMIN_PWD).getService(RoleService.class);

        try {
            roleService2.update(roleMod.getId(), roleMod);
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertEquals(HttpStatus.SC_FORBIDDEN, e.getStatusCode());
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. update as puccini, owner of role 7 because owner of role 6 with inheritance - success
        RoleService roleService3 =
                clientFactory.create("puccini", ADMIN_PWD).getService(RoleService.class);

        roleTO = roleService3.update(roleMod.getId(), roleMod);
        assertEquals("Managing Director", roleTO.getName());

        // issue SYNCOPE-15
        assertNotNull(roleTO.getCreationDate());
        assertNotNull(roleTO.getLastChangeDate());
        assertEquals("admin", roleTO.getCreator());
        assertEquals("puccini", roleTO.getLastModifier());
        assertTrue(roleTO.getCreationDate().before(roleTO.getLastChangeDate()));
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

        RoleTO actual = createRole(roleService, roleTO);

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
        roleTO.getEntitlements().add("USER_READ");
        roleTO.getEntitlements().add("SCHEMA_READ");

        roleTO = createRole(roleService, roleTO);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getEntitlements());
        assertFalse(roleTO.getEntitlements().isEmpty());

        List<String> entitlements = roleTO.getEntitlements();

        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setInheritDerAttrs(Boolean.TRUE);

        roleTO = roleService.update(roleMod.getId(), roleMod);
        assertNotNull(roleTO);
        assertEquals(entitlements, roleTO.getEntitlements());

        roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setModEntitlements(true);
        roleMod.getEntitlements().clear();

        roleTO = roleService.update(roleMod.getId(), roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().isEmpty());
    }

    @Test
    public void unlink() {
        RoleTO actual = createRole(roleService, buildRoleTO("unlink"));
        assertNotNull(actual);

        assertNotNull(readConnectorObject("resource-ldap", actual.getId()));

        PropagationTargetsTO res = new PropagationTargetsTO();
        res.getResources().add("resource-ldap");

        actual = roleService.unlink(actual.getId(), res);
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        actual = roleService.read(actual.getId());
        assertNotNull(actual);

        assertTrue(actual.getResources().isEmpty());

        assertNotNull(readConnectorObject("resource-ldap", actual.getId()));
    }

    @Test
    public void unassign() {
        RoleTO actual = createRole(roleService, buildRoleTO("unassign"));
        assertNotNull(actual);

        assertNotNull(readConnectorObject("resource-ldap", actual.getId()));

        PropagationTargetsTO res = new PropagationTargetsTO();
        res.getResources().add("resource-ldap");

        actual = roleService.unassign(actual.getId(), res);
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        actual = roleService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            readConnectorObject("resource-ldap", actual.getId());
            fail();
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void deprovision() {
        RoleTO actual = createRole(roleService, buildRoleTO("deprovision"));
        assertNotNull(actual);

        assertNotNull(readConnectorObject("resource-ldap", actual.getId()));

        PropagationTargetsTO res = new PropagationTargetsTO();
        res.getResources().add("resource-ldap");

        actual = roleService.deprovision(actual.getId(), res);
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        actual = roleService.read(actual.getId());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            readConnectorObject("resource-ldap", actual.getId());
            fail();
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void createWithMandatorySchemaNotTemplate() {
        // 1. create a role mandatory schema
        SchemaTO badge = new SchemaTO();
        badge.setName("badge");
        badge.setMandatoryCondition("true");
        schemaService.create(AttributableType.ROLE, SchemaType.NORMAL, badge);

        // 2. create a role *without* an attribute for that schema: it works
        RoleTO roleTO = buildRoleTO("lastRole");
        assertFalse(roleTO.getAttrMap().containsKey(badge.getName()));
        roleTO = createRole(roleService, roleTO);
        assertNotNull(roleTO);
        assertFalse(roleTO.getAttrMap().containsKey(badge.getName()));

        // 3. add a template for badge to the role just created - 
        // failure since no values are provided and it is mandatory
        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setModRAttrTemplates(true);
        roleMod.getRAttrTemplates().add("badge");

        try {
            roleService.update(roleMod.getId(), roleMod);
            fail();
        } catch (SyncopeClientCompositeException e) {
            assertNotNull(e.getException(SyncopeClientExceptionType.RequiredValuesMissing));
        }

        // 4. also add an actual attribute for badge - it will work        
        roleMod.getAttrsToUpdate().add(attributeMod(badge.getName(), "xxxxxxxxxx"));

        roleTO = roleService.update(roleMod.getId(), roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getAttrMap().containsKey(badge.getName()));
    }

    @Test
    public void anonymous() {
        RoleService unauthenticated = clientFactory.create(null, null).getService(RoleService.class);
        try {
            unauthenticated.list();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        RoleService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(RoleService.class);
        assertFalse(anonymous.list().isEmpty());
    }
}
