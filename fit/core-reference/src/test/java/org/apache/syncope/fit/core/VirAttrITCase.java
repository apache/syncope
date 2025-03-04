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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class VirAttrITCase extends AbstractITCase {

    @Test
    public void issueSYNCOPE16() {
        UserCR userCR = UserITCase.getUniqueSample("issue16@apache.org");
        userCR.getVirAttrs().add(attr("virtualdata", "virtualvalue"));
        userCR.getResources().add(RESOURCE_NAME_DBVIRATTR);
        userCR.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());

        // 1. create user
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 2. check for virtual attribute value
        userTO = USER_SERVICE.read(userTO.getKey());
        assertNotNull(userTO);
        assertEquals("virtualvalue", userTO.getVirAttr("virtualdata").get().getValues().getFirst());

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getVirAttrs().add(attr("virtualdata", "virtualupdated"));

        // 3. update virtual attribute
        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);

        // 4. check for virtual attribute value
        userTO = USER_SERVICE.read(userTO.getKey());
        assertNotNull(userTO);
        assertEquals("virtualupdated", userTO.getVirAttr("virtualdata").get().getValues().getFirst());
    }

    @Test
    public void issueSYNCOPE260() {
        // create new virtual schema for the resource below
        ResourceTO ws2 = RESOURCE_SERVICE.read(RESOURCE_NAME_WS2);
        Provision provision = ws2.getProvision(AnyTypeKind.USER.name()).get();
        assertNotNull(provision);

        VirSchemaTO virSchema = new VirSchemaTO();
        virSchema.setKey("syncope260" + getUUIDString());
        virSchema.setExtAttrName("companyName");
        virSchema.setResource(RESOURCE_NAME_WS2);
        virSchema.setAnyType(provision.getAnyType());
        virSchema = createSchema(SchemaType.VIRTUAL, virSchema);
        assertNotNull(virSchema);

        AnyTypeClassTO newClass = new AnyTypeClassTO();
        newClass.setKey("syncope260" + getUUIDString());
        newClass.getVirSchemas().add(virSchema.getKey());
        Response response = ANY_TYPE_CLASS_SERVICE.create(newClass);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);

        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserCR userCR = UserITCase.getUniqueSample("260@a.com");
        userCR.getAuxClasses().add(newClass.getKey());
        userCR.getVirAttrs().add(attr(virSchema.getKey(), "virtualvalue"));
        userCR.getResources().add(RESOURCE_NAME_WS2);

        ProvisioningResult<UserTO> result = createUser(userCR);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, result.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().getFirst().getStatus());
        UserTO userTO = result.getEntity();

        ConnObject connObjectTO =
                RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue", connObjectTO.getAttr("COMPANYNAME").get().getValues().getFirst());
        // ----------------------------------

        // ----------------------------------
        // update user virtual attribute and check virtual attribute value update propagation
        // ----------------------------------
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getVirAttrs().add(attr(virSchema.getKey(), "virtualvalue2"));

        result = updateUser(userUR);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, result.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().getFirst().getStatus());
        userTO = result.getEntity();

        connObjectTO = RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue2", connObjectTO.getAttr("COMPANYNAME").get().getValues().getFirst());
        // ----------------------------------

        // ----------------------------------
        // suspend/reactivate user and check virtual attribute value (unchanged)
        // ----------------------------------
        StatusR statusR = new StatusR.Builder(userTO.getKey(), StatusRType.SUSPEND).build();
        userTO = USER_SERVICE.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue2", connObjectTO.getAttr("COMPANYNAME").get().getValues().getFirst());

        statusR = new StatusR.Builder(userTO.getKey(), StatusRType.REACTIVATE).build();
        userTO = USER_SERVICE.status(statusR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertEquals("active", userTO.getStatus());

        connObjectTO = RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("virtualvalue2", connObjectTO.getAttr("COMPANYNAME").get().getValues().getFirst());
        // ----------------------------------

        // ----------------------------------
        // update user attribute and check virtual attribute value (unchanged)
        // ----------------------------------
        userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getPlainAttrs().add(attrAddReplacePatch("surname", "Surname2"));

        result = updateUser(userUR);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, result.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().getFirst().getStatus());
        userTO = result.getEntity();

        connObjectTO = RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertEquals("Surname2", connObjectTO.getAttr("SURNAME").get().getValues().getFirst());

        // virtual attribute value did not change
        assertFalse(connObjectTO.getAttr("COMPANYNAME").get().getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getAttr("COMPANYNAME").get().getValues().getFirst());
        // ----------------------------------
    }

    @Test
    public void virAttrCache() {
        UserCR userCR = UserITCase.getUniqueSample("virattrcache@apache.org");
        userCR.getVirAttrs().clear();

        Attr virAttr = new Attr();
        virAttr.setSchema("virtualdata");
        virAttr.getValues().add("virattrcache");
        userCR.getVirAttrs().add(virAttr);

        userCR.getMemberships().clear();
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // 1. create user
        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = USER_SERVICE.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttr("virtualdata").get().getValues().getFirst());

        // 3. update virtual attribute directly
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT USERNAME FROM testpull WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testpull set USERNAME='virattrcache2' WHERE ID=?", actual.getKey());

        value = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT USERNAME FROM testpull WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache2", value);

        // 4. check for cached attribute value
        actual = USER_SERVICE.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttr("virtualdata").get().getValues().getFirst());

        UserUR userUR = new UserUR();
        userUR.setKey(actual.getKey());
        userUR.getVirAttrs().add(attr("virtualdata", "virtualupdated"));

        // 5. update virtual attribute
        actual = updateUser(userUR).getEntity();
        assertNotNull(actual);

        // 6. check for virtual attribute value
        actual = USER_SERVICE.read(actual.getKey());
        assertNotNull(actual);
        assertEquals("virtualupdated", actual.getVirAttr("virtualdata").get().getValues().getFirst());
    }

    @Test
    public void issueSYNCOPE397() {
        assumeFalse(IS_NEO4J_PERSISTENCE);

        ResourceTO csv = RESOURCE_SERVICE.read(RESOURCE_NAME_CSV);

        // change mapping of resource-csv
        Mapping origMapping = SerializationUtils.clone(csv.getProvisions().getFirst().getMapping());
        try {
            // remove this mapping
            Optional<Item> email = csv.getProvisions().getFirst().getMapping().getItems().stream().
                    filter(item -> "email".equals(item.getIntAttrName())).findFirst();
            if (email.isPresent()) {
                csv.getProvisions().getFirst().getMapping().getItems().remove(email.get());
            }

            RESOURCE_SERVICE.update(csv);
            csv = RESOURCE_SERVICE.read(RESOURCE_NAME_CSV);
            assertNotNull(csv.getProvisions().getFirst().getMapping());

            // create new virtual schema for the resource below
            Provision provision = csv.getProvision(AnyTypeKind.USER.name()).get();
            assertNotNull(provision);

            VirSchemaTO virSchema = new VirSchemaTO();
            virSchema.setKey("syncope397" + getUUIDString());
            virSchema.setExtAttrName("email");
            virSchema.setResource(RESOURCE_NAME_CSV);
            virSchema.setAnyType(provision.getAnyType());
            virSchema = createSchema(SchemaType.VIRTUAL, virSchema);
            assertNotNull(virSchema);

            AnyTypeClassTO newClass = new AnyTypeClassTO();
            newClass.setKey("syncope397" + getUUIDString());
            newClass.getVirSchemas().add(virSchema.getKey());
            Response response = ANY_TYPE_CLASS_SERVICE.create(newClass);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
            newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);

            // create a new user
            UserCR userCR = UserITCase.getUniqueSample("397@syncope.apache.org");
            userCR.getAuxClasses().add("csv");
            userCR.getAuxClasses().add(newClass.getKey());
            userCR.getResources().clear();
            userCR.getMemberships().clear();

            userCR.getVirAttrs().clear();
            userCR.getVirAttrs().add(attr(virSchema.getKey(), "test@testone.org"));
            // assign resource-csv to user
            userCR.getResources().add(RESOURCE_NAME_CSV);
            // save user
            UserTO userTO = createUser(userCR).getEntity();
            // make std controls about user
            assertNotNull(userTO);
            assertTrue(RESOURCE_NAME_CSV.equals(userTO.getResources().iterator().next()));
            assertEquals("test@testone.org", userTO.getVirAttrs().iterator().next().getValues().getFirst());

            // update user
            UserTO toBeUpdated = USER_SERVICE.read(userTO.getKey());
            UserUR userUR = new UserUR();
            userUR.setKey(toBeUpdated.getKey());
            userUR.setPassword(new PasswordPatch.Builder().value("password234").build());
            // assign new resource to user
            userUR.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS2).build());
            // modify virtual attribute
            userUR.getVirAttrs().add(attr(virSchema.getKey(), "test@testoneone.com"));

            // check Syncope change password
            userUR.setPassword(new PasswordPatch.Builder().
                    value("password234").
                    onSyncope(true).
                    resource(RESOURCE_NAME_WS2).
                    build());

            ProvisioningResult<UserTO> result = updateUser(userUR);
            assertNotNull(result);
            toBeUpdated = result.getEntity();
            assertTrue(toBeUpdated.getVirAttrs().iterator().next().getValues().contains("test@testoneone.com"));
            // check if propagates correctly with assertEquals on size of tasks list
            assertEquals(2, result.getPropagationStatuses().size());
        } finally {
            // restore mapping of resource-csv
            csv.getProvisions().getFirst().setMapping(origMapping);
            RESOURCE_SERVICE.update(csv);
        }
    }

    @Test
    public void issueSYNCOPE442() {
        UserCR userCR = UserITCase.getUniqueSample("syncope442@apache.org");
        userCR.getVirAttrs().clear();

        Attr virAttr = new Attr();
        virAttr.setSchema("virtualdata");
        virAttr.getValues().add("virattrcache");
        userCR.getVirAttrs().add(virAttr);

        userCR.getMemberships().clear();
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // 1. create user
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 2. check for virtual attribute value
        userTO = USER_SERVICE.read(userTO.getKey());
        assertEquals("virattrcache", userTO.getVirAttr("virtualdata").get().getValues().getFirst());

        // ----------------------------------------
        // 3. change connector URL so that we are sure that any provided value will come from virtual cache
        // ----------------------------------------
        String jdbcURL = null;
        ConnInstanceTO connInstanceTO = CONNECTOR_SERVICE.readByResource(
                RESOURCE_NAME_DBVIRATTR, Locale.ENGLISH.getLanguage());
        for (ConnConfProperty prop : connInstanceTO.getConf()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                jdbcURL = prop.getValues().getFirst().toString();
                prop.getValues().clear();
                prop.getValues().add("jdbc:h2:tcp://localhost:9092/xxx");
            }
        }

        CONNECTOR_SERVICE.update(connInstanceTO);
        // ----------------------------------------

        // ----------------------------------------
        // 4. update value on external resource
        // ----------------------------------------
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT USERNAME FROM testpull WHERE ID=?", String.class, userTO.
                        getKey());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testpull set USERNAME='virattrcache2' WHERE ID=?", userTO.getKey());

        value = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT USERNAME FROM testpull WHERE ID=?", String.class, userTO.getKey());
        assertEquals("virattrcache2", value);
        // ----------------------------------------

        userTO = USER_SERVICE.read(userTO.getKey());
        assertEquals("virattrcache", userTO.getVirAttr("virtualdata").get().getValues().getFirst());

        // ----------------------------------------
        // 5. restore connector URL, values can be read again from external resource
        // ----------------------------------------
        for (ConnConfProperty prop : connInstanceTO.getConf()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                prop.getValues().clear();
                prop.getValues().add(jdbcURL);
            }
        }

        CONNECTOR_SERVICE.update(connInstanceTO);
        // ----------------------------------------

        // cached value still in place...
        userTO = USER_SERVICE.read(userTO.getKey());
        assertEquals("virattrcache", userTO.getVirAttr("virtualdata").get().getValues().getFirst());

        // force cache update by adding a resource which has virtualdata mapped for propagation
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS2).build());
        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);

        userTO = USER_SERVICE.read(userTO.getKey());
        assertEquals("virattrcache2", userTO.getVirAttr("virtualdata").get().getValues().getFirst());
    }

    @Test
    public void issueSYNCOPE436() {
        UserCR userCR = UserITCase.getUniqueSample("syncope436@syncope.apache.org");
        userCR.getMemberships().clear();
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_LDAP);
        userCR.getVirAttrs().add(attr("virtualReadOnly", "readOnly"));
        UserTO userTO = createUser(userCR).getEntity();
        // finding no values because the virtual attribute is readonly 
        assertTrue(userTO.getVirAttr("virtualReadOnly").get().getValues().isEmpty());
    }

    @Test
    public void issueSYNCOPE453() {
        assumeFalse(IS_NEO4J_PERSISTENCE);

        String resourceName = "issueSYNCOPE453Res" + getUUIDString();
        String groupKey = null;
        String groupName = "issueSYNCOPE453Group" + getUUIDString();

        try {
            // -------------------------------------------
            // Create a VirAttrITCase ad-hoc
            // -------------------------------------------
            VirSchemaTO rvirtualdata;
            try {
                rvirtualdata = SCHEMA_SERVICE.read(SchemaType.VIRTUAL, "rvirtualdata");
            } catch (SyncopeClientException e) {
                LOG.warn("rvirtualdata not found, re-creating", e);

                rvirtualdata = new VirSchemaTO();
                rvirtualdata.setKey("rvirtualdata");
                rvirtualdata.setExtAttrName("businessCategory");
                rvirtualdata.setResource(RESOURCE_NAME_LDAP);
                rvirtualdata.setAnyType(AnyTypeKind.GROUP.name());

                rvirtualdata = createSchema(SchemaType.VIRTUAL, rvirtualdata);
            }
            assertNotNull(rvirtualdata);

            if (!"minimal group".equals(rvirtualdata.getAnyTypeClass())) {
                LOG.warn("rvirtualdata not in minimal group, restoring");

                AnyTypeClassTO minimalGroup = ANY_TYPE_CLASS_SERVICE.read("minimal group");
                minimalGroup.getVirSchemas().add(rvirtualdata.getKey());
                ANY_TYPE_CLASS_SERVICE.update(minimalGroup);

                rvirtualdata = SCHEMA_SERVICE.read(SchemaType.VIRTUAL, rvirtualdata.getKey());
                assertEquals("minimal group", rvirtualdata.getAnyTypeClass());
            }

            // -------------------------------------------
            // Create a resource ad-hoc
            // -------------------------------------------
            ResourceTO resourceTO = new ResourceTO();

            resourceTO.setKey(resourceName);
            resourceTO.setConnector("be24b061-019d-4e3e-baf0-0a6d0a45cb9c");

            Provision provisionTO = new Provision();
            provisionTO.setAnyType(AnyTypeKind.USER.name());
            provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
            resourceTO.getProvisions().add(provisionTO);

            Mapping mapping = new Mapping();
            provisionTO.setMapping(mapping);

            Item item = new Item();
            item.setIntAttrName("fullname");
            item.setExtAttrName("ID");
            item.setPurpose(MappingPurpose.PROPAGATION);
            item.setConnObjectKey(true);
            mapping.setConnObjectKeyItem(item);

            item = new Item();
            item.setIntAttrName("username");
            item.setExtAttrName("USERNAME");
            item.setPurpose(MappingPurpose.PROPAGATION);
            mapping.getItems().add(item);

            item = new Item();
            item.setIntAttrName("groups[" + groupName + "].rvirtualdata");
            item.setExtAttrName("EMAIL");
            item.setPurpose(MappingPurpose.PROPAGATION);
            mapping.getItems().add(item);

            assertNotNull(getObject(
                    RESOURCE_SERVICE.create(resourceTO).getLocation(), ResourceService.class, ResourceTO.class));
            // -------------------------------------------

            GroupCR groupCR = new GroupCR();
            groupCR.setName(groupName);
            groupCR.setRealm("/");
            groupCR.getVirAttrs().add(attr(rvirtualdata.getKey(), "ml@group.it"));
            groupCR.getResources().add(RESOURCE_NAME_LDAP);
            GroupTO groupTO = createGroup(groupCR).getEntity();
            groupKey = groupTO.getKey();
            assertEquals(1, groupTO.getVirAttrs().size());
            assertEquals("ml@group.it", groupTO.getVirAttrs().iterator().next().getValues().getFirst());
            // -------------------------------------------

            // -------------------------------------------
            // Create new user
            // -------------------------------------------
            UserCR userCR = UserITCase.getUniqueSample("syn453@syncope.apache.org");
            userCR.getPlainAttrs().add(attr("fullname", "123"));
            userCR.getResources().clear();
            userCR.getResources().add(resourceName);
            userCR.getVirAttrs().clear();
            userCR.getMemberships().clear();

            userCR.getMemberships().add(new MembershipTO.Builder(groupTO.getKey()).build());

            ProvisioningResult<UserTO> result = createUser(userCR);
            assertEquals(2, result.getPropagationStatuses().size());
            assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
            assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(1).getStatus());
            UserTO userTO = result.getEntity();

            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

            Map<String, Object> actuals = jdbcTemplate.queryForMap(
                    "SELECT id, surname, email FROM testpull WHERE id=?",
                    new Object[] { userTO.getPlainAttr("fullname").get().getValues().getFirst() });

            assertEquals(userTO.getPlainAttr("fullname").get().getValues().getFirst(), actuals.get("id").toString());
            assertEquals("ml@group.it", actuals.get("email"));
            // -------------------------------------------
        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        } finally {
            // -------------------------------------------
            // Delete resource and group ad-hoc
            // -------------------------------------------
            RESOURCE_SERVICE.delete(resourceName);
            if (groupKey != null) {
                GROUP_SERVICE.delete(groupKey);
            }
            // -------------------------------------------
        }
    }

    @Test
    public void issueSYNCOPE459() {
        UserCR userCR = UserITCase.getUniqueSample("syncope459@apache.org");
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_LDAP);
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();

        UserTO userTO = createUser(userCR).getEntity();

        assertNotNull(userTO.getVirAttr("virtualReadOnly"));
    }

    @Test
    public void issueSYNCOPE501() {
        // 1. create user and propagate him on resource-db-virattr
        UserCR userCR = UserITCase.getUniqueSample("syncope501@apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();

        userCR.getResources().add(RESOURCE_NAME_DBVIRATTR);

        // virtualdata is mapped with username
        userCR.getVirAttrs().add(attr("virtualdata", "syncope501@apache.org"));

        UserTO userTO = createUser(userCR).getEntity();

        assertNotNull(userTO.getVirAttr("virtualdata"));
        assertEquals("syncope501@apache.org", userTO.getVirAttr("virtualdata").get().getValues().getFirst());

        // 2. update virtual attribute
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        // change virtual attribute value
        userUR.getVirAttrs().add(attr("virtualdata", "syncope501_updated@apache.org"));

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);

        // 3. check that user virtual attribute has really been updated 
        assertFalse(userTO.getVirAttr("virtualdata").get().getValues().isEmpty());
        assertEquals("syncope501_updated@apache.org", userTO.getVirAttr("virtualdata").get().getValues().getFirst());
    }

    @Test
    public void issueSYNCOPE691() {
        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        try {
            Provision provision = ldap.getProvision(AnyTypeKind.USER.name()).orElse(null);
            assertNotNull(provision);
            provision.getMapping().getItems().removeIf(item -> "mail".equals(item.getExtAttrName()));
            provision.getVirSchemas().clear();

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provision);
            ldap.setKey(RESOURCE_NAME_LDAP + "691" + getUUIDString());
            RESOURCE_SERVICE.create(ldap);

            ldap = RESOURCE_SERVICE.read(ldap.getKey());
            provision = ldap.getProvision(AnyTypeKind.USER.name()).get();
            assertNotNull(provision);

            // create new virtual schema for the resource below
            VirSchemaTO virSchema = new VirSchemaTO();
            virSchema.setKey("syncope691" + getUUIDString());
            virSchema.setExtAttrName("mail");
            virSchema.setResource(ldap.getKey());
            virSchema.setAnyType(provision.getAnyType());
            virSchema = createSchema(SchemaType.VIRTUAL, virSchema);
            assertNotNull(virSchema);

            AnyTypeClassTO newClass = new AnyTypeClassTO();
            newClass.setKey("syncope691" + getUUIDString());
            newClass.getVirSchemas().add(virSchema.getKey());
            Response response = ANY_TYPE_CLASS_SERVICE.create(newClass);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
            newClass = getObject(response.getLocation(), AnyTypeClassService.class, AnyTypeClassTO.class);

            // create a new user
            UserCR userCR = UserITCase.getUniqueSample("syncope691@syncope.apache.org");
            userCR.getAuxClasses().add(newClass.getKey());
            userCR.getResources().clear();
            userCR.getMemberships().clear();
            userCR.getVirAttrs().clear();

            Attr emailTO = new Attr();
            emailTO.setSchema(virSchema.getKey());
            emailTO.getValues().add("test@issue691.dom1.org");
            emailTO.getValues().add("test@issue691.dom2.org");

            userCR.getVirAttrs().add(emailTO);
            // assign resource-ldap691 to user
            userCR.getResources().add(ldap.getKey());
            // save user
            UserTO userTO = createUser(userCR).getEntity();
            // make std controls about user
            assertNotNull(userTO);
            assertTrue(ldap.getKey().equals(userTO.getResources().iterator().next()));

            assertEquals(2, userTO.getVirAttrs().iterator().next().getValues().size());
            assertTrue(userTO.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom1.org"));
            assertTrue(userTO.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom2.org"));

            // update user
            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            // modify virtual attribute
            userUR.getVirAttrs().add(new Attr.Builder(virSchema.getKey()).
                    value("test@issue691.dom3.org").
                    value("test@issue691.dom4.org").
                    build());

            UserTO updated = updateUser(userUR).getEntity();
            assertNotNull(updated);
            assertEquals(2, updated.getVirAttrs().iterator().next().getValues().size());
            assertTrue(updated.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom3.org"));
            assertTrue(updated.getVirAttrs().iterator().next().getValues().contains("test@issue691.dom4.org"));
        } finally {
            try {
                RESOURCE_SERVICE.delete(ldap.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
}
