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

import java.security.SecureRandom;
import org.apache.wicket.util.crypt.Base64;
import org.apache.commons.codec.digest.DigestUtils;

public final class SaltGenerator {

    public static String generate(final String input) {
        // generate salt
        byte[] salt = new byte[16];
        // fill array with random bytes
        new SecureRandom().nextBytes(salt);
        // create digest with MD5
        return DigestUtils.md2Hex(input + Base64.encodeBase64String(salt));
    }

    private SaltGenerator() {
    }
}
