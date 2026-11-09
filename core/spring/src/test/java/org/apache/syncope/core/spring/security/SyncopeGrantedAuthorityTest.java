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
package org.apache.syncope.core.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.junit.jupiter.api.Test;

class SyncopeGrantedAuthorityTest {

    @Test
    void entitlementOnlyConstructor() {
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ);

        assertEquals(IdRepoEntitlement.USER_READ, authority.getAuthority());
        assertTrue(authority.getRealms().isEmpty());
    }

    @Test
    void realmConstructor() {
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/a");

        assertEquals(Set.of("/a"), authority.getRealms());
    }

    @Test
    void normalizingAddRealm() {
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ);

        assertTrue(authority.addRealm("/a"));
        assertEquals(Set.of("/a"), authority.getRealms());

        assertFalse(authority.addRealm("/a/b"));
        assertEquals(Set.of("/a"), authority.getRealms());

        assertTrue(authority.addRealm(SyncopeConstants.ROOT_REALM));
        assertEquals(Set.of(SyncopeConstants.ROOT_REALM), authority.getRealms());
    }

    @Test
    void addRealms() {
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ);

        authority.addRealms(List.of("/a", "/a/b", "/c"));

        assertEquals(Set.of("/a", "/c"), authority.getRealms());
    }

    @Test
    void realmsAreUnmodifiable() {
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/a");

        assertThrows(UnsupportedOperationException.class, () -> authority.getRealms().add("/b"));
    }

    @Test
    void equalsAndHashCode() {
        SyncopeGrantedAuthority first = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/a");
        SyncopeGrantedAuthority same = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/a");
        SyncopeGrantedAuthority differentRealm = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/b");
        SyncopeGrantedAuthority differentEntitlement = new SyncopeGrantedAuthority(IdRepoEntitlement.GROUP_READ, "/a");

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, differentRealm);
        assertNotEquals(first, differentEntitlement);
        assertNotEquals(first, null);
        assertNotEquals(first, "other");
    }

    @Test
    void toStringContainsEntitlementAndRealms() {
        SyncopeGrantedAuthority authority = new SyncopeGrantedAuthority(IdRepoEntitlement.USER_READ, "/a");

        assertTrue(authority.toString().contains(IdRepoEntitlement.USER_READ));
        assertTrue(authority.toString().contains("/a"));
    }
}
