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

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.Preference;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.common.wrap.ResourceName;
import org.identityconnectors.framework.common.objects.Name;
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

        roleTO.getResources().add(RESOURCE_NAME_LDAP);
        return roleTO;
    }

    @Test
    public void createWithException() {
        RoleTO newRoleTO = new RoleTO();
        newRoleTO.getAttrs().add(attributeTO("attr1", "value1"));

        try {
            createRole(newRoleTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidSyncopeRole, e.getType());
        }
    }

    @Test
    public void create() {
        RoleTO roleTO = buildRoleTO("lastRole");
        roleTO.getRVirAttrTemplates().add("rvirtualdata");
        roleTO.getVirAttrs().add(attributeTO("rvirtualdata", "rvirtualvalue"));
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
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, roleTO.getId());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getAttrMap().get("owner"));
    }

    @Test
    public void createWithPasswordPolicy() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("roleWithPassword" + getUUIDString());
        roleTO.setParent(8L);
        roleTO.setPasswordPolicy(4L);

        RoleTO actual = createRole(roleTO);
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
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        RoleTO roleTO = new RoleTO();
        roleTO.setName("toBeDeleted" + getUUIDString());
        roleTO.setParent(8L);

        roleTO.getResources().add(RESOURCE_NAME_LDAP);

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);

        RoleTO deletedRole = deleteRole(roleTO.getId());
        assertNotNull(deletedRole);

        try {
            roleService.read(deletedRole.getId());
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

        RoleService roleService2 = clientFactory.create("rossini", ADMIN_PWD).getService(RoleService.class);

        try {
            roleService2.readSelf(3L);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.UnauthorizedRole, e.getType());
        }

        RoleTO roleTO = roleService2.readSelf(1L);
        assertNotNull(roleTO);
        assertNotNull(roleTO.getAttrs());
        assertFalse(roleTO.getAttrs().isEmpty());
    }

    @Test
    public void update() {
        RoleTO roleTO = buildRoleTO("latestRole" + getUUIDString());
        roleTO.getRAttrTemplates().add("show");
        roleTO = createRole(roleTO);

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

        roleTO = updateRole(roleMod);

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

        roleTO = createRole(roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getVirAttrs().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.getVirAttrsToRemove().add("rvirtualdata");

        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getVirAttrs().isEmpty());
    }

    @Test
    public void updateRemovingDerAttribute() {
        RoleTO roleTO = buildBasicRoleTO("withderived" + getUUIDString());
        roleTO.getRDerAttrTemplates().add("rderivedschema");
        roleTO.getDerAttrs().add(attributeTO("rderivedschema", null));

        roleTO = createRole(roleTO);

        assertNotNull(roleTO);
        assertEquals(1, roleTO.getDerAttrs().size());

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
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
        roleMod.setId(roleTO.getId());
        roleMod.setName("Managing Director");

        // 3. try to update as verdi, not owner of role 7 - fail
        RoleService roleService2 = clientFactory.create("verdi", ADMIN_PWD).getService(RoleService.class);

        try {
            roleService2.update(roleMod.getId(), roleMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.UNAUTHORIZED, e.getType().getResponseStatus());
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. update as puccini, owner of role 7 because owner of role 6 with inheritance - success
        RoleService roleService3 = clientFactory.create("puccini", ADMIN_PWD).getService(RoleService.class);

        roleTO = roleService3.update(roleMod.getId(), roleMod).readEntity(RoleTO.class);
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
        roleMod.setId(actual.getId());
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
        roleMod.setId(roleTO.getId());
        roleMod.setInheritDerAttrs(Boolean.TRUE);

        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertEquals(entitlements, roleTO.getEntitlements());

        roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
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

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId()));

        assertNotNull(roleService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.UNLINK,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId()));
    }

    @Test
    public void link() {
        RoleTO roleTO = buildRoleTO("link");
        roleTO.getResources().clear();

        RoleTO actual = createRole(roleTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getId(),
                ResourceAssociationActionType.LINK,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void unassign() {
        RoleTO actual = createRole(buildRoleTO("unassign"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId()));

        assertNotNull(roleService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.UNASSIGN,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
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
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getId(),
                ResourceAssociationActionType.ASSIGN,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId()));
    }

    @Test
    public void deprovision() {
        RoleTO actual = createRole(buildRoleTO("deprovision"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId()));

        assertNotNull(roleService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
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
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getId(),
                ResourceAssociationActionType.PROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId()));
    }

    @Test
    public void deprovisionUnlinked() {
        RoleTO roleTO = buildRoleTO("assign");
        roleTO.getResources().clear();

        RoleTO actual = createRole(roleTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(roleService.bulkAssociation(actual.getId(),
                ResourceAssociationActionType.PROVISION,
                CollectionWrapper.wrap("resource-ldap", ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId()));

        assertNotNull(roleService.bulkDeassociation(actual.getId(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = roleService.read(actual.getId());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, actual.getId());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
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
        roleTO = createRole(roleTO);
        assertNotNull(roleTO);
        assertFalse(roleTO.getAttrMap().containsKey(badge.getName()));

        // 3. add a template for badge to the role just created - 
        // failure since no values are provided and it is mandatory
        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());
        roleMod.setModRAttrTemplates(true);
        roleMod.getRAttrTemplates().add("badge");

        try {
            updateRole(roleMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        // 4. also add an actual attribute for badge - it will work        
        roleMod.getAttrsToUpdate().add(attributeMod(badge.getName(), "xxxxxxxxxx"));

        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertTrue(roleTO.getAttrMap().containsKey(badge.getName()));
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
        roleMod.getAttrsToUpdate().add(attributeMod("badge", "xxxxxxxxxx"));

        response = noContentService.update(role.getId(), roleMod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));

        response = noContentService.delete(role.getId());
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
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, parent.getId());
        assertNotNull(parentRemoteObject);
        assertNotNull(getLdapRemoteObject(parentRemoteObject.getAttrMap().get(Name.NAME).getValues().get(0)));

        // 2. create child role
        RoleTO child = buildBasicRoleTO(childName);
        child.getResources().add(RESOURCE_NAME_LDAP);
        child.setParent(parent.getId());

        child = createRole(child);
        assertTrue(child.getResources().contains(RESOURCE_NAME_LDAP));

        final ConnObjectTO childRemoteObject =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, child.getId());
        assertNotNull(childRemoteObject);
        assertNotNull(getLdapRemoteObject(childRemoteObject.getAttrMap().get(Name.NAME).getValues().get(0)));

        // 3. remove parent role
        roleService.delete(parent.getId());

        // 4. asserts for issue 455
        try {
            roleService.read(parent.getId());
            fail();
        } catch (SyncopeClientException scce) {
            assertNotNull(scce);
        }

        try {
            roleService.read(child.getId());
            fail();
        } catch (SyncopeClientException scce) {
            assertNotNull(scce);
        }

        assertNull(getLdapRemoteObject(parentRemoteObject.getAttrMap().get(Name.NAME).getValues().get(0)));
        assertNull(getLdapRemoteObject(childRemoteObject.getAttrMap().get(Name.NAME).getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE493() {
        // 1.  create role and check that title is propagated on resource with mapping for title set to BOTH
        RoleTO roleTO = buildBasicRoleTO("issueSYNCOPE493-Role");
        roleTO.getResources().add(RESOURCE_NAME_LDAP);
        roleTO.getRAttrTemplates().add("title");
        roleTO.getAttrs().add(attributeTO("title", "TITLE"));

        roleTO = createRole(roleTO);
        assertTrue(roleTO.getResources().contains(RESOURCE_NAME_LDAP));

        ConnObjectTO actual = resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE,
                roleTO.getId());
        assertNotNull(actual);

        // check if mapping attribute with purpose BOTH has really been propagated
        assertNotNull(actual.getAttrMap().get("description"));
        assertEquals("TITLE", actual.getAttrMap().get("description").getValues().get(0));

        // 2.  update resource LDAP
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(ldap);

        MappingTO ldapNewRMapping = ldap.getRmapping();
        // change purpose from BOTH to NONE
        for (MappingItemTO itemTO : ldapNewRMapping.getItems()) {
            if ("title".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.NONE);
            }
        }

        ldap.setRmapping(ldapNewRMapping);
        ldap.setUmapping(ldap.getUmapping());

        resourceService.update(RESOURCE_NAME_LDAP, ldap);
        ResourceTO newLdap = resourceService.read(ldap.getName());
        assertNotNull(newLdap);

        // check for existence
        Collection<MappingItemTO> mapItems = newLdap.getRmapping().getItems();
        assertNotNull(mapItems);
        assertEquals(4, mapItems.size());

        // 3.  update role and check that title han not been propagated, external attribute description must not be present
        // in role mapping
        RoleMod roleMod = new RoleMod();
        roleMod.setId(roleTO.getId());

        AttributeMod attr = attributeMod("title", "TITLENEW");
        attr.getValuesToBeRemoved().add("TITLE");
        roleMod.getAttrsToUpdate().add(attr);
        roleTO = updateRole(roleMod);
        assertNotNull(roleTO);
        assertEquals(1, roleTO.getPropagationStatusTOs().size());
        assertTrue(roleTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
        // check update on Syncope
        assertEquals("TITLENEW", roleTO.getAttrMap().get("title").getValues().get(0));

        final ConnObjectTO newRole = resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE,
                roleTO.getId());

        // due to NONE mapping for attribute title external attribute description must not be present
        assertNull(newRole.getAttrMap().get("description"));

        // 4.  restore resource LDAP mapping
        ldapNewRMapping = newLdap.getRmapping();
        // restore purpose from NONE to BOTH
        for (MappingItemTO itemTO : ldapNewRMapping.getItems()) {
            if ("title".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.BOTH);
            }
        }

        newLdap.setRmapping(ldapNewRMapping);
        newLdap.setUmapping(newLdap.getUmapping());

        resourceService.update(RESOURCE_NAME_LDAP, newLdap);
    }
}
