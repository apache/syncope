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

import com.fasterxml.jackson.core.type.TypeReference;
import com.yubico.data.CredentialRegistration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.wa.WebAuthnAccount;
import org.apache.syncope.common.lib.wa.WebAuthnDeviceCredential;
import org.apache.syncope.common.rest.api.service.wa.WebAuthnRegistrationService;
import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.webauthn.WebAuthnUtils;
import org.apereo.cas.webauthn.storage.BaseWebAuthnCredentialRepository;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String id = credentialRegistration.getCredential().getCredentialId().getHex();
        getService().delete(username, id);
        return true;
    }

    @Override
    public boolean removeAllRegistrations(final String username) {
        getService().delete(username);
        return true;
    }

    @Override
    public Stream<? extends CredentialRegistration> stream() {
        return getService().list().
                stream().
                map(WebAuthnAccount::getCredentials).
                flatMap(Collection::stream).
                map(Unchecked.function(record -> {
                    String json = getCipherExecutor().decode(record.getJson());
                    return WebAuthnUtils.getObjectMapper().readValue(json, new TypeReference<>() {
                    });
                }));
    }

    @Override
    protected void update(final String username, final Collection<CredentialRegistration> records) {
        try {
            List<WebAuthnDeviceCredential> credentials = records.stream().
                    map(Unchecked.function(record -> {
                        String json = getCipherExecutor().encode(WebAuthnUtils.getObjectMapper().
                                writeValueAsString(record));
                        return new WebAuthnDeviceCredential.Builder().
                                json(json).
                                identifier(record.getCredential().getCredentialId().getHex()).
                                build();
                    })).
                    collect(Collectors.toList());

            WebAuthnAccount account = getService().read(username);
            if (account != null) {
                account.getCredentials().addAll(credentials);
                getService().update(username, account);
            } else {
                account = new WebAuthnAccount.Builder().credentials(credentials).build();
                getService().create(username, account);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public Collection<CredentialRegistration> getRegistrationsByUsername(final String username) {
        try {
            return getService().read(username).getCredentials().stream().
                    map(Unchecked.function(record -> {
                        String json = getCipherExecutor().decode(record.getJson());
                        return WebAuthnUtils.getObjectMapper()
                                .readValue(json, new TypeReference<CredentialRegistration>() {
                                });
                    })).
                    collect(Collectors.toList());
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
            throw new IllegalStateException("Syncope core is not yet ready");
        }
        return waRestClient.getSyncopeClient().getService(WebAuthnRegistrationService.class);
    }
}
