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

    public class TaskExecutorInfo {

        private int size;

        private int active;

        private int queued;

        private int completed;

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }

        public int getActive() {
            return active;
        }

        public void setActive(final int active) {
            this.active = active;
        }

        public int getQueued() {
            return queued;
        }

        public void setQueued(final int queued) {
            this.queued = queued;
        }

        public int getCompleted() {
            return completed;
        }

        public void setCompleted(final int completed) {
            this.completed = completed;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().
                    append(size).
                    append(active).
                    append(queued).
                    append(completed).
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
            final TaskExecutorInfo other = (TaskExecutorInfo) obj;
            return new EqualsBuilder().
                    append(size, other.size).
                    append(active, other.active).
                    append(queued, other.queued).
                    append(completed, other.completed).
                    build();
        }
    }

    private int totalUsers;

    private final Map<String, Integer> usersByRealm = new HashMap<>();

    private final Map<String, Integer> usersByStatus = new HashMap<>();

    private int totalGroups;

    private final Map<String, Integer> groupsByRealm = new HashMap<>();

    private String anyType1;

    private Integer totalAny1;

    private final Map<String, Integer> any1ByRealm = new HashMap<>();

    private String anyType2;

    private Integer totalAny2;

    private final Map<String, Integer> any2ByRealm = new HashMap<>();

    private int totalResources;

    private int totalRoles;

    private final Map<String, Boolean> confCompleteness = new HashMap<>();

    private final TaskExecutorInfo asyncConnectorExecutor = new TaskExecutorInfo();

    private final TaskExecutorInfo propagationTaskExecutor = new TaskExecutorInfo();

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

    public Map<String, Integer> getUsersByRealm() {
        return usersByRealm;
    }

    public Map<String, Integer> getUsersByStatus() {
        return usersByStatus;
    }

    public Map<String, Integer> getGroupsByRealm() {
        return groupsByRealm;
    }

    public Map<String, Integer> getAny1ByRealm() {
        return any1ByRealm;
    }

    public Map<String, Integer> getAny2ByRealm() {
        return any2ByRealm;
    }

    public Map<String, Boolean> getConfCompleteness() {
        return confCompleteness;
    }

    public TaskExecutorInfo getAsyncConnectorExecutor() {
        return asyncConnectorExecutor;
    }

    public TaskExecutorInfo getPropagationTaskExecutor() {
        return propagationTaskExecutor;
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
                append(asyncConnectorExecutor).
                append(propagationTaskExecutor).
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
                append(asyncConnectorExecutor, other.asyncConnectorExecutor).
                append(propagationTaskExecutor, other.propagationTaskExecutor).
                build();
    }
}
