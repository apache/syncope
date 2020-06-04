/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.core;

import org.apache.syncope.common.lib.types.GoogleMfaAuthAccount;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GoogleMfaAuthAccountITCase extends AbstractITCase {

    private static GoogleMfaAuthAccount createGoogleMfaAuthAccount() {
        String id = UUID.randomUUID().toString();
        return new GoogleMfaAuthAccount.Builder()
            .registrationDate(new Date())
            .scratchCodes(List.of(1, 2, 3, 4, 5))
            .secretKey(UUID.randomUUID().toString())
            .validationCode(123456)
            .owner(id)
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
        assertEquals(1, googleMfaAuthAccountService.countAll());
        assertEquals(1, googleMfaAuthAccountService.findAccountFor(acct.getOwner()));
    }

    @Test
    public void deleteByOwner() {
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        Response response = googleMfaAuthAccountService.save(acct);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        assertNotNull(key);
        response = googleMfaAuthAccountService.deleteAccountFor(acct.getOwner());
        assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode());
        assertThrows(NotFoundException.class, () -> googleMfaAuthAccountService.findAccountFor(acct.getOwner()));
    }

    @Test
    public void update() {
        GoogleMfaAuthAccount acct = createGoogleMfaAuthAccount();
        googleMfaAuthAccountService.save(acct);
        assertEquals(1, googleMfaAuthAccountService.countAll());
        acct.setOwner("NewOwner");
        acct.setScratchCodes(List.of(9, 8, 7, 6, 5));
        googleMfaAuthAccountService.update(acct);
        assertEquals(1, googleMfaAuthAccountService.countAll());
        assertEquals(1, googleMfaAuthAccountService.findAccountFor(acct.getOwner()));
        googleMfaAuthAccountService.deleteAccountBy(acct.getKey());
    }
}
