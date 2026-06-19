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
package org.apache.syncope.core.persistence.neo4j.entity.am;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WAConsentDecision;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.neo4j.converters.GoogleMfaAuthAccountListConverter;
import org.apache.syncope.core.persistence.neo4j.converters.GoogleMfaAuthTokenListConverter;
import org.apache.syncope.core.persistence.neo4j.converters.ImpersonationAccountListConverter;
import org.apache.syncope.core.persistence.neo4j.converters.MfaTrustedDeviceListConverter;
import org.apache.syncope.core.persistence.neo4j.converters.WAConsentDecisionListConverter;
import org.apache.syncope.core.persistence.neo4j.converters.WebAuthnDeviceCredentialListConverter;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jAuthProfile.NODE)
public class Neo4jAuthProfile extends AbstractGeneratedKeyNode implements AuthProfile {

    private static final long serialVersionUID = 57352617217394093L;

    public static final String NODE = "AuthProfile";

    @NotNull
    private String owner;

    @ConvertWith(converter = GoogleMfaAuthAccountListConverter.class)
    private List<GoogleMfaAuthAccount> googleMfaAuthAccounts = new ArrayList<>();

    @ConvertWith(converter = GoogleMfaAuthTokenListConverter.class)
    private List<GoogleMfaAuthToken> googleMfaAuthTokens = new ArrayList<>();

    @ConvertWith(converter = MfaTrustedDeviceListConverter.class)
    private List<MfaTrustedDevice> mfaTrustedDevices = new ArrayList<>();

    @ConvertWith(converter = ImpersonationAccountListConverter.class)
    private List<ImpersonationAccount> impersonationAccounts = new ArrayList<>();

    @ConvertWith(converter = WebAuthnDeviceCredentialListConverter.class)
    private List<WebAuthnDeviceCredential> webAuthnDeviceCredentials = new ArrayList<>();

    @ConvertWith(converter = WAConsentDecisionListConverter.class)
    private List<WAConsentDecision> waConsentDecisions = new ArrayList<>();

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public boolean add(final GoogleMfaAuthToken googleMfaAuthToken) {
        return googleMfaAuthTokens.add(googleMfaAuthToken);
    }

    @Override
    public List<GoogleMfaAuthToken> getGoogleMfaAuthTokens() {
        return googleMfaAuthTokens;
    }

    @Override
    public boolean add(final GoogleMfaAuthAccount googleMfaAuthAccount) {
        return googleMfaAuthAccounts.add(googleMfaAuthAccount);
    }

    @Override
    public List<GoogleMfaAuthAccount> getGoogleMfaAuthAccounts() {
        return googleMfaAuthAccounts;
    }

    @Override
    public boolean add(final MfaTrustedDevice mfaTrustedDevice) {
        return mfaTrustedDevices.add(mfaTrustedDevice);
    }

    @Override
    public List<MfaTrustedDevice> getMfaTrustedDevices() {
        return mfaTrustedDevices;
    }

    @Override
    public boolean add(final ImpersonationAccount impersonationAccount) {
        return impersonationAccounts.add(impersonationAccount);
    }

    @Override
    public List<ImpersonationAccount> getImpersonationAccounts() {
        return impersonationAccounts;
    }

    @Override
    public boolean add(final WebAuthnDeviceCredential webAuthnDeviceCredential) {
        return webAuthnDeviceCredentials.add(webAuthnDeviceCredential);
    }

    @Override
    public List<WebAuthnDeviceCredential> getWebAuthnDeviceCredentials() {
        return webAuthnDeviceCredentials;
    }

    @Override
    public boolean add(final WAConsentDecision consentDecision) {
        return waConsentDecisions.contains(consentDecision);
    }

    @Override
    public List<WAConsentDecision> getConsentDecisions() {
        return waConsentDecisions;
    }
}
