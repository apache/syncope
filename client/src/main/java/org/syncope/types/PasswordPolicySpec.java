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
package org.syncope.types;

import java.util.ArrayList;
import java.util.List;
import org.syncope.client.SchemaList;

public class PasswordPolicySpec extends AbstractPolicySpec {

    private static final long serialVersionUID = -7988778083915548547L;

    /**
     * History length
     */
    private int historyLength;

    /**
     * Minimum length.
     */
    private int maxLength;

    /**
     * Maximum length.
     */
    private int minLength;

    /**
     * Substrings not permitted.
     */
    private List<String> wordsNotPermitted;

    /**
     * User attribute values not permitted.
     */
    @SchemaList
    private List<String> schemasNotPermitted;

    /**
     * Specify if one or more non alphanumeric characters are required.
     */
    private boolean nonAlphanumericRequired;

    /**
     * Specify if one or more alphanumeric characters are required.
     */
    private boolean alphanumericRequired;

    /**
     * Specify if one or more digits are required.
     */
    private boolean digitRequired;

    /**
     * Specify if one or more lowercase alphabetic characters are required.
     */
    private boolean lowercaseRequired;

    /**
     * Specify if one or more uppercase alphabetic characters are required.
     */
    private boolean uppercaseRequired;

    /**
     * Specify if must start with a digit.
     */
    private boolean mustStartWithDigit;

    /**
     * Specify if mustn't start with a digit.
     */
    private boolean mustntStartWithDigit;

    /**
     * Specify if must end with a digit.
     */
    private boolean mustEndWithDigit;

    /**
     * Specify if mustn't end with a digit.
     */
    private boolean mustntEndWithDigit;

    /**
     * Specify if must start with a non alphanumeric caracther.
     */
    private boolean mustStartWithNonAlpha;

    /**
     * Specify if must start with a alphanumeric caracther.
     */
    private boolean mustStartWithAlpha;

    /**
     * Specify if mustn't start with a non alphanumeric caracther.
     */
    private boolean mustntStartWithNonAlpha;

    /**
     * Specify if mustn't start with a alphanumeric caracther.
     */
    private boolean mustntStartWithAlpha;

    /**
     * Specify if must end with a non alphanumeric caracther.
     */
    private boolean mustEndWithNonAlpha;

    /**
     * Specify if must end with a alphanumeric caracther.
     */
    private boolean mustEndWithAlpha;

    /**
     * Specify if mustn't end with a non alphanumeric caracther.
     */
    private boolean mustntEndWithNonAlpha;

    /**
     * Specify if mustn't end with a alphanumeric caracther.
     */
    private boolean mustntEndWithAlpha;

    /**
     * Substrings not permitted as prefix.
     */
    private List<String> prefixesNotPermitted;

    /**
     * Substrings not permitted as suffix.
     */
    private List<String> suffixesNotPermitted;

    public boolean isDigitRequired() {
        return digitRequired;
    }

    public void setDigitRequired(boolean digitRequired) {
        this.digitRequired = digitRequired;
    }

    public boolean isLowercaseRequired() {
        return lowercaseRequired;
    }

    public void setLowercaseRequired(boolean lowercaseRequired) {
        this.lowercaseRequired = lowercaseRequired;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public boolean isMustEndWithDigit() {
        return mustEndWithDigit;
    }

    public void setMustEndWithDigit(boolean mustEndWithDigit) {
        this.mustEndWithDigit = mustEndWithDigit;
    }

    public boolean isMustEndWithNonAlpha() {
        return mustEndWithNonAlpha;
    }

    public void setMustEndWithNonAlpha(boolean mustEndWithNonAlpha) {
        this.mustEndWithNonAlpha = mustEndWithNonAlpha;
    }

    public boolean isMustStartWithDigit() {
        return mustStartWithDigit;
    }

    public void setMustStartWithDigit(boolean mustStartWithDigit) {
        this.mustStartWithDigit = mustStartWithDigit;
    }

    public boolean isMustStartWithNonAlpha() {
        return mustStartWithNonAlpha;
    }

    public void setMustStartWithNonAlpha(boolean mustStartWithNonAlpha) {
        this.mustStartWithNonAlpha = mustStartWithNonAlpha;
    }

    public boolean isMustntEndWithDigit() {
        return mustntEndWithDigit;
    }

    public void setMustntEndWithDigit(boolean mustntEndWithDigit) {
        this.mustntEndWithDigit = mustntEndWithDigit;
    }

    public boolean isMustntEndWithNonAlpha() {
        return mustntEndWithNonAlpha;
    }

    public void setMustntEndWithNonAlpha(boolean mustntEndWithNonAlpha) {
        this.mustntEndWithNonAlpha = mustntEndWithNonAlpha;
    }

    public boolean isMustntStartWithDigit() {
        return mustntStartWithDigit;
    }

    public void setMustntStartWithDigit(boolean mustntStartWithDigit) {
        this.mustntStartWithDigit = mustntStartWithDigit;
    }

    public boolean isMustntStartWithNonAlpha() {
        return mustntStartWithNonAlpha;
    }

    public void setMustntStartWithNonAlpha(boolean mustntStartWithNonAlpha) {
        this.mustntStartWithNonAlpha = mustntStartWithNonAlpha;
    }

    public boolean isNonAlphanumericRequired() {
        return nonAlphanumericRequired;
    }

    public void setNonAlphanumericRequired(boolean nonAlphanumericRequired) {
        this.nonAlphanumericRequired = nonAlphanumericRequired;
    }

    public List<String> getPrefixesNotPermitted() {
        if (prefixesNotPermitted == null) {
            prefixesNotPermitted = new ArrayList<String>();
        }
        return prefixesNotPermitted;
    }

    public void setPrefixesNotPermitted(List<String> prefixesNotPermitted) {
        this.prefixesNotPermitted = prefixesNotPermitted;
    }

    public List<String> getSchemasNotPermitted() {
        if (schemasNotPermitted == null) {
            schemasNotPermitted = new ArrayList<String>();
        }
        return schemasNotPermitted;
    }

    public void setSchemasNotPermitted(List<String> schemasNotPermitted) {
        this.schemasNotPermitted = schemasNotPermitted;
    }

    public List<String> getSuffixesNotPermitted() {
        if (suffixesNotPermitted == null) {
            suffixesNotPermitted = new ArrayList<String>();
        }
        return suffixesNotPermitted;
    }

    public void setSuffixesNotPermitted(List<String> suffixesNotPermitted) {
        this.suffixesNotPermitted = suffixesNotPermitted;
    }

    public boolean isUppercaseRequired() {
        return uppercaseRequired;
    }

    public void setUppercaseRequired(boolean uppercaseRequired) {
        this.uppercaseRequired = uppercaseRequired;
    }

    public List<String> getWordsNotPermitted() {
        if (wordsNotPermitted == null) {
            wordsNotPermitted = new ArrayList<String>();
        }
        return wordsNotPermitted;
    }

    public void setWordsNotPermitted(List<String> wordsNotPermitted) {
        this.wordsNotPermitted = wordsNotPermitted;
    }

    public boolean isAlphanumericRequired() {
        return alphanumericRequired;
    }

    public void setAlphanumericRequired(boolean alphanumericRequired) {
        this.alphanumericRequired = alphanumericRequired;
    }

    public boolean isMustEndWithAlpha() {
        return mustEndWithAlpha;
    }

    public void setMustEndWithAlpha(boolean mustEndWithAlpha) {
        this.mustEndWithAlpha = mustEndWithAlpha;
    }

    public boolean isMustStartWithAlpha() {
        return mustStartWithAlpha;
    }

    public void setMustStartWithAlpha(boolean mustStartWithAlpha) {
        this.mustStartWithAlpha = mustStartWithAlpha;
    }

    public boolean isMustntEndWithAlpha() {
        return mustntEndWithAlpha;
    }

    public void setMustntEndWithAlpha(boolean mustntEndWithAlpha) {
        this.mustntEndWithAlpha = mustntEndWithAlpha;
    }

    public boolean isMustntStartWithAlpha() {
        return mustntStartWithAlpha;
    }

    public void setMustntStartWithAlpha(boolean mustntStartWithAlpha) {
        this.mustntStartWithAlpha = mustntStartWithAlpha;
    }

    public int getHistoryLength() {
        return historyLength;
    }

    public void setHistoryLength(int historyLength) {
        this.historyLength = historyLength;
    }
}
