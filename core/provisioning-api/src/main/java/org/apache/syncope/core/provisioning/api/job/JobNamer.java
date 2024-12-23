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
package org.apache.syncope.core.provisioning.api.job;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobNamer {

    private static final Logger LOG = LoggerFactory.getLogger(JobNamer.class);

    private static Optional<String> getKeyFromJobName(final String name, final String pattern, final int prefixLength) {
        String result = null;

        Matcher jobMatcher = Pattern.compile(pattern).matcher(name);
        if (jobMatcher.matches()) {
            try {
                result = name.substring(prefixLength);
            } catch (IllegalArgumentException e) {
                LOG.error("Unparsable id: {}", name.substring(prefixLength), e);
            }
        }

        return Optional.ofNullable(result);
    }

    public static Optional<String> getTaskKeyFromJobName(final String name) {
        return getKeyFromJobName(name, "taskJob" + SyncopeConstants.UUID_REGEX, 7);
    }

    public static Optional<String> getReportKeyFromJobName(final String name) {
        return getKeyFromJobName(name, "reportJob" + SyncopeConstants.UUID_REGEX, 9);
    }

    public static String getJobName(final Task<?> task) {
        return "taskJob" + task.getKey();
    }

    public static String getJobName(final Report report) {
        return "reportJob" + report.getKey();
    }

    private JobNamer() {
        // private constructor for static utility class
    }
}
