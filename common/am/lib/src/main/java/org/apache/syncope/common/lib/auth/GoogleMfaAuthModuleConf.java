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
package org.apache.syncope.common.lib.auth;

import java.io.Serializable;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AbstractLDAPConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.OIDCTokenEncryptionEncoding;

public class GoogleMfaAuthModuleConf implements MFAAuthModuleConf, LDAPDependantAuthModuleConf {

    private static final long serialVersionUID = -7883257599139312426L;

    public enum CryptoStrategy {
        /**
         * Encrypt the value first, and then sign.
         */
        ENCRYPT_AND_SIGN,
        /**
         * Sign the value first, and then encrypt.
         */
        SIGN_AND_ENCRYPT;

    }

    public static class LDAP extends AbstractLDAPConf implements Serializable {

        private static final long serialVersionUID = -7274446267090678730L;

        /**
         * Name of LDAP attribute that holds GAuth account/credential as JSON.
         */
        private String accountAttributeName = "casGAuthRecord";

        public String getAccountAttributeName() {
            return accountAttributeName;
        }

        public void setAccountAttributeName(final String accountAttributeName) {
            this.accountAttributeName = accountAttributeName;
        }
    }

    /**
     * Issuer used in the barcode when dealing with device registration events.
     * Used in the registration URL to identify CAS.
     */
    private String issuer = "Syncope";

    /**
     * Label used in the barcode when dealing with device registration events.
     * Used in the registration URL to identify CAS.
     */
    private String label = "Syncope";

    /**
     * Length of the generated code.
     */
    private int codeDigits = 6;

    /**
     * The expiration time of the generated code in seconds.
     */
    private long timeStepSize = 30;

    /**
     * Since TOTP passwords are time-based, it is essential that
     * the clock of both the server and
     * the client are synchronised within
     * the tolerance defined here as the window size.
     */
    private int windowSize = 3;

    /**
     * Whether crypto operations are enabled.
     */
    private boolean enableCrypto = true;

    /**
     * The signing/encryption algorithm to use.
     */
    private OIDCTokenEncryptionEncoding cryptoAlgorithm = OIDCTokenEncryptionEncoding.A256CBC_HS512;

    /**
     * Control the cipher sequence of operations.
     */
    private CryptoStrategy cryptoStrategy = CryptoStrategy.ENCRYPT_AND_SIGN;

    /**
     * The signing key size.
     */
    private int signingKeySize = 512;

    /**
     * The signing key is a JWT whose length is defined by the signing key size setting.
     */
    private String signingKey = StringUtils.EMPTY;

    /**
     * The encryption key size.
     */
    private int encryptionKeySize = 512;

    /**
     * The encryption key is a JWT whose length is defined by the encryption key size setting.
     */
    private String encryptionKey = StringUtils.EMPTY;

    private LDAP ldap;

    @Override
    public AbstractLDAPConf ldapInstance() {
        return new GoogleMfaAuthModuleConf.LDAP();
    }

    @Override
    public String getFriendlyName() {
        return "Google Authenticator";
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public int getCodeDigits() {
        return codeDigits;
    }

    public void setCodeDigits(final int codeDigits) {
        this.codeDigits = codeDigits;
    }

    public long getTimeStepSize() {
        return timeStepSize;
    }

    public void setTimeStepSize(final long timeStepSize) {
        this.timeStepSize = timeStepSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(final int windowSize) {
        this.windowSize = windowSize;
    }

    public boolean isEnableCrypto() {
        return enableCrypto;
    }

    public void setEnableCrypto(final boolean enableCrypto) {
        this.enableCrypto = enableCrypto;
    }

    public OIDCTokenEncryptionEncoding getCryptoAlgorithm() {
        return cryptoAlgorithm;
    }

    public void setCryptoAlgorithm(final OIDCTokenEncryptionEncoding cryptoAlgorithm) {
        this.cryptoAlgorithm = cryptoAlgorithm;
    }

    public CryptoStrategy getCryptoStrategy() {
        return cryptoStrategy;
    }

    public void setCryptoStrategy(final CryptoStrategy cryptoStrategy) {
        this.cryptoStrategy = cryptoStrategy;
    }

    public int getSigningKeySize() {
        return signingKeySize;
    }

    public void setSigningKeySize(final int signingKeySize) {
        this.signingKeySize = signingKeySize;
    }

    public String getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(final String signingKey) {
        this.signingKey = signingKey;
    }

    public int getEncryptionKeySize() {
        return encryptionKeySize;
    }

    public void setEncryptionKeySize(final int encryptionKeySize) {
        this.encryptionKeySize = encryptionKeySize;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(final String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public LDAP getLdap() {
        return ldap;
    }

    public void setLdap(final LDAP ldap) {
        this.ldap = ldap;
    }

    @Override
    public Map<String, Object> map(final AuthModuleTO authModule, final Mapper mapper) {
        return mapper.map(authModule, this);
    }
}
