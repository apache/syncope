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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.BulkMembersActionType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.java.job.GroupMemberProvisionTaskJobDelegate;
import org.apache.syncope.core.provisioning.java.job.TaskJob;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.quartz.JobDataMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class GroupLogic extends AbstractAnyLogic<GroupTO, GroupPatch> {

    @Resource(name = "adminUser")
    protected String adminUser;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected ConfDAO confDAO;

    @Autowired
    protected GroupDataBinder binder;

    @Autowired
    protected GroupProvisioningManager provisioningManager;

    @Autowired
    protected TaskDataBinder taskDataBinder;

    @Autowired
    protected JobManager jobManager;

    @Autowired
    protected SchedulerFactoryBean scheduler;

    @Autowired
    protected EntityFactory entityFactory;

    @Override
    protected boolean securityChecks(final Set<String> effectiveRealms, final String realm, final String key) {
        boolean authorized = effectiveRealms.stream().anyMatch(ownedRealm
                -> realm.startsWith(ownedRealm) || ownedRealm.equals(RealmUtils.getGroupOwnerRealm(realm, key)));
        if (!authorized) {
            authorized = groupDAO.findDynRealms(key).stream().
                    filter(dynRealm -> effectiveRealms.contains(dynRealm)).findFirst().isPresent();
        }
        if (!authorized) {
            throw new DelegatedAdministrationException(realm, AnyTypeKind.GROUP.name(), key);
        }

        return effectiveRealms.stream().anyMatch(new RealmUtils.DynRealmsPredicate());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_READ + "')")
    @Transactional(readOnly = true)
    @Override
    public GroupTO read(final String key) {
        return binder.getGroupTO(key);
    }

    @PreAuthorize("isAuthenticated() and not(hasRole('" + StandardEntitlement.ANONYMOUS + "'))")
    @Transactional(readOnly = true)
    public List<GroupTO> own() {
        if (adminUser.equals(AuthContextUtils.getUsername())) {
            return Collections.emptyList();
        }

        return userDAO.findAllGroups(userDAO.findByUsername(AuthContextUtils.getUsername())).stream().
                map(group -> binder.getGroupTO(group, true)).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_SEARCH + "')")
    @Transactional(readOnly = true)
    @Override
    public Pair<Integer, List<GroupTO>> search(
            final SearchCond searchCond,
            final int page, final int size, final List<OrderByClause> orderBy,
            final String realm,
            final boolean details) {

        int count = searchDAO.count(
                RealmUtils.getEffective(SyncopeConstants.FULL_ADMIN_REALMS, realm),
                searchCond == null ? groupDAO.getAllMatchingCond() : searchCond, AnyTypeKind.GROUP);

        List<Group> matching = searchDAO.search(
                RealmUtils.getEffective(SyncopeConstants.FULL_ADMIN_REALMS, realm),
                searchCond == null ? groupDAO.getAllMatchingCond() : searchCond,
                page, size, orderBy, AnyTypeKind.GROUP);
        List<GroupTO> result = matching.stream().
                map(group -> binder.getGroupTO(group, details)).collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_CREATE + "')")
    @Override
    public ProvisioningResult<GroupTO> create(final GroupTO groupTO, final boolean nullPriorityAsync) {
        Pair<GroupTO, List<LogicActions>> before = beforeCreate(groupTO);

        if (before.getLeft().getRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_CREATE),
                before.getLeft().getRealm());
        securityChecks(effectiveRealms, before.getLeft().getRealm(), null);

        Pair<String, List<PropagationStatus>> created =
                provisioningManager.create(before.getLeft(), nullPriorityAsync);

        return afterCreate(binder.getGroupTO(created.getKey()), created.getRight(), before.getRight());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> update(final GroupPatch groupPatch, final boolean nullPriorityAsync) {
        GroupTO groupTO = binder.getGroupTO(groupPatch.getKey());
        Set<String> dynRealmsBefore = new HashSet<>(groupTO.getDynRealms());
        Pair<GroupPatch, List<LogicActions>> before = beforeUpdate(groupPatch, groupTO.getRealm());

        String realm =
                before.getLeft().getRealm() != null && StringUtils.isNotBlank(before.getLeft().getRealm().getValue())
                ? before.getLeft().getRealm().getValue()
                : groupTO.getRealm();
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_UPDATE),
                realm);
        boolean authDynRealms = securityChecks(effectiveRealms, realm, before.getLeft().getKey());

        Pair<GroupPatch, List<PropagationStatus>> updated =
                provisioningManager.update(groupPatch, nullPriorityAsync);

        return afterUpdate(
                binder.getGroupTO(updated.getLeft().getKey()),
                updated.getRight(),
                before.getRight(),
                authDynRealms,
                dynRealmsBefore);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_DELETE + "')")
    @Override
    public ProvisioningResult<GroupTO> delete(final String key, final boolean nullPriorityAsync) {
        GroupTO group = binder.getGroupTO(key);
        Pair<GroupTO, List<LogicActions>> before = beforeDelete(group);

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_DELETE),
                before.getLeft().getRealm());
        securityChecks(effectiveRealms, before.getLeft().getRealm(), before.getLeft().getKey());

        List<Group> ownedGroups = groupDAO.findOwnedByGroup(before.getLeft().getKey());
        if (!ownedGroups.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(ownedGroups.stream().
                    map(g -> g.getKey() + " " + g.getName()).collect(Collectors.toList()));
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(before.getLeft().getKey(), nullPriorityAsync);

        GroupTO groupTO = new GroupTO();
        groupTO.setKey(before.getLeft().getKey());

        return afterDelete(groupTO, statuses, before.getRight());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO unlink(final String key, final Collection<String> resources) {
        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_UPDATE),
                group.getRealm());
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupPatch patch = new GroupPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().
                map(resource -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build()).
                collect(Collectors.toList()));

        return binder.getGroupTO(provisioningManager.unlink(patch));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_UPDATE + "')")
    @Override
    public GroupTO link(final String key, final Collection<String> resources) {
        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_UPDATE),
                group.getRealm());
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupPatch patch = new GroupPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build()).
                collect(Collectors.toList()));

        return binder.getGroupTO(provisioningManager.link(patch));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> unassign(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_UPDATE),
                group.getRealm());
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupPatch patch = new GroupPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.DELETE).value(resource).build()).
                collect(Collectors.toList()));

        return update(patch, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> assign(
            final String key,
            final Collection<String> resources,
            final boolean changepwd,
            final String password,
            final boolean nullPriorityAsync) {

        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_UPDATE),
                group.getRealm());
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        GroupPatch patch = new GroupPatch();
        patch.setKey(key);
        patch.getResources().addAll(resources.stream().map(resource
                -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(resource).build()).
                collect(Collectors.toList()));

        return update(patch, nullPriorityAsync);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> deprovision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_UPDATE),
                group.getRealm());
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        List<PropagationStatus> statuses = provisioningManager.deprovision(key, resources, nullPriorityAsync);

        ProvisioningResult<GroupTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getGroupTO(key));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.GROUP_UPDATE + "')")
    @Override
    public ProvisioningResult<GroupTO> provision(
            final String key,
            final Collection<String> resources,
            final boolean changePwd,
            final String password,
            final boolean nullPriorityAsync) {

        // security checks
        GroupTO group = binder.getGroupTO(key);
        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.GROUP_UPDATE),
                group.getRealm());
        securityChecks(effectiveRealms, group.getRealm(), group.getKey());

        List<PropagationStatus> statuses = provisioningManager.provision(key, resources, nullPriorityAsync);

        ProvisioningResult<GroupTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getGroupTO(key));
        result.getPropagationStatuses().addAll(statuses);
        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.TASK_CREATE + "') "
            + "and hasRole('" + StandardEntitlement.TASK_EXECUTE + "')")
    @Transactional
    public ExecTO bulkMembersAction(final String key, final BulkMembersActionType actionType) {
        Group group = groupDAO.find(key);
        if (group == null) {
            throw new NotFoundException("Group " + key);
        }

        SchedTask task = entityFactory.newEntity(SchedTask.class);
        task.setName("Bulk member provision for group " + group.getName());
        task.setActive(true);
        task.setJobDelegateClassName(GroupMemberProvisionTaskJobDelegate.class.getName());
        task = taskDAO.save(task);

        try {
            Map<String, Object> jobDataMap = jobManager.register(
                    task,
                    null,
                    confDAO.find("tasks.interruptMaxRetries", 1L));

            jobDataMap.put(TaskJob.DRY_RUN_JOBDETAIL_KEY, false);
            jobDataMap.put(GroupMemberProvisionTaskJobDelegate.GROUP_KEY_JOBDETAIL_KEY, key);
            jobDataMap.put(GroupMemberProvisionTaskJobDelegate.ACTION_TYPE_JOBDETAIL_KEY, actionType);

            scheduler.getScheduler().triggerJob(
                    JobNamer.getJobKey(task),
                    new JobDataMap(jobDataMap));
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
        result.setStart(new Date());
        result.setStatus("JOB_FIRED");
        result.setMessage("Job fired; waiting for results...");

        return result;
    }

    @Override
    protected GroupTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof GroupTO) {
                    key = ((GroupTO) args[i]).getKey();
                } else if (args[i] instanceof GroupPatch) {
                    key = ((GroupPatch) args[i]).getKey();
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
