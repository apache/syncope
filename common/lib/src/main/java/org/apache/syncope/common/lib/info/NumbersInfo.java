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
package org.apache.syncope.common.lib.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;

@XmlRootElement(name = "numbersInfo")
@XmlType
public class NumbersInfo extends AbstractBaseBean {

    private static final long serialVersionUID = 7691187370598649583L;

    @XmlEnum
    @XmlType(name = "confItem")
    public enum ConfItem {

        RESOURCE(20),
        ACCOUNT_POLICY(10),
        PASSWORD_POLICY(10),
        NOTIFICATION(8),
        PULL_TASK(10),
        VIR_SCHEMA(10),
        ANY_TYPE(5),
        SECURITY_QUESTION(12),
        ROLE(15);

        private final int score;

        ConfItem(final int score) {
            this.score = score;
        }

        public static int getScore(final String name) {
            int score = 0;
            for (ConfItem value : values()) {
                if (value.name().equals(name)) {
                    score = value.score;
                }
            }
            return score;
        }

    }

    private int totalUsers;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Integer> usersByRealm = new HashMap<>();

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Integer> usersByStatus = new HashMap<>();

    private int totalGroups;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Integer> groupsByRealm = new HashMap<>();

    private String anyType1;

    private Integer totalAny1;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Integer> any1ByRealm = new HashMap<>();

    private String anyType2;

    private Integer totalAny2;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Integer> any2ByRealm = new HashMap<>();

    private int totalResources;

    private int totalRoles;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Boolean> confCompleteness = new HashMap<>();

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(final int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(final int totalGroups) {
        this.totalGroups = totalGroups;
    }

    public String getAnyType1() {
        return anyType1;
    }

    public void setAnyType1(final String anyType1) {
        this.anyType1 = anyType1;
    }

    public Integer getTotalAny1() {
        return totalAny1;
    }

    public void setTotalAny1(final Integer totalAny1) {
        this.totalAny1 = totalAny1;
    }

    public String getAnyType2() {
        return anyType2;
    }

    public void setAnyType2(final String anyType2) {
        this.anyType2 = anyType2;
    }

    public Integer getTotalAny2() {
        return totalAny2;
    }

    public void setTotalAny2(final Integer totalAny2) {
        this.totalAny2 = totalAny2;
    }

    public int getTotalResources() {
        return totalResources;
    }

    public void setTotalResources(final int totalResources) {
        this.totalResources = totalResources;
    }

    public int getTotalRoles() {
        return totalRoles;
    }

    public void setTotalRoles(final int totalRoles) {
        this.totalRoles = totalRoles;
    }

    @JsonProperty
    public Map<String, Integer> getUsersByRealm() {
        return usersByRealm;
    }

    @JsonProperty
    public Map<String, Integer> getUsersByStatus() {
        return usersByStatus;
    }

    @JsonProperty
    public Map<String, Integer> getGroupsByRealm() {
        return groupsByRealm;
    }

    @JsonProperty
    public Map<String, Integer> getAny1ByRealm() {
        return any1ByRealm;
    }

    @JsonProperty
    public Map<String, Integer> getAny2ByRealm() {
        return any2ByRealm;
    }

    @JsonProperty
    public Map<String, Boolean> getConfCompleteness() {
        return confCompleteness;
    }

}
