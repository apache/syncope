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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jNotification;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.task.AbstractTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jAnyTemplatePullTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jFormPropertyDef;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jLiveSyncTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTaskCommand;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jNotificationTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPropagationTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPropagationTaskExec;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPullTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPushTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jSchedTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jSchedTaskExec;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

public class Neo4jTaskDAO extends AbstractDAO implements TaskDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(TaskDAO.class);

    public static String execRelationship(final TaskType type) {
        String result = null;

        switch (type) {
            case NOTIFICATION:
                result = Neo4jNotificationTask.NOTIFICATION_TASK_EXEC_REL;
                break;

            case PROPAGATION:
                result = Neo4jPropagationTask.PROPAGATION_TASK_EXEC_REL;
                break;

            case PUSH:
                result = Neo4jPushTask.PUSH_TASK_EXEC_REL;
                break;

            case PULL:
                result = Neo4jPullTask.PULL_TASK_EXEC_REL;
                break;

            case LIVE_SYNC:
                result = Neo4jLiveSyncTask.LIVE_SYNC_TASK_EXEC_REL;
                break;

            case MACRO:
                result = Neo4jMacroTask.MACRO_TASK_EXEC_REL;
                break;

            case SCHEDULED:
                result = Neo4jSchedTask.SCHED_TASK_EXEC_REL;
                break;

            default:
        }

        return result;
    }

    protected final RealmSearchDAO realmSearchDAO;

    protected final RemediationDAO remediationDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final SecurityProperties securityProperties;

    protected final NodeValidator nodeValidator;

    public Neo4jTaskDAO(
            final RealmSearchDAO realmSearchDAO,
            final RemediationDAO remediationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final SecurityProperties securityProperties,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.realmSearchDAO = realmSearchDAO;
        this.remediationDAO = remediationDAO;
        this.taskUtilsFactory = taskUtilsFactory;
        this.securityProperties = securityProperties;
        this.nodeValidator = nodeValidator;
    }

    @Override
    public boolean existsById(final String key) {
        return findById(key).isPresent();
    }

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task<T>> Optional<T> findById(final TaskType type, final String key) {
        return neo4jTemplate.findById(key, (Class<T>) taskUtilsFactory.getInstance(type).getTaskEntity());
    }

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public <T extends SchedTask> Optional<T> findByName(final TaskType type, final String name) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);
        return neo4jClient.query(
                "MATCH (n:" + taskUtils.getTaskStorage() + ") WHERE n.name = $name RETURN n.id").
                bindAll(Map.of("name", name)).fetch().one().
                flatMap(toOptional("n.id", (Class<AbstractTask<?>>) taskUtils.getTaskEntity(), null));
    }

    @Override
    public Optional<? extends Task<?>> findById(final String key) {
        // concrete task types first: their labels are more specific than the shared SchedTask one
        List<TaskType> types = new ArrayList<>(List.of(TaskType.values()));
        types.remove(TaskType.SCHEDULED);
        types.add(TaskType.SCHEDULED);

        StringBuilder query = new StringBuilder("CALL () { ");
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                query.append(" UNION ALL ");
            }
            TaskType type = types.get(i);
            query.append("MATCH (n:").append(taskUtilsFactory.getInstance(type).getTaskStorage()).
                    append(") WHERE n.id = $id RETURN ").append(i).append(" AS priority, '").
                    append(type.name()).append("' AS type");
        }
        query.append(" } RETURN type, priority ORDER BY priority LIMIT 1");

        Optional<TaskType> type = neo4jClient.query(query.toString()).
                bindAll(Map.of("id", key)).fetch().one().
                map(found -> TaskType.valueOf(found.get("type").toString()));

        return type.isEmpty() ? Optional.empty() : findById(type.get(), key);
    }

    @Override
    public List<SchedTask> findByDelegate(final Implementation delegate) {
        return findByRelationship(
                Neo4jSchedTask.NODE, Neo4jSchedTaskExec.NODE, delegate.getKey(), Neo4jSchedTask.class, null);
    }

    @Override
    public List<PullTask> findByReconFilterBuilder(final Implementation reconFilterBuilder) {
        return findByRelationship(
                Neo4jPullTask.NODE, Neo4jImplementation.NODE, reconFilterBuilder.getKey(), Neo4jPullTask.class, null);
    }

    @Override
    public List<PullTask> findByInboundActions(final Implementation inboundActions) {
        return findByRelationship(
                Neo4jPullTask.NODE, Neo4jImplementation.NODE, inboundActions.getKey(), Neo4jPullTask.class, null);
    }

    @Override
    public List<PushTask> findByPushActions(final Implementation pushActions) {
        return findByRelationship(
                Neo4jPushTask.NODE, Neo4jImplementation.NODE, pushActions.getKey(), Neo4jPushTask.class, null);
    }

    @Override
    public List<MacroTask> findByRealm(final Realm realm) {
        return findByRelationship(Neo4jMacroTask.NODE, Neo4jRealm.NODE, realm.getKey(), Neo4jMacroTask.class, null);
    }

    @Override
    public List<MacroTask> findByCommand(final Implementation command) {
        return findByRelationship(
                Neo4jMacroTask.NODE, Neo4jImplementation.NODE, command.getKey(), Neo4jMacroTask.class, null);
    }

    @Override
    public long countToExec(final TaskType type) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);
        StringBuilder query = new StringBuilder("MATCH (n:" + taskUtils.getTaskStorage() + ") WHERE ");

        if (type == TaskType.NOTIFICATION) {
            query.append("n.executed = false ");
        } else {
            query.append("(n)-[:").append(execRelationship(type)).append("]-() ");
        }

        query.append(" RETURN COUNT(DISTINCT n)");

        return neo4jTemplate.count(query.toString());
    }

    protected String toOrderByStatement(
            final Class<? extends Task<?>> beanClass,
            final Stream<Sort.Order> orderByClauses) {

        StringBuilder subStatement = new StringBuilder();
        orderByClauses.forEach(clause -> {
            String field = null;
            switch (clause.getProperty().trim()) {
                case "latestExecStatus":
                    field = "status";
                    break;

                case "start":
                    field = "startDate";
                    break;

                case "end":
                    field = "endDate";
                    break;

                default:
                    Field beanField = ReflectionUtils.findField(beanClass, clause.getProperty().trim());
                    if (beanField != null) {
                        field = beanField.getName();
                    }
            }

            if (field == null) {
                LOG.debug("Unsupported ORDER BY clause: {}", clause.getProperty().trim());
            } else {
                subStatement.append("p.").append(field).append(' ').append(clause.getDirection().name()).append(',');
            }
        });

        StringBuilder statement = new StringBuilder(" ORDER BY ");
        if (subStatement.length() == 0) {
            statement.append("p.id DESC");
        } else {
            subStatement.deleteCharAt(subStatement.length() - 1);
            statement.append(subStatement);
        }

        return statement.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task<T>> List<T> findToExec(final TaskType type, final Pageable pageable) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);
        StringBuilder query = new StringBuilder("MATCH (p:" + taskUtils.getTaskStorage() + ") WHERE ");

        if (type == TaskType.NOTIFICATION) {
            query.append("p.executed = false ");
        } else {
            query.append("(p)-[:").append(execRelationship(type)).append("]-() ");
        }

        query.append("RETURN p.id ").append(toOrderByStatement(
                taskUtilsFactory.getInstance(type).getTaskEntity(), pageable.getSort().stream()));

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(query.toString()).fetch().all(),
                "p.id",
                (Class<AbstractTask<?>>) taskUtils.getTaskEntity(),
                null);
    }

    @Override
    public List<? extends Task<?>> findAll() {
        throw new UnsupportedOperationException();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends Task<T>> List<T> findAll(final TaskType type) {
        return findAll(type, null, null, null, null, Pageable.unpaged());
    }

    protected StringBuilder query(
            final TaskType type,
            final TaskUtils taskUtils,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final Map<String, Object> parameters) {

        if (resource != null
                && type != TaskType.PROPAGATION
                && type != TaskType.LIVE_SYNC
                && type != TaskType.PUSH
                && type != TaskType.PULL) {

            throw new IllegalArgumentException(type + " is not related to " + ExternalResource.class.getSimpleName());
        }

        if ((anyTypeKind != null || entityKey != null)
                && type != TaskType.PROPAGATION && type != TaskType.NOTIFICATION) {

            throw new IllegalArgumentException(type + " is not related to users, groups or any objects");
        }

        if (notification != null && type != TaskType.NOTIFICATION) {
            throw new IllegalArgumentException(type + " is not related to notifications");
        }

        List<Pair<String, String>> relationships = new ArrayList<>();
        List<String> nodeConditions = new ArrayList<>();

        if (anyTypeKind != null) {
            nodeConditions.add("n.anyTypeKind = $anyTypeKind");
            parameters.put("anyTypeKind", anyTypeKind.name());
        }
        if (entityKey != null) {
            nodeConditions.add("n.entityKey = $entityKey");
            parameters.put("entityKey", entityKey);
        }

        // OPTIONAL: tasks without executions are legitimate search results
        relationships.add(Pair.of("OPTIONAL MATCH (n)-[]-(p:" + taskUtils.getTaskExecStorage() + ")", null));

        if (resource != null) {
            relationships.add(Pair.of("MATCH (n)-[]-(e:" + Neo4jExternalResource.NODE + " {id: $eid})", null));
            parameters.put("eid", resource.getKey());
        }
        if (notification != null) {
            relationships.add(Pair.of("MATCH (n)-[]-(s:" + Neo4jNotification.NODE + " {id: $nid})", null));
            parameters.put("nid", notification.getKey());
        }

        if (type == TaskType.MACRO
                && !AuthContextUtils.getUsername().equals(securityProperties.getAdminUser())) {

            Stream<String> realmKeys = AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.TASK_LIST).stream().
                    map(realmSearchDAO::findByFullPath).
                    filter(Optional::isPresent).
                    flatMap(r -> realmSearchDAO.findDescendants(r.get().getFullPath(), null).
                            stream()).
                    map(Realm::getKey).
                    distinct();

            AtomicInteger index = new AtomicInteger(0);
            String realmCond = realmKeys.map(realm -> {
                int idx = index.incrementAndGet();
                parameters.put("realm" + idx, realm);
                return "q.id = $realm" + idx;
            }).collect(Collectors.joining(" OR "));

            relationships.add(Pair.of("MATCH (n)-[]-(q:" + Neo4jRealm.NODE + ")", "(" + realmCond + ")"));
        }

        StringBuilder query = new StringBuilder("MATCH (n:").append(taskUtils.getTaskStorage()).append(")");

        if (type == TaskType.SCHEDULED) {
            nodeConditions.add("NOT n:" + Neo4jMacroTask.NODE);
            nodeConditions.add("NOT n:" + Neo4jPullTask.NODE);
            nodeConditions.add("NOT n:" + Neo4jPushTask.NODE);
        }

        if (!nodeConditions.isEmpty()) {
            // conditions on n only must be part of the first MATCH: inside OPTIONAL MATCH they would not filter n
            // out, and the planner would not be able to use indexes on n
            query.append(" WHERE ").append(String.join(" AND ", nodeConditions));
        }

        query.append(relationships.stream().map(r -> " " + r.getLeft()).collect(Collectors.joining()));

        List<String> relatedConditions = relationships.stream().
                filter(r -> r.getRight() != null).
                map(Pair::getRight).
                toList();
        if (!relatedConditions.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", relatedConditions));
        }

        return query;
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey) {

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder query = query(
                type,
                taskUtilsFactory.getInstance(type),
                resource,
                notification,
                anyTypeKind,
                entityKey,
                parameters).
                append(" RETURN COUNT(DISTINCT n)");

        return neo4jTemplate.count(query.toString(), parameters);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Task<T>> List<T> findAll(
            final TaskType type,
            final ExternalResource resource,
            final Notification notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final Pageable pageable) {

        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder query = query(
                type,
                taskUtils,
                resource,
                notification,
                anyTypeKind,
                entityKey,
                parameters);

        query.append(" WITH n ");

        query.append(toOrderByStatement(
                taskUtilsFactory.getInstance(type).getTaskEntity(), pageable.getSort().stream()));

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        query.append(" RETURN DISTINCT n.id");

        return toList(neo4jClient.query(
                query.toString()).bindAll(parameters).fetch().all(),
                "n.id",
                (Class<AbstractTask<?>>) taskUtils.getTaskEntity(),
                null);
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task<?>> T save(final T task) {
        List<TaskExec<?>> execs = task.getExecs().stream().<TaskExec<?>>map(nodeValidator::validate).toList();
        if (!execs.isEmpty()) {
            neo4jTemplate.saveAll(execs);
        }

        T saved = neo4jTemplate.save(nodeValidator.validate(task));

        switch (saved) {
            case Neo4jPullTask pullTask ->
                neo4jTemplate.findById(pullTask.getKey(), Neo4jPullTask.class).ifPresent(t -> {
                    if (t.getReconFilterBuilder() != null && pullTask.getReconFilterBuilder() == null) {
                        deleteRelationship(
                                Neo4jPullTask.NODE,
                                Neo4jImplementation.NODE,
                                pullTask.getKey(),
                                t.getReconFilterBuilder().getKey(),
                                Neo4jPullTask.PULL_TASK_RECON_FILTER_BUIDER_REL);
                    }

                    t.getActions().stream().filter(act -> !pullTask.getActions().contains(act)).
                            forEach(impl -> deleteRelationship(Neo4jPullTask.NODE,
                                    Neo4jImplementation.NODE,
                                    pullTask.getKey(),
                                    impl.getKey(),
                                    Neo4jPullTask.PULL_TASK_INBOUND_ACTIONS_REL));
                });

            case Neo4jPushTask pushTask -> {
                neo4jTemplate.findById(pushTask.getKey(), Neo4jPushTask.class).
                        ifPresent(t -> t.getActions().stream().filter(act -> !pushTask.getActions().contains(act)).
                                forEach(impl -> deleteRelationship(
                                        Neo4jPushTask.NODE,
                                        Neo4jImplementation.NODE,
                                        pushTask.getKey(),
                                        impl.getKey(),
                                        Neo4jPushTask.PUSH_TASK_PUSH_ACTIONS_REL)));
            }

            case Neo4jMacroTask macroTask ->
                neo4jTemplate.findById(macroTask.getKey(), Neo4jMacroTask.class).ifPresent(t -> {
                    if (t.getMacroActions() != null && macroTask.getMacroActions() == null) {
                        deleteRelationship(
                                Neo4jMacroTask.NODE,
                                Neo4jImplementation.NODE,
                                macroTask.getKey(),
                                t.getMacroActions().getKey(),
                                Neo4jMacroTask.MACRO_TASK_MACRO_ACTIONS_REL);
                    }
                });

            default -> {
            }
        }

        return saved;
    }

    @Override
    public void delete(final TaskType type, final String key) {
        findById(type, key).ifPresent(this::delete);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }

    @Override
    public void delete(final Task<?> task) {
        switch (task) {
            case PullTask pullTask -> {
                remediationDAO.findByPullTask(pullTask).forEach(remediation -> remediation.setPullTask(null));
                neo4jTemplate.deleteAllById(
                        pullTask.getTemplates().stream().map(e -> e.getKey()).toList(),
                        Neo4jAnyTemplatePullTask.class);
            }

            case MacroTask macroTask -> {
                neo4jTemplate.deleteAllById(
                        macroTask.getCommands().stream().map(e -> e.getKey()).toList(),
                        Neo4jMacroTaskCommand.class);
                neo4jTemplate.deleteAllById(
                        macroTask.getFormPropertyDefs().stream().map(e -> e.getKey()).toList(),
                        Neo4jFormPropertyDef.class);
            }

            default -> {
            }
        }

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        neo4jTemplate.deleteAllById(
                task.getExecs().stream().map(e -> e.getKey()).toList(),
                taskUtils.getTaskExecEntity());

        neo4jTemplate.deleteById(task.getKey(), taskUtils.getTaskEntity());
    }

    @Override
    public void deleteAll(final ExternalResource resource, final TaskType type) {
        findAll(type, resource, null, null, null, Pageable.unpaged()).
                stream().map(Task<?>::getKey).forEach(key -> delete(type, key));
    }

    @Override
    public List<PropagationTaskTO> purgePropagations(
            final OffsetDateTime since,
            final List<ExecStatus> statuses,
            final List<String> resources) {

        Map<String, Object> parameters = new HashMap<>();

        StringBuilder query = new StringBuilder(
                "MATCH (n:" + Neo4jPropagationTask.NODE + ")-[]-(p:" + Neo4jPropagationTaskExec.NODE + ")");
        if (!CollectionUtils.isEmpty(resources)) {
            query.append(" MATCH (n)-[]-(r:" + Neo4jExternalResource.NODE + ")");
        }
        query.append(" WHERE 1=1");

        if (since != null) {
            parameters.put("since", since);
            query.append(" AND p.endDate <= $since");
        }
        if (!CollectionUtils.isEmpty(statuses)) {
            AtomicInteger index = new AtomicInteger(0);
            query.append(" AND (").
                    append(statuses.stream().map(status -> {
                        int idx = index.incrementAndGet();
                        parameters.put("status" + idx, status.name());
                        return "p.status = $status" + idx;
                    }).collect(Collectors.joining(" OR "))).
                    append(")");
        }
        if (!CollectionUtils.isEmpty(resources)) {
            AtomicInteger index = new AtomicInteger(0);
            query.append(" AND (").
                    append(resources.stream().map(r -> {
                        int idx = index.incrementAndGet();
                        parameters.put("rid" + idx, r);
                        return "r.id = $rid" + idx;
                    }).collect(Collectors.joining(" OR "))).
                    append(")");
        }

        query.append(" RETURN n.id");

        Stream<String> keys = neo4jClient.query(query.toString()).bindAll(parameters).fetch().all().stream().
                map(found -> (String) found.get("n.id")).distinct();

        List<PropagationTaskTO> purged = new ArrayList<>();
        keys.forEach(key -> findById(TaskType.PROPAGATION, key).map(PropagationTask.class::cast).ifPresent(task -> {
            PropagationTaskTO taskTO = new PropagationTaskTO();

            taskTO.setOperation(task.getOperation());
            taskTO.setConnObjectKey(task.getConnObjectKey());
            taskTO.setOldConnObjectKey(task.getOldConnObjectKey());
            taskTO.setPropagationData(task.getSerializedPropagationData());
            taskTO.setResource(task.getResource().getKey());
            taskTO.setObjectClassName(task.getObjectClassName());
            taskTO.setAnyTypeKind(task.getAnyTypeKind());
            taskTO.setAnyType(task.getAnyType());
            taskTO.setEntityKey(task.getEntityKey());

            purged.add(taskTO);

            delete(task);
        }));

        return purged;
    }
}
