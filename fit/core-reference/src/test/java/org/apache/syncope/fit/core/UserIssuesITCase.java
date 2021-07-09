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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.naming.NamingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.cxf.helpers.IOUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
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
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.java.propagation.DBPasswordPropagationActions;
import org.apache.syncope.core.provisioning.java.propagation.LDAPPasswordPropagationActions;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.ElasticsearchDetector;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class UserIssuesITCase extends AbstractITCase {

    @Test
    public void issue186() {
        // 1. create an user with strict mandatory attributes only
        UserCR userCR = new UserCR();
        userCR.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "issue186@syncope.apache.org";
        userCR.setUsername(userId);
        userCR.setPassword("password123");

        userCR.getPlainAttrs().add(attr("userId", userId));
        userCR.getPlainAttrs().add(attr("fullname", userId));
        userCR.getPlainAttrs().add(attr("surname", userId));

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource forcing mandatory constraints: must fail with RequiredValuesMissing
        UserUR userUR = new UserUR.Builder(userTO.getKey()).
                password(new PasswordPatch.Builder().value("newPassword123").build()).
                resource(new StringPatchItem.Builder().
                        operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS2).build()).
                build();

        try {
            userTO = updateUser(userUR).getEntity();
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
        }

        // 3. update assigning a resource NOT forcing mandatory constraints
        // AND priority: must fail with PropagationException
        userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());

        ProvisioningResult<UserTO> result = updateUser(userUR);
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        userTO = result.getEntity();

        // 4. update assigning a resource NOT forcing mandatory constraints
        // BUT not priority: must succeed
        userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("newPassword123456").build());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_CSV).build());

        updateUser(userUR);
    }

    @Test
    public void issue213() {
        UserCR userCR = UserITCase.getUniqueSample("issue213@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getResources().size());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String username = queryForObject(
                jdbcTemplate, MAX_WAIT_SECONDS, "SELECT id FROM test WHERE id=?", String.class, userTO.getUsername());
        assertEquals(userTO.getUsername(), username);

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getResources().add(
                new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(RESOURCE_NAME_TESTDB).build());

        userTO = updateUser(userUR).getEntity();
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
        UserCR inUserTO = UserITCase.getUniqueSample("issue234@syncope.apache.org");
        inUserTO.getResources().add(RESOURCE_NAME_LDAP);

        UserTO userTO = createUser(inUserTO).getEntity();
        assertNotNull(userTO);

        UserUR userUR = new UserUR();

        userUR.setKey(userTO.getKey());
        userUR.setUsername(new StringReplacePatchItem.Builder().value('1' + userTO.getUsername()).build());

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);
        assertEquals('1' + inUserTO.getUsername(), userTO.getUsername());
    }

    @Test
    public void issue280() {
        UserCR userCR = UserITCase.getUniqueSample("issue280@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().onSyncope(false).
                resource(RESOURCE_NAME_TESTDB).value("123password").build());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userUR);
        assertNotNull(result);

        List<PropagationStatus> propagations = result.getPropagationStatuses();
        assertNotNull(propagations);
        assertEquals(1, propagations.size());

        assertEquals(ExecStatus.SUCCESS, propagations.get(0).getStatus());

        String resource = propagations.get(0).getResource();
        assertEquals(RESOURCE_NAME_TESTDB, resource);
    }

    @Test
    public void issue281() {
        UserCR userCR = UserITCase.getUniqueSample("issue281@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getResources().add(RESOURCE_NAME_CSV);

        ProvisioningResult<UserTO> result = createUser(userCR);
        assertNotNull(result);

        List<PropagationStatus> propagations = result.getPropagationStatuses();
        assertNotNull(propagations);
        assertEquals(1, propagations.size());
        assertNotEquals(ExecStatus.SUCCESS, propagations.get(0).getStatus());

        String resource = propagations.get(0).getResource();
        assertEquals(RESOURCE_NAME_CSV, resource);
    }

    @Test
    public void issue288() {
        UserCR userTO = UserITCase.getSample("issue288@syncope.apache.org");
        userTO.getPlainAttrs().add(attr("aLong", "STRING"));

        try {
            createUser(userTO);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidValues, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE108() {
        UserCR userCR = UserITCase.getUniqueSample("syncope108@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");

        userCR.getMemberships().add(new MembershipTO.Builder("0626100b-a4ba-4e00-9971-86fad52a6216").build());
        userCR.getMemberships().add(new MembershipTO.Builder("ba9ed509-b1f5-48ab-a334-c8530a6422dc").build());

        userCR.getResources().add(RESOURCE_NAME_CSV);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertEquals(2, userTO.getMemberships().size());
        assertEquals(1, userTO.getResources().size());

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // -----------------------------------
        // Remove the first membership: de-provisioning shouldn't happen
        // -----------------------------------
        UserUR userUR = new UserUR.Builder(userTO.getKey()).
                membership(new MembershipUR.Builder(userTO.getMemberships().get(0).getGroupKey()).
                        operation(PatchOperation.DELETE).build()).
                build();

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the resource assigned directly: de-provisioning shouldn't happen
        // -----------------------------------
        userUR = new UserUR();
        userUR.setKey(userTO.getKey());

        userUR.getResources().add(new StringPatchItem.Builder().operation(PatchOperation.DELETE).
                value(userTO.getResources().iterator().next()).build());

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);
        assertEquals(1, userTO.getMemberships().size());
        assertFalse(userTO.getResources().isEmpty());

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);
        // -----------------------------------

        // -----------------------------------
        // Remove the first membership: de-provisioning should happen
        // -----------------------------------
        userUR = new UserUR.Builder(userTO.getKey()).
                membership(new MembershipUR.Builder(userTO.getMemberships().get(0).getGroupKey()).
                        operation(PatchOperation.DELETE).build()).
                build();

        userTO = updateUser(userUR).getEntity();
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
        UserCR userCR = UserITCase.getSample("syncope185@syncope.apache.org");
        userCR.getVirAttrs().clear();
        userCR.getResources().add(RESOURCE_NAME_LDAP);

        ProvisioningResult<UserTO> result = createUser(userCR);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        UserTO userTO = result.getEntity();

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
        String originalCA = confParamOps.get(SyncopeConstants.MASTER_DOMAIN,
                "password.cipher.algorithm", null, String.class);
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", "MD5");

        UserCR userCR = UserITCase.getSample("syncope51@syncope.apache.org");
        userCR.setPassword("password");

        try {
            createUser(userCR);
            fail("Create user should not succeed");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
            assertTrue(e.getElements().iterator().next().contains("MD5"));
        } finally {
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", originalCA);
        }
    }

    @Test
    public void issueSYNCOPE267() {
        // ----------------------------------
        // create user and check virtual attribute value propagation
        // ----------------------------------
        UserCR userCR = UserITCase.getUniqueSample("syncope267@apache.org");
        userCR.getVirAttrs().add(attr("virtualdata", "virtualvalue"));
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_DBVIRATTR);

        ProvisioningResult<UserTO> result = createUser(userCR);
        assertNotNull(result);
        assertFalse(result.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_DBVIRATTR, result.getPropagationStatuses().get(0).getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        UserTO userTO = result.getEntity();

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
        UserCR userCR = UserITCase.getUniqueSample("syncope266@apache.org");
        userCR.getResources().clear();

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());

        // this resource has not a mapping for Password
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_UPDATE).build());

        userTO = updateUser(userUR).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE279() {
        UserCR userCR = UserITCase.getUniqueSample("syncope279@apache.org");
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_TIMEOUT);
        ProvisioningResult<UserTO> result = createUser(userCR);
        assertEquals(RESOURCE_NAME_TIMEOUT, result.getPropagationStatuses().get(0).getResource());
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        assertEquals(ExecStatus.FAILURE, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE122() {
        // 1. create user on testdb and testdb2
        UserCR userCR = UserITCase.getUniqueSample("syncope122@apache.org");
        userCR.getResources().clear();

        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        userCR.getResources().add(RESOURCE_NAME_TESTDB2);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB2));

        String pwdOnSyncope = userTO.getPassword();

        ConnObjectTO userOnDb = resourceService.readConnObject(
                RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
        Attr pwdOnTestDbAttr = userOnDb.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDbAttr);
        assertNotNull(pwdOnTestDbAttr.getValues());
        assertFalse(pwdOnTestDbAttr.getValues().isEmpty());
        String pwdOnTestDb = pwdOnTestDbAttr.getValues().get(0);

        ConnObjectTO userOnDb2 = resourceService.readConnObject(
                RESOURCE_NAME_TESTDB2, AnyTypeKind.USER.name(), userTO.getKey());
        Attr pwdOnTestDb2Attr = userOnDb2.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDb2Attr);
        assertNotNull(pwdOnTestDb2Attr.getValues());
        assertFalse(pwdOnTestDb2Attr.getValues().isEmpty());
        String pwdOnTestDb2 = pwdOnTestDb2Attr.getValues().get(0);

        // 2. request to change password only on testdb (no Syncope, no testdb2)
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value(getUUIDString()).onSyncope(false).
                resource(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userUR);
        userTO = result.getEntity();

        // 3a. Chech that only a single propagation took place
        assertNotNull(result.getPropagationStatuses());
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_TESTDB, result.getPropagationStatuses().get(0).getResource());

        // 3b. verify that password hasn't changed on Syncope
        assertEquals(pwdOnSyncope, userTO.getPassword());

        // 3c. verify that password *has* changed on testdb
        userOnDb = resourceService.readConnObject(RESOURCE_NAME_TESTDB, AnyTypeKind.USER.name(), userTO.getKey());
        Attr pwdOnTestDbAttrAfter = userOnDb.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDbAttrAfter);
        assertNotNull(pwdOnTestDbAttrAfter.getValues());
        assertFalse(pwdOnTestDbAttrAfter.getValues().isEmpty());
        assertNotEquals(pwdOnTestDb, pwdOnTestDbAttrAfter.getValues().get(0));

        // 3d. verify that password hasn't changed on testdb2
        userOnDb2 = resourceService.readConnObject(RESOURCE_NAME_TESTDB2, AnyTypeKind.USER.name(), userTO.getKey());
        Attr pwdOnTestDb2AttrAfter = userOnDb2.getAttr(OperationalAttributes.PASSWORD_NAME).get();
        assertNotNull(pwdOnTestDb2AttrAfter);
        assertNotNull(pwdOnTestDb2AttrAfter.getValues());
        assertFalse(pwdOnTestDb2AttrAfter.getValues().isEmpty());
        assertEquals(pwdOnTestDb2, pwdOnTestDb2AttrAfter.getValues().get(0));
    }

    @Test
    public void issueSYNCOPE136AES() {
        // 1. read configured cipher algorithm in order to be able to restore it at the end of test
        String origpwdCipherAlgo = confParamOps.get(SyncopeConstants.MASTER_DOMAIN,
                "password.cipher.algorithm", null, String.class);

        // 2. set AES password cipher algorithm
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", "AES");

        UserTO userTO = null;
        try {
            // 3. create user with no resources
            UserCR userCR = UserITCase.getUniqueSample("syncope136_AES@apache.org");
            userCR.getResources().clear();

            userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);

            // 4. update user, assign a propagation priority resource but don't provide any password
            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            userUR.getResources().add(new StringPatchItem.Builder().
                    operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
            userUR.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

            ProvisioningResult<UserTO> result = updateUser(userUR);
            assertNotNull(result);
            userTO = result.getEntity();
            assertNotNull(userTO);

            // 5. verify that propagation was successful
            List<PropagationStatus> props = result.getPropagationStatuses();
            assertNotNull(props);
            assertEquals(1, props.size());
            PropagationStatus prop = props.get(0);
            assertNotNull(prop);
            assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
            assertEquals(ExecStatus.SUCCESS, prop.getStatus());
        } finally {
            // restore initial cipher algorithm
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", origpwdCipherAlgo);

            if (userTO != null) {
                deleteUser(userTO.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE136Random() {
        // 1. create user with no resources
        UserCR userCR = UserITCase.getUniqueSample("syncope136_Random@apache.org");
        userCR.getResources().clear();
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 2. update user, assign a propagation priority resource but don't provide any password
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
        userUR.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

        ProvisioningResult<UserTO> result = updateUser(userUR);
        assertNotNull(result);

        // 3. verify that propagation was successful
        List<PropagationStatus> props = result.getPropagationStatuses();
        assertNotNull(props);
        assertEquals(1, props.size());
        PropagationStatus prop = props.get(0);
        assertNotNull(prop);
        assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
        assertEquals(ExecStatus.SUCCESS, prop.getStatus());
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
            UserUR userUR = new UserUR();
            userUR.setKey(userKey);
            userUR.getPlainAttrs().add(attrAddReplacePatch("ctype", "a type"));
            UserTO userTO = updateUser(userUR).getEntity();
            assertEquals("a type", userTO.getPlainAttr("ctype").get().getValues().get(0));
        }
    }

    @Test
    public void issueSYNCOPE354() {
        // change resource-ldap group mapping for including uniqueMember (need for assertions below)
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        ldap.getProvision(AnyTypeKind.GROUP.name()).get().getMapping().getItems().stream().
                filter(item -> ("description".equals(item.getExtAttrName()))).
                forEach(item -> item.setExtAttrName("uniqueMember"));
        resourceService.update(ldap);

        // 1. create group with LDAP resource
        GroupCR groupCR = new GroupCR();
        groupCR.setName("SYNCOPE354-" + getUUIDString());
        groupCR.setRealm("/");
        groupCR.getResources().add(RESOURCE_NAME_LDAP);

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);

        // 2. create user with LDAP resource and membership of the above group
        UserCR userCR = UserITCase.getUniqueSample("syncope354@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_LDAP);
        userCR.getMemberships().add(new MembershipTO.Builder(groupTO.getKey()).build());

        UserTO userTO = createUser(userCR).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey()));

        // 3. read group on resource, check that user DN is included in uniqueMember
        ConnObjectTO connObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObj);
        assertTrue(connObj.getAttr("uniqueMember").get().getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 4. remove membership
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getMemberships().add(new MembershipUR.Builder(userTO.getMemberships().get(0).getGroupKey()).
                operation(PatchOperation.DELETE).build());

        userTO = updateUser(userUR).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));

        // 5. read group on resource, check that user DN was removed from uniqueMember
        connObj = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), groupTO.getKey());
        assertNotNull(connObj);
        assertFalse(connObj.getAttr("uniqueMember").get().getValues().
                contains("uid=" + userTO.getUsername() + ",ou=people,o=isp"));

        // 6. user has still the LDAP resource assigned - SYNCOPE-1222
        userTO = userService.read(userTO.getKey());
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey()));

        // 7. restore original resource-ldap group mapping
        ldap.getProvision(AnyTypeKind.GROUP.name()).get().getMapping().getItems().stream().
                filter(item -> "uniqueMember".equals(item.getExtAttrName())).
                forEach(item -> item.setExtAttrName("description"));
        resourceService.update(ldap);
    }

    @Test
    public void issueSYNCOPE357() throws IOException {
        // 1. create group with LDAP resource
        GroupCR groupCR = new GroupCR();
        groupCR.setName("SYNCOPE357-" + getUUIDString());
        groupCR.setRealm("/");
        groupCR.getResources().add(RESOURCE_NAME_LDAP);

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);

        // 2. create user with membership of the above group
        UserCR userCR = UserITCase.getUniqueSample("syncope357@syncope.apache.org");
        userCR.getPlainAttrs().add(attr("obscure", "valueToBeObscured"));
        userCR.getPlainAttrs().add(attr("photo", Base64.getEncoder().encodeToString(
                IOUtils.readBytesFromStream(getClass().getResourceAsStream("/favicon.jpg")))));
        userCR.getMemberships().add(new MembershipTO.Builder(groupTO.getKey()).build());

        UserTO userTO = createUser(userCR).getEntity();
        assertTrue(userTO.getResources().contains(RESOURCE_NAME_LDAP));
        assertNotNull(userTO.getPlainAttr("obscure"));
        assertNotNull(userTO.getPlainAttr("photo"));

        // 3. read user on resource
        ConnObjectTO connObj = resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObj);
        Attr registeredAddress = connObj.getAttr("registeredAddress").get();
        assertNotNull(registeredAddress);
        assertEquals(userTO.getPlainAttr("obscure").get().getValues(), registeredAddress.getValues());
        Optional<Attr> jpegPhoto = connObj.getAttr("jpegPhoto");
        assertTrue(jpegPhoto.isPresent());
        assertEquals(userTO.getPlainAttr("photo").get().getValues().get(0), jpegPhoto.get().getValues().get(0));

        // 4. remove group
        groupService.delete(groupTO.getKey());

        // 5. try to read user on resource: fail
        try {
            resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), userTO.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE383() {
        // 1. create user without resources
        UserCR userCR = UserITCase.getUniqueSample("syncope383@apache.org");
        userCR.getResources().clear();
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 2. assign resource without specifying a new pwd and check propagation failure
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        ProvisioningResult<UserTO> result = updateUser(userUR);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertNotEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertNotNull(result.getPropagationStatuses().get(0).getFailureReason());
        userTO = result.getEntity();

        // 3. request to change password only on testdb
        userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(
                new PasswordPatch.Builder().value(getUUIDString() + "abbcbcbddd123").resource(RESOURCE_NAME_TESTDB).
                        build());

        result = updateUser(userUR);
        assertEquals(RESOURCE_NAME_TESTDB, userTO.getResources().iterator().next());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE402() {
        // 1. create an user with strict mandatory attributes only
        UserCR userCR = new UserCR();
        userCR.setRealm(SyncopeConstants.ROOT_REALM);
        String userId = getUUIDString() + "syncope402@syncope.apache.org";
        userCR.setUsername(userId);
        userCR.setPassword("password123");

        userCR.getPlainAttrs().add(attr("userId", userId));
        userCR.getPlainAttrs().add(attr("fullname", userId));
        userCR.getPlainAttrs().add(attr("surname", userId));

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertTrue(userTO.getResources().isEmpty());

        // 2. update assigning a resource NOT forcing mandatory constraints
        // AND priority: must fail with PropagationException
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("newPassword123").build());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());
        ProvisioningResult<UserTO> result = updateUser(userUR);

        PropagationStatus ws1PropagationStatus = result.getPropagationStatuses().stream().
                filter(propStatus -> RESOURCE_NAME_WS1.equals(propStatus.getResource())).
                findFirst().orElse(null);
        assertNotNull(ws1PropagationStatus);
        assertEquals(RESOURCE_NAME_WS1, ws1PropagationStatus.getResource());
        assertNotNull(ws1PropagationStatus.getFailureReason());
        assertEquals(ExecStatus.FAILURE, ws1PropagationStatus.getStatus());
    }

    @Test
    public void issueSYNCOPE420() throws IOException {
        ImplementationTO logicActions;
        try {
            logicActions = implementationService.read(
                    IdRepoImplementationType.LOGIC_ACTIONS, "DoubleValueLogicActions");
        } catch (SyncopeClientException e) {
            logicActions = new ImplementationTO();
            logicActions.setKey("DoubleValueLogicActions");
            logicActions.setEngine(ImplementationEngine.GROOVY);
            logicActions.setType(IdRepoImplementationType.LOGIC_ACTIONS);
            logicActions.setBody(org.apache.commons.io.IOUtils.toString(
                    getClass().getResourceAsStream("/DoubleValueLogicActions.groovy"), StandardCharsets.UTF_8));
            Response response = implementationService.create(logicActions);
            logicActions = implementationService.read(
                    logicActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        }
        assertNotNull(logicActions);

        RealmTO realm = realmService.search(new RealmQuery.Builder().keyword("two").build()).getResult().get(0);
        assertNotNull(realm);
        realm.getActions().add(logicActions.getKey());
        realmService.update(realm);

        UserCR userCR = UserITCase.getUniqueSample("syncope420@syncope.apache.org");
        userCR.setRealm(realm.getFullPath());
        userCR.getPlainAttrs().add(attr("makeItDouble", "3"));

        UserTO userTO = createUser(userCR).getEntity();
        assertEquals("6", userTO.getPlainAttr("makeItDouble").get().getValues().get(0));

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getPlainAttrs().add(attrAddReplacePatch("makeItDouble", "7"));

        userTO = updateUser(userUR).getEntity();
        assertEquals("14", userTO.getPlainAttr("makeItDouble").get().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE426() {
        UserCR userCR = UserITCase.getUniqueSample("syncope426@syncope.apache.org");
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().value("anotherPassword123").build());
        userTO = userService.update(userUR).readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        }).getEntity();
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE435() {
        // 1. create user without password
        UserCR userCR = UserITCase.getUniqueSample("syncope435@syncope.apache.org");
        userCR.setPassword(null);
        userCR.setStorePassword(false);
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 2. try to update user by subscribing a resource - works but propagation is not even attempted
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_WS1).build());

        ProvisioningResult<UserTO> result = updateUser(userUR);
        assertNotNull(result);
        userTO = result.getEntity();
        assertEquals(Set.of(RESOURCE_NAME_WS1), userTO.getResources());
        assertNotEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertTrue(result.getPropagationStatuses().get(0).getFailureReason().
                startsWith("Not attempted because there are mandatory attributes without value(s): [__PASSWORD__]"));
    }

    @Test
    public void issueSYNCOPE454() throws NamingException {
        // 1. create user with LDAP resource (with 'Generate password if missing' enabled)
        UserCR userCR = UserITCase.getUniqueSample("syncope454@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_LDAP);
        UserTO userTO = createUser(userCR).getEntity();
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
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch());
        userUR.getPlainAttrs().add(attrAddReplacePatch("surname", "surname2"));

        userService.update(userUR);

        // 5. try (and succeed again) to perform simple LDAP binding: password has not changed
        assertNotNull(getLdapRemoteObject(
                connObject.getAttr(Name.NAME).get().getValues().get(0),
                "password123",
                connObject.getAttr(Name.NAME).get().getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE493() {
        // 1.  create user and check that firstname is not propagated on resource with mapping for firstname set to NONE
        UserCR userCR = UserITCase.getUniqueSample("493@test.org");
        userCR.getResources().add(RESOURCE_NAME_WS1);
        ProvisioningResult<UserTO> result = createUser(userCR);
        assertNotNull(result);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        UserTO userTO = result.getEntity();

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
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch());
        userUR.getPlainAttrs().add(attrAddReplacePatch("firstname", "firstnameNew"));

        result = updateUser(userUR);
        assertNotNull(userTO);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
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
        UserCR userCR = UserITCase.getUniqueSample("syncope505-db@syncope.apache.org");
        userCR.setPassword("security123");
        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user);
        assertTrue(user.getResources().isEmpty());

        // 2. Add DBPasswordPropagationActions
        ImplementationTO propagationActions = new ImplementationTO();
        propagationActions.setKey(DBPasswordPropagationActions.class.getSimpleName());
        propagationActions.setEngine(ImplementationEngine.JAVA);
        propagationActions.setType(IdMImplementationType.PROPAGATION_ACTIONS);
        propagationActions.setBody(DBPasswordPropagationActions.class.getName());
        Response response = implementationService.create(propagationActions);
        propagationActions = implementationService.read(
                propagationActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        assertNotNull(propagationActions);

        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActions().add(propagationActions.getKey());
        resourceService.update(resourceTO);

        // 3. Add a db resource to the User
        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_TESTDB).build());

        userUR.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_TESTDB).build());

        user = updateUser(userUR).getEntity();
        assertNotNull(user);
        assertEquals(1, user.getResources().size());

        // 4. Check that the DB resource has the correct password
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = jdbcTemplate.queryForObject(
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("security123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 5. Remove DBPasswordPropagationActions
        resourceTO = resourceService.read(RESOURCE_NAME_TESTDB);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActions().remove(propagationActions.getKey());
        resourceService.update(resourceTO);
    }

    @Test
    public void issueSYNCOPE505LDAP() throws Exception {
        // 1. create user
        UserCR userCR = UserITCase.getUniqueSample("syncope505-ldap@syncope.apache.org");
        userCR.setPassword("security123");
        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user);
        assertTrue(user.getResources().isEmpty());

        // 2. Add LDAPPasswordPropagationActions
        ImplementationTO propagationActions = new ImplementationTO();
        propagationActions.setKey(LDAPPasswordPropagationActions.class.getSimpleName());
        propagationActions.setEngine(ImplementationEngine.JAVA);
        propagationActions.setType(IdMImplementationType.PROPAGATION_ACTIONS);
        propagationActions.setBody(LDAPPasswordPropagationActions.class.getName());
        Response response = implementationService.create(propagationActions);
        propagationActions = implementationService.read(
                propagationActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        assertNotNull(propagationActions);

        ResourceTO resourceTO = resourceService.read(RESOURCE_NAME_LDAP);
        assertNotNull(resourceTO);
        resourceTO.getPropagationActions().add(propagationActions.getKey());
        resourceTO.setRandomPwdIfNotProvided(false);
        resourceService.update(resourceTO);

        // 3. Add a resource to the User
        UserUR userUR = new UserUR();
        userUR.setKey(user.getKey());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());

        userUR.setPassword(new PasswordPatch.Builder().onSyncope(false).resource(RESOURCE_NAME_LDAP).build());

        user = updateUser(userUR).getEntity();
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
        resourceTO.getPropagationActions().remove(propagationActions.getKey());
        resourceTO.setRandomPwdIfNotProvided(true);
        resourceService.update(resourceTO);
    }

    @Test
    public void issueSYNCOPE391() {
        assumeFalse(ElasticsearchDetector.isElasticSearchEnabled(adminClient.platform()));

        // 1. create user on Syncope with null password
        UserCR userCR = UserITCase.getUniqueSample("syncope391@syncope.apache.org");
        userCR.setPassword(null);
        userCR.setStorePassword(false);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);
        assertNull(userTO.getPassword());

        // 2. create existing user on csv and check that password on Syncope is null and that password on resource
        // doesn't change
        userCR = new UserCR();
        userCR.setRealm(SyncopeConstants.ROOT_REALM);
        userCR.setPassword(null);
        userCR.setStorePassword(false);
        userCR.setUsername("syncope391@syncope.apache.org");
        userCR.getPlainAttrs().add(attr("fullname", "fullname"));
        userCR.getPlainAttrs().add(attr("firstname", "nome0"));
        userCR.getPlainAttrs().add(attr("surname", "cognome0"));
        userCR.getPlainAttrs().add(attr("userId", "syncope391@syncope.apache.org"));
        userCR.getPlainAttrs().add(attr("email", "syncope391@syncope.apache.org"));
        userCR.getAuxClasses().add("csv");
        userCR.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has not changed
        assertEquals("password0", connObjectTO.getAttr(OperationalAttributes.PASSWORD_NAME).get().getValues().get(0));
        assertNull(userTO.getPassword());

        // 3. create user with not null password and propagate onto resource-csv, specify not to save password on
        // Syncope local storage
        userCR = UserITCase.getUniqueSample("syncope391@syncope.apache.org");
        userCR.setPassword("passwordTESTNULL1");
        userCR.setStorePassword(false);
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");
        userCR.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userCR).getEntity();
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
        userCR = UserITCase.getUniqueSample("syncope391@syncope.apache.org");
        userCR.setPassword("passwordTESTNULL1");
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");
        userCR.getResources().add(RESOURCE_NAME_CSV);

        // storePassword true by default
        userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_CSV, AnyTypeKind.USER.name(), userTO.getKey());
        assertNotNull(connObjectTO);

        // check if password has been correctly propagated on Syncope and resource-csv as usual
        assertEquals(
                "passwordTESTNULL1",
                connObjectTO.getAttr(OperationalAttributes.PASSWORD_NAME).get().getValues().get(0));
        Triple<Map<String, Set<String>>, List<String>, UserTO> self =
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

            userCR = UserITCase.getUniqueSample("syncope391@syncope.apache.org");
            userCR.setPassword(null);
            userCR.setStorePassword(false);
            userCR.getVirAttrs().clear();
            userCR.getAuxClasses().add("csv");
            userCR.getResources().add(RESOURCE_NAME_CSV);

            createUser(userCR);
            fail("This should not happen");
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
        UserCR userCR = UserITCase.getUniqueSample("syncope647@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getVirAttrs().clear();
        userCR.getAuxClasses().add("csv");

        userCR.getAuxClasses().add("generic membership");
        userCR.getPlainAttrs().add(attr("postalAddress", "postalAddress"));

        userCR.getResources().add(RESOURCE_NAME_LDAP);

        UserTO actual = createUser(userCR).getEntity();
        assertNotNull(actual);
        assertNotNull(actual.getDerAttr("csvuserid"));

        ConnObjectTO connObjectTO =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("postalAddress", connObjectTO.getAttr("postalAddress").get().getValues().get(0));

        UserUR userUR = new UserUR();
        userUR.setKey(actual.getKey());
        userUR.getPlainAttrs().add(attrAddReplacePatch("postalAddress", "newPostalAddress"));

        actual = updateUser(userUR).getEntity();

        connObjectTO = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), actual.getKey());
        assertNotNull(connObjectTO);
        assertEquals("newPostalAddress", connObjectTO.getAttr("postalAddress").get().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE626() {
        DefaultPasswordRuleConf ruleConf = new DefaultPasswordRuleConf();
        ruleConf.setUsernameAllowed(false);

        ImplementationTO rule = new ImplementationTO();
        rule.setKey("DefaultPasswordRuleConf" + getUUIDString());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(IdRepoImplementationType.PASSWORD_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        Response response = implementationService.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        PasswordPolicyTO passwordPolicy = new PasswordPolicyTO();
        passwordPolicy.setName("Password Policy for SYNCOPE-626");
        passwordPolicy.getRules().add(rule.getKey());

        passwordPolicy = createPolicy(PolicyType.PASSWORD, passwordPolicy);
        assertNotNull(passwordPolicy);

        RealmTO realm = realmService.search(new RealmQuery.Builder().keyword("two").build()).getResult().get(0);
        String oldPasswordPolicy = realm.getPasswordPolicy();
        realm.setPasswordPolicy(passwordPolicy.getKey());
        realmService.update(realm);

        try {
            UserCR userCR = UserITCase.getUniqueSample("syncope626@syncope.apache.org");
            userCR.setRealm(realm.getFullPath());
            userCR.setPassword(userCR.getUsername());
            try {
                createUser(userCR);
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getElements().iterator().next().startsWith("InvalidPassword"));
            }

            userCR.setPassword("password123");
            UserTO user = createUser(userCR).getEntity();
            assertNotNull(user);
        } finally {
            realm.setPasswordPolicy(oldPasswordPolicy);
            realmService.update(realm);

            policyService.delete(PolicyType.PASSWORD, passwordPolicy.getKey());
        }

    }

    @Test
    public void issueSYNCOPE686() {
        // 1. read configured cipher algorithm in order to be able to restore it at the end of test
        String origpwdCipherAlgo = confParamOps.get(SyncopeConstants.MASTER_DOMAIN,
                "password.cipher.algorithm", null, String.class);

        // 2. set AES password cipher algorithm
        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", "AES");

        try {
            // 3. create group with LDAP resource assigned
            GroupCR groupCR = GroupITCase.getBasicSample("syncope686");
            groupCR.getResources().add(RESOURCE_NAME_LDAP);
            GroupTO group = createGroup(groupCR).getEntity();
            assertNotNull(group);

            // 4. create user with no resources
            UserCR userCR = UserITCase.getUniqueSample("syncope686@apache.org");
            userCR.getResources().clear();

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);

            // 5. update user with the new group, and don't provide any password
            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            userUR.getMemberships().add(new MembershipUR.Builder(group.getKey()).
                    operation(PatchOperation.ADD_REPLACE).build());

            ProvisioningResult<UserTO> result = updateUser(userUR);
            assertNotNull(result);

            // 5. verify that propagation was successful
            List<PropagationStatus> props = result.getPropagationStatuses();
            assertNotNull(props);
            assertEquals(1, props.size());
            PropagationStatus prop = props.get(0);
            assertNotNull(prop);
            assertEquals(RESOURCE_NAME_LDAP, prop.getResource());
            assertEquals(ExecStatus.SUCCESS, prop.getStatus());
        } finally {
            // restore initial cipher algorithm
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", origpwdCipherAlgo);
        }
    }

    @Test
    public void issueSYNCOPE710() {
        // 1. create groups for indirect resource assignment
        GroupCR ldapGroupCR = GroupITCase.getBasicSample("syncope710.ldap");
        ldapGroupCR.getResources().add(RESOURCE_NAME_LDAP);
        GroupTO ldapGroup = createGroup(ldapGroupCR).getEntity();

        GroupCR dbGroupCR = GroupITCase.getBasicSample("syncope710.db");
        dbGroupCR.getResources().add(RESOURCE_NAME_TESTDB);
        GroupTO dbGroup = createGroup(dbGroupCR).getEntity();

        // 2. create user with memberships for the groups created above
        UserCR userCR = UserITCase.getUniqueSample("syncope710@syncope.apache.org");
        userCR.getResources().clear();
        userCR.getMemberships().clear();
        userCR.getMemberships().add(new MembershipTO.Builder(ldapGroup.getKey()).build());
        userCR.getMemberships().add(new MembershipTO.Builder(dbGroup.getKey()).build());

        ProvisioningResult<UserTO> result = createUser(userCR);
        assertEquals(2, result.getPropagationStatuses().size());
        UserTO userTO = result.getEntity();

        // 3. request to propagate password only to db
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.setPassword(new PasswordPatch.Builder().
                onSyncope(false).resource(RESOURCE_NAME_TESTDB).value("newpassword123").build());

        result = updateUser(userUR);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_TESTDB, result.getPropagationStatuses().get(0).getResource());
    }

    @Test
    public void issueSYNCOPE881() {
        // 1. create group and assign LDAP
        GroupCR groupCR = GroupITCase.getSample("syncope881G");
        groupCR.getVirAttrs().add(attr("rvirtualdata", "rvirtualvalue"));

        GroupTO group = createGroup(groupCR).getEntity();
        assertNotNull(group);
        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), group.getKey()));

        // 2. create user and assign such group
        UserCR userCR = UserITCase.getUniqueSample("syncope881U@apache.org");
        userCR.getMemberships().clear();
        userCR.getMemberships().add(new MembershipTO.Builder(group.getKey()).build());

        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user);

        // 3. verify that user is in LDAP
        ConnObjectTO connObject =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
        assertNotNull(connObject);
        Attr userDn = connObject.getAttr(Name.NAME).get();
        assertNotNull(userDn);
        assertEquals(1, userDn.getValues().size());
        assertNotNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, userDn.getValues().get(0)));

        // 4. remove user
        userService.delete(user.getKey());

        // 5. verify that user is not in LDAP anymore
        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, userDn.getValues().get(0)));
    }

    @Test
    public void issueSYNCOPE1099() {
        // 1. create group with dynamic condition and resource
        GroupCR groupCR = GroupITCase.getSample("syncope1099G");
        groupCR.getResources().clear();
        groupCR.getResources().add(RESOURCE_NAME_TESTDB);
        groupCR.setUDynMembershipCond("firstname==issueSYNCOPE1099");

        GroupTO group = createGroup(groupCR).getEntity();
        assertNotNull(group);

        // 2. create user matching the condition above
        UserCR userCR = UserITCase.getUniqueSample("syncope1099U@apache.org");
        userCR.getPlainAttr("firstname").get().getValues().set(0, "issueSYNCOPE1099");

        ProvisioningResult<UserTO> created = createUser(userCR);
        assertNotNull(created);

        // 3. verify that dynamic membership is set and that resource is consequently assigned
        UserTO user = created.getEntity();
        String groupKey = group.getKey();
        assertTrue(user.getDynMemberships().stream().anyMatch(m -> m.getGroupKey().equals(groupKey)));
        assertTrue(user.getResources().contains(RESOURCE_NAME_TESTDB));

        // 4. verify that propagation happened towards the resource of the dynamic group
        assertFalse(created.getPropagationStatuses().isEmpty());
        assertEquals(RESOURCE_NAME_TESTDB, created.getPropagationStatuses().get(0).getResource());
    }

    @Test
    public void issueSYNCOPE1166() {
        UserCR userCR = UserITCase.getUniqueSample("syncope1166@apache.org");
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        // resource-ldap has password mapped, resource-db-virattr does not
        userUR.setPassword(new PasswordPatch.Builder().
                onSyncope(true).
                resource(RESOURCE_NAME_LDAP).
                value("new2Password").build());

        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_LDAP).build());
        userUR.getResources().add(new StringPatchItem.Builder().
                operation(PatchOperation.ADD_REPLACE).value(RESOURCE_NAME_DBVIRATTR).build());

        ProvisioningResult<UserTO> result = updateUser(userUR);
        assertNotNull(result);
        assertEquals(2, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());
        assertEquals(RESOURCE_NAME_DBVIRATTR, result.getPropagationStatuses().get(1).getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(1).getStatus());
    }

    @Test
    public void issueSYNCOPE1206() {
        // 1. create group with dynamic user condition 'cool==true'
        GroupCR dynGroupCR = GroupITCase.getSample("syncope1206");
        dynGroupCR.setUDynMembershipCond(
                SyncopeClient.getUserSearchConditionBuilder().is("cool").equalTo("true").query());
        GroupTO dynGroup = createGroup(dynGroupCR).getEntity();
        assertNotNull(dynGroup);
        assertTrue(dynGroup.getResources().contains(RESOURCE_NAME_LDAP));

        // 2. create user (no value for cool, no dynamic membership, no propagation to LDAP)
        UserCR userCR = UserITCase.getUniqueSample("syncope1206@apache.org");
        userCR.getResources().clear();

        ProvisioningResult<UserTO> result = createUser(userCR);
        assertTrue(result.getPropagationStatuses().isEmpty());

        // 3. update user to match the dynamic condition: expect propagation to LDAP
        UserUR userUR = new UserUR();
        userUR.setKey(result.getEntity().getKey());
        userUR.getPlainAttrs().add(new AttrPatch.Builder(attr("cool", "true")).build());

        result = updateUser(userUR);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());

        // 4. update again user to not match the dynamic condition any more: expect propagation to LDAP
        userUR = new UserUR();
        userUR.setKey(result.getEntity().getKey());
        userUR.getPlainAttrs().add(new AttrPatch.Builder(attr("cool", "false")).build());

        result = updateUser(userUR);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP, result.getPropagationStatuses().get(0).getResource());
    }

    @Test
    public void issueSYNCOPE1337() {
        // 1. save current cipher algorithm and set it to something salted
        String original = confParamOps.get(SyncopeConstants.MASTER_DOMAIN,
                "password.cipher.algorithm", null, String.class);

        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", CipherAlgorithm.SSHA512.name());

        try {
            // 2. create user under /even/two to get password policy with history length 1
            UserCR userCR = UserITCase.getUniqueSample("syncope1337@apache.org");
            userCR.setPassword("Password123");
            userCR.setRealm("/even/two");
            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);

            // 3. attempt to set the same password value: fails
            UserUR req = new UserUR();
            req.setKey(userTO.getKey());
            req.setPassword(new PasswordPatch.Builder().onSyncope(true).value("Password123").build());
            try {
                updateUser(req);
                fail("Password update should not work");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.InvalidUser, e.getType());
                assertTrue(e.getMessage().contains("InvalidPassword"));
            }

            // 4. set another password value: works
            req.setPassword(new PasswordPatch.Builder().onSyncope(true).value("Password124").build());
            userTO = updateUser(req).getEntity();
            assertNotNull(userTO);

            // 5. set the original password value: works (history length is 1)
            req.setPassword(new PasswordPatch.Builder().onSyncope(true).value("Password123").build());
            userTO = updateUser(req).getEntity();
            assertNotNull(userTO);
        } finally {
            // finally revert the cipher algorithm
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "password.cipher.algorithm", original);
        }
    }

    @Test
    public void issueSYNCOPE1472() {
        // 1. update user rossini by assigning twice resource-testdb2 and auxiliary class csv
        UserUR userUR = new UserUR();
        userUR.setKey("1417acbe-cbf6-4277-9372-e75e04f97000");
        userUR.setPassword(new PasswordPatch.Builder()
                .onSyncope(false)
                .resource(RESOURCE_NAME_TESTDB)
                .value("Password123")
                .build());
        userUR.getResources().add(new StringPatchItem.Builder()
                .value(RESOURCE_NAME_TESTDB)
                .operation(PatchOperation.ADD_REPLACE)
                .build());
        userUR.getAuxClasses().add(new StringPatchItem.Builder()
                .operation(PatchOperation.ADD_REPLACE)
                .value("csv")
                .build());
        userUR.getRoles().add(new StringPatchItem.Builder()
                .operation(PatchOperation.ADD_REPLACE)
                .value("Other")
                .build());

        for (int i = 0; i < 2; i++) {
            updateUser(userUR);
        }

        // 2. remove resources, auxiliary classes and roles
        userUR.getResources().clear();
        userUR.getResources().add(new StringPatchItem.Builder()
                .value(RESOURCE_NAME_TESTDB)
                .operation(PatchOperation.DELETE)
                .build());
        userUR.getAuxClasses().clear();
        userUR.getAuxClasses().add(new StringPatchItem.Builder()
                .value("csv")
                .operation(PatchOperation.DELETE)
                .build());
        userUR.getRoles().clear();
        userUR.getRoles().add(new StringPatchItem.Builder()
                .value("Other")
                .operation(PatchOperation.DELETE)
                .build());
        updateUser(userUR);

        UserTO userTO = userService.read("1417acbe-cbf6-4277-9372-e75e04f97000");
        assertFalse(userTO.getResources().contains(RESOURCE_NAME_TESTDB), "Should not contain removed resources");
        assertFalse(userTO.getAuxClasses().contains("csv"), "Should not contain removed auxiliary classes");
        assertFalse(userTO.getRoles().contains("Other"), "Should not contain removed roles");
    }
}
