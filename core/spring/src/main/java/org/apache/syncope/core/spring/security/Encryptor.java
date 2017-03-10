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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.jasypt.commons.CommonUtils;
import org.jasypt.digest.StandardStringDigester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.codec.Base64;

public final class Encryptor {

    private static final Logger LOG = LoggerFactory.getLogger(Encryptor.class);

    private static final Map<String, Encryptor> INSTANCES = new ConcurrentHashMap<>();

    private static final String DEFAULT_SECRET_KEY = "1abcdefghilmnopqrstuvz2!";

    /**
     * Default value for salted {@link StandardStringDigester#setIterations(int)}.
     */
    private static final int DEFAULT_SALT_ITERATIONS = 1;

    /**
     * Default value for {@link StandardStringDigester#setSaltSizeBytes(int)}.
     */
    private static final int DEFAULT_SALT_SIZE_BYTES = 8;

    /**
     * Default value for {@link StandardStringDigester#setInvertPositionOfPlainSaltInEncryptionResults(boolean)}.
     */
    private static final boolean DEFAULT_IPOPSIER = true;

    /**
     * Default value for salted {@link StandardStringDigester#setInvertPositionOfSaltInMessageBeforeDigesting(boolean)}.
     */
    private static final boolean DEFAULT_IPOSIMBD = true;

    /**
     * Default value for salted {@link StandardStringDigester#setUseLenientSaltSizeCheck(boolean)}.
     */
    private static final boolean DEFAULT_ULSSC = true;

    private static String SECRET_KEY;

    private static Integer SALT_ITERATIONS;

    private static Integer SALT_SIZE_BYTES;

    private static Boolean IPOPSIER;

    private static Boolean IPOSIMBD;

    private static Boolean ULSSC;

    static {
        InputStream propStream = null;
        try {
            propStream = Encryptor.class.getResourceAsStream("/security.properties");
            Properties props = new Properties();
            props.load(propStream);

            SECRET_KEY = props.getProperty("secretKey");
            SALT_ITERATIONS = Integer.valueOf(props.getProperty("digester.saltIterations"));
            SALT_SIZE_BYTES = Integer.valueOf(props.getProperty("digester.saltSizeBytes"));
            IPOPSIER = Boolean.valueOf(props.getProperty("digester.invertPositionOfPlainSaltInEncryptionResults"));
            IPOSIMBD = Boolean.valueOf(props.getProperty("digester.invertPositionOfSaltInMessageBeforeDigesting"));
            ULSSC = Boolean.valueOf(props.getProperty("digester.useLenientSaltSizeCheck"));
        } catch (Exception e) {
            LOG.error("Could not read security parameters", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }

        if (SECRET_KEY == null) {
            SECRET_KEY = DEFAULT_SECRET_KEY;
            LOG.debug("secretKey not found, reverting to default");
        }
        if (SALT_ITERATIONS == null) {
            SALT_ITERATIONS = DEFAULT_SALT_ITERATIONS;
            LOG.debug("digester.saltIterations not found, reverting to default");
        }
        if (SALT_SIZE_BYTES == null) {
            SALT_SIZE_BYTES = DEFAULT_SALT_SIZE_BYTES;
            LOG.debug("digester.saltSizeBytes not found, reverting to default");
        }
        if (IPOPSIER == null) {
            IPOPSIER = DEFAULT_IPOPSIER;
            LOG.debug("digester.invertPositionOfPlainSaltInEncryptionResults not found, reverting to default");
        }
        if (IPOSIMBD == null) {
            IPOSIMBD = DEFAULT_IPOSIMBD;
            LOG.debug("digester.invertPositionOfSaltInMessageBeforeDigesting not found, reverting to default");
        }
        if (ULSSC == null) {
            ULSSC = DEFAULT_ULSSC;
            LOG.debug("digester.useLenientSaltSizeCheck not found, reverting to default");
        }
    }

    public static Encryptor getInstance() {
        return getInstance(SECRET_KEY);
    }

    public static Encryptor getInstance(final String secretKey) {
        String actualKey = StringUtils.isBlank(secretKey) ? DEFAULT_SECRET_KEY : secretKey;

        Encryptor instance = INSTANCES.get(actualKey);
        if (instance == null) {
            instance = new Encryptor(actualKey);
            INSTANCES.put(actualKey, instance);
        }

        return instance;
    }

    private SecretKeySpec keySpec;

    private Encryptor(final String secretKey) {
        String actualKey = secretKey;
        if (actualKey.length() < 16) {
            StringBuilder actualKeyPadding = new StringBuilder(actualKey);
            for (int i = 0; i < 16 - actualKey.length(); i++) {
                actualKeyPadding.append('0');
            }
            actualKey = actualKeyPadding.toString();
            LOG.debug("actualKey too short, adding some random characters");
        }

        try {
            keySpec = new SecretKeySpec(ArrayUtils.subarray(
                    actualKey.getBytes(StandardCharsets.UTF_8), 0, 16),
                    CipherAlgorithm.AES.getAlgorithm());
        } catch (Exception e) {
            LOG.error("Error during key specification", e);
        }
    }

    public String encode(final String value, final CipherAlgorithm cipherAlgorithm)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        String encodedValue = null;

        if (value != null) {
            if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {
                final byte[] cleartext = value.getBytes(StandardCharsets.UTF_8);

                final Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);

                encodedValue = new String(Base64.encode(cipher.doFinal(cleartext)));
            } else if (cipherAlgorithm == CipherAlgorithm.BCRYPT) {
                encodedValue = BCrypt.hashpw(value, BCrypt.gensalt());
            } else {
                encodedValue = getDigester(cipherAlgorithm).digest(value);
            }
        }

        return encodedValue;
    }

    public boolean verify(final String value, final CipherAlgorithm cipherAlgorithm, final String encodedValue) {
        boolean res = false;

        try {
            if (value != null) {
                if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {
                    res = encode(value, cipherAlgorithm).equals(encodedValue);
                } else if (cipherAlgorithm == CipherAlgorithm.BCRYPT) {
                    res = BCrypt.checkpw(value, encodedValue);
                } else {
                    res = getDigester(cipherAlgorithm).matches(value, encodedValue);
                }
            }
        } catch (Exception e) {
            LOG.error("Could not verify encoded value", e);
        }

        return res;
    }

    public String decode(final String encodedValue, final CipherAlgorithm cipherAlgorithm)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        String value = null;

        if (encodedValue != null && cipherAlgorithm == CipherAlgorithm.AES) {
            final byte[] encoded = encodedValue.getBytes(StandardCharsets.UTF_8);

            final Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            value = new String(cipher.doFinal(Base64.decode(encoded)), StandardCharsets.UTF_8);
        }

        return value;
    }

    private StandardStringDigester getDigester(final CipherAlgorithm cipherAlgorithm) {
        StandardStringDigester digester = new StandardStringDigester();

        if (cipherAlgorithm.getAlgorithm().startsWith("S-")) {
            // Salted ...
            digester.setAlgorithm(cipherAlgorithm.getAlgorithm().replaceFirst("S\\-", ""));
            digester.setIterations(SALT_ITERATIONS);
            digester.setSaltSizeBytes(SALT_SIZE_BYTES);
            digester.setInvertPositionOfPlainSaltInEncryptionResults(IPOPSIER);
            digester.setInvertPositionOfSaltInMessageBeforeDigesting(IPOSIMBD);
            digester.setUseLenientSaltSizeCheck(ULSSC);
        } else {
            // Not salted ...
            digester.setAlgorithm(cipherAlgorithm.getAlgorithm());
            digester.setIterations(1);
            digester.setSaltSizeBytes(0);
        }

        digester.setStringOutputType(CommonUtils.STRING_OUTPUT_TYPE_HEXADECIMAL);
        return digester;
    }
}
