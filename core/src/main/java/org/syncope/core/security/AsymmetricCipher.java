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
package org.syncope.core.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.Cipher;

public class AsymmetricCipher {

    final private static String xform = "RSA/NONE/PKCS1Padding";
    final private static String algorithm = "RSA";

    public static byte[] encrypt(byte[] inpBytes, PublicKey key)
            throws Exception {

        Cipher cipher = Cipher.getInstance(xform);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(inpBytes);
    }

    public static byte[] decrypt(byte[] inpBytes, PrivateKey key)
            throws Exception {

        Cipher cipher = Cipher.getInstance(xform);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(inpBytes);
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
        kpg.initialize(2048, new SecureRandom());

        return kpg.generateKeyPair();
    }

    public static byte[] serializeKeyPair(KeyPair keyPair) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(keyPair);
        oos.close();

        return baos.toByteArray();
    }

    public static KeyPair deserializeKeyPair(byte[] serializedKeyPair)
            throws IOException, ClassNotFoundException {

        ByteArrayInputStream bais = new ByteArrayInputStream(serializedKeyPair);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (KeyPair) ois.readObject();
    }
}
