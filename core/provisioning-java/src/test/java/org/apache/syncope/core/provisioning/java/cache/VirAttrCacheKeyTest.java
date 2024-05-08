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
package org.apache.syncope.core.provisioning.java.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class VirAttrCacheKeyTest {

    @Test
    public void test() {
        String type = "type";
        String key = "key";
        String virSchema = "virSchema";
        VirAttrCacheKey cacheKey = VirAttrCacheKey.of(type, key, virSchema);
        VirAttrCacheKey cacheKey2 = VirAttrCacheKey.of(type, key, virSchema);
        VirAttrCacheKey cacheKey3 = VirAttrCacheKey.of(type, String.format(type, "3"), String.format(virSchema, "3"));
        Object nullObj = null;

        assertEquals(cacheKey.hashCode(), cacheKey2.hashCode());
        assertFalse(cacheKey.equals(nullObj));
        assertFalse(cacheKey.equals(String.class));
        assertTrue(cacheKey.equals(cacheKey));
        assertTrue(cacheKey.equals(cacheKey2));
        assertFalse(cacheKey.equals(cacheKey3));

        assertEquals(cacheKey.toString(), cacheKey2.toString());
    }
}
