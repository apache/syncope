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

import java.util.regex.Pattern;

public final class PolicyPattern {

    public static final Pattern DIGIT = Pattern.compile(".*\\d+.*");

    public static final Pattern ALPHA_LOWERCASE = Pattern.compile(".*[a-z]+.*");

    public static final Pattern ALPHA_UPPERCASE = Pattern.compile(".*[A-Z]+.*");

    public static final Pattern FIRST_DIGIT = Pattern.compile("\\d.*");

    public static final Pattern LAST_DIGIT = Pattern.compile(".*\\d");

    public static final Pattern ALPHANUMERIC = Pattern.compile(".*\\w.*");

    public static final Pattern FIRST_ALPHANUMERIC = Pattern.compile("\\w.*");

    public static final Pattern LAST_ALPHANUMERIC = Pattern.compile(".*\\w");

    public static final Pattern NON_ALPHANUMERIC =
            Pattern.compile(".*[~!@#£$%^&*_\\-`(){}\\[\\]:;\"'<>,.?/\\=\\+\\\\\\|].*");

    public static final Pattern FIRST_NON_ALPHANUMERIC =
            Pattern.compile("[~!@#£$%^&*_\\-`(){}\\[\\]:;\"'<>,.?/\\=\\+\\\\\\|].*");

    public static final Pattern LAST_NON_ALPHANUMERIC =
            Pattern.compile(".*[~!@#£$%^&*_\\-`(){}\\[\\]:;\"'<>,.?/\\=\\+\\\\\\|]");

    public static final char[] NON_ALPHANUMERIC_CHARS_FOR_PASSWORD_VALUES = {
        '!', '£', '%', '&', '(', ')', '?', '#', '$' };

    private PolicyPattern() {
        // private constructor for static utility class
    }
}
