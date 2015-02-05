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
package org.apache.syncope.fit.server.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.ReferenceMod;
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.common.lib.wrap.ResourceName;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class RoleITCase extends AbstractITCase {

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

        roleTO.getRPlainAttrTemplates().add("icon");
        roleTO.getPlainAttrs().add(attrTO("icon", "anIcon"));

        roleTO.getResources().add(RESOURCE_NAME_LDAP);
        return roleTO;
    }

    @Test
    public void createWithException() {
        RoleTO newRoleTO = new RoleTO();
        newRoleTO.getPlainAttrs().add(attrTO("attr1", "value1"));

        try {
            createRole(newRoleTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidRole, e.getType());
        }
    }

    @Test
    @Ignore
    public void create() {
        RoleTO roleTO = buildRoleTO("lastRole");
        roleTO.getRVirAttrTemplates().add("rvirtualdata");
        roleTO.getVirAttrs().add(attrTO("rvirtualdata", "rvirtualvalue"));
        roleTO.setRoleOwner(8L);

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);

        assertNotNull(roleTO.getVirAttrMap());
        assertNotNull(roleTO.getVirAttrMap().get("rvirtualdata").getValues());
        assertFalse(roleTO.getVirAttrMap().get("rvirtualdata").getValues().isEmpty());
        assertEquals("rvirtualvalue", roleTO.getVirAttrMap().get("rvirtualdata").getValues().get(0));

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(4L, (long) roleTO.getPasswordPolicy());

        assertTrue(roleTO.getResources().contains(RESOURCE_NAME_LDAP));

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, roleTO.getKey());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getPlainAttrMap().get("owner"));

        // SYNCOPE-515: remove ownership
        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleTO.getKey());
        roleMod.setRoleOwner(new ReferenceMod());

        assertNull(updateRole(roleMod).getRoleOwner());
    }

    @Test
    public void createWithPasswordPolicy() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("roleWithPassword" + getUUIDString());
        roleTO.setParent(8L);
        roleTO.setPasswordPolicy(4L);

        RoleTO actual = createRole(roleTO);
        assertNotNull(actual);

        actual = roleService.read(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());
        assertEquals(4L, (long) actual.getPasswordPolicy());
    }

    @Test
    public void delete() {
        try {
            roleService.delete(0L);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        RoleTO roleTO = new RoleTO();
        roleTO.setName("toBeDeleted" + getUUIDString());
        roleTO.setParent(8L);

        roleTO.getResources().add(RESOURCE_NAME_LDAP);

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);

        RoleTO deletedRole = deleteRole(roleTO.getKey());
        assertNotNull(deletedRole);

        try {
            roleService.read(deletedRole.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        PagedResult<RoleTO> roleTOs = roleService.list();
        assertNotNull(roleTOs);
        assertTrue(roleTOs.getResult().size() >= 8);
        for (RoleTO roleTO : roleTOs.getResult()) {
            assertNotNull(roleTO);
        }
    }

    @Test
    public void parent() {
        RoleTO roleTO = roleService.parent(7L);

        assertNotNull(roleTO);
        assertEquals(roleTO.getKey(), 6L);
    }

    @Test
    public void read() {
        RoleTO roleTO = roleService.read(1L);

        assertNotNull(roleTO);
        assertNotNull(roleTO.getPlainAttrs());
        assertFalse(roleTO.getPlainAttrs().isEmpty());
    }

    @Test
    public void selfRead() {
        UserTO userTO = userService.read(1L);
        assertNotNull(userTO);

        assertTrue(userTO.getMembershipMap().containsKey(1L));
        assertFalse(userTO.getMembershipMap().containsKey(3L));

        RoleService roleService2 = clientFactory.create("rossini", ADMIN_PWD).getService(RoleService.class);

        try {
            roleService2.readSelf(3L);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.UnauthorizedRole, e.getType());
        }

        RoleTO roleTO = roleService2.readSelf(1L);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getPlainAttrs());
        assertFalse(roleTO.getPlainAttrs().isEmpty());
    }

    @Test
    public void update() {
        RoleTO roleTO = buildRoleTO("latestRole" + getUUIDString());
        roleTO.getRPlainAttrTemplates().add("show");
        roleTO = createRole(roleTO);

        assertEquals(1, roleTO.getPlainAttrs().size());

        assertNotNull(roleTO.getAccountPolicy());
        assertEquals(6L, (long) roleTO.getAccountPolicy());

        assertNotNull(roleTO.getPasswordPolicy());
        assertEquals(4L, (long) roleTO.getPasswordPolicy());

        RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleTO.getKey());
        String modName = "finalRole" + getUUIDString();
        roleMod.setName(modName);
        roleMod.getPlainAttrsToUpdate().add(attrMod("show", "FALSE"));

        // change password policy inheritance
        roleMod.setInheritPasswordPolicy(Boolean.FALSE);

        roleTO = updateRole(roleMod);

        assertEquals(modName, roleTO.getName());
        assertEquals(2, roleTO.getPlainAttrs().size());

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
        roleTO.getVirAttrs().add(attrTO("rvirtualdata", null));

        roleTO = createRole(roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getVirAttrs().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleTO.getKey());
        roleMod.getVirAttrsToRemove().add("rvirtualdata");

        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getVirAttrs().isEmpty());
    }

    @Test
    public void updateRemovingDerAttribute() {
        RoleTO roleTO = buildBasicRoleTO("withderived" + getUUIDString());
        roleTO.getRDerAttrTemplates().add("rderivedschema");
        roleTO.getDerAttrs().add(attrTO("rderivedschema", null));

        roleTO = createRole(roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getDerAttrs().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleTO.getKey());
        roleMod.getDerAttrsToRemove().add("rderivedschema");

        roleTO = updateRole(roleMod);
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
        roleMod.setKey(roleTO.getKey());
        roleMod.setName("Managing Director");

        // 3. try to update as verdi, not owner of role 7 - fail
        RoleService roleService2 = clientFactory.create("verdi", ADMIN_PWD).getService(RoleService.class);

        try {
            roleService2.update(roleMod.getKey(), roleMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.UNAUTHORIZED, e.getType().getResponseStatus());
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. update as puccini, owner of role 7 because owner of role 6 with inheritance - success
        RoleService roleService3 = clientFactory.create("puccini", ADMIN_PWD).getService(RoleService.class);

        roleTO = roleService3.update(roleMod.getKey(), roleMod).readEntity(RoleTO.class);
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

        RoleTO actual = createRole(roleTO);

        assertNotNull(actual);
        assertEquals(roleName, actual.getName());
        assertEquals(0L, actual.getParent());

        RoleMod roleMod = new RoleMod();
        roleMod.setKey(actual.getKey());
        String renamedRole = "renamed" + getUUIDString();
        roleMod.setName(renamedRole);

        actual = updateRole(roleMod);
        assertNotNull(actual);
        assertEquals(renamedRole, actual.getName());
        assertEquals(0L, actual.getParent());
    }

    @Test
    public void issueSYNCOPE228() {
        RoleTO roleTO = buildRoleTO("issueSYNCOPE228");
        roleTO.getEntitlements().add("USER_READ");
        roleTO.getEntitlements().add("SCHEMA_READ");

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getEntitlements());
        assertFalse(roleTO.getEntitlements().isEmpty());

        List<String> entitlements = roleTO.getEntitlements();

        RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleTO.getKey());
        roleMod.setInheritDerAttrs(Boolean.TRUE);

        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertEquals(entitlements, roleTO.getEntitlements());

        roleMod = new RoleMod();
        roleMod.setKey(roleTO.getKey());
        roleMod.setModEntitlements(true);
        roleMod.getEntitlements().clear();

        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getEntitlements().isEmpty());
    }

    @Test
    public void unlink() {
        RoleTO actual = createRole(buildRoleTO("unlink"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey()));

        assertNotNull(roleService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.UNLINK,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey()));
    }

    @Test
    public void link() {
        RoleTO roleTO = buildRoleTO("link");
        roleTO.getResources().clear();

        RoleTO actual = createRole(roleTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.LINK,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void unassign() {
        RoleTO actual = createRole(buildRoleTO("unassign"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey()));

        assertNotNull(roleService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.UNASSIGN,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void assign() {
        RoleTO roleTO = buildRoleTO("assign");
        roleTO.getResources().clear();

        RoleTO actual = createRole(roleTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.ASSIGN,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey()));
    }

    @Test
    public void deprovision() {
        RoleTO actual = createRole(buildRoleTO("deprovision"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey()));

        assertNotNull(roleService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void provision() {
        RoleTO roleTO = buildRoleTO("assign");
        roleTO.getResources().clear();

        RoleTO actual = createRole(roleTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.PROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey()));
    }

    @Test
    public void deprovisionUnlinked() {
        RoleTO roleTO = buildRoleTO("assign");
        roleTO.getResources().clear();

        RoleTO actual = createRole(roleTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.PROVISION,
                CollectionWrapper.wrap("resource-ldap", ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey()));

        assertNotNull(roleService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void createWithMandatorySchemaNotTemplate() {
        // 1. create a role mandatory schema
        PlainSchemaTO badge = new PlainSchemaTO();
        badge.setKey("badge");
        badge.setMandatoryCondition("true");
        schemaService.create(AttributableType.ROLE, SchemaType.PLAIN, badge);

        // 2. create a role *without* an attribute for that schema: it works
        RoleTO roleTO = buildRoleTO("lastRole");
        assertFalse(roleTO.getPlainAttrMap().containsKey(badge.getKey()));
        roleTO = createRole(roleTO);
        assertNotNull(roleTO);
        assertFalse(roleTO.getPlainAttrMap().containsKey(badge.getKey()));

        // 3. add a template for badge to the role just created - 
        // failure since no values are provided and it is mandatory
        RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleTO.getKey());
        roleMod.setModRAttrTemplates(true);
        roleMod.getRPlainAttrTemplates().add("badge");

        try {
            updateRole(roleMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        // 4. also add an actual attribute for badge - it will work        
        roleMod.getPlainAttrsToUpdate().add(attrMod(badge.getKey(), "xxxxxxxxxx"));

        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getPlainAttrMap().containsKey(badge.getKey()));
    }

    @Test
    public void anonymous() {
        RoleService unauthenticated = clientFactory.createAnonymous().getService(RoleService.class);
        try {
            unauthenticated.list();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        RoleService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(RoleService.class);
        assertFalse(anonymous.list().getResult().isEmpty());
    }

    @Test
    public void noContent() throws IOException {
        SyncopeClient noContentclient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        RoleService noContentService = noContentclient.prefer(RoleService.class, Preference.RETURN_NO_CONTENT);

        RoleTO role = buildRoleTO("noContent");

        Response response = noContentService.create(role);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));

        role = getObject(response.getLocation(), RoleService.class, RoleTO.class);
        assertNotNull(role);

        RoleMod roleMod = new RoleMod();
        roleMod.getPlainAttrsToUpdate().add(attrMod("badge", "xxxxxxxxxx"));

        response = noContentService.update(role.getKey(), roleMod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));

        response = noContentService.delete(role.getKey());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));
    }

    @Test
    public void issueSYNCOPE455() {
        final String parentName = "issueSYNCOPE455-PRole";
        final String childName = "issueSYNCOPE455-CRole";

        // 1. create parent role
        RoleTO parent = buildBasicRoleTO(parentName);
        parent.getResources().add(RESOURCE_NAME_LDAP);

        parent = createRole(parent);
        assertTrue(parent.getResources().contains(RESOURCE_NAME_LDAP));

        final ConnObjectTO parentRemoteObject =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, parent.getKey());
        assertNotNull(parentRemoteObject);
        assertNotNull(getLdapRemoteObject(parentRemoteObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));

        // 2. create child role
        RoleTO child = buildBasicRoleTO(childName);
        child.getResources().add(RESOURCE_NAME_LDAP);
        child.setParent(parent.getKey());

        child = createRole(child);
        assertTrue(child.getResources().contains(RESOURCE_NAME_LDAP));

        final ConnObjectTO childRemoteObject =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, child.getKey());
        assertNotNull(childRemoteObject);
        assertNotNull(getLdapRemoteObject(childRemoteObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));

        // 3. remove parent role
        roleService.delete(parent.getKey());

        // 4. asserts for issue 455
        try {
            roleService.read(parent.getKey());
            fail();
        } catch (SyncopeClientException scce) {
            assertNotNull(scce);
        }

        try {
            roleService.read(child.getKey());
            fail();
        } catch (SyncopeClientException scce) {
            assertNotNull(scce);
        }

        assertNull(getLdapRemoteObject(parentRemoteObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));
        assertNull(getLdapRemoteObject(childRemoteObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE543() {
        final String ancestorName = "issueSYNCOPE543-ARole";
        final String parentName = "issueSYNCOPE543-PRole";
        final String childName = "issueSYNCOPE543-CRole";

        // 1. create ancestor role
        RoleTO ancestor = buildBasicRoleTO(ancestorName);
        ancestor.setParent(0L);
        ancestor.getRPlainAttrTemplates().add("icon");
        ancestor.getPlainAttrs().add(attrTO("icon", "ancestorIcon"));
        ancestor = createRole(ancestor);
        assertEquals("ancestorIcon", ancestor.getPlainAttrMap().get("icon").getValues().get(0));

        // 2. create parent role
        RoleTO parent = buildBasicRoleTO(parentName);
        parent.setParent(ancestor.getKey());
        parent.getRPlainAttrTemplates().add("icon");
        parent.getPlainAttrs().add(attrTO("icon", "parentIcon"));
        parent = createRole(parent);
        assertEquals("parentIcon", parent.getPlainAttrMap().get("icon").getValues().get(0));

        // 3. create child role
        RoleTO child = buildBasicRoleTO(childName);
        child.setParent(parent.getKey());
        child.getRPlainAttrTemplates().add("icon");
        child.getPlainAttrs().add(attrTO("icon", "childIcon"));
        child = createRole(child);
        assertEquals("childIcon", child.getPlainAttrMap().get("icon").getValues().get(0));

        final RoleMod roleChildMod = new RoleMod();
        roleChildMod.setKey(child.getKey());
        roleChildMod.setInheritPlainAttrs(Boolean.TRUE);
        updateRole(roleChildMod);

        child = roleService.read(child.getKey());
        assertNotNull(child);
        assertNotNull(child.getPlainAttrMap().get("icon").getValues());
        assertEquals("parentIcon", child.getPlainAttrMap().get("icon").getValues().get(0));

        final RoleMod roleParentMod = new RoleMod();
        roleParentMod.setKey(parent.getKey());
        roleParentMod.setInheritPlainAttrs(Boolean.TRUE);
        updateRole(roleParentMod);

        child = roleService.read(child.getKey());
        assertNotNull(child);
        assertNotNull(child.getPlainAttrMap().get("icon").getValues());
        assertEquals("ancestorIcon", child.getPlainAttrMap().get("icon").getValues().get(0));

        parent = roleService.read(parent.getKey());
        assertNotNull(parent);
        assertNotNull(parent.getPlainAttrMap().get("icon").getValues());
        assertEquals("ancestorIcon", parent.getPlainAttrMap().get("icon").getValues().get(0));

        roleParentMod.setInheritPlainAttrs(Boolean.FALSE);
        updateRole(roleParentMod);

        child = roleService.read(child.getKey());
        assertNotNull(child);
        assertNotNull(child.getPlainAttrMap().get("icon").getValues());
        assertEquals("parentIcon", child.getPlainAttrMap().get("icon").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE632() {
        RoleTO roleTO = null;
        try {
            // 1. create new LDAP resource having account id mapped to a derived attribute
            ResourceTO newLDAP = resourceService.read(RESOURCE_NAME_LDAP);
            newLDAP.setKey("new-ldap");
            newLDAP.setPropagationPrimary(true);
            MappingItemTO accountId = newLDAP.getRmapping().getAccountIdItem();
            accountId.setIntMappingType(IntMappingType.RoleDerivedSchema);
            accountId.setIntAttrName("displayProperty");
            newLDAP.getRmapping().setAccountIdItem(accountId);
            newLDAP.getRmapping().setAccountLink("'cn=' + displayProperty + ',ou=groups,o=isp'");

            MappingItemTO description = new MappingItemTO();
            description.setIntMappingType(IntMappingType.RoleId);
            description.setExtAttrName("description");
            description.setPurpose(MappingPurpose.BOTH);
            newLDAP.getRmapping().addItem(description);

            newLDAP = createResource(newLDAP);
            assertNotNull(newLDAP);

            // 2. create a role and give the resource created above
            roleTO = buildRoleTO("lastRole");
            roleTO.getRPlainAttrTemplates().add("icon");
            roleTO.getPlainAttrs().add(attrTO("icon", "anIcon"));
            roleTO.getRPlainAttrTemplates().add("show");
            roleTO.getPlainAttrs().add(attrTO("show", "true"));
            roleTO.getRDerAttrTemplates().add("displayProperty");
            roleTO.getDerAttrs().add(attrTO("displayProperty", null));
            roleTO.getResources().clear();
            roleTO.getResources().add("new-ldap");

            roleTO = createRole(roleTO);
            assertNotNull(roleTO);

            // 3. update the role
            RoleMod roleMod = new RoleMod();
            roleMod.setKey(roleTO.getKey());
            roleMod.getPlainAttrsToRemove().add("icon");
            roleMod.getPlainAttrsToUpdate().add(attrMod("icon", "anotherIcon"));

            roleTO = updateRole(roleMod);
            assertNotNull(roleTO);

            // 4. check that a single group exists in LDAP for the role created and updated above
            int entries = 0;
            DirContext ctx = null;
            try {
                ctx = getLdapResourceDirContext(null, null);

                SearchControls ctls = new SearchControls();
                ctls.setReturningAttributes(new String[] { "*", "+" });
                ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

                NamingEnumeration<SearchResult> result =
                        ctx.search("ou=groups,o=isp", "(description=" + roleTO.getKey() + ")", ctls);
                while (result.hasMore()) {
                    result.next();
                    entries++;
                }
            } catch (Exception e) {
                // ignore
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                        // ignore
                    }
                }
            }

            assertEquals(1, entries);
        } finally {
            if (roleTO != null) {
                roleService.delete(roleTO.getKey());
            }
            resourceService.delete("new-ldap");
        }
    }
}
