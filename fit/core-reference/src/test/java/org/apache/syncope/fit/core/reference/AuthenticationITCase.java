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
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormPropertyTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.wrap.ResourceKey;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.misc.security.Encryptor;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class AuthenticationITCase extends AbstractITCase {

    private int getFailedLogins(final UserService userService, final long userId) {
        UserTO readUserTO = userService.read(userId);
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        return readUserTO.getFailedLogins();
    }

    private void assertReadFails(final SyncopeClient client) {
        try {
            client.self();
            fail("access should not work");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testReadEntitlements() {
        // 1. as anonymous (not allowed)
        try {
            clientFactory.createAnonymous().self();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 2. as authenticated anonymous (used by admin console)
        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create(ANONYMOUS_UNAME, ANONYMOUS_KEY).self();
        assertEquals(1, self.getKey().size());
        assertTrue(self.getKey().keySet().contains(Entitlement.ANONYMOUS));
        assertEquals(ANONYMOUS_UNAME, self.getValue().getUsername());

        // 3. as admin
        self = adminClient.self();
        assertEquals(Entitlement.values().size() - 1, self.getKey().size());
        assertFalse(self.getKey().keySet().contains(Entitlement.ANONYMOUS));
        assertEquals(ADMIN_UNAME, self.getValue().getUsername());

        // 4. as user
        self = clientFactory.create("bellini", ADMIN_PWD).self();
        assertFalse(self.getKey().isEmpty());
        assertFalse(self.getKey().keySet().contains(Entitlement.ANONYMOUS));
        assertEquals("bellini", self.getValue().getUsername());
    }

    @Test
    public void testUserSchemaAuthorization() {
        // 0. create a role that can only read schemas
        RoleTO roleTO = new RoleTO();
        roleTO.setName("authRole" + getUUIDString());
        roleTO.getEntitlements().add(Entitlement.SCHEMA_READ);
        roleTO.getRealms().add("/odd");

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);

        String schemaName = "authTestSchema" + getUUIDString();

        // 1. create a schema (as admin)
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey(schemaName);
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(AttrSchemaType.String);

        PlainSchemaTO newPlainSchemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertEquals(schemaTO, newPlainSchemaTO);

        // 2. create an user with the role created above (as admin)
        UserTO userTO = UserITCase.getUniqueSampleTO("auth@test.org");
        userTO.getRoles().add(roleTO.getKey());

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = schemaService.read(SchemaType.PLAIN, schemaName);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        SchemaService schemaService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(SchemaService.class);
        schemaTO = schemaService2.read(SchemaType.PLAIN, schemaName);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        try {
            schemaService2.update(SchemaType.PLAIN, schemaTO);
            fail("Schemaupdate as user should not work");
        } catch (AccessControlException e) {
            // CXF Service will throw this exception
            assertNotNull(e);
        }

        assertEquals(0, getFailedLogins(userService, userTO.getKey()));
    }

    @Test
    public void testUserRead() {
        UserTO userTO = UserITCase.getUniqueSampleTO("testuserread@test.org");
        userTO.getRoles().add(2L);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        UserTO readUserTO = userService2.read(1L);
        assertNotNull(readUserTO);

        UserService userService3 = clientFactory.create("puccini", ADMIN_PWD).getService(UserService.class);

        try {
            userService3.read(3L);
            fail();
        } catch (SyncopeClientException e) {
            assertNotNull(e);
            assertEquals(ClientExceptionType.Unauthorized, e.getType());
        }
    }

    @Test
    public void testUserSearch() {
        UserTO userTO = UserITCase.getUniqueSampleTO("testusersearch@test.org");
        userTO.getRoles().add(1L);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        // 1. user assigned to role 1, with search entitlement on realms /odd and /even: won't find anything with 
        // root realm
        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);

        PagedResult<UserTO> matchedUsers = userService2.search(
                SyncopeClient.getAnySearchQueryBuilder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("key").query()).build());
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.getResult().isEmpty());
        Set<Long> matchedUserKeys = CollectionUtils.collect(matchedUsers.getResult(),
                new Transformer<UserTO, Long>() {

                    @Override
                    public Long transform(final UserTO input) {
                        return input.getKey();
                    }
                }, new HashSet<Long>());
        assertTrue(matchedUserKeys.contains(1L));
        assertFalse(matchedUserKeys.contains(2L));
        assertFalse(matchedUserKeys.contains(5L));

        // 2. user assigned to role 4, with search entitlement on realm /even/two
        UserService userService3 = clientFactory.create("puccini", ADMIN_PWD).getService(UserService.class);

        matchedUsers = userService3.search(
                SyncopeClient.getAnySearchQueryBuilder().realm("/even/two").
                fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("loginDate").query()).build());
        assertNotNull(matchedUsers);
        assertTrue(CollectionUtils.matchesAll(matchedUsers.getResult(), new Predicate<UserTO>() {

            @Override
            public boolean evaluate(final UserTO matched) {
                return "/even/two".equals(matched.getRealm());
            }
        }));
    }

    @Test
    public void checkFailedLogins() {
        UserTO userTO = UserITCase.getUniqueSampleTO("checkFailedLogin@syncope.apache.org");
        userTO.getRoles().add(2L);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        long userId = userTO.getKey();

        UserService userService2 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);
        assertEquals(0, getFailedLogins(userService2, userId));

        // authentications failed ...
        SyncopeClient badPwdClient = clientFactory.create(userTO.getUsername(), "wrongpwd1");
        assertReadFails(badPwdClient);
        assertReadFails(badPwdClient);

        assertEquals(2, getFailedLogins(userService, userId));

        UserService userService4 = clientFactory.create(userTO.getUsername(), "password123").
                getService(UserService.class);
        assertEquals(0, getFailedLogins(userService4, userId));
    }

    @Test
    public void checkUserSuspension() {
        UserTO userTO = UserITCase.getUniqueSampleTO("checkSuspension@syncope.apache.org");
        userTO.setRealm("/odd");
        userTO.getRoles().add(2L);

        userTO = createUser(userTO);
        long userKey = userTO.getKey();
        assertNotNull(userTO);

        assertEquals(0, getFailedLogins(userService, userKey));

        // authentications failed ...
        SyncopeClient badPwdClient = clientFactory.create(userTO.getUsername(), "wrongpwd1");
        assertReadFails(badPwdClient);
        assertReadFails(badPwdClient);
        assertReadFails(badPwdClient);

        assertEquals(3, getFailedLogins(userService, userKey));

        // last authentication before suspension
        assertReadFails(badPwdClient);

        userTO = userService.read(userTO.getKey());
        assertNotNull(userTO);
        assertNotNull(userTO.getFailedLogins());
        assertEquals(3, userTO.getFailedLogins(), 0);
        assertEquals("suspended", userTO.getStatus());

        // Access with correct credentials should fail as user is suspended
        SyncopeClient goodPwdClient = clientFactory.create(userTO.getUsername(), "password123");
        assertReadFails(goodPwdClient);

        StatusMod reactivate = new StatusMod();
        reactivate.setKey(userTO.getKey());
        reactivate.setType(StatusMod.ModType.REACTIVATE);
        userTO = userService.status(reactivate).readEntity(UserTO.class);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        assertEquals(0, goodPwdClient.self().getValue().getFailedLogins(), 0);
    }

    @Test
    public void issueSYNCOPE434() {
        Assume.assumeTrue(ActivitiDetector.isActivitiEnabledForUsers(syncopeService));

        // 1. create user with group 9 (users with group 9 are defined in workflow as subject to approval)
        UserTO userTO = UserITCase.getUniqueSampleTO("createWithReject@syncope.apache.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRightKey(9L);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals("createApproval", userTO.getStatus());

        // 2. try to authenticate: fail
        try {
            clientFactory.create(userTO.getUsername(), "password123").self();
            fail();
        } catch (AccessControlException e) {
            assertNotNull(e);
        }

        // 3. approve user
        WorkflowFormTO form = userWorkflowService.getFormForUser(userTO.getKey());
        form = userWorkflowService.claimForm(form.getTaskId());
        Map<String, WorkflowFormPropertyTO> props = form.getPropertyMap();
        props.get("approve").setValue(Boolean.TRUE.toString());
        form.getProperties().clear();
        form.getProperties().addAll(props.values());
        userTO = userWorkflowService.submitForm(form);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());

        // 4. try to authenticate again: success
        Pair<Map<String, Set<String>>, UserTO> self =
                clientFactory.create(userTO.getUsername(), "password123").self();
        assertNotNull(self);
        assertNotNull(self.getKey());
        assertNotNull(self.getValue());
    }

    @Test
    public void issueSYNCOPE164() throws Exception {
        // 1. create user with db resource
        UserTO user = UserITCase.getUniqueSampleTO("syncope164@syncope.apache.org");
        user.setRealm("/even/two");
        user.setPassword("password123");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user = createUser(user);
        assertNotNull(user);

        // 2. unlink the resource from the created user
        assertNotNull(userService.deassociate(user.getKey(),
                ResourceDeassociationActionType.UNLINK,
                CollectionWrapper.wrap(RESOURCE_NAME_TESTDB, ResourceKey.class)).
                readEntity(BulkActionResult.class));

        // 3. change password on Syncope
        UserMod userMod = new UserMod();
        userMod.setKey(user.getKey());
        userMod.setPassword("password234");
        user = updateUser(userMod);
        assertNotNull(user);

        // 4. check that the db resource has still the initial password value
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = jdbcTemplate.queryForObject(
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("password123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 5. successfully authenticate with old (on db resource) and new (on internal storage) password values
        Pair<Map<String, Set<String>>, UserTO> self =
                clientFactory.create(user.getUsername(), "password123").self();
        assertNotNull(self);
        self = clientFactory.create(user.getUsername(), "password234").self();
        assertNotNull(self);
    }
}
