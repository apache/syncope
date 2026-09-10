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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides some methods to check whether default credentials are being used, and logs a warning if they are.
 */
public class DefaultCredentialChecker {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCredentialChecker.class);

    private static final String DEFAULT_AES_KEY_ERROR_MESSAGE =
            "The default AES key property is being used. "
            + "This must be changed to avoid a security breach!";

    private static final String DEFAULT_JWS_KEY_ERROR_MESSAGE =
            "The default JWKS key property is being used. "
            + "This must be changed to avoid a security breach!";

    private static final String DEFAULT_ADMIN_PASSWORD_ERROR_MESSAGE =
            "The default adminPassword property is being used. "
            + "This must be changed to avoid a security breach!";

    private static final String DEFAULT_ANON_KEY_ERROR_MESSAGE =
            "The default anonymousKey property is being used. "
            + "This must be changed to avoid a security breach!";

    private final boolean defaultAesKeyInUse;

    private final boolean defaultJwsKeyInUse;

    private final boolean defaultAdminPasswordInUse;

    private final boolean defaultAnonymousKeyInUse;

    private final boolean productionMode;

    public DefaultCredentialChecker(
            final String aesKey,
            final String jwsKey,
            final String adminPassword,
            final String anonymousKey,
            final boolean productionMode) throws IOException {

        try (InputStream in = getClass().getResourceAsStream("/META-INF/default-credentials.properties")) {
            Properties defaultCredentials = new Properties();
            defaultCredentials.load(in);
            defaultAesKeyInUse = defaultCredentials.getProperty("default.aesSecretKey").equals(aesKey);
            defaultJwsKeyInUse = defaultCredentials.getProperty("default.jwsKey").equals(jwsKey);
            defaultAdminPasswordInUse = defaultCredentials.getProperty("default.adminPassword").equals(adminPassword);
            defaultAnonymousKeyInUse = defaultCredentials.getProperty("default.anonymousKey").equals(anonymousKey);
        }

        this.productionMode = productionMode;
    }

    public void checkIsDefaultAESKeyInUse() {
        if (defaultAesKeyInUse) {
            if (productionMode) {
                throw new IllegalStateException(DEFAULT_AES_KEY_ERROR_MESSAGE);
            }
            LOG.warn(DEFAULT_AES_KEY_ERROR_MESSAGE);
        }
    }

    public void checkIsDefaultJWSKeyInUse() {
        if (defaultJwsKeyInUse) {
            if (productionMode) {
                throw new IllegalStateException(DEFAULT_JWS_KEY_ERROR_MESSAGE);
            }
            LOG.warn(DEFAULT_JWS_KEY_ERROR_MESSAGE);
        }
    }

    public void checkIsDefaultAdminPasswordInUse() {
        if (defaultAdminPasswordInUse) {
            if (productionMode) {
                throw new IllegalStateException(DEFAULT_ADMIN_PASSWORD_ERROR_MESSAGE);
            }
            LOG.warn(DEFAULT_ADMIN_PASSWORD_ERROR_MESSAGE);
        }
    }

    public void checkIsDefaultAnonymousKeyInUse() {
        if (defaultAnonymousKeyInUse) {
            if (productionMode) {
                throw new IllegalStateException(DEFAULT_ANON_KEY_ERROR_MESSAGE);
            }
            LOG.warn(DEFAULT_ANON_KEY_ERROR_MESSAGE);
        }
    }
}
