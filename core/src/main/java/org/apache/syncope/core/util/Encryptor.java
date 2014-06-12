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
package org.apache.syncope.core.util;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.jasypt.commons.CommonUtils;
import org.jasypt.digest.StandardStringDigester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.codec.Base64;

public class Encryptor {

    private static final Logger LOG = LoggerFactory.getLogger(Encryptor.class);

    private static final Map<String, Encryptor> INSTANCES = new ConcurrentHashMap<String, Encryptor>();

    private static final String DEFAULT_SECRET_KEY = "1abcdefghilmnopqrstuvz2!";

    private static String PASSWORD_SECRET_KEY;

    private SecretKeySpec keySpec;

    static {
        InputStream propStream = null;
        try {
            propStream = Encryptor.class.getResourceAsStream("/security.properties");
            Properties props = new Properties();
            props.load(propStream);
            PASSWORD_SECRET_KEY = props.getProperty("secretKey");
        } catch (Exception e) {
            LOG.error("Could not read password secretKey", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }

        if (PASSWORD_SECRET_KEY == null) {
            PASSWORD_SECRET_KEY = DEFAULT_SECRET_KEY;
            LOG.debug("password secretKey not found, reverting to default");
        }
    }

    public static Encryptor getInstance() {
        return getInstance(PASSWORD_SECRET_KEY);
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
                    actualKey.getBytes(SyncopeConstants.DEFAULT_ENCODING), 0, 16),
                    CipherAlgorithm.AES.getAlgorithm());
        } catch (Exception e) {
            LOG.error("Error during key specification", e);
        }
    }

    public String encode(final String password, final CipherAlgorithm cipherAlgorithm)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        String encodedPassword = null;

        if (password != null) {
            if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {
                final byte[] cleartext = password.getBytes(SyncopeConstants.DEFAULT_ENCODING);

                final Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);

                encodedPassword = new String(Base64.encode(cipher.doFinal(cleartext)));
            } else if (cipherAlgorithm == CipherAlgorithm.BCRYPT) {
                encodedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            } else {
                encodedPassword = getDigester(cipherAlgorithm).digest(password);
            }
        }

        return encodedPassword;
    }

    public boolean verify(final String password, final CipherAlgorithm cipherAlgorithm,
            final String digestedPassword) {

        boolean res = false;

        try {
            if (password != null) {
                if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {
                    res = encode(password, cipherAlgorithm).equals(digestedPassword);
                } else if (cipherAlgorithm == CipherAlgorithm.BCRYPT) {
                    res = BCrypt.checkpw(password, digestedPassword);
                } else {
                    res = getDigester(cipherAlgorithm).matches(password, digestedPassword);
                }
            }
        } catch (Exception e) {
            LOG.error("Could not verify password", e);
        }

        return res;
    }

    public String decode(final String encodedPassword, final CipherAlgorithm cipherAlgorithm)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        String password = null;

        if (encodedPassword != null && cipherAlgorithm == CipherAlgorithm.AES) {
            final byte[] encoded = encodedPassword.getBytes(SyncopeConstants.DEFAULT_ENCODING);

            final Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            password = new String(cipher.doFinal(Base64.decode(encoded)));
        }

        return password;
    }

    private StandardStringDigester getDigester(final CipherAlgorithm cipherAlgorithm) {
        StandardStringDigester digester = new StandardStringDigester();

        if (cipherAlgorithm.getAlgorithm().startsWith("S-")) {
            // Salted ...
            digester.setAlgorithm(cipherAlgorithm.getAlgorithm().replaceFirst("S\\-", ""));
            digester.setIterations(100000);
            digester.setSaltSizeBytes(16);
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
