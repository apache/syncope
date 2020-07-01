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
package org.apache.syncope.fit.core;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.GoogleMfaAuthAccount;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GoogleMfaAuthAccountITCase extends AbstractITCase {

    private static GoogleMfaAuthAccount createGoogleMfaAuthAccount() {
        String id = SecureRandomUtils.generateRandomUUID().toString();
        return new GoogleMfaAuthAccount.Builder()
            .registrationDate(new Date())
            .scratchCodes(List.of(1, 2, 3, 4, 5))
            .secretKey(SecureRandomUtils.generateRandomUUID().toString())
            .validationCode(123456)
            .owner(id)
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
        assertDoesNotThrow(() -> {
            Response response = googleMfaAuthAccountService.save(acct);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
                if (ex != null) {
                    throw ex;
                }
            }
        });
    }

    @Test
    public void count() {
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        googleMfaAuthAccountService.save(acct);
        assertEquals(1, googleMfaAuthAccountService.countAll().getTotalCount());
        assertEquals(1, googleMfaAuthAccountService.countFor(acct.getOwner()).getTotalCount());
        assertFalse(googleMfaAuthAccountService.findAccountsFor(acct.getOwner()).getResult().isEmpty());
    }

    @Test
    public void deleteByOwner() {
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        Response response = googleMfaAuthAccountService.save(acct);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);
        response = googleMfaAuthAccountService.deleteAccountsFor(acct.getOwner());
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode());
        assertThrows(SyncopeClientException.class, () -> googleMfaAuthAccountService.findAccountsFor(acct.getOwner()));
    }

    @Test
    public void update() {
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        Response response = googleMfaAuthAccountService.save(acct);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        acct = googleMfaAuthAccountService.findAccountBy(key);
        acct.setSecretKey("NewSecret");
        acct.setScratchCodes(List.of(9, 8, 7, 6, 5));
        googleMfaAuthAccountService.update(acct);
        assertEquals(1, googleMfaAuthAccountService.countAll().getTotalCount());
        acct = googleMfaAuthAccountService.findAccountsFor(acct.getOwner()).getResult().get(0);
        assertEquals(acct.getSecretKey(), acct.getSecretKey());
        googleMfaAuthAccountService.deleteAccountBy(acct.getKey());
    }
}
