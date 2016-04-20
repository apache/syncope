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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class GroupITCase extends AbstractITCase {

    public static GroupTO getBasicSampleTO(final String name) {
        GroupTO groupTO = new GroupTO();
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);
        groupTO.setName(name + getUUIDString());
        return groupTO;
    }

    public static GroupTO getSampleTO(final String name) {
        GroupTO groupTO = getBasicSampleTO(name);

        groupTO.getPlainAttrs().add(attrTO("icon", "anIcon"));

        groupTO.getResources().add(RESOURCE_NAME_LDAP);
        return groupTO;
    }

    @Test
    public void create() {
        GroupTO groupTO = getSampleTO("lastGroup");
        groupTO.getVirAttrs().add(attrTO("rvirtualdata", "rvirtualvalue"));
        groupTO.setGroupOwner("f779c0d4-633b-4be5-8f57-32eb478a3ca5");

        groupTO = createGroup(groupTO).getAny();
        assertNotNull(groupTO);

        assertNotNull(groupTO.getVirAttrMap());
        assertNotNull(groupTO.getVirAttrMap().get("rvirtualdata").getValues());
        assertFalse(groupTO.getVirAttrMap().get("rvirtualdata").getValues().isEmpty());
        assertEquals("rvirtualvalue", groupTO.getVirAttrMap().get("rvirtualdata").getValues().get(0));

        assertTrue(groupTO.getResources().contains(RESOURCE_NAME_LDAP));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getPlainAttrMap().get("owner"));

        // SYNCOPE-515: remove ownership
        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(groupTO.getKey());
        groupPatch.setGroupOwner(new StringReplacePatchItem());

        assertNull(updateGroup(groupPatch).getAny().getGroupOwner());
    }

    @Test
    public void delete() {
        try {
            groupService.delete(UUID.randomUUID().toString());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        GroupTO groupTO = new GroupTO();
        groupTO.setName("toBeDeleted" + getUUIDString());
        groupTO.setRealm("/even");

        groupTO.getResources().add(RESOURCE_NAME_LDAP);

        groupTO = createGroup(groupTO).getAny();
        assertNotNull(groupTO);

        GroupTO deletedGroup = deleteGroup(groupTO.getKey()).getAny();
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
                groupService.list(new AnyListQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build());
        assertNotNull(groupTOs);
        assertTrue(groupTOs.getResult().size() >= 8);
        for (GroupTO groupTO : groupTOs.getResult()) {
            assertNotNull(groupTO);
        }
    }

    @Test
    public void read() {
        GroupTO groupTO = groupService.read("37d15e4c-cdc1-460b-a591-8505c8133806");

        assertNotNull(groupTO);
        assertNotNull(groupTO.getPlainAttrs());
        assertFalse(groupTO.getPlainAttrs().isEmpty());
    }

    @Test
    public void selfRead() {
        UserTO userTO = userService.read("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(userTO);

        assertTrue(userTO.getMembershipMap().containsKey("37d15e4c-cdc1-460b-a591-8505c8133806"));
        assertFalse(userTO.getMembershipMap().containsKey("29f96485-729e-4d31-88a1-6fc60e4677f3"));

        GroupService groupService2 = clientFactory.create("rossini", ADMIN_PWD).getService(GroupService.class);

        try {
            groupService2.read("29f96485-729e-4d31-88a1-6fc60e4677f3");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        List<GroupTO> groups = groupService2.own();
        assertNotNull(groups);
        assertTrue(IterableUtils.matchesAny(groups, new Predicate<GroupTO>() {

            @Override
            public boolean evaluate(final GroupTO group) {
                return "37d15e4c-cdc1-460b-a591-8505c8133806".equals(group.getKey());
            }
        }));
    }

    @Test
    public void update() {
        GroupTO groupTO = getSampleTO("latestGroup" + getUUIDString());
        groupTO = createGroup(groupTO).getAny();

        assertEquals(1, groupTO.getPlainAttrs().size());

        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(groupTO.getKey());
        String modName = "finalGroup" + getUUIDString();
        groupPatch.setName(new StringReplacePatchItem.Builder().value(modName).build());
        groupPatch.getPlainAttrs().add(attrAddReplacePatch("show", "FALSE"));

        groupTO = updateGroup(groupPatch).getAny();

        assertEquals(modName, groupTO.getName());
        assertEquals(2, groupTO.getPlainAttrs().size());

        groupTO.getPlainAttrMap().get("show").getValues().clear();

        groupTO = groupService.update(groupTO).readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
        }).getAny();

        assertFalse(groupTO.getPlainAttrMap().containsKey("show"));
    }

    @Test
    public void patch() {
        GroupTO original = getBasicSampleTO("patch");
        original.setUDynMembershipCond("(($groups==3;$resources!=ws-target-resource-1);aLong==1)");
        original.getADynMembershipConds().put(
                "PRINTER",
                "(($groups==7;cool==ss);$resources==ws-target-resource-2);$type==PRINTER");

        GroupTO updated = createGroup(original).getAny();

        updated.getPlainAttrs().add(new AttrTO.Builder().schema("icon").build());
        updated.getPlainAttrs().add(new AttrTO.Builder().schema("show").build());
        updated.getPlainAttrs().add(new AttrTO.Builder().schema("rderived_sx").value("sx").build());
        updated.getPlainAttrs().add(new AttrTO.Builder().schema("rderived_dx").value("dx").build());
        updated.getPlainAttrs().add(new AttrTO.Builder().schema("title").value("mr").build());

        original = groupService.read(updated.getKey());

        GroupPatch patch = AnyOperations.diff(updated, original, true);
        GroupTO group = updateGroup(patch).getAny();

        Map<String, AttrTO> attrs = group.getPlainAttrMap();
        assertFalse(attrs.containsKey("icon"));
        assertFalse(attrs.containsKey("show"));
        assertEquals(Collections.singletonList("sx"), attrs.get("rderived_sx").getValues());
        assertEquals(Collections.singletonList("dx"), attrs.get("rderived_dx").getValues());
        assertEquals(Collections.singletonList("mr"), attrs.get("title").getValues());
    }

    @Test
    public void updateAsGroupOwner() {
        // 1. read group as admin
        GroupTO groupTO = groupService.read("ebf97068-aa4b-4a85-9f01-680e8c4cf227");

        // issue SYNCOPE-15
        assertNotNull(groupTO.getCreationDate());
        assertNotNull(groupTO.getLastChangeDate());
        assertEquals("admin", groupTO.getCreator());
        assertEquals("admin", groupTO.getLastModifier());

        // 2. prepare update
        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(groupTO.getKey());
        groupPatch.setName(new StringReplacePatchItem.Builder().value("Director").build());

        // 3. try to update as verdi, not owner of group 6 - fail
        GroupService groupService2 = clientFactory.create("verdi", ADMIN_PWD).getService(GroupService.class);

        try {
            groupService2.update(groupPatch);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.UNAUTHORIZED, e.getType().getResponseStatus());
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 4. update as puccini, owner of group 6 - success
        GroupService groupService3 = clientFactory.create("puccini", ADMIN_PWD).getService(GroupService.class);

        groupTO = groupService3.update(groupPatch).readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
        }).getAny();
        assertEquals("Director", groupTO.getName());

        // issue SYNCOPE-15
        assertNotNull(groupTO.getCreationDate());
        assertNotNull(groupTO.getLastChangeDate());
        assertEquals("admin", groupTO.getCreator());
        assertEquals("puccini", groupTO.getLastModifier());
        assertTrue(groupTO.getCreationDate().before(groupTO.getLastChangeDate()));
    }

    @Test
    public void unlink() {
        GroupTO actual = createGroup(getSampleTO("unlink")).getAny();
        assertNotNull(actual);

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
        deassociationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));
    }

    @Test
    public void link() {
        GroupTO groupTO = getSampleTO("link");
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO).getAny();
        assertNotNull(actual);

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setAction(ResourceAssociationAction.LINK);
        associationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void unassign() {
        GroupTO actual = createGroup(getSampleTO("unassign")).getAny();
        assertNotNull(actual);

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.UNASSIGN);
        deassociationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void assign() {
        GroupTO groupTO = getSampleTO("assign");
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO).getAny();
        assertNotNull(actual);

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setAction(ResourceAssociationAction.ASSIGN);
        associationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertFalse(actual.getResources().isEmpty());
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));
    }

    @Test
    public void deprovision() {
        GroupTO actual = createGroup(getSampleTO("deprovision")).getAny();
        assertNotNull(actual);
        assertNotNull(actual.getKey());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.DEPROVISION);
        deassociationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void provision() {
        GroupTO groupTO = getSampleTO("assign" + getUUIDString());
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO).getAny();
        assertNotNull(actual);

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setAction(ResourceAssociationAction.PROVISION);
        associationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));
    }

    @Test
    public void deprovisionUnlinked() {
        GroupTO groupTO = getSampleTO("assign" + getUUIDString());
        groupTO.getResources().clear();

        GroupTO actual = createGroup(groupTO).getAny();
        assertNotNull(actual);

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }

        AssociationPatch associationPatch = new AssociationPatch();
        associationPatch.setKey(actual.getKey());
        associationPatch.setAction(ResourceAssociationAction.PROVISION);
        associationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.associate(associationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));

        DeassociationPatch deassociationPatch = new DeassociationPatch();
        deassociationPatch.setKey(actual.getKey());
        deassociationPatch.setAction(ResourceDeassociationAction.DEPROVISION);
        deassociationPatch.getResources().add(RESOURCE_NAME_LDAP);

        assertNotNull(groupService.deassociate(deassociationPatch).readEntity(BulkActionResult.class));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail();
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void createWithMandatorySchema() {
        // 1. create a mandatory schema
        PlainSchemaTO badge = new PlainSchemaTO();
        badge.setKey("badge" + getUUIDString());
        badge.setMandatoryCondition("true");
        schemaService.create(SchemaType.PLAIN, badge);

        // 2. create a group *without* an attribute for that schema: it works
        GroupTO groupTO = getSampleTO("lastGroup");
        assertFalse(groupTO.getPlainAttrMap().containsKey(badge.getKey()));
        groupTO = createGroup(groupTO).getAny();
        assertNotNull(groupTO);
        assertFalse(groupTO.getPlainAttrMap().containsKey(badge.getKey()));

        // 3. add the new mandatory schema to the default group type
        AnyTypeTO type = anyTypeService.read(AnyTypeKind.GROUP.name());
        String typeClassName = type.getClasses().get(0);
        AnyTypeClassTO typeClass = anyTypeClassService.read(typeClassName);
        typeClass.getPlainSchemas().add(badge.getKey());
        anyTypeClassService.update(typeClass);
        typeClass = anyTypeClassService.read(typeClassName);
        assertTrue(typeClass.getPlainSchemas().contains(badge.getKey()));

        try {
            // 4. update group: failure since no values are provided and it is mandatory
            GroupPatch groupPatch = new GroupPatch();
            groupPatch.setKey(groupTO.getKey());

            try {
                updateGroup(groupPatch);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
            }

            // 5. also add an actual attribute for badge - it will work        
            groupPatch.getPlainAttrs().add(attrAddReplacePatch(badge.getKey(), "xxxxxxxxxx"));

            groupTO = updateGroup(groupPatch).getAny();
            assertNotNull(groupTO);
            assertTrue(groupTO.getPlainAttrMap().containsKey(badge.getKey()));
        } finally {
            // restore the original group class
            typeClass.getPlainSchemas().remove(badge.getKey());
            anyTypeClassService.update(typeClass);
            typeClass = anyTypeClassService.read(typeClassName);
            assertFalse(typeClass.getPlainSchemas().contains(badge.getKey()));
        }
    }

    @Test
    public void anonymous() {
        GroupService unauthenticated = clientFactory.create().getService(GroupService.class);
        try {
            unauthenticated.list(new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build());
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        GroupService anonymous = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).getService(GroupService.class);
        assertFalse(anonymous.list(new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build()).
                getResult().isEmpty());
    }

    @Test
    public void uDynMembership() {
        assertTrue(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynGroups().isEmpty());

        GroupTO group = getBasicSampleTO("uDynMembership");
        group.setUDynMembershipCond("cool==true");
        group = createGroup(group).getAny();
        assertNotNull(group);

        assertTrue(userService.read(
                "c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynGroups().contains(group.getKey()));

        GroupPatch patch = new GroupPatch();
        patch.setKey(group.getKey());
        patch.setUDynMembershipCond("cool==false");
        groupService.update(patch);

        assertTrue(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynGroups().isEmpty());
    }

    @Test
    public void aDynMembership() {
        String fiql = SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").is("location").notNullValue().query();

        // 1. create group with a given aDynMembership condition
        GroupTO group = getBasicSampleTO("aDynMembership");
        group.getADynMembershipConds().put("PRINTER", fiql);
        group = createGroup(group).getAny();
        assertEquals(fiql, group.getADynMembershipConds().get("PRINTER"));

        group = groupService.read(group.getKey());
        assertEquals(fiql, group.getADynMembershipConds().get("PRINTER"));

        // verify that the condition is dynamically applied
        AnyObjectTO newAny = AnyObjectITCase.getSampleTO("aDynMembership");
        newAny.getResources().clear();
        newAny = createAnyObject(newAny).getAny();
        assertNotNull(newAny.getPlainAttrMap().get("location"));
        assertTrue(anyObjectService.read(
                "fc6dbc3a-6c07-4965-8781-921e7401a4a5").getDynGroups().contains(group.getKey()));
        assertTrue(anyObjectService.read(
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").getDynGroups().contains(group.getKey()));
        assertTrue(anyObjectService.read(newAny.getKey()).getDynGroups().contains(group.getKey()));

        // 2. update group and change aDynMembership condition
        fiql = SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").is("location").nullValue().query();

        GroupPatch patch = new GroupPatch();
        patch.setKey(group.getKey());
        patch.getADynMembershipConds().put("PRINTER", fiql);

        group = updateGroup(patch).getAny();
        assertEquals(fiql, group.getADynMembershipConds().get("PRINTER"));

        group = groupService.read(group.getKey());
        assertEquals(fiql, group.getADynMembershipConds().get("PRINTER"));

        // verify that the condition is dynamically applied
        AnyObjectPatch anyPatch = new AnyObjectPatch();
        anyPatch.setKey(newAny.getKey());
        anyPatch.getPlainAttrs().add(new AttrPatch.Builder().
                operation(PatchOperation.DELETE).
                attrTO(new AttrTO.Builder().schema("location").build()).
                build());
        newAny = updateAnyObject(anyPatch).getAny();
        assertNull(newAny.getPlainAttrMap().get("location"));
        assertFalse(anyObjectService.read(
                "fc6dbc3a-6c07-4965-8781-921e7401a4a5").getDynGroups().contains(group.getKey()));
        assertFalse(anyObjectService.read(
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").getDynGroups().contains(group.getKey()));
        assertTrue(anyObjectService.read(newAny.getKey()).getDynGroups().contains(group.getKey()));
    }

    @Test
    public void capabilitiesOverride() {
        // resource with no capability override
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(ldap);
        assertFalse(ldap.isOverrideCapabilities());
        assertTrue(ldap.getCapabilitiesOverride().isEmpty());

        // connector with all required for create and update
        ConnInstanceTO conn = connectorService.read(ldap.getConnector(), null);
        assertNotNull(conn);
        assertTrue(conn.getCapabilities().contains(ConnectorCapability.CREATE));
        assertTrue(conn.getCapabilities().contains(ConnectorCapability.UPDATE));

        try {
            // 1. create succeeds
            GroupTO group = getSampleTO("syncope714");
            group.getPlainAttrs().add(attrTO("title", "first"));
            group.getResources().add(RESOURCE_NAME_LDAP);

            ProvisioningResult<GroupTO> result = createGroup(group);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            group = result.getAny();

            // 2. update succeeds
            GroupPatch patch = new GroupPatch();
            patch.setKey(group.getKey());
            patch.getPlainAttrs().add(new AttrPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).attrTO(attrTO("title", "second")).build());

            result = updateGroup(patch);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            group = result.getAny();

            // 3. set capability override with only search allowed, but not enable
            ldap.getCapabilitiesOverride().add(ConnectorCapability.SEARCH);
            resourceService.update(ldap);
            ldap = resourceService.read(RESOURCE_NAME_LDAP);
            assertNotNull(ldap);
            assertFalse(ldap.isOverrideCapabilities());
            assertEquals(1, ldap.getCapabilitiesOverride().size());
            assertTrue(ldap.getCapabilitiesOverride().contains(ConnectorCapability.SEARCH));

            // 4. update succeeds again
            patch = new GroupPatch();
            patch.setKey(group.getKey());
            patch.getPlainAttrs().add(new AttrPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).attrTO(attrTO("title", "third")).build());

            result = updateGroup(patch);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            group = result.getAny();

            // 5. enable capability override
            ldap.setOverrideCapabilities(true);
            resourceService.update(ldap);
            ldap = resourceService.read(RESOURCE_NAME_LDAP);
            assertNotNull(ldap);
            assertTrue(ldap.isOverrideCapabilities());
            assertEquals(1, ldap.getCapabilitiesOverride().size());
            assertTrue(ldap.getCapabilitiesOverride().contains(ConnectorCapability.SEARCH));

            // 6. update now fails
            patch = new GroupPatch();
            patch.setKey(group.getKey());
            patch.getPlainAttrs().add(new AttrPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).attrTO(attrTO("title", "fourth")).build());

            result = updateGroup(patch);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(PropagationTaskExecStatus.NOT_ATTEMPTED, result.getPropagationStatuses().get(0).getStatus());
        } finally {
            ldap.getCapabilitiesOverride().clear();
            ldap.setOverrideCapabilities(false);
            resourceService.update(ldap);
        }
    }

    @Test
    public void typeExtensions() {
        TypeExtensionTO typeExtension = new TypeExtensionTO();
        typeExtension.setAnyType(AnyTypeKind.USER.name());
        typeExtension.getAuxClasses().add("csv");

        GroupTO groupTO = getBasicSampleTO("typeExtensions");
        groupTO.getTypeExtensions().add(typeExtension);

        groupTO = createGroup(groupTO).getAny();
        assertNotNull(groupTO);
        assertEquals(1, groupTO.getTypeExtensions().size());
        assertEquals(1, groupTO.getTypeExtension(AnyTypeKind.USER.name()).getAuxClasses().size());
        assertTrue(groupTO.getTypeExtension(AnyTypeKind.USER.name()).getAuxClasses().contains("csv"));

        typeExtension = new TypeExtensionTO();
        typeExtension.setAnyType(AnyTypeKind.USER.name());
        typeExtension.getAuxClasses().add("csv");
        typeExtension.getAuxClasses().add("other");

        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(groupTO.getKey());
        groupPatch.getTypeExtensions().add(typeExtension);

        groupTO = updateGroup(groupPatch).getAny();
        assertNotNull(groupTO);
        assertEquals(1, groupTO.getTypeExtensions().size());
        assertEquals(2, groupTO.getTypeExtension(AnyTypeKind.USER.name()).getAuxClasses().size());
        assertTrue(groupTO.getTypeExtension(AnyTypeKind.USER.name()).getAuxClasses().contains("csv"));
        assertTrue(groupTO.getTypeExtension(AnyTypeKind.USER.name()).getAuxClasses().contains("other"));
    }

    @Test
    public void issue178() {
        GroupTO groupTO = new GroupTO();
        String groupName = "torename" + getUUIDString();
        groupTO.setName(groupName);
        groupTO.setRealm("/");

        GroupTO actual = createGroup(groupTO).getAny();

        assertNotNull(actual);
        assertEquals(groupName, actual.getName());

        GroupPatch groupPatch = new GroupPatch();
        groupPatch.setKey(actual.getKey());
        String renamedGroup = "renamed" + getUUIDString();
        groupPatch.setName(new StringReplacePatchItem.Builder().value(renamedGroup).build());

        actual = updateGroup(groupPatch).getAny();
        assertNotNull(actual);
        assertEquals(renamedGroup, actual.getName());
    }

    @Test
    public void issueSYNCOPE632() {
        GroupTO groupTO = null;
        try {
            // 1. create new LDAP resource having ConnObjectKey mapped to a derived attribute
            ResourceTO newLDAP = resourceService.read(RESOURCE_NAME_LDAP);
            newLDAP.setKey("new-ldap");
            newLDAP.setPropagationPriority(0);

            for (ProvisionTO provision : newLDAP.getProvisions()) {
                provision.getVirSchemas().clear();
            }

            MappingTO mapping = newLDAP.getProvision(AnyTypeKind.GROUP.name()).getMapping();

            MappingItemTO connObjectKey = mapping.getConnObjectKeyItem();
            connObjectKey.setIntMappingType(IntMappingType.GroupDerivedSchema);
            connObjectKey.setIntAttrName("displayProperty");
            connObjectKey.setPurpose(MappingPurpose.PROPAGATION);
            mapping.setConnObjectKeyItem(connObjectKey);
            mapping.setConnObjectLink("'cn=' + displayProperty + ',ou=groups,o=isp'");

            MappingItemTO description = new MappingItemTO();
            description.setIntMappingType(IntMappingType.GroupKey);
            description.setExtAttrName("description");
            description.setPurpose(MappingPurpose.BOTH);
            mapping.add(description);

            newLDAP = createResource(newLDAP);
            assertNotNull(newLDAP);

            // 2. create a group and give the resource created above
            groupTO = getSampleTO("lastGroup" + getUUIDString());
            groupTO.getPlainAttrs().add(attrTO("icon", "anIcon"));
            groupTO.getPlainAttrs().add(attrTO("show", "true"));
            groupTO.getDerAttrs().add(attrTO("displayProperty", null));
            groupTO.getResources().clear();
            groupTO.getResources().add("new-ldap");

            groupTO = createGroup(groupTO).getAny();
            assertNotNull(groupTO);

            // 3. update the group
            GroupPatch groupPatch = new GroupPatch();
            groupPatch.setKey(groupTO.getKey());
            groupPatch.getPlainAttrs().add(attrAddReplacePatch("icon", "anotherIcon"));

            groupTO = updateGroup(groupPatch).getAny();
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

    @Test
    public void issueSYNCOPE717() {
        String doubleSchemaName = "double" + getUUIDString();

        // 1. create double schema without conversion pattern
        PlainSchemaTO schema = new PlainSchemaTO();
        schema.setKey(doubleSchemaName);
        schema.setType(AttrSchemaType.Double);

        schema = createSchema(SchemaType.PLAIN, schema);
        assertNotNull(schema);
        assertNull(schema.getConversionPattern());

        AnyTypeClassTO minimalGroup = anyTypeClassService.read("minimal group");
        assertNotNull(minimalGroup);
        minimalGroup.getPlainSchemas().add(doubleSchemaName);
        anyTypeClassService.update(minimalGroup);

        // 2. create group, provide valid input value
        GroupTO groupTO = getBasicSampleTO("syncope717");
        groupTO.getPlainAttrs().add(attrTO(doubleSchemaName, "11.23"));

        groupTO = createGroup(groupTO).getAny();
        assertNotNull(groupTO);
        assertEquals("11.23", groupTO.getPlainAttrMap().get(doubleSchemaName).getValues().get(0));

        // 3. update schema, set conversion pattern
        schema.setConversionPattern("0.000");
        schemaService.update(SchemaType.PLAIN, schema);

        // 4. re-read group, verify that pattern was applied
        groupTO = groupService.read(groupTO.getKey());
        assertNotNull(groupTO);
        assertEquals("11.230", groupTO.getPlainAttrMap().get(doubleSchemaName).getValues().get(0));

        // 5. modify group with new double value
        GroupPatch patch = new GroupPatch();
        patch.setKey(groupTO.getKey());
        patch.getPlainAttrs().add(new AttrPatch.Builder().attrTO(attrTO(doubleSchemaName, "11.257")).build());

        groupTO = updateGroup(patch).getAny();
        assertNotNull(groupTO);
        assertEquals("11.257", groupTO.getPlainAttrMap().get(doubleSchemaName).getValues().get(0));

        // 6. update schema, unset conversion pattern
        schema.setConversionPattern(null);
        schemaService.update(SchemaType.PLAIN, schema);

        // 7. modify group with new double value, verify that no pattern is applied
        patch = new GroupPatch();
        patch.setKey(groupTO.getKey());
        patch.getPlainAttrs().add(new AttrPatch.Builder().attrTO(attrTO(doubleSchemaName, "11.23")).build());

        groupTO = updateGroup(patch).getAny();
        assertNotNull(groupTO);
        assertEquals("11.23", groupTO.getPlainAttrMap().get(doubleSchemaName).getValues().get(0));
    }

}
