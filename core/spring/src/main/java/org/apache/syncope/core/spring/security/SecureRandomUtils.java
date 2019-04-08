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

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import java.security.SecureRandom;
import java.util.UUID;
import org.apache.commons.text.RandomStringGenerator;

public final class SecureRandomUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final RandomStringGenerator FOR_PASSWORD = new RandomStringGenerator.Builder().
            usingRandom(RANDOM::nextInt).
            withinRange('0', 'z').
            filteredBy(Character::isLetterOrDigit).
            build();

    private static final RandomStringGenerator FOR_LETTERS = new RandomStringGenerator.Builder().
            usingRandom(RANDOM::nextInt).
            withinRange('a', 'z').
            build();

    private static final RandomStringGenerator FOR_NUMBERS = new RandomStringGenerator.Builder().
            usingRandom(RANDOM::nextInt).
            withinRange('0', '9').
            build();

    private static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator(RANDOM);

    public static String generateRandomPassword(final int tokenLength) {
        return FOR_PASSWORD.generate(tokenLength);
    }

    public static String generateRandomLetter() {
        return FOR_LETTERS.generate(1);
    }

    public static String generateRandomLetters(final int length) {
        return FOR_LETTERS.generate(length);
    }

    public static String generateRandomNumber() {
        return FOR_NUMBERS.generate(1);
    }

    public static String generateRandomNonAlphanumericChar(final char[] characters) {
        return new RandomStringGenerator.Builder().
                usingRandom(RANDOM::nextInt).
                filteredBy(codePoint -> {
                    boolean found = false;
                    for (int i = 0; i < characters.length && !found; i++) {
                        found = codePoint == Character.codePointAt(characters, i);
                    }

                    return found;
                }).build().generate(1);
    }

    public static UUID generateRandomUUID() {
        return UUID_GENERATOR.generate();
    }

    private SecureRandomUtils() {
        // private constructor for static utility class
    }
}
