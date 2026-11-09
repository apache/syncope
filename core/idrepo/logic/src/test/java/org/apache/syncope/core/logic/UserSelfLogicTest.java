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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.cache.Cache;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.StandardConfParams;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.common.rest.api.beans.ComplianceQuery;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.spring.policy.PasswordPolicyException;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.throttle.ThrottlerAttempts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSelfLogicTest {

    private RealmSearchDAO realmSearchDAO;

    private AnyTypeDAO anyTypeDAO;

    private UserDAO userDAO;

    private UserDataBinder binder;

    private UserProvisioningManager provisioningManager;

    private ConfParamOps confParamOps;

    private DelegationDAO delegationDAO;

    private AccessTokenDAO accessTokenDAO;

    private ExternalResourceDAO resourceDAO;

    private RuleProvider ruleProvider;

    private SecurityProperties securityProperties;

    private UserSelfLogic logic;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        realmSearchDAO = mock(RealmSearchDAO.class);
        anyTypeDAO = mock(AnyTypeDAO.class);
        userDAO = mock(UserDAO.class);
        binder = mock(UserDataBinder.class);
        provisioningManager = mock(UserProvisioningManager.class);
        confParamOps = mock(ConfParamOps.class);
        delegationDAO = mock(DelegationDAO.class);
        accessTokenDAO = mock(AccessTokenDAO.class);
        resourceDAO = mock(ExternalResourceDAO.class);
        ruleProvider = mock(RuleProvider.class);
        securityProperties = new SecurityProperties();
        securityProperties.setAnonymousUser("anonymous");
        securityProperties.setProductionMode(false);

        Cache<String, ThrottlerAttempts> cache = mock(Cache.class);

        logic = new UserSelfLogic(
                realmSearchDAO,
                anyTypeDAO,
                mock(TemplateUtils.class),
                userDAO,
                binder,
                provisioningManager,
                mock(EncryptorManager.class),
                confParamOps,
                delegationDAO,
                accessTokenDAO,
                resourceDAO,
                ruleProvider,
                securityProperties,
                cache);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void createForbidden() {
        when(confParamOps.get(any(), eq(StandardConfParams.SELF_REGISTRATION_ALLOWED), eq(false), eq(boolean.class))).
                thenReturn(false);

        UserCR userCR = new UserCR.Builder(SyncopeConstants.ROOT_REALM, "user").build();

        assertThrows(SyncopeClientException.class, () -> logic.create(userCR, false));
    }

    @Test
    void updateDeletesTokenWhenStatusNotAuthenticated() {
        LogicTestAuth.authenticate("user", "Master",
                Map.of(IdRepoEntitlement.USER_UPDATE, Set.of(SyncopeConstants.ROOT_REALM)));

        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setUsername("user");
        userTO.setStatus("ACTIVE");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        UserUR userUR = new UserUR.Builder("key").build();
        UserTO output = new UserTO();
        output.setKey("key");
        output.setUsername("user");
        output.setStatus("SUSPENDED");

        when(binder.getAuthenticatedUserTO()).thenReturn(userTO);
        doReturn(userTO).doReturn(output).when(binder).getUserTO("key");
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(provisioningManager.update(eq(userUR), anyBoolean(), eq("user"), eq("REST"))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(userUR, List.of()));
        when(confParamOps.get(any(), eq(StandardConfParams.AUTHENTICATION_STATUSES), any(), eq(String[].class))).
                thenReturn(new String[] { "ACTIVE" });

        logic.update(userUR, false);

        verify(accessTokenDAO).deleteByOwner("user");
    }

    @Test
    void updateKeepsTokenWhenStatusAuthenticated() {
        LogicTestAuth.authenticate("user", "Master",
                Map.of(IdRepoEntitlement.USER_UPDATE, Set.of(SyncopeConstants.ROOT_REALM)));

        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setUsername("user");
        userTO.setStatus("ACTIVE");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        UserUR userUR = new UserUR.Builder("key").build();
        UserTO output = new UserTO();
        output.setKey("key");
        output.setUsername("user");
        output.setStatus("ACTIVE");

        when(binder.getAuthenticatedUserTO()).thenReturn(userTO);
        doReturn(userTO).doReturn(output).when(binder).getUserTO("key");
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(provisioningManager.update(eq(userUR), anyBoolean(), eq("user"), eq("REST"))).
                thenReturn(new ProvisioningManager.ProvisioningResult<>(userUR, List.of()));
        when(confParamOps.get(any(), eq(StandardConfParams.AUTHENTICATION_STATUSES), any(), eq(String[].class))).
                thenReturn(new String[] { "ACTIVE" });

        logic.update(userUR, false);

        verify(accessTokenDAO, never()).deleteByOwner(any());
    }

    @Test
    void statusSetsKeyAndDelegates() {
        LogicTestAuth.authenticate("user", "Master");

        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);

        StatusR statusR = new StatusR.Builder("old", StatusRType.SUSPEND).build();
        Realm realm = mock(Realm.class);

        when(binder.getAuthenticatedUserTO()).thenReturn(userTO);
        when(binder.getUserTO("key")).thenReturn(userTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(provisioningManager.suspend(statusR, false, "user", "REST")).
                thenReturn(new ProvisioningManager.ProvisioningResult<>("key", List.of()));

        logic.status(statusR, false);

        assertEquals("key", statusR.getKey());
    }

    @Test
    void deleteDelegates() {
        LogicTestAuth.authenticate("user", "Master");

        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        Realm realm = mock(Realm.class);

        when(binder.getAuthenticatedUserTO()).thenReturn(userTO);
        when(binder.getUserTO("key")).thenReturn(userTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(userDAO.isManager("key")).thenReturn(false);
        when(provisioningManager.delete("key", false, "user", "REST")).thenReturn(List.of());
        when(userDAO.existsById("key")).thenReturn(false);

        logic.delete(false);

        verify(userDAO).isManager("key");
        verify(provisioningManager).delete("key", false, "user", "REST");
    }

    @Test
    void mustChangePassword() {
        LogicTestAuth.authenticate("user", "Master",
                Map.of(IdRepoEntitlement.MUST_CHANGE_PASSWORD, Set.of(SyncopeConstants.ROOT_REALM)));

        UserTO userTO = new UserTO();
        userTO.setKey("key");
        userTO.setUsername("user");
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setStatus("ACTIVE");
        Realm realm = mock(Realm.class);

        PasswordPatch password = new PasswordPatch.Builder().build();
        when(binder.getAuthenticatedUserTO()).thenReturn(userTO);
        when(userDAO.findAllResourceKeys("key")).thenReturn(List.of("res1"));
        when(binder.getUserTO("key")).thenReturn(userTO);
        doReturn(Optional.of(realm)).when(realmSearchDAO).findByFullPath(SyncopeConstants.ROOT_REALM);
        when(realm.getActions()).thenReturn(List.of());
        when(provisioningManager.update(any(UserUR.class), anyBoolean(), eq("user"), eq("REST"))).
                thenAnswer(invocation -> new ProvisioningManager.ProvisioningResult<>(
                        (UserUR) invocation.getArgument(0), List.of()));
        when(binder.getUserTO("key")).thenReturn(userTO);
        when(confParamOps.get(any(), eq(StandardConfParams.AUTHENTICATION_STATUSES), any(), eq(String[].class))).
                thenReturn(new String[] { "ACTIVE" });

        logic.mustChangePassword(password, false);

        verify(accessTokenDAO).deleteByOwner("user");
    }

    @Test
    void readAuthenticated() {
        LogicTestAuth.authenticate("user", "Master",
                Map.of(IdRepoEntitlement.USER_READ, Set.of(SyncopeConstants.ROOT_REALM)));

        UserTO authenticated = new UserTO();
        authenticated.setKey("key");

        when(binder.getAuthenticatedUserTO()).thenReturn(authenticated);
        when(delegationDAO.findValidDelegating("key", OffsetDateTime.now())).thenReturn(List.of());

        UserSelfLogic.Self self = logic.read();

        assertNotNull(self);
        assertEquals(authenticated, self.user());
    }

    @Test
    void readAnonymous() {
        LogicTestAuth.authenticate("anonymous", "Master");

        UserTO authenticated = new UserTO();
        authenticated.setKey("key");

        when(binder.getAuthenticatedUserTO()).thenReturn(authenticated);
        when(delegationDAO.findValidDelegating("key", OffsetDateTime.now())).thenReturn(List.of());

        UserSelfLogic.Self self = logic.read();

        assertNotNull(self);
        assertEquals(authenticated, self.user());
    }

    @Test
    void complianceEmptyQuery() {
        ComplianceQuery query = new ComplianceQuery.Builder().build();

        assertThrows(SyncopeClientException.class, () -> logic.compliance(query));
    }

    @Test
    void complianceTokenValid() {
        ComplianceQuery query = new ComplianceQuery.Builder().
                token("token").
                username("user").
                build();

        Realm realm = mock(Realm.class);
        User user = mock(User.class);

        when(userDAO.findByToken("token")).thenReturn(Optional.of("key"));
        when(user.getRealm()).thenReturn(realm);
        when(user.getResources()).thenReturn(List.of());
        doReturn(Optional.of(user)).when(userDAO).findById("key");
        when(ruleProvider.getAccountPolicies(any(), any())).thenReturn(List.of());

        logic.compliance(query);
    }

    @Test
    void complianceUsernameInvalid() {
        ComplianceQuery query = new ComplianceQuery.Builder().
                token("token").
                username("user!").
                build();

        Realm realm = mock(Realm.class);
        User user = mock(User.class);

        when(userDAO.findByToken("token")).thenReturn(Optional.of("key"));
        when(user.getRealm()).thenReturn(realm);
        when(user.getResources()).thenReturn(List.of());
        doReturn(Optional.of(user)).when(userDAO).findById("key");
        when(ruleProvider.getAccountPolicies(any(), any())).thenReturn(List.of());

        assertThrows(InvalidEntityException.class, () -> logic.compliance(query));
    }

    @Test
    void compliancePasswordInvalid() {
        ComplianceQuery query = new ComplianceQuery.Builder().
                token("token").
                password("bad").
                build();

        Realm realm = mock(Realm.class);
        User user = mock(User.class);
        AccountPolicy accountPolicy = mock(AccountPolicy.class);
        PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);

        when(userDAO.findByToken("token")).thenReturn(Optional.of("key"));
        when(user.getRealm()).thenReturn(realm);
        when(user.getResources()).thenReturn(List.of());
        doReturn(Optional.of(user)).when(userDAO).findById("key");
        when(ruleProvider.getAccountPolicies(any(), any())).thenReturn(List.of(accountPolicy));
        when(ruleProvider.getAccountRules(accountPolicy)).thenReturn(List.of());
        when(ruleProvider.getPasswordPolicies(any(), any())).thenReturn(List.of(passwordPolicy));
        when(ruleProvider.getPasswordRules(passwordPolicy)).
                thenThrow(new PasswordPolicyException("bad"));

        assertThrows(InvalidEntityException.class, () -> logic.compliance(query));
    }
}
