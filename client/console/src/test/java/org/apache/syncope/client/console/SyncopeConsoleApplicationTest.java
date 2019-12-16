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
package org.apache.syncope.client.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class SyncopeConsoleApplicationTest extends AbstractTest {

    @Test
    public void securityHeaders() throws IOException {
        Map<String, String> securityHeaders = getConfiguredSecurityHeaders();
        assertEquals(4, securityHeaders.size());

        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);
        securityHeaders.forEach((key, value) -> assertEquals(value, TESTER.getLastResponse().getHeader(key)));

        FormTester formTester = TESTER.newFormTester("login");
        formTester.setValue("username", "username");
        formTester.setValue("password", "password");
        formTester.submit("submit");

        TESTER.assertRenderedPage(Dashboard.class);
        securityHeaders.forEach((key, value) -> assertEquals(value, TESTER.getLastResponse().getHeader(key)));
    }
}
