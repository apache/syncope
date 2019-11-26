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

import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditITCase extends AbstractITCase {
    private static final String USER_KEY = getUUIDString();
    private static final String GROUP_KEY = getUUIDString();

    private static GroupTO getSampleGroupTO(final String name) {
        GroupTO groupTO = new GroupTO();
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);
        groupTO.setName(name + getUUIDString());
        groupTO.setKey(GROUP_KEY);
        return groupTO;
    }

    private static UserTO getSampleUserTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword("password123");
        userTO.setUsername(email);
        userTO.setKey(USER_KEY);
        userTO.getPlainAttrs().add(attrTO("fullname", "Apache Syncope"));
        userTO.getPlainAttrs().add(attrTO("firstname", "Apache"));
        userTO.getPlainAttrs().add(attrTO("surname", "Syncope"));
        userTO.getPlainAttrs().add(attrTO("userId", email));
        userTO.getPlainAttrs().add(attrTO("email", email));
        return userTO;
    }

    @Test
    public void findByUser() {
        UserTO userTO = getSampleUserTO("example@syncope.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        userTO = result.getEntity();
        userService.read(userTO.getKey());

        PagedResult<AuditEntryTO> auditResult = auditService.search(
            new AuditQuery.Builder()
                .key(USER_KEY)
                .orderBy("event_date desc")
                .page(1)
                .size(1)
                .build());
        assertNotNull(auditResult);
        List<AuditEntryTO> results = auditResult.getResult();
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertTrue(results.stream().allMatch(entry -> entry.getKey().equalsIgnoreCase(USER_KEY)));
        userService.delete(userTO.getKey());
    }

    @Test
    public void findByGroup() {
        GroupTO groupTO = getSampleGroupTO("AuditGroup");
        ProvisioningResult<GroupTO> groupResult = createGroup(groupTO);
        assertNotNull(groupResult);
        groupTO = groupResult.getEntity();
        groupService.read(groupTO.getKey());

        PagedResult<AuditEntryTO> result = auditService.search(
            new AuditQuery.Builder()
                .key(GROUP_KEY)
                .orderBy("event_date asc")
                .page(1)
                .size(1)
                .build());
        assertNotNull(result);
        List<AuditEntryTO> results = result.getResult();
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertTrue(results.stream().allMatch(entry -> entry.getKey().equalsIgnoreCase(GROUP_KEY)));
        groupService.delete(groupTO.getKey());
    }
}
