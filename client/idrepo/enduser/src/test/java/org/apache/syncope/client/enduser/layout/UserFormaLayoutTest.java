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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.junit.jupiter.api.Test;

class UserFormLayoutsTest {

    @Test
    void shouldReturnExactRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of("/even", customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/even"));
    }

    @Test
    void shouldReturnParentRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of("/even", customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/even/two"));
    }

    @Test
    void shouldReturnRootRealmLayoutWhenNoParentExists() {
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of());
        UserFormLayoutInfo rootLayout = userFormLayouts.getLayouts().get(SyncopeConstants.ROOT_REALM);
        assertNotNull(rootLayout);
        assertSame(rootLayout, userFormLayouts.getLayout("/odd"));
    }

    @Test
    void shouldReturnCustomRootRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of(SyncopeConstants.ROOT_REALM, customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/odd"));
    }

    @Test
    void shouldNotLoopWhenRealmDoesNotExist() {
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of());
        assertNotNull(userFormLayouts.getLayout("/unknown"));
    }

    @Test
    void shouldReturnCustomLayoutForRealm() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of("/even", customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/even"));
    }

    @Test
    void shouldKeepCustomRootRealmLayout() {
        UserFormLayoutInfo customLayout = new UserFormLayoutInfo();
        UserFormLayouts userFormLayouts = new UserFormLayouts(Map.of(SyncopeConstants.ROOT_REALM, customLayout));
        assertSame(customLayout, userFormLayouts.getLayout("/"));
    }
}
