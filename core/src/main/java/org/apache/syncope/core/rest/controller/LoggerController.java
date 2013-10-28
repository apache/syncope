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
package org.apache.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.LoggerSubCategory;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.types.LoggerType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.SyncopeLogger;
import org.apache.syncope.core.persistence.dao.LoggerDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.common.util.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class LoggerController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private LoggerDAO loggerDAO;

    private List<LoggerTO> list(final LoggerType type) {
        List<LoggerTO> result = new ArrayList<LoggerTO>();
        for (SyncopeLogger syncopeLogger : loggerDAO.findAll(type)) {
            LoggerTO loggerTO = new LoggerTO();
            BeanUtils.copyProperties(syncopeLogger, loggerTO);
            result.add(loggerTO);
        }

        auditManager.audit(Category.logger, LoggerSubCategory.list, Result.success,
                "Successfully listed all loggers (" + type + "): " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('LOG_LIST')")
    @Transactional(readOnly = true)
    public List<LoggerTO> listLogs() {
        return list(LoggerType.LOG);
    }

    @PreAuthorize("hasRole('AUDIT_LIST')")
    @Transactional(readOnly = true)
    public List<AuditLoggerName> listAudits() {
        List<AuditLoggerName> result = new ArrayList<AuditLoggerName>();

        for (LoggerTO logger : list(LoggerType.AUDIT)) {
            try {
                result.add(AuditLoggerName.fromLoggerName(logger.getName()));
            } catch (Exception e) {
                LOG.error("Unexpected audit logger name: {}", logger.getName(), e);
            }
        }

        return result;
    }

    private void throwInvalidLogger(final LoggerType type) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidLogger);
        sce.getElements().add("Expected " + type.name());

        throw sce;
    }

    private LoggerTO setLevel(final String name, final Level level, final LoggerType expectedType) {
        SyncopeLogger syncopeLogger = loggerDAO.find(name);
        if (syncopeLogger == null) {
            LOG.debug("Logger {} not found: creating new...", name);

            syncopeLogger = new SyncopeLogger();
            syncopeLogger.setName(name);
            syncopeLogger.setType(name.startsWith(LoggerType.AUDIT.getPrefix())
                    ? LoggerType.AUDIT
                    : LoggerType.LOG);
        }

        if (expectedType != syncopeLogger.getType()) {
            throwInvalidLogger(expectedType);
        }

        syncopeLogger.setLevel(LoggerLevel.fromLevel(level));
        syncopeLogger = loggerDAO.save(syncopeLogger);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig logConf = SyncopeConstants.ROOT_LOGGER.equals(name)
                ? ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
                : ctx.getConfiguration().getLoggerConfig(name);
        logConf.setLevel(level);
        ctx.updateLoggers();

        LoggerTO result = new LoggerTO();
        BeanUtils.copyProperties(syncopeLogger, result);

        auditManager.audit(Category.logger, LoggerSubCategory.setLevel, Result.success,
                String.format("Successfully set level %s to logger %s (%s)", level, name, expectedType));

        return result;
    }

    @PreAuthorize("hasRole('LOG_SET_LEVEL')")
    public LoggerTO setLogLevel(final String name, final Level level) {
        return setLevel(name, level, LoggerType.LOG);
    }

    @PreAuthorize("hasRole('AUDIT_ENABLE')")
    public void enableAudit(final AuditLoggerName auditLoggerName) {
        try {
            setLevel(auditLoggerName.toLoggerName(), Level.DEBUG, LoggerType.AUDIT);
        } catch (IllegalArgumentException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidLogger);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    private LoggerTO delete(final String name, final LoggerType expectedType) throws NotFoundException {
        SyncopeLogger syncopeLogger = loggerDAO.find(name);
        if (syncopeLogger == null) {
            throw new NotFoundException("Logger " + name);
        } else if (expectedType != syncopeLogger.getType()) {
            throwInvalidLogger(expectedType);
        }

        LoggerTO loggerToDelete = new LoggerTO();
        BeanUtils.copyProperties(syncopeLogger, loggerToDelete);

        // remove SyncopeLogger from local storage, so that LoggerLoader won't load this next time
        loggerDAO.delete(syncopeLogger);

        // set log level to OFF in order to disable configured logger until next reboot
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Logger logger = SyncopeConstants.ROOT_LOGGER.equals(name)
                ? ctx.getLogger(LogManager.ROOT_LOGGER_NAME) : ctx.getLogger(name);
        logger.setLevel(Level.OFF);
        ctx.updateLoggers();

        auditManager.audit(Category.logger, LoggerSubCategory.setLevel, Result.success, String.format(
                "Successfully deleted logger %s (%s)", name, expectedType));

        return loggerToDelete;
    }

    @PreAuthorize("hasRole('LOG_DELETE')")
    public LoggerTO deleteLog(final String name) throws NotFoundException {
        return delete(name, LoggerType.LOG);
    }

    @PreAuthorize("hasRole('AUDIT_DISABLE')")
    public void disableAudit(final AuditLoggerName auditLoggerName) {
        try {
            delete(auditLoggerName.toLoggerName(), LoggerType.AUDIT);
        } catch (NotFoundException e) {
            LOG.debug("Ignoring disable of non existing logger {}", auditLoggerName.toLoggerName());
        } catch (IllegalArgumentException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidLogger);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }
}
