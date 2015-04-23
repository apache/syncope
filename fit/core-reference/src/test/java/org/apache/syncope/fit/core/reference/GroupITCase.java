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
package org.apache.syncope.fit.core.reference;

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
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.mod.ReferenceMod;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
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
import org.apache.syncope.common.rest.api.service.GroupService;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class GroupITCase extends AbstractITCase {

    private GroupTO buildBasicGroupTO(final String name) {
        GroupTO groupTO = new GroupTO();
        groupTO.setRealm("/");
        groupTO.setName(name + getUUIDString());
        return groupTO;
    }

    private GroupTO buildGroupTO(final String name) {
        GroupTO groupTO = buildBasicGroupTO(name);

        groupTO.getGPlainAttrTemplates().add("icon");
        groupTO.getPlainAttrs().add(attrTO("icon", "anIcon"));

        groupTO.getResources().add(RESOURCE_NAME_LDAP);
        return groupTO;
    }

    @Test
    @Ignore
    public void create() {
        GroupTO groupTO = buildGroupTO("lastGroup");
        groupTO.getGVirAttrTemplates().add("rvirtualdata");
        groupTO.getVirAttrs().add(attrTO("rvirtualdata", "rvirtualvalue"));
        groupTO.setGroupOwner(8L);

        groupTO = createGroup(groupTO);
        assertNotNull(groupTO);

        assertNotNull(groupTO.getVirAttrMap());
        assertNotNull(groupTO.getVirAttrMap().get("rvirtualdata").getValues());
        assertFalse(groupTO.getVirAttrMap().get("rvirtualdata").getValues().isEmpty());
        assertEquals("rvirtualvalue", groupTO.getVirAttrMap().get("rvirtualdata").getValues().get(0));

        assertTrue(groupTO.getResources().contains(RESOURCE_NAME_LDAP));

        ConnObjectTO connObjectTO =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, groupTO.getKey());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getPlainAttrMap().get("owner"));

        // SYNCOPE-515: remove ownership
        final GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupTO.getKey());
        groupMod.setGroupOwner(new ReferenceMod());

        assertNull(updateGroup(groupMod).getGroupOwner());
    }

    @Test
    public void delete() {
        try {
            groupService.delete(0L);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        GroupTO groupTO = new GroupTO();
        groupTO.setName("toBeDeleted" + getUUIDString());
        groupTO.setRealm("/even");

        groupTO.getResources().add(RESOURCE_NAME_LDAP);

        groupTO = createGroup(groupTO);
        assertNotNull(groupTO);

        GroupTO deletedGroup = deleteGroup(groupTO.getKey());
        assertNotNull(deletedGroup);

        try {
            groupService.read(deletedGroup.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void list() {
        PagedResult<GroupTO> groupTOs =
                groupService.list(SyncopeClient.getSubjectListQueryBuilder().realm(SyncopeConstants.ROOT_REALM).build());
        assertNotNull(groupTOs);
        assertTrue(groupTOs.getResult().size() >= 8);
        for (GroupTO groupTO : groupTOs.getResult()) {
            assertNotNull(groupTO);
        }
    }

    @Test
    public void read() {
        GroupTO groupTO = groupService.read(1L);

        assertNotNull(groupTO);
        assertNotNull(groupTO.getPlainAttrs());
        assertFalse(groupTO.getPlainAttrs().isEmpty());
    }

    @Test
    public void selfRead() {
        UserTO userTO = userService.read(1L);
        assertNotNull(userTO);

        assertTrue(userTO.getMembershipMap().containsKey(1L));
        assertFalse(userTO.getMembershipMap().containsKey(3L));

        GroupService groupService2 = clientFactory.create("rossini", ADMIN_PWD).getService(GroupService.class);

        try {
            groupService2.read(3L);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.Unauthorized, e.getType());
        }

        List<GroupTO> groups = groupService2.own();
        assertNotNull(groups);
        assertFalse(groups.isEmpty());
        assertNotNull(groups.get(0).getPlainAttrs());
        assertFalse(groups.get(0).getPlainAttrs().isEmpty());
    }

    @Test
    public void update() {
        GroupTO groupTO = buildGroupTO("latestGroup" + getUUIDString());
        groupTO.getGPlainAttrTemplates().add("show");
        groupTO = createGroup(groupTO);

        assertEquals(1, groupTO.getPlainAttrs().size());

        GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupTO.getKey());
        String modName = "finalGroup" + getUUIDString();
        groupMod.setName(modName);
        groupMod.getPlainAttrsToUpdate().add(attrMod("show", "FALSE"));

        // change password policy inheritance
        groupMod.setInheritPasswordPolicy(Boolean.FALSE);

        groupTO = updateGroup(groupMod);

        assertEquals(modName, groupTO.getName());
        assertEquals(2, groupTO.getPlainAttrs().size());
    }

    @Test
    public void updateRemovingVirAttribute() {
        GroupTO groupTO = buildBasicGroupTO("withvirtual" + getUUIDString());
        groupTO.getGVirAttrTemplates().add("rvirtualdata");
        groupTO.getVirAttrs().add(attrTO("rvirtualdata", null));

        groupTO = createGroup(groupTO);

        assertNotNull(groupTO);
        assertEquals(1, groupTO.getVirAttrs().size());

        final GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupTO.getKey());
        groupMod.getVirAttrsToRemove().add("rvirtualdata");

        groupTO = updateGroup(groupMod);
        assertNotNull(groupTO);
        assertTrue(groupTO.getVirAttrs().isEmpty());
    }

    @Test
    public void updateRemovingDerAttribute() {
        GroupTO groupTO = buildBasicGroupTO("withderived" + getUUIDString());
        groupTO.getGDerAttrTemplates().add("rderivedschema");
        groupTO.getDerAttrs().add(attrTO("rderivedschema", null));

        groupTO = createGroup(groupTO);

        assertNotNull(groupTO);
        assertEquals(1, groupTO.getDerAttrs().size());

        final GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupTO.getKey());
        groupMod.getDerAttrsToRemove().add("rderivedschema");

        groupTO = updateGroup(groupMod);
        assertNotNull(groupTO);
        assertTrue(groupTO.getDerAttrs().isEmpty());
    }

    @Test
    public void updateAsGroupOwner() {
        // 1. read group as admin
        GroupTO groupTO = groupService.read(6L);

        // issue SYNCOPE-15
        assertNotNull(groupTO.getCreationDate());
        assertNotNull(groupTO.getLastChangeDate());
        assertEquals("admin", groupTO.getCreator());
        assertEquals("admin", groupTO.getLastModifier());

        // 2. prepare update
        GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupTO.getKey());
        groupMod.setName("Director");

        // 3. try to update as verdi, not owner of group 6 - fail
        GroupService groupService2 = clientFactory.create("verdi", ADMIN_PWD).getService(GroupService.class);

        try {
            groupService2.update(groupMod.getKey(), groupMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.UNAUTHORIZED, e.getType().getResponseStatus());
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. update as puccini, owner of group 6 - success
        GroupService groupService3 = clientFactory.create("puccini", ADMIN_PWD).getService(GroupService.class);

        groupTO = groupService3.update(groupMod.getKey(), groupMod).readEntity(GroupTO.class);
        assertEquals("Director", groupTO.getName());

        // issue SYNCOPE-15
        assertNotNull(groupTO.getCreationDate());
        assertNotNull(groupTO.getLastChangeDate());
        assertEquals("admin", groupTO.getCreator());
        assertEquals("puccini", groupTO.getLastModifier());
        assertTrue(groupTO.getCreationDate().before(groupTO.getLastChangeDate()));
    }

    @Test
    public void issue178() {
        GroupTO groupTO = new GroupTO();
        String groupName = "torename" + getUUIDString();
        groupTO.setName(groupName);
        groupTO.setRealm("/");

        GroupTO actual = createGroup(groupTO);

        assertNotNull(actual);
        assertEquals(groupName, actual.getName());

        GroupMod groupMod = new GroupMod();
        groupMod.setKey(actual.getKey());
        String renamedGroup = "renamed" + getUUIDString();
        groupMod.setName(renamedGroup);

        actual = updateGroup(groupMod);
        assertNotNull(actual);
        assertEquals(renamedGroup, actual.getName());
    }

    @Test
    public void unlink() {
        GroupTO actual = createGroup(buildGroupTO("unlink"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey()));

        assertNotNull(groupService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.UNLINK,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey()));
    }

    @Test
    public void link() {
        GroupTO groupTO = buildGroupTO("link");
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(groupService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.LINK,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void unassign() {
        GroupTO actual = createGroup(buildGroupTO("unassign"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey()));

        assertNotNull(groupService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.UNASSIGN,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void assign() {
        GroupTO groupTO = buildGroupTO("assign");
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(groupService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.ASSIGN,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey()));
    }

    @Test
    public void deprovision() {
        GroupTO actual = createGroup(buildGroupTO("deprovision"));
        assertNotNull(actual);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey()));

        assertNotNull(groupService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void provision() {
        GroupTO groupTO = buildGroupTO("assign" + getUUIDString());
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(groupService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.PROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey()));
    }

    @Test
    public void deprovisionUnlinked() {
        GroupTO groupTO = buildGroupTO("assign" + getUUIDString());
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO);
        assertNotNull(actual);

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNotNull(groupService.bulkAssociation(actual.getKey(),
                ResourceAssociationActionType.PROVISION,
                CollectionWrapper.wrap("resource-ldap", ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey()));

        assertNotNull(groupService.bulkDeassociation(actual.getKey(),
                ResourceDeassociationActionType.DEPROVISION,
                CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class)).
                readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.GROUP, actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void createWithMandatorySchemaNotTemplate() {
        // 1. create a group mandatory schema
        PlainSchemaTO badge = new PlainSchemaTO();
        badge.setKey("badge" + getUUIDString());
        badge.setMandatoryCondition("true");
        schemaService.create(AttributableType.GROUP, SchemaType.PLAIN, badge);

        // 2. create a group *without* an attribute for that schema: it works
        GroupTO groupTO = buildGroupTO("lastGroup");
        assertFalse(groupTO.getPlainAttrMap().containsKey(badge.getKey()));
        groupTO = createGroup(groupTO);
        assertNotNull(groupTO);
        assertFalse(groupTO.getPlainAttrMap().containsKey(badge.getKey()));

        // 3. add a template for badge to the group just created - 
        // failure since no values are provided and it is mandatory
        GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupTO.getKey());
        groupMod.setModGAttrTemplates(true);
        groupMod.getGPlainAttrTemplates().add(badge.getKey());

        try {
            updateGroup(groupMod);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        // 4. also add an actual attribute for badge - it will work        
        groupMod.getPlainAttrsToUpdate().add(attrMod(badge.getKey(), "xxxxxxxxxx"));

        groupTO = updateGroup(groupMod);
        assertNotNull(groupTO);
        assertTrue(groupTO.getPlainAttrMap().containsKey(badge.getKey()));
    }

    @Test
    public void anonymous() {
        GroupService unauthenticated = clientFactory.createAnonymous().getService(GroupService.class);
        try {
            unauthenticated.list(SyncopeClient.getSubjectSearchQueryBuilder().realm(SyncopeConstants.ROOT_REALM).build());
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        GroupService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(GroupService.class);
        assertFalse(anonymous.list(SyncopeClient.getSubjectSearchQueryBuilder().realm(SyncopeConstants.ROOT_REALM).build()).
                getResult().isEmpty());
    }

    @Test
    public void noContent() throws IOException {
        SyncopeClient noContentclient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);
        GroupService noContentService = noContentclient.prefer(GroupService.class, Preference.RETURN_NO_CONTENT);

        GroupTO group = buildGroupTO("noContent");

        Response response = noContentService.create(group);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));

        group = getObject(response.getLocation(), GroupService.class, GroupTO.class);
        assertNotNull(group);

        GroupMod groupMod = new GroupMod();
        groupMod.getPlainAttrsToUpdate().add(attrMod("badge", "xxxxxxxxxx"));

        response = noContentService.update(group.getKey(), groupMod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));

        response = noContentService.delete(group.getKey());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Preference.RETURN_NO_CONTENT.toString(), response.getHeaderString(RESTHeaders.PREFERENCE_APPLIED));
        assertEquals(StringUtils.EMPTY, IOUtils.toString((InputStream) response.getEntity()));
    }

    @Test
    public void issueSYNCOPE632() {
        GroupTO groupTO = null;
        try {
            // 1. create new LDAP resource having account id mapped to a derived attribute
            ResourceTO newLDAP = resourceService.read(RESOURCE_NAME_LDAP);
            newLDAP.setKey("new-ldap");
            newLDAP.setPropagationPrimary(true);
            MappingItemTO accountId = newLDAP.getGmapping().getAccountIdItem();
            accountId.setIntMappingType(IntMappingType.GroupDerivedSchema);
            accountId.setIntAttrName("displayProperty");
            newLDAP.getGmapping().setAccountIdItem(accountId);
            newLDAP.getGmapping().setAccountLink("'cn=' + displayProperty + ',ou=groups,o=isp'");

            MappingItemTO description = new MappingItemTO();
            description.setIntMappingType(IntMappingType.GroupId);
            description.setExtAttrName("description");
            description.setPurpose(MappingPurpose.BOTH);
            newLDAP.getGmapping().addItem(description);

            newLDAP = createResource(newLDAP);
            assertNotNull(newLDAP);

            // 2. create a group and give the resource created above
            groupTO = buildGroupTO("lastGroup" + getUUIDString());
            groupTO.getGPlainAttrTemplates().add("icon");
            groupTO.getPlainAttrs().add(attrTO("icon", "anIcon"));
            groupTO.getGPlainAttrTemplates().add("show");
            groupTO.getPlainAttrs().add(attrTO("show", "true"));
            groupTO.getGDerAttrTemplates().add("displayProperty");
            groupTO.getDerAttrs().add(attrTO("displayProperty", null));
            groupTO.getResources().clear();
            groupTO.getResources().add("new-ldap");

            groupTO = createGroup(groupTO);
            assertNotNull(groupTO);

            // 3. update the group
            GroupMod groupMod = new GroupMod();
            groupMod.setKey(groupTO.getKey());
            groupMod.getPlainAttrsToRemove().add("icon");
            groupMod.getPlainAttrsToUpdate().add(attrMod("icon", "anotherIcon"));

            groupTO = updateGroup(groupMod);
            assertNotNull(groupTO);

            // 4. check that a single group exists in LDAP for the group created and updated above
            int entries = 0;
            DirContext ctx = null;
            try {
                ctx = getLdapResourceDirContext(null, null);

                SearchControls ctls = new SearchControls();
                ctls.setReturningAttributes(new String[] { "*", "+" });
                ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

                NamingEnumeration<SearchResult> result =
                        ctx.search("ou=groups,o=isp", "(description=" + groupTO.getKey() + ")", ctls);
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
            if (groupTO != null) {
                groupService.delete(groupTO.getKey());
            }
            resourceService.delete("new-ldap");
        }
    }
}
