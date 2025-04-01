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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class RealmITCase extends AbstractITCase {

    @Test
    public void search() {
        PagedResult<RealmTO> match = REALM_SERVICE.search(new RealmQuery.Builder().keyword("*o*").build());
        assertTrue(match.getResult().stream().allMatch(realm -> realm.getName().contains("o")));
    }

    @Test
    public void createUpdate() {
        RealmTO realm = new RealmTO();
        realm.setName("last");

        // 1. create
        Response response = REALM_SERVICE.create("/even/two", realm);
        RealmTO actual = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);
        assertNotNull(actual.getKey());
        assertEquals("last", actual.getName());
        assertEquals("/even/two/last", actual.getFullPath());
        assertEquals(actual.getParent(), getRealm("/even/two").get().getKey());
        assertNull(realm.getAccountPolicy());
        assertNull(realm.getPasswordPolicy());

        // 2. update setting policies
        actual.setAccountPolicy("06e2ed52-6966-44aa-a177-a0ca7434201f");
        actual.setPasswordPolicy("986d1236-3ac5-4a19-810c-5ab21d79cba1");
        actual.setAuthPolicy("b912a0d4-a890-416f-9ab8-84ab077eb028");
        actual.setAccessPolicy("419935c7-deb3-40b3-8a9a-683037e523a2");
        actual.setAttrReleasePolicy("319935c7-deb3-40b3-8a9a-683037e523a2");
        REALM_SERVICE.update(actual);

        actual = getRealm(actual.getFullPath()).get();
        assertNotNull(actual.getAccountPolicy());
        assertNotNull(actual.getPasswordPolicy());
        assertNotNull(actual.getAuthPolicy());
        assertNotNull(actual.getAccessPolicy());
        assertNotNull(actual.getAttrReleasePolicy());

        // 3. update changing parent
        actual.setParent(getRealm("/odd").get().getKey());
        REALM_SERVICE.update(actual);

        actual = getRealm("/odd/last").get();
        assertNotNull(actual);
        assertEquals("/odd/last", actual.getFullPath());

        assertEquals(1, REALM_SERVICE.search(new RealmQuery.Builder().base(SyncopeConstants.ROOT_REALM).build()).
                getResult().stream().filter(r -> realm.getName().equals(r.getName())).count());

        // 4. create under invalid path
        try {
            REALM_SERVICE.create("a name", realm);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPath, e.getType());
        }

        // 5. attempt to create duplicate
        try {
            REALM_SERVICE.create("/odd", realm);
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void createWithTilde() {
        RealmTO realm = new RealmTO();
        realm.setName("73~1~19534");

        Response response = REALM_SERVICE.create("/even/two", realm);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        List<RealmTO> realms = REALM_SERVICE.search(new RealmQuery.Builder().
                base("/even/two/73~1~19534").build()).getResult();
        assertEquals(1, realms.size());
        assertEquals(realm.getName(), realms.getFirst().getName());
    }

    @Test
    public void deletingAccountPolicy() {
        // 1. create account policy
        DefaultAccountRuleConf ruleConf = new DefaultAccountRuleConf();
        ruleConf.setMinLength(3);
        ruleConf.setMaxLength(8);

        ImplementationTO rule = new ImplementationTO();
        rule.setKey("DefaultAccountRuleConf" + UUID.randomUUID());
        rule.setEngine(ImplementationEngine.JAVA);
        rule.setType(IdRepoImplementationType.ACCOUNT_RULE);
        rule.setBody(POJOHelper.serialize(ruleConf));
        Response response = IMPLEMENTATION_SERVICE.create(rule);
        rule.setKey(response.getHeaderString(RESTHeaders.RESOURCE_KEY));

        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setName("deletingAccountPolicy");
        policy.getRules().add(rule.getKey());

        policy = createPolicy(PolicyType.ACCOUNT, policy);
        assertNotNull(policy);

        // 2. create realm with policy assigned
        RealmTO realm = new RealmTO();
        realm.setName("withPolicy");

        response = REALM_SERVICE.create(SyncopeConstants.ROOT_REALM, realm);
        realm = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);

        String existingAccountPolicy = realm.getAccountPolicy();

        realm.setAccountPolicy(policy.getKey());
        REALM_SERVICE.update(realm);

        RealmTO actual = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);
        assertEquals(policy.getKey(), actual.getAccountPolicy());

        // 3. remove policy
        POLICY_SERVICE.delete(PolicyType.ACCOUNT, policy.getKey());

        // 4. verify
        actual = getRealm(actual.getFullPath()).get();
        assertEquals(existingAccountPolicy, actual.getAccountPolicy());
    }

    @Test
    public void deletingAuthPolicy() {
        // 1. create authentication policy
        DefaultAuthPolicyConf ruleConf = new DefaultAuthPolicyConf();
        ruleConf.getAuthModules().add("LdapAuthentication1");

        AuthPolicyTO policy = new AuthPolicyTO();
        policy.setName("Test Authentication policy");
        policy.setConf(ruleConf);
        policy = createPolicy(PolicyType.AUTH, policy);
        assertNotNull(policy);

        // 2. create realm with policy assigned
        RealmTO realm = new RealmTO();
        realm.setName("withAuthPolicy");

        Response response = REALM_SERVICE.create(SyncopeConstants.ROOT_REALM, realm);
        realm = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);

        String existingAuthPolicy = realm.getAuthPolicy();

        realm.setAuthPolicy(policy.getKey());
        REALM_SERVICE.update(realm);

        RealmTO actual = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);
        assertEquals(policy.getKey(), actual.getAuthPolicy());

        // 3. remove policy
        POLICY_SERVICE.delete(PolicyType.AUTH, policy.getKey());

        // 4. verify
        actual = getRealm(actual.getFullPath()).get();
        assertEquals(existingAuthPolicy, actual.getAuthPolicy());
    }

    @Test
    public void deletingAccessPolicy() {
        // 1. create access policy
        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.getRequiredAttrs().put("cn", "admin,Admin,TheAdmin");

        AccessPolicyTO policy = new AccessPolicyTO();
        policy.setName("Test Access policy");
        policy.setConf(conf);
        policy = createPolicy(PolicyType.ACCESS, policy);
        assertNotNull(policy);

        // 2. create realm with policy assigned
        RealmTO realm = new RealmTO();
        realm.setName("withAccessPolicy");

        Response response = REALM_SERVICE.create(SyncopeConstants.ROOT_REALM, realm);
        realm = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);

        String existingAccessPolicy = realm.getAccessPolicy();

        realm.setAccessPolicy(policy.getKey());
        REALM_SERVICE.update(realm);

        RealmTO actual = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);
        assertEquals(policy.getKey(), actual.getAccessPolicy());

        // 3. remove policy
        POLICY_SERVICE.delete(PolicyType.ACCESS, policy.getKey());

        // 4. verify
        actual = getRealm(actual.getFullPath()).get();
        assertEquals(existingAccessPolicy, actual.getAccessPolicy());
    }

    @Test
    public void deletingAttributeReleasePolicy() {
        // 1. create attribute release policy
        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.getAllowedAttrs().addAll(List.of("cn", "givenName"));
        conf.getIncludeOnlyAttrs().add("cn");

        AttrReleasePolicyTO policy = new AttrReleasePolicyTO();
        policy.setName("Test Attribute Release policy");
        policy.setConf(conf);
        policy = createPolicy(PolicyType.ATTR_RELEASE, policy);
        assertNotNull(policy);

        // 2. create realm with policy assigned
        RealmTO realm = new RealmTO();
        realm.setName("withAttrReleasePolicy");

        Response response = REALM_SERVICE.create(SyncopeConstants.ROOT_REALM, realm);
        realm = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);

        String existingAttrReleasePolicy = realm.getAttrReleasePolicy();

        realm.setAttrReleasePolicy(policy.getKey());
        REALM_SERVICE.update(realm);

        RealmTO actual = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);
        assertEquals(policy.getKey(), actual.getAttrReleasePolicy());

        // 3. remove policy
        POLICY_SERVICE.delete(PolicyType.ATTR_RELEASE, policy.getKey());

        // 4. verify
        actual = getRealm(actual.getFullPath()).get();
        assertEquals(existingAttrReleasePolicy, actual.getAttrReleasePolicy());
    }

    @Test
    public void delete() {
        RealmTO realm = new RealmTO();
        realm.setName("deletable3");

        Response response = REALM_SERVICE.create("/even/two", realm);
        RealmTO actual = getRealm(response.getHeaderString(RESTHeaders.RESOURCE_KEY)).
                orElseThrow(NotFoundException::new);

        REALM_SERVICE.delete(actual.getFullPath());

        try {
            REALM_SERVICE.search(new RealmQuery.Builder().base(actual.getFullPath()).build());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void deleteNonEmpty() {
        try {
            REALM_SERVICE.delete("/even/two");
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RealmContains, e.getType());
        }
    }

    @Test
    public void propagate() {
        // 1. create realm and add the LDAP resource
        RealmTO realm = new RealmTO();
        realm.setName("test");
        realm.getResources().add(RESOURCE_NAME_LDAP_ORGUNIT);
        RealmTO childRealm = new RealmTO();
        childRealm.setName("child");
        childRealm.getResources().add(RESOURCE_NAME_LDAP_ORGUNIT);
        RealmTO descendantRealm = new RealmTO();
        descendantRealm.setName("test");
        descendantRealm.getResources().add(RESOURCE_NAME_LDAP_ORGUNIT);

        // 2. check propagation
        ProvisioningResult<RealmTO> result = REALM_SERVICE.create("/", realm).readEntity(new GenericType<>() {
        });
        assertNotNull(result);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP_ORGUNIT, result.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().getFirst().getStatus());

        ProvisioningResult<RealmTO> resultChild = REALM_SERVICE.create("/test", childRealm).readEntity(
                new GenericType<>() {
        });
        assertNotNull(resultChild);
        assertEquals(1, resultChild.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP_ORGUNIT, resultChild.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, resultChild.getPropagationStatuses().getFirst().getStatus());

        ProvisioningResult<RealmTO> resultDescendant = REALM_SERVICE.create("/test/child", descendantRealm).readEntity(
                new GenericType<>() {
        });
        assertNotNull(resultDescendant);
        assertEquals(1, resultDescendant.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP_ORGUNIT, resultDescendant.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, resultDescendant.getPropagationStatuses().getFirst().getStatus());

        // 3. check on LDAP
        assertNotNull(getLdapRemoteObject("ou=test,o=isp"));
        assertNotNull(getLdapRemoteObject("ou=child,ou=test,o=isp"));
        assertNotNull(getLdapRemoteObject("ou=test,ou=child,ou=test,o=isp"));

        // 4. remove realms
        REALM_SERVICE.delete("/test/child/test");
        REALM_SERVICE.delete("/test/child");
        REALM_SERVICE.delete("/test");

        // 5. check on LDAP: both realms should be deleted
        assertNull(getLdapRemoteObject("ou=test,ou=child,ou=test,o=isp"));
        assertNull(getLdapRemoteObject("ou=child,ou=test,o=isp"));
        assertNull(getLdapRemoteObject("ou=test,o=isp"));
    }

    @Test
    public void issueSYNCOPE1472() {
        // 1. assign twice resource-ldap-orgunit to /odd
        RealmTO realmTO = REALM_SERVICE.search(new RealmQuery.Builder().base("/odd").build()).getResult().getFirst();
        realmTO.getResources().clear();
        realmTO.getResources().add("resource-ldap-orgunit");
        realmTO.getResources().add("resource-ldap-orgunit");
        realmTO = REALM_SERVICE.update(realmTO).readEntity(new GenericType<ProvisioningResult<RealmTO>>() {
        }).getEntity();

        // 2. remove resource-ldap-orgunit resource
        realmTO.getResources().remove("resource-ldap-orgunit");

        realmTO = REALM_SERVICE.update(realmTO).readEntity(new GenericType<ProvisioningResult<RealmTO>>() {
        }).getEntity();

        assertFalse(realmTO.getResources().contains("resource-ldap-orgunit"), "Should not contain removed resources");
    }

    @Test
    public void issueSYNCOPE1856() {
        // CREATE ROLE
        RoleTO role = new RoleTO();
        role.getEntitlements().addAll(
                List.of(IdRepoEntitlement.REALM_SEARCH, IdRepoEntitlement.REALM_CREATE,
                        IdRepoEntitlement.REALM_UPDATE, IdRepoEntitlement.REALM_DELETE));
        role.getRealms().add("/even");
        role.setKey("REALM_ADMIN");
        role = createRole(role);

        // CREATE REALM MANAGER
        UserCR userCR = UserITCase.getUniqueSample("manager@syncope.apache.org");
        userCR.setRealm("/even");
        userCR.getRoles().add(role.getKey());
        UserTO manager = createUser(userCR).getEntity();
        RealmService managerRealmService = CLIENT_FACTORY.create(
                manager.getUsername(), "password123").getService(RealmService.class);

        RealmTO childRealm = null;
        try {
            // MANAGER CANNOT CREATE REALM CHILD OF /
            RealmTO realmTO = new RealmTO();
            realmTO.setName("child");
            try {
                managerRealmService.create(SyncopeConstants.ROOT_REALM, realmTO);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }

            Response response = REALM_SERVICE.create(SyncopeConstants.ROOT_REALM, realmTO);
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
            childRealm = REALM_SERVICE.search(new RealmQuery.Builder().
                    base(SyncopeConstants.ROOT_REALM).keyword("child").build()).getResult().getFirst();

            // MANAGER CANNOT UPDATE /child
            try {
                managerRealmService.update(childRealm);
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }

            // MANAGER CANNOT DELETE /child
            try {
                managerRealmService.delete(childRealm.getFullPath());
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
            }
        } finally {
            Optional.ofNullable(childRealm).ifPresent(r -> REALM_SERVICE.delete(r.getFullPath()));
            USER_SERVICE.delete(manager.getKey());
            ROLE_SERVICE.delete(role.getKey());
        }
    }

    @Test
    public void issueSYNCOPE1871() {
        PagedResult<RealmTO> result = REALM_SERVICE.search(new RealmQuery.Builder().base("/odd").base("/even").build());
        assertDoesNotThrow(() -> result.getResult().stream().
                filter(r -> "odd".equals(r.getName())).findFirst().orElseThrow());
        assertDoesNotThrow(() -> result.getResult().stream().
                filter(r -> "even".equals(r.getName())).findFirst().orElseThrow());
        assertDoesNotThrow(() -> result.getResult().stream().
                filter(r -> "two".equals(r.getName())).findFirst().orElseThrow());
    }
}
