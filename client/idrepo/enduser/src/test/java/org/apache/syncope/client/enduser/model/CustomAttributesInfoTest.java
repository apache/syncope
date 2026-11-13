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
package org.apache.syncope.client.enduser.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomAttributesInfoTest {

    @Test
    void defaults() {
        CustomAttributesInfo info = new CustomAttributesInfo();

        assertNotNull(info);
        assertNotNull(info.getAttributes());
        assertTrue(info.getAttributes().isEmpty());
    }

    @Test
    void settersAndGetters() {
        CustomAttributesInfo info = new CustomAttributesInfo();

        Map<String, CustomAttribute> attributes = new HashMap<>();
        attributes.put("attr1", new CustomAttribute());
        info.setAttributes(attributes);

        assertSame(attributes, info.getAttributes());
    }

    @Test
    void fluentApi() {
        CustomAttributesInfo info = new CustomAttributesInfo();
        CustomAttribute attribute = new CustomAttribute();

        CustomAttributesInfo result = info.attributes(Map.of("attr1", attribute));

        assertSame(info, result);
        assertEquals(1, info.getAttributes().size());
        assertSame(attribute, info.getAttributes().get("attr1"));
    }
}
