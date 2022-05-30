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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.Schema;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;

public class DefaultPasswordRuleConf extends AbstractPasswordRuleConf {

    private static final long serialVersionUID = -7988778083915548547L;

    private int maxLength;

    private int minLength;

    private int alphabetical;

    private int uppercase;

    private int lowercase;

    private int digit;

    private int special;

    private final List<Character> specialChars = new ArrayList<>();

    private final List<Character> illegalChars = new ArrayList<>();

    private int repeatSame;

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

    public int getAlphabetical() {
        return alphabetical;
    }

    public void setAlphabetical(final int alphabetical) {
        this.alphabetical = alphabetical;
    }

    public int getUppercase() {
        return uppercase;
    }

    public void setUppercase(final int uppercase) {
        this.uppercase = uppercase;
    }

    public int getLowercase() {
        return lowercase;
    }

    public void setLowercase(final int lowercase) {
        this.lowercase = lowercase;
    }

    public int getDigit() {
        return digit;
    }

    public void setDigit(final int digit) {
        this.digit = digit;
    }

    public int getSpecial() {
        return special;
    }

    public void setSpecial(final int special) {
        this.special = special;
    }

    @JacksonXmlElementWrapper(localName = "specialChars")
    @JacksonXmlProperty(localName = "char")
    public List<Character> getSpecialChars() {
        return specialChars;
    }

    @JacksonXmlElementWrapper(localName = "illegalChars")
    @JacksonXmlProperty(localName = "char")
    public List<Character> getIllegalChars() {
        return illegalChars;
    }

    public int getRepeatSame() {
        return repeatSame;
    }

    public void setRepeatSame(final int repeatSame) {
        this.repeatSame = repeatSame;
    }

    public boolean isUsernameAllowed() {
        return usernameAllowed;
    }

    public void setUsernameAllowed(final boolean usernameAllowed) {
        this.usernameAllowed = usernameAllowed;
    }

    @JacksonXmlElementWrapper(localName = "wordsNotPermitted")
    @JacksonXmlProperty(localName = "word")
    public List<String> getWordsNotPermitted() {
        return wordsNotPermitted;
    }

    @JacksonXmlElementWrapper(localName = "schemasNotPermitted")
    @JacksonXmlProperty(localName = "schema")
    public List<String> getSchemasNotPermitted() {
        return schemasNotPermitted;
    }
}
