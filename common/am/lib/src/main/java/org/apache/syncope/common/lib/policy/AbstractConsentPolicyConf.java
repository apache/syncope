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
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.BaseBean;

public abstract class AbstractConsentPolicyConf implements BaseBean, ConsentPolicyConf {

    private static final long serialVersionUID = 1153200197344709778L;

    private Boolean status = null;

    @JacksonXmlElementWrapper(localName = "excludedAttributes")
    @JacksonXmlProperty(localName = "excludedAttributes")
    @JsonProperty("excludedAttributes")
    private final Set<String> excludedAttrs = new HashSet<>();

    @JacksonXmlElementWrapper(localName = "includeOnlyAttrs")
    @JacksonXmlProperty(localName = "includeOnlyAttrs")
    @JsonProperty("includeOnlyAttrs")
    private final Set<String> includeOnlyAttrs = new HashSet<>();

    @Override
    public Boolean getStatus() {
        return status;
    }

    public void setStatus(final Boolean status) {
        this.status = status;
    }

    @Override
    public Set<String> getExcludedAttrs() {
        return excludedAttrs;
    }

    public void addExcludedAttr(final String attr) {
        excludedAttrs.add(attr);
    }

    @Override
    public Set<String> getIncludeOnlyAttrs() {
        return includeOnlyAttrs;
    }

    public void addIncludeOnlyAttribute(final String attr) {
        includeOnlyAttrs.add(attr);
    }

}
