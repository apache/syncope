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
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StatusPatchType;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class VirAttrITCase extends AbstractITCase {

    @Test
    public void issueSYNCOPE16() {
        UserTO userTO = UserITCase.getUniqueSampleTO("issue16@apache.org");
        userTO.getVirAttrs().add(attrTO("virtualdata", "virtualvalue"));
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);
        userTO.getMemberships().add(new MembershipTO.Builder().group(8L).build());

        // 1. create user
        userTO = createUser(userTO).getAny();
        assertNotNull(userTO);

        // 2. check for virtual attribute value
        userTO = userService.read(userTO.getKey());
        assertNotNull(userTO);
        assertEquals("virtualvalue", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getVirAttrs().add(attrTO("virtualdata", "virtualupdated"));

        // 3. update virtual attribute
        userTO = updateUser(userPatch).getAny();
        assertNotNull(userTO);

        // 4. check for virtual attribute value
        userTO = userService.read(userTO.getKey());
        assertNotNull(userTO);
        assertEquals("virtualupdated", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE260() {
        // create new virtual schema for the resource below
        ResourceTO ws2 = resourceService.read(RESOURCE_NAME_WS2);
        ProvisionTO provision = ws2.getProvision(AnyTypeKind.USER.name());
        assertNotNull(provision);

        VirSchemaTO virSchema = new VirSchemaTO();
        virSchema.setKey("syncope260" + getUUIDString());
        virSchema.setExtAttrName("companyName");
        virSchema.setProvision(provision.getKey());
        virSchema = createSchema(SchemaType.VIRTUAL, virSchema);
        assertNotNull(virSchema);

        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("syncope260" + getUUIDString());
        newClass.getVirSchemas().add(virSchema.getKey());
        Response response = anyTypeClassService.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);

        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserTO userTO = UserITCase.getUniqueSampleTO("260@a.com");
        userTO.getAuxClasses().add(newClass.getKey());
        userTO.getVirAttrs().add(attrTO(virSchema.getKey(), "virtualvalue"));
        userTO.getResources().add(RESOURCE_NAME_WS2);

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getAny();

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue", connObjectTO.getPlainAttrMap().get("COMPANYNAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // update user virtual attribute and check virtual attribute value update propagation
        // ----------------------------------
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getVirAttrs().add(attrTO(virSchema.getKey(), "virtualvalue2"));

        result = updateUser(userPatch);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getAny();

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("COMPANYNAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // suspend/reactivate user and check virtual attribute value (unchanged)
        // ----------------------------------
        StatusPatch statusPatch = new StatusPatch();
        statusPatch.setKey(userTO.getKey());
        statusPatch.setType(StatusPatchType.SUSPEND);
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getAny();
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("COMPANYNAME").getValues().get(0));

        statusPatch = new StatusPatch();
        statusPatch.setKey(userTO.getKey());
        statusPatch.setType(StatusPatchType.REACTIVATE);
        userTO = userService.status(statusPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getAny();
        assertEquals("active", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("COMPANYNAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // update user attribute and check virtual attribute value (unchanged)
        // ----------------------------------
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("surname", "Surname2"));

        result = updateUser(userPatch);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getAny();

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("Surname2", connObjectTO.getPlainAttrMap().get("SURNAME").getValues().get(0));

        // virtual attribute value did not change
        assertFalse(connObjectTO.getPlainAttrMap().get("COMPANYNAME").getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("COMPANYNAME").getValues().get(0));
        // ----------------------------------
    }

    @Test
    public void virAttrCache() {
        UserTO userTO = UserITCase.getUniqueSampleTO("virattrcache@apache.org");
        userTO.getVirAttrs().clear();

        AttrTO virAttrTO = new AttrTO();
        virAttrTO.setSchema("virtualdata");
        virAttrTO.getValues().add("virattrcache");
        userTO.getVirAttrs().add(virAttrTO);

        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // 1. create user
        UserTO actual = createUser(userTO).getAny();
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = userService.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        // 3. update virtual attribute directly
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testpull WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testpull set USERNAME='virattrcache2' WHERE ID=?", actual.getKey());

        value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testpull WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache2", value);

        // 4. check for cached attribute value
        actual = userService.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(actual.getKey());
        userPatch.getVirAttrs().add(attrTO("virtualdata", "virtualupdated"));

        // 5. update virtual attribute
        actual = updateUser(userPatch).getAny();
        assertNotNull(actual);

        // 6. check for virtual attribute value
        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertEquals("virtualupdated", actual.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE397() {
        ResourceTO csv = resourceService.read(RESOURCE_NAME_CSV);

        // change mapping of resource-csv
        MappingTO origMapping = SerializationUtils.clone(csv.getProvisions().get(0).getMapping());
        try {
            // remove this mapping
            CollectionUtils.filterInverse(csv.getProvisions().get(0).getMapping().getItems(),
                    new Predicate<MappingItemTO>() {

                @Override
                public boolean evaluate(final MappingItemTO item) {
                    return "email".equals(item.getIntAttrName());
                }
            });

            resourceService.update(csv);
            csv = resourceService.read(RESOURCE_NAME_CSV);
            assertNotNull(csv.getProvisions().get(0).getMapping());

            // create new virtual schema for the resource below
            ProvisionTO provision = csv.getProvision(AnyTypeKind.USER.name());
            assertNotNull(provision);

            VirSchemaTO virSchema = new VirSchemaTO();
            virSchema.setKey("syncope397" + getUUIDString());
            virSchema.setExtAttrName("email");
            virSchema.setProvision(provision.getKey());
            virSchema = createSchema(SchemaType.VIRTUAL, virSchema);
            assertNotNull(virSchema);

            AnyTypeClassTO newClass = new AnyTypeClassTO();
            newClass.setKey("syncope397" + getUUIDString());
            newClass.getVirSchemas().add(virSchema.getKey());
            Response response = anyTypeClassService.create(newClass);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
            newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);

            // create a new user
            UserTO userTO = UserITCase.getUniqueSampleTO("397@syncope.apache.org");
            userTO.getAuxClasses().add("csv");
            userTO.getAuxClasses().add(newClass.getKey());
            userTO.getResources().clear();
            userTO.getMemberships().clear();
            userTO.getDerAttrs().clear();
            userTO.getVirAttrs().clear();

            userTO.getDerAttrs().add(attrTO("csvuserid", null));
            userTO.getDerAttrs().add(attrTO("cn", null));
            userTO.getVirAttrs().add(attrTO(virSchema.getKey(), "test@testone.org"));
            // assign resource-csv to user
            userTO.getResources().add(RESOURCE_NAME_CSV);
            // save user
            userTO = createUser(userTO).getAny();
            // make std controls about user
            assertNotNull(userTO);
            assertTrue(RESOURCE_NAME_CSV.equals(userTO.getResources().iterator().next()));
            assertEquals("test@testone.org", userTO.getVirAttrs().iterator().next().getValues().get(0));

            // update user
            UserTO toBeUpdated = userService.read(userTO.getKey());
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(toBeUpdated.getKey());
            userPatch.setPassword(new PasswordPatch.Builder().value("password234").build());
            // assign new resource to user
            userPatch.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS2).build());
            // modify virtual attribute
            userPatch.getVirAttrs().add(attrTO(virSchema.getKey(), "test@testoneone.com"));

            // check Syncope change password
            userPatch.setPassword(new PasswordPatch.Builder().
                    value("password234").
                    onSyncope(true).
                    resource(RESOURCE_NAME_WS2).
                    build());

            ProvisioningResult<UserTO> result = updateUser(userPatch);
            assertNotNull(result);
            toBeUpdated = result.getAny();
            assertTrue(toBeUpdated.getVirAttrs().iterator().next().getValues().contains("test@testoneone.com"));
            // check if propagates correctly with assertEquals on size of tasks list
            assertEquals(2, result.getPropagationStatuses().size());
        } finally {
            // restore mapping of resource-csv
            csv.getProvisions().get(0).setMapping(origMapping);
            resourceService.update(csv);
        }
    }

    @Test
    public void issueSYNCOPE442() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope442@apache.org");
        userTO.getVirAttrs().clear();

        AttrTO virAttrTO = new AttrTO();
        virAttrTO.setSchema("virtualdata");
        virAttrTO.getValues().add("virattrcache");
        userTO.getVirAttrs().add(virAttrTO);

        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // 1. create user
        userTO = createUser(userTO).getAny();
        assertNotNull(userTO);

        // 2. check for virtual attribute value
        userTO = userService.read(userTO.getKey());
        assertEquals("virattrcache", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ----------------------------------------
        // 3. change connector URL so that we are sure that any provided value will come from virtual cache
        // ----------------------------------------
        String jdbcURL = null;
        ConnInstanceTO connInstanceTO = connectorService.readByResource(
                RESOURCE_NAME_DBVIRATTR, Locale.ENGLISH.getLanguage());
        for (ConnConfProperty prop : connInstanceTO.getConf()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                jdbcURL = prop.getValues().iterator().next().toString();
                prop.getValues().clear();
                prop.getValues().add("jdbc:h2:tcp://localhost:9092/xxx");
            }
        }

        connectorService.update(connInstanceTO);
        // ----------------------------------------

        // ----------------------------------------
        // 4. update value on external resource
        // ----------------------------------------
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testpull WHERE ID=?", String.class, userTO.getKey());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testpull set USERNAME='virattrcache2' WHERE ID=?", userTO.getKey());

        value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testpull WHERE ID=?", String.class, userTO.getKey());
        assertEquals("virattrcache2", value);
        // ----------------------------------------

        userTO = userService.read(userTO.getKey());
        assertEquals("virattrcache", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ----------------------------------------
        // 5. restore connector URL, values can be read again from external resource
        // ----------------------------------------
        for (ConnConfProperty prop : connInstanceTO.getConf()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                prop.getValues().clear();
                prop.getValues().add(jdbcURL);
            }
        }

        connectorService.update(connInstanceTO);
        // ----------------------------------------

        // cached value still in place...
        userTO = userService.read(userTO.getKey());
        assertEquals("virattrcache", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // force cache update by adding a resource which has virtualdata mapped for propagation
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS2).build());
        userTO = updateUser(userPatch).getAny();
        assertNotNull(userTO);

        userTO = userService.read(userTO.getKey());
        assertEquals("virattrcache2", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE436() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope436@syncope.apache.org");
        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO.getVirAttrs().add(attrTO("virtualReadOnly", "readOnly"));
        userTO = createUser(userTO).getAny();
        // finding no values because the virtual attribute is readonly 
        assertTrue(userTO.getVirAttrMap().get("virtualReadOnly").getValues().isEmpty());
    }

    @Test
    public void issueSYNCOPE453() {
        String resourceName = "issueSYNCOPE453-Res-" + getUUIDString();
        Long groupKey = null;
        String groupName = "issueSYNCOPE453-Group-" + getUUIDString();

        try {
            // -------------------------------------------
            // Create a VirAttrITCase ad-hoc
            // -------------------------------------------
            VirSchemaTO rvirtualdata;
            try {
                rvirtualdata = schemaService.read(SchemaType.VIRTUAL, "rvirtualdata");
            } catch (SyncopeClientException e) {
                LOG.warn("rvirtualdata not found, re-creating", e);

                rvirtualdata = new VirSchemaTO();
                rvirtualdata.setKey("rvirtualdata");
                rvirtualdata.setExtAttrName("businessCategory");
                rvirtualdata.setProvision(20);

                rvirtualdata = createSchema(SchemaType.VIRTUAL, rvirtualdata);
            }
            assertNotNull(rvirtualdata);

            if (!"minimal group".equals(rvirtualdata.getAnyTypeClass())) {
                LOG.warn("rvirtualdata not in minimal group, restoring");

                AnyTypeClassTO minimalGroup = anyTypeClassService.read("minimal group");
                minimalGroup.getVirSchemas().add(rvirtualdata.getKey());
                anyTypeClassService.update(minimalGroup);

                rvirtualdata = schemaService.read(SchemaType.VIRTUAL, rvirtualdata.getKey());
                assertEquals("minimal group", rvirtualdata.getAnyTypeClass());
            }

            // -------------------------------------------
            // Create a resource ad-hoc
            // -------------------------------------------
            ResourceTO resourceTO = new ResourceTO();

            resourceTO.setKey(resourceName);
            resourceTO.setConnector(107L);

            ProvisionTO provisionTO = new ProvisionTO();
            provisionTO.setAnyType(AnyTypeKind.USER.name());
            provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
            provisionTO.getAuxClasses().add("minimal group");
            resourceTO.getProvisions().add(provisionTO);

            MappingTO mapping = new MappingTO();
            provisionTO.setMapping(mapping);

            MappingItemTO item = new MappingItemTO();
            item.setIntAttrName("aLong");
            item.setIntMappingType(IntMappingType.UserPlainSchema);
            item.setExtAttrName("ID");
            item.setPurpose(MappingPurpose.PROPAGATION);
            item.setConnObjectKey(true);
            mapping.setConnObjectKeyItem(item);

            item = new MappingItemTO();
            item.setExtAttrName("USERNAME");
            item.setIntAttrName("username");
            item.setIntMappingType(IntMappingType.Username);
            item.setPurpose(MappingPurpose.PROPAGATION);
            mapping.getItems().add(item);

            item = new MappingItemTO();
            item.setExtAttrName("EMAIL");
            item.setIntAttrName("rvirtualdata");
            item.setIntMappingType(IntMappingType.GroupVirtualSchema);
            item.setPurpose(MappingPurpose.PROPAGATION);
            mapping.getItems().add(item);

            assertNotNull(getObject(
                    resourceService.create(resourceTO).getLocation(), ResourceService.class, ResourceTO.class));
            // -------------------------------------------

            GroupTO groupTO = new GroupTO();
            groupTO.setName(groupName);
            groupTO.setRealm("/");
            groupTO.getVirAttrs().add(attrTO(rvirtualdata.getKey(), "ml@group.it"));
            groupTO.getResources().add(RESOURCE_NAME_LDAP);
            groupTO = createGroup(groupTO).getAny();
            groupKey = groupTO.getKey();
            assertEquals(1, groupTO.getVirAttrs().size());
            assertEquals("ml@group.it", groupTO.getVirAttrs().iterator().next().getValues().get(0));
            // -------------------------------------------

            // -------------------------------------------
            // Create new user
            // -------------------------------------------
            UserTO userTO = UserITCase.getUniqueSampleTO("syncope453@syncope.apache.org");
            userTO.getPlainAttrs().add(attrTO("aLong", "123"));
            userTO.getResources().clear();
            userTO.getResources().add(resourceName);
            userTO.getVirAttrs().clear();
            userTO.getDerAttrs().clear();
            userTO.getMemberships().clear();

            userTO.getMemberships().add(new MembershipTO.Builder().group(groupTO.getKey()).build());

            ProvisioningResult<UserTO> result = createUser(userTO);
            assertEquals(2, result.getPropagationStatuses().size());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(1).getStatus());
            userTO = result.getAny();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

            Map<String, Object> actuals = jdbcTemplate.queryForMap(
                    "SELECT id, surname, email FROM testpull WHERE id=?",
                    new Object[] { Integer.parseInt(userTO.getPlainAttrMap().get("aLong").getValues().get(0)) });

            assertEquals(userTO.getPlainAttrMap().get("aLong").getValues().get(0), actuals.get("id").toString());
            assertEquals("ml@group.it", actuals.get("email"));
            // -------------------------------------------
        } finally {
            // -------------------------------------------
            // Delete resource and group ad-hoc
            // -------------------------------------------
            resourceService.delete(resourceName);
            if (groupKey != null) {
                groupService.delete(groupKey);
            }
            // -------------------------------------------
        }
    }

    @Test
    public void issueSYNCOPE459() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope459@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();

        userTO = createUser(userTO).getAny();

        assertNotNull(userTO.getVirAttrMap().get("virtualReadOnly"));
    }

    @Test
    public void issueSYNCOPE501() {
        // 1. create user and propagate him on resource-db-virattr
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope501@apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();

        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // virtualdata is mapped with username
        userTO.getVirAttrs().add(attrTO("virtualdata", "syncope501@apache.org"));

        userTO = createUser(userTO).getAny();

        assertNotNull(userTO.getVirAttrMap().get("virtualdata"));
        assertEquals("syncope501@apache.org", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // 2. update virtual attribute
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        // change virtual attribute value
        userPatch.getVirAttrs().add(attrTO("virtualdata", "syncope501_updated@apache.org"));

        userTO = updateUser(userPatch).getAny();
        assertNotNull(userTO);

        // 3. check that user virtual attribute has really been updated 
        assertFalse(userTO.getVirAttrMap().get("virtualdata").getValues().isEmpty());
        assertEquals("syncope501_updated@apache.org", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE691() {
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        try {
            ProvisionTO provision = ldap.getProvision(AnyTypeKind.USER.name());
            assertNotNull(provision);
            CollectionUtils.filterInverse(provision.getMapping().getItems(), new Predicate<MappingItemTO>() {

                @Override
                public boolean evaluate(final MappingItemTO item) {
                    return "mail".equals(item.getExtAttrName());
                }
            });
            provision.getVirSchemas().clear();

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provision);
            ldap.setKey(RESOURCE_NAME_LDAP + "691" + getUUIDString());
            resourceService.create(ldap);

            ldap = resourceService.read(ldap.getKey());
            provision = ldap.getProvision(AnyTypeKind.USER.name());
            assertNotNull(provision);

            // create new virtual schema for the resource below
            VirSchemaTO virSchema = new VirSchemaTO();
            virSchema.setKey("syncope691" + getUUIDString());
            virSchema.setExtAttrName("mail");
            virSchema.setProvision(provision.getKey());
            virSchema = createSchema(SchemaType.VIRTUAL, virSchema);
            assertNotNull(virSchema);

            AnyTypeClassTO newClass = new AnyTypeClassTO();
            newClass.setKey("syncope691" + getUUIDString());
            newClass.getVirSchemas().add(virSchema.getKey());
            Response response = anyTypeClassService.create(newClass);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
            newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);

            // create a new user
            UserTO userTO = UserITCase.getUniqueSampleTO("syncope691@syncope.apache.org");
            userTO.getAuxClasses().add(newClass.getKey());
            userTO.getResources().clear();
            userTO.getMemberships().clear();
            userTO.getDerAttrs().clear();
            userTO.getVirAttrs().clear();

            AttrTO emailTO = new AttrTO();
            emailTO.setSchema(virSchema.getKey());
            emailTO.getValues().add("test@issue691.dom1.org");
            emailTO.getValues().add("test@issue691.dom2.org");

            userTO.getVirAttrs().add(emailTO);
            // assign resource-ldap691 to user
            userTO.getResources().add(ldap.getKey());
            // save user
            userTO = createUser(userTO).getAny();
            // make std controls about user
            assertNotNull(userTO);
            assertTrue(ldap.getKey().equals(userTO.getResources().iterator().next()));

            assertEquals(2, userTO.getVirAttrs().iterator().next().getValues().size(), 0);
            assertTrue(userTO.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom1.org"));
            assertTrue(userTO.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom2.org"));

            // update user
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userTO.getKey());
            // modify virtual attribute
            userPatch.getVirAttrs().add(
                    new AttrTO.Builder().schema(virSchema.getKey()).
                    value("test@issue691.dom3.org").
                    value("test@issue691.dom4.org").
                    build());

            UserTO updated = updateUser(userPatch).getAny();
            assertNotNull(updated);
            assertEquals(2, updated.getVirAttrs().iterator().next().getValues().size(), 0);
            assertTrue(updated.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom3.org"));
            assertTrue(updated.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom4.org"));
        } finally {
            try {
                resourceService.delete(ldap.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

}
