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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.report.Schema;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;

@XmlRootElement(name = "defaultPasswordRuleConf")
@XmlType
public class DefaultPasswordRuleConf extends AbstractPasswordRuleConf {

    private static final long serialVersionUID = -7988778083915548547L;

    /**
     * Minimum length.
     */
    private int maxLength;

    /**
     * Maximum length.
     */
    private int minLength;

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
     * Specify if must start with a non alphanumeric character.
     */
    private boolean mustStartWithNonAlpha;

    /**
     * Specify if must start with a alphanumeric character.
     */
    private boolean mustStartWithAlpha;

    /**
     * Specify if mustn't start with a non alphanumeric character.
     */
    private boolean mustntStartWithNonAlpha;

    /**
     * Specify if mustn't start with a alphanumeric character.
     */
    private boolean mustntStartWithAlpha;

    /**
     * Specify if must end with a non alphanumeric character.
     */
    private boolean mustEndWithNonAlpha;

    /**
     * Specify if must end with a alphanumeric character.
     */
    private boolean mustEndWithAlpha;

    /**
     * Specify if mustn't end with a non alphanumeric character.
     */
    private boolean mustntEndWithNonAlpha;

    /**
     * Specify if mustn't end with a alphanumeric character.
     */
    private boolean mustntEndWithAlpha;

    /**
     * Specify if using username as password is allowed.
     */
    private boolean usernameAllowed;

    /**
     * Substrings not permitted.
     */
    private final List<String> wordsNotPermitted = new ArrayList<>();

    /**
     * User attribute values not permitted.
     */
    @Schema(anyTypeKind = AnyTypeKind.USER,
            type = { SchemaType.PLAIN, SchemaType.DERIVED, SchemaType.VIRTUAL })
    private final List<String> schemasNotPermitted = new ArrayList<>();

    /**
     * Substrings not permitted as prefix.
     */
    private final List<String> prefixesNotPermitted = new ArrayList<>();

    /**
     * Substrings not permitted as suffix.
     */
    private final List<String> suffixesNotPermitted = new ArrayList<>();

    public boolean isDigitRequired() {
        return digitRequired;
    }

    public void setDigitRequired(final boolean digitRequired) {
        this.digitRequired = digitRequired;
    }

    public boolean isLowercaseRequired() {
        return lowercaseRequired;
    }

    public void setLowercaseRequired(final boolean lowercaseRequired) {
        this.lowercaseRequired = lowercaseRequired;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(final int minLength) {
        this.minLength = minLength;
    }

    public boolean isMustEndWithDigit() {
        return mustEndWithDigit;
    }

    public void setMustEndWithDigit(final boolean mustEndWithDigit) {
        this.mustEndWithDigit = mustEndWithDigit;
    }

    public boolean isMustEndWithNonAlpha() {
        return mustEndWithNonAlpha;
    }

    public void setMustEndWithNonAlpha(final boolean mustEndWithNonAlpha) {
        this.mustEndWithNonAlpha = mustEndWithNonAlpha;
    }

    public boolean isMustStartWithDigit() {
        return mustStartWithDigit;
    }

    public void setMustStartWithDigit(final boolean mustStartWithDigit) {
        this.mustStartWithDigit = mustStartWithDigit;
    }

    public boolean isMustStartWithNonAlpha() {
        return mustStartWithNonAlpha;
    }

    public void setMustStartWithNonAlpha(final boolean mustStartWithNonAlpha) {
        this.mustStartWithNonAlpha = mustStartWithNonAlpha;
    }

    public boolean isMustntEndWithDigit() {
        return mustntEndWithDigit;
    }

    public void setMustntEndWithDigit(final boolean mustntEndWithDigit) {
        this.mustntEndWithDigit = mustntEndWithDigit;
    }

    public boolean isMustntEndWithNonAlpha() {
        return mustntEndWithNonAlpha;
    }

    public void setMustntEndWithNonAlpha(final boolean mustntEndWithNonAlpha) {
        this.mustntEndWithNonAlpha = mustntEndWithNonAlpha;
    }

    public boolean isMustntStartWithDigit() {
        return mustntStartWithDigit;
    }

    public void setMustntStartWithDigit(final boolean mustntStartWithDigit) {
        this.mustntStartWithDigit = mustntStartWithDigit;
    }

    public boolean isMustntStartWithNonAlpha() {
        return mustntStartWithNonAlpha;
    }

    public void setMustntStartWithNonAlpha(final boolean mustntStartWithNonAlpha) {
        this.mustntStartWithNonAlpha = mustntStartWithNonAlpha;
    }

    public boolean isNonAlphanumericRequired() {
        return nonAlphanumericRequired;
    }

    public void setNonAlphanumericRequired(final boolean nonAlphanumericRequired) {
        this.nonAlphanumericRequired = nonAlphanumericRequired;
    }

    public boolean isUppercaseRequired() {
        return uppercaseRequired;
    }

    public void setUppercaseRequired(final boolean uppercaseRequired) {
        this.uppercaseRequired = uppercaseRequired;
    }

    public boolean isAlphanumericRequired() {
        return alphanumericRequired;
    }

    public void setAlphanumericRequired(final boolean alphanumericRequired) {
        this.alphanumericRequired = alphanumericRequired;
    }

    public boolean isMustEndWithAlpha() {
        return mustEndWithAlpha;
    }

    public void setMustEndWithAlpha(final boolean mustEndWithAlpha) {
        this.mustEndWithAlpha = mustEndWithAlpha;
    }

    public boolean isMustStartWithAlpha() {
        return mustStartWithAlpha;
    }

    public void setMustStartWithAlpha(final boolean mustStartWithAlpha) {
        this.mustStartWithAlpha = mustStartWithAlpha;
    }

    public boolean isMustntEndWithAlpha() {
        return mustntEndWithAlpha;
    }

    public void setMustntEndWithAlpha(final boolean mustntEndWithAlpha) {
        this.mustntEndWithAlpha = mustntEndWithAlpha;
    }

    public boolean isMustntStartWithAlpha() {
        return mustntStartWithAlpha;
    }

    public void setMustntStartWithAlpha(final boolean mustntStartWithAlpha) {
        this.mustntStartWithAlpha = mustntStartWithAlpha;
    }

    public boolean isUsernameAllowed() {
        return usernameAllowed;
    }

    public void setUsernameAllowed(final boolean usernameAllowed) {
        this.usernameAllowed = usernameAllowed;
    }

    @XmlElementWrapper(name = "wordsNotPermitted")
    @XmlElement(name = "word")
    @JsonProperty("wordsNotPermitted")
    public List<String> getWordsNotPermitted() {
        return wordsNotPermitted;
    }

    @XmlElementWrapper(name = "prefixesNotPermitted")
    @XmlElement(name = "prefix")
    @JsonProperty("prefixesNotPermitted")
    public List<String> getPrefixesNotPermitted() {
        return prefixesNotPermitted;
    }

    @XmlElementWrapper(name = "schemasNotPermitted")
    @XmlElement(name = "schema")
    @JsonProperty("schemasNotPermitted")
    public List<String> getSchemasNotPermitted() {
        return schemasNotPermitted;
    }

    @XmlElementWrapper(name = "suffixesNotPermitted")
    @XmlElement(name = "suffix")
    @JsonProperty("suffixesNotPermitted")
    public List<String> getSuffixesNotPermitted() {
        return suffixesNotPermitted;
    }
}
