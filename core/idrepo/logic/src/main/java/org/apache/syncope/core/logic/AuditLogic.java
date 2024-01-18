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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.audit.EventCategory;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.logic.audit.AuditAppender;
import org.apache.syncope.core.logic.init.AuditLoader;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

public class AuditLogic extends AbstractTransactionalLogic<AuditConfTO> {

    protected static final List<EventCategory> EVENTS = new ArrayList<>();

    protected final AuditConfDAO auditConfDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityFactory entityFactory;

    protected final AuditDataBinder binder;

    protected final AuditManager auditManager;

    protected final List<AuditAppender> auditAppenders;

    protected final LoggingSystem loggingSystem;

    public AuditLogic(
            final AuditConfDAO auditConfDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityFactory entityFactory,
            final AuditDataBinder binder,
            final AuditManager auditManager,
            final List<AuditAppender> auditAppenders,
            final LoggingSystem loggingSystem) {

        this.auditConfDAO = auditConfDAO;
        this.resourceDAO = resourceDAO;
        this.entityFactory = entityFactory;
        this.binder = binder;
        this.auditManager = auditManager;
        this.auditAppenders = auditAppenders;
        this.loggingSystem = loggingSystem;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "')")
    @Transactional(readOnly = true)
    public List<AuditConfTO> list() {
        return auditConfDAO.findAll().stream().map(binder::getAuditTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_READ + "')")
    @Transactional(readOnly = true)
    public AuditConfTO read(final String key) {
        return auditConfDAO.findById(key).map(binder::getAuditTO).
                orElseThrow(() -> new NotFoundException("Audit " + key));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_SET + "')")
    public void set(final AuditConfTO auditTO) {
        AuditConf audit = auditConfDAO.findById(auditTO.getKey()).orElse(null);
        if (audit == null) {
            audit = entityFactory.newEntity(AuditConf.class);
            audit.setKey(auditTO.getKey());
        }
        audit.setActive(auditTO.isActive());
        audit = auditConfDAO.save(audit);

        setLevel(audit.getKey(), audit.isActive() ? LogLevel.DEBUG : LogLevel.OFF);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_DELETE + "')")
    public void delete(final String key) {
        AuditConf audit = auditConfDAO.findById(key).
                orElseThrow(() -> new NotFoundException("Audit " + key));
        auditConfDAO.delete(audit);

        setLevel(audit.getKey(), LogLevel.OFF);
    }

    protected void setLevel(final String key, final LogLevel level) {
        String auditLoggerName = AuditLoggerName.getAuditEventLoggerName(AuthContextUtils.getDomain(), key);

        LoggerContext logCtx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig logConf = logCtx.getConfiguration().getLoggerConfig(auditLoggerName);

        auditAppenders.stream().
                filter(appender -> appender.getEvents().stream().
                anyMatch(event -> key.equalsIgnoreCase(event.toAuditKey()))).
                forEach(auditAppender -> AuditLoader.addAppenderToLoggerContext(logCtx, auditAppender, logConf));

        loggingSystem.setLogLevel(auditLoggerName, level);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "') "
            + "or hasRole('" + IdRepoEntitlement.NOTIFICATION_LIST + "')")
    public List<EventCategory> events() {
        synchronized (EVENTS) {
            if (!EVENTS.isEmpty()) {
                return EVENTS;
            }
        }

        Set<EventCategory> events = new HashSet<>();

        EventCategory authenticationEventCategory = new EventCategory();
        authenticationEventCategory.setCategory(AuditElements.AUTHENTICATION_CATEGORY);
        authenticationEventCategory.getEvents().add(AuditElements.LOGIN_EVENT);
        events.add(authenticationEventCategory);

        EventCategory pullTaskEventCategory = new EventCategory(EventCategoryType.TASK);
        pullTaskEventCategory.setCategory(PullJobDelegate.class.getSimpleName());
        events.add(pullTaskEventCategory);

        EventCategory pushTaskEventCategory = new EventCategory(EventCategoryType.TASK);
        pushTaskEventCategory.setCategory(PushJobDelegate.class.getSimpleName());
        events.add(pushTaskEventCategory);

        events.add(new EventCategory(EventCategoryType.WA));
        events.add(new EventCategory(EventCategoryType.CUSTOM));

        try {
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + ClassUtils.convertClassNameToResourcePath(
                            SystemPropertyUtils.resolvePlaceholders(getClass().getPackage().getName()))
                    + "/**/*.class";

            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    Class<?> clazz = Class.forName(metadataReader.getClassMetadata().getClassName());

                    if (AbstractLogic.class.isAssignableFrom(clazz)) {
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

            for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
                resourceDAO.findAll().forEach(resource -> {
                    EventCategory propEventCategory = new EventCategory(EventCategoryType.PROPAGATION);
                    EventCategory pullEventCategory = new EventCategory(EventCategoryType.PULL);
                    EventCategory pushEventCategory = new EventCategory(EventCategoryType.PUSH);

                    propEventCategory.setCategory(anyTypeKind.name());
                    propEventCategory.setSubcategory(resource.getKey());

                    pullEventCategory.setCategory(anyTypeKind.name());
                    pushEventCategory.setCategory(anyTypeKind.name());
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
        } catch (Exception e) {
            LOG.error("Failure retrieving audit/notification events", e);
        }

        EVENTS.addAll(events);
        return EVENTS;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_SEARCH + "')")
    @Transactional(readOnly = true)
    public Page<AuditEntry> search(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        long count = auditConfDAO.countEntries(entityKey, type, category, subcategory, events, result, before, after);

        List<AuditEntry> matching = auditConfDAO.searchEntries(
                entityKey, type, category, subcategory, events, result, before, after, pageable);

        return new SyncopePage<>(matching, pageable, count);
    }

    @PreAuthorize("isAuthenticated()")
    public void create(final AuditEntry auditEntry) {
        boolean authorized =
                AuthContextUtils.getAuthorizations().containsKey(IdRepoEntitlement.AUDIT_SET)
                || AuthContextUtils.getAuthorizations().containsKey(IdRepoEntitlement.ANONYMOUS)
                && AuditElements.EventCategoryType.WA == auditEntry.getLogger().getType();
        if (authorized) {
            auditManager.audit(
                    auditEntry.getWho(),
                    auditEntry.getLogger().getType(),
                    auditEntry.getLogger().getCategory(),
                    auditEntry.getLogger().getSubcategory(),
                    auditEntry.getLogger().getEvent(),
                    auditEntry.getLogger().getResult(),
                    auditEntry.getBefore(),
                    auditEntry.getOutput(),
                    auditEntry.getInputs());
        } else {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.DelegatedAdministration);
            sce.getElements().add("Not allowed to create Audit entries");
            throw sce;
        }
    }

    @Override
    protected AuditConfTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof AuditConfTO auditConfTO) {
                    key = auditConfTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getAuditTO(auditConfDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
