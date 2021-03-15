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

import com.warrenstrange.googleauth.IGoogleAuthenticator;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthAccountService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.gauth.credential.BaseGoogleAuthenticatorTokenCredentialRepository;
import org.apereo.cas.gauth.credential.GoogleAuthenticatorAccount;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeWAGoogleMfaAuthCredentialRepository extends BaseGoogleAuthenticatorTokenCredentialRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAGoogleMfaAuthTokenRepository.class);

    private final WARestClient waRestClient;

    public SyncopeWAGoogleMfaAuthCredentialRepository(
            final WARestClient waRestClient, final IGoogleAuthenticator googleAuthenticator) {

        super(CipherExecutor.noOpOfStringToString(), googleAuthenticator);
        this.waRestClient = waRestClient;
    }

    private static GoogleMfaAuthAccount mapGoogleMfaAuthAccount(final OneTimeTokenAccount account) {
        return new GoogleMfaAuthAccount.Builder()
                .registrationDate(new Date())
                .scratchCodes(account.getScratchCodes())
                .validationCode(account.getValidationCode())
                .secretKey(account.getSecretKey())
                .id(account.getId())
                .build();
    }

    private static GoogleAuthenticatorAccount mapGoogleMfaAuthAccount(final GoogleMfaAuthAccount account) {
        return GoogleAuthenticatorAccount.builder().
                secretKey(account.getSecretKey()).
                validationCode(account.getValidationCode()).
                scratchCodes(account.getScratchCodes()).
                name(account.getName()).
                id(account.getId()).
                build();
    }

    @Override
    public OneTimeTokenAccount get(final long id) {
        try {
            GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                    getService(GoogleMfaAuthAccountService.class);
            GoogleMfaAuthAccount account = googleService.read(id);
            if (account != null) {
                return mapGoogleMfaAuthAccount(account);
            }
        } catch (final SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.info("Could not locate account for id {}", id);
            } else {
                LOG.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public OneTimeTokenAccount get(final String username, final long id) {
        try {
            GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                    getService(GoogleMfaAuthAccountService.class);
            googleService.readFor(username).getResult().stream().
                    filter(account -> account.getId() == id).
                    map(SyncopeWAGoogleMfaAuthCredentialRepository::mapGoogleMfaAuthAccount).
                    collect(Collectors.toList());
        } catch (final SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.info("Could not locate account for owner {} and id {}", username, id);
            } else {
                LOG.error(e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public Collection<? extends OneTimeTokenAccount> get(final String username) {
        try {
            GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                    getService(GoogleMfaAuthAccountService.class);
            googleService.readFor(username).getResult().stream().
                    map(SyncopeWAGoogleMfaAuthCredentialRepository::mapGoogleMfaAuthAccount).
                    collect(Collectors.toList());
        } catch (final SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.info("Could not locate account for owner {}", username);
            } else {
                LOG.error(e.getMessage(), e);
            }
        }
        return List.of();
    }

    @Override
    public Collection<? extends OneTimeTokenAccount> load() {
        GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                getService(GoogleMfaAuthAccountService.class);
        return googleService.list().getResult().stream().
                map(SyncopeWAGoogleMfaAuthCredentialRepository::mapGoogleMfaAuthAccount).
                collect(Collectors.toList());
    }

    @Override
    public OneTimeTokenAccount save(final OneTimeTokenAccount tokenAccount) {
        GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                getService(GoogleMfaAuthAccountService.class);
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount.Builder()
                .registrationDate(new Date())
                .scratchCodes(tokenAccount.getScratchCodes())
                .validationCode(tokenAccount.getValidationCode())
                .secretKey(tokenAccount.getSecretKey())
                .name(tokenAccount.getName())
                .id(tokenAccount.getId())
                .build();
        Response response = googleService.create(tokenAccount.getUsername(), account);
        String key = response.getHeaderString(RESTHeaders.RESOURCE_KEY);
        account.setKey(key);
        return mapGoogleMfaAuthAccount(account);
    }

    @Override
    public OneTimeTokenAccount update(final OneTimeTokenAccount tokenAccount) {
        GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                getService(GoogleMfaAuthAccountService.class);
        GoogleMfaAuthAccount acct = mapGoogleMfaAuthAccount(tokenAccount);
        googleService.update(tokenAccount.getUsername(), acct);
        return tokenAccount;
    }

    @Override
    public void deleteAll() {
        GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                getService(GoogleMfaAuthAccountService.class);
        googleService.delete();
    }

    @Override
    public void delete(final String username) {
        GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                getService(GoogleMfaAuthAccountService.class);
        googleService.deleteFor(username);
    }

    @Override
    public long count() {
        GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                getService(GoogleMfaAuthAccountService.class);
        return googleService.list().getTotalCount();
    }

    @Override
    public long count(final String username) {
        GoogleMfaAuthAccountService googleService = waRestClient.getSyncopeClient().
                getService(GoogleMfaAuthAccountService.class);
        return googleService.readFor(username).getTotalCount();
    }
}
