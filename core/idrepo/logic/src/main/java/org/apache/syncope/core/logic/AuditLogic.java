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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
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

    protected static final List<OpEvent> EVENTS = new ArrayList<>();

    protected static void addForOutcomes(
            final Set<OpEvent> events,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op) {

        events.add(new OpEvent(type, category, subcategory, op, OpEvent.Outcome.SUCCESS));
        events.add(new OpEvent(type, category, subcategory, op, OpEvent.Outcome.FAILURE));
    }

    protected final AuditConfDAO auditConfDAO;

    protected final AuditEventDAO auditEventDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityFactory entityFactory;

    protected final ImplementationLookup implementationLookup;

    protected final AuditDataBinder binder;

    protected final AuditManager auditManager;

    public AuditLogic(
            final AuditConfDAO auditConfDAO,
            final AuditEventDAO auditEventDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityFactory entityFactory,
            final ImplementationLookup implementationLookup,
            final AuditDataBinder binder,
            final AuditManager auditManager) {

        this.auditConfDAO = auditConfDAO;
        this.auditEventDAO = auditEventDAO;
        this.resourceDAO = resourceDAO;
        this.entityFactory = entityFactory;
        this.implementationLookup = implementationLookup;
        this.binder = binder;
        this.auditManager = auditManager;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "')")
    @Transactional(readOnly = true)
    public List<AuditConfTO> confs() {
        return auditConfDAO.findAll().stream().map(binder::getAuditConfTO).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_READ + "')")
    @Transactional(readOnly = true)
    public AuditConfTO getConf(final String key) {
        return auditConfDAO.findById(key).map(binder::getAuditConfTO).
                orElseThrow(() -> new NotFoundException("AuditConf " + key));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_SET + "')")
    public void setConf(final AuditConfTO auditTO) {
        AuditConf audit = auditConfDAO.findById(auditTO.getKey()).orElse(null);
        if (audit == null) {
            audit = entityFactory.newEntity(AuditConf.class);
            audit.setKey(auditTO.getKey());
        }
        audit.setActive(auditTO.isActive());
        auditConfDAO.save(audit);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_DELETE + "')")
    public void deleteConf(final String key) {
        AuditConf audit = auditConfDAO.findById(key).
                orElseThrow(() -> new NotFoundException("AuditConf " + key));
        auditConfDAO.delete(audit);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_LIST + "') "
            + "or hasRole('" + IdRepoEntitlement.NOTIFICATION_LIST + "')")
    public List<OpEvent> events() {
        synchronized (EVENTS) {
            if (!EVENTS.isEmpty()) {
                return EVENTS;
            }
        }

        Set<OpEvent> events = new HashSet<>();

        addForOutcomes(
                events,
                OpEvent.CategoryType.LOGIC,
                OpEvent.AUTHENTICATION_CATEGORY,
                null,
                OpEvent.LOGIN_OP);

        implementationLookup.getClassNames(IdRepoImplementationType.TASKJOB_DELEGATE).
                forEach(clazz -> addForOutcomes(
                events,
                OpEvent.CategoryType.TASK,
                StringUtils.substringAfterLast(clazz, '.'),
                null,
                null));

        addForOutcomes(
                events,
                OpEvent.CategoryType.WA,
                null,
                null,
                null);

        addForOutcomes(
                events,
                OpEvent.CategoryType.CUSTOM,
                null,
                null,
                null);

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
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (Modifier.isPublic(method.getModifiers())) {
                                addForOutcomes(
                                        events,
                                        OpEvent.CategoryType.LOGIC,
                                        clazz.getSimpleName(),
                                        null,
                                        method.getName());
                            }
                        }
                    }
                }
            }

            for (AnyTypeKind anyTypeKind : AnyTypeKind.values()) {
                resourceDAO.findAll().forEach(resource -> {
                    for (ResourceOperation resourceOperation : ResourceOperation.values()) {
                        addForOutcomes(
                                events,
                                OpEvent.CategoryType.PROPAGATION,
                                anyTypeKind.name(),
                                resource.getKey(),
                                resourceOperation.name().toLowerCase());
                    }

                    for (UnmatchingRule unmatching : UnmatchingRule.values()) {
                        String op = UnmatchingRule.toOp(unmatching);

                        addForOutcomes(
                                events,
                                OpEvent.CategoryType.PULL,
                                anyTypeKind.name(),
                                resource.getKey(),
                                op);
                        addForOutcomes(
                                events,
                                OpEvent.CategoryType.PUSH,
                                anyTypeKind.name(),
                                resource.getKey(),
                                op);
                    }

                    for (MatchingRule matching : MatchingRule.values()) {
                        String op = MatchingRule.toOp(matching);

                        addForOutcomes(
                                events,
                                OpEvent.CategoryType.PULL,
                                anyTypeKind.name(),
                                resource.getKey(),
                                op);
                        addForOutcomes(
                                events,
                                OpEvent.CategoryType.PUSH,
                                anyTypeKind.name(),
                                resource.getKey(),
                                op);
                    }

                    addForOutcomes(
                            events,
                            OpEvent.CategoryType.PULL,
                            anyTypeKind.name(),
                            resource.getKey(),
                            ResourceOperation.DELETE.name().toLowerCase());
                });
            }
        } catch (Exception e) {
            LOG.error("Failure retrieving op events", e);
        }

        EVENTS.addAll(events);
        return EVENTS;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.AUDIT_SEARCH + "')")
    @Transactional(readOnly = true)
    public Page<AuditEventTO> search(
            final String entityKey,
            final OpEvent.CategoryType type,
            final String category,
            final String subcategory,
            final String op,
            final OpEvent.Outcome result,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        long count = auditEventDAO.count(entityKey, type, category, subcategory, op, result, before, after);

        List<AuditEventTO> matching = auditEventDAO.search(
                entityKey, type, category, subcategory, op, result, before, after, pageable);

        return new SyncopePage<>(matching, pageable, count);
    }

    @PreAuthorize("isAuthenticated()")
    public void create(final AuditEventTO eventTO) {
        boolean authorized =
                AuthContextUtils.getAuthorizations().containsKey(IdRepoEntitlement.AUDIT_SET)
                || AuthContextUtils.getAuthorizations().containsKey(IdRepoEntitlement.ANONYMOUS)
                && OpEvent.CategoryType.WA == eventTO.getOpEvent().getType();
        if (authorized) {
            auditManager.audit(
                    AuthContextUtils.getDomain(),
                    eventTO.getWho(),
                    eventTO.getOpEvent().getType(),
                    eventTO.getOpEvent().getCategory(),
                    eventTO.getOpEvent().getSubcategory(),
                    eventTO.getOpEvent().getOp(),
                    eventTO.getOpEvent().getOutcome(),
                    eventTO.getBefore(),
                    eventTO.getOutput(),
                    eventTO.getInputs());
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
                return binder.getAuditConfTO(auditConfDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
