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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.common.lib.CollectionUtils2;
import org.apache.syncope.core.persistence.api.GroupEntitlementUtil;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AttributableTransformer;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.security.AuthContextUtil;
import org.apache.syncope.core.misc.security.UnauthorizedGroupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Note that this controller does not extend {@link AbstractTransactionalLogic}, hence does not provide any
 * Spring's Transactional logic at class level.
 */
@Component
public class GroupLogic extends AbstractSubjectLogic<GroupTO, GroupMod> {

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected SubjectSearchDAO searchDAO;

    @Autowired
    protected GroupDataBinder binder;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AttributableTransformer attrTransformer;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Autowired
    protected GroupProvisioningManager provisioningManager;

    @PreAuthorize("hasAnyRole('GROUP_READ', T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    @Transactional(readOnly = true)
    @Override
    public GroupTO read(final Long groupKey) {
        Group group;
        // bypass group entitlements check
        if (anonymousUser.equals(AuthContextUtil.getAuthenticatedUsername())) {
            group = groupDAO.find(groupKey);
        } else {
            group = groupDAO.authFetch(groupKey);
        }

        if (group == null) {
            throw new NotFoundException("Group " + groupKey);
        }

        return binder.getGroupTO(group);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public GroupTO readSelf(final Long groupKey) {
        // Explicit search instead of using binder.getGroupFromId() in order to bypass auth checks - will do here
        Group group = groupDAO.find(groupKey);
        if (group == null) {
            throw new NotFoundException("Group " + groupKey);
        }

        Set<Long> ownedGroupIds;
        User authUser = userDAO.find(AuthContextUtil.getAuthenticatedUsername());
        if (authUser == null) {
            ownedGroupIds = Collections.<Long>emptySet();
        } else {
            ownedGroupIds = authUser.getGroupKeys();
        }

        Set<Long> allowedGroupIds = GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());
        allowedGroupIds.addAll(ownedGroupIds);
        if (!allowedGroupIds.contains(group.getKey())) {
            throw new UnauthorizedGroupException(group.getKey());
        }

        return binder.getGroupTO(group);
    }

    @PreAuthorize("hasRole('GROUP_READ')")
    @Transactional(readOnly = true)
    public GroupTO parent(final Long groupKey) {
        Group group = groupDAO.authFetch(groupKey);

        Set<Long> allowedGroupIds = GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());
        if (group.getParent() != null && !allowedGroupIds.contains(group.getParent().getKey())) {
            throw new UnauthorizedGroupException(group.getParent().getKey());
        }

        GroupTO result = group.getParent() == null
                ? null
                : binder.getGroupTO(group.getParent());

        return result;
    }

    @PreAuthorize("hasRole('GROUP_READ')")
    @Transactional(readOnly = true)
    public List<GroupTO> children(final Long groupKey) {
        Group group = groupDAO.authFetch(groupKey);
        final Set<Long> allowedGroupKeys =
                GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());

        return CollectionUtils2.collect(groupDAO.findChildren(group), new Transformer<Group, GroupTO>() {

            @Override
            public GroupTO transform(final Group group) {
                return binder.getGroupTO(group);
            }
        }, new Predicate<Group>() {

            @Override
            public boolean evaluate(final Group group) {
                return allowedGroupKeys.contains(group.getKey());
            }
        }, new ArrayList<GroupTO>());
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int count() {
        return groupDAO.count();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Override
    public List<GroupTO> list(final int page, final int size, final List<OrderByClause> orderBy) {
        return CollectionUtils.collect(groupDAO.findAll(page, size, orderBy), new Transformer<Group, GroupTO>() {

            @Override
            public GroupTO transform(final Group input) {
                return binder.getGroupTO(input);
            }
        }, new ArrayList<GroupTO>());
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int searchCount(final SearchCond searchCondition) {
        final Set<Long> adminGroupIds = GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());
        return searchDAO.count(adminGroupIds, searchCondition, SubjectType.GROUP);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public List<GroupTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy) {

        List<Group> matchingGroups = searchDAO.search(GroupEntitlementUtil.getGroupKeys(AuthContextUtil.
                getOwnedEntitlementNames()),
                searchCondition, page, size, orderBy, SubjectType.GROUP);
        return CollectionUtils.collect(matchingGroups, new Transformer<Group, GroupTO>() {

            @Override
            public GroupTO transform(final Group input) {
                return binder.getGroupTO(input);
            }
        }, new ArrayList<GroupTO>());
    }

    @PreAuthorize("hasRole('GROUP_CREATE')")
    public GroupTO create(final GroupTO groupTO) {
        // Check that this operation is allowed to be performed by caller
        Set<Long> allowedGroupIds = GroupEntitlementUtil.getGroupKeys(AuthContextUtil.getOwnedEntitlementNames());
        if (groupTO.getParent() != 0 && !allowedGroupIds.contains(groupTO.getParent())) {
            throw new UnauthorizedGroupException(groupTO.getParent());
        }

        // Attributable transformation (if configured)
        GroupTO actual = attrTransformer.transform(groupTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation
         */
        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(groupTO);
        final GroupTO savedTO = binder.getGroupTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("hasRole('GROUP_UPDATE')")
    @Override
    public GroupTO update(final GroupMod groupMod) {
        // Check that this operation is allowed to be performed by caller
        groupDAO.authFetch(groupMod.getKey());

        // Attribute value transformation (if configured)
        GroupMod actual = attrTransformer.transform(groupMod);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(groupMod);

        final GroupTO updatedTO = binder.getGroupTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    @PreAuthorize("hasRole('GROUP_DELETE')")
    @Override
    public GroupTO delete(final Long groupKey) {
        List<Group> ownedGroups = groupDAO.findOwnedByGroup(groupKey);
        if (!ownedGroups.isEmpty()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.GroupOwnership);
            sce.getElements().addAll(CollectionUtils.collect(ownedGroups, new Transformer<Group, String>() {

                @Override
                public String transform(final Group group) {
                    return group.getKey() + " " + group.getName();
                }
            }, new ArrayList<String>()));
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(groupKey);

        GroupTO groupTO = new GroupTO();
        groupTO.setKey(groupKey);

        groupTO.getPropagationStatusTOs().addAll(statuses);

        return groupTO;
    }

    @PreAuthorize("(hasRole('GROUP_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE)")
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult res = new BulkActionResult();

        if (bulkAction.getOperation() == BulkAction.Type.DELETE) {
            for (String groupKey : bulkAction.getTargets()) {
                try {
                    res.add(delete(Long.valueOf(groupKey)).getKey(), BulkActionResult.Status.SUCCESS);
                } catch (Exception e) {
                    LOG.error("Error performing delete for group {}", groupKey, e);
                    res.add(groupKey, BulkActionResult.Status.FAILURE);
                }
            }
        } else {
            LOG.warn("Unsupported bulk action: {}", bulkAction.getOperation());
        }

        return res;
    }

    @PreAuthorize("hasRole('GROUP_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public GroupTO unlink(final Long groupKey, final Collection<String> resources) {
        final GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupKey);
        groupMod.getResourcesToRemove().addAll(resources);
        final Long updatedResult = provisioningManager.unlink(groupMod);

        return binder.getGroupTO(updatedResult);
    }

    @PreAuthorize("hasRole('GROUP_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public GroupTO link(final Long groupKey, final Collection<String> resources) {
        final GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupKey);
        groupMod.getResourcesToAdd().addAll(resources);
        return binder.getGroupTO(provisioningManager.link(groupMod));
    }

    @PreAuthorize("hasRole('GROUP_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public GroupTO unassign(final Long groupKey, final Collection<String> resources) {
        final GroupMod groupMod = new GroupMod();
        groupMod.setKey(groupKey);
        groupMod.getResourcesToRemove().addAll(resources);
        return update(groupMod);
    }

    @PreAuthorize("hasRole('GROUP_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public GroupTO assign(
            final Long groupKey, final Collection<String> resources, final boolean changePwd, final String password) {

        final GroupMod userMod = new GroupMod();
        userMod.setKey(groupKey);
        userMod.getResourcesToAdd().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('GROUP_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public GroupTO deprovision(final Long groupKey, final Collection<String> resources) {
        final Group group = groupDAO.authFetch(groupKey);

        List<PropagationStatus> statuses = provisioningManager.deprovision(groupKey, resources);

        final GroupTO updatedTO = binder.getGroupTO(group);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('GROUP_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public GroupTO provision(
            final Long groupKey, final Collection<String> resources, final boolean changePwd, final String password) {
        final GroupTO original = binder.getGroupTO(groupKey);

        //trick: assign and retrieve propagation statuses ...
        original.getPropagationStatusTOs().addAll(
                assign(groupKey, resources, changePwd, password).getPropagationStatusTOs());

        // .... rollback.
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        return original;
    }

    @Override
    protected GroupTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Long key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof GroupTO) {
                    key = ((GroupTO) args[i]).getKey();
                } else if (args[i] instanceof GroupMod) {
                    key = ((GroupMod) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0l)) {
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
