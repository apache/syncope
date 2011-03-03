/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import org.identityconnectors.common.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.core.persistence.dao.ConfDAO;

public class EncryptionUtil {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(EncryptionUtil.class);

    @Autowired
    private ConfDAO confDAO;

    public String encrypt(final String toBeCrypted) {
        String result = null;

        try {
            final DESKeySpec keySpec =
                    new DESKeySpec(confDAO.find(
                    "token.encryption.key").getConfValue().getBytes("UTF8"));

            final SecretKeyFactory keyFactory =
                    SecretKeyFactory.getInstance("DES");

            final SecretKey key = keyFactory.generateSecret(keySpec);

            final byte[] cleartext = toBeCrypted.getBytes("UTF8");

            final Cipher cipher = Cipher.getInstance("DES");

            LOG.debug("To Be Encrypted: {}", toBeCrypted);

            cipher.init(Cipher.ENCRYPT_MODE, key);
            result = Base64.encode(cipher.doFinal(cleartext));

            LOG.debug("Encrypted: {}", result);
        } catch (Exception e) {
            LOG.error("Encrypt operation failed", e);
        }

        return result;
    }

    public String decrypt(final String toBeDecrypted) {
        String result = null;

        try {
            final DESKeySpec keySpec =
                    new DESKeySpec(confDAO.find(
                    "token.encryption.key").getConfValue().getBytes("UTF8"));

            final SecretKeyFactory keyFactory =
                    SecretKeyFactory.getInstance("DES");

            final SecretKey key = keyFactory.generateSecret(keySpec);

            final byte[] encrypedPwdBytes =
                    Base64.decode(toBeDecrypted);

            final Cipher cipher = Cipher.getInstance("DES");

            LOG.debug("To Be Dencrypted: {}", toBeDecrypted);

            cipher.init(Cipher.DECRYPT_MODE, key);
            result = new String(cipher.doFinal(encrypedPwdBytes));

            LOG.debug("Dencrypted: {}", result);
        } catch (Exception e) {
            LOG.error("Decrypt operation failed", e);
        }

        return result;
    }
}
