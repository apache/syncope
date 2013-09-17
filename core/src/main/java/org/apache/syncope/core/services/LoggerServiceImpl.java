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
package org.apache.syncope.core.services;

import java.text.ParseException;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.syncope.common.services.LoggerService;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.LoggerType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.LoggerController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoggerServiceImpl extends AbstractServiceImpl implements LoggerService, ContextAware {

    @Autowired
    private LoggerController controller;

    @Override
    public void delete(final LoggerType type, final String name) {
        switch (type) {
            case LOG:
                controller.deleteLog(name);
                break;

            case AUDIT:
                try {
                    controller.disableAudit(AuditLoggerName.fromLoggerName(name));
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException(e);
                } catch (ParseException e) {
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
                return controller.listLogs();

            case AUDIT:
                List<AuditLoggerName> auditLogger = controller.listAudits();
                return CollectionWrapper.unwrapLogger(auditLogger);

            default:
                throw new BadRequestException();
        }
    }

    @Override
    public LoggerTO read(final LoggerType type, final String name) {
        List<LoggerTO> logger = list(type);
        for (LoggerTO l : logger) {
            if (l.getName().equals(name)) {
                return l;
            }
        }
        throw new NotFoundException();
    }

    @Override
    public void update(final LoggerType type, final String name, final LoggerTO logger) {
        switch (type) {
            case LOG:
                controller.setLogLevel(name, logger.getLevel().getLevel());
                break;

            case AUDIT:
                try {
                    controller.enableAudit(AuditLoggerName.fromLoggerName(name));
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException(e);
                } catch (ParseException e) {
                    throw new BadRequestException(e);
                }
                break;

            default:
                throw new BadRequestException();
        }
    }
}
