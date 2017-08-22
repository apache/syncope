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
import static org.junit.Assert.fail;

import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCEnv.xml" })
public class MembershipITCase extends AbstractITCase {

    @Autowired
    private DataSource testDataSource;

    @Test
    public void misc() {
        UserTO user = UserITCase.getUniqueSampleTO("memb@apache.org");
        user.setRealm("/even/two");
        user.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1976").build());
        user.getPlainAttrs().remove(user.getPlainAttr("ctype").get());

        // the group 034740a9-fa10-453b-af37-dc7897e98fb1 has USER type extensions for 'csv' and 'other' 
        // any type classes
        MembershipTO membership = new MembershipTO.Builder().group("034740a9-fa10-453b-af37-dc7897e98fb1").build();
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1977").build());

        // 'fullname' is in 'minimal user', so it is not allowed for this membership
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("fullname").value("discarded").build());

        user.getMemberships().add(membership);

        // user creation fails because of fullname
        try {
            createUser(user);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertTrue(e.getMessage().contains("InvalidPlainAttr: fullname not allowed for membership of group"));
        }

        // remove fullname and try again
        membership.getPlainAttrs().remove(membership.getPlainAttr("fullname").get());
        try {
            user = createUser(user).getEntity();

            // 1. verify that 'aLong' is correctly populated for user
            assertEquals(1, user.getPlainAttr("aLong").get().getValues().size());
            assertEquals("1976", user.getPlainAttr("aLong").get().getValues().get(0));

            // 2. verify that 'aLong' is correctly populated for user's membership
            assertEquals(1, user.getMemberships().size());
            membership = user.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1").get();
            assertNotNull(membership);
            assertEquals(1, membership.getPlainAttr("aLong").get().getValues().size());
            assertEquals("1977", membership.getPlainAttr("aLong").get().getValues().get(0));

            // 3. verify that derived attrbutes from 'csv' and 'other' are also populated for user's membership
            assertFalse(membership.getDerAttr("csvuserid").get().getValues().isEmpty());
            assertFalse(membership.getDerAttr("noschema").get().getValues().isEmpty());

            // update user - change some values and add new membership attribute
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(user.getKey());

            userPatch.getPlainAttrs().add(new AttrPatch.Builder().
                    attrTO(new AttrTO.Builder().schema("aLong").value("1977").build()).build());

            MembershipPatch membershipPatch = new MembershipPatch.Builder().group(membership.getGroupKey()).build();
            membershipPatch.getPlainAttrs().add(
                    new AttrTO.Builder().schema("aLong").value("1976").build());
            membershipPatch.getPlainAttrs().add(
                    new AttrTO.Builder().schema("ctype").value("membership type").build());
            userPatch.getMemberships().add(membershipPatch);

            user = updateUser(userPatch).getEntity();

            // 4. verify that 'aLong' is correctly populated for user
            assertEquals(1, user.getPlainAttr("aLong").get().getValues().size());
            assertEquals("1977", user.getPlainAttr("aLong").get().getValues().get(0));
            assertFalse(user.getPlainAttr("ctype").isPresent());

            // 5. verify that 'aLong' is correctly populated for user's membership
            assertEquals(1, user.getMemberships().size());
            membership = user.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1").get();
            assertNotNull(membership);
            assertEquals(1, membership.getPlainAttr("aLong").get().getValues().size());
            assertEquals("1976", membership.getPlainAttr("aLong").get().getValues().get(0));

            // 6. verify that 'ctype' is correctly populated for user's membership
            assertEquals("membership type", membership.getPlainAttr("ctype").get().getValues().get(0));

            // finally remove membership
            userPatch = new UserPatch();
            userPatch.setKey(user.getKey());

            membershipPatch = new MembershipPatch.Builder().group(membership.getGroupKey()).
                    operation(PatchOperation.DELETE).build();
            userPatch.getMemberships().add(membershipPatch);

            user = updateUser(userPatch).getEntity();

            assertTrue(user.getMemberships().isEmpty());
        } finally {
            if (user.getKey() != null) {
                userService.delete(user.getKey());
            }
        }
    }

    @Test
    public void deleteUserWithMembership() {
        UserTO user = UserITCase.getUniqueSampleTO("memb@apache.org");
        user.setRealm("/even/two");
        user.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1976").build());

        MembershipTO membership = new MembershipTO.Builder().group("034740a9-fa10-453b-af37-dc7897e98fb1").build();
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1977").build());
        user.getMemberships().add(membership);

        user = createUser(user).getEntity();
        assertNotNull(user.getKey());

        userService.delete(user.getKey());
    }

    @Test
    public void onGroupDelete() {
        // pre: create group with type extension
        TypeExtensionTO typeExtension = new TypeExtensionTO();
        typeExtension.setAnyType(AnyTypeKind.USER.name());
        typeExtension.getAuxClasses().add("csv");
        typeExtension.getAuxClasses().add("other");

        GroupTO groupTO = GroupITCase.getBasicSampleTO("typeExt");
        groupTO.getTypeExtensions().add(typeExtension);
        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);

        // pre: create user with membership to such group
        UserTO user = UserITCase.getUniqueSampleTO("typeExt@apache.org");

        MembershipTO membership = new MembershipTO.Builder().group(groupTO.getKey()).build();
        membership.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("1454").build());
        user.getMemberships().add(membership);

        user = createUser(user).getEntity();

        // verify that 'aLong' is correctly populated for user's membership
        assertEquals(1, user.getMemberships().size());
        membership = user.getMembership(groupTO.getKey()).get();
        assertNotNull(membership);
        assertEquals(1, membership.getPlainAttr("aLong").get().getValues().size());
        assertEquals("1454", membership.getPlainAttr("aLong").get().getValues().get(0));

        // verify that derived attrbutes from 'csv' and 'other' are also populated for user's membership
        assertFalse(membership.getDerAttr("csvuserid").get().getValues().isEmpty());
        assertFalse(membership.getDerAttr("noschema").get().getValues().isEmpty());

        // now remove the group -> all related memberships should have been removed as well
        groupService.delete(groupTO.getKey());

        // re-read user and verify that no memberships are available any more
        user = userService.read(user.getKey());
        assertTrue(user.getMemberships().isEmpty());
    }

    @Test
    public void pull() {
        // 0. create ad-hoc resource, with adequate mapping
        ResourceTO newResource = resourceService.read(RESOURCE_NAME_DBPULL);
        newResource.setKey(getUUIDString());

        ItemTO item = newResource.getProvision("USER").get().getMapping().getItems().stream().
                filter(object -> "firstname".equals(object.getIntAttrName())).findFirst().get();
        assertNotNull(item);
        assertEquals("ID", item.getExtAttrName());
        item.setIntAttrName("memberships[additional].aLong");
        item.setPurpose(MappingPurpose.BOTH);

        item = newResource.getProvision("USER").get().getMapping().getItems().stream().
                filter(object -> "fullname".equals(object.getIntAttrName())).findFirst().get();
        item.setPurpose(MappingPurpose.PULL);

        PullTaskTO newTask = null;
        try {
            newResource = createResource(newResource);
            assertNotNull(newResource);

            // 1. create user with new resource assigned
            UserTO user = UserITCase.getUniqueSampleTO("memb@apache.org");
            user.setRealm("/even/two");
            user.getPlainAttrs().remove(user.getPlainAttr("ctype").get());
            user.getResources().clear();
            user.getResources().add(newResource.getKey());

            MembershipTO membership = new MembershipTO.Builder().group("034740a9-fa10-453b-af37-dc7897e98fb1").build();
            membership.getPlainAttrs().add(new AttrTO.Builder().schema("aLong").value("5432").build());
            user.getMemberships().add(membership);

            user = createUser(user).getEntity();
            assertNotNull(user);

            // 2. verify that user was found on resource
            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
            String idOnResource = queryForObject(
                    jdbcTemplate, 50, "SELECT id FROM testpull WHERE id=?", String.class, "5432");
            assertEquals("5432", idOnResource);

            // 3. unlink user from resource, then remove it
            DeassociationPatch patch = new DeassociationPatch();
            patch.setKey(user.getKey());
            patch.setAction(ResourceDeassociationAction.UNLINK);
            patch.getResources().add(newResource.getKey());
            assertNotNull(userService.deassociate(patch).readEntity(BulkActionResult.class));

            userService.delete(user.getKey());

            // 4. create pull task and execute
            newTask = taskService.read("7c2242f4-14af-4ab5-af31-cdae23783655", true);
            newTask.setResource(newResource.getKey());
            newTask.setDestinationRealm("/even/two");

            Response response = taskService.create(newTask);
            newTask = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(newTask);

            ExecTO execution = AbstractTaskITCase.execProvisioningTask(taskService, newTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // 5. verify that pulled user has
            PagedResult<UserTO> users = userService.search(new AnyQuery.Builder().
                    realm("/").
                    fiql(SyncopeClient.getUserSearchConditionBuilder().
                            is("username").equalTo(user.getUsername()).query()).build());
            assertEquals(1, users.getTotalCount());
            assertEquals(1, users.getResult().get(0).getMemberships().size());
            assertEquals("5432", users.getResult().get(0).getMemberships().get(0).
                    getPlainAttr("aLong").get().getValues().get(0));
        } catch (Exception e) {
            LOG.error("Unexpected error", e);
            fail(e.getMessage());
        } finally {
            if (newTask != null && !"83f7e85d-9774-43fe-adba-ccd856312994".equals(newTask.getKey())) {
                taskService.delete(newTask.getKey());
            }
            resourceService.delete(newResource.getKey());
        }
    }
}
