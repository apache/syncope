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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.security.AccessControlException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.ResourceAR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ElasticsearchDetector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GroupITCase extends AbstractITCase {

    public static GroupCR getBasicSample(final String name) {
        return new GroupCR.Builder(SyncopeConstants.ROOT_REALM, name + getUUIDString()).build();
    }

    public static GroupCR getSample(final String name) {
        GroupCR groupCR = getBasicSample(name);

        groupCR.getPlainAttrs().add(attr("icon", "anIcon"));

        groupCR.getResources().add(RESOURCE_NAME_LDAP);
        return groupCR;
    }

    @Test
    public void create() {
        GroupCR groupCR = getSample("lastGroup");
        groupCR.getVirAttrs().add(attr("rvirtualdata", "rvirtualvalue"));
        groupCR.setGroupOwner("f779c0d4-633b-4be5-8f57-32eb478a3ca5");

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);

        assertNotNull(groupTO.getVirAttr("rvirtualdata").get().getValues());
        assertFalse(groupTO.getVirAttr("rvirtualdata").get().getValues().isEmpty());
        assertEquals("rvirtualvalue", groupTO.getVirAttr("rvirtualdata").get().getValues().get(0));

        assertTrue(groupTO.getResources().contains(RESOURCE_NAME_LDAP));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObjectTO);
        assertNotNull(connObjectTO.getAttr("owner"));

        // SYNCOPE-515: remove ownership
        GroupUR groupUR = new GroupUR();
        groupUR.setKey(groupTO.getKey());
        groupUR.setGroupOwner(new StringReplacePatchItem());

        assertNull(updateGroup(groupUR).getEntity().getGroupOwner());
    }

    @Test
    public void createWithInternationalCharacters() {
        GroupCR groupCR = getSample("räksmörgås");

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);
    }

    @Test
    public void delete() {
        try {
            groupService.delete(UUID.randomUUID().toString());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }

        GroupCR groupCR = new GroupCR();
        groupCR.setName("toBeDeleted" + getUUIDString());
        groupCR.setRealm("/even");

        groupCR.getResources().add(RESOURCE_NAME_LDAP);

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);

        GroupTO deletedGroup = deleteGroup(groupTO.getKey()).getEntity();
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
                groupService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).build());
        assertNotNull(groupTOs);
        assertTrue(groupTOs.getResult().size() >= 8);
        groupTOs.getResult().forEach(Assertions::assertNotNull);
    }

    @Test
    public void read() {
        GroupTO groupTO = groupService.read("37d15e4c-cdc1-460b-a591-8505c8133806");

        assertNotNull(groupTO);
        assertNotNull(groupTO.getPlainAttrs());
        assertFalse(groupTO.getPlainAttrs().isEmpty());
        assertEquals(2, groupTO.getStaticUserMembershipCount());
    }

    @Test
    public void selfRead() {
        UserTO userTO = userService.read("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertNotNull(userTO);

        assertTrue(userTO.getMembership("37d15e4c-cdc1-460b-a591-8505c8133806").isPresent());
        assertFalse(userTO.getMembership("29f96485-729e-4d31-88a1-6fc60e4677f3").isPresent());

        GroupService groupService2 = clientFactory.create("rossini", ADMIN_PWD).getService(GroupService.class);

        try {
            groupService2.read("29f96485-729e-4d31-88a1-6fc60e4677f3");
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }

        List<GroupTO> groups = groupService2.own();
        assertNotNull(groups);
        assertTrue(groups.stream().anyMatch(group -> "37d15e4c-cdc1-460b-a591-8505c8133806".equals(group.getKey())));
    }

    @Test
    public void update() {
        GroupCR groupCR = getSample("latestGroup" + getUUIDString());
        GroupTO groupTO = createGroup(groupCR).getEntity();

        assertEquals(1, groupTO.getPlainAttrs().size());

        GroupUR groupUR = new GroupUR();
        groupUR.setKey(groupTO.getKey());
        String modName = "finalGroup" + getUUIDString();
        groupUR.setName(new StringReplacePatchItem.Builder().value(modName).build());
        groupUR.getPlainAttrs().add(attrAddReplacePatch("show", "FALSE"));

        groupTO = updateGroup(groupUR).getEntity();

        assertEquals(modName, groupTO.getName());
        assertEquals(2, groupTO.getPlainAttrs().size());

        groupTO.getPlainAttr("show").get().getValues().clear();

        groupUR = new GroupUR.Builder(groupTO.getKey()).
                plainAttr(new AttrPatch.Builder(new Attr.Builder("show").build()).
                        operation(PatchOperation.DELETE).build()).build();

        groupTO = updateGroup(groupUR).getEntity();

        assertFalse(groupTO.getPlainAttr("show").isPresent());
    }

    @Test
    public void patch() {
        GroupCR createReq = getBasicSample("patch");
        createReq.setUDynMembershipCond("(($groups==3;$resources!=ws-target-resource-1);aLong==1)");
        createReq.getADynMembershipConds().put(
                PRINTER,
                "(($groups==7;cool==ss);$resources==ws-target-resource-2);$type==PRINTER");

        GroupTO created = createGroup(createReq).getEntity();

        created.getPlainAttrs().add(new Attr.Builder("icon").build());
        created.getPlainAttrs().add(new Attr.Builder("show").build());
        created.getPlainAttrs().add(new Attr.Builder("rderived_sx").value("sx").build());
        created.getPlainAttrs().add(new Attr.Builder("rderived_dx").value("dx").build());
        created.getPlainAttrs().add(new Attr.Builder("title").value("mr").build());

        GroupTO original = groupService.read(created.getKey());

        GroupUR groupUR = AnyOperations.diff(created, original, true);
        GroupTO updated = updateGroup(groupUR).getEntity();

        Map<String, Attr> attrs = EntityTOUtils.buildAttrMap(updated.getPlainAttrs());
        assertFalse(attrs.containsKey("icon"));
        assertFalse(attrs.containsKey("show"));
        assertEquals(List.of("sx"), attrs.get("rderived_sx").getValues());
        assertEquals(List.of("dx"), attrs.get("rderived_dx").getValues());
        assertEquals(List.of("mr"), attrs.get("title").getValues());
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
        GroupUR groupUR = new GroupUR();
        groupUR.setKey(groupTO.getKey());
        groupUR.setName(new StringReplacePatchItem.Builder().value("Director").build());

        // 3. try to update as verdi, not owner of group 6 - fail
        GroupService groupService2 = clientFactory.create("verdi", ADMIN_PWD).getService(GroupService.class);

        try {
            groupService2.update(groupUR);
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        // 4. update as puccini, owner of group 6 - success
        GroupService groupService3 = clientFactory.create("puccini", ADMIN_PWD).getService(GroupService.class);

        groupTO = groupService3.update(groupUR).readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
        }).getEntity();
        assertEquals("Director", groupTO.getName());

        // issue SYNCOPE-15
        assertNotNull(groupTO.getCreationDate());
        assertNotNull(groupTO.getLastChangeDate());
        assertEquals("admin", groupTO.getCreator());
        assertEquals("puccini", groupTO.getLastModifier());
        assertTrue(groupTO.getCreationDate().before(groupTO.getLastChangeDate()));
    }

    @Test
    public void unlink() throws IOException {
        GroupTO actual = createGroup(getSample("unlink")).getEntity();
        assertNotNull(actual);

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));

        ResourceDR resourceDR = new ResourceDR.Builder().key(actual.getKey()).
                action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_LDAP).build();

        assertNotNull(parseBatchResponse(groupService.deassociate(resourceDR)));

        actual = groupService.read(actual.getKey());
        assertNotNull(actual);
        assertTrue(actual.getResources().isEmpty());

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey()));
    }

    @Test
    public void link() throws IOException {
        GroupCR groupCR = getSample("link");
        groupCR.getResources().clear();

        GroupTO actual = createGroup(groupCR).getEntity();
        assertNotNull(actual);

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        ResourceAR resourceAR = new ResourceAR.Builder().key(actual.getKey()).
                action(ResourceAssociationAction.LINK).resource(RESOURCE_NAME_LDAP).build();

        assertNotNull(parseBatchResponse(groupService.associate(resourceAR)));

        actual = groupService.read(actual.getKey());
        assertFalse(actual.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), actual.getKey());
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void unassign() throws IOException {
        GroupTO groupTO = null;

        try {
            groupTO = createGroup(getSample("unassign")).getEntity();
            assertNotNull(groupTO);

            assertNotNull(resourceService.readConnObject(
                    RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey()));

            ResourceDR resourceDR = new ResourceDR();
            resourceDR.setKey(groupTO.getKey());
            resourceDR.setAction(ResourceDeassociationAction.UNASSIGN);
            resourceDR.getResources().add(RESOURCE_NAME_LDAP);

            assertNotNull(parseBatchResponse(groupService.deassociate(resourceDR)));

            groupTO = groupService.read(groupTO.getKey());
            assertNotNull(groupTO);
            assertTrue(groupTO.getResources().isEmpty());

            try {
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
                fail("This should not happen");
            } catch (Exception e) {
                assertNotNull(e);
            }
        } finally {
            if (groupTO != null) {
                groupService.delete(groupTO.getKey());
            }
        }
    }

    @Test
    public void assign() throws IOException {
        GroupCR groupCR = getSample("assign");
        groupCR.getResources().clear();

        GroupTO groupTO = null;
        try {
            groupTO = createGroup(groupCR).getEntity();
            assertNotNull(groupTO);

            try {
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
                fail("This should not happen");
            } catch (Exception e) {
                assertNotNull(e);
            }

            ResourceAR resourceAR = new ResourceAR.Builder().key(groupTO.getKey()).
                    action(ResourceAssociationAction.ASSIGN).resource(RESOURCE_NAME_LDAP).build();

            assertNotNull(parseBatchResponse(groupService.associate(resourceAR)));

            groupTO = groupService.read(groupTO.getKey());
            assertFalse(groupTO.getResources().isEmpty());
            assertNotNull(resourceService.readConnObject(
                    RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey()));
        } finally {
            if (groupTO != null) {
                groupService.delete(groupTO.getKey());
            }
        }
    }

    @Test
    public void deprovision() throws IOException {
        GroupTO groupTO = null;

        try {
            groupTO = createGroup(getSample("deprovision")).getEntity();
            assertNotNull(groupTO);
            assertNotNull(groupTO.getKey());

            assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey()));

            ResourceDR resourceDR = new ResourceDR.Builder().key(groupTO.getKey()).
                    action(ResourceDeassociationAction.DEPROVISION).resource(RESOURCE_NAME_LDAP).build();

            assertNotNull(parseBatchResponse(groupService.deassociate(resourceDR)));

            groupTO = groupService.read(groupTO.getKey());
            assertNotNull(groupTO);
            assertFalse(groupTO.getResources().isEmpty());

            try {
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
                fail("This should not happen");
            } catch (Exception e) {
                assertNotNull(e);
            }
        } finally {
            if (groupTO != null) {
                groupService.delete(groupTO.getKey());
            }
        }
    }

    @Test
    public void provision() throws IOException {
        GroupCR groupCR = getSample("provision");
        groupCR.getResources().clear();

        GroupTO groupTO = null;
        try {
            groupTO = createGroup(groupCR).getEntity();
            assertNotNull(groupTO);

            try {
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
                fail("This should not happen");
            } catch (Exception e) {
                assertNotNull(e);
            }

            ResourceAR resourceAR = new ResourceAR.Builder().key(groupTO.getKey()).
                    action(ResourceAssociationAction.PROVISION).resource(RESOURCE_NAME_LDAP).build();

            assertNotNull(parseBatchResponse(groupService.associate(resourceAR)));

            groupTO = groupService.read(groupTO.getKey());
            assertTrue(groupTO.getResources().isEmpty());

            assertNotNull(resourceService.readConnObject(
                    RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey()));
        } finally {
            if (groupTO != null) {
                groupService.delete(groupTO.getKey());
            }
        }
    }

    @Test
    public void deprovisionUnlinked() throws IOException {
        GroupCR groupCR = getSample("deprovision");
        groupCR.getResources().clear();

        GroupTO groupTO = null;
        try {
            groupTO = createGroup(groupCR).getEntity();
            assertNotNull(groupTO);

            try {
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
                fail("This should not happen");
            } catch (Exception e) {
                assertNotNull(e);
            }

            ResourceAR resourceAR = new ResourceAR.Builder().key(groupTO.getKey()).
                    action(ResourceAssociationAction.PROVISION).resource(RESOURCE_NAME_LDAP).build();

            assertNotNull(parseBatchResponse(groupService.associate(resourceAR)));

            groupTO = groupService.read(groupTO.getKey());
            assertTrue(groupTO.getResources().isEmpty());

            assertNotNull(resourceService.readConnObject(
                    RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey()));

            ResourceDR resourceDR = new ResourceDR.Builder().key(groupTO.getKey()).
                    action(ResourceDeassociationAction.DEPROVISION).resource(RESOURCE_NAME_LDAP).build();

            assertNotNull(parseBatchResponse(groupService.deassociate(resourceDR)));

            groupTO = groupService.read(groupTO.getKey());
            assertNotNull(groupTO);
            assertTrue(groupTO.getResources().isEmpty());

            try {
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
                fail("This should not happen");
            } catch (Exception e) {
                assertNotNull(e);
            }
        } finally {
            if (groupTO != null) {
                groupService.delete(groupTO.getKey());
            }
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
        GroupCR groupCR = getSample("lastGroup");
        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);
        assertFalse(groupTO.getPlainAttr(badge.getKey()).isPresent());

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
            GroupUR groupUR = new GroupUR();
            groupUR.setKey(groupTO.getKey());

            try {
                updateGroup(groupUR);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
            }

            // 5. also add an actual attribute for badge - it will work
            groupUR.getPlainAttrs().add(attrAddReplacePatch(badge.getKey(), "xxxxxxxxxx"));

            groupTO = updateGroup(groupUR).getEntity();
            assertNotNull(groupTO);
            assertNotNull(groupTO.getPlainAttr(badge.getKey()));
        } finally {
            // restore the original group class
            typeClass.getPlainSchemas().remove(badge.getKey());
            anyTypeClassService.update(typeClass);
            typeClass = anyTypeClassService.read(typeClassName);
            assertFalse(typeClass.getPlainSchemas().contains(badge.getKey()));
        }
    }

    @Test
    public void encrypted() throws Exception {
        // 1. create encrypted schema with secret key as system property
        PlainSchemaTO encrypted = new PlainSchemaTO();
        encrypted.setKey("encrypted" + getUUIDString());
        encrypted.setType(AttrSchemaType.Encrypted);
        encrypted.setCipherAlgorithm(CipherAlgorithm.SHA512);
        encrypted.setSecretKey("${obscureSecretKey}");
        schemaService.create(SchemaType.PLAIN, encrypted);

        // 2. add the new schema to the default group type
        AnyTypeTO type = anyTypeService.read(AnyTypeKind.GROUP.name());
        String typeClassName = type.getClasses().get(0);
        AnyTypeClassTO typeClass = anyTypeClassService.read(typeClassName);
        typeClass.getPlainSchemas().add(encrypted.getKey());
        anyTypeClassService.update(typeClass);
        typeClass = anyTypeClassService.read(typeClassName);
        assertTrue(typeClass.getPlainSchemas().contains(encrypted.getKey()));

        // 3. create group, verify that the correct encrypted value is returned
        GroupCR groupCR = getSample("encrypted");
        groupCR.getPlainAttrs().add(new Attr.Builder(encrypted.getKey()).value("testvalue").build());
        GroupTO group = createGroup(groupCR).getEntity();

        assertEquals(Encryptor.getInstance(System.getProperty("obscureSecretKey")).
                encode("testvalue", encrypted.getCipherAlgorithm()),
                group.getPlainAttr(encrypted.getKey()).get().getValues().get(0));

        // 4. update schema to return cleartext values
        encrypted.setAnyTypeClass(typeClassName);
        encrypted.setCipherAlgorithm(CipherAlgorithm.AES);
        encrypted.setConversionPattern(SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN);
        schemaService.update(SchemaType.PLAIN, encrypted);

        // 5. update group, verify that the cleartext value is returned
        GroupUR groupUR = new GroupUR();
        groupUR.setKey(group.getKey());
        groupUR.getPlainAttrs().add(new AttrPatch.Builder(
                new Attr.Builder(encrypted.getKey()).value("testvalue").build()).build());
        group = updateGroup(groupUR).getEntity();

        assertEquals("testvalue", group.getPlainAttr(encrypted.getKey()).get().getValues().get(0));

        // 6. update schema again to disallow cleartext values
        encrypted.setConversionPattern(null);
        schemaService.update(SchemaType.PLAIN, encrypted);

        group = groupService.read(group.getKey());
        assertNotEquals("testvalue", group.getPlainAttr(encrypted.getKey()).get().getValues().get(0));
    }

    @Test
    public void anonymous() {
        GroupService unauthenticated = clientFactory.create().getService(GroupService.class);
        try {
            unauthenticated.search(new AnyQuery.Builder().realm("/even").build());
            fail("This should not happen");
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        try {
            anonymous.getService(GroupService.class).
                    search(new AnyQuery.Builder().realm("/even").build());
            fail("This should not happen");
        } catch (ForbiddenException e) {
            assertNotNull(e);
        }

        assertFalse(anonymous.getService(SyncopeService.class).
                searchAssignableGroups("/even", null, 1, 100).getResult().isEmpty());
    }

    @Test
    public void uDynMembership() {
        assertTrue(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynMemberships().isEmpty());

        GroupCR groupCR = getBasicSample("uDynMembership");
        groupCR.setUDynMembershipCond("cool==true");
        GroupTO group = createGroup(groupCR).getEntity();
        assertNotNull(group);
        final String groupKey = group.getKey();

        List<MembershipTO> memberships = userService.read(
                "c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynMemberships();
        assertTrue(memberships.stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));
        assertEquals(1, groupService.read(group.getKey()).getDynamicUserMembershipCount());

        GroupUR groupUR = new GroupUR();
        groupUR.setKey(group.getKey());
        groupUR.setUDynMembershipCond("cool==false");
        groupService.update(groupUR);

        assertTrue(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").getDynMemberships().isEmpty());
        assertEquals(0, groupService.read(group.getKey()).getDynamicUserMembershipCount());
    }

    @Test
    public void aDynMembership() {
        String fiql = SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).is("location").notNullValue().query();

        // 1. create group with a given aDynMembership condition
        GroupCR groupCR = getBasicSample("aDynMembership");
        groupCR.getADynMembershipConds().put(PRINTER, fiql);
        GroupTO group = createGroup(groupCR).getEntity();
        assertEquals(fiql, group.getADynMembershipConds().get(PRINTER));

        if (ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform())) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        group = groupService.read(group.getKey());
        String groupKey = group.getKey();
        assertEquals(fiql, group.getADynMembershipConds().get(PRINTER));

        // verify that the condition is dynamically applied
        AnyObjectCR newAnyCR = AnyObjectITCase.getSample("aDynMembership");
        newAnyCR.getResources().clear();
        AnyObjectTO newAny = createAnyObject(newAnyCR).getEntity();
        assertNotNull(newAny.getPlainAttr("location"));
        List<MembershipTO> memberships = anyObjectService.read(
                "fc6dbc3a-6c07-4965-8781-921e7401a4a5").getDynMemberships();
        assertTrue(memberships.stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));

        memberships = anyObjectService.read(
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").getDynMemberships();
        assertTrue(memberships.stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));

        memberships = anyObjectService.read(newAny.getKey()).getDynMemberships();
        assertTrue(memberships.stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));

        // 2. update group and change aDynMembership condition
        fiql = SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).is("location").nullValue().query();

        GroupUR groupUR = new GroupUR();
        groupUR.setKey(group.getKey());
        groupUR.getADynMembershipConds().put(PRINTER, fiql);

        group = updateGroup(groupUR).getEntity();
        assertEquals(fiql, group.getADynMembershipConds().get(PRINTER));

        group = groupService.read(group.getKey());
        assertEquals(fiql, group.getADynMembershipConds().get(PRINTER));

        // verify that the condition is dynamically applied
        AnyObjectUR anyObjectUR = new AnyObjectUR();
        anyObjectUR.setKey(newAny.getKey());
        anyObjectUR.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder("location").build()).
                operation(PatchOperation.DELETE).
                build());
        newAny = updateAnyObject(anyObjectUR).getEntity();
        assertFalse(newAny.getPlainAttr("location").isPresent());

        memberships = anyObjectService.read(
                "fc6dbc3a-6c07-4965-8781-921e7401a4a5").getDynMemberships();
        assertFalse(memberships.stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));
        memberships = anyObjectService.read(
                "8559d14d-58c2-46eb-a2d4-a7d35161e8f8").getDynMemberships();
        assertFalse(memberships.stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));
        memberships = anyObjectService.read(newAny.getKey()).getDynMemberships();
        assertTrue(memberships.stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));
    }

    @Test
    public void aDynMembershipCount() {
        // Create a new printer as a dynamic member of a new group
        GroupCR groupCR = getBasicSample("aDynamicMembership");
        String fiql = SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).is("location").equalTo("home").query();
        groupCR.getADynMembershipConds().put(PRINTER, fiql);
        GroupTO group = createGroup(groupCR).getEntity();

        AnyObjectCR printerCR = new AnyObjectCR();
        printerCR.setRealm(SyncopeConstants.ROOT_REALM);
        printerCR.setName("Printer_" + getUUIDString());
        printerCR.setType(PRINTER);
        printerCR.getPlainAttrs().add(new Attr.Builder("location").value("home").build());
        AnyObjectTO printer = createAnyObject(printerCR).getEntity();

        group = groupService.read(group.getKey());
        assertEquals(0, group.getStaticAnyObjectMembershipCount());
        assertEquals(1, group.getDynamicAnyObjectMembershipCount());

        anyObjectService.delete(printer.getKey());
        groupService.delete(group.getKey());
    }

    @Test
    public void aStaticMembershipCount() {
        // Create a new printer as a static member of a new group
        GroupCR groupCR = getBasicSample("aStaticMembership");
        GroupTO group = createGroup(groupCR).getEntity();

        AnyObjectCR printerCR = new AnyObjectCR();
        printerCR.setRealm(SyncopeConstants.ROOT_REALM);
        printerCR.setName("Printer_" + getUUIDString());
        printerCR.setType(PRINTER);
        printerCR.getMemberships().add(new MembershipTO.Builder(group.getKey()).build());
        AnyObjectTO printer = createAnyObject(printerCR).getEntity();

        group = groupService.read(group.getKey());
        assertEquals(0, group.getDynamicAnyObjectMembershipCount());
        assertEquals(1, group.getStaticAnyObjectMembershipCount());

        anyObjectService.delete(printer.getKey());
        groupService.delete(group.getKey());
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
            GroupCR groupCR = getSample("syncope714");
            groupCR.getPlainAttrs().add(attr("title", "first"));
            groupCR.getResources().add(RESOURCE_NAME_LDAP);

            ProvisioningResult<GroupTO> result = createGroup(groupCR);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            GroupTO group = result.getEntity();

            // 2. update succeeds
            GroupUR groupUR = new GroupUR();
            groupUR.setKey(group.getKey());
            groupUR.getPlainAttrs().add(new AttrPatch.Builder(attr("title", "second")).
                    operation(PatchOperation.ADD_REPLACE).build());

            result = updateGroup(groupUR);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            group = result.getEntity();

            // 3. set capability override with only search allowed, but not enable
            ldap.getCapabilitiesOverride().add(ConnectorCapability.SEARCH);
            resourceService.update(ldap);
            ldap = resourceService.read(RESOURCE_NAME_LDAP);
            assertNotNull(ldap);
            assertFalse(ldap.isOverrideCapabilities());
            assertEquals(1, ldap.getCapabilitiesOverride().size());
            assertTrue(ldap.getCapabilitiesOverride().contains(ConnectorCapability.SEARCH));

            // 4. update succeeds again
            groupUR = new GroupUR();
            groupUR.setKey(group.getKey());
            groupUR.getPlainAttrs().add(new AttrPatch.Builder(attr("title", "third")).
                    operation(PatchOperation.ADD_REPLACE).build());

            result = updateGroup(groupUR);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            group = result.getEntity();

            // 5. enable capability override
            ldap.setOverrideCapabilities(true);
            resourceService.update(ldap);
            ldap = resourceService.read(RESOURCE_NAME_LDAP);
            assertNotNull(ldap);
            assertTrue(ldap.isOverrideCapabilities());
            assertEquals(1, ldap.getCapabilitiesOverride().size());
            assertTrue(ldap.getCapabilitiesOverride().contains(ConnectorCapability.SEARCH));

            // 6. update now fails
            groupUR = new GroupUR();
            groupUR.setKey(group.getKey());
            groupUR.getPlainAttrs().add(new AttrPatch.Builder(attr("title", "fourth")).
                    operation(PatchOperation.ADD_REPLACE).build());

            result = updateGroup(groupUR);
            assertNotNull(result);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
            assertEquals(ExecStatus.NOT_ATTEMPTED, result.getPropagationStatuses().get(0).getStatus());
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

        GroupCR groupCR = getBasicSample("typeExtensions");
        groupCR.getTypeExtensions().add(typeExtension);

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);
        assertEquals(1, groupTO.getTypeExtensions().size());
        assertEquals(1, groupTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().size());
        assertTrue(groupTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().contains("csv"));

        typeExtension = new TypeExtensionTO();
        typeExtension.setAnyType(AnyTypeKind.USER.name());
        typeExtension.getAuxClasses().add("csv");
        typeExtension.getAuxClasses().add("other");

        GroupUR groupUR = new GroupUR();
        groupUR.setKey(groupTO.getKey());
        groupUR.getTypeExtensions().add(typeExtension);

        groupTO = updateGroup(groupUR).getEntity();
        assertNotNull(groupTO);
        assertEquals(1, groupTO.getTypeExtensions().size());
        assertEquals(2, groupTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().size());
        assertTrue(groupTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().contains("csv"));
        assertTrue(groupTO.getTypeExtension(AnyTypeKind.USER.name()).get().getAuxClasses().contains("other"));
    }

    @Test
    public void provisionMembers() throws InterruptedException {
        assumeFalse(ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform()));

        // 1. create group without resources
        GroupCR groupCR = getBasicSample("forProvision");
        GroupTO groupTO = createGroup(groupCR).getEntity();

        // 2. create user with such group assigned
        UserCR userCR = UserITCase.getUniqueSample("forProvision@syncope.apache.org");
        userCR.getMemberships().add(new MembershipTO.Builder(groupTO.getKey()).build());
        UserTO userTO = createUser(userCR).getEntity();

        // 3. modify the group by assiging the LDAP resource
        GroupUR groupUR = new GroupUR();
        groupUR.setKey(groupTO.getKey());
        groupUR.getResources().add(new StringPatchItem.Builder().value(RESOURCE_NAME_LDAP).build());
        ProvisioningResult<GroupTO> groupUpdateResult = updateGroup(groupUR);
        groupTO = groupUpdateResult.getEntity();

        PropagationStatus propStatus = groupUpdateResult.getPropagationStatuses().get(0);
        assertEquals(RESOURCE_NAME_LDAP, propStatus.getResource());
        assertEquals(ExecStatus.SUCCESS, propStatus.getStatus());

        // 4. verify that the user above is not found on LDAP
        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }

        try {
            // 5. provision group members
            ExecTO exec = groupService.provisionMembers(groupTO.getKey(), ProvisionAction.PROVISION);
            assertNotNull(exec.getRefKey());

            AtomicReference<List<ExecTO>> execs = new AtomicReference<>();
            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                try {
                    execs.set(taskService.read(TaskType.SCHEDULED, exec.getRefKey(), true).getExecutions());
                    return !execs.get().isEmpty();
                } catch (Exception e) {
                    return false;
                }
            });
            assertEquals(TaskJob.Status.SUCCESS.name(), execs.get().get(0).getStatus());

            // 6. verify that the user above is now fond on LDAP
            ConnObjectTO userOnLdap =
                    resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
            assertNotNull(userOnLdap);
        } finally {
            groupService.delete(groupTO.getKey());
            userService.delete(userTO.getKey());
        }
    }

    @Test
    public void issue178() {
        GroupCR groupCR = new GroupCR();
        String groupName = "torename" + getUUIDString();
        groupCR.setName(groupName);
        groupCR.setRealm("/");

        GroupTO actual = createGroup(groupCR).getEntity();

        assertNotNull(actual);
        assertEquals(groupName, actual.getName());

        GroupUR groupUR = new GroupUR();
        groupUR.setKey(actual.getKey());
        String renamedGroup = "renamed" + getUUIDString();
        groupUR.setName(new StringReplacePatchItem.Builder().value(renamedGroup).build());

        actual = updateGroup(groupUR).getEntity();
        assertNotNull(actual);
        assertEquals(renamedGroup, actual.getName());
    }

    @Test
    public void issueSYNCOPE632() {
        DerSchemaTO orig = schemaService.read(SchemaType.DERIVED, "displayProperty");
        DerSchemaTO modified = SerializationUtils.clone(orig);
        modified.setExpression("icon + '_' + show");

        GroupCR groupCR = GroupITCase.getSample("lastGroup");
        GroupTO groupTO = null;
        try {
            schemaService.update(SchemaType.DERIVED, modified);

            // 0. create group
            groupCR.getPlainAttrs().add(attr("icon", "anIcon"));
            groupCR.getPlainAttrs().add(attr("show", "true"));
            groupCR.getResources().clear();

            groupTO = createGroup(groupCR).getEntity();
            assertNotNull(groupTO);

            // 1. create new LDAP resource having ConnObjectKey mapped to a derived attribute
            ResourceTO newLDAP = resourceService.read(RESOURCE_NAME_LDAP);
            newLDAP.setKey("new-ldap");
            newLDAP.setPropagationPriority(0);

            for (ProvisionTO provision : newLDAP.getProvisions()) {
                provision.getVirSchemas().clear();
            }

            MappingTO mapping = newLDAP.getProvision(AnyTypeKind.GROUP.name()).get().getMapping();

            ItemTO connObjectKey = mapping.getConnObjectKeyItem();
            connObjectKey.setIntAttrName("displayProperty");
            connObjectKey.setPurpose(MappingPurpose.PROPAGATION);
            mapping.setConnObjectKeyItem(connObjectKey);
            mapping.setConnObjectLink("'cn=' + displayProperty + ',ou=groups,o=isp'");

            ItemTO description = new ItemTO();
            description.setIntAttrName("key");
            description.setExtAttrName("description");
            description.setPurpose(MappingPurpose.PROPAGATION);
            mapping.add(description);

            newLDAP = createResource(newLDAP);
            assertNotNull(newLDAP);

            // 2. update group and give the resource created above
            GroupUR groupUR = new GroupUR();
            groupUR.setKey(groupTO.getKey());
            groupUR.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).
                    value("new-ldap").build());

            groupTO = updateGroup(groupUR).getEntity();
            assertNotNull(groupTO);

            // 3. update the group
            groupUR = new GroupUR();
            groupUR.setKey(groupTO.getKey());
            groupUR.getPlainAttrs().add(attrAddReplacePatch("icon", "anotherIcon"));

            groupTO = updateGroup(groupUR).getEntity();
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
                        ctx.search("ou=groups,o=isp", "(description=" + groupTO.getKey() + ')', ctls);
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
            schemaService.update(SchemaType.DERIVED, orig);
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
        GroupCR groupCR = GroupITCase.getBasicSample("syncope717");
        groupCR.getPlainAttrs().add(attr(doubleSchemaName, "11.23"));

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);
        assertEquals("11.23", groupTO.getPlainAttr(doubleSchemaName).get().getValues().get(0));

        // 3. update schema, set conversion pattern
        schema = schemaService.read(SchemaType.PLAIN, schema.getKey());
        schema.setConversionPattern("0.000");
        schemaService.update(SchemaType.PLAIN, schema);

        // 4. re-read group, verify that pattern was applied
        groupTO = groupService.read(groupTO.getKey());
        assertNotNull(groupTO);
        assertEquals("11.230", groupTO.getPlainAttr(doubleSchemaName).get().getValues().get(0));

        // 5. modify group with new double value
        GroupUR groupUR = new GroupUR();
        groupUR.setKey(groupTO.getKey());
        groupUR.getPlainAttrs().add(new AttrPatch.Builder(attr(doubleSchemaName, "11.257")).build());

        groupTO = updateGroup(groupUR).getEntity();
        assertNotNull(groupTO);
        assertEquals("11.257", groupTO.getPlainAttr(doubleSchemaName).get().getValues().get(0));

        // 6. update schema, unset conversion pattern
        schema.setConversionPattern(null);
        schemaService.update(SchemaType.PLAIN, schema);

        // 7. modify group with new double value, verify that no pattern is applied
        groupUR = new GroupUR();
        groupUR.setKey(groupTO.getKey());
        groupUR.getPlainAttrs().add(new AttrPatch.Builder(attr(doubleSchemaName, "11.23")).build());

        groupTO = updateGroup(groupUR).getEntity();
        assertNotNull(groupTO);
        assertEquals("11.23", groupTO.getPlainAttr(doubleSchemaName).get().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE1467() {
        GroupTO groupTO = null;
        try {
            GroupCR groupCR = new GroupCR();
            groupCR.setRealm(SyncopeConstants.ROOT_REALM);
            groupCR.setName("issueSYNCOPE1467");
            groupCR.getResources().add(RESOURCE_NAME_LDAP);

            groupTO = createGroup(groupCR).getEntity();
            assertNotNull(groupTO);
            assertTrue(groupTO.getResources().contains(RESOURCE_NAME_LDAP));

            ConnObjectTO connObjectTO =
                    resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
            assertNotNull(connObjectTO);
            assertEquals("issueSYNCOPE1467", connObjectTO.getAttr("cn").get().getValues().get(0));

            GroupUR groupUR = new GroupUR();
            groupUR.setKey(groupTO.getKey());
            groupUR.setName(new StringReplacePatchItem.Builder().value("fixedSYNCOPE1467").build());

            assertNotNull(updateGroup(groupUR).getEntity());

            // Assert resources are present
            ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
            assertNotNull(ldap);

            connObjectTO = resourceService.
                    readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
            assertNotNull(connObjectTO);
            assertEquals("fixedSYNCOPE1467", connObjectTO.getAttr("cn").get().getValues().get(0));
        } finally {
            if (groupTO.getKey() != null) {
                groupService.delete(groupTO.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE1472() {
        // 1. update group artDirector by assigning twice resource-testdb and auxiliary class csv
        GroupUR groupUR = new GroupUR();
        groupUR.setKey("ece66293-8f31-4a84-8e8d-23da36e70846");
        groupUR.getResources().add(new StringPatchItem.Builder()
                .value(RESOURCE_NAME_TESTDB)
                .operation(PatchOperation.ADD_REPLACE)
                .build());
        groupUR.getAuxClasses().add(new StringPatchItem.Builder()
                .operation(PatchOperation.ADD_REPLACE)
                .value("csv")
                .build());
        for (int i = 0; i < 2; i++) {
            updateGroup(groupUR);
        }

        // 2. remove resources and auxiliary classes
        groupUR.getResources().clear();
        groupUR.getResources().add(new StringPatchItem.Builder()
                .value(RESOURCE_NAME_TESTDB)
                .operation(PatchOperation.DELETE)
                .build());
        groupUR.getAuxClasses().clear();
        groupUR.getAuxClasses().add(new StringPatchItem.Builder()
                .value("csv")
                .operation(PatchOperation.DELETE)
                .build());

        updateGroup(groupUR);

        GroupTO groupTO = groupService.read("ece66293-8f31-4a84-8e8d-23da36e70846");
        assertFalse(groupTO.getResources().contains(RESOURCE_NAME_TESTDB), "Should not contain removed resources");
        assertFalse(groupTO.getAuxClasses().contains("csv"), "Should not contain removed auxiliary classes");
    }
}
