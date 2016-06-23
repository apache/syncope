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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;

@XmlRootElement(name = "groupReportletConf")
@XmlType
public class GroupReportletConf extends AbstractAnyReportletConf {

    private static final long serialVersionUID = -8488503068032439699L;

    @XmlEnum
    @XmlType(name = "groupReportletConfFeature")
    public enum Feature {

        key,
        name,
        groupOwner,
        userOwner,
        users,
        resources

    }

    @Schema(anyTypeKind = AnyTypeKind.GROUP, type = { SchemaType.PLAIN })
    private final List<String> plainAttrs = new ArrayList<>();

    @Schema(anyTypeKind = AnyTypeKind.GROUP, type = { SchemaType.DERIVED })
    private final List<String> derAttrs = new ArrayList<>();

    @Schema(anyTypeKind = AnyTypeKind.GROUP, type = { SchemaType.VIRTUAL })
    private final List<String> virAttrs = new ArrayList<>();

    @SearchCondition(type = "GROUP")
    protected String matchingCond;

    private final List<Feature> features = new ArrayList<>();

    public String getMatchingCond() {
        return matchingCond;
    }

    public void setMatchingCond(final String matchingCond) {
        this.matchingCond = matchingCond;
    }

    @XmlElementWrapper(name = "plainAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("plainAttrs")
    public List<String> getPlainAttrs() {
        return plainAttrs;
    }

    @XmlElementWrapper(name = "derAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("derAttrs")
    public List<String> getDerAttrs() {
        return derAttrs;
    }

    @XmlElementWrapper(name = "virAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrs")
    public List<String> getVirAttrs() {
        return virAttrs;
    }

    public GroupReportletConf() {
        super();
    }

    public GroupReportletConf(final String name) {
        super(name);
    }

    @XmlElementWrapper(name = "features")
    @XmlElement(name = "feature")
    @JsonProperty("features")
    public List<Feature> getFeatures() {
        return features;
    }
}
