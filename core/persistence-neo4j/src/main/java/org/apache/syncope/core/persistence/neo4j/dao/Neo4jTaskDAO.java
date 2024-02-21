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
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
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
import org.apache.syncope.core.persistence.api.entity.task.TaskUtils;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.task.AbstractTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jAnyTemplatePullTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTask;
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

public class Neo4jTaskDAO extends AbstractDAO implements TaskDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(TaskDAO.class);

    protected final RealmDAO realmDAO;

    protected final RemediationDAO remediationDAO;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final SecurityProperties securityProperties;

    protected final NodeValidator nodeValidator;

    public Neo4jTaskDAO(
            final RealmDAO realmDAO,
            final RemediationDAO remediationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final SecurityProperties securityProperties,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        super(neo4jTemplate, neo4jClient);
        this.realmDAO = realmDAO;
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
                "MATCH (n:" + taskUtils.getTaskTable() + ") WHERE n.name = $name RETURN n.id").
                bindAll(Map.of("name", name)).fetch().one().
                flatMap(toOptional("n.id", (Class<AbstractTask<?>>) taskUtils.getTaskEntity()));
    }

    @Override
    public Optional<? extends Task<?>> findById(final String key) {
        Optional<? extends Task<?>> task = findById(TaskType.SCHEDULED, key);
        if (task.isEmpty()) {
            task = findById(TaskType.PULL, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.PUSH, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.MACRO, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.PROPAGATION, key);
        }
        if (task.isEmpty()) {
            task = findById(TaskType.NOTIFICATION, key);
        }

        return task;
    }

    @Override
    public List<SchedTask> findByDelegate(final Implementation delegate) {
        return findByRelationship(
                Neo4jSchedTask.NODE, Neo4jSchedTaskExec.NODE, delegate.getKey(), Neo4jSchedTask.class);
    }

    @Override
    public List<PullTask> findByReconFilterBuilder(final Implementation reconFilterBuilder) {
        return findByRelationship(
                Neo4jPullTask.NODE, Neo4jImplementation.NODE, reconFilterBuilder.getKey(), Neo4jPullTask.class);
    }

    @Override
    public List<PullTask> findByPullActions(final Implementation pullActions) {
        return findByRelationship(
                Neo4jPullTask.NODE, Neo4jImplementation.NODE, pullActions.getKey(), Neo4jPullTask.class);
    }

    @Override
    public List<PushTask> findByPushActions(final Implementation pushActions) {
        return findByRelationship(
                Neo4jPushTask.NODE, Neo4jImplementation.NODE, pushActions.getKey(), Neo4jPushTask.class);
    }

    @Override
    public List<MacroTask> findByRealm(final Realm realm) {
        return findByRelationship(Neo4jMacroTask.NODE, Neo4jRealm.NODE, realm.getKey(), Neo4jMacroTask.class);
    }

    @Override
    public List<MacroTask> findByCommand(final Implementation command) {
        return findByRelationship(
                Neo4jMacroTask.NODE, Neo4jImplementation.NODE, command.getKey(), Neo4jMacroTask.class);
    }

    protected String execRelationship(final TaskType type) {
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

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Task<T>> List<T> findToExec(final TaskType type) {
        TaskUtils taskUtils = taskUtilsFactory.getInstance(type);
        StringBuilder queryString = new StringBuilder("MATCH (n:" + taskUtils.getTaskTable() + ") WHERE ");

        if (type == TaskType.NOTIFICATION) {
            queryString.append("n.executed = false ");
        } else {
            queryString.append("(n)-[:").append(execRelationship(type)).append("]-() ");
        }
        queryString.append("RETURN n.id ORDER BY n.id DESC");

        return toList(neo4jClient.query(queryString.toString()).fetch().all(),
                "n.id",
                (Class<AbstractTask<?>>) taskUtils.getTaskEntity());
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
            final boolean orderByTaskExecInfo,
            final Map<String, Object> parameters) {

        if (resource != null
                && type != TaskType.PROPAGATION && type != TaskType.PUSH && type != TaskType.PULL) {

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
        List<String> properties = new ArrayList<>();

        if (orderByTaskExecInfo) {
            relationships.add(Pair.of("(p:" + taskUtils.getTaskExecTable() + ")", "EXISTS((n)-[]-(p))"));
        }

        if (resource != null) {
            relationships.add(Pair.of("(e:" + Neo4jExternalResource.NODE + " {id: $eid})", "EXISTS((n)-[]-(e))"));
            parameters.put("eid", resource.getKey());
        }
        if (notification != null) {
            relationships.add(Pair.of("(s:" + Neo4jExternalResource.NODE + " {id: $sid})", "EXISTS((n)-[]-(s))"));
            parameters.put("sid", notification.getKey());
        }
        if (anyTypeKind != null) {
            properties.add("anyTypeKind: $anyTypeKind");
            parameters.put("anyTypeKind", anyTypeKind);
        }
        if (entityKey != null) {
            properties.add("entityKey: $entityKey");
            parameters.put("entityKey", entityKey);
        }
        if (type == TaskType.MACRO
                && !AuthContextUtils.getUsername().equals(securityProperties.getAdminUser())) {

            Stream<String> realmKeys = AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.TASK_LIST).stream().
                    map(realmDAO::findByFullPath).
                    filter(Optional::isPresent).
                    flatMap(r -> realmDAO.findDescendants(r.get().getFullPath(), null, Pageable.unpaged()).stream()).
                    map(Realm::getKey).
                    distinct();

            AtomicInteger index = new AtomicInteger(0);
            String realmCond = realmKeys.map(realm -> {
                int idx = index.incrementAndGet();
                parameters.put("realm" + idx, realm);
                return "q.id: $realm," + idx;
            }).collect(Collectors.joining(" OR "));

            relationships.add(Pair.of("(q:" + Neo4jRealm.NODE + ")", "EXISTS((n)-[]-(q) AND (" + realmCond + ")"));
        }

        StringBuilder queryString = new StringBuilder("MATCH (n:").append(taskUtils.getTaskTable()).append(")");
        if (!relationships.isEmpty()) {
            queryString.append(", ").
                    append(relationships.stream().map(Pair::getLeft).collect(Collectors.joining(", ")));

            properties.addAll(relationships.stream().map(Pair::getRight).toList());
        }

        if (type == TaskType.SCHEDULED) {
            properties.add("NOT n:" + Neo4jMacroTask.NODE);
            properties.add("NOT n:" + Neo4jPullTask.NODE);
            properties.add("NOT n:" + Neo4jPushTask.NODE);
        }

        if (!properties.isEmpty()) {
            queryString.append(" WHERE ").append(properties.stream().collect(Collectors.joining(" AND ")));
        }

        return queryString;
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

        StringBuilder queryString = query(
                type,
                taskUtilsFactory.getInstance(type),
                resource,
                notification,
                anyTypeKind,
                entityKey,
                false,
                parameters).
                append(" RETURN COUNT(n)");
        return neo4jTemplate.count(queryString.toString(), parameters);
    }

    protected String toOrderByStatement(
            final Class<? extends Task<?>> beanClass,
            final Stream<Sort.Order> orderByClauses) {

        StringBuilder statement = new StringBuilder();

        statement.append(" ORDER BY ");

        StringBuilder subStatement = new StringBuilder();
        orderByClauses.forEach(clause -> {
            String field = clause.getProperty().trim();
            switch (field) {
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
            }

            subStatement.append("n.").append(field).append(' ').append(clause.getDirection().name()).append(',');
        });

        if (subStatement.length() == 0) {
            statement.append("n.id DESC");
        } else {
            subStatement.deleteCharAt(subStatement.length() - 1);
            statement.append(subStatement);
        }

        return statement.toString();
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

        boolean orderByTaskExecInfo = pageable.getSort().stream().
                anyMatch(clause -> clause.getProperty().equals("start")
                || clause.getProperty().equals("end")
                || clause.getProperty().equals("latestExecStatus")
                || clause.getProperty().equals("status"));

        StringBuilder query = query(
                type,
                taskUtils,
                resource,
                notification,
                anyTypeKind,
                entityKey,
                orderByTaskExecInfo,
                parameters);

        query.append(" RETURN n.id ");

        if (orderByTaskExecInfo) {
            // UNION with tasks without executions...
            query.append("UNION ").
                    append(query(
                            type,
                            taskUtils,
                            resource,
                            notification,
                            anyTypeKind,
                            entityKey,
                            false,
                            parameters)).
                    append(" RETURN n.id ");
        }

        query.append(toOrderByStatement(taskUtils.getTaskEntity(), pageable.getSort().get()));

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        return toList(neo4jClient.query(
                query.toString()).bindAll(parameters).fetch().all(),
                "n.id",
                (Class<AbstractTask<?>>) taskUtils.getTaskEntity());
    }

    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public <T extends Task<?>> T save(final T task) {
        task.getExecs().forEach(exec -> neo4jTemplate.save(nodeValidator.validate(exec)));

        switch (task) {
            case Neo4jNotificationTask notificationTask ->
                notificationTask.list2json();
            case Neo4jPushTask pushTask ->
                pushTask.map2json();
            case Neo4jMacroTask macroTask ->
                macroTask.list2json();
            default -> {
            }
        }

        T saved = neo4jTemplate.save(nodeValidator.validate(task));

        switch (saved) {
            case Neo4jNotificationTask notificationTask ->
                notificationTask.postSave();
            case Neo4jPushTask pushTask ->
                pushTask.postSave();
            case Neo4jMacroTask macroTask ->
                macroTask.postSave();
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
        if (task instanceof PullTask pullTask) {
            remediationDAO.findByPullTask(pullTask).forEach(remediation -> remediation.setPullTask(null));
            pullTask.getTemplates().
                    forEach(template -> neo4jTemplate.deleteById(template.getKey(), Neo4jAnyTemplatePullTask.class));
        }

        TaskUtils taskUtils = taskUtilsFactory.getInstance(task);

        task.getExecs().forEach(exec -> neo4jTemplate.deleteById(exec.getKey(), taskUtils.getTaskExecEntity()));

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

        StringBuilder queryString = new StringBuilder(
                "MATCH (n:" + Neo4jPropagationTask.NODE + ")-[]-"
                + "(p:" + Neo4jPropagationTaskExec.NODE + ")");
        if (!CollectionUtils.isEmpty(resources)) {
            queryString.append("-[]-(r:" + Neo4jExternalResource.NODE + ")");
        }
        queryString.append(" WHERE 1=1 ");

        if (since != null) {
            parameters.put("since", since);
            queryString.append("AND p.enddate <= $since ");
        }
        if (!CollectionUtils.isEmpty(statuses)) {
            AtomicInteger index = new AtomicInteger(0);
            queryString.append("AND (").
                    append(statuses.stream().map(status -> {
                        int idx = index.incrementAndGet();
                        parameters.put("status" + idx, status.name());
                        return "p.status = $status" + idx;
                    }).collect(Collectors.joining(" OR "))).
                    append(")");
        }
        if (!CollectionUtils.isEmpty(resources)) {
            queryString.append("AND (").
                    append(resources.stream().map(r -> {
                        AtomicInteger index = new AtomicInteger(0);
                        int idx = index.incrementAndGet();
                        parameters.put("r.id" + idx, r);
                        return "r.id = $r.id" + idx;
                    }).collect(Collectors.joining(" OR "))).
                    append(")");
        }

        queryString.append("RETURN n.id");

        Stream<String> keys = neo4jClient.query(queryString.toString()).bindAll(parameters).fetch().all().stream().
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
