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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationReportletConf extends AbstractReportletConf {

    private static final long serialVersionUID = 6602717600064602764L;

    public enum Feature {

        key,
        username,
        groupName,
        status,
        creationDate,
        lastLoginDate,
        changePwdDate,
        passwordHistorySize,
        failedLoginCount;

    }

    @SearchCondition(type = "USER")
    private String userMatchingCond;

    @SearchCondition(type = "GROUP")
    private String groupMatchingCond;

    @SearchCondition(type = "")
    private String anyObjectMatchingCond;

    private final List<Feature> features = new ArrayList<>();

    public ReconciliationReportletConf() {
        super();
    }

    public ReconciliationReportletConf(final String name) {
        super(name);
    }

    public String getUserMatchingCond() {
        return userMatchingCond;
    }

    public void setUserMatchingCond(final String userMatchingCond) {
        this.userMatchingCond = userMatchingCond;
    }

    public String getGroupMatchingCond() {
        return groupMatchingCond;
    }

    public void setGroupMatchingCond(final String groupMatchingCond) {
        this.groupMatchingCond = groupMatchingCond;
    }

    public String getAnyObjectMatchingCond() {
        return anyObjectMatchingCond;
    }

    public void setAnyObjectMatchingCond(final String anyObjectMatchingCond) {
        this.anyObjectMatchingCond = anyObjectMatchingCond;
    }

    @JacksonXmlElementWrapper(localName = "features")
    @JacksonXmlProperty(localName = "feature")
    public List<Feature> getFeatures() {
        return features;
    }
}
