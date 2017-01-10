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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.TransformerUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.log.EventCategoryTO;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.logic.init.LoggerLoader;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
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
public class LoggerLogic extends AbstractTransactionalLogic<LoggerTO> {

    @Autowired
    private LoggerLoader loggerLoader;

    @Autowired
    private LoggerDAO loggerDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private EntityFactory entityFactory;

    private List<LoggerTO> list(final LoggerType type) {
        return CollectionUtils.collect(loggerDAO.findAll(type), new Transformer<Logger, LoggerTO>() {

            @Override
            public LoggerTO transform(final Logger logger) {
                LoggerTO loggerTO = new LoggerTO();
                BeanUtils.copyProperties(logger, loggerTO);
                return loggerTO;
            }
        }, new ArrayList<LoggerTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.LOG_LIST + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public List<LogAppender> memoryAppenders() {
        return CollectionUtils.collect(
                loggerLoader.getMemoryAppenders().keySet(),
                new Transformer<String, LogAppender>() {

            @Override
            public LogAppender transform(final String input) {
                LogAppender logAppender = new LogAppender();
                logAppender.setName(input);
                return logAppender;
            }
        }, new ArrayList<LogAppender>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.LOG_READ + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public List<LogStatementTO> getLastLogStatements(final String memoryAppender) {
        MemoryAppender appender = loggerLoader.getMemoryAppenders().get(memoryAppender);
        if (appender == null) {
            throw new NotFoundException("Appender " + memoryAppender);
        }

        return CollectionUtils.collect(
                appender.getStatements(),
                TransformerUtils.<LogStatementTO>nopTransformer(),
                new ArrayList<LogStatementTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.LOG_LIST + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    @Transactional(readOnly = true)
    public List<LoggerTO> listLogs() {
        return list(LoggerType.LOG);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.AUDIT_LIST + "')")
    @Transactional(readOnly = true)
    public List<AuditLoggerName> listAudits() {
        return CollectionUtils.collect(
                IteratorUtils.filteredIterator(list(LoggerType.AUDIT).iterator(), PredicateUtils.notNullPredicate()),
                new Transformer<LoggerTO, AuditLoggerName>() {

            @Override
            public AuditLoggerName transform(final LoggerTO logger) {
                AuditLoggerName result = null;
                try {
                    result = AuditLoggerName.fromLoggerName(logger.getKey());
                } catch (Exception e) {
                    LOG.warn("Unexpected audit logger name: {}", logger.getKey(), e);
                }

                return result;
            }
        }, new ArrayList<AuditLoggerName>());
    }

    private void throwInvalidLogger(final LoggerType type) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidLogger);
        sce.getElements().add("Expected " + type.name());

        throw sce;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.LOG_READ + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    @Transactional(readOnly = true)
    public LoggerTO readLog(final String name) {
        for (final LoggerTO logger : listLogs()) {
            if (logger.getKey().equals(name)) {
                return logger;
            }
        }
        throw new NotFoundException("Logger " + name);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.AUDIT_READ + "')")
    @Transactional(readOnly = true)
    public LoggerTO readAudit(final String name) {
        for (final AuditLoggerName logger : listAudits()) {
            if (logger.toLoggerName().equals(name)) {
                final LoggerTO loggerTO = new LoggerTO();
                loggerTO.setKey(logger.toLoggerName());
                loggerTO.setLevel(LoggerLevel.DEBUG);
                return loggerTO;
            }
        }
        throw new NotFoundException("Logger " + name);
    }

    private LoggerTO setLevel(final String name, final Level level, final LoggerType expectedType) {
        Logger syncopeLogger = loggerDAO.find(name);
        if (syncopeLogger == null) {
            LOG.debug("Logger {} not found: creating new...", name);

            syncopeLogger = entityFactory.newEntity(Logger.class);
            syncopeLogger.setKey(name);
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

    @PreAuthorize("hasRole('" + StandardEntitlement.LOG_SET_LEVEL + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public LoggerTO setLogLevel(final String name, final Level level) {
        return setLevel(name, level, LoggerType.LOG);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.AUDIT_ENABLE + "')")
    public void enableAudit(final AuditLoggerName auditLoggerName) {
        try {
            setLevel(auditLoggerName.toLoggerName(), Level.DEBUG, LoggerType.AUDIT);
        } catch (IllegalArgumentException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidLogger);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    private LoggerTO delete(final String name, final LoggerType expectedType) {
        Logger syncopeLogger = loggerDAO.find(name);
        if (syncopeLogger == null) {
            throw new NotFoundException("Logger " + name);
        }
        if (expectedType != syncopeLogger.getType()) {
            throwInvalidLogger(expectedType);
        }

        LoggerTO loggerToDelete = new LoggerTO();
        BeanUtils.copyProperties(syncopeLogger, loggerToDelete);

        // remove SyncopeLogger from local storage, so that LoggerLoader won't load this next time
        loggerDAO.delete(syncopeLogger);

        // set log level to OFF in order to disable configured logger until next reboot
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.Logger logger = SyncopeConstants.ROOT_LOGGER.equals(name)
                ? ctx.getLogger(LogManager.ROOT_LOGGER_NAME) : ctx.getLogger(name);
        logger.setLevel(Level.OFF);
        ctx.updateLoggers();

        return loggerToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.LOG_DELETE + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public LoggerTO deleteLog(final String name) {
        return delete(name, LoggerType.LOG);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.AUDIT_DISABLE + "')")
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

    @PreAuthorize("hasRole('" + StandardEntitlement.AUDIT_LIST + "') or hasRole('"
            + StandardEntitlement.NOTIFICATION_LIST + "')")
    public List<EventCategoryTO> listAuditEvents() {
        // use set to avoid duplications or null elements
        Set<EventCategoryTO> events = new HashSet<>();

        try {
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + ClassUtils.convertClassNameToResourcePath(
                            SystemPropertyUtils.resolvePlaceholders(this.getClass().getPackage().getName()))
                    + "/**/*.class";

            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    final MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    final Class<?> clazz = Class.forName(metadataReader.getClassMetadata().getClassName());

                    if (clazz.isAnnotationPresent(Component.class) && AbstractLogic.class.isAssignableFrom(clazz)) {
                        EventCategoryTO eventCategoryTO = new EventCategoryTO();
                        eventCategoryTO.setCategory(clazz.getSimpleName());
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (Modifier.isPublic(method.getModifiers())
                                    && !eventCategoryTO.getEvents().contains(method.getName())) {

                                eventCategoryTO.getEvents().add(method.getName());
                            }
                        }

                        events.add(eventCategoryTO);
                    }
                }
            }

            // SYNCOPE-608
            EventCategoryTO authenticationControllerEvents = new EventCategoryTO();
            authenticationControllerEvents.setCategory(AuditElements.AUTHENTICATION_CATEGORY);
            authenticationControllerEvents.getEvents().add(AuditElements.LOGIN_EVENT);
            events.add(authenticationControllerEvents);

            events.add(new EventCategoryTO(EventCategoryType.PROPAGATION));
            events.add(new EventCategoryTO(EventCategoryType.PULL));
            events.add(new EventCategoryTO(EventCategoryType.PUSH));

            for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
                for (ExternalResource resource : resourceDAO.findAll()) {
                    EventCategoryTO propEventCategoryTO = new EventCategoryTO(EventCategoryType.PROPAGATION);
                    EventCategoryTO syncEventCategoryTO = new EventCategoryTO(EventCategoryType.PULL);
                    EventCategoryTO pushEventCategoryTO = new EventCategoryTO(EventCategoryType.PUSH);

                    propEventCategoryTO.setCategory(anyTypeKind.name().toLowerCase());
                    propEventCategoryTO.setSubcategory(resource.getKey());

                    syncEventCategoryTO.setCategory(anyTypeKind.name().toLowerCase());
                    pushEventCategoryTO.setCategory(anyTypeKind.name().toLowerCase());
                    syncEventCategoryTO.setSubcategory(resource.getKey());
                    pushEventCategoryTO.setSubcategory(resource.getKey());

                    for (ResourceOperation resourceOperation : ResourceOperation.values()) {
                        propEventCategoryTO.getEvents().add(resourceOperation.name().toLowerCase());
                        syncEventCategoryTO.getEvents().add(resourceOperation.name().toLowerCase());
                        pushEventCategoryTO.getEvents().add(resourceOperation.name().toLowerCase());
                    }

                    for (UnmatchingRule unmatching : UnmatchingRule.values()) {
                        String event = UnmatchingRule.toEventName(unmatching);
                        syncEventCategoryTO.getEvents().add(event);
                        pushEventCategoryTO.getEvents().add(event);
                    }

                    for (MatchingRule matching : MatchingRule.values()) {
                        String event = MatchingRule.toEventName(matching);
                        syncEventCategoryTO.getEvents().add(event);
                        pushEventCategoryTO.getEvents().add(event);
                    }

                    events.add(propEventCategoryTO);
                    events.add(syncEventCategoryTO);
                    events.add(pushEventCategoryTO);
                }
            }

            for (SchedTask task : taskDAO.<SchedTask>findAll(TaskType.SCHEDULED)) {
                EventCategoryTO eventCategoryTO = new EventCategoryTO(EventCategoryType.TASK);
                eventCategoryTO.setCategory(Class.forName(task.getJobDelegateClassName()).getSimpleName());
                events.add(eventCategoryTO);
            }

            EventCategoryTO eventCategoryTO = new EventCategoryTO(EventCategoryType.TASK);
            eventCategoryTO.setCategory(PullJobDelegate.class.getSimpleName());
            events.add(eventCategoryTO);

            eventCategoryTO = new EventCategoryTO(EventCategoryType.TASK);
            eventCategoryTO.setCategory(PushJobDelegate.class.getSimpleName());
            events.add(eventCategoryTO);
        } catch (Exception e) {
            LOG.error("Failure retrieving audit/notification events", e);
        }

        return new ArrayList<>(events);
    }

    @Override
    protected LoggerTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
