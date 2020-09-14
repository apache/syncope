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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllowedAttrReleasePolicyConf implements AttrReleasePolicyConf {

    private static final long serialVersionUID = -1969836661359025380L;

    /**
     * Specify the list of allowed attribute to release.
     * Use the special {@code *} to release everything.
     */
    @JacksonXmlElementWrapper(localName = "allowedAttrs")
    @JacksonXmlProperty(localName = "allowedAttrs")
    @JsonProperty("allowedAttrs")
    private final List<String> allowedAttrs = new ArrayList<>();

    private ConsentPolicy consentPolicy;

    public List<String> getAllowedAttrs() {
        return allowedAttrs;
    }

    public ConsentPolicy getConsentPolicy() {
        return consentPolicy;
    }

    public void setConsentPolicy(final ConsentPolicy consentPolicy) {
        this.consentPolicy = consentPolicy;
    }

    public class ConsentPolicy {

        private Boolean status = null;

        @JacksonXmlElementWrapper(localName = "excludedAttributes")
        @JacksonXmlProperty(localName = "excludedAttributes")
        @JsonProperty("excludedAttributes")
        private final Set<String> excludedAttrs = new HashSet<>();

        @JacksonXmlElementWrapper(localName = "includeOnlyAttrs")
        @JacksonXmlProperty(localName = "includeOnlyAttrs")
        @JsonProperty("includeOnlyAttrs")
        private final Set<String> includeOnlyAttrs = new HashSet<>();

        public Boolean getStatus() {
            return status;
        }

        public void setStatus(final Boolean status) {
            this.status = status;
        }

        public Set<String> getExcludedAttrs() {
            return excludedAttrs;
        }

        public void addExcludedAttr(final String attr) {
            excludedAttrs.add(attr);
        }

        public Set<String> getIncludeOnlyAttrs() {
            return includeOnlyAttrs;
        }

        public void addIncludeOnlyAttribute(final String attr) {
            includeOnlyAttrs.add(attr);
        }

    }

}
