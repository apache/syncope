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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.types.AuditLoggerName;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.LoggerLevel;
import org.apache.syncope.common.types.LoggerType;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.util.BeanUtils;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.SyncopeLogger;
import org.apache.syncope.core.persistence.dao.LoggerDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

@Component
public class LoggerController extends AbstractTransactionalController<LoggerTO> {

    @Autowired
    private LoggerDAO loggerDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private TaskDAO taskDAO;

    private List<LoggerTO> list(final LoggerType type) {
        List<LoggerTO> result = new ArrayList<LoggerTO>();
        for (SyncopeLogger syncopeLogger : loggerDAO.findAll(type)) {
            LoggerTO loggerTO = new LoggerTO();
            BeanUtils.copyProperties(syncopeLogger, loggerTO);
            result.add(loggerTO);
        }

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
                LOG.warn("Unexpected audit logger name: {}", logger.getName(), e);
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

    @PreAuthorize("hasRole('AUDIT_LIST') or hasRole('NOTIFICATION_LIST')")
    public List<EventCategoryTO> listAuditEvents() {
        // use set to avoi duplications or null elements
        final Set<EventCategoryTO> events = new HashSet<EventCategoryTO>();

        try {
            final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            final MetadataReaderFactory metadataReaderFactory =
                    new CachingMetadataReaderFactory(resourcePatternResolver);

            final String packageSearchPath =
                    ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + ClassUtils.convertClassNameToResourcePath(
                    SystemPropertyUtils.resolvePlaceholders(this.getClass().getPackage().getName()))
                    + "/" + "**/*.class";

            final Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    final MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    final Class<?> clazz = Class.forName(metadataReader.getClassMetadata().getClassName());

                    if (clazz.isAnnotationPresent(Component.class)
                            && AbstractController.class.isAssignableFrom(clazz)) {
                        final EventCategoryTO eventCategoryTO = new EventCategoryTO();
                        eventCategoryTO.setCategory(clazz.getSimpleName());
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (Modifier.isPublic(method.getModifiers())) {
                                eventCategoryTO.getEvents().add(method.getName());
                            }
                        }
                        events.add(eventCategoryTO);
                    }
                }
            }

            events.add(new EventCategoryTO(EventCategoryType.PROPAGATION));
            events.add(new EventCategoryTO(EventCategoryType.SYNCHRONIZATION));

            for (AttributableType attributableType : AttributableType.values()) {
                for (ExternalResource resource : resourceDAO.findAll()) {
                    final EventCategoryTO propEventCategoryTO = new EventCategoryTO(EventCategoryType.PROPAGATION);
                    final EventCategoryTO syncEventCategoryTO = new EventCategoryTO(EventCategoryType.SYNCHRONIZATION);

                    propEventCategoryTO.setCategory(attributableType.name().toLowerCase());
                    propEventCategoryTO.setSubcategory(resource.getName());

                    syncEventCategoryTO.setCategory(attributableType.name().toLowerCase());
                    syncEventCategoryTO.setSubcategory(resource.getName());

                    for (ResourceOperation resourceOperation : ResourceOperation.values()) {
                        propEventCategoryTO.getEvents().add(resourceOperation.name().toLowerCase());
                        syncEventCategoryTO.getEvents().add(resourceOperation.name().toLowerCase());
                    }

                    events.add(propEventCategoryTO);
                    events.add(syncEventCategoryTO);
                }
            }

            for (SchedTask task : taskDAO.findAll(SchedTask.class)) {
                final EventCategoryTO eventCategoryTO = new EventCategoryTO(EventCategoryType.TASK);
                eventCategoryTO.setCategory(Class.forName(task.getJobClassName()).getSimpleName());
                events.add(eventCategoryTO);
            }

            for (SyncTask task : taskDAO.findAll(SyncTask.class)) {
                final EventCategoryTO eventCategoryTO = new EventCategoryTO(EventCategoryType.TASK);
                eventCategoryTO.setCategory(Class.forName(task.getJobClassName()).getSimpleName());
                events.add(eventCategoryTO);
            }
        } catch (Exception e) {
            LOG.error("Failure retrieving audit/notification events", e);
        }

        return new ArrayList<EventCategoryTO>(events);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LoggerTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        throw new UnresolvedReferenceException();
    }
}
