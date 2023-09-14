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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;

public class AuthProfileTO implements EntityTO {

    private static final long serialVersionUID = -6543425997956703057L;

    public static class Builder {

        private final AuthProfileTO instance = new AuthProfileTO();

        public AuthProfileTO.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public AuthProfileTO.Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public AuthProfileTO.Builder googleMfaAuthToken(final GoogleMfaAuthToken token) {
            instance.getGoogleMfaAuthTokens().add(token);
            return this;
        }

        public AuthProfileTO.Builder googleMfaAuthTokens(final GoogleMfaAuthToken... tokens) {
            instance.getGoogleMfaAuthTokens().addAll(List.of(tokens));
            return this;
        }

        public AuthProfileTO.Builder googleMfaAuthTokens(final Collection<GoogleMfaAuthToken> tokens) {
            instance.getGoogleMfaAuthTokens().addAll(tokens);
            return this;
        }

        public AuthProfileTO.Builder googleMfaAuthAccount(final GoogleMfaAuthAccount account) {
            instance.getGoogleMfaAuthAccounts().add(account);
            return this;
        }

        public AuthProfileTO.Builder googleMfaAuthAccounts(final GoogleMfaAuthAccount... accounts) {
            instance.getGoogleMfaAuthAccounts().addAll(List.of(accounts));
            return this;
        }

        public AuthProfileTO.Builder googleMfaAuthAccounts(final Collection<GoogleMfaAuthAccount> accounts) {
            instance.getGoogleMfaAuthAccounts().addAll(accounts);
            return this;
        }

        public AuthProfileTO.Builder mfaTrustedDevice(final MfaTrustedDevice device) {
            instance.getMfaTrustedDevices().add(device);
            return this;
        }

        public AuthProfileTO.Builder mfaTrustedDevices(final MfaTrustedDevice... devices) {
            instance.getMfaTrustedDevices().addAll(List.of(devices));
            return this;
        }

        public AuthProfileTO.Builder mfaTrustedDevices(final Collection<MfaTrustedDevice> devices) {
            instance.getMfaTrustedDevices().addAll(devices);
            return this;
        }

        public AuthProfileTO.Builder credential(final WebAuthnDeviceCredential credential) {
            instance.getWebAuthnDeviceCredentials().add(credential);
            return this;
        }

        public AuthProfileTO.Builder credentials(final WebAuthnDeviceCredential... credentials) {
            instance.getWebAuthnDeviceCredentials().addAll(List.of(credentials));
            return this;
        }

        public AuthProfileTO.Builder credentials(final Collection<WebAuthnDeviceCredential> credentials) {
            instance.getWebAuthnDeviceCredentials().addAll(credentials);
            return this;
        }

        public AuthProfileTO build() {
            return instance;
        }
    }

    private String key;

    private String owner;

    private final List<ImpersonationAccount> impersonationAccounts = new ArrayList<>();

    private final List<GoogleMfaAuthToken> googleMfaAuthTokens = new ArrayList<>();

    private final List<GoogleMfaAuthAccount> googleMfaAuthAccounts = new ArrayList<>();

    private final List<MfaTrustedDevice> mfaTrustedDevices = new ArrayList<>();

    private final List<WebAuthnDeviceCredential> webAuthnDeviceCredentials = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @JacksonXmlElementWrapper(localName = "impersonationAccounts")
    @JacksonXmlProperty(localName = "impersonationAccount")
    public List<ImpersonationAccount> getImpersonationAccounts() {
        return impersonationAccounts;
    }

    @JacksonXmlElementWrapper(localName = "googleMfaAuthTokens")
    @JacksonXmlProperty(localName = "googleMfaAuthToken")
    public List<GoogleMfaAuthToken> getGoogleMfaAuthTokens() {
        return googleMfaAuthTokens;
    }

    @JacksonXmlElementWrapper(localName = "googleMfaAuthAccounts")
    @JacksonXmlProperty(localName = "googleMfaAuthAccount")
    public List<GoogleMfaAuthAccount> getGoogleMfaAuthAccounts() {
        return googleMfaAuthAccounts;
    }

    @JacksonXmlElementWrapper(localName = "mfaTrustedDevices")
    @JacksonXmlProperty(localName = "mfaTrustedDevice")
    public List<MfaTrustedDevice> getMfaTrustedDevices() {
        return mfaTrustedDevices;
    }

    @JacksonXmlElementWrapper(localName = "credentials")
    @JacksonXmlProperty(localName = "credential")
    public List<WebAuthnDeviceCredential> getWebAuthnDeviceCredentials() {
        return webAuthnDeviceCredentials;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(owner).
                append(impersonationAccounts).
                append(googleMfaAuthTokens).
                append(googleMfaAuthAccounts).
                append(mfaTrustedDevices).
                append(webAuthnDeviceCredentials).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AuthProfileTO other = (AuthProfileTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(owner, other.owner).
                append(impersonationAccounts, other.impersonationAccounts).
                append(googleMfaAuthTokens, other.googleMfaAuthTokens).
                append(googleMfaAuthAccounts, other.googleMfaAuthAccounts).
                append(mfaTrustedDevices, other.mfaTrustedDevices).
                append(webAuthnDeviceCredentials, other.webAuthnDeviceCredentials).
                build();
    }
}
