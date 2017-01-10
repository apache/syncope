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
package org.apache.syncope.core.rest.cxf.service;

import java.text.ParseException;
import java.util.List;
import javax.ws.rs.BadRequestException;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.rest.api.LoggerWrapper;
import org.apache.syncope.common.rest.api.service.LoggerService;
import org.apache.syncope.core.logic.LoggerLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoggerServiceImpl extends AbstractServiceImpl implements LoggerService {

    @Autowired
    private LoggerLogic logic;

    @Override
    public List<LogAppender> memoryAppenders() {
        return logic.memoryAppenders();
    }

    @Override
    public List<LogStatementTO> getLastLogStatements(final String memoryAppender) {
        return logic.getLastLogStatements(memoryAppender);
    }

    @Override
    public void delete(final LoggerType type, final String name) {
        switch (type) {
            case LOG:
                logic.deleteLog(name);
                break;

            case AUDIT:
                try {
                    logic.disableAudit(AuditLoggerName.fromLoggerName(name));
                } catch (IllegalArgumentException | ParseException e) {
                    throw new BadRequestException(e);
                }
                break;

            default:
                throw new BadRequestException();
        }

    }

    @Override
    public List<LoggerTO> list(final LoggerType type) {
        switch (type) {
            case LOG:
                return logic.listLogs();

            case AUDIT:
                List<AuditLoggerName> auditLogger = logic.listAudits();
                return LoggerWrapper.unwrap(auditLogger);

            default:
                throw new BadRequestException();
        }
    }

    @Override
    public LoggerTO read(final LoggerType type, final String name) {
        switch (type) {
            case LOG:
                return logic.readLog(name);

            case AUDIT:
                return logic.readAudit(name);

            default:
                throw new BadRequestException();
        }
    }

    @Override
    public void update(final LoggerType type, final LoggerTO logger) {
        switch (type) {
            case LOG:
                logic.setLogLevel(logger.getKey(), logger.getLevel().getLevel());
                break;

            case AUDIT:
                try {
                    logic.enableAudit(AuditLoggerName.fromLoggerName(logger.getKey()));
                } catch (Exception e) {
                    throw new BadRequestException(e);
                }
                break;

            default:
                throw new BadRequestException();
        }
    }

    @Override
    public List<EventCategoryTO> events() {
        return logic.listAuditEvents();
    }
}
