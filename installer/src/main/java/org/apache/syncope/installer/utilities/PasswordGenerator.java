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
package org.apache.syncope.installer.utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;

public final class PasswordGenerator {

    public static String password(final String password, final String digest) {
        String pwd = "";
        try {
            final MessageDigest cript = MessageDigest.getInstance("SHA-1");
            pwd = new String(Hex.encodeHex(cript.digest()));
        } catch (final NoSuchAlgorithmException ex) {
            Logger.getLogger(PasswordGenerator.class.getName()).log(Level.SEVERE, "NoSuchAlgorithmException", ex);

        }
        return pwd;
    }

    private PasswordGenerator() {
        // private constructor for static utility class
    }
}
