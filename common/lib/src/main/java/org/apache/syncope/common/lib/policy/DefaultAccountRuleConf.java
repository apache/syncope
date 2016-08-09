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

@XmlRootElement(name = "defaultAccountRuleConf")
@XmlType
public class DefaultAccountRuleConf extends AbstractAccountRuleConf implements AccountRuleConf {

    private static final long serialVersionUID = 3259256974414758406L;

    /**
     * Minimum length.
     */
    private int maxLength;

    /**
     * Maximum length.
     */
    private int minLength;

    /**
     * Pattern (regular expression) that must match.
     */
    private String pattern;

    /**
     * Specify if one or more lowercase characters are permitted.
     */
    private boolean allUpperCase;

    /**
     * Specify if one or more uppercase characters are permitted.
     */
    private boolean allLowerCase;

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

    public boolean isAllLowerCase() {
        return allLowerCase;
    }

    public void setAllLowerCase(final boolean allLowerCase) {
        this.allLowerCase = allLowerCase;
    }

    public boolean isAllUpperCase() {
        return allUpperCase;
    }

    public void setAllUpperCase(final boolean allUpperCase) {
        this.allUpperCase = allUpperCase;
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

    public String getPattern() {
        return pattern;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern;
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
