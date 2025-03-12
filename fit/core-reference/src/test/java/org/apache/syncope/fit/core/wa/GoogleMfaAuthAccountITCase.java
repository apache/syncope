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
package org.apache.syncope.fit.core.wa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleMfaAuthAccountITCase extends AbstractITCase {

    private static GoogleMfaAuthAccount createGoogleMfaAuthAccount() {
        return new GoogleMfaAuthAccount.Builder()
                .registrationDate(OffsetDateTime.now())
                .scratchCodes(List.of(1, 2, 3, 4, 5))
                .secretKey(SecureRandomUtils.generateRandomUUID().toString())
                .validationCode(123456)
                .name(SecureRandomUtils.generateRandomUUID().toString())
                .build();
    }

    @BeforeEach
    public void setup() {
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.deleteAll();
    }

    @Test
    public void create() {
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        assertDoesNotThrow(() -> GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.create(UUID.randomUUID().toString(), acct));
    }

    @Test
    public void count() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.create(owner, acct);
        PagedResult<GoogleMfaAuthAccount> list = GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.list();
        assertFalse(list.getResult().isEmpty());
        assertEquals(1, list.getTotalCount());

        PagedResult<GoogleMfaAuthAccount> read = GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.read(owner);
        assertEquals(1, read.getTotalCount());
        assertFalse(read.getResult().isEmpty());
    }

    @Test
    public void delete() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.create(owner, acct);
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.delete(owner);
        assertThrows(SyncopeClientException.class, () -> GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.read(owner));
    }

    @Test
    public void update() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.create(owner, acct);
        acct = GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.read(acct.getId());
        acct.setSecretKey("NewSecret");
        acct.setScratchCodes(List.of(9, 8, 7, 6, 5));
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.update(owner, acct);
        assertEquals(1, GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.list().getTotalCount());
        acct = GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.read(owner).getResult().getFirst();
        assertEquals(acct.getSecretKey(), acct.getSecretKey());
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE.delete(owner);
    }
}
