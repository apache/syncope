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
import org.apache.syncope.common.lib.types.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.types.U2FRegisteredDevice;
import org.apache.syncope.core.persistence.api.entity.auth.AuthProfile;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = JPAAuthProfile.TABLE, uniqueConstraints = @UniqueConstraint(columnNames = {"owner"}))
public class JPAAuthProfile extends AbstractGeneratedKeyEntity implements AuthProfile {

    public static final String TABLE = "AuthProfile";

    private static final long serialVersionUID = 57352617217394093L;

    @Lob
    private String u2fRegisteredDevices;

    @Lob
    private String googleMfaAuthAccounts;

    @Lob
    private String googleMfaAuthTokens;

    @Column(nullable = false)
    private String owner;

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
        return googleMfaAuthTokens == null
            ? new ArrayList<>(0)
            : POJOHelper.deserialize(googleMfaAuthTokens, new TypeReference<List<GoogleMfaAuthToken>>() {
        });
    }

    @Override
    public void setGoogleMfaAuthTokens(final List<GoogleMfaAuthToken> tokens) {
        this.googleMfaAuthTokens = POJOHelper.serialize(tokens);
    }

    @Override
    public List<GoogleMfaAuthAccount> getGoogleMfaAuthAccounts() {
        return googleMfaAuthAccounts == null
            ? new ArrayList<>(0)
            : POJOHelper.deserialize(googleMfaAuthAccounts, new TypeReference<List<GoogleMfaAuthAccount>>() {
        });
    }

    @Override
    public void setGoogleMfaAuthAccounts(final List<GoogleMfaAuthAccount> accounts) {
        this.googleMfaAuthAccounts = POJOHelper.serialize(accounts);
    }

    @Override
    public void add(final GoogleMfaAuthToken token) {
        checkType(token, GoogleMfaAuthToken.class);
        final List<GoogleMfaAuthToken> tokens = getGoogleMfaAuthTokens();
        tokens.add(token);
        setGoogleMfaAuthTokens(tokens);
    }

    @Override
    public List<U2FRegisteredDevice> getU2FRegisteredDevices() {
        return u2fRegisteredDevices == null
            ? new ArrayList<>(0)
            : POJOHelper.deserialize(u2fRegisteredDevices, new TypeReference<List<U2FRegisteredDevice>>() {
        });
    }

    @Override
    public void setU2FRegisteredDevices(final List<U2FRegisteredDevice> records) {
        this.u2fRegisteredDevices = POJOHelper.serialize(records);
    }

    @Override
    public void add(final GoogleMfaAuthAccount account) {
        checkType(account, GoogleMfaAuthAccount.class);
        final List<GoogleMfaAuthAccount> accounts = getGoogleMfaAuthAccounts();
        accounts.add(account);
        setGoogleMfaAuthAccounts(accounts);
    }

    @Override
    public void add(final U2FRegisteredDevice registration) {
        checkType(registration, U2FRegisteredDevice.class);
        final List<U2FRegisteredDevice> records = getU2FRegisteredDevices();
        records.add(registration);
        setU2FRegisteredDevices(records);
    }
}
