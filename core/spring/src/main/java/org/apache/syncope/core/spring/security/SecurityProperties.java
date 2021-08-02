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

    private String anonymousUser;

    private String anonymousKey;

    private String jwtIssuer = "ApacheSyncope";

    private String jwsKey;

    private JWSAlgorithm jwsAlgorithm = JWSAlgorithm.HS512;

    private String secretKey;

    private Class<? extends PasswordGenerator> passwordGenerator = DefaultPasswordGenerator.class;

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

    public JWSAlgorithm getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    public void setJwsAlgorithm(final JWSAlgorithm jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public Class<? extends PasswordGenerator> getPasswordGenerator() {
        return passwordGenerator;
    }

    public void setPasswordGenerator(final Class<? extends PasswordGenerator> passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    public DigesterProperties getDigester() {
        return digester;
    }
}
