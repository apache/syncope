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
package org.apache.syncope.core.spring.policy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolicyPatternTest {

    @Test
    void digit() {
        assertTrue(PolicyPattern.DIGIT.matcher("a1").matches());
        assertFalse(PolicyPattern.DIGIT.matcher("abc").matches());
    }

    @Test
    void alphabeticalCase() {
        assertTrue(PolicyPattern.ALPHA_LOWERCASE.matcher("aB").matches());
        assertFalse(PolicyPattern.ALPHA_LOWERCASE.matcher("AB1").matches());

        assertTrue(PolicyPattern.ALPHA_UPPERCASE.matcher("Ab").matches());
        assertFalse(PolicyPattern.ALPHA_UPPERCASE.matcher("ab1").matches());
    }

    @Test
    void firstAndLastDigit() {
        assertTrue(PolicyPattern.FIRST_DIGIT.matcher("1abc").matches());
        assertFalse(PolicyPattern.FIRST_DIGIT.matcher("a1bc").matches());

        assertTrue(PolicyPattern.LAST_DIGIT.matcher("abc1").matches());
        assertFalse(PolicyPattern.LAST_DIGIT.matcher("ab1c").matches());
    }

    @Test
    void alphanumeric() {
        assertTrue(PolicyPattern.ALPHANUMERIC.matcher("a").matches());
        assertTrue(PolicyPattern.ALPHANUMERIC.matcher("1").matches());
        assertFalse(PolicyPattern.ALPHANUMERIC.matcher("!").matches());

        assertTrue(PolicyPattern.FIRST_ALPHANUMERIC.matcher("a!").matches());
        assertFalse(PolicyPattern.FIRST_ALPHANUMERIC.matcher("!a").matches());

        assertTrue(PolicyPattern.LAST_ALPHANUMERIC.matcher("!a").matches());
        assertFalse(PolicyPattern.LAST_ALPHANUMERIC.matcher("a!").matches());
    }

    @Test
    void nonAlphanumeric() {
        assertTrue(PolicyPattern.NON_ALPHANUMERIC.matcher("a!b").matches());
        assertFalse(PolicyPattern.NON_ALPHANUMERIC.matcher("ab1").matches());

        assertTrue(PolicyPattern.FIRST_NON_ALPHANUMERIC.matcher("!ab").matches());
        assertFalse(PolicyPattern.FIRST_NON_ALPHANUMERIC.matcher("a!b").matches());

        assertTrue(PolicyPattern.LAST_NON_ALPHANUMERIC.matcher("ab!").matches());
        assertFalse(PolicyPattern.LAST_NON_ALPHANUMERIC.matcher("a!b").matches());
    }

    @Test
    void nonAlphanumericCharsForPasswordValues() {
        assertArrayEquals(
                new char[] { '!', '£', '%', '&', '(', ')', '?', '#', '$' },
                PolicyPattern.NON_ALPHANUMERIC_CHARS_FOR_PASSWORD_VALUES);
    }
}
