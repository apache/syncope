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
package org.apache.syncope.core.persistence.jpa.entity.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAAuthProfile.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "owner" }))
public class JPAAuthProfile extends AbstractGeneratedKeyEntity implements AuthProfile {

    public static final String TABLE = "AuthProfile";

    private static final long serialVersionUID = 57352617217394093L;

    @Column(nullable = false)
    private String owner;

    @Lob
    private String impersonationAccounts;

    @Lob
    private String googleMfaAuthAccounts;

    @Lob
    private String googleMfaAuthTokens;

    @Lob
    private String u2fRegisteredDevices;

    @Lob
    private String webAuthnDeviceCredentials;

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public List<GoogleMfaAuthToken> getGoogleMfaAuthTokens() {
        return Optional.ofNullable(googleMfaAuthTokens).
                map(v -> POJOHelper.deserialize(v, new TypeReference<List<GoogleMfaAuthToken>>() {
        })).orElseGet(() -> new ArrayList<>(0));
    }

    @Override
    public void setGoogleMfaAuthTokens(final List<GoogleMfaAuthToken> tokens) {
        googleMfaAuthTokens = POJOHelper.serialize(tokens);
    }

    @Override
    public List<GoogleMfaAuthAccount> getGoogleMfaAuthAccounts() {
        return Optional.ofNullable(googleMfaAuthAccounts).
                map(v -> POJOHelper.deserialize(v, new TypeReference<List<GoogleMfaAuthAccount>>() {
        })).orElseGet(() -> new ArrayList<>(0));
    }

    @Override
    public void setGoogleMfaAuthAccounts(final List<GoogleMfaAuthAccount> accounts) {
        googleMfaAuthAccounts = POJOHelper.serialize(accounts);
    }

    @Override
    public List<U2FDevice> getU2FRegisteredDevices() {
        return Optional.ofNullable(u2fRegisteredDevices).
                map(v -> POJOHelper.deserialize(v, new TypeReference<List<U2FDevice>>() {
        })).orElseGet(() -> new ArrayList<>(0));
    }

    @Override
    public void setU2FRegisteredDevices(final List<U2FDevice> records) {
        u2fRegisteredDevices = POJOHelper.serialize(records);
    }

    @Override
    public List<ImpersonationAccount> getImpersonationAccounts() {
        return Optional.ofNullable(impersonationAccounts).
                map(v -> POJOHelper.deserialize(v, new TypeReference<List<ImpersonationAccount>>() {
        })).orElseGet(() -> new ArrayList<>(0));
    }

    @Override
    public void setImpersonationAccounts(final List<ImpersonationAccount> accounts) {
        impersonationAccounts = POJOHelper.serialize(accounts);
    }

    @Override
    public List<WebAuthnDeviceCredential> getWebAuthnDeviceCredentials() {
        return Optional.ofNullable(webAuthnDeviceCredentials).
                map(v -> POJOHelper.deserialize(v, new TypeReference<List<WebAuthnDeviceCredential>>() {
        })).orElseGet(() -> new ArrayList<>(0));
    }

    @Override
    public void setWebAuthnDeviceCredentials(final List<WebAuthnDeviceCredential> credentials) {
        webAuthnDeviceCredentials = POJOHelper.serialize(credentials);
    }
}
