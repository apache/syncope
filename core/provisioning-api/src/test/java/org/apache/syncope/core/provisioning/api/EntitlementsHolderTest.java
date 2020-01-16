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
package org.apache.syncope.core.provisioning.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.junit.jupiter.api.Test;

public class EntitlementsHolderTest extends AbstractTest {

    private final EntitlementsHolder entitlementsHolder = EntitlementsHolder.getInstance();

    Collection<String> values = new HashSet<>();

    @Test
    public void test() {
        String testValue = "testValue";
        values.add(testValue);

        entitlementsHolder.init(values);
        assertEquals(values, entitlementsHolder.getValues());

        Set<String> added = entitlementsHolder.addFor(testValue);
        assertTrue(added.contains(String.format("%s_%s", testValue, "SEARCH")));
        assertTrue(added.contains(String.format("%s_%s", testValue, "READ")));
        assertTrue(added.contains(String.format("%s_%s", testValue, "CREATE")));
        assertTrue(added.contains(String.format("%s_%s", testValue, "UPDATE")));
        assertTrue(added.contains(String.format("%s_%s", testValue, "DELETE")));
        assertEquals(values.size() + AnyEntitlement.values().length, entitlementsHolder.getValues().size());

        Set<String> removed = entitlementsHolder.removeFor(testValue);
        assertTrue(removed.contains(String.format("%s_%s", testValue, "SEARCH")));
        assertTrue(removed.contains(String.format("%s_%s", testValue, "READ")));
        assertTrue(removed.contains(String.format("%s_%s", testValue, "CREATE")));
        assertTrue(removed.contains(String.format("%s_%s", testValue, "UPDATE")));
        assertTrue(removed.contains(String.format("%s_%s", testValue, "DELETE")));
        
        assertEquals(values.size(), entitlementsHolder.getValues().size());
    }

}
