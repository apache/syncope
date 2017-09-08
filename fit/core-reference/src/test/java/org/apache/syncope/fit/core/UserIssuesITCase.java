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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.GenericType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.StringReplacePatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.core.provisioning.java.propagation.DBPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPPasswordPropagationActions;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.DoubleValueLogicActions;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCEnv.xml" })
public class UserIssuesITCase extends AbstractITCase {

    @Autowired
    private DataSource testDataSource;

    @Test
    public void issue186() {
        // 1. create an user with strict mandatory attributes only
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "issue186@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password123");

        userTO.getPlainAttrs().add(attrTO("userId", userId));
        userTO.getPlainAttrs().add(attrTO("fullname", userId));
        userTO.getPlainAttrs().add(attrTO("surname", userId));

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource forcing mandatory constraints: must fail with RequiredValuesMissing
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS2).build());

        try {
            userTO = updateUser(userPatch).getEntity();
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        // 3. update assigning a resource NOT forcing mandatory constraints
        // AND priority: must fail with PropagationException
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        userTO = result.getEntity();

        // 4. update assigning a resource NOT forcing mandatory constraints
        // BUT not priority: must succeed
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123456").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_CSV).build());

        updateUser(userPatch);
    }

    @Test
    public void issue213() {
        UserTO userTO = UserITCase.getUniqueSampleTO("issue213@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getResources().size());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String username = queryForObject(
                jdbcTemplate, 50, "SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
        assertEquals(userTO.getUsername(), username);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(RESOURCE_NAME_TESTDB).build());

        userTO = updateUser(userPatch).getEntity();
        assertTrue(userTO.getResources().isEmpty());

        Exception exception = null;
        try {
            jdbcTemplate.queryForObject("SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
        } catch (EmptyResultDataAccessException e) {
            exception = e;
        }
        assertNotNull(exception);
    }

    @Test
    public void issue234() {
        UserTO inUserTO = UserITCase.getUniqueSampleTO("issue234@syncope.apache.org");
        inUserTO.getResources().add(RESOURCE_NAME_LDAP);

        UserTO userTO = createUser(inUserTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();

        userPatch.setKey(userTO.getKey());
        userPatch.setUsername(new StringReplacePatchItem.Builder().value("1" + userTO.getUsername()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertEquals("1" + inUserTO.getUsername(), userTO.getUsername());
    }

    @Test
    public final void issue280() {
        UserTO userTO = UserITCase.getUniqueSampleTO("issue280@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).
                resource(RESOURCE_NAME_TESTDB).value("123password").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);

        List<PropagationStatus> propagations = result.getPropagationStatuses();
        assertNotNull(propagations);
        assertEquals(1, propagations.size());

        assertEquals(PropagationTaskExecStatus.SUCCESS, propagations.get(0).getStatus());

        String resource = propagations.get(0).getResource();
        assertEquals(RESOURCE_NAME_TESTDB, resource);
    }

    @Test
    public void issue281() {
        UserTO userTO = UserITCase.getUniqueSampleTO("issue281@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getResources().add(RESOURCE_NAME_CSV);

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);

        List<PropagationStatus> propagations = result.getPropagationStatuses();
        assertNotNull(propagations);
        assertEquals(1, propagations.size());
        assertNotEquals(PropagationTaskExecStatus.SUCCESS, propagations.get(0).getStatus());

        String resource = propagations.get(0).getResource();
        assertEquals(RESOURCE_NAME_CSV, resource);
    }

    @Test
    public void issue288() {
        UserTO userTO = UserITCase.getSampleTO("issue288@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO("aLong", "STRING"));

        try {
            createUser(userTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidValues, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE108() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope108@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        userTO.getMemberships().add(new MembershipTO.Builder().
                group("0626100b-a4ba-4e00-9971-86fad52a6216").build());
        userTO.getMemberships().add(new MembershipTO.Builder().
                group("ba9ed509-b1f5-48ab-a334-c8530a6422dc").build());

        userTO.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertEquals(2, userTO.getMemberships().size());
        assertEquals(1, userTO.getResources().size());

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // -----------------------------------
        // Remove the first membership: de-provisioning shouldn't happen
        // -----------------------------------
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        userPatch.getMemberships().add(new MembershipPatch.Builder().
                operation(PatchOperation.DELETE).group(userTO.getMemberships().get(0).getGroupKey()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the resource assigned directly: de-provisioning shouldn't happen
        // -----------------------------------
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        userPatch.getResources().add(new StringPatchItem.Builder().operation(PatchOperation.DELETE).
                value(userTO.getResources().iterator().next()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertFalse(userTO.getResources().isEmpty());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the first membership: de-provisioning should happen
        // -----------------------------------
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        userPatch.getMemberships().add(new MembershipPatch.Builder().
                operation(PatchOperation.DELETE).group(userTO.getMemberships().get(0).getGroupKey()).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getMemberships().isEmpty());
        assertTrue(userTO.getResources().isEmpty());

        try {
            resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
            fail("Read should not succeeed");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE185() {
        // 1. create user with LDAP resource, succesfully propagated
        UserTO userTO = UserITCase.getSampleTO("syncope185@syncope.apache.org");
        userTO.getVirAttrs().clear();
        userTO.getResources().add(RESOURCE_NAME_LDAP);

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        // 2. delete this user
        userService.delete(userTO.getKey());

        // 3. try (and fail) to find this user on the external LDAP resource
        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
            fail("This entry should not be present on this resource");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test()
    public void issueSYNCOPE51() {
        AttrTO defaultCA = configurationService.get("password.cipher.algorithm");
        final String originalCAValue = defaultCA.getValues().get(0);
        defaultCA.getValues().set(0, "MD5");
        configurationService.set(defaultCA);

        AttrTO newCA = configurationService.get(defaultCA.getSchema());
        assertEquals(defaultCA, newCA);

        UserTO userTO = UserITCase.getSampleTO("syncope51@syncope.apache.org");
        userTO.setPassword("password");

        try {
            createUser(userTO);
            fail("Create user should not succeed");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getElements().iterator().next().contains("MD5"));
        }

        defaultCA.getValues().set(0, originalCAValue);
        configurationService.set(defaultCA);

        AttrTO oldCA = configurationService.get(defaultCA.getSchema());
        assertEquals(defaultCA, oldCA);
    }

    @Test
    public void issueSYNCOPE267() {
        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope267@apache.org");
        userTO.getVirAttrs().add(attrTO("virtualdata", "virtualvalue"));
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_DBVIRATTR);

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_DBVIRATTR, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_DBVIRATTR, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        assertEquals("virtualvalue", connObjectTO.getAttr("USERNAME").get().getValues().get(0));
        // ----------------------------------

        userTO = userService.read(userTO.getKey());

        assertNotNull(userTO);
        assertEquals(1, userTO.getVirAttrs().size());
        assertEquals("virtualvalue", userTO.getVirAttrs().iterator().next().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE266() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope266@apache.org");
        userTO.getResources().clear();

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());

        // this resource has not a mapping for Password
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_UPDATE).build());

        userTO = updateUser(userPatch).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE279() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope279@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_TIMEOUT);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertEquals(RESOURCE_NAME_TIMEOUT, result.getPropagationStatuses().get(0).getResource());
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        assertEquals(PropagationTaskExecStatus.FAILURE, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE122() {
        // 1. create user on testdb and testdb2
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope122@apache.org");
        userTO.getResources().clear();

        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        userTO.getResources().add(RESOURCE_NAME_TESTDB2);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB2));

        final String pwdOnSyncope = userTO.getPassword();

        ConnObjectTO userOnDb = resourceService.readConnObject(
                RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDbAttr = userOnDb.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDbAttr);
        assertNotNull(pwdOnTestDbAttr.getValues());
        assertFalse(pwdOnTestDbAttr.getValues().isEmpty());
        final String pwdOnTestDb = pwdOnTestDbAttr.getValues().iterator().next();

        ConnObjectTO userOnDb2 = resourceService.readConnObject(
                RESOURCE_NAME_TESTDB2, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDb2Attr = userOnDb2.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDb2Attr);
        assertNotNull(pwdOnTestDb2Attr.getValues());
        assertFalse(pwdOnTestDb2Attr.getValues().isEmpty());
        final String pwdOnTestDb2 = pwdOnTestDb2Attr.getValues().iterator().next();

        // 2. request to change password only on testdb (no Syncope, no testdb2)
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value(getUUIDString()).onSyncope(false).
                resource(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        userTO = result.getEntity();

        // 3a. Chech that only a single propagation took place
        assertNotNull(result.getPropagationStatuses());
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_TESTDB, result.getPropagationStatuses().iterator().next().getResource());

        // 3b. verify that password hasn't changed on Syncope
        assertEquals(pwdOnSyncope, userTO.getPassword());

        // 3c. verify that password *has* changed on testdb
        userOnDb = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDbAttrAfter = userOnDb.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDbAttrAfter);
        assertNotNull(pwdOnTestDbAttrAfter.getValues());
        assertFalse(pwdOnTestDbAttrAfter.getValues().isEmpty());
        assertNotEquals(pwdOnTestDb, pwdOnTestDbAttrAfter.getValues().iterator().next());

        // 3d. verify that password hasn't changed on testdb2
        userOnDb2 = resourceService.readConnObject(RESOURCE_NAME_TESTDB2, AnyTypeKind.USER.name(), userTO.getKey());
        final AttrTO pwdOnTestDb2AttrAfter = userOnDb2.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDb2AttrAfter);
        assertNotNull(pwdOnTestDb2AttrAfter.getValues());
        assertFalse(pwdOnTestDb2AttrAfter.getValues().isEmpty());
        assertEquals(pwdOnTestDb2, pwdOnTestDb2AttrAfter.getValues().iterator().next());
    }

    @Test
    public void issueSYNCOPE136AES() {
        // 1. read configured cipher algorithm in order to be able to restore it at the end of test
        AttrTO pwdCipherAlgo = configurationService.get("password.cipher.algorithm");
        final String origpwdCipherAlgo = pwdCipherAlgo.getValues().get(0);

        // 2. set AES password cipher algorithm
        pwdCipherAlgo.getValues().set(0, "AES");
        configurationService.set(pwdCipherAlgo);

        UserTO userTO = null;
        try {
            // 3. create user with no resources
            userTO = UserITCase.getUniqueSampleTO("syncope136_AES@apache.org");
            userTO.getResources().clear();

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);

            // 4. update user, assign a propagation priority resource but don't provide any password
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userTO.getKey());
            userPatch.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
            userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

            ProvisioningResult<UserTO> result = updateUser(userPatch);
            assertNotNull(result);
            userTO = result.getEntity();
            assertNotNull(userTO);

            // 5. verify that propagation was successful
            List<PropagationStatus> props = result.getPropagationStatuses();
            assertNotNull(props);
            assertEquals(1, props.size());
            PropagationStatus prop = props.iterator().next();
            assertNotNull(prop);
            assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
            assertEquals(PropagationTaskExecStatus.SUCCESS, prop.getStatus());
        } finally {
            // restore initial cipher algorithm
            pwdCipherAlgo.getValues().set(0, origpwdCipherAlgo);
            configurationService.set(pwdCipherAlgo);

            if (userTO != null) {
                deleteUser(userTO.getKey());
            }
        }
    }

    @Test
    public void isseSYNCOPE136Random() {
        // 1. create user with no resources
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope136_Random@apache.org");
        userTO.getResources().clear();
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 2. update user, assign a propagation priority resource but don't provide any password
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);

        // 3. verify that propagation was successful
        List<PropagationStatus> props = result.getPropagationStatuses();
        assertNotNull(props);
        assertEquals(1, props.size());
        PropagationStatus prop = props.iterator().next();
        assertNotNull(prop);
        assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, prop.getStatus());
    }

    @Test
    public void issueSYNCOPE265() {
        String[] userKeys = new String[] {
            "1417acbe-cbf6-4277-9372-e75e04f97000",
            "74cd8ece-715a-44a4-a736-e17b46c4e7e6",
            "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee",
            "c9b2dec2-00a7-4855-97c0-d854842b4b24",
            "823074dc-d280-436d-a7dd-07399fae48ec" };

        for (String userKey : userKeys) {
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userKey);
            userPatch.getPlainAttrs().add(attrAddReplacePatch("ctype", "a type"));
            UserTO userTO = updateUser(userPatch).getEntity();
            assertEquals("a type", userTO.getPlainAttr("ctype").get().getValues().get(0));
        }
    }

    @Test
    public void issueSYNCOPE354() {
        // change resource-ldap group mapping for including uniqueMember (need for assertions below)
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        ldap.getProvision(AnyTypeKind.GROUP.name()).get().getMapping().getItems().stream().
                filter(item -> ("description".equals(item.getExtAttrName()))).
                forEachOrdered(item -> {
                    item.setExtAttrName("uniqueMember");
                });
        resourceService.update(ldap);

        // 1. create group with LDAP resource
        GroupTO groupTO = new GroupTO();
        groupTO.setName("SYNCOPE354-" + getUUIDString());
        groupTO.setRealm("/");
        groupTO.getResources().add(RESOURCE_NAME_LDAP);

        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);

        // 2. create user with LDAP resource and membership of the above group
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope354@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO.getMemberships().add(new MembershipTO.Builder().group(groupTO.getKey()).build());

        userTO = createUser(userTO).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));

        // 3. read group on resource, check that user DN is included in uniqueMember
        ConnObjectTO connObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObj);
        assertTrue(connObj.getAttr("uniqueMember").get().getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 4. remove membership
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getMemberships().add(new MembershipPatch.Builder().operation(PatchOperation.DELETE).
                group(userTO.getMemberships().get(0).getGroupKey()).build());

        userTO = updateUser(userPatch).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));

        // 5. read group on resource, check that user DN was removed from uniqueMember
        connObj = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObj);
        assertFalse(connObj.getAttr("uniqueMember").get().getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 6. restore original resource-ldap group mapping
        ldap.getProvision(AnyTypeKind.GROUP.name()).get().getMapping().getItems().stream().
                filter(item -> ("uniqueMember".equals(item.getExtAttrName()))).
                forEachOrdered(item -> {
                    item.setExtAttrName("description");
                });
        resourceService.update(ldap);
    }

    @Test
    public void issueSYNCOPE357() throws IOException {
        // 1. create group with LDAP resource
        GroupTO groupTO = new GroupTO();
        groupTO.setName("SYNCOPE357-" + getUUIDString());
        groupTO.setRealm("/");
        groupTO.getResources().add(RESOURCE_NAME_LDAP);

        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);

        // 2. create user with membership of the above group
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope357@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO("obscure", "valueToBeObscured"));
        userTO.getPlainAttrs().add(attrTO("photo", Base64.getEncoder().encodeToString(
                IOUtils.readBytesFromStream(getClass().getResourceAsStream("/favicon.jpg")))));
        userTO.getMemberships().add(new MembershipTO.Builder().group(groupTO.getKey()).build());

        userTO = createUser(userTO).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));
        assertNotNull(userTO.getPlainAttr("obscure"));
        assertNotNull(userTO.getPlainAttr("photo"));

        // 3. read user on resource
        ConnObjectTO connObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObj);
        AttrTO registeredAddress = connObj.getAttr("registeredAddress").get();
        assertNotNull(registeredAddress);
        assertEquals(userTO.getPlainAttr("obscure").get().getValues(), registeredAddress.getValues());
        Optional<AttrTO> jpegPhoto = connObj.getAttr("jpegPhoto");
        assertTrue(jpegPhoto.isPresent());
        assertEquals(userTO.getPlainAttr("photo").get().getValues().get(0), jpegPhoto.get().getValues().get(0));

        // 4. remove group
        groupService.delete(groupTO.getKey());

        // 5. try to read user on resource: fail
        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE383() {
        // 1. create user without resources
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope383@apache.org");
        userTO.getResources().clear();
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 2. assign resource without specifying a new pwd and check propagation failure
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertNotEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        userTO = result.getEntity();

        // 3. request to change password only on testdb
        userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(
                new PasswordPatch.Builder().value(getUUIDString() + "abbcbcbddd123").resource(RESOURCE_NAME_TESTDB).
                        build());

        result = updateUser(userPatch);
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE402() {
        // 1. create an user with strict mandatory attributes only
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "syncope402@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password123");

        userTO.getPlainAttrs().add(attrTO("userId", userId));
        userTO.getPlainAttrs().add(attrTO("fullname", userId));
        userTO.getPlainAttrs().add(attrTO("surname", userId));

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource NOT forcing mandatory constraints
        // AND priority: must fail with PropagationException
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());
        ProvisioningResult<UserTO> result = updateUser(userPatch);

        List<PropagationStatus> propagationStatuses = result.getPropagationStatuses();
        PropagationStatus ws1PropagationStatus = null;
        if (propagationStatuses != null) {
            for (PropagationStatus propStatus : propagationStatuses) {
                if (RESOURCE_NAME_WS1.equals(propStatus.getResource())) {
                    ws1PropagationStatus = propStatus;
                    break;
                }
            }
        }
        assertNotNull(ws1PropagationStatus);
        assertEquals(RESOURCE_NAME_WS1, ws1PropagationStatus.getResource());
        assertNotNull(ws1PropagationStatus.getFailureReason());
        assertEquals(PropagationTaskExecStatus.FAILURE, ws1PropagationStatus.getStatus());
    }

    @Test
    public void issueSYNCOPE420() {
        RealmTO realm = realmService.list("/even/two").iterator().next();
        assertNotNull(realm);
        realm.getActionsClassNames().add(DoubleValueLogicActions.class.getName());
        realmService.update(realm);

        UserTO userTO = UserITCase.getUniqueSampleTO("syncope420@syncope.apache.org");
        userTO.setRealm(realm.getFullPath());
        userTO.getPlainAttrs().add(attrTO("makeItDouble", "3"));

        userTO = createUser(userTO).getEntity();
        assertEquals("6", userTO.getPlainAttr("makeItDouble").get().getValues().get(0));

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("makeItDouble", "7"));

        userTO = updateUser(userPatch).getEntity();
        assertEquals("14", userTO.getPlainAttr("makeItDouble").get().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE426() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope426@syncope.apache.org");
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value("anotherPassword123").build());
        userTO = userService.update(userPatch).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE435() {
        // 1. create user without password
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope435@syncope.apache.org");
        userTO.setPassword(null);
        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);

        // 2. try to update user by subscribing a resource - works but propagation is not even attempted
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(Collections.singleton(RESOURCE_NAME_WS1), userTO.getResources());
        assertNotEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertTrue(result.getPropagationStatuses().get(0).getFailureReason().
                startsWith("Not attempted because there are mandatory attributes without value(s): [__PASSWORD__]"));
    }

    @Test
    public void issueSYNCOPE454() throws NamingException {
        // 1. create user with LDAP resource (with 'Generate password if missing' enabled)
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope454@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_LDAP);
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        // 2. read resource configuration for LDAP binding
        ConnObjectTO connObject =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());

        // 3. try (and succeed) to perform simple LDAP binding with provided password ('password123')
        assertNotNull(getLdapRemoteObject(
                connObject.getAttr(Name.NAME).get().getValues().get(0),
                "password123",
                connObject.getAttr(Name.NAME).get().getValues().get(0)));

        // 4. update user without any password change request
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("surname", "surname2"));

        userService.update(userPatch);

        // 5. try (and succeed again) to perform simple LDAP binding: password has not changed
        assertNotNull(getLdapRemoteObject(
                connObject.getAttr(Name.NAME).get().getValues().get(0),
                "password123",
                connObject.getAttr(Name.NAME).get().getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE493() {
        // 1.  create user and check that firstname is not propagated on resource with mapping for firstname set to NONE
        UserTO userTO = UserITCase.getUniqueSampleTO("493@test.org");
        userTO.getResources().add(RESOURCE_NAME_WS1);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(userTO);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        ConnObjectTO actual =
                resourceService.readConnObject(RESOURCE_NAME_WS1, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(actual);
        // check if mapping attribute with purpose NONE really hasn't been propagated
        assertFalse(actual.getAttr("NAME").isPresent());

        // 2.  update resource ws-target-resource-1
        ResourceTO ws1 = resourceService.read(RESOURCE_NAME_WS1);
        assertNotNull(ws1);

        MappingTO ws1NewUMapping = ws1.getProvision(AnyTypeKind.USER.name()).get().getMapping();
        // change purpose from NONE to BOTH
        for (ItemTO itemTO : ws1NewUMapping.getItems()) {
            if ("firstname".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.BOTH);
            }
        }

        ws1.getProvision(AnyTypeKind.USER.name()).get().setMapping(ws1NewUMapping);

        resourceService.update(ws1);
        ResourceTO newWs1 = resourceService.read(ws1.getKey());
        assertNotNull(newWs1);

        // check for existence
        Collection<ItemTO> mapItems = newWs1.getProvision(AnyTypeKind.USER.name()).get().getMapping().getItems();
        assertNotNull(mapItems);
        assertEquals(7, mapItems.size());

        // 3.  update user and check firstname propagation
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("firstname", "firstnameNew"));

        result = updateUser(userPatch);
        assertNotNull(userTO);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        userTO = result.getEntity();

        ConnObjectTO newUser =
                resourceService.readConnObject(RESOURCE_NAME_WS1, AnyTypeKind.USER.name(), userTO.getKey());

        assertNotNull(newUser.getAttr("NAME"));
        assertEquals("firstnameNew", newUser.getAttr("NAME").get().getValues().get(0));

        // 4.  restore resource ws-target-resource-1 mapping
        ws1NewUMapping = newWs1.getProvision(AnyTypeKind.USER.name()).get().getMapping();
        // restore purpose from BOTH to NONE
        for (ItemTO itemTO : ws1NewUMapping.getItems()) {
            if ("firstname".equals(itemTO.getIntAttrName())) {
                itemTO.setPurpose(MappingPurpose.NONE);
            }
        }

        newWs1.getProvision(AnyTypeKind.USER.name()).get().setMapping(ws1NewUMapping);

        resourceService.update(newWs1);
    }

    @Test
    public void issueSYNCOPE505DB() throws Exception {
        // 1. create user
        UserTO user = UserITCase.getUniqueSampleTO("syncope505-db@syncope.apache.org");
        user.setPassword("security123");
        user = createUser(user).getEntity();
        assertNotNull(user);
        assertTrue(user.getResources().isEmpty());

        // 2. Add DBPasswordPropagationActions
        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().add(DBPasswordPropagationActions.class.getName());
        resourceService.update(resourceTO);

        // 3. Add a db resource to the User
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_TESTDB).build());

        user = updateUser(userPatch).getEntity();
        assertNotNull(user);
        assertEquals(1, user.getResources().size());

        // 4. Check that the DB resource has the correct password
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = jdbcTemplate.queryForObject(
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("security123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 5. Remove DBPasswordPropagationActions
        resourceTO = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().remove(DBPasswordPropagationActions.class.getName());
        resourceService.update(resourceTO);
    }

    @Test
    public void issueSYNCOPE505LDAP() throws Exception {
        // 1. create user
        UserTO user = UserITCase.getUniqueSampleTO("syncope505-ldap@syncope.apache.org");
        user.setPassword("security123");
        user = createUser(user).getEntity();
        assertNotNull(user);
        assertTrue(user.getResources().isEmpty());

        // 2. Add LDAPPasswordPropagationActions
        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().add(LDAPPasswordPropagationActions.class.getName());
        resourceTO.setRandomPwdIfNotProvided(false);
        resourceService.update(resourceTO);

        // 3. Add a resource to the User
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());

        userPatch.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

        user = updateUser(userPatch).getEntity();
        assertNotNull(user);
        assertEquals(1, user.getResources().size());

        // 4. Check that the LDAP resource has the correct password
        ConnObjectTO connObject =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());

        assertNotNull(getLdapRemoteObject(
                connObject.getAttr(Name.NAME).get().getValues().get(0),
                "security123",
                connObject.getAttr(Name.NAME).get().getValues().get(0)));

        // 5. Remove LDAPPasswordPropagationActions
        resourceTO = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActionsClassNames().remove(LDAPPasswordPropagationActions.class.getName());
        resourceTO.setRandomPwdIfNotProvided(true);
        resourceService.update(resourceTO);
    }

    @Test
    public void issueSYNCOPE391() {
        // 1. create user on Syncope with null password
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope391@syncope.apache.org");
        userTO.setPassword(null);

        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);
        assertNull(userTO.getPassword());

        // 2. create existing user on csv and check that password on Syncope is null and that password on resource
        // doesn't change
        userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword(null);
        userTO.setUsername("syncope391@syncope.apache.org");
        userTO.getPlainAttrs().add(attrTO("fullname", "fullname"));
        userTO.getPlainAttrs().add(attrTO("firstname", "nome0"));
        userTO.getPlainAttrs().add(attrTO("surname", "cognome0"));
        userTO.getPlainAttrs().add(attrTO("userId", "syncope391@syncope.apache.org"));
        userTO.getPlainAttrs().add(attrTO("email", "syncope391@syncope.apache.org"));

        userTO.getAuxClasses().add("csv");
        userTO.getResources().add(RESOURCE_NAME_CSV);
        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has not changed
        assertEquals("password0", connObjectTO.getAttr(OperationalAttributes.PASSWORD_NAME).get().getValues().get(0));
        assertNull(userTO.getPassword());

        // 3. create user with not null password and propagate onto resource-csv, specify not to save password on
        // Syncope local storage
        userTO = UserITCase.getUniqueSampleTO("syncope391@syncope.apache.org");
        userTO.setPassword("passwordTESTNULL1");
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        userTO.getResources().add(RESOURCE_NAME_CSV);
        userTO = createUser(userTO, false).getEntity();
        assertNotNull(userTO);

        connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has been propagated and that saved userTO's password is null
        assertEquals(
                "passwordTESTNULL1",
                connObjectTO.getAttr(OperationalAttributes.PASSWORD_NAME).get().getValues().get(0));
        assertNull(userTO.getPassword());

        // 4. create user and propagate password on resource-csv and on Syncope local storage
        userTO = UserITCase.getUniqueSampleTO("syncope391@syncope.apache.org");
        userTO.setPassword("passwordTESTNULL1");
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        userTO.getResources().add(RESOURCE_NAME_CSV);
        // storePassword true by default
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has been correctly propagated on Syncope and resource-csv as usual
        assertEquals(
                "passwordTESTNULL1",
                connObjectTO.getAttr(OperationalAttributes.PASSWORD_NAME).get().getValues().get(0));
        Pair<Map<String, Set<String>>, UserTO> self =
                clientFactory.create(userTO.getUsername(), "passwordTESTNULL1").self();
        assertNotNull(self);

        // 4. add password policy to resource with passwordNotStore to false --> must store password
        ResourceTO csv = resourceService.read(RESOURCE_NAME_CSV);
        assertNotNull(csv);
        try {
            csv.setPasswordPolicy("55e5de0b-c79c-4e66-adda-251b6fb8579a");
            resourceService.update(csv);
            csv = resourceService.read(RESOURCE_NAME_CSV);
            assertEquals("55e5de0b-c79c-4e66-adda-251b6fb8579a", csv.getPasswordPolicy());

            userTO = UserITCase.getUniqueSampleTO("syncope391@syncope.apache.org");
            userTO.setPassword(null);
            userTO.getVirAttrs().clear();
            userTO.getAuxClasses().add("csv");

            userTO.getResources().add(RESOURCE_NAME_CSV);
            createUser(userTO, false);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidUser, e.getType());
            assertTrue(e.getMessage().contains("Password mandatory"));
        } finally {
            // resource csv with null password policy
            csv.setPasswordPolicy(null);
            resourceService.update(csv);
        }
    }

    @Test
    public void issueSYNCOPE647() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope647@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getVirAttrs().clear();
        userTO.getAuxClasses().add("csv");

        userTO.getAuxClasses().add("generic membership");
        userTO.getPlainAttrs().add(attrTO("postalAddress", "postalAddress"));

        userTO.getResources().add(RESOURCE_NAME_LDAP);

        UserTO actual = createUser(userTO).getEntity();
        assertNotNull(actual);
        assertNotNull(actual.getDerAttr("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("postalAddress", connObjectTO.getAttr("postalAddress").get().getValues().get(0));

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(actual.getKey());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("postalAddress", "newPostalAddress"));

        actual = updateUser(userPatch).getEntity();

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("newPostalAddress", connObjectTO.getAttr("postalAddress").get().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE626() {
        PasswordPolicyTO passwordPolicy = new PasswordPolicyTO();
        passwordPolicy.setDescription("Password Policy for SYNCOPE-626");

        DefaultPasswordRuleConf ruleConf = new DefaultPasswordRuleConf();
        ruleConf.setUsernameAllowed(false);
        passwordPolicy.getRuleConfs().add(ruleConf);

        passwordPolicy = createPolicy(passwordPolicy);
        assertNotNull(passwordPolicy);

        RealmTO realm = realmService.list("/even/two").get(0);
        String oldPasswordPolicy = realm.getPasswordPolicy();
        realm.setPasswordPolicy(passwordPolicy.getKey());
        realmService.update(realm);

        try {
            UserTO user = UserITCase.getUniqueSampleTO("syncope626@syncope.apache.org");
            user.setRealm(realm.getFullPath());
            user.setPassword(user.getUsername());
            try {
                createUser(user);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidPassword"));
            }

            user.setPassword("password123");
            user = createUser(user).getEntity();
            assertNotNull(user);
        } finally {
            realm.setPasswordPolicy(oldPasswordPolicy);
            realmService.update(realm);

            policyService.delete(passwordPolicy.getKey());
        }

    }

    @Test
    public void issueSYNCOPE686() {
        // 1. read configured cipher algorithm in order to be able to restore it at the end of test
        AttrTO pwdCipherAlgo = configurationService.get("password.cipher.algorithm");
        String origpwdCipherAlgo = pwdCipherAlgo.getValues().get(0);

        // 2. set AES password cipher algorithm
        pwdCipherAlgo.getValues().set(0, "AES");
        configurationService.set(pwdCipherAlgo);

        try {
            // 3. create group with LDAP resource assigned
            GroupTO group = GroupITCase.getBasicSampleTO("syncope686");
            group.getResources().add(RESOURCE_NAME_LDAP);
            group = createGroup(group).getEntity();
            assertNotNull(group);

            // 4. create user with no resources
            UserTO userTO = UserITCase.getUniqueSampleTO("syncope686@apache.org");
            userTO.getResources().clear();

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);

            // 5. update user with the new group, and don't provide any password
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(userTO.getKey());
            userPatch.getMemberships().add(new MembershipPatch.Builder().operation(PatchOperation.ADD_REPLACE).
                    group(group.getKey()).build());

            ProvisioningResult<UserTO> result = updateUser(userPatch);
            assertNotNull(result);

            // 5. verify that propagation was successful
            List<PropagationStatus> props = result.getPropagationStatuses();
            assertNotNull(props);
            assertEquals(1, props.size());
            PropagationStatus prop = props.iterator().next();
            assertNotNull(prop);
            assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
            assertEquals(PropagationTaskExecStatus.SUCCESS, prop.getStatus());
        } finally {
            // restore initial cipher algorithm
            pwdCipherAlgo.getValues().set(0, origpwdCipherAlgo);
            configurationService.set(pwdCipherAlgo);
        }
    }

    @Test
    public void issueSYNCOPE710() {
        // 1. create groups for indirect resource assignment
        GroupTO ldapGroup = GroupITCase.getBasicSampleTO("syncope710.ldap");
        ldapGroup.getResources().add(RESOURCE_NAME_LDAP);
        ldapGroup = createGroup(ldapGroup).getEntity();

        GroupTO dbGroup = GroupITCase.getBasicSampleTO("syncope710.db");
        dbGroup.getResources().add(RESOURCE_NAME_TESTDB);
        dbGroup = createGroup(dbGroup).getEntity();

        // 2. create user with memberships for the groups created above
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope710@syncope.apache.org");
        userTO.getResources().clear();
        userTO.getMemberships().clear();
        userTO.getMemberships().add(new MembershipTO.Builder().group(ldapGroup.getKey()).build());
        userTO.getMemberships().add(new MembershipTO.Builder().group(dbGroup.getKey()).build());

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertEquals(2, result.getPropagationStatuses().size());
        userTO = result.getEntity();

        // 3. request to propagate passwod only to db
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().
                onSyncope(false).resource(RESOURCE_NAME_TESTDB).value("newpassword123").build());

        result = updateUser(userPatch);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_TESTDB, result.getPropagationStatuses().get(0).getResource());
    }

    @Test
    public void issueSYNCOPE881() {
        // 1. create group and assign LDAP
        GroupTO group = GroupITCase.getSampleTO("syncope881G");
        group.getVirAttrs().add(attrTO("rvirtualdata", "rvirtualvalue"));

        group = createGroup(group).getEntity();
        assertNotNull(group);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), group.getKey()));

        // 2. create user and assign such group
        UserTO user = UserITCase.getUniqueSampleTO("syncope881U@apache.org");
        user.getMemberships().clear();
        user.getMemberships().add(new MembershipTO.Builder().group(group.getKey()).build());

        user = createUser(user).getEntity();
        assertNotNull(user);

        // 3. verify that user is in LDAP
        ConnObjectTO connObject =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
        assertNotNull(connObject);
        AttrTO userDn = connObject.getAttr(Name.NAME).get();
        assertNotNull(userDn);
        assertEquals(1, userDn.getValues().size());
        assertNotNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, userDn.getValues().get(0)));

        // 4. remove user
        userService.delete(user.getKey());

        // 5. verify that user is not in LDAP anynmore
        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, userDn.getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE1099() {
        // 1. create group with dynamic condition and resource
        GroupTO group = GroupITCase.getSampleTO("syncope1099G");
        group.getResources().clear();
        group.getResources().add(RESOURCE_NAME_TESTDB);
        group.setUDynMembershipCond("firstname==issueSYNCOPE1099");

        group = createGroup(group).getEntity();
        assertNotNull(group);

        // 2. create user matching the condition above
        UserTO user = UserITCase.getUniqueSampleTO("syncope1099U@apache.org");
        user.getPlainAttr("firstname").get().getValues().set(0, "issueSYNCOPE1099");

        ProvisioningResult<UserTO> created = createUser(user);
        assertNotNull(created);

        // 3. verify that dynamic membership is set and that resource is consequently assigned
        user = created.getEntity();
        final String groupKey = group.getKey();
        assertTrue(user.getDynMemberships().stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));
        assertTrue(user.getResources().contains(RESOURCE_NAME_TESTDB));

        // 4. verify that propagation happened towards the resource of the dynamic group
        assertFalse(created.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_TESTDB, created.getPropagationStatuses().get(0).getResource());
    }

    @Test
    public void issueSYNCOPE1166() {
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope1166@apache.org");
        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        // resource-ldap has password mapped, resource-db-virattr does not
        userPatch.setPassword(new PasswordPatch.Builder().
                onSyncope(true).
                resource(RESOURCE_NAME_LDAP).
                value("new2Password").build());

        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
        userPatch.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_DBVIRATTR).build());

        ProvisioningResult<UserTO> result = updateUser(userPatch);
        assertNotNull(result);
        assertEquals(2, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals(RESOURCE_NAME_DBVIRATTR, result.getPropagationStatuses().get(1).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(1).getStatus());
    }

    @Test
    public void issueSYNCOPE1206() {
        // 1. create group with dynamic user condition 'cool==true'
        GroupTO dynGroup = GroupITCase.getSampleTO("syncope1206");
        dynGroup.setUDynMembershipCond(
                SyncopeClient.getUserSearchConditionBuilder().is("cool").equalTo("true").query());
        dynGroup = createGroup(dynGroup).getEntity();
        assertNotNull(dynGroup);
        assertTrue(dynGroup.getResources().contains(RESOURCE_NAME_LDAP));

        // 2. create user (no value for cool, no dynamic membership, no propagation to LDAP)
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope1206@apache.org");
        userTO.getResources().clear();

        ProvisioningResult<UserTO> result = createUser(userTO);
        assertTrue(result.getPropagationStatuses().isEmpty());

        // 3. update user to match the dynamic condition: expect propagation to LDAP
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(result.getEntity().getKey());
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().attrTO(attrTO("cool", "true")).build());

        result = updateUser(userPatch);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());

        // 4. update again user to not match the dynamic condition any more: expect propagation to LDAP
        userPatch = new UserPatch();
        userPatch.setKey(result.getEntity().getKey());
        userPatch.getPlainAttrs().add(new AttrPatch.Builder().attrTO(attrTO("cool", "false")).build());

        result = updateUser(userPatch);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
    }
}
