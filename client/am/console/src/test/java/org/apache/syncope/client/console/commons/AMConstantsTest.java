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
package org.apache.syncope.client.console.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AMConstantsTest {

    @Test
    void constants() {
        assertEquals("gatewayroute.paginator.rows", AMConstants.PREF_GATEWAYROUTE_PAGINATOR_ROWS);
        assertEquals("waconfig.paginator.rows", AMConstants.PREF_WACONFIG_PAGINATOR_ROWS);
        assertEquals("wasession.paginator.rows", AMConstants.PREF_WASESSION_PAGINATOR_ROWS);
        assertEquals("authmodule.paginator.rows", AMConstants.PREF_AUTHMODULE_PAGINATOR_ROWS);
        assertEquals("clientapp.paginator.rows", AMConstants.PREF_CLIENTAPP_PAGINATOR_ROWS);
        assertEquals("clientapp.properties.paginator.rows",
                AMConstants.PREF_CLIENTAPP_PROPERTIES_PAGINATOR_ROWS);
        assertEquals("accesspolicy.conf.attrs.paginator.rows",
                AMConstants.PREF_ACCESS_POLICY_CONF_ATTRS_PAGINATOR_ROWS);
        assertEquals("saml2idpentity.properties.paginator.rows",
                AMConstants.PREF_SAML2_IDP_ENTITY_PAGINATOR_ROWS);
        assertEquals("authprofile.paginator.rows", AMConstants.PREF_AUTHPROFILE_PAGINATOR_ROWS);
        assertEquals("authprofile.impersonated.paginator.rows",
                AMConstants.PREF_AUTHPROFILE_IMPERSONATED_PAGINATOR_ROWS);
        assertEquals("authprofile.googlemfaauthtokens.paginator.rows",
                AMConstants.PREF_AUTHPROFILE_GOOGLEMFAAUTHTOKENS_PAGINATOR_ROWS);
        assertEquals("authprofile.googlemfaauthaccounts.paginator.rows",
                AMConstants.PREF_AUTHPROFILE_GOOGLEMFAAUTHACCOUNTS_PAGINATOR_ROWS);
        assertEquals("authprofile.mfaTrustedDevices.paginator.rows",
                AMConstants.PREF_AUTHPROFILE_MFA_TRUSTED_FDEVICES_PAGINATOR_ROWS);
        assertEquals("authprofile.webAuthnDeviceCredentials.paginator.rows",
                AMConstants.PREF_AUTHPROFILE_WEBAUTHNDEVICECREDENTIALS_PAGINATOR_ROWS);
        assertEquals("authprofile.consentDecisions.paginator.rows",
                AMConstants.PREF_AUTHPROFILE_CONSENT_DECISION_PAGINATOR_ROWS);
        assertEquals("oidc.customScopes.paginator.rows", AMConstants.PREF_OIDC_CUSTOMSCOPES_PAGINATOR_ROWS);
    }
}
