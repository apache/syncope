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

import org.apache.syncope.common.lib.types.WebAuthnDeviceCredential;
import org.apache.syncope.common.lib.types.WebAuthnRegisteredAccount;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebAuthnRegisteredAccountITCase extends AbstractITCase {

    private static WebAuthnRegisteredAccount createWebAuthnRegisteredAccount() {
        String id = SecureRandomUtils.generateRandomUUID().toString();
        String record = "[ {" +
            "    \"userIdentity\" : {" +
            "      \"name\" : \"%s\"," +
            "      \"displayName\" : \"%s\"" +
            "    }," +
            "    \"credential\" : {" +
            "      \"credentialId\" : \"fFGyV3K5x1\"" +
            "    }," +
            "    \"username\" : \"%s\"" +
            "  } ]";
        WebAuthnDeviceCredential credential = new WebAuthnDeviceCredential.Builder().
            json(String.format(record, id, id, id)).
            owner(id).
            identifier("fFGyV3K5x1").
            build();
        return new WebAuthnRegisteredAccount.Builder()
            .owner(id)
            .records(List.of(credential))
            .build();
    }

    @BeforeEach
    public void setup() {
        webAuthnRegistrationService.deleteAll();
    }

    @Test
    public void listAndFind() {
        WebAuthnRegisteredAccount acct = createWebAuthnRegisteredAccount();
        webAuthnRegistrationService.create(acct);
        assertTrue(webAuthnRegistrationService.list().getSize() > 0);
        assertNotNull(webAuthnRegistrationService.findAccountFor(acct.getOwner()));
    }

    @Test
    public void deleteByOwner() {
        WebAuthnRegisteredAccount acct = createWebAuthnRegisteredAccount();
        webAuthnRegistrationService.create(acct);
        assertNotNull(webAuthnRegistrationService.delete(acct.getOwner()));
        assertEquals(0, webAuthnRegistrationService.list().getSize());
    }

    @Test
    public void deleteByAcct() {
        WebAuthnRegisteredAccount acct = createWebAuthnRegisteredAccount();
        webAuthnRegistrationService.create(acct);
        assertNotNull(webAuthnRegistrationService.delete(acct.getOwner(), acct.getRecords().get(0).getIdentifier()));
        acct = webAuthnRegistrationService.findAccountFor(acct.getOwner());
        assertTrue(acct.getRecords().isEmpty());
    }

    @Test
    public void create() {
        WebAuthnRegisteredAccount acct = createWebAuthnRegisteredAccount();
        assertDoesNotThrow(() -> {
            Response response = webAuthnRegistrationService.create(acct);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
                if (ex != null) {
                    throw ex;
                }
            }
        });
    }
}
