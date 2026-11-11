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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SAML2SP4UITypesTest {

    @Test
    void entitlements() {
        assertEquals(
                Set.of(
                        SAML2SP4UIEntitlement.IDP_READ,
                        SAML2SP4UIEntitlement.IDP_IMPORT,
                        SAML2SP4UIEntitlement.IDP_UPDATE,
                        SAML2SP4UIEntitlement.IDP_DELETE),
                SAML2SP4UIEntitlement.values());
        assertTrue(SAML2SP4UIEntitlement.values().contains(SAML2SP4UIEntitlement.IDP_IMPORT));
        assertThrows(
                UnsupportedOperationException.class,
                () -> SAML2SP4UIEntitlement.values().add("unexpected"));
    }

    @Test
    void implementationTypes() {
        assertEquals(
                Map.of(
                        SAML2SP4UIImplementationType.IDP_ACTIONS,
                        "org.apache.syncope.core.provisioning.api.SAML2SP4UIIdPActions",
                        SAML2SP4UIImplementationType.REQUESTED_AUTHN_CONTEXT_PROVIDER,
                        "org.apache.syncope.core.provisioning.api.RequestedAuthnContextProvider"),
                SAML2SP4UIImplementationType.values());
        assertThrows(
                UnsupportedOperationException.class,
                () -> SAML2SP4UIImplementationType.values().put("unexpected", "value"));
    }
}
