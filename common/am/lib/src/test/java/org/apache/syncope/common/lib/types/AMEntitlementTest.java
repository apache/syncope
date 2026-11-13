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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AMEntitlementTest {

    @Test
    void values() {
        Set<String> values = AMEntitlement.values();

        assertEquals(40, values.size());
        assertTrue(values.contains(AMEntitlement.CLIENTAPP_READ));
        assertTrue(values.contains(AMEntitlement.AUTH_MODULE_CREATE));
        assertTrue(values.contains(AMEntitlement.ATTR_REPO_UPDATE));
        assertTrue(values.contains(AMEntitlement.PASSWORD_MANAGEMENT_DELETE));
        assertTrue(values.contains(AMEntitlement.SAML2_IDP_ENTITY_GET));
        assertTrue(values.contains(AMEntitlement.AUTH_PROFILE_LIST));
        assertTrue(values.contains(AMEntitlement.OIDC_JWKS_GENERATE));
        assertTrue(values.contains(AMEntitlement.WA_CONFIG_PUSH));
        assertTrue(values.contains(AMEntitlement.WA_SESSION_DELETE));
        assertFalse(values.contains("CLIENTAPP_READ_UNKNOWN"));
    }

    @Test
    void valuesAreUnmodifiableAndStable() {
        assertSame(AMEntitlement.values(), AMEntitlement.values());
        assertThrows(UnsupportedOperationException.class, () -> AMEntitlement.values().add("__test__"));
    }
}
