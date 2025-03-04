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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.apache.syncope.common.lib.wa.WebAuthnAccount;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class WebAuthnAccountITCase extends AbstractITCase {

    private static WebAuthnAccount createWebAuthnRegisteredAccount() {
        String id = SecureRandomUtils.generateRandomUUID().toString();
        String record = "[ {"
                + "    \"userIdentity\" : {"
                + "      \"name\" : \"%s\","
                + "      \"displayName\" : \"%s\""
                + "    },"
                + "    \"credential\" : {"
                + "      \"credentialId\" : \"fFGyV3K5x1\""
                + "    },"
                + "    \"username\" : \"%s\""
                + "  } ]";
        WebAuthnDeviceCredential credential = new WebAuthnDeviceCredential.Builder().
                json(String.format(record, id, id, id)).
                identifier("fFGyV3K5x1").
                build();
        return new WebAuthnAccount.Builder().credential(credential).build();
    }

    @Test
    public void listAndFind() {
        String owner = UUID.randomUUID().toString();
        WebAuthnAccount acct = createWebAuthnRegisteredAccount();
        WEBAUTHN_REGISTRATION_SERVICE.create(owner, acct);
        assertFalse(WEBAUTHN_REGISTRATION_SERVICE.list().isEmpty());
        assertNotNull(WEBAUTHN_REGISTRATION_SERVICE.read(owner));
    }

    @Test
    public void deleteByOwner() {
        String owner = UUID.randomUUID().toString();
        WebAuthnAccount acct = createWebAuthnRegisteredAccount();
        WEBAUTHN_REGISTRATION_SERVICE.create(owner, acct);
        WEBAUTHN_REGISTRATION_SERVICE.delete(owner);
        assertTrue(WEBAUTHN_REGISTRATION_SERVICE.read(owner).getCredentials().isEmpty());
    }

    @Test
    public void deleteByAcct() {
        String owner = UUID.randomUUID().toString();
        WebAuthnAccount acct = createWebAuthnRegisteredAccount();
        WEBAUTHN_REGISTRATION_SERVICE.create(owner, acct);
        WEBAUTHN_REGISTRATION_SERVICE.delete(owner, acct.getCredentials().getFirst().getIdentifier());
        acct = WEBAUTHN_REGISTRATION_SERVICE.read(owner);
        assertTrue(acct.getCredentials().isEmpty());
    }

    @Test
    public void create() {
        WebAuthnAccount acct = createWebAuthnRegisteredAccount();
        assertDoesNotThrow(() -> WEBAUTHN_REGISTRATION_SERVICE.create(UUID.randomUUID().toString(), acct));
    }
}
