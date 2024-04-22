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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(allOf = { PolicyTO.class })
public class AccountPolicyTO extends PolicyTO implements ComposablePolicy {

    private static final long serialVersionUID = -1557150042828800134L;

    private boolean propagateSuspension;

    private int maxAuthenticationAttempts;

    private final List<String> rules = new ArrayList<>();

    private final List<String> passthroughResources = new ArrayList<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.policy.AccountPolicyTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public boolean isPropagateSuspension() {
        return propagateSuspension;
    }

    public void setPropagateSuspension(final boolean propagateSuspension) {
        this.propagateSuspension = propagateSuspension;
    }

    public int getMaxAuthenticationAttempts() {
        return maxAuthenticationAttempts;
    }

    public void setMaxAuthenticationAttempts(final int maxAuthenticationAttempts) {
        this.maxAuthenticationAttempts = maxAuthenticationAttempts;
    }

    @JacksonXmlElementWrapper(localName = "rules")
    @JacksonXmlProperty(localName = "rule")
    @Override
    public List<String> getRules() {
        return rules;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    @JacksonXmlElementWrapper(localName = "passthroughResources")
    @JacksonXmlProperty(localName = "passthroughResource")
    public List<String> getPassthroughResources() {
        return passthroughResources;
    }
}
