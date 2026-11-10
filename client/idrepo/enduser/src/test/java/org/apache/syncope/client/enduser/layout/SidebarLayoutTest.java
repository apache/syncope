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
package org.apache.syncope.client.enduser.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SidebarLayoutTest {

    @Test
    void defaults() {
        SidebarLayout layout = new SidebarLayout();

        assertNotNull(layout);
        assertTrue(layout.isEditUserEnabled());
        assertTrue(layout.isPasswordManagementEnabled());
        assertTrue(layout.isSecurityQuestionManagementEnabled());
        assertTrue(layout.isDismissMfaEnabled());
        assertNotNull(layout.getExtensionsEnabled());
        assertTrue(layout.getExtensionsEnabled().isEmpty());
    }

    @Test
    void isExtensionEnabledDefaultTrue() {
        SidebarLayout layout = new SidebarLayout();

        assertTrue(layout.isExtensionEnabled("unknown"));
    }

    @Test
    void settersAndGetters() {
        SidebarLayout layout = new SidebarLayout();

        layout.setEditUserEnabled(false);
        assertFalse(layout.isEditUserEnabled());

        layout.setPasswordManagementEnabled(false);
        assertFalse(layout.isPasswordManagementEnabled());

        layout.setSecurityQuestionManagementEnabled(false);
        assertFalse(layout.isSecurityQuestionManagementEnabled());

        layout.setDismissMfaEnabled(false);
        assertFalse(layout.isDismissMfaEnabled());

        Map<String, Boolean> extensions = Map.of("ext1", false, "ext2", true);
        layout.getExtensionsEnabled().putAll(extensions);
        assertEquals(2, layout.getExtensionsEnabled().size());
        assertFalse(layout.isExtensionEnabled("ext1"));
        assertTrue(layout.isExtensionEnabled("ext2"));
    }
}
