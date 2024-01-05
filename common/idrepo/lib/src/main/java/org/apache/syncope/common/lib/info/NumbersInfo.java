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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class NumbersInfo implements BaseBean {

    private static final long serialVersionUID = 7691187370598649583L;

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

    private long totalUsers;

    private final Map<String, Long> usersByRealm = new HashMap<>();

    private final Map<String, Long> usersByStatus = new HashMap<>();

    private long totalGroups;

    private final Map<String, Long> groupsByRealm = new HashMap<>();

    private String anyType1;

    private Long totalAny1;

    private final Map<String, Long> any1ByRealm = new HashMap<>();

    private String anyType2;

    private Long totalAny2;

    private final Map<String, Long> any2ByRealm = new HashMap<>();

    private long totalResources;

    private long totalRoles;

    private final Map<String, Boolean> confCompleteness = new HashMap<>();

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(final long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalGroups() {
        return totalGroups;
    }

    public void setTotalGroups(final long totalGroups) {
        this.totalGroups = totalGroups;
    }

    public String getAnyType1() {
        return anyType1;
    }

    public void setAnyType1(final String anyType1) {
        this.anyType1 = anyType1;
    }

    public Long getTotalAny1() {
        return totalAny1;
    }

    public void setTotalAny1(final Long totalAny1) {
        this.totalAny1 = totalAny1;
    }

    public String getAnyType2() {
        return anyType2;
    }

    public void setAnyType2(final String anyType2) {
        this.anyType2 = anyType2;
    }

    public Long getTotalAny2() {
        return totalAny2;
    }

    public void setTotalAny2(final Long totalAny2) {
        this.totalAny2 = totalAny2;
    }

    public long getTotalResources() {
        return totalResources;
    }

    public void setTotalResources(final long totalResources) {
        this.totalResources = totalResources;
    }

    public long getTotalRoles() {
        return totalRoles;
    }

    public void setTotalRoles(final long totalRoles) {
        this.totalRoles = totalRoles;
    }

    public Map<String, Long> getUsersByRealm() {
        return usersByRealm;
    }

    public Map<String, Long> getUsersByStatus() {
        return usersByStatus;
    }

    public Map<String, Long> getGroupsByRealm() {
        return groupsByRealm;
    }

    public Map<String, Long> getAny1ByRealm() {
        return any1ByRealm;
    }

    public Map<String, Long> getAny2ByRealm() {
        return any2ByRealm;
    }

    public Map<String, Boolean> getConfCompleteness() {
        return confCompleteness;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(totalUsers).
                append(usersByRealm).
                append(usersByStatus).
                append(totalGroups).
                append(groupsByRealm).
                append(anyType1).
                append(totalAny1).
                append(any1ByRealm).
                append(anyType2).
                append(totalAny2).
                append(any2ByRealm).
                append(totalResources).
                append(totalRoles).
                append(confCompleteness).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NumbersInfo other = (NumbersInfo) obj;
        return new EqualsBuilder().
                append(totalUsers, other.totalUsers).
                append(totalGroups, other.totalGroups).
                append(totalResources, other.totalResources).
                append(totalRoles, other.totalRoles).
                append(anyType1, other.anyType1).
                append(anyType2, other.anyType2).
                append(usersByRealm, other.usersByRealm).
                append(usersByStatus, other.usersByStatus).
                append(groupsByRealm, other.groupsByRealm).
                append(totalAny1, other.totalAny1).
                append(any1ByRealm, other.any1ByRealm).
                append(totalAny2, other.totalAny2).
                append(any2ByRealm, other.any2ByRealm).
                append(confCompleteness, other.confCompleteness).
                build();
    }
}
