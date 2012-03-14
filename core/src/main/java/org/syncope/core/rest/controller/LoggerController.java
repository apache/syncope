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
package org.syncope.core.rest.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.LoggerTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.audit.AuditManager;
import org.syncope.core.persistence.beans.SyncopeLogger;
import org.syncope.core.persistence.dao.LoggerDAO;
import org.syncope.types.AuditElements.Category;
import org.syncope.types.AuditElements.LoggerSubCategory;
import org.syncope.types.AuditElements.Result;
import org.syncope.types.SyncopeClientExceptionType;
import org.syncope.types.SyncopeLoggerLevel;
import org.syncope.types.SyncopeLoggerType;

@Controller
@RequestMapping("/logger")
public class LoggerController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private LoggerDAO loggerDAO;

    private List<LoggerTO> list(final SyncopeLoggerType type) {
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
    @RequestMapping(method = RequestMethod.GET, value = "/log/list")
    @Transactional(readOnly = true)
    public List<LoggerTO> listLogs() {
        return list(SyncopeLoggerType.LOG);
    }

    @PreAuthorize("hasRole('AUDIT_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/audit/list")
    @Transactional(readOnly = true)
    public List<LoggerTO> listAudits() {
        return list(SyncopeLoggerType.AUDIT);
    }

    private void throwInvalidLogger(final SyncopeLoggerType type) {
        SyncopeClientCompositeErrorException sccee = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.InvalidLogger);
        sce.addElement("Expected " + type.name());

        throw sccee;
    }

    private LoggerTO setLevel(final String name, final Level level, final SyncopeLoggerType expectedType) {
        SyncopeLogger syncopeLogger = loggerDAO.find(name);
        if (syncopeLogger == null) {
            LOG.debug("Logger {} not found: creating new...", name);

            syncopeLogger = new SyncopeLogger();
            syncopeLogger.setName(name);
            syncopeLogger.setType(name.startsWith(SyncopeLoggerType.AUDIT.getPrefix())
                    ? SyncopeLoggerType.AUDIT : SyncopeLoggerType.LOG);
        }

        if (expectedType != syncopeLogger.getType()) {
            throwInvalidLogger(expectedType);
        }

        syncopeLogger.setLevel(SyncopeLoggerLevel.fromLevel(level));
        syncopeLogger = loggerDAO.save(syncopeLogger);

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = lc.getLogger(name);
        logger.setLevel(level);

        LoggerTO result = new LoggerTO();
        BeanUtils.copyProperties(syncopeLogger, result);

        auditManager.audit(Category.logger, LoggerSubCategory.setLevel, Result.success,
                String.format("Successfully set level %s to logger %s (%s)", level, name, expectedType));

        return result;
    }

    @PreAuthorize("hasRole('LOG_SET_LEVEL')")
    @RequestMapping(method = RequestMethod.POST, value = "/log/{name}/{level}")
    public LoggerTO setLogLevel(@PathVariable("name") final String name, @PathVariable("level") final Level level) {
        return setLevel(name, level, SyncopeLoggerType.LOG);
    }

    @PreAuthorize("hasRole('AUDIT_SET_LEVEL')")
    @RequestMapping(method = RequestMethod.POST, value = "/audit/{name}/{level}")
    public LoggerTO setAuditLevel(@PathVariable("name") final String name, @PathVariable("level") final Level level) {
        return setLevel(name, level, SyncopeLoggerType.AUDIT);
    }

    private void delete(final String name, final SyncopeLoggerType expectedType)
            throws NotFoundException {

        SyncopeLogger syncopeLogger = loggerDAO.find(name);
        if (syncopeLogger == null) {
            throw new NotFoundException("Logger " + name);
        } else if (expectedType != syncopeLogger.getType()) {
            throwInvalidLogger(expectedType);
        }

        // remove SyncopeLogger from local storage, so that LoggerLoader won't load this next time
        loggerDAO.delete(syncopeLogger);

        // set log level to OFF in order to disable configured logger until next reboot
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = lc.getLogger(name);
        logger.setLevel(Level.OFF);

        auditManager.audit(Category.logger, LoggerSubCategory.setLevel, Result.success,
                String.format("Successfully deleted logger %s (%s)", name, expectedType));
    }

    @PreAuthorize("hasRole('LOG_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/log/delete/{name}")
    public void deleteLog(@PathVariable("name") final String name)
            throws NotFoundException {

        delete(name, SyncopeLoggerType.LOG);
    }

    @PreAuthorize("hasRole('AUDIT_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/audit/delete/{name}")
    public void deleteAudit(@PathVariable("name") final String name)
            throws NotFoundException {

        delete(name, SyncopeLoggerType.AUDIT);
    }
}
