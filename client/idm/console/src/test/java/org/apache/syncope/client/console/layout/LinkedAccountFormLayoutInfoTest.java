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
package org.apache.syncope.client.console.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.client.console.wizards.any.LinkedAccountWizardBuilder;
import org.junit.jupiter.api.Test;

class LinkedAccountFormLayoutInfoTest {

    @Test
    void defaults() {
        LinkedAccountFormLayoutInfo info = new LinkedAccountFormLayoutInfo();

        assertNotNull(info);
        assertEquals(LinkedAccountWizardBuilder.class, info.getFormClass());
        assertTrue(info.isPlainAttrs());
        assertTrue(info.isCredentials());
        assertNotNull(info.getWhichPlainAttrs());
        assertTrue(info.getWhichPlainAttrs().isEmpty());
        assertNotNull(info.getWhichCredentials());
        assertTrue(info.getWhichCredentials().isEmpty());
    }

    @Test
    void settersAndGetters() {
        LinkedAccountFormLayoutInfo info = new LinkedAccountFormLayoutInfo();

        info.setPlainAttrs(false);
        assertFalse(info.isPlainAttrs());

        info.setCredentials(false);
        assertFalse(info.isCredentials());

        List<String> credentials = List.of("cred1", "cred2");
        info.getWhichCredentials().addAll(credentials);
        assertEquals(credentials, info.getWhichCredentials());

        List<String> plainAttrs = List.of("attr1", "attr2");
        info.getWhichPlainAttrs().addAll(plainAttrs);
        assertEquals(plainAttrs, info.getWhichPlainAttrs());
    }
}
