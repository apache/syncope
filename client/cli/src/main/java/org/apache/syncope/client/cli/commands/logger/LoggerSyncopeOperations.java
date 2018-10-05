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
package org.apache.syncope.client.cli.commands.logger;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatement;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.rest.api.service.LoggerService;

public class LoggerSyncopeOperations {

    private final LoggerService loggerService = SyncopeServices.get(LoggerService.class);

    public List<String> listMemoryAppenders() {
        return loggerService.memoryAppenders().stream().map(LogAppender::getName).collect(Collectors.toList());
    }

    public List<LogStatement> getLastLogStatements(final String appender) {
        return loggerService.getLastLogStatements(appender);
    }

    public List<LoggerTO> list() {
        return loggerService.list(LoggerType.LOG);
    }

    public void update(final LoggerTO loggerTO) {
        loggerService.update(LoggerType.LOG, loggerTO);
    }

    public LoggerTO read(final String loggerName) {
        return loggerService.read(LoggerType.LOG, loggerName);
    }

    public void delete(final String loggerName) {
        loggerService.delete(LoggerType.LOG, loggerName);
    }
}
