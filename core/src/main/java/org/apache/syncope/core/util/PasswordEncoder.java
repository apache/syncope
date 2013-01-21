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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.ArrayUtils;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.jasypt.digest.StandardStringDigester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.codec.Base64;

public final class PasswordEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordEncoder.class);

    private static SecretKeySpec keySpec;

    static {
        String secretKey = null;

        InputStream propStream = null;
        try {
            propStream = PasswordEncoder.class.getResourceAsStream("/security.properties");
            Properties props = new Properties();
            props.load(propStream);
            secretKey = props.getProperty("secretKey");
        } catch (Exception e) {
            LOG.error("Could not read secretKey", e);
        } finally {
            if (propStream != null) {
                try {
                    propStream.close();
                } catch (IOException e) {
                    LOG.error("While closing property stream", e);
                }
            }
        }

        if (secretKey == null) {
            secretKey = "1abcdefghilmnopqrstuvz2!";
            LOG.debug("secretKey not found, reverting to default");
        }
        if (secretKey.length() < 16) {
            StringBuilder secretKeyPadding = new StringBuilder(secretKey);
            for (int i = 0; i < 16 - secretKey.length(); i++) {
                secretKeyPadding.append('0');
            }
            secretKey = secretKeyPadding.toString();
            LOG.debug("secretKey too short, adding some random characters");
        }

        try {
            keySpec = new SecretKeySpec(ArrayUtils.subarray(secretKey.getBytes("UTF8"), 0, 16), "AES");
        } catch (Exception e) {
            LOG.error("Error during key specification", e);
        }
    }

    public static String encodePassword(final String password, final CipherAlgorithm cipherAlgorithm)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {

        String encodedPassword = null;

        if (password != null) {
            if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {
                final byte[] cleartext = password.getBytes("UTF8");

                final Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                byte[] encoded = cipher.doFinal(cleartext);

                encodedPassword = new String(Base64.encode(encoded));
            } else if (cipherAlgorithm.getAlgorithm().equals("BCRYPT")) {
                encodedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            } else {
                encodedPassword = getDigester(cipherAlgorithm).digest(password);
            }
        }

        return encodedPassword;
    }

    public static boolean verifyPassword(final String password, final CipherAlgorithm cipherAlgorithm,
            final String digestedPassword) {

        boolean res = false;

        try {
            if (password != null) {
                if (cipherAlgorithm == null || cipherAlgorithm == CipherAlgorithm.AES) {

                    final byte[] cleartext = password.getBytes("UTF8");

                    final Cipher cipher = Cipher.getInstance(CipherAlgorithm.AES.getAlgorithm());
                    cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                    byte[] encoded = cipher.doFinal(cleartext);

                    res = new String(Base64.encode(encoded)).equals(digestedPassword);
                } else if (cipherAlgorithm.getAlgorithm().equals("BCRYPT")) {
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

    private static StandardStringDigester getDigester(final CipherAlgorithm cipherAlgorithm) {
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

        digester.setStringOutputType("hexadecimal");
        return digester;
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private PasswordEncoder() {
    }
}
