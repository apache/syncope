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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.log.AuditEntry;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.log.EventCategory;
import org.apache.syncope.common.lib.log.LogAppender;
import org.apache.syncope.common.lib.log.LogStatement;
import org.apache.syncope.common.lib.log.LoggerTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.LoggerLevel;
import org.apache.syncope.common.lib.types.LoggerType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.logic.init.LoggerLoader;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.LoggerDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;
import org.apache.syncope.core.provisioning.api.data.LoggerDataBinder;

@Component
public class LoggerLogic extends AbstractTransactionalLogic<EntityTO> {

    @Autowired
    private DomainHolder domainHolder;

    @Autowired
    private LoggerLoader loggerLoader;

    @Autowired
    private LoggerDAO loggerDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private LoggerDataBinder binder;

    @PreAuthorize("hasRole('" + IdRepoEntitlement.LOG_LIST + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    @Transactional(readOnly = true)
    public List<LogAppender> memoryAppenders() {
        return loggerLoader.getMemoryAppenders().keySet().stream().map(appender -> {
            LogAppender logAppender = new LogAppender();
            logAppender.setName(appender);
            return logAppender;
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.LOG_READ + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    @Transactional(readOnly = true)
    public List<LogStatement> getLastLogStatements(final String memoryAppender) {
        MemoryAppender appender = loggerLoader.getMemoryAppenders().get(memoryAppender);
        if (appender == null) {
            throw new NotFoundException("Appender " + memoryAppender);
        }

        return appender.getStatements().stream().collect(Collectors.toList());
    }

    private List<LoggerTO> list(final LoggerType type) {
        return loggerDAO.findAll(type).stream().map(binder::getLoggerTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.LOG_LIST + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    @Transactional(readOnly = true)
    public List<LoggerTO> listLogs() {
        return list(LoggerType.LOG).stream().
                filter(logger -> !logger.getKey().startsWith(SyncopeConstants.MASTER_DOMAIN)).
                filter(logger -> domainHolder.getDomains().keySet().stream().
                noneMatch(domain -> logger.getKey().startsWith(domain))).
                collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "')")
    @Transactional(readOnly = true)
    public List<AuditLoggerName> listAudits() {
        return list(LoggerType.AUDIT).stream().
                map(logger -> {
                    AuditLoggerName result = null;
                    try {
                        result = AuditLoggerName.fromLoggerName(logger.getKey());
                    } catch (Exception e) {
                        LOG.warn("Unexpected audit logger name: {}", logger.getKey(), e);
                    }

                    return result;
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    private static void throwInvalidLogger(final LoggerType type) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidLogger);
        sce.getElements().add("Expected " + type.name());

        throw sce;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.LOG_READ + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    @Transactional(readOnly = true)
    public LoggerTO readLog(final String name) {
        return listLogs().stream().
                filter(logger -> logger.getKey().equals(name)).findFirst().
                orElseThrow(() -> new NotFoundException("Logger " + name));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_READ + "')")
    @Transactional(readOnly = true)
    public LoggerTO readAudit(final String name) {
        return listAudits().stream().
                filter(logger -> logger.toLoggerName().equals(name)).findFirst().
                map(binder::getLoggerTO).orElseThrow(() -> new NotFoundException("Audit " + name));
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
        LoggerConfig logConf;

        if (LoggerType.AUDIT.equals(syncopeLogger.getType())) {
            String auditLoggerName = AuditLoggerName.getAuditEventLoggerName(
                    AuthContextUtils.getDomain(), syncopeLogger.getKey());

            logConf = ctx.getConfiguration().getLoggerConfig(auditLoggerName);

            // SYNCOPE-1144 For each custom audit appender class add related appenders to log4j logger
            boolean isRootLogConf = LogManager.ROOT_LOGGER_NAME.equals(logConf.getName());
            if (isRootLogConf) {
                logConf = new LoggerConfig(auditLoggerName, null, false);
            }
            for (AuditAppender auditAppender : loggerLoader.auditAppenders(AuthContextUtils.getDomain())) {
                if (auditAppender.getEvents().stream().anyMatch(event -> name.equalsIgnoreCase(event.toLoggerName()))) {
                    LoggerLoader.addAppenderToContext(ctx, auditAppender, logConf);
                }
            }
            if (isRootLogConf) {
                ctx.getConfiguration().addLogger(auditLoggerName, logConf);
            }
        } else {
            logConf = SyncopeConstants.ROOT_LOGGER.equals(name)
                    ? ctx.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME)
                    : ctx.getConfiguration().getLoggerConfig(name);
        }

        logConf.setLevel(level);
        ctx.updateLoggers();

        return binder.getLoggerTO(syncopeLogger);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.LOG_SET_LEVEL + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public LoggerTO setLogLevel(final String name, final Level level) {
        return setLevel(name, level, LoggerType.LOG);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_ENABLE + "')")
    public void enableAudit(final AuditLoggerName auditLoggerName) {
        try {
            setLevel(auditLoggerName.toLoggerName(), Level.DEBUG, LoggerType.AUDIT);
        } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
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

        LoggerTO loggerToDelete = binder.getLoggerTO(syncopeLogger);

        // remove SyncopeLogger from local storage, so that LoggerLoader won't load this next time
        loggerDAO.delete(syncopeLogger);

        // set log level to OFF in order to disable configured logger until next reboot
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

        String auditLoggerName = AuditLoggerName.getAuditEventLoggerName(
                AuthContextUtils.getDomain(), syncopeLogger.getKey());
        org.apache.logging.log4j.core.Logger logger = SyncopeConstants.ROOT_LOGGER.equals(name)
                ? ctx.getLogger(LogManager.ROOT_LOGGER_NAME)
                : LoggerType.AUDIT.equals(syncopeLogger.getType())
                ? ctx.getLogger(auditLoggerName)
                : ctx.getLogger(name);

        logger.setLevel(Level.OFF);
        ctx.updateLoggers();

        return loggerToDelete;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.LOG_DELETE + "') and authentication.details.domain == "
            + "T(org.apache.syncope.common.lib.SyncopeConstants).MASTER_DOMAIN")
    public LoggerTO deleteLog(final String name) {
        return delete(name, LoggerType.LOG);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_DISABLE + "')")
    public void disableAudit(final AuditLoggerName auditLoggerName) {
        try {
            delete(auditLoggerName.toLoggerName(), LoggerType.AUDIT);
        } catch (NotFoundException e) {
            LOG.debug("Ignoring disable of non existing logger {}", auditLoggerName.toLoggerName());
        } catch (IllegalArgumentException | InvalidDataAccessApiUsageException e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidLogger);
            sce.getElements().add(e.getMessage());
            throw sce;
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "') "
            + "or hasRole('" + IdRepoEntitlement.NOTIFICATION_LIST + "')")
    public List<EventCategory> listAuditEvents() {
        // use set to avoid duplications or null elements
        Set<EventCategory> events = new HashSet<>();

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
                        EventCategory eventCategory = new EventCategory();
                        eventCategory.setCategory(clazz.getSimpleName());
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (Modifier.isPublic(method.getModifiers())
                                    && !eventCategory.getEvents().contains(method.getName())) {

                                eventCategory.getEvents().add(method.getName());
                            }
                        }

                        events.add(eventCategory);
                    }
                }
            }

            // SYNCOPE-608
            EventCategory authenticationControllerEvents = new EventCategory();
            authenticationControllerEvents.setCategory(AuditElements.AUTHENTICATION_CATEGORY);
            authenticationControllerEvents.getEvents().add(AuditElements.LOGIN_EVENT);
            events.add(authenticationControllerEvents);

            events.add(new EventCategory(EventCategoryType.PROPAGATION));
            events.add(new EventCategory(EventCategoryType.PULL));
            events.add(new EventCategory(EventCategoryType.PUSH));

            for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
                resourceDAO.findAll().forEach(resource -> {
                    EventCategory propEventCategory = new EventCategory(EventCategoryType.PROPAGATION);
                    EventCategory pullEventCategory = new EventCategory(EventCategoryType.PULL);
                    EventCategory pushEventCategory = new EventCategory(EventCategoryType.PUSH);

                    propEventCategory.setCategory(anyTypeKind.name().toLowerCase());
                    propEventCategory.setSubcategory(resource.getKey());

                    pullEventCategory.setCategory(anyTypeKind.name().toLowerCase());
                    pushEventCategory.setCategory(anyTypeKind.name().toLowerCase());
                    pullEventCategory.setSubcategory(resource.getKey());
                    pushEventCategory.setSubcategory(resource.getKey());

                    for (ResourceOperation resourceOperation : ResourceOperation.values()) {
                        propEventCategory.getEvents().add(resourceOperation.name().toLowerCase());
                    }
                    pullEventCategory.getEvents().add(ResourceOperation.DELETE.name().toLowerCase());

                    for (UnmatchingRule unmatching : UnmatchingRule.values()) {
                        String event = UnmatchingRule.toEventName(unmatching);
                        pullEventCategory.getEvents().add(event);
                        pushEventCategory.getEvents().add(event);
                    }

                    for (MatchingRule matching : MatchingRule.values()) {
                        String event = MatchingRule.toEventName(matching);
                        pullEventCategory.getEvents().add(event);
                        pushEventCategory.getEvents().add(event);
                    }

                    events.add(propEventCategory);
                    events.add(pullEventCategory);
                    events.add(pushEventCategory);
                });
            }

            EventCategory eventCategory = new EventCategory(EventCategoryType.TASK);
            eventCategory.setCategory(PullJobDelegate.class.getSimpleName());
            events.add(eventCategory);

            eventCategory = new EventCategory(EventCategoryType.TASK);
            eventCategory.setCategory(PushJobDelegate.class.getSimpleName());
            events.add(eventCategory);
        } catch (Exception e) {
            LOG.error("Failure retrieving audit/notification events", e);
        }

        return new ArrayList<>(events);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_SEARCH + "')")
    @Transactional(readOnly = true)
    public Pair<Integer, List<AuditEntry>> search(
            final String entityKey,
            final int page,
            final int size,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final List<OrderByClause> orderByClauses) {

        int count = loggerDAO.countAuditEntries(entityKey);
        List<AuditEntry> matching = loggerDAO.findAuditEntries(
                entityKey, page, size, type, category, subcategory, events, result, orderByClauses);
        return Pair.of(count, matching);
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
