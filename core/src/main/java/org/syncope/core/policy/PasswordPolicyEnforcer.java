/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.policy;

import java.io.InvalidObjectException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.syncope.types.PasswordPolicy;
import org.syncope.types.PolicyType;

@Component
public class PasswordPolicyEnforcer
        implements PolicyEnforcer<PasswordPolicy, String> {

    private static final Pattern DIGIT = Pattern.compile(".*\\d+.*");

    private static final Pattern ALPHA_LOWERCASE = Pattern.compile(".*[a-z]+.*");

    private static final Pattern ALPHA_UPPERCASE = Pattern.compile(".*[A-Z]+.*");

    private static final Pattern FIRSTDIGIT = Pattern.compile("\\d.*");

    private static final Pattern LASTDIGIT = Pattern.compile(".*\\d");

    private static final Pattern ALPHANUMERIC = Pattern.compile(".*\\w.*");

    private static final Pattern FIRSTALPHANUMERIC = Pattern.compile("\\w.*");

    private static final Pattern LASTALPHANUMERIC = Pattern.compile(".*\\w");

    private static final Pattern NONALPHANUMERIC = Pattern.compile(".*\\W.*");

    private static final Pattern FIRSTNONALPHANUMERIC = Pattern.compile("\\W.*");

    private static final Pattern LASTNONALPHANUMERIC = Pattern.compile(".*\\W");

    @Override
    public void enforce(
            final PasswordPolicy policy,
            final PolicyType type,
            final String object) throws InvalidObjectException, Exception {

        if (!(object instanceof String)) {
            throw new InvalidObjectException("Invalid object type");
        }

        // check length
        if (policy.getMinLength() > 0
                && policy.getMinLength() > object.length()) {
            throw new Exception("Password too short");
        }

        if (policy.getMaxLength() > 0
                && policy.getMaxLength() < object.length()) {
            throw new Exception("Password too long");
        }

        // check words not permitted
        for (String word : policy.getWordsNotPermitted()) {
            if (object.contains(word)) {
                throw new Exception("Used word(s) not permitted");
            }
        }

        // check digits occurrence
        if (policy.isDigitRequired()
                && !checkForDigit(object)) {
            throw new Exception("Password must contain digit(s)");
        }

        // check lowercase alphabetic characters occurrence
        if (policy.isLowercaseRequired()
                && !checkForLowercase(object)) {
            throw new Exception(
                    "Password must contain lowercase alphabetic character(s)");
        }

        // check uppercase alphabetic characters occurrence
        if (policy.isUppercaseRequired()
                && !checkForUppercase(object)) {
            throw new Exception(
                    "Password must contain uppercase alphabetic character(s)");
        }

        // check prefix
        for (String prefix : policy.getPrefixesNotPermitted()) {
            if (object.startsWith(prefix)) {
                throw new Exception("Prefix not permitted");
            }
        }

        // check suffix
        for (String suffix : policy.getSuffixesNotPermitted()) {
            if (object.endsWith(suffix)) {
                throw new Exception("Suffix not permitted");
            }
        }

        // check digit first occurrence
        if (policy.isMustStartWithDigit()
                && !checkForFirstDigit(object)) {
            throw new Exception("Password must start with a digit");
        }

        if (policy.isMustntStartWithDigit()
                && checkForFirstDigit(object)) {
            throw new Exception("Password mustn't start with a digit");
        }

        // check digit last occurrence
        if (policy.isMustEndWithDigit()
                && !checkForLastDigit(object)) {
            throw new Exception("Password must end with a digit");
        }

        if (policy.isMustntEndWithDigit()
                && checkForLastDigit(object)) {
            throw new Exception("Password mustn't end with a digit");
        }

        // check alphanumeric characters occurence
        if (policy.isAlphanumericRequired()
                && !checkForAlphanumeric(object)) {
            throw new Exception(
                    "Password must contain alphanumeric character(s)");
        }

        // check non alphanumeric characters occurence
        if (policy.isNonAlphanumericRequired()
                && !checkForNonAlphanumeric(object)) {
            throw new Exception(
                    "Password must contain non-alphanumeric character(s)");
        }

        // check alphanumeric character first occurrence
        if (policy.isMustStartWithAlpha()
                && !checkForFirstAlphanumeric(object)) {
            throw new Exception(
                    "Password must start with an alphanumeric character");
        }

        if (policy.isMustntStartWithAlpha()
                && checkForFirstAlphanumeric(object)) {
            throw new Exception(
                    "Password mustn't start with an alphanumeric character");
        }

        // check alphanumeric character last occurrence
        if (policy.isMustEndWithAlpha()
                && !checkForLastAlphanumeric(object)) {
            throw new Exception(
                    "Password must end with an alphanumeric character");
        }

        if (policy.isMustntEndWithAlpha()
                && checkForLastAlphanumeric(object)) {
            throw new Exception(
                    "Password mustn't end with an alphanumeric character");
        }

        // check non alphanumeric character first occurrence
        if (policy.isMustStartWithNonAlpha()
                && !checkForFirstNonAlphanumeric(object)) {
            throw new Exception(
                    "Password must start with a non-alphanumeric character");
        }

        if (policy.isMustntStartWithNonAlpha()
                && checkForFirstNonAlphanumeric(object)) {
            throw new Exception(
                    "Password mustn't start with a non-alphanumeric character");
        }

        // check non alphanumeric character last occurrence
        if (policy.isMustEndWithNonAlpha()
                && !checkForLastNonAlphanumeric(object)) {
            throw new Exception(
                    "Password must end with a non-alphanumeric character");
        }

        if (policy.isMustntEndWithNonAlpha()
                && checkForLastNonAlphanumeric(object)) {
            throw new Exception(
                    "Password mustn't end with a non-alphanumeric character");
        }
    }

    private boolean checkForDigit(String str) {
        return DIGIT.matcher((CharSequence) str).matches();
    }

    private boolean checkForLowercase(String str) {
        return ALPHA_LOWERCASE.matcher((CharSequence) str).matches();
    }

    private boolean checkForUppercase(String str) {
        return ALPHA_UPPERCASE.matcher((CharSequence) str).matches();
    }

    private boolean checkForFirstDigit(String str) {
        return FIRSTDIGIT.matcher((CharSequence) str).matches();
    }

    private boolean checkForLastDigit(String str) {
        return LASTDIGIT.matcher((CharSequence) str).matches();
    }

    private boolean checkForAlphanumeric(String str) {
        return ALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForFirstAlphanumeric(String str) {
        return FIRSTALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForLastAlphanumeric(String str) {
        return LASTALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForNonAlphanumeric(String str) {
        return NONALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForFirstNonAlphanumeric(String str) {
        return FIRSTNONALPHANUMERIC.matcher(str).matches();
    }

    private boolean checkForLastNonAlphanumeric(String str) {
        return LASTNONALPHANUMERIC.matcher(str).matches();
    }
}
