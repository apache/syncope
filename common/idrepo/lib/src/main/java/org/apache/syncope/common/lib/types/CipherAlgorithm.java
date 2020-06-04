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
package org.apache.syncope.common.lib.types;

public enum CipherAlgorithm {

    SHA("SHA-1", false),
    SHA1("SHA-1", false),
    SHA256("SHA-256", false),
    SHA512("SHA-512", false),
    AES("AES", true),
    SMD5("S-MD5", false),
    SSHA("S-SHA-1", false),
    SSHA1("S-SHA-1", false),
    SSHA256("S-SHA-256", false),
    SSHA512("S-SHA-512", false),
    BCRYPT("BCRYPT", false);

    private final String algorithm;

    private final boolean invertible;

    CipherAlgorithm(final String algorithm, final boolean invertible) {
        this.algorithm = algorithm;
        this.invertible = invertible;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public boolean isInvertible() {
        return invertible;
    }

    public boolean isSalted() {
        return algorithm.startsWith("S-");
    }
}
