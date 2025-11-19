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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.jasypt.commons.CommonUtils;
import org.jasypt.digest.StandardStringDigester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;

public final class Encryptor {

    private static final Logger LOG = LoggerFactory.getLogger(Encryptor.class);

    private static final Map<String, Encryptor> INSTANCES = new ConcurrentHashMap<>();

    public static Encryptor getInstance() {
        return getInstance(null);
    }

    public static Encryptor getInstance(final String aesSecretKey) {
        SecurityProperties securityProperties = Optional.ofNullable(ApplicationContextProvider.getApplicationContext()).
                flatMap(ctx -> {
                    try {
                        return Optional.ofNullable(ctx.getBean(SecurityProperties.class));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                }).
                orElseGet(() -> {
                    SecurityProperties props = new SecurityProperties();
                    props.setAesSecretKey(StringUtils.EMPTY);
                    return props;
                });

        String actualKey = StringUtils.isBlank(aesSecretKey) ? securityProperties.getAesSecretKey() : aesSecretKey;
        return INSTANCES.computeIfAbsent(actualKey, k -> new Encryptor(k, securityProperties.getDigester()));
    }

    private final SecurityProperties.DigesterProperties digesterProperties;

    private final Map<CipherAlgorithm, StandardStringDigester> digesters = new ConcurrentHashMap<>();

    private final Optional<SecretKeySpec> aesKeySpec;

    private Encryptor(
            final String aesSecretKey,
            final SecurityProperties.DigesterProperties digesterProperties) {

        this.digesterProperties = digesterProperties;

        SecretKeySpec sks = null;

        if (StringUtils.isNotBlank(aesSecretKey)) {
            String actualKey = aesSecretKey;

            Integer pad = null;
            boolean truncate = false;
            if (actualKey.length() < 16) {
                pad = 16 - actualKey.length();
            } else if (actualKey.length() > 16 && actualKey.length() < 24) {
                pad = 24 - actualKey.length();
            } else if (actualKey.length() > 24 && actualKey.length() < 32) {
                pad = 32 - actualKey.length();
            } else if (actualKey.length() > 32) {
                truncate = true;
            }

            if (pad != null) {
                StringBuilder actualKeyPadding = new StringBuilder(actualKey);
                String randomChars = SecureRandomUtils.generateRandomPassword(pad);

                actualKeyPadding.append(randomChars);
                actualKey = actualKeyPadding.toString();
                LOG.warn("The configured AES secret key is too short (< {}), padding with random chars: {}",
                        actualKey.length(), actualKey);
            }
            if (truncate) {
                actualKey = actualKey.substring(0, 32);
                LOG.warn("The configured AES secret key is too long (> 32), truncating: {}", actualKey);
            }

            try {
                sks = new SecretKeySpec(actualKey.getBytes(StandardCharsets.UTF_8), CipherAlgorithm.AES.getAlgorithm());
                LOG.debug("AES-{} successfully configured", actualKey.length() * 8);
            } catch (Exception e) {
                LOG.error("Error during key specification", e);
            }
        }

        aesKeySpec = Optional.ofNullable(sks);
    }

    public String encode(final String value, final CipherAlgorithm cipherAlgorithm)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        String encoded = null;

        if (value != null) {
            if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {
                Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, aesKeySpec.
                        orElseThrow(() -> new IllegalArgumentException("AES not configured")));

                encoded = Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
            } else if (cipherAlgorithm == CipherAlgorithm.BCRYPT) {
                encoded = BCrypt.hashpw(value, BCrypt.gensalt());
            } else {
                encoded = getDigester(cipherAlgorithm).digest(value);
            }
        }

        return encoded;
    }

    public boolean verify(final String value, final CipherAlgorithm cipherAlgorithm, final String encoded) {
        boolean verified = false;

        try {
            if (value != null) {
                if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {
                    verified = encode(value, cipherAlgorithm).equals(encoded);
                } else if (cipherAlgorithm == CipherAlgorithm.BCRYPT) {
                    verified = BCrypt.checkpw(value, encoded);
                } else {
                    verified = getDigester(cipherAlgorithm).matches(value, encoded);
                }
            }
        } catch (Exception e) {
            LOG.error("Could not verify encoded value", e);
        }

        return verified;
    }

    public String decode(final String encoded, final CipherAlgorithm cipherAlgorithm)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        String decoded = null;

        if (encoded != null && cipherAlgorithm == CipherAlgorithm.AES) {
            Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, aesKeySpec.
                    orElseThrow(() -> new IllegalArgumentException("AES not configured")));

            decoded = new String(cipher.doFinal(Base64.getDecoder().decode(encoded)), StandardCharsets.UTF_8);
        }

        return decoded;
    }

    private StandardStringDigester getDigester(final CipherAlgorithm cipherAlgorithm) {
        return digesters.computeIfAbsent(cipherAlgorithm, k -> {
            StandardStringDigester digester = new StandardStringDigester();

            if (cipherAlgorithm.getAlgorithm().startsWith("S-")) {
                // Salted ...
                digester.setAlgorithm(cipherAlgorithm.getAlgorithm().replaceFirst("S\\-", ""));
                digester.setIterations(digesterProperties.getSaltIterations());
                digester.setSaltSizeBytes(digesterProperties.getSaltSizeBytes());
                digester.setInvertPositionOfPlainSaltInEncryptionResults(
                        digesterProperties.isInvertPositionOfPlainSaltInEncryptionResults());
                digester.setInvertPositionOfSaltInMessageBeforeDigesting(
                        digesterProperties.isInvertPositionOfSaltInMessageBeforeDigesting());
                digester.setUseLenientSaltSizeCheck(digesterProperties.isUseLenientSaltSizeCheck());
            } else {
                // Not salted ...
                digester.setAlgorithm(cipherAlgorithm.getAlgorithm());
                digester.setIterations(1);
                digester.setSaltSizeBytes(0);
            }

            digester.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_HEXADECIMAL);
            return digester;
        });
    }
}
