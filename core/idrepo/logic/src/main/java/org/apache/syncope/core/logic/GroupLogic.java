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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.logic.api.LogicActions;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.GroupMemberProvisionTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
public class GroupLogic extends AbstractAnyLogic<GroupTO, GroupCR, GroupUR> {

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final SecurityProperties securityProperties;

    protected final AnySearchDAO searchDAO;

    protected final ImplementationDAO implementationDAO;

    protected final TaskDAO taskDAO;

    protected final GroupDataBinder binder;

    protected final GroupProvisioningManager provisioningManager;

    protected final TaskDataBinder taskDataBinder;

    protected final JobManager jobManager;

    protected final SyncopeTaskScheduler scheduler;

    protected final EntityFactory entityFactory;

    public GroupLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final SecurityProperties securityProperties,
            final AnySearchDAO searchDAO,
            final ImplementationDAO implementationDAO,
            final TaskDAO taskDAO,
            final GroupDataBinder binder,
            final GroupProvisioningManager provisioningManager,
            final TaskDataBinder taskDataBinder,
            final JobManager jobManager,
            final SyncopeTaskScheduler scheduler,
            final EntityFactory entityFactory) {

        super(realmSearchDAO, anyTypeDAO, templateUtils);

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.securityProperties = securityProperties;
        this.searchDAO = searchDAO;
        this.implementationDAO = implementationDAO;
        this.taskDAO = taskDAO;
        this.binder = binder;
        this.provisioningManager = provisioningManager;
        this.taskDataBinder = taskDataBinder;
        this.jobManager = jobManager;
        this.scheduler = scheduler;
        this.entityFactory = entityFactory;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public GroupTO read(final String key) {
        return binder.getGroupTO(key);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + IdRepoEntitlement.ANONYMOUS + "'))")
    @Transactional(readOnly = true)
    public List<GroupTO> own() {
        if (securityProperties.getAdminUser().equals(AuthContextUtils.getUsername())) {
            return List.of();
        }

        return userDAO.findAllGroups(
                userDAO.findByUsername(AuthContextUtils.getUsername()).
                        orElseThrow(() -> new NotFoundException("User " + AuthContextUtils.getUsername()))).stream().
                map(group -> binder.getGroupTO(group, true)).toList();
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public Page<GroupTO> search(
            final SearchCond searchCond,
            final Pageable pageable,
            final String realm,
            final boolean recursive,
            final boolean details) {

        Realm base = realmSearchDAO.findByFullPath(realm).
                orElseThrow(() -> new NotFoundException("Realm " + realm));

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.GROUP_SEARCH), realm);

        SearchCond effectiveCond = searchCond == null ? groupDAO.getAllMatchingCond() : searchCond;

        long count = searchDAO.count(base, recursive, authRealms, effectiveCond, AnyTypeKind.GROUP);

        List<Group> matching = searchDAO.search(
                base, recursive, authRealms, effectiveCond, pageable, AnyTypeKind.GROUP);
        List<GroupTO> result = matching.stream().
                map(group -> binder.getGroupTO(group, details)).
                toList();

        return new SyncopePage<>(result, pageable, count);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_CREATE + "')")
    public ProvisioningResult<GroupTO> create(final GroupCR createReq, final boolean nullPriorityAsync) {
        Pair<GroupCR, List<LogicActions>> before = beforeCreate(createReq);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.GROUP_CREATE),
                before.getLeft().getRealm());
        groupDAO.securityChecks(
                authRealms,
                null,
                before.getLeft().getRealm());

        Pair<String, List<PropagationStatus>> created = provisioningManager.create(
                before.getLeft(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        return afterCreate(binder.getGroupTO(created.getKey()), created.getRight(), before.getRight());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> update(final GroupUR req, final boolean nullPriorityAsync) {
        GroupTO groupTO = binder.getGroupTO(req.getKey());
        Pair<GroupUR, List<LogicActions>> before = beforeUpdate(req, groupTO.getRealm());

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.GROUP_UPDATE),
                groupTO.getRealm());
        groupDAO.securityChecks(
                authRealms,
                before.getLeft().getKey(),
                groupTO.getRealm());

        Pair<GroupUR, List<PropagationStatus>> after = provisioningManager.update(
                req, Set.of(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        ProvisioningResult<GroupTO> result = afterUpdate(
                binder.getGroupTO(after.getLeft().getKey()),
                after.getRight(),
                before.getRight());

        // check if group can still be managed by the caller
        authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.GROUP_UPDATE),
                result.getEntity().getRealm());
        groupDAO.securityChecks(
                authRealms,
                after.getLeft().getKey(),
                result.getEntity().getRealm());

        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_DELETE + "')")
    @Override
    public ProvisioningResult<GroupTO> delete(final String key, final boolean nullPriorityAsync) {
        Pair<GroupTO, List<LogicActions>> before = beforeDelete(binder.getGroupTO(key));

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.GROUP_DELETE),
                before.getLeft().getRealm());
        groupDAO.securityChecks(
                authRealms,
                before.getLeft().getKey(),
                before.getLeft().getRealm());

        List<Group> ownedGroups = groupDAO.findOwnedByGroup(before.getLeft().getKey());
        if (!ownedGroups.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(ownedGroups.stream().
                    map(g -> g.getKey() + ' ' + g.getName()).toList());
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(
                before.getLeft().getKey(), nullPriorityAsync, AuthContextUtils.getUsername(), REST_CONTEXT);

        GroupTO deletedTO;
        if (groupDAO.existsById(before.getLeft().getKey())) {
            deletedTO = binder.getGroupTO(before.getLeft().getKey());
        } else {
            deletedTO = new GroupTO();
            deletedTO.setKey(before.getLeft().getKey());
        }

        return afterDelete(deletedTO, statuses, before.getRight());
    }

    protected GroupTO updateChecks(final String key) {
        GroupTO group = binder.getGroupTO(key);

        Set<String> authRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.GROUP_UPDATE),
                group.getRealm());
        groupDAO.securityChecks(
                authRealms,
                group.getKey(),
                group.getRealm());

        return group;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO unlink(final String key, final Collection<String> resources) {
        GroupTO groupTO = updateChecks(key);

        GroupUR req = new GroupUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                        toList()).
                udynMembershipCond(groupTO.getUDynMembershipCond()).
                adynMembershipConds(groupTO.getADynMembershipConds()).
                build();

        return binder.getGroupTO(provisioningManager.unlink(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO link(final String key, final Collection<String> resources) {
        GroupTO groupTO = updateChecks(key);

        GroupUR req = new GroupUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                        toList()).
                udynMembershipCond(groupTO.getUDynMembershipCond()).
                adynMembershipConds(groupTO.getADynMembershipConds()).
                build();

        return binder.getGroupTO(provisioningManager.link(req, AuthContextUtils.getUsername(), REST_CONTEXT));
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        GroupTO groupTO = updateChecks(key);

        GroupUR req = new GroupUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(r).build()).
                        toList()).
                udynMembershipCond(groupTO.getUDynMembershipCond()).
                adynMembershipConds(groupTO.getADynMembershipConds()).
                build();

        return update(req, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> assign(
            final String key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        GroupTO groupTO = updateChecks(key);

        GroupUR req = new GroupUR.Builder(key).
                resources(resources.stream().
                        map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                        toList()).
                udynMembershipCond(groupTO.getUDynMembershipCond()).
                adynMembershipConds(groupTO.getADynMembershipConds()).
                build();

        return update(req, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> deprovision(
            final String key,
            final List<String> resources,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.deprovision(
                key, resources, nullPriorityAsync, AuthContextUtils.getUsername());

        ProvisioningResult<GroupTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getGroupTO(key));
        result.getPropagationStatuses().addAll(statuses);
        result.getPropagationStatuses().sort(Comparator.comparing(item -> resources.indexOf(item.getResource())));
        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> provision(
            final String key,
            final List<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        updateChecks(key);

        List<PropagationStatus> statuses = provisioningManager.provision(
                key, resources, nullPriorityAsync, AuthContextUtils.getUsername());

        ProvisioningResult<GroupTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getGroupTO(key));
        result.getPropagationStatuses().addAll(statuses);
        result.getPropagationStatuses().sort(Comparator.comparing(item -> resources.indexOf(item.getResource())));
        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.TASK_CREATE + "') "
            + "and hasRole('" + IdRepoEntitlement.TASK_EXECUTE + "')")
    @Transactional
    public ExecTO provisionMembers(final String key, final ProvisionAction action) {
        Group group = groupDAO.findById(key).orElseThrow(() -> new NotFoundException("Group " + key));

        Implementation jobDelegate = implementationDAO.findById(
                GroupMemberProvisionTaskJobDelegate.class.getSimpleName()).
                orElseThrow(() -> new NotFoundException(
                "Implementation " + GroupMemberProvisionTaskJobDelegate.class.getSimpleName()));

        String name = (action == ProvisionAction.DEPROVISION ? "de" : "")
                + "provision members of group " + group.getName();
        SchedTask task = taskDAO.<SchedTask>findByName(TaskType.SCHEDULED, name).
                orElseGet(() -> {
                    SchedTask t = entityFactory.newEntity(SchedTask.class);
                    t.setName(name);
                    return t;
                });
        task.setActive(true);
        task.setJobDelegate(jobDelegate);
        task = taskDAO.save(task);

        try {
            jobManager.register(
                    task,
                    OffsetDateTime.now().plusSeconds(1),
                    AuthContextUtils.getUsername(),
                    false,
                    Map.of(GroupMemberProvisionTaskJobDelegate.GROUP_KEY_JOBDETAIL_KEY, key,
                            GroupMemberProvisionTaskJobDelegate.ACTION_JOBDETAIL_KEY, action));
        } catch (Exception e) {
            LOG.error("While executing task {}", task, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.Scheduling);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        ExecTO result = new ExecTO();
        result.setJobType(JobType.TASK);
        result.setRefKey(task.getKey());
        result.setRefDesc(taskDataBinder.buildRefDesc(task));
        result.setStart(OffsetDateTime.now());
        result.setStatus(JobStatusDAO.JOB_FIRED_STATUS);
        result.setMessage("Job fired; waiting for results...");

        return result;
    }

    @Override
    protected GroupTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof GroupTO groupTO) {
                    key = groupTO.getKey();
                } else if (args[i] instanceof GroupUR groupUR) {
                    key = groupUR.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getGroupTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
