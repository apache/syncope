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
package org.apache.syncope.core.persistence.jpa.entity.am;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.jpa.converters.GoogleMfaAuthAccountListConverter;
import org.apache.syncope.core.persistence.jpa.converters.GoogleMfaAuthTokenListConverter;
import org.apache.syncope.core.persistence.jpa.converters.ImpersonationAccountListConverter;
import org.apache.syncope.core.persistence.jpa.converters.MfaTrustedDeviceListConverter;
import org.apache.syncope.core.persistence.jpa.converters.WebAuthnDeviceCredentialListConverter;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPAAuthProfile.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "owner" }))
public class JPAAuthProfile extends AbstractGeneratedKeyEntity implements AuthProfile {

    private static final long serialVersionUID = 57352617217394093L;

    public static final String TABLE = "AuthProfile";

    @Column(nullable = false)
    private String owner;

    @Convert(converter = ImpersonationAccountListConverter.class)
    @Lob
    private List<ImpersonationAccount> impersonationAccounts = new ArrayList<>();

    @Convert(converter = GoogleMfaAuthAccountListConverter.class)
    @Lob
    private List<GoogleMfaAuthAccount> googleMfaAuthAccounts = new ArrayList<>();

    @Convert(converter = GoogleMfaAuthTokenListConverter.class)
    @Lob
    private List<GoogleMfaAuthToken> googleMfaAuthTokens = new ArrayList<>();

    @Convert(converter = MfaTrustedDeviceListConverter.class)
    @Lob
    private List<MfaTrustedDevice> mfaTrustedDevices = new ArrayList<>();

    @Convert(converter = WebAuthnDeviceCredentialListConverter.class)
    @Lob
    private List<WebAuthnDeviceCredential> webAuthnDeviceCredentials = new ArrayList<>();

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
        return !googleMfaAuthTokens.contains(googleMfaAuthToken)
                && googleMfaAuthTokens.add(googleMfaAuthToken);
    }

    @Override
    public List<GoogleMfaAuthToken> getGoogleMfaAuthTokens() {
        return googleMfaAuthTokens;
    }

    @Override
    public boolean add(final GoogleMfaAuthAccount googleMfaAuthAccount) {
        return !googleMfaAuthAccounts.contains(googleMfaAuthAccount)
                && googleMfaAuthAccounts.add(googleMfaAuthAccount);
    }

    @Override
    public List<GoogleMfaAuthAccount> getGoogleMfaAuthAccounts() {
        return googleMfaAuthAccounts;
    }

    @Override
    public boolean add(final MfaTrustedDevice mfaTrustedDevice) {
        return !mfaTrustedDevices.contains(mfaTrustedDevice)
                && mfaTrustedDevices.add(mfaTrustedDevice);
    }

    @Override
    public List<MfaTrustedDevice> getMfaTrustedDevices() {
        return mfaTrustedDevices;
    }

    @Override
    public boolean add(final ImpersonationAccount impersonationAccount) {
        return !impersonationAccounts.contains(impersonationAccount)
                && impersonationAccounts.add(impersonationAccount);
    }

    @Override
    public List<ImpersonationAccount> getImpersonationAccounts() {
        return impersonationAccounts;
    }

    @Override
    public boolean add(final WebAuthnDeviceCredential webAuthnDeviceCredential) {
        return !webAuthnDeviceCredentials.contains(webAuthnDeviceCredential)
                && webAuthnDeviceCredentials.add(webAuthnDeviceCredential);
    }

    @Override
    public List<WebAuthnDeviceCredential> getWebAuthnDeviceCredentials() {
        return webAuthnDeviceCredentials;
    }
}
