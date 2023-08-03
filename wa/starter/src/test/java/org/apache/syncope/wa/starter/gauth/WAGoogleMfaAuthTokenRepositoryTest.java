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
package org.apache.syncope.wa.starter.gauth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.syncope.wa.starter.AbstractTest;
import org.apereo.cas.gauth.token.GoogleAuthenticatorToken;
import org.apereo.cas.otp.repository.token.OneTimeTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class WAGoogleMfaAuthTokenRepositoryTest extends AbstractTest {

    @Autowired
    private OneTimeTokenRepository<GoogleAuthenticatorToken> tokenRepository;

    @Test
    public void verifyOps() {
        tokenRepository.removeAll();
        GoogleAuthenticatorToken token = new GoogleAuthenticatorToken(123456, "SyncopeWA");
        tokenRepository.store(token);
        assertEquals(1, tokenRepository.count(token.getUserId()));
        assertEquals(1, tokenRepository.count());
    }
}
