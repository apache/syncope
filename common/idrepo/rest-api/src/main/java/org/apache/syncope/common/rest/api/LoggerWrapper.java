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
package org.apache.syncope.common.rest.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerLevel;

public final class LoggerWrapper {

    private LoggerWrapper() {
        // empty constructor for static utility class
    }

    public static List<AuditLoggerName> wrap(final Collection<LoggerTO> logger) {
        List<AuditLoggerName> result = new ArrayList<>();
        logger.forEach(loggerTO -> {
            try {
                result.add(AuditLoggerName.fromLoggerName(loggerTO.getKey()));
            } catch (Exception ignore) {
                // ignore
            }
        });
        return result;
    }

    public static List<LoggerTO> unwrap(final Collection<AuditLoggerName> auditNames) {
        return auditNames.stream().map(name -> {
            LoggerTO loggerTO = new LoggerTO();
            loggerTO.setKey(name.toLoggerName());
            loggerTO.setLevel(LoggerLevel.DEBUG);
            return loggerTO;
        }).collect(Collectors.toList());
    }
}
