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
package org.apache.syncope.core.persistence.api.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class RealmUtilsTest extends AbstractTest {

    @Test
    public void getGroupOwnerRealm() {
        String realmPath = "realmPath";
        String groupKey = "groupKey";
        assertEquals(realmPath + "@" + groupKey, RealmUtils.getGroupOwnerRealm(realmPath, groupKey));
    }

    @Test
    public void parseGroupOwnerRealm() {
        assertEquals(
                Optional.of(Pair.of("realmPath", "groupKey")),
                RealmUtils.parseGroupOwnerRealm("realmPath@groupKey"));
        assertFalse(RealmUtils.parseGroupOwnerRealm("realmPath").isPresent());
    }

    @Test
    public void normalizingAddTo() {
        Set<String> realms = new HashSet<>();
        realms.add("realm1");
        realms.add("realm2");
        String newRealm = "realm123";
        assertFalse(RealmUtils.normalizingAddTo(realms, newRealm));
        assertEquals(2, realms.size());

        realms.clear();
        realms.add("testRealm1");
        realms.add("realm2");
        newRealm = "test";
        assertTrue(RealmUtils.normalizingAddTo(realms, newRealm));
        assertEquals(2, realms.size());
    }

    @Test
    public void getEffective() {
        Set<String> allowedRealms = new HashSet<>();
        String requestedRealm = "requestedRealm";
        allowedRealms.add("testRealm1");
        allowedRealms.add("testRealm2");
        allowedRealms.add("testRealm3");
        allowedRealms.add("requestedRealm");
        Set<String> effective = RealmUtils.getEffective(allowedRealms, requestedRealm);
        assertEquals(allowedRealms, effective);
    }
}
