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
package org.apache.syncope.client.ui.commons.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;

class ConnObjectWrapperTest {

    @Test
    void constructorAndGetters() {
        UserTO any = new UserTO();
        any.setKey("key1");

        String resource = "ldap";

        ConnObjectWrapper wrapper = new ConnObjectWrapper(any, resource, null);

        assertNotNull(wrapper);
        assertEquals(any, wrapper.getAny());
        assertEquals(resource, wrapper.getResource());
        assertEquals(null, wrapper.getConnObjectTO());
    }

    @Test
    void constructorWithConnObject() {
        GroupTO any = new GroupTO();
        any.setKey("key2");

        ConnObject connObject = new ConnObject() {
            private static final long serialVersionUID = 1L;
        };

        ConnObjectWrapper wrapper = new ConnObjectWrapper(any, "adb", connObject);

        assertEquals(any, wrapper.getAny());
        assertEquals("adb", wrapper.getResource());
        assertEquals(connObject, wrapper.getConnObjectTO());
    }
}
