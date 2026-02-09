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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.junit.jupiter.api.Test;

public class RealmUtilsTest extends AbstractTest {

    @Test
    public void getManagerRealm() {
        String realmPath = "realmPath";
        String groupKey = "groupKey";
        assertEquals(
                realmPath + "@GROUP@" + groupKey,
                new RealmUtils.ManagerRealm(realmPath, AnyTypeKind.GROUP, groupKey).output());
    }

    @Test
    public void parseGroupOwnerRealm() {
        assertEquals(
                Optional.of(new RealmUtils.ManagerRealm("realmPath", AnyTypeKind.GROUP, "groupKey")),
                RealmUtils.ManagerRealm.of("realmPath@GROUP@groupKey"));
        assertFalse(RealmUtils.ManagerRealm.of("realmPath").isPresent());
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
        Set<String> allowedRealms = Set.of(
                "testRealm1@k1",
                "testRealm2@k2",
                "testRealm3@k3",
                "requestedRealm");
        Set<String> effective = RealmUtils.getEffective(allowedRealms, "requestedRealm");
        assertEquals(allowedRealms, effective);
    }
}
