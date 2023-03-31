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
package org.apache.syncope.wa.starter.mapping;

import org.apereo.cas.services.RegisteredServiceAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceDelegatedAuthenticationPolicy;
import org.apereo.cas.services.RegisteredServiceMultifactorPolicy;

public class AuthMapperResult {

    public static final AuthMapperResult EMPTY = new AuthMapperResult(null, null, null);

    private final RegisteredServiceAuthenticationPolicy authPolicy;

    private final RegisteredServiceMultifactorPolicy mfaPolicy;

    private final RegisteredServiceDelegatedAuthenticationPolicy delegateAuthPolicy;

    public AuthMapperResult(
            final RegisteredServiceAuthenticationPolicy authPolicy,
            final RegisteredServiceMultifactorPolicy mfaPolicy,
            final RegisteredServiceDelegatedAuthenticationPolicy delegateAuthPolicy) {

        this.authPolicy = authPolicy;
        this.mfaPolicy = mfaPolicy;
        this.delegateAuthPolicy = delegateAuthPolicy;
    }

    public RegisteredServiceAuthenticationPolicy getAuthPolicy() {
        return authPolicy;
    }

    public RegisteredServiceMultifactorPolicy getMfaPolicy() {
        return mfaPolicy;
    }

    public RegisteredServiceDelegatedAuthenticationPolicy getDelegateAuthPolicy() {
        return delegateAuthPolicy;
    }
}
