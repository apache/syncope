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
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.rest.api.service.ResourceService;
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

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRightKey(8L);
        userTO.getMemberships().add(membershipTO);

        // 1. create user
        UserTO actual = createUser(userTO);
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertEquals("virtualvalue", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        UserMod userMod = new UserMod();
        userMod.setKey(actual.getKey());
        userMod.getVirAttrsToRemove().add("virtualdata");
        userMod.getVirAttrsToUpdate().add(attrMod("virtualdata", "virtualupdated"));

        // 3. update virtual attribute
        actual = updateUser(userMod);
        assertNotNull(actual);

        // 4. check for virtual attribute value
        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertEquals("virtualupdated", actual.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE260() {
        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserTO userTO = UserITCase.getUniqueSampleTO("260@a.com");
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertEquals("virtualvalue", connObjectTO.getPlainAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // update user virtual attribute and check virtual attribute value update propagation
        // ----------------------------------
        UserMod userMod = new UserMod();
        userMod.setKey(userTO.getKey());

        AttrMod attrMod = new AttrMod();
        attrMod.setSchema("virtualdata");
        attrMod.getValuesToBeRemoved().add("virtualvalue");
        attrMod.getValuesToBeAdded().add("virtualvalue2");

        userMod.getVirAttrsToUpdate().add(attrMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals("ws-target-resource-2", userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // suspend/reactivate user and check virtual attribute value (unchanged)
        // ----------------------------------
        StatusMod statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.SUSPEND);
        userTO = userService.status(userTO.getKey(), statusMod).readEntity(UserTO.class);
        assertEquals("suspended", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertFalse(connObjectTO.getPlainAttrMap().get("NAME").getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("NAME").getValues().get(0));

        statusMod = new StatusMod();
        statusMod.setType(StatusMod.ModType.REACTIVATE);
        userTO = userService.status(userTO.getKey(), statusMod).readEntity(UserTO.class);
        assertEquals("active", userTO.getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertFalse(connObjectTO.getPlainAttrMap().get("NAME").getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // update user attribute and check virtual attribute value (unchanged)
        // ----------------------------------
        userMod = new UserMod();
        userMod.setKey(userTO.getKey());

        attrMod = new AttrMod();
        attrMod.setSchema("surname");
        attrMod.getValuesToBeRemoved().add("Surname");
        attrMod.getValuesToBeAdded().add("Surname2");

        userMod.getPlainAttrsToUpdate().add(attrMod);

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertEquals("Surname2", connObjectTO.getPlainAttrMap().get("SURNAME").getValues().get(0));

        // attribute "name" mapped on virtual attribute "virtualdata" shouldn't be changed
        assertFalse(connObjectTO.getPlainAttrMap().get("NAME").getValues().isEmpty());
        assertEquals("virtualvalue2", connObjectTO.getPlainAttrMap().get("NAME").getValues().get(0));
        // ----------------------------------

        // ----------------------------------
        // remove user virtual attribute and check virtual attribute value (reset)
        // ----------------------------------
        userMod = new UserMod();
        userMod.setKey(userTO.getKey());
        userMod.getVirAttrsToRemove().add("virtualdata");

        userTO = updateUser(userMod);
        assertNotNull(userTO);
        assertTrue(userTO.getVirAttrs().isEmpty());
        assertFalse(userTO.getPropagationStatusTOs().isEmpty());
        assertEquals(RESOURCE_NAME_WS2, userTO.getPropagationStatusTOs().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUBMITTED, userTO.getPropagationStatusTOs().get(0).getStatus());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_WS2, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // attribute "name" mapped on virtual attribute "virtualdata" should be reset
        assertTrue(connObjectTO.getPlainAttrMap().get("NAME").getValues() == null
                || connObjectTO.getPlainAttrMap().get("NAME").getValues().isEmpty());
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
        UserTO actual = createUser(userTO);
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = userService.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        // 3. update virtual attribute directly
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testsync set USERNAME='virattrcache2' WHERE ID=?", actual.getKey());

        value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache2", value);

        // 4. check for cached attribute value
        actual = userService.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        UserMod userMod = new UserMod();
        userMod.setKey(actual.getKey());

        AttrMod virtualdata = new AttrMod();
        virtualdata.setSchema("virtualdata");
        virtualdata.getValuesToBeAdded().add("virtualupdated");

        userMod.getVirAttrsToRemove().add("virtualdata");
        userMod.getVirAttrsToUpdate().add(virtualdata);

        // 5. update virtual attribute
        actual = updateUser(userMod);
        assertNotNull(actual);

        // 6. check for virtual attribute value
        actual = userService.read(actual.getKey());
        assertNotNull(actual);
        assertEquals("virtualupdated", actual.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE397() {
        ResourceTO csv = resourceService.read(RESOURCE_NAME_CSV);
        MappingTO origMapping = SerializationUtils.clone(csv.getProvisions().get(0).getMapping());
        try {
            // change mapping of resource-csv
            assertNotNull(origMapping);
            for (MappingItemTO item : csv.getProvisions().get(0).getMapping().getItems()) {
                if ("email".equals(item.getIntAttrName())) {
                    // unset internal attribute mail and set virtual attribute virtualdata as mapped to external email
                    item.setIntMappingType(IntMappingType.UserVirtualSchema);
                    item.setIntAttrName("virtualdata");
                    item.setPurpose(MappingPurpose.BOTH);
                    item.setExtAttrName("email");
                }
            }

            resourceService.update(csv.getKey(), csv);
            csv = resourceService.read(RESOURCE_NAME_CSV);
            assertNotNull(csv.getProvisions().get(0).getMapping());

            boolean found = false;
            for (MappingItemTO item : csv.getProvisions().get(0).getMapping().getItems()) {
                if ("email".equals(item.getExtAttrName()) && "virtualdata".equals(item.getIntAttrName())) {
                    found = true;
                }
            }

            assertTrue(found);

            // create a new user
            UserTO userTO = UserITCase.getUniqueSampleTO("syncope397@syncope.apache.org");
            userTO.getResources().clear();
            userTO.getMemberships().clear();
            userTO.getDerAttrs().clear();
            userTO.getVirAttrs().clear();

            userTO.getDerAttrs().add(attrTO("csvuserid", null));
            userTO.getDerAttrs().add(attrTO("cn", null));
            userTO.getVirAttrs().add(attrTO("virtualdata", "test@testone.org"));
            // assign resource-csv to user
            userTO.getResources().add(RESOURCE_NAME_CSV);
            // save user
            UserTO created = createUser(userTO);
            // make std controls about user
            assertNotNull(created);
            assertTrue(RESOURCE_NAME_CSV.equals(created.getResources().iterator().next()));
            // update user
            UserTO toBeUpdated = userService.read(created.getKey());
            UserMod userMod = new UserMod();
            userMod.setKey(toBeUpdated.getKey());
            userMod.setPassword("password234");
            // assign new resource to user
            userMod.getResourcesToAdd().add(RESOURCE_NAME_WS2);
            //modify virtual attribute
            userMod.getVirAttrsToRemove().add("virtualdata");
            userMod.getVirAttrsToUpdate().add(attrMod("virtualdata", "test@testoneone.com"));

            // check Syncope change password
            StatusMod pwdPropRequest = new StatusMod();
            pwdPropRequest.setOnSyncope(true);
            pwdPropRequest.getResourceNames().add(RESOURCE_NAME_WS2);
            userMod.setPwdPropRequest(pwdPropRequest);

            toBeUpdated = updateUser(userMod);
            assertNotNull(toBeUpdated);
            assertEquals("test@testoneone.com", toBeUpdated.getVirAttrs().iterator().next().getValues().get(0));
            // check if propagates correctly with assertEquals on size of tasks list
            assertEquals(2, toBeUpdated.getPropagationStatusTOs().size());
        } finally {
            // restore mapping of resource-csv
            csv.getProvisions().get(0).setMapping(origMapping);
            resourceService.update(csv.getKey(), csv);
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
        UserTO actual = createUser(userTO);
        assertNotNull(actual);

        // 2. check for virtual attribute value
        actual = userService.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ----------------------------------------
        // 3. force cache expiring without any modification
        // ----------------------------------------
        String jdbcURL = null;
        ConnInstanceTO connInstanceBean = connectorService.readByResource(RESOURCE_NAME_DBVIRATTR);
        for (ConnConfProperty prop : connInstanceBean.getConfiguration()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                jdbcURL = prop.getValues().iterator().next().toString();
                prop.getValues().clear();
                prop.getValues().add("jdbc:h2:tcp://localhost:9092/xxx");
            }
        }

        connectorService.update(connInstanceBean.getKey(), connInstanceBean);

        UserMod userMod = new UserMod();
        userMod.setKey(actual.getKey());

        AttrMod virtualdata = new AttrMod();
        virtualdata.setSchema("virtualdata");
        virtualdata.getValuesToBeAdded().add("virtualupdated");

        userMod.getVirAttrsToRemove().add("virtualdata");
        userMod.getVirAttrsToUpdate().add(virtualdata);

        actual = updateUser(userMod);
        assertNotNull(actual);
        // ----------------------------------------

        // ----------------------------------------
        // 4. update virtual attribute
        // ----------------------------------------
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache", value);

        jdbcTemplate.update("UPDATE testsync set USERNAME='virattrcache2' WHERE ID=?", actual.getKey());

        value = jdbcTemplate.queryForObject(
                "SELECT USERNAME FROM testsync WHERE ID=?", String.class, actual.getKey());
        assertEquals("virattrcache2", value);
        // ----------------------------------------

        actual = userService.read(actual.getKey());
        assertEquals("virattrcache", actual.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ----------------------------------------
        // 5. restore connector
        // ----------------------------------------
        for (ConnConfProperty prop : connInstanceBean.getConfiguration()) {
            if ("jdbcUrlTemplate".equals(prop.getSchema().getName())) {
                prop.getValues().clear();
                prop.getValues().add(jdbcURL);
            }
        }

        connectorService.update(connInstanceBean.getKey(), connInstanceBean);
        // ----------------------------------------

        actual = userService.read(actual.getKey());
        assertEquals("virattrcache2", actual.getVirAttrMap().get("virtualdata").getValues().get(0));
    }

    @Test
    public void issueSYNCOPE436() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope436@syncope.apache.org");
        userTO.getMemberships().clear();
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO.getVirAttrs().add(attrTO("virtualReadOnly", "readOnly"));
        userTO = createUser(userTO);
        //Finding no values because the virtual attribute is readonly 
        assertTrue(userTO.getVirAttrMap().get("virtualReadOnly").getValues().isEmpty());
    }

    @Test
    public void issueSYNCOPE453() {
        final String resourceName = "issueSYNCOPE453-Res-" + getUUIDString();
        final String groupName = "issueSYNCOPE453-Group-" + getUUIDString();

        // -------------------------------------------
        // Create a resource ad-hoc
        // -------------------------------------------
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setKey(resourceName);
        resourceTO.setConnectorId(107L);

        ProvisionTO provisionTO = new ProvisionTO();
        provisionTO.setAnyType(AnyTypeKind.USER.name());
        provisionTO.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resourceTO.getProvisions().add(provisionTO);

        MappingTO mapping = new MappingTO();
        provisionTO.setMapping(mapping);

        MappingItemTO item = new MappingItemTO();
        item.setIntAttrName("aLong");
        item.setIntMappingType(IntMappingType.UserPlainSchema);
        item.setExtAttrName(groupName);
        item.setPurpose(MappingPurpose.PROPAGATION);
        item.setAccountid(true);
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

        // -------------------------------------------
        // Create a VirAttrITCase ad-hoc
        // -------------------------------------------
        GroupTO groupTO = new GroupTO();
        groupTO.setName(groupName);
        groupTO.setRealm("/");
        groupTO.getVirAttrs().add(attrTO("rvirtualdata", "ml@group.it"));
        groupTO.getResources().add(RESOURCE_NAME_LDAP);
        groupTO = createGroup(groupTO);
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

        MembershipTO membership = new MembershipTO();
        membership.setRightKey(groupTO.getKey());
        userTO.getMemberships().add(membership);

        userTO = createUser(userTO);
        assertEquals(2, userTO.getPropagationStatusTOs().size());
        assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());
        assertTrue(userTO.getPropagationStatusTOs().get(1).getStatus().isSuccessful());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        final Map<String, Object> actuals = jdbcTemplate.queryForMap(
                "SELECT id, surname, email FROM testsync WHERE id=?",
                new Object[] { Integer.parseInt(userTO.getPlainAttrMap().get("aLong").getValues().get(0)) });

        assertEquals(userTO.getPlainAttrMap().get("aLong").getValues().get(0), actuals.get("id").toString());
        assertEquals("ml@group.it", actuals.get("email"));
        // -------------------------------------------

        // -------------------------------------------
        // Delete resource and group ad-hoc
        // -------------------------------------------
        resourceService.delete(resourceName);
        groupService.delete(groupTO.getKey());
        // -------------------------------------------
    }

    @Test
    public void issueSYNCOPE459() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope459@apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();

        AttrTO virtualReadOnly = attrTO("virtualReadOnly", "");
        virtualReadOnly.getValues().clear();

        userTO.getVirAttrs().add(virtualReadOnly);

        userTO = createUser(userTO);

        assertNotNull(userTO.getVirAttrMap().get("virtualReadOnly"));

        UserMod userMod = new UserMod();
        userMod.setKey(userTO.getKey());

        AttrMod virtualdata = new AttrMod();
        virtualdata.setSchema("virtualdata");

        userMod.getVirAttrsToUpdate().add(virtualdata);

        userTO = updateUser(userMod);
        assertNotNull(userTO.getVirAttrMap().get("virtualdata"));
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
        final AttrTO virtualData = attrTO("virtualdata", "syncope501@apache.org");
        userTO.getVirAttrs().add(virtualData);

        userTO = createUser(userTO);

        assertNotNull(userTO.getVirAttrMap().get("virtualdata"));
        assertEquals("syncope501@apache.org", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // 2. update virtual attribute
        UserMod userMod = new UserMod();
        userMod.setKey(userTO.getKey());

        final StatusMod statusMod = new StatusMod();
        statusMod.getResourceNames().addAll(Collections.<String>emptySet());
        statusMod.setOnSyncope(false);

        userMod.setPwdPropRequest(statusMod);
        // change virtual attribute value
        final AttrMod virtualDataMod = new AttrMod();
        virtualDataMod.setSchema("virtualdata");
        virtualDataMod.getValuesToBeAdded().add("syncope501_updated@apache.org");
        virtualDataMod.getValuesToBeRemoved().add("syncope501@apache.org");
        userMod.getVirAttrsToUpdate().add(virtualDataMod);
        userMod.getVirAttrsToRemove().add("virtualdata");

        userTO = updateUser(userMod);
        assertNotNull(userTO);

        // 3. check that user virtual attribute has really been updated 
        assertFalse(userTO.getVirAttrMap().get("virtualdata").getValues().isEmpty());
        assertEquals("syncope501_updated@apache.org", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));
    }
}
