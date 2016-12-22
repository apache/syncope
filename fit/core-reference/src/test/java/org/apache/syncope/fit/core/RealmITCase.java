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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.policy.DefaultAccountRuleConf;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class RealmITCase extends AbstractITCase {

    private RealmTO getRealm(final String fullPath) {
        return IterableUtils.find(realmService.list(fullPath), new Predicate<RealmTO>() {

            @Override
            public boolean evaluate(final RealmTO object) {
                return fullPath.equals(object.getFullPath());
            }
        });
    }

    @Test
    public void list() {
        List<RealmTO> realms = realmService.list();
        assertNotNull(realms);
        assertFalse(realms.isEmpty());
        for (RealmTO realm : realms) {
            assertNotNull(realm);
        }

        try {
            realmService.list("a name");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPath, e.getType());
        }
    }

    @Test
    public void createUpdate() {
        final RealmTO realm = new RealmTO();
        realm.setName("last");

        // 1. create
        Response response = realmService.create("/even/two", realm);
        RealmTO[] actuals = getObject(response.getLocation(), RealmService.class, RealmTO[].class);
        assertNotNull(actuals);
        assertTrue(actuals.length > 0);
        RealmTO actual = actuals[0];
        assertNotNull(actual.getKey());
        assertEquals("last", actual.getName());
        assertEquals("/even/two/last", actual.getFullPath());
        assertEquals(actual.getParent(), getRealm("/even/two").getKey());
        assertNull(realm.getAccountPolicy());
        assertNull(realm.getPasswordPolicy());

        // 2. update setting policies
        actual.setAccountPolicy("06e2ed52-6966-44aa-a177-a0ca7434201f");
        actual.setPasswordPolicy("986d1236-3ac5-4a19-810c-5ab21d79cba1");
        realmService.update(actual);

        actual = getRealm(actual.getFullPath());
        assertNotNull(actual.getAccountPolicy());
        assertNotNull(actual.getPasswordPolicy());

        // 3. update changing parent
        actual.setParent(getRealm("/odd").getKey());
        realmService.update(actual);

        actual = getRealm("/odd/last");
        assertNotNull(actual);
        assertEquals("/odd/last", actual.getFullPath());

        assertEquals(1, IterableUtils.countMatches(realmService.list(), new Predicate<RealmTO>() {

            @Override
            public boolean evaluate(final RealmTO object) {
                return realm.getName().equals(object.getName());
            }
        }));

        // 4. create under invalid path
        try {
            realmService.create("a name", realm);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidPath, e.getType());
        }

        // 5. attempt to create duplicate
        try {
            realmService.create("/odd", realm);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.EntityExists, e.getType());
        }
    }

    @Test
    public void deletingAccountPolicy() {
        // 1. create account policy
        AccountPolicyTO policy = new AccountPolicyTO();
        policy.setDescription("deletingAccountPolicy");

        DefaultAccountRuleConf ruleConf = new DefaultAccountRuleConf();
        ruleConf.setMinLength(3);
        ruleConf.setMaxLength(8);
        policy.getRuleConfs().add(ruleConf);

        policy = createPolicy(policy);
        assertNotNull(policy);

        // 2. create realm with policy assigned
        RealmTO realm = new RealmTO();
        realm.setName("withppolicy");
        realm.setAccountPolicy(policy.getKey());

        Response response = realmService.create(SyncopeConstants.ROOT_REALM, realm);
        RealmTO[] actuals = getObject(response.getLocation(), RealmService.class, RealmTO[].class);
        assertNotNull(actuals);
        assertTrue(actuals.length > 0);
        RealmTO actual = actuals[0];
        assertEquals(policy.getKey(), actual.getAccountPolicy());

        // 3. remove policy
        policyService.delete(policy.getKey());

        // 4. verify
        actual = getRealm(actual.getFullPath());
        assertNull(actual.getAccountPolicy());
    }

    @Test
    public void delete() {
        RealmTO realm = new RealmTO();
        realm.setName("deletable3");

        Response response = realmService.create("/even/two", realm);
        RealmTO[] actuals = getObject(response.getLocation(), RealmService.class, RealmTO[].class);
        assertNotNull(actuals);
        assertTrue(actuals.length > 0);
        RealmTO actual = actuals[0];

        realmService.delete(actual.getFullPath());

        try {
            realmService.list(actual.getFullPath());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void deleteNonEmpty() {
        try {
            realmService.delete("/even/two");
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.AssociatedAnys, e.getType());
            assertEquals(3, e.getElements().size());
        }
    }

    @Test
    public void propagate() {
        // 1. create realm and add the LDAP resource
        RealmTO realm = new RealmTO();
        realm.setName("test");
        realm.getResources().add(RESOURCE_NAME_LDAP_ORGUNIT);

        // 2. check propagation
        ProvisioningResult<RealmTO> result =
                realmService.create("/", realm).readEntity(new GenericType<ProvisioningResult<RealmTO>>() {
        });
        assertNotNull(result);
        assertEquals(1, result.getPropagationStatuses().size());
        assertEquals(RESOURCE_NAME_LDAP_ORGUNIT, result.getPropagationStatuses().get(0).getResource());
        assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

        realm = result.getEntity();

        // 3. check on LDAP
        assertNotNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=test,o=isp"));

        // 4. remove realm
        realmService.delete(realm.getFullPath());

        // 5. check on LDAP
        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=test,o=isp"));
    }
}
