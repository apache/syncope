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
package org.apache.syncope.sra.security.saml2;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * @see org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository
 */
public class InMemoryReactiveRelyingPartyRegistrationRepository
        implements ReactiveRelyingPartyRegistrationRepository {

    private final Map<String, ExtendedRelyingPartyRegistration> byRegistrationId;

    public InMemoryReactiveRelyingPartyRegistrationRepository(final ExtendedRelyingPartyRegistration... registrations) {
        this(List.of(registrations));
    }

    private static Map<String, ExtendedRelyingPartyRegistration> createMappingToIdentityProvider(
            final Collection<ExtendedRelyingPartyRegistration> registrations) {

        Map<String, ExtendedRelyingPartyRegistration> result = new LinkedHashMap<>();
        registrations.forEach(rp -> {
            Assert.notNull(rp, "relying party collection cannot contain null values");
            String key = rp.getRelyingPartyRegistration().getRegistrationId();
            Assert.notNull(rp, "relying party identifier cannot be null");
            Assert.isNull(result.get(key), () -> "relying party duplicate identifier '" + key + "' detected.");
            result.put(key, rp);
        });
        return Collections.unmodifiableMap(result);
    }

    public InMemoryReactiveRelyingPartyRegistrationRepository(
            final Collection<ExtendedRelyingPartyRegistration> registrations) {

        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.byRegistrationId = createMappingToIdentityProvider(registrations);
    }

    @Override
    public Mono<RelyingPartyRegistration> findByRegistrationId(final String registrationId) {
        return findExtendedByRegistrationId(registrationId).
                flatMap(registration -> Mono.just(registration.getRelyingPartyRegistration()));
    }

    @Override
    public Mono<ExtendedRelyingPartyRegistration> findExtendedByRegistrationId(final String registrationId) {
        return Mono.justOrEmpty(this.byRegistrationId.get(registrationId));
    }
}
