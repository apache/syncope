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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.apache.syncope.client.console.panels.AnyPanel;
import org.junit.jupiter.api.Test;

class ConsolePropertiesTest {

    @Test
    void defaults() {
        ConsoleProperties props = new ConsoleProperties();

        assertNotNull(props);
        assertEquals(AnyPanel.class.getName(), props.getDefaultAnyPanelClass());
        assertEquals(20, props.getRealmsFullTreeThreshold());
        assertEquals(3, props.getJwtExpirationMinutesThreshold());
        assertTrue(props.getPage().isEmpty());
        assertTrue(props.isUseGZIPCompression());
        assertTrue(props.isxForward());
        assertTrue(props.isCsrf());
        assertEquals("admin", props.getAdminUser());
    }

    @Test
    void settersAndGetters() {
        ConsoleProperties props = new ConsoleProperties();

        props.setDefaultAnyPanelClass("custom.panel.Class");
        assertEquals("custom.panel.Class", props.getDefaultAnyPanelClass());

        props.setRealmsFullTreeThreshold(50);
        assertEquals(50, props.getRealmsFullTreeThreshold());

        props.setJwtExpirationMinutesThreshold(10);
        assertEquals(10, props.getJwtExpirationMinutesThreshold());

        props.setAdminUser("adminUser");
        assertEquals("adminUser", props.getAdminUser());

        props.setAnonymousUser("anonymous");
        assertEquals("anonymous", props.getAnonymousUser());

        props.setAnonymousKey("anonymousKey");
        assertEquals("anonymousKey", props.getAnonymousKey());

        props.setUseGZIPCompression(false);
        assertFalse(props.isUseGZIPCompression());

        props.setxForward(false);
        assertFalse(props.isxForward());

        props.setCsrf(false);
        assertFalse(props.isCsrf());

        Map<String, String> securityHeaders = props.getSecurityHeaders();
        securityHeaders.put("Custom-Header", "value");
        assertEquals(1, securityHeaders.size());
    }

    @Test
    void pageMap() {
        ConsoleProperties props = new ConsoleProperties();

        Map<String, Class<? extends org.apache.syncope.client.console.pages.BasePage>> page = props.getPage();
        assertNotNull(page);
        assertTrue(page.isEmpty());
    }
}
