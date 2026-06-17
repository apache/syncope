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

import com.nimbusds.jose.JWSAlgorithm;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security")
public class SecurityProperties {

    public static class ThrottleProperties {

        private boolean enabled = true;

        private int maxAttempts = 5;

        private long windowSeconds = 60;

        private long lockSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(final int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(final long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public long getLockSeconds() {
            return lockSeconds;
        }

        public void setLockSeconds(final long lockSeconds) {
            this.lockSeconds = lockSeconds;
        }
    }

    public static class AuthenticationErrorProperties {

        public static final String DEFAULT_GENERIC_MESSAGE = "Authentication failed";

        private boolean exposeDetails = false;

        private String genericMessage = DEFAULT_GENERIC_MESSAGE;

        public boolean isExposeDetails() {
            return exposeDetails;
        }

        public void setExposeDetails(final boolean exposeDetails) {
            this.exposeDetails = exposeDetails;
        }

        public String getGenericMessage() {
            return genericMessage;
        }

        public void setGenericMessage(final String genericMessage) {
            this.genericMessage = genericMessage;
        }
    }

    public static class DigesterProperties {

        private int saltIterations = 1;

        private int saltSizeBytes = 8;

        private boolean invertPositionOfPlainSaltInEncryptionResults = true;

        private boolean invertPositionOfSaltInMessageBeforeDigesting = true;

        private boolean useLenientSaltSizeCheck = true;

        public int getSaltIterations() {
            return saltIterations;
        }

        public void setSaltIterations(final int saltIterations) {
            this.saltIterations = saltIterations;
        }

        public int getSaltSizeBytes() {
            return saltSizeBytes;
        }

        public void setSaltSizeBytes(final int saltSizeBytes) {
            this.saltSizeBytes = saltSizeBytes;
        }

        public boolean isInvertPositionOfPlainSaltInEncryptionResults() {
            return invertPositionOfPlainSaltInEncryptionResults;
        }

        public void setInvertPositionOfPlainSaltInEncryptionResults(
                final boolean invertPositionOfPlainSaltInEncryptionResults) {

            this.invertPositionOfPlainSaltInEncryptionResults = invertPositionOfPlainSaltInEncryptionResults;
        }

        public boolean isInvertPositionOfSaltInMessageBeforeDigesting() {
            return invertPositionOfSaltInMessageBeforeDigesting;
        }

        public void setInvertPositionOfSaltInMessageBeforeDigesting(
                final boolean invertPositionOfSaltInMessageBeforeDigesting) {

            this.invertPositionOfSaltInMessageBeforeDigesting = invertPositionOfSaltInMessageBeforeDigesting;
        }

        public boolean isUseLenientSaltSizeCheck() {
            return useLenientSaltSizeCheck;
        }

        public void setUseLenientSaltSizeCheck(final boolean useLenientSaltSizeCheck) {
            this.useLenientSaltSizeCheck = useLenientSaltSizeCheck;
        }
    }

    private String adminUser;

    private String adminPassword;

    private CipherAlgorithm adminPasswordAlgorithm;

    private String adminMfaSecret;

    private String anonymousUser;

    private String anonymousKey;

    private String jwtIssuer = "ApacheSyncope";

    private String jwsKey;

    private String jwsAlgorithm = JWSAlgorithm.HS512.getName();

    private String aesSecretKey;

    private String groovyBlacklist = "classpath:META-INF/groovy.blacklist";

    private final ThrottleProperties authenticationThrottle = new ThrottleProperties();

    private boolean passwordResetHideDetails = true;

    private final ThrottleProperties passwordResetThrottle = new ThrottleProperties();

    private final AuthenticationErrorProperties authenticationError = new AuthenticationErrorProperties();

    private final DigesterProperties digester = new DigesterProperties();

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(final String adminUser) {
        this.adminUser = adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public CipherAlgorithm getAdminPasswordAlgorithm() {
        return adminPasswordAlgorithm;
    }

    public void setAdminPasswordAlgorithm(final CipherAlgorithm adminPasswordAlgorithm) {
        this.adminPasswordAlgorithm = adminPasswordAlgorithm;
    }

    public String getAdminMfaSecret() {
        return adminMfaSecret;
    }

    public void setAdminMfaSecret(final String adminMfaSecret) {
        this.adminMfaSecret = adminMfaSecret;
    }

    public String getAnonymousUser() {
        return anonymousUser;
    }

    public void setAnonymousUser(final String anonymousUser) {
        this.anonymousUser = anonymousUser;
    }

    public String getAnonymousKey() {
        return anonymousKey;
    }

    public void setAnonymousKey(final String anonymousKey) {
        this.anonymousKey = anonymousKey;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(final String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public String getJwsKey() {
        return jwsKey;
    }

    public void setJwsKey(final String jwsKey) {
        this.jwsKey = jwsKey;
    }

    public String getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    public void setJwsAlgorithm(final String jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    public String getAesSecretKey() {
        return aesSecretKey;
    }

    public void setAesSecretKey(final String secretKey) {
        this.aesSecretKey = secretKey;
    }

    public String getGroovyBlacklist() {
        return groovyBlacklist;
    }

    public void setGroovyBlacklist(final String groovyBlacklist) {
        this.groovyBlacklist = groovyBlacklist;
    }

    public SecurityProperties.ThrottleProperties getAuthenticationThrottle() {
        return authenticationThrottle;
    }

    public boolean isPasswordResetHideDetails() {
        return passwordResetHideDetails;
    }

    public void setPasswordResetHideDetails(final boolean passwordResetHideDetails) {
        this.passwordResetHideDetails = passwordResetHideDetails;
    }

    public ThrottleProperties getPasswordResetThrottle() {
        return passwordResetThrottle;
    }

    public AuthenticationErrorProperties getAuthenticationError() {
        return authenticationError;
    }

    public DigesterProperties getDigester() {
        return digester;
    }
}
