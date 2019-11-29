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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class AuditITCase extends AbstractITCase {

    private AuditEntryTO query(final AuditQuery query, final int maxWaitSeconds) {
        int i = 0;
        List<AuditEntryTO> results = Collections.emptyList();
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            results = auditService.search(query).getResult();

            i++;
        } while (results.isEmpty() && i < maxWaitSeconds);
        if (results.isEmpty()) {
            fail("Timeout when executing query for key " + query.getEntityKey());
        }

        return results.get(0);
    }

    @Test
    public void findByUser() {
        UserTO userTO = createUser(UserITCase.getUniqueSampleTO("audit@syncope.org")).getEntity();
        assertNotNull(userTO.getKey());

        AuditQuery query = new AuditQuery.Builder(userTO.getKey()).orderBy("event_date desc").
                page(1).size(1).build();
        AuditEntryTO entry = query(query, 50);
        assertEquals(userTO.getKey(), entry.getKey());
        userService.delete(userTO.getKey());
    }

    @Test
    public void findByUserAndOther() {
        UserTO userTO = createUser(UserITCase.getUniqueSampleTO("audit-2@syncope.org")).getEntity();
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
        AuditEntryTO entry = query(query, 50);
        assertEquals(userTO.getKey(), entry.getKey());
        userService.delete(userTO.getKey());
    }

    @Test
    public void findByGroup() {
        GroupTO groupTO = createGroup(GroupITCase.getBasicSampleTO("AuditGroup")).getEntity();
        assertNotNull(groupTO.getKey());

        AuditQuery query = new AuditQuery.Builder(groupTO.getKey()).orderBy("event_date desc").
                page(1).size(1).build();
        AuditEntryTO entry = query(query, 50);
        assertEquals(groupTO.getKey(), entry.getKey());
        groupService.delete(groupTO.getKey());
    }
}
