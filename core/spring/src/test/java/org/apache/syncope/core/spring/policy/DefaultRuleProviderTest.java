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
package org.apache.syncope.core.spring.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.api.rules.AccountRule;
import org.apache.syncope.core.provisioning.api.rules.PasswordRule;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DefaultRuleProviderTest {

    @Test
    void accountPoliciesFromResourcesAreDeduplicated() {
        AccountPolicy policy = mock(AccountPolicy.class);
        ExternalResource first = mock(ExternalResource.class);
        when(first.getAccountPolicy()).thenReturn(policy);
        ExternalResource second = mock(ExternalResource.class);
        when(second.getAccountPolicy()).thenReturn(policy);
        ExternalResource withoutPolicy = mock(ExternalResource.class);

        DefaultRuleProvider provider = new DefaultRuleProvider(mock(RealmSearchDAO.class));

        assertEquals(
                List.of(policy),
                provider.getAccountPolicies(null, List.of(first, second, withoutPolicy)));
    }

    @Test
    void accountPoliciesFromRealmAncestorsAreDeduplicated() {
        Realm realm = mock(Realm.class);
        Realm root = mock(Realm.class);
        when(root.getFullPath()).thenReturn("/");
        Realm child = mock(Realm.class);
        when(child.getFullPath()).thenReturn("/child");

        AccountPolicy policy = mock(AccountPolicy.class);
        AccountPolicy childPolicy = mock(AccountPolicy.class);
        Realm rootEntity = mock(Realm.class);
        when(rootEntity.getAccountPolicy()).thenReturn(policy);
        Realm otherEntity = mock(Realm.class);
        when(otherEntity.getAccountPolicy()).thenReturn(childPolicy);

        RealmSearchDAO realmSearchDAO = mock(RealmSearchDAO.class);
        when(realmSearchDAO.findAncestors(realm)).thenReturn(List.of(root, child));
        when(realmSearchDAO.findByFullPath("/")).thenReturn(Optional.of(rootEntity));
        when(realmSearchDAO.findByFullPath("/child")).thenReturn(Optional.of(otherEntity));

        DefaultRuleProvider provider = new DefaultRuleProvider(realmSearchDAO);

        assertEquals(List.of(policy, childPolicy), provider.getAccountPolicies(realm, List.of()));
    }

    @Test
    void accountPoliciesFromMissingRealmAreIgnored() {
        Realm realm = mock(Realm.class);
        Realm ancestor = mock(Realm.class);
        when(ancestor.getFullPath()).thenReturn("/gone");

        RealmSearchDAO realmSearchDAO = mock(RealmSearchDAO.class);
        when(realmSearchDAO.findAncestors(realm)).thenReturn(List.of(ancestor));
        when(realmSearchDAO.findByFullPath("/gone")).thenReturn(Optional.empty());

        DefaultRuleProvider provider = new DefaultRuleProvider(realmSearchDAO);

        assertTrue(provider.getAccountPolicies(realm, List.of()).isEmpty());
    }

    @Test
    void passwordPoliciesFromResourcesAndRealm() {
        PasswordPolicy resourcePolicy = mock(PasswordPolicy.class);
        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getPasswordPolicy()).thenReturn(resourcePolicy);

        Realm realm = mock(Realm.class);
        Realm ancestor = mock(Realm.class);
        when(ancestor.getFullPath()).thenReturn("/");

        PasswordPolicy realmPolicy = mock(PasswordPolicy.class);
        Realm realmEntity = mock(Realm.class);
        when(realmEntity.getPasswordPolicy()).thenReturn(realmPolicy);

        RealmSearchDAO realmSearchDAO = mock(RealmSearchDAO.class);
        when(realmSearchDAO.findAncestors(realm)).thenReturn(List.of(ancestor));
        when(realmSearchDAO.findByFullPath("/")).thenReturn(Optional.of(realmEntity));

        DefaultRuleProvider provider = new DefaultRuleProvider(realmSearchDAO);

        assertEquals(
                List.of(resourcePolicy, realmPolicy),
                provider.getPasswordPolicies(realm, List.of(resource)));
    }

    @Test
    void accountRulesAreBuiltAndFailuresIgnored() throws Exception {
        Implementation first = mock(Implementation.class);
        Implementation second = mock(Implementation.class);
        AccountPolicy policy = mock(AccountPolicy.class);
        doReturn(List.of(first, second)).when(policy).getRules();

        AccountRule rule = mock(AccountRule.class);

        try (MockedStatic<ImplementationManager> manager = mockStatic(ImplementationManager.class)) {
            manager.when(() -> ImplementationManager.buildAccountRule(
                    anyString(), eq(first), any(), any())).
                    thenReturn(Optional.of(rule));
            manager.when(() -> ImplementationManager.buildAccountRule(
                    anyString(), eq(second), any(), any())).
                    thenThrow(new ClassNotFoundException("not found"));

            DefaultRuleProvider provider = new DefaultRuleProvider(mock(RealmSearchDAO.class));

            assertEquals(List.of(rule), provider.getAccountRules(policy));
        }
    }

    @Test
    void passwordRulesAreBuiltAndFailuresIgnored() throws Exception {
        Implementation first = mock(Implementation.class);
        Implementation second = mock(Implementation.class);
        PasswordPolicy policy = mock(PasswordPolicy.class);
        doReturn(List.of(first, second)).when(policy).getRules();

        PasswordRule rule = mock(PasswordRule.class);

        try (MockedStatic<ImplementationManager> manager = mockStatic(ImplementationManager.class)) {
            manager.when(() -> ImplementationManager.buildPasswordRule(
                    anyString(), eq(first), any(), any())).
                    thenReturn(Optional.of(rule));
            manager.when(() -> ImplementationManager.buildPasswordRule(
                    anyString(), eq(second), any(), any())).
                    thenThrow(new ClassNotFoundException("not found"));

            DefaultRuleProvider provider = new DefaultRuleProvider(mock(RealmSearchDAO.class));

            assertEquals(List.of(rule), provider.getPasswordRules(policy));
        }
    }
}
