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

import java.util.Date;
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
                .registrationDate(new Date())
                .scratchCodes(List.of(1, 2, 3, 4, 5))
                .secretKey(SecureRandomUtils.generateRandomUUID().toString())
                .validationCode(123456)
                .name(SecureRandomUtils.generateRandomUUID().toString())
                .build();
    }

    @BeforeEach
    public void setup() {
        googleMfaAuthAccountService.deleteAll();
    }

    @Test
    public void create() {
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        assertDoesNotThrow(() -> googleMfaAuthAccountService.create(UUID.randomUUID().toString(), acct));
    }

    @Test
    public void count() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        googleMfaAuthAccountService.create(owner, acct);
        PagedResult<GoogleMfaAuthAccount> list = googleMfaAuthAccountService.list();
        assertFalse(list.getResult().isEmpty());
        assertEquals(1, list.getTotalCount());

        PagedResult<GoogleMfaAuthAccount> read = googleMfaAuthAccountService.read(owner);
        assertEquals(1, read.getTotalCount());
        assertFalse(read.getResult().isEmpty());
    }

    @Test
    public void delete() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        googleMfaAuthAccountService.create(owner, acct);
        googleMfaAuthAccountService.delete(owner);
        assertThrows(SyncopeClientException.class, () -> googleMfaAuthAccountService.read(owner));
    }

    @Test
    public void update() {
        String owner = UUID.randomUUID().toString();
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        googleMfaAuthAccountService.create(owner, acct);
        acct = googleMfaAuthAccountService.read(acct.getId());
        acct.setSecretKey("NewSecret");
        acct.setScratchCodes(List.of(9, 8, 7, 6, 5));
        googleMfaAuthAccountService.update(owner, acct);
        assertEquals(1, googleMfaAuthAccountService.list().getTotalCount());
        acct = googleMfaAuthAccountService.read(owner).getResult().get(0);
        assertEquals(acct.getSecretKey(), acct.getSecretKey());
        googleMfaAuthAccountService.delete(owner);
    }
}
