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
import org.apache.syncope.common.lib.search.UserFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AuditEntryTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuditITCase extends AbstractITCase {

    public static UserTO getSampleUserTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword("password123");
        userTO.setUsername(email);
        userTO.getPlainAttrs().add(attrTO("fullname", "Apache Syncope"));
        userTO.getPlainAttrs().add(attrTO("firstname", "Apache"));
        userTO.getPlainAttrs().add(attrTO("surname", "Syncope"));
        userTO.getPlainAttrs().add(attrTO("userId", email));
        userTO.getPlainAttrs().add(attrTO("email", email));
        return userTO;
    }

    private static Optional<UserTO> findUser(final String username) {
        PagedResult<UserTO> matching = userService.search(
            new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM)
                .fiql(new UserFiqlSearchConditionBuilder()
                    .is("username").equalTo(username).query())
                .size(1)
                .page(1)
                .details(false)
                .build());
        assertNotNull(matching);
        if (matching.getSize() > 0) {
            return Optional.of(matching.getResult().get(0));
        }
        return Optional.empty();
    }

    private static void deleteUserIfFound() {
        findUser("example@syncope.org").ifPresent(user -> userService.delete(user.getKey()));
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
        findUser("example@syncope.org").ifPresent(user -> {
            PagedResult<AuditEntryTO> result = auditService.search(
                new AuditQuery.Builder()
                    .key(user.getKey())
                    .orderBy("event_date desc")
                    .page(1)
                    .size(1)
                    .build());
            assertNotNull(result);
            List<AuditEntryTO> results = result.getResult();
            assertFalse(results.isEmpty());
            assertTrue(results.stream().allMatch(entry -> entry.getKey().equalsIgnoreCase(user.getKey())));
        });
    }
}
