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

import java.time.LocalDateTime;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthToken;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.gauth.token.GoogleAuthenticatorToken;
import org.apereo.cas.otp.repository.token.BaseOneTimeTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAGoogleMfaAuthTokenRepository extends BaseOneTimeTokenRepository<GoogleAuthenticatorToken> {

    protected static final Logger LOG = LoggerFactory.getLogger(WAGoogleMfaAuthTokenRepository.class);

    protected final WARestClient waRestClient;

    protected final long expireTokensInSeconds;

    public WAGoogleMfaAuthTokenRepository(final WARestClient waRestClient, final long expireTokensInSeconds) {
        this.waRestClient = waRestClient;
        this.expireTokensInSeconds = expireTokensInSeconds;
    }

    protected GoogleMfaAuthTokenService service() {
        return waRestClient.getService(GoogleMfaAuthTokenService.class);
    }

    @Override
    protected void cleanInternal() {
        service().delete(LocalDateTime.now().minusSeconds(expireTokensInSeconds));
    }

    @Override
    public GoogleAuthenticatorToken store(final GoogleAuthenticatorToken token) {
        GoogleMfaAuthToken tokenTO = new GoogleMfaAuthToken.Builder().
                token(token.getToken()).
                issueDate(token.getIssuedDateTime()).
                build();
        service().store(token.getUserId(), tokenTO);
        return token;
    }

    @Override
    public GoogleAuthenticatorToken get(final String username, final Integer otp) {
        try {
            GoogleMfaAuthToken tokenTO = service().read(username, otp);
            GoogleAuthenticatorToken token = new GoogleAuthenticatorToken(tokenTO.getOtp(), username);
            token.setIssuedDateTime(tokenTO.getIssueDate());
            return token;
        } catch (final Exception e) {
            LOG.debug("Unable to fetch token {} for user {}", otp, username);
        }
        return null;
    }

    @Override
    public void remove(final String username, final Integer otp) {
        service().delete(username, otp);
    }

    @Override
    public void remove(final String username) {
        service().delete(username);
    }

    @Override
    public void remove(final Integer otp) {
        service().delete(otp);
    }

    @Override
    public void removeAll() {
        service().delete((LocalDateTime) null);
    }

    @Override
    public long count(final String username) {
        return service().read(username).getTotalCount();
    }

    @Override
    public long count() {
        return service().list().getTotalCount();
    }
}
