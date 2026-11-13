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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.passay.data.EnglishCharacterData;
import org.passay.rule.CharacterRule;

class SecureRandomUtilsTest {

    @Test
    void generateRandomPassword() {
        String password = SecureRandomUtils.generateRandomPassword(16);

        assertEquals(16, password.length());
        assertTrue(password.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void generateRandomLetters() {
        assertEquals(1, SecureRandomUtils.generateRandomLetter().length());
        assertEquals(8, SecureRandomUtils.generateRandomLetters(8).length());
        assertTrue(SecureRandomUtils.generateRandomLetters(8).chars().allMatch(Character::isLowerCase));
    }

    @Test
    void generateRandomNumber() {
        String number = SecureRandomUtils.generateRandomNumber();

        assertEquals(1, number.length());
        assertTrue(number.chars().allMatch(Character::isDigit));
    }

    @Test
    void generateRandomNonAlphanumericChar() {
        char[] allowed = { '!', '@', '#' };

        String value = SecureRandomUtils.generateRandomNonAlphanumericChar(allowed);

        assertEquals(1, value.length());
        assertTrue(List.of('!', '@', '#').contains(value.charAt(0)));
    }

    @Test
    void generateRandomNumbersInRange() {
        for (int i = 0; i < 100; i++) {
            int intValue = SecureRandomUtils.generateRandomInt(0, 10);
            assertTrue(intValue >= 0 && intValue < 10);

            long longValue = SecureRandomUtils.generateRandomLong(0, 10);
            assertTrue(longValue >= 0 && longValue < 10);
        }
    }

    @Test
    void generateRandomUUIDs() {
        UUID first = SecureRandomUtils.generateRandomUUID();
        UUID second = SecureRandomUtils.generateRandomUUID();

        assertNotNull(first);
        assertNotEquals(first, second);
    }

    @Test
    void passwordGenerator() {
        assertNotNull(SecureRandomUtils.passwordGenerator(
                10, List.of(new CharacterRule(EnglishCharacterData.Alphabetical, 1))));
    }
}
