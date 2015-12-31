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
package org.apache.syncope.client.lib;

import java.security.SecureRandom;
import java.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaltGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(SaltGenerator.class);

    public static String generate(final String input) {
        // generate salt
        byte[] salt = new byte[16];
        // fill array with random bytes
        new SecureRandom().nextBytes(salt);
        // create digest with MD5
        return DigestUtils.md2Hex(input + Base64.getEncoder().encodeToString(salt));
    }

    private SaltGenerator() {
    }
}
