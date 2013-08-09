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
package org.apache.syncope.core.policy;

import static org.junit.Assert.fail;

import org.apache.syncope.common.types.AccountPolicySpec;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.junit.Before;
import org.junit.Test;

public class AccountPolicyEnforcerTest {

    private AccountPolicyEnforcer enforcer;

    private AccountPolicySpec spec;

    private SyncopeUser user;

    @Before
    public void setUp() {
        enforcer = new AccountPolicyEnforcer();
        spec = new AccountPolicySpec();
        user = new SyncopeUser();
    }

    @Test
    public void testAllUpperCase() throws Exception {
        spec.setAllUpperCase(true);

        user.setUsername("ABC123XYZ");
        enforcer.enforce(spec, null, user);

        user.setUsername("ABC123xYZ");
        try {
            enforcer.enforce(spec, null, user);
            fail();
        } catch (AccountPolicyException e) {
        }
    }

    @Test
    public void testAllLowerCase() throws Exception {
        spec.setAllLowerCase(true);

        user.setUsername("abc123xyz");
        enforcer.enforce(spec, null, user);

        user.setUsername("abc123xYz");
        try {
            enforcer.enforce(spec, null, user);
            fail();
        } catch (AccountPolicyException e) {
        }
    }

    @Test
    public void testDefaultPattern() throws Exception {
        user.setUsername("abc123xyz");
        enforcer.enforce(spec, null, user);

        user.setUsername("abc123xYz+");
        try {
            enforcer.enforce(spec, null, user);
            fail();
        } catch (AccountPolicyException e) {
        }
    }

    @Test
    public void testExplicitPattern() throws Exception {
        spec.setPattern("[a-zA-Z0-9-_@.+ ]+");

        user.setUsername("abc123xYz+");
        enforcer.enforce(spec, null, user);

        user.setUsername("abc123xYz+/");
        try {
            enforcer.enforce(spec, null, user);
            fail();
        } catch (AccountPolicyException e) {
        }
    }
}
