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
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class AuditITCase extends AbstractITCase {

    public static UserTO getSampleUserTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword("password123");
        userTO.setUsername(email);
        userTO.getPlainAttrs().add(attrTO("email", email));
        return userTO;
    }

    @BeforeEach
    public void setup() {
        /**
         * Execute operations to activate audits.
         */
        PagedResult<UserTO> matching = userService.search(
            new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM)
                .fiql(new UserFiqlSearchConditionBuilder()
                    .is("username").equalTo("example@syncope.org").query())
                .size(1)
                .page(1)
                .details(false)
                .build());
        assertNotNull(matching);
        if (matching.getSize() > 0) {
            UserTO userTO = matching.getResult().get(0);
            userService.delete(userTO.getKey());
        }

        UserTO userTO = getSampleUserTO("example@syncope.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        ProvisioningResult<UserTO> result = createUser(userTO);
        assertNotNull(result);
        userTO = result.getEntity();
        userTO = userService.read(userTO.getKey());
        userService.delete(userTO.getKey());
    }

    @Test
    public void list() {
    }
}
