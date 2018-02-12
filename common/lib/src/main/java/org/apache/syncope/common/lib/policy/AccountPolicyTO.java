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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "accountPolicy")
@XmlType
@ApiModel(parent = PolicyTO.class)
public class AccountPolicyTO extends PolicyTO implements ComposablePolicy<AbstractAccountRuleConf> {

    private static final long serialVersionUID = -1557150042828800134L;

    private boolean propagateSuspension;

    private int maxAuthenticationAttempts;

    private final List<AbstractAccountRuleConf> ruleConfs = new ArrayList<>();

    private final List<String> passthroughResources = new ArrayList<>();

    @XmlTransient
    @JsonProperty("@class")
    @ApiModelProperty(
            name = "@class", required = true, example = "org.apache.syncope.common.lib.policy.AccountPolicyTO")
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

    @XmlElementWrapper(name = "ruleConfs")
    @XmlElement(name = "ruleConf")
    @JsonProperty("ruleConfs")
    @Override
    public List<AbstractAccountRuleConf> getRuleConfs() {
        return ruleConfs;
    }

    @XmlElementWrapper(name = "passthroughResources")
    @XmlElement(name = "passthroughResource")
    @JsonProperty("passthroughResources")
    public List<String> getPassthroughResources() {
        return passthroughResources;
    }
}
