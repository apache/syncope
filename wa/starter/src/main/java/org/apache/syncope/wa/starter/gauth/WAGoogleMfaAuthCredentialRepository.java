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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthAccountService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.gauth.credential.BaseGoogleAuthenticatorTokenCredentialRepository;
import org.apereo.cas.gauth.credential.GoogleAuthenticatorAccount;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WAGoogleMfaAuthCredentialRepository extends BaseGoogleAuthenticatorTokenCredentialRepository {

    protected static final Logger LOG = LoggerFactory.getLogger(WAGoogleMfaAuthTokenRepository.class);

    protected final WARestClient waRestClient;

    public WAGoogleMfaAuthCredentialRepository(
            final WARestClient waRestClient, final IGoogleAuthenticator googleAuthenticator) {

        super(CipherExecutor.noOpOfStringToString(), CipherExecutor.noOpOfNumberToNumber(), googleAuthenticator);
        this.waRestClient = waRestClient;
    }

    protected GoogleMfaAuthAccount mapGoogleMfaAuthAccount(final OneTimeTokenAccount otta) {
        return new GoogleMfaAuthAccount.Builder().
                registrationDate(OffsetDateTime.now()).
                scratchCodes(otta.getScratchCodes().stream().map(Number::intValue).toList()).
                validationCode(otta.getValidationCode()).
                secretKey(otta.getSecretKey()).
                id(otta.getId()).
                build();
    }

    protected GoogleAuthenticatorAccount mapGoogleMfaAuthAccount(final GoogleMfaAuthAccount gmfaa) {
        return GoogleAuthenticatorAccount.builder().
                secretKey(gmfaa.getSecretKey()).
                validationCode(gmfaa.getValidationCode()).
                scratchCodes(gmfaa.getScratchCodes().stream().map(Number::intValue).collect(Collectors.toList())).
                name(gmfaa.getName()).
                id(gmfaa.getId()).
                build();
    }

    protected GoogleMfaAuthAccountService service() {
        return waRestClient.getService(GoogleMfaAuthAccountService.class);
    }

    @Override
    public OneTimeTokenAccount get(final long id) {
        try {
            GoogleMfaAuthAccount account = service().read(id);
            if (account != null) {
                return mapGoogleMfaAuthAccount(account);
            }
        } catch (SyncopeClientException e) {
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
            return service().read(username).
                    getResult().stream().
                    filter(account -> account.getId() == id).
                    map(this::mapGoogleMfaAuthAccount).
                    findFirst().
                    orElse(null);
        } catch (SyncopeClientException e) {
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
            return service().read(username).
                    getResult().stream().
                    map(this::mapGoogleMfaAuthAccount).
                    toList();
        } catch (SyncopeClientException e) {
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
        return service().list().
                getResult().stream().
                map(this::mapGoogleMfaAuthAccount).
                toList();
    }

    @Override
    public OneTimeTokenAccount save(final OneTimeTokenAccount otta) {
        GoogleMfaAuthAccount account = new GoogleMfaAuthAccount.Builder().
                registrationDate(OffsetDateTime.now()).
                scratchCodes(otta.getScratchCodes().stream().map(Number::intValue).toList()).
                validationCode(otta.getValidationCode()).
                secretKey(otta.getSecretKey()).
                name(otta.getName()).
                id(otta.getId()).
                build();
        service().create(otta.getUsername(), account);
        return mapGoogleMfaAuthAccount(account);
    }

    @Override
    public OneTimeTokenAccount update(final OneTimeTokenAccount tokenAccount) {
        GoogleMfaAuthAccount acct = mapGoogleMfaAuthAccount(tokenAccount);
        service().update(tokenAccount.getUsername(), acct);
        return tokenAccount;
    }

    @Override
    public void deleteAll() {
        service().deleteAll();
    }

    @Override
    public void delete(final String username) {
        try {
            service().delete(username);
        } catch (SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.info("Could not locate account for owner {}", username);
            } else {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void delete(final long id) {
        service().delete(id);
    }

    @Override
    public long count() {
        return service().list().getTotalCount();
    }

    @Override
    public long count(final String username) {
        try {
            return service().read(username).getTotalCount();
        } catch (SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.info("Could not locate account for owner {}", username);
            } else {
                LOG.error(e.getMessage(), e);
            }
            return 0L;
        }
    }
}
