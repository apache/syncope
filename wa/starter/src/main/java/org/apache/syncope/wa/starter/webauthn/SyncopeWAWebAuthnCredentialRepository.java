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

package org.apache.syncope.wa.starter.webauthn;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.webauthn.storage.BaseWebAuthnCredentialRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yubico.webauthn.data.CredentialRegistration;
import lombok.val;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.WebAuthnRegisteredAccount;
import org.apache.syncope.common.rest.api.service.wa.WebAuthnRegistrationService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class SyncopeWAWebAuthnCredentialRepository extends BaseWebAuthnCredentialRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWAWebAuthnCredentialRepository.class);

    private final WARestClient waRestClient;

    public SyncopeWAWebAuthnCredentialRepository(final CasConfigurationProperties properties,
                                                 final WARestClient waRestClient) {
        super(properties, CipherExecutor.noOpOfStringToString());
        this.waRestClient = waRestClient;
    }

    @Override
    public boolean removeRegistrationByUsername(final String username,
                                                final CredentialRegistration credentialRegistration) {
        val id = credentialRegistration.getCredential().getCredentialId().getHex();
        getService().delete(username, id);
        return true;
    }

    @Override
    public boolean removeAllRegistrations(final String username) {
        getService().delete(username);
        return true;
    }

    @Override
    protected Stream<CredentialRegistration> load() {
        return getService().list().
            getResult().
            stream().
            map(record -> getCipherExecutor().decode(record.getRecords())).
            map(Unchecked.function(record -> getObjectMapper().
                readValue(record, new TypeReference<Set<CredentialRegistration>>() {
            })))
            .flatMap(Collection::stream);
    }

    @Override
    protected void update(final String username, final Collection<CredentialRegistration> records) {
        try {
            val jsonRecords = getCipherExecutor().encode(getObjectMapper().writeValueAsString(records));
            WebAuthnRegisteredAccount account = getService().findAccountFor(username);
            if (account != null) {
                account.setRecords(jsonRecords);
                getService().update(account);
            } else {
                account = new WebAuthnRegisteredAccount.Builder()
                    .owner(username)
                    .records(jsonRecords)
                    .build();
                getService().create(account);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public Collection<CredentialRegistration> getRegistrationsByUsername(final String username) {
        try {
            WebAuthnRegisteredAccount account = getService().findAccountFor(username);
            if (account != null) {
                return getObjectMapper().readValue(getCipherExecutor().decode(account.getRecords()),
                    new TypeReference<Set<CredentialRegistration>>() {
                    });
            }
        } catch (final SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.info("Could not locate account for {}", username);
            } else {
                LOG.error(e.getMessage(), e);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return List.of();
    }

    private WebAuthnRegistrationService getService() {
        if (!WARestClient.isReady()) {
            throw new RuntimeException("Syncope core is not yet ready");
        }
        return waRestClient.getSyncopeClient().getService(WebAuthnRegistrationService.class);
    }
}
