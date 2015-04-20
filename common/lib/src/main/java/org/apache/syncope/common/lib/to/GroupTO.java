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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "group")
@XmlType
@JsonIgnoreProperties({ "displayName" })
public class GroupTO extends AbstractSubjectTO {

    private static final long serialVersionUID = -7785920258290147542L;

    private String name;

    private Long userOwner;

    private Long groupOwner;

    private final List<String> gPlainAttrTemplates = new ArrayList<>();

    private final List<String> gDerAttrTemplates = new ArrayList<>();

    private final List<String> gVirAttrTemplates = new ArrayList<>();

    private final List<String> mPlainAttrTemplates = new ArrayList<>();

    private final List<String> mDerAttrTemplates = new ArrayList<>();

    private final List<String> mVirAttrTemplates = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final Long userOwner) {
        this.userOwner = userOwner;
    }

    public Long getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final Long groupOwner) {
        this.groupOwner = groupOwner;
    }

    @XmlElementWrapper(name = "gPlainAttrTemplates")
    @XmlElement(name = "gPlainAttrTemplate")
    @JsonProperty("gPlainAttrTemplates")
    public List<String> getGPlainAttrTemplates() {
        return gPlainAttrTemplates;
    }

    @XmlElementWrapper(name = "gDerAttrTemplates")
    @XmlElement(name = "gDerAttrTemplate")
    @JsonProperty("gDerAttrTemplates")
    public List<String> getGDerAttrTemplates() {
        return gDerAttrTemplates;
    }

    @XmlElementWrapper(name = "gVirAttrTemplates")
    @XmlElement(name = "gVirAttrTemplate")
    @JsonProperty("gVirAttrTemplates")
    public List<String> getGVirAttrTemplates() {
        return gVirAttrTemplates;
    }

    @XmlElementWrapper(name = "mPlainAttrTemplates")
    @XmlElement(name = "mPlainAttrTemplate")
    @JsonProperty("mPlainAttrTemplates")
    public List<String> getMPlainAttrTemplates() {
        return mPlainAttrTemplates;
    }

    @XmlElementWrapper(name = "mDerAttrTemplates")
    @XmlElement(name = "mDerAttrTemplate")
    @JsonProperty("mDerAttrTemplates")
    public List<String> getMDerAttrTemplates() {
        return mDerAttrTemplates;
    }

    @XmlElementWrapper(name = "mVirAttrTemplates")
    @XmlElement(name = "mVirAttrTemplate")
    @JsonProperty("mVirAttrTemplates")
    public List<String> getMVirAttrTemplates() {
        return mVirAttrTemplates;
    }

}
