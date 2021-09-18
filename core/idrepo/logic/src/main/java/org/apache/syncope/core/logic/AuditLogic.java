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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.audit.EventCategory;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.types.AuditElements.EventCategoryType;
import org.apache.syncope.common.lib.types.AuditLoggerName;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.logic.init.AuditLoader;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;

public class AuditLogic extends AbstractTransactionalLogic<AuditConfTO> {

    protected static final List<EventCategory> EVENTS = new ArrayList<>();

    protected final AuditLoader auditLoader;

    protected final AuditConfDAO auditConfDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityFactory entityFactory;

    protected final AuditDataBinder binder;

    protected final AuditManager auditManager;

    public AuditLogic(
            final AuditLoader auditLoader,
            final AuditConfDAO auditConfDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityFactory entityFactory,
            final AuditDataBinder binder,
            final AuditManager auditManager) {

        this.auditLoader = auditLoader;
        this.auditConfDAO = auditConfDAO;
        this.resourceDAO = resourceDAO;
        this.entityFactory = entityFactory;
        this.binder = binder;
        this.auditManager = auditManager;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "')")
    @Transactional(readOnly = true)
    public List<AuditConfTO> list() {
        return auditConfDAO.findAll().stream().map(binder::getAuditTO).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_READ + "')")
    @Transactional(readOnly = true)
    public AuditConfTO read(final String key) {
        return Optional.ofNullable(auditConfDAO.find(key)).map(binder::getAuditTO).
                orElseThrow(() -> new NotFoundException("Audit " + key));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_CREATE + "')")
    public void create(final AuditConfTO auditTO) {
        AuditConf audit = entityFactory.newEntity(AuditConf.class);
        audit.setKey(auditTO.getKey());
        audit.setActive(auditTO.isActive());
        audit = auditConfDAO.save(audit);

        if (audit.isActive()) {
            setLevel(audit.getKey(), Level.DEBUG);
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_UPDATE + "')")
    public void update(final AuditConfTO auditTO) {
        AuditConf audit = Optional.ofNullable(auditConfDAO.find(auditTO.getKey())).
                orElseThrow(() -> new NotFoundException("Audit " + auditTO.getKey()));
        audit.setActive(auditTO.isActive());
        audit = auditConfDAO.save(audit);

        if (audit.isActive()) {
            setLevel(audit.getKey(), Level.OFF);
        }
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_DELETE + "')")
    public void delete(final String key) {
        AuditConf audit = Optional.ofNullable(auditConfDAO.find(key)).
                orElseThrow(() -> new NotFoundException("Audit " + key));
        auditConfDAO.delete(audit);

        setLevel(audit.getKey(), Level.OFF);
    }

    protected void setLevel(final String key, final Level level) {
        String auditLoggerName = AuditLoggerName.getAuditEventLoggerName(AuthContextUtils.getDomain(), key);

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig logConf = ctx.getConfiguration().getLoggerConfig(auditLoggerName);

        // SYNCOPE-1144 For each custom audit appender class add related appenders to log4j logger
        auditLoader.auditAppenders(AuthContextUtils.getDomain()).stream().
                filter(appender -> appender.getEvents().stream().
                anyMatch(event -> key.equalsIgnoreCase(event.toAuditKey()))).
                forEach(auditAppender -> AuditLoader.addAppenderToContext(ctx, auditAppender, logConf));

        logConf.setLevel(level);
        ctx.updateLoggers();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "') "
            + "or hasRole('" + IdRepoEntitlement.NOTIFICATION_LIST + "')")
    public List<EventCategory> events() {
        synchronized (EVENTS) {
            if (!EVENTS.isEmpty()) {
                return EVENTS;
            }
        }

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

        EVENTS.addAll(events);
        return EVENTS;
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

        int count = auditConfDAO.countEntries(entityKey, type, category, subcategory, events, result);
        List<AuditEntry> matching = auditConfDAO.searchEntries(
                entityKey, page, size, type, category, subcategory, events, result, orderByClauses);
        return Pair.of(count, matching);
    }

    @PreAuthorize("isAuthenticated()")
    public void create(final AuditEntry auditEntry) {
        boolean authorized =
                AuthContextUtils.getAuthorizations().containsKey(IdRepoEntitlement.AUDIT_CREATE)
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
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof AuditConfTO) {
                    key = ((AuditConfTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getAuditTO(auditConfDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
