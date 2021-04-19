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
package org.apache.syncope.core.persistence.api.entity.auth;

import java.util.List;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.common.lib.wa.U2FDevice;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.persistence.api.entity.Entity;

public interface AuthProfile extends Entity {

    String getOwner();

    void setOwner(String owner);

    List<GoogleMfaAuthToken> getGoogleMfaAuthTokens();

    void setGoogleMfaAuthTokens(List<GoogleMfaAuthToken> tokens);

    List<U2FDevice> getU2FRegisteredDevices();

    void setU2FRegisteredDevices(List<U2FDevice> records);

    List<GoogleMfaAuthAccount> getGoogleMfaAuthAccounts();

    void setGoogleMfaAuthAccounts(List<GoogleMfaAuthAccount> accounts);

    List<WebAuthnDeviceCredential> getWebAuthnDeviceCredentials();

    void setWebAuthnDeviceCredentials(List<WebAuthnDeviceCredential> credentials);

    List<ImpersonationAccount> getImpersonationAccounts();

    void setImpersonationAccounts(List<ImpersonationAccount> accounts);
}
