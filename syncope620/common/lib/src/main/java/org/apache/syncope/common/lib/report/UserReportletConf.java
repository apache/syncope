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
package org.apache.syncope.common.lib.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.annotation.FormAttributeField;
import org.apache.syncope.common.lib.types.IntMappingType;

@XmlRootElement(name = "userReportletConf")
@XmlType
public class UserReportletConf extends AbstractReportletConf {

    @XmlEnum
    @XmlType(name = "userReportletConfFeature")
    public enum Feature {

        key,
        username,
        workflowId,
        status,
        creationDate,
        lastLoginDate,
        changePwdDate,
        passwordHistorySize,
        failedLoginCount,
        memberships,
        resources

    }

    private static final long serialVersionUID = 6602717600064602764L;

    @FormAttributeField(userSearch = true)
    private String matchingCond;

    @FormAttributeField(schema = IntMappingType.UserPlainSchema)
    private final List<String> attrs = new ArrayList<>();

    @FormAttributeField(schema = IntMappingType.UserDerivedSchema)
    private final List<String> derAttrs = new ArrayList<>();

    @FormAttributeField(schema = IntMappingType.UserVirtualSchema)
    private final List<String> virAttrs = new ArrayList<>();

    private final List<Feature> features = new ArrayList<>();

    public UserReportletConf() {
        super();
    }

    public UserReportletConf(final String name) {
        super(name);
    }

    @XmlElementWrapper(name = "plainAttributes")
    @XmlElement(name = "plainAttribute")
    @JsonProperty("plainAttributes")
    public List<String> getPlainAttrs() {
        return attrs;
    }

    @XmlElementWrapper(name = "derivedAttributes")
    @XmlElement(name = "attribute")
    @JsonProperty("derivedAttributes")
    public List<String> getDerAttrs() {
        return derAttrs;
    }

    @XmlElementWrapper(name = "virtualAttributes")
    @XmlElement(name = "attribute")
    @JsonProperty("virtualAttributes")
    public List<String> getVirAttrs() {
        return virAttrs;
    }

    @XmlElementWrapper(name = "features")
    @XmlElement(name = "feature")
    @JsonProperty("features")
    public List<Feature> getFeatures() {
        return features;
    }

    public String getMatchingCond() {
        return matchingCond;
    }

    public void setMatchingCond(final String matchingCond) {
        this.matchingCond = matchingCond;
    }
}
