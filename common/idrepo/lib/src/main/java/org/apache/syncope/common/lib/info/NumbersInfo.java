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

import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;

public record NumbersInfo(
        long totalUsers,
        Map<String, Long> usersByRealm,
        Map<String, Long> usersByStatus,
        long totalGroups,
        Map<String, Long> groupsByRealm,
        String anyType1,
        long totalAny1,
        Map<String, Long> any1ByRealm,
        String anyType2,
        long totalAny2,
        Map<String, Long> any2ByRealm,
        long totalResources,
        long totalRoles,
        Map<String, Boolean> confCompleteness) implements BaseBean {

    private static final long serialVersionUID = 7691187370598649583L;

    public enum ConfItem {

        RESOURCE(20),
        ACCOUNT_POLICY(10),
        PASSWORD_POLICY(10),
        NOTIFICATION(8),
        PULL_TASK(10),
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
}
