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

import org.apache.commons.text.CharacterPredicate;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.syncope.common.lib.SecureTextRandomProvider;

public final class SecureRandomUtils {

    private static final RandomStringGenerator FOR_PASSWORD = new RandomStringGenerator.Builder().
            usingRandom(new SecureTextRandomProvider()).
            filteredBy(new CharacterPredicate() {

                @Override
                public boolean test(final int codePoint) {
                    return (codePoint >= 'a' && codePoint <= 'z') || (codePoint >= '0' && codePoint <= '9');
                }
            }).
            build();

    private static final RandomStringGenerator FOR_LETTERS = new RandomStringGenerator.Builder().
            usingRandom(new SecureTextRandomProvider()).
            withinRange('a', 'z').
            build();

    private static final RandomStringGenerator FOR_NUMBERS = new RandomStringGenerator.Builder().
            usingRandom(new SecureTextRandomProvider()).
            withinRange('0', '9').
            build();

    public static String generateRandomPassword(final int tokenLength) {
        return FOR_PASSWORD.generate(tokenLength);
    }

    public static String generateRandomLetter() {
        return FOR_LETTERS.generate(1);
    }

    public static String generateRandomNumber() {
        return FOR_NUMBERS.generate(1);
    }

    public static String generateRandomSpecialCharacter(final char[] characters) {
        return new RandomStringGenerator.Builder().
                usingRandom(new SecureTextRandomProvider()).
                filteredBy(new CharacterPredicate() {

                    @Override
                    public boolean test(final int codePoint) {
                        boolean found = false;
                        for (int i = 0; i < characters.length && !found; i++) {
                            found = codePoint == Character.codePointAt(characters, i);
                        }

                        return found;
                    }
                }).build().generate(1);
    }

    private SecureRandomUtils() {
        // private constructor for static utility class
    }
}
