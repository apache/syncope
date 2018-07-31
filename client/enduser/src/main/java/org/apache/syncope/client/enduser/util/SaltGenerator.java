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
package org.apache.syncope.client.enduser.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.xml.bind.DatatypeConverter;

public final class SaltGenerator {

    public static String generate(final String input) {
        // generate salt
        byte[] salt = new byte[16];
        // fill array with random bytes
        new SecureRandom().nextBytes(salt);
        // create digest with MD2
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD2");
            return DatatypeConverter.printHexBinary(
                    md.digest((input + Base64.getMimeEncoder().encodeToString(salt)).getBytes()));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private SaltGenerator() {
    }
}
