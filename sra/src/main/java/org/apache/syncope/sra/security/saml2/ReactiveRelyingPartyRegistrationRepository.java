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

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import reactor.core.publisher.Mono;

/**
 * Resolves a {@link RelyingPartyRegistration}, a configured service provider and remote identity provider pair
 * based on a unique registrationId.
 *
 * @see org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
 */
public interface ReactiveRelyingPartyRegistrationRepository {

    /**
     * Resolves an {@link RelyingPartyRegistration} by registrationId, or returns the default provider
     * if no registrationId is provided
     *
     * @param registrationId - a provided registrationId, may be be null or empty
     * @return {@link RelyingPartyRegistration} if found, {@code null} if an registrationId is provided and
     * no registration is found. Returns a default, implementation specific,
     * {@link RelyingPartyRegistration} if no registrationId is provided
     */
    Mono<RelyingPartyRegistration> findByRegistrationId(String registrationId);

    Mono<ExtendedRelyingPartyRegistration> findExtendedByRegistrationId(String registrationId);
}
