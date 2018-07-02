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
package org.apache.syncope.core.spring.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides some methods to check whether default credentials are being used, and logs a warning if they are.
 */
public class DefaultCredentialChecker {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCredentialChecker.class);

    private static final String DEFAULT_JWS_KEY = "ZW7pRixehFuNUtnY5Se47IemgMryTzazPPJ9CGX5LTCmsOJpOgHAQEuPQeV9A28f";

    private static final String DEFAULT_ADMIN_PASSWORD =
        "DE088591C00CC98B36F5ADAAF7DA2B004CF7F2FE7BBB45B766B6409876E2F3DB13C7905C6AA59464";

    private static final String DEFAULT_ANON_KEY = "anonymousKey";

    private final boolean defaultAdminPasswordInUse;

    private final boolean defaultJwsKeyInUse;

    private final boolean defaultAnonymousKeyInUse;

    public DefaultCredentialChecker(final String jwsKey, final String adminPassword, final String anonymousKey) {
        defaultJwsKeyInUse = DEFAULT_JWS_KEY.equals(jwsKey);
        defaultAdminPasswordInUse = DEFAULT_ADMIN_PASSWORD.equals(adminPassword);
        defaultAnonymousKeyInUse = DEFAULT_ANON_KEY.equals(anonymousKey);
    }

    public void checkIsDefaultJWSKeyInUse() {
        if (defaultJwsKeyInUse) {
            LOG.warn("The default jwsKey property is being used. "
                    + "This must be changed to avoid a security breach!");
        }
    }

    public void checkIsDefaultAdminPasswordInUse() {
        if (defaultAdminPasswordInUse) {
            LOG.warn("The default adminPassword property is being used. "
                    + "This must be changed to avoid a security breach!");
        }
    }

    public void checkIsDefaultAnonymousKeyInUse() {
        if (defaultAnonymousKeyInUse) {
            LOG.warn("The default anonymousKey property is being used. "
                    + "This must be changed to avoid a security breach!");
        }
    }
}
