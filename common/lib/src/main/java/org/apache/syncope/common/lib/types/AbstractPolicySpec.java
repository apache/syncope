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
package org.apache.syncope.common.lib.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@XmlType
public abstract class AbstractPolicySpec implements PolicySpec {

    private static final long serialVersionUID = -6210646284287392063L;

    /**
     * Substrings not permitted.
     */
    private final List<String> wordsNotPermitted = new ArrayList<>();

    /**
     * User attribute values not permitted.
     */
    protected final List<String> schemasNotPermitted = new ArrayList<>();

    /**
     * Substrings not permitted as prefix.
     */
    protected final List<String> prefixesNotPermitted = new ArrayList<>();

    /**
     * Substrings not permitted as suffix.
     */
    protected final List<String> suffixesNotPermitted = new ArrayList<>();

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

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

}
