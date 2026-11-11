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
package org.apache.syncope.client.console.resources.saml2sp4ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.syncope.client.console.pages.SAML2SPLogin;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;

class ConsoleAssertionConsumerResourceTest {

    @Test
    void loginPageIsSAML2SPLogin() {
        assertEquals(SAML2SPLogin.class, new ConsoleAssertionConsumerResource().getLoginPageClass());
    }

    @Test
    void selfRegistrationIsNotSupported() {
        ConsoleAssertionConsumerResource resource = new ConsoleAssertionConsumerResource();

        assertThrows(UnsupportedOperationException.class, () -> resource.getSelfRegInfo(new UserTO()));
    }
}
