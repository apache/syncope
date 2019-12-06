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

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class AuditITCase extends AbstractITCase {

    private static final int MAX_WAIT_SECONDS = 50;

    private static AuditEntryTO query(final AuditQuery query, final int maxWaitSeconds, final boolean failIfEmpty) {
        List<AuditEntryTO> results = query(query, maxWaitSeconds);
        if (results.isEmpty()) {
            if (failIfEmpty) {
                fail("Timeout when executing query for key " + query.getEntityKey());
            }
            return null;
        }
        return results.get(0);
    }

    private static List<AuditEntryTO> query(final AuditQuery query, final int maxWaitSeconds) {
        int i = 0;
        List<AuditEntryTO> results = List.of();
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            results = auditService.search(query).getResult();
            i++;
        } while (results.isEmpty() && i < maxWaitSeconds);
        return results;
    }

    @Test
    public void userReadAndSearchYieldsNoAudit() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder(userTO.getKey()).build();
        List<AuditEntryTO> entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());

        PagedResult<UserTO> usersTOs = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo(userTO.getUsername()).
                                query()).
                        build());
        assertNotNull(usersTOs);
        assertFalse(usersTOs.getResult().isEmpty());

        entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());
    }

    @Test
    public void findByUser() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder(userTO.getKey()).orderBy("event_date desc").
                page(1).size(1).build();
        AuditEntryTO entry = query(query, MAX_WAIT_SECONDS, true);
        assertEquals(userTO.getKey(), entry.getKey());
        userService.delete(userTO.getKey());
    }

    @Test
    public void findByUserAndOther() {
        UserTO userTO = createUser(UserITCase.getUniqueSample("audit-2@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder(userTO.getKey()).
                orderBy("event_date desc").
                page(1).
                size(1).
                type(AuditElements.EventCategoryType.LOGIC).
                category("UserLogic").
                event("create").
                result(AuditElements.Result.SUCCESS).
                build();
        AuditEntryTO entry = query(query, MAX_WAIT_SECONDS, true);
        assertEquals(userTO.getKey(), entry.getKey());
        userService.delete(userTO.getKey());
    }

    @Test
    public void findByGroup() {
        GroupTO groupTO = createGroup(GroupITCase.getBasicSample("AuditGroup")).getEntity();
        assertNotNull(groupTO.getKey());

        AuditQuery query = new AuditQuery.Builder(groupTO.getKey()).orderBy("event_date desc").
                page(1).size(1).build();
        AuditEntryTO entry = query(query, MAX_WAIT_SECONDS, true);
        assertEquals(groupTO.getKey(), entry.getKey());
        groupService.delete(groupTO.getKey());
    }

    @Test
    public void groupReadAndSearchYieldsNoAudit() {
        GroupTO groupTO = createGroup(GroupITCase.getBasicSample("AuditGroupSearch")).getEntity();
        assertNotNull(groupTO.getKey());

        AuditQuery query = new AuditQuery.Builder(groupTO.getKey()).build();
        List<AuditEntryTO> entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());

        PagedResult<GroupTO> groups = groupService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo(groupTO.getName()).
                                query()).
                        build());
        assertNotNull(groups);
        assertFalse(groups.getResult().isEmpty());

        entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());
    }

    @Test
    public void findByAnyObject() {
        AnyObjectTO anyObjectTO = createAnyObject(AnyObjectITCase.getSample("Italy")).getEntity();
        assertNotNull(anyObjectTO.getKey());
        AuditQuery query = new AuditQuery.Builder(anyObjectTO.getKey()).orderBy("event_date desc").
                page(1).size(1).build();
        AuditEntryTO entry = query(query, MAX_WAIT_SECONDS, true);
        assertEquals(anyObjectTO.getKey(), entry.getKey());
        anyObjectService.delete(anyObjectTO.getKey());
    }

    @Test
    public void anyObjectReadAndSearchYieldsNoAudit() {
        AnyObjectTO anyObjectTO = createAnyObject(AnyObjectITCase.getSample("USA")).getEntity();
        assertNotNull(anyObjectTO);

        AuditQuery query = new AuditQuery.Builder(anyObjectTO.getKey()).build();
        List<AuditEntryTO> entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());

        PagedResult<AnyObjectTO> anyObjects = anyObjectService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(anyObjectTO.getType()).query()).
                        build());
        assertNotNull(anyObjects);
        assertFalse(anyObjects.getResult().isEmpty());

        entries = query(query, MAX_WAIT_SECONDS);
        assertEquals(1, entries.size());
    }
}
