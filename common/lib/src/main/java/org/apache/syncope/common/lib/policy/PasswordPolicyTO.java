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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "passwordPolicy")
@XmlType
@Schema(allOf = { PolicyTO.class })
public class PasswordPolicyTO extends PolicyTO implements ComposablePolicy {

    private static final long serialVersionUID = -5606086441294799690L;

    private boolean allowNullPassword;

    private int historyLength;

    private final List<String> rules = new ArrayList<>();

    @XmlTransient
    @JsonProperty("@class")
    @Schema(name = "@class", required = true, example = "org.apache.syncope.common.lib.policy.PasswordPolicyTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public boolean isAllowNullPassword() {
        return allowNullPassword;
    }

    public void setAllowNullPassword(final boolean allowNullPassword) {
        this.allowNullPassword = allowNullPassword;
    }

    public int getHistoryLength() {
        return historyLength;
    }

    public void setHistoryLength(final int historyLength) {
        this.historyLength = historyLength;
    }

    @XmlElementWrapper(name = "rules")
    @XmlElement(name = "rule")
    @JsonProperty("rules")
    @Override
    public List<String> getRules() {
        return rules;
    }
}
