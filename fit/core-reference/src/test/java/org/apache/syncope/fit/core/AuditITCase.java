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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditITCase extends AbstractITCase {
    private static final String USER_KEY = getUUIDString();

    public static UserTO getSampleUserTO(final String email) {
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
    
    private static void deleteUserIfFound() {
        try {
            UserTO userTO = userService.read(USER_KEY);
            userService.delete(userTO.getKey());
        } catch (SyncopeClientException e) {
            if (e.getType() != ClientExceptionType.NotFound) {
                fail(e.getMessage(), e);
            }
        }
    }

    @AfterEach
    public void afterEach() {
        deleteUserIfFound();
    }

    @BeforeEach
    public void setup() {
        deleteUserIfFound();

        UserTO userTO = getSampleUserTO("example@syncope.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        userTO = result.getEntity();
        userService.read(userTO.getKey());
    }

    @Test
    public void findByUser() {
        PagedResult<AuditEntryTO> result = auditService.search(
            new AuditQuery.Builder()
                .key(USER_KEY)
                .orderBy("event_date desc")
                .page(1)
                .size(1)
                .build());
        assertNotNull(result);
        List<AuditEntryTO> results = result.getResult();
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(entry -> entry.getKey().equalsIgnoreCase(USER_KEY)));
    }
}
