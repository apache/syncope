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
import java.util.Optional;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import tools.jackson.core.type.TypeReference;

@Node(Neo4jAuthProfile.NODE)
public class Neo4jAuthProfile extends AbstractGeneratedKeyNode implements AuthProfile {

    private static final long serialVersionUID = 57352617217394093L;

    public static final String NODE = "AuthProfile";

    protected static final TypeReference<List<GoogleMfaAuthToken>> GOOGLE_MFA_TOKENS_TYPEREF =
            new TypeReference<List<GoogleMfaAuthToken>>() {
    };

    protected static final TypeReference<List<GoogleMfaAuthAccount>> GOOGLE_MFA_ACCOUNTS_TYPEREF =
            new TypeReference<List<GoogleMfaAuthAccount>>() {
    };

    protected static final TypeReference<List<MfaTrustedDevice>> MFA_TRUSTED_DEVICE_TYPEREF =
            new TypeReference<List<MfaTrustedDevice>>() {
    };

    protected static final TypeReference<List<ImpersonationAccount>> IMPERSONATION_TYPEREF =
            new TypeReference<List<ImpersonationAccount>>() {
    };

    protected static final TypeReference<List<WebAuthnDeviceCredential>> WEBAUTHN_TYPEREF =
            new TypeReference<List<WebAuthnDeviceCredential>>() {
    };

    @NotNull
    private String owner;

    private String googleMfaAuthAccounts;

    @Transient
    private List<GoogleMfaAuthAccount> googleMfaAuthAccountsList = new ArrayList<>();

    private String googleMfaAuthTokens;

    @Transient
    private List<GoogleMfaAuthToken> googleMfaAuthTokensList = new ArrayList<>();

    private String mfaTrustedDevices;

    @Transient
    private List<MfaTrustedDevice> mfaTrustedDevicesList = new ArrayList<>();

    private String impersonationAccounts;

    @Transient
    private List<ImpersonationAccount> impersonationAccountsList = new ArrayList<>();

    private String webAuthnDeviceCredentials;

    @Transient
    private List<WebAuthnDeviceCredential> webAuthnDeviceCredentialsList = new ArrayList<>();

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
        return googleMfaAuthTokensList.add(googleMfaAuthToken);
    }

    @Override
    public List<GoogleMfaAuthToken> getGoogleMfaAuthTokens() {
        return googleMfaAuthTokensList;
    }

    @Override
    public boolean add(final GoogleMfaAuthAccount googleMfaAuthAccount) {
        return googleMfaAuthAccountsList.add(googleMfaAuthAccount);
    }

    @Override
    public List<GoogleMfaAuthAccount> getGoogleMfaAuthAccounts() {
        return googleMfaAuthAccountsList;
    }

    @Override
    public boolean add(final MfaTrustedDevice mfaTrustedDevice) {
        return mfaTrustedDevicesList.add(mfaTrustedDevice);
    }

    @Override
    public List<MfaTrustedDevice> getMfaTrustedDevices() {
        return mfaTrustedDevicesList;
    }

    @Override
    public boolean add(final ImpersonationAccount impersonationAccount) {
        return impersonationAccountsList.add(impersonationAccount);
    }

    @Override
    public List<ImpersonationAccount> getImpersonationAccounts() {
        return impersonationAccountsList;
    }

    @Override
    public boolean add(final WebAuthnDeviceCredential webAuthnDeviceCredential) {
        return webAuthnDeviceCredentialsList.add(webAuthnDeviceCredential);
    }

    @Override
    public List<WebAuthnDeviceCredential> getWebAuthnDeviceCredentials() {
        return webAuthnDeviceCredentialsList;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getGoogleMfaAuthTokens().clear();
            getGoogleMfaAuthAccounts().clear();
            getMfaTrustedDevices().clear();
            getImpersonationAccounts().clear();
            getWebAuthnDeviceCredentials().clear();
        }
        Optional.ofNullable(googleMfaAuthTokens).ifPresent(v -> getGoogleMfaAuthTokens().
                addAll(POJOHelper.deserialize(v, GOOGLE_MFA_TOKENS_TYPEREF)));
        Optional.ofNullable(googleMfaAuthAccounts).ifPresent(v -> getGoogleMfaAuthAccounts().
                addAll(POJOHelper.deserialize(v, GOOGLE_MFA_ACCOUNTS_TYPEREF)));
        Optional.ofNullable(mfaTrustedDevices).ifPresent(v -> getMfaTrustedDevices().
                addAll(POJOHelper.deserialize(v, MFA_TRUSTED_DEVICE_TYPEREF)));
        Optional.ofNullable(impersonationAccounts).ifPresent(v -> getImpersonationAccounts().
                addAll(POJOHelper.deserialize(v, IMPERSONATION_TYPEREF)));
        Optional.ofNullable(webAuthnDeviceCredentials).ifPresent(v -> getWebAuthnDeviceCredentials().
                addAll(POJOHelper.deserialize(v, WEBAUTHN_TYPEREF)));
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        googleMfaAuthTokens = POJOHelper.serialize(getGoogleMfaAuthTokens());
        googleMfaAuthAccounts = POJOHelper.serialize(getGoogleMfaAuthAccounts());
        mfaTrustedDevices = POJOHelper.serialize(getMfaTrustedDevices());
        impersonationAccounts = POJOHelper.serialize(getImpersonationAccounts());
        webAuthnDeviceCredentials = POJOHelper.serialize(getWebAuthnDeviceCredentials());
    }
}
