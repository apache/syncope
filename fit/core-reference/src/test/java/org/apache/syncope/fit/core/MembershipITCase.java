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
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class MembershipITCase extends AbstractITCase {

    @Test
    public void misc() throws JsonProcessingException {
        UserCR userCR = UserITCase.getUniqueSample("memb@apache.org");
        userCR.setRealm("/even/two");
        userCR.getPlainAttrs().add(new Attr.Builder("aLong").value("1976").build());
        userCR.getPlainAttrs().removeIf(attr -> "ctype".equals(attr.getSchema()));

        // the group 034740a9-fa10-453b-af37-dc7897e98fb1 has USER type extensions for 'csv' and 'other' 
        // any type classes
        MembershipTO membership = new MembershipTO.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").build();
        membership.getPlainAttrs().add(new Attr.Builder("aLong").value("1977").build());

        // 'fullname' is in 'minimal user', so it is not allowed for this membership
        membership.getPlainAttrs().add(new Attr.Builder("fullname").value("discarded").build());

        userCR.getMemberships().add(membership);

        // user creation fails because of fullname
        try {
            createUser(userCR);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidEntity, e.getType());
            assertTrue(e.getMessage().contains("InvalidPlainAttr: fullname not allowed for membership of group"));
        }

        // remove fullname and try again
        membership.getPlainAttrs().remove(membership.getPlainAttr("fullname").orElseThrow());
        UserTO userTO = null;
        try {
            userTO = createUser(userCR).getEntity();

            // 1. verify that 'aLong' is correctly populated for user
            assertEquals(1, userTO.getPlainAttr("aLong").orElseThrow().getValues().size());
            assertEquals("1976", userTO.getPlainAttr("aLong").orElseThrow().getValues().getFirst());

            // 2. verify that 'aLong' is correctly populated for user's membership
            assertEquals(1, userCR.getMemberships().size());
            membership = userTO.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1").orElseThrow();
            assertNotNull(membership);
            assertEquals(1, membership.getPlainAttr("aLong").orElseThrow().getValues().size());
            assertEquals("1977", membership.getPlainAttr("aLong").orElseThrow().getValues().getFirst());

            // 3. verify that derived attributes from 'csv' and 'other' are also populated for user's membership
            assertFalse(membership.getDerAttr("csvuserid").orElseThrow().getValues().isEmpty());
            assertFalse(membership.getDerAttr("noschema").orElseThrow().getValues().isEmpty());

            // update user - change some values and add new membership attribute
            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());

            userUR.getPlainAttrs().
                    add(new AttrPatch.Builder(new Attr.Builder("aLong").value("1977").build()).build());

            MembershipUR membershipPatch = new MembershipUR.Builder(membership.getGroupKey()).build();
            membershipPatch.getPlainAttrs().add(new Attr.Builder("aLong").value("1976").build());
            membershipPatch.getPlainAttrs().add(new Attr.Builder("ctype").value("membership type").build());
            userUR.getMemberships().add(membershipPatch);

            userTO = updateUser(userUR).getEntity();

            // 4. verify that 'aLong' is correctly populated for user
            assertEquals(1, userTO.getPlainAttr("aLong").orElseThrow().getValues().size());
            assertEquals("1977", userTO.getPlainAttr("aLong").orElseThrow().getValues().getFirst());
            assertFalse(userTO.getPlainAttr("ctype").isPresent());

            // 5. verify that 'aLong' is correctly populated for user's membership
            assertEquals(1, userCR.getMemberships().size());
            membership = userTO.getMembership("034740a9-fa10-453b-af37-dc7897e98fb1").orElseThrow();
            assertNotNull(membership);
            assertEquals(1, membership.getPlainAttr("aLong").orElseThrow().getValues().size());
            assertEquals("1976", membership.getPlainAttr("aLong").orElseThrow().getValues().getFirst());

            // 6. verify that 'ctype' is correctly populated for user's membership
            assertEquals("membership type", membership.getPlainAttr("ctype").orElseThrow().getValues().getFirst());

            // finally remove membership
            userUR = new UserUR();
            userUR.setKey(userTO.getKey());

            membershipPatch = new MembershipUR.Builder(membership.getGroupKey()).
                    operation(PatchOperation.DELETE).build();
            userUR.getMemberships().add(membershipPatch);

            userTO = updateUser(userUR).getEntity();

            assertTrue(userTO.getMemberships().isEmpty());
        } finally {
            if (userTO != null) {
                USER_SERVICE.delete(userTO.getKey());
            }
        }
    }

    @Test
    public void deleteUserWithMembership() {
        UserCR userCR = UserITCase.getUniqueSample("memb@apache.org");
        userCR.setRealm("/even/two");
        userCR.getPlainAttrs().add(new Attr.Builder("aLong").value("1976").build());

        MembershipTO membership = new MembershipTO.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").build();
        membership.getPlainAttrs().add(new Attr.Builder("aLong").value("1977").build());
        userCR.getMemberships().add(membership);

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user.getKey());

        USER_SERVICE.delete(user.getKey());
    }

    @Test
    public void onGroupDelete() {
        // pre: create group with type extension
        TypeExtensionTO typeExtension = new TypeExtensionTO();
        typeExtension.setAnyType(AnyTypeKind.USER.name());
        typeExtension.getAuxClasses().add("csv");
        typeExtension.getAuxClasses().add("other");

        GroupCR groupCR = GroupITCase.getBasicSample("typeExt");
        groupCR.getTypeExtensions().add(typeExtension);
        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);

        // pre: create user with membership to such group
        UserCR userCR = UserITCase.getUniqueSample("typeExt@apache.org");

        MembershipTO membership = new MembershipTO.Builder(groupTO.getKey()).build();
        membership.getPlainAttrs().add(new Attr.Builder("aLong").value("1454").build());
        userCR.getMemberships().add(membership);

        UserTO user = createUser(userCR).getEntity();

        // verify that 'aLong' is correctly populated for user's membership
        assertEquals(1, user.getMemberships().size());
        membership = user.getMembership(groupTO.getKey()).orElseThrow();
        assertNotNull(membership);
        assertEquals(1, membership.getPlainAttr("aLong").orElseThrow().getValues().size());
        assertEquals("1454", membership.getPlainAttr("aLong").orElseThrow().getValues().getFirst());

        // verify that derived attrbutes from 'csv' and 'other' are also populated for user's membership
        assertFalse(membership.getDerAttr("csvuserid").orElseThrow().getValues().isEmpty());
        assertFalse(membership.getDerAttr("noschema").orElseThrow().getValues().isEmpty());

        // now remove the group -> all related memberships should have been removed as well
        GROUP_SERVICE.delete(groupTO.getKey());

        // re-read user and verify that no memberships are available any more
        user = USER_SERVICE.read(user.getKey());
        assertTrue(user.getMemberships().isEmpty());
    }

    @Test
    public void pull() {
        // 0. create ad-hoc resource, with adequate mapping
        ResourceTO newResource = RESOURCE_SERVICE.read(RESOURCE_NAME_DBPULL);
        newResource.setKey(getUUIDString());

        Item item = newResource.getProvision("USER").orElseThrow().getMapping().getItems().stream().
                filter(object -> "firstname".equals(object.getIntAttrName())).findFirst().orElseThrow();
        assertNotNull(item);
        assertEquals("ID", item.getExtAttrName());
        item.setIntAttrName("memberships[additional].aLong");
        item.setPurpose(MappingPurpose.BOTH);

        item = newResource.getProvision("USER").orElseThrow().getMapping().getItems().stream().
                filter(object -> "fullname".equals(object.getIntAttrName())).findFirst().orElseThrow();
        item.setPurpose(MappingPurpose.PULL);

        PullTaskTO newTask = null;
        try {
            newResource = createResource(newResource);
            assertNotNull(newResource);

            // 1. create user with new resource assigned
            UserCR userCR = UserITCase.getUniqueSample("memb@apache.org");
            userCR.setRealm("/even/two");
            UserTO user;
            userCR.getPlainAttrs().removeIf(attr -> "ctype".equals(attr.getSchema()));
            userCR.getResources().clear();
            userCR.getResources().add(newResource.getKey());

            MembershipTO membership = new MembershipTO.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").build();
            membership.getPlainAttrs().add(new Attr.Builder("aLong").value("5432").build());
            userCR.getMemberships().add(membership);

            user = createUser(userCR).getEntity();
            assertNotNull(user);

            // 2. verify that user was found on resource
            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
            String idOnResource = queryForObject(
                    jdbcTemplate, MAX_WAIT_SECONDS, "SELECT id FROM testpull WHERE id=?", String.class, "5432");
            assertEquals("5432", idOnResource);

            // 3. unlink user from resource, then remove it
            ResourceDR req = new ResourceDR();
            req.setKey(user.getKey());
            req.setAction(ResourceDeassociationAction.UNLINK);
            req.getResources().add(newResource.getKey());
            assertNotNull(parseBatchResponse(USER_SERVICE.deassociate(req)));

            USER_SERVICE.delete(user.getKey());

            // 4. create pull task and execute
            newTask = TASK_SERVICE.read(TaskType.PULL, "7c2242f4-14af-4ab5-af31-cdae23783655", true);
            newTask.setName(getUUIDString());
            newTask.setResource(newResource.getKey());
            newTask.setDestinationRealm("/even/two");

            Response response = TASK_SERVICE.create(TaskType.PULL, newTask);
            newTask = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(newTask);

            ExecTO execution = AbstractTaskITCase.execSchedTask(
                    TASK_SERVICE, TaskType.PULL, newTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            // 5. verify that pulled user has
            if (IS_EXT_SEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
            PagedResult<UserTO> users = USER_SERVICE.search(new AnyQuery.Builder().
                    realm("/").
                    fiql(SyncopeClient.getUserSearchConditionBuilder().
                            is("username").equalTo(user.getUsername()).query()).build());
            assertEquals(1, users.getTotalCount());
            assertEquals(1, users.getResult().getFirst().getMemberships().size());
            assertEquals("5432", users.getResult().getFirst().getMemberships().getFirst().
                    getPlainAttr("aLong").orElseThrow().getValues().getFirst());
        } catch (Exception e) {
            LOG.error("Unexpected error", e);
            fail(e::getMessage);
        } finally {
            if (newTask != null && !"83f7e85d-9774-43fe-adba-ccd856312994".equals(newTask.getKey())) {
                TASK_SERVICE.delete(TaskType.PULL, newTask.getKey());
            }
            RESOURCE_SERVICE.delete(newResource.getKey());
        }
    }

    @Test
    public void createDoubleMembership() {
        AnyObjectCR anyObjectCR = AnyObjectITCase.getSample("createDoubleMembership");
        anyObjectCR.setRealm("/even/two");
        anyObjectCR.getMemberships().add(new MembershipTO.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").build());
        anyObjectCR.getMemberships().add(new MembershipTO.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").build());

        try {
            createAnyObject(anyObjectCR);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidMembership, e.getType());
        }
    }

    @Test
    public void updateDoubleMembership() {
        AnyObjectCR anyObjecCR = AnyObjectITCase.getSample("update");
        anyObjecCR.setRealm("/even/two");
        AnyObjectTO anyObjecTO = createAnyObject(anyObjecCR).getEntity();
        assertNotNull(anyObjecTO.getKey());

        AnyObjectUR req = new AnyObjectUR();
        req.setKey(anyObjecTO.getKey());
        req.getMemberships().add(new MembershipUR.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").
                build());
        req.getMemberships().add(new MembershipUR.Builder("034740a9-fa10-453b-af37-dc7897e98fb1").
                plainAttr(attr("any", "useless")).build());

        try {
            updateAnyObject(req).getEntity();
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidMembership, e.getType());
        }
    }
}
