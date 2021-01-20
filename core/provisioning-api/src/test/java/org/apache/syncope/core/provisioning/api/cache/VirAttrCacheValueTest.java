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
package org.apache.syncope.core.provisioning.api.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;

public class VirAttrCacheValueTest extends AbstractTest {

    @Test
    public void test() {
        Object nullObj = null;
        VirAttrCacheValue cacheValue = new VirAttrCacheValue(List.of("testValue"));
        VirAttrCacheValue cacheValue2 = new VirAttrCacheValue(List.of());

        assertNotEquals(cacheValue.getValues(), cacheValue2.getValues());
        assertNotEquals(cacheValue.hashCode(), cacheValue2.hashCode());
        assertFalse(cacheValue.equals(cacheValue2));
        assertTrue(cacheValue.equals(cacheValue));
        assertFalse(cacheValue2.equals(nullObj));
        assertFalse(cacheValue2.equals(String.class));
        assertNotEquals(cacheValue.toString(), cacheValue2.toString());
    }
}
