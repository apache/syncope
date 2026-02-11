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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClient.Self;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class ManagerITCase extends AbstractITCase {

    private static void check(
            final String groupKey,
            final String managedKey,
            final String managedFIQL,
            final String managerUsername,
            final Optional<Consumer<Self>> selfCheck) {

        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        PagedResult<UserTO> matching = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).fiql(managedFIQL).page(1).size(1000).build());
        int fullMatchSize = matching.getResult().size();
        assertTrue(matching.getResult().stream().anyMatch(user -> managedKey.equals(user.getKey())));

        // check manager realms
        SyncopeClient managerClient = CLIENT_FACTORY.create(managerUsername, "password123");
        Self self = managerClient.self();
        Set<String> managerRealms = self.entitlements().get(IdRepoEntitlement.USER_SEARCH);
        assertEquals(1, managerRealms.size());
        RealmUtils.ManagerRealm realm = RealmUtils.ManagerRealm.of(managerRealms.iterator().next()).orElseThrow();
        assertEquals(SyncopeConstants.ROOT_REALM, realm.realmPath());
        assertEquals(AnyTypeKind.USER, realm.kind());
        assertEquals(managedKey, realm.anyKey());

        selfCheck.ifPresent(checker -> checker.accept(self));

        UserService managerService = managerClient.getService(UserService.class);

        // 1. search
        matching = managerService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().isNotNull("key").query()).
                        page(1).size(1000).build());
        assertEquals(fullMatchSize, matching.getResult().size());
        assertTrue(matching.getResult().stream().anyMatch(user -> managedKey.equals(user.getKey())));

        // 2. update and read
        UserUR memberUR = new UserUR();
        memberUR.setKey(managedKey);
        memberUR.setUsername(new StringReplacePatchItem.Builder().value("new" + getUUIDString()).build());

        Response response = managerService.update(memberUR);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        UserTO managed = managerService.read(managedKey);
        assertEquals(memberUR.getUsername().getValue(), managed.getUsername());

        // 3. create and update user under /even -> fail
        UserCR managedCR = UserITCase.getUniqueSample("uManagerTest2@syncope.org");
        managedCR.setRealm("/even");
        managedCR.getMemberships().add(new MembershipTO.Builder(groupKey).build());
        managed = createUser(managedCR).getEntity();

        UserUR evenUR = new UserUR();
        evenUR.setKey(managed.getKey());
        evenUR.setUsername(new StringReplacePatchItem.Builder().value("new" + getUUIDString()).build());
        SyncopeClientException e = assertThrows(SyncopeClientException.class, () -> managerService.update(evenUR));
        assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());

        // 4. delete
        managerService.delete(managedKey);
        e = assertThrows(SyncopeClientException.class, () -> USER_SERVICE.read(managedKey));
        assertEquals(ClientExceptionType.NotFound, e.getType());
    }

    /**
     * Layout:
     * 1. manager@syncope.org user
     * 2. uManagerTestGroup has uManager set to manager@syncope.org
     * 3. uManagerTest@syncope.org user is member of uManagerTestGroup
     */
    @Test
    public void uManager() {
        // prepare
        UserTO manager = createUser(UserITCase.getUniqueSample("manager@syncope.org")).getEntity();
        assertNotNull(manager);

        GroupCR groupCR = GroupITCase.getSample("uManagerTestGroup");
        groupCR.setUManager(manager.getKey());
        GroupTO group = createGroup(groupCR).getEntity();
        assertNotNull(group);
        assertEquals(manager.getKey(), group.getUManager());

        UserCR managedCR = UserITCase.getUniqueSample("uManagerTest@syncope.org");
        managedCR.getMemberships().add(new MembershipTO.Builder(group.getKey()).build());
        UserTO managed = createUser(managedCR).getEntity();

        // check
        check(
                group.getKey(),
                managed.getKey(),
                SyncopeClient.getUserSearchConditionBuilder().inGroups(group.getKey()).query(),
                manager.getUsername(),
                Optional.of(self -> {
                    Set<String> managerRealms = self.entitlements().get(IdRepoEntitlement.GROUP_READ);
                    assertEquals(1, managerRealms.size());
                    RealmUtils.ManagerRealm realm = RealmUtils.ManagerRealm.of(managerRealms.iterator().next()).
                            orElseThrow();
                    assertEquals(SyncopeConstants.ROOT_REALM, realm.realmPath());
                    assertEquals(AnyTypeKind.GROUP, realm.kind());
                    assertEquals(group.getKey(), realm.anyKey());
                }));
    }

    /**
     * Layout:
     * 1. uManagerTestGroup
     * 2. manager@syncope.org user is member of uManagerTestGroup
     * 3. uManagerTest@syncope.org user has gManager set to uManagerTestGroup
     */
    @Test
    public void gManager() {
        // prepare
        GroupTO group = createGroup(GroupITCase.getSample("uManagerTestGroup")).getEntity();
        assertNotNull(group);

        UserCR managerCR = UserITCase.getUniqueSample("manager@syncope.org");
        managerCR.getMemberships().add(new MembershipTO.Builder(group.getKey()).build());
        UserTO manager = createUser(managerCR).getEntity();
        assertNotNull(manager);

        UserCR managedCR = UserITCase.getUniqueSample("uManagerTest@syncope.org");
        managedCR.setGManager(group.getKey());
        UserTO managed = createUser(managedCR).getEntity();
        assertEquals(group.getKey(), managed.getGManager());

        // check
        check(
                group.getKey(),
                managed.getKey(),
                SyncopeClient.getUserSearchConditionBuilder().is("gManager").equalTo(group.getKey()).query(),
                manager.getUsername(),
                Optional.empty());
    }
}
