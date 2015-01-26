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
package org.apache.syncope.server.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.server.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.server.persistence.api.dao.UserDAO;
import org.apache.syncope.server.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.server.persistence.api.dao.search.SearchCond;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.persistence.api.entity.user.User;
import org.apache.syncope.server.provisioning.api.AttributableTransformer;
import org.apache.syncope.server.provisioning.api.RoleProvisioningManager;
import org.apache.syncope.server.provisioning.api.data.RoleDataBinder;
import org.apache.syncope.server.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.server.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.server.misc.security.AuthContextUtil;
import org.apache.syncope.server.misc.security.UnauthorizedRoleException;
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
public class RoleLogic extends AbstractSubjectLogic<RoleTO, RoleMod> {

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected SubjectSearchDAO searchDAO;

    @Autowired
    protected RoleDataBinder binder;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AttributableTransformer attrTransformer;

    @Resource(name = "anonymousUser")
    protected String anonymousUser;

    @Autowired
    protected RoleProvisioningManager provisioningManager;

    @PreAuthorize("hasAnyRole('ROLE_READ', T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    @Transactional(readOnly = true)
    @Override
    public RoleTO read(final Long roleKey) {
        Role role;
        // bypass role entitlements check
        if (anonymousUser.equals(AuthContextUtil.getAuthenticatedUsername())) {
            role = roleDAO.find(roleKey);
        } else {
            role = roleDAO.authFetch(roleKey);
        }

        if (role == null) {
            throw new NotFoundException("Role " + roleKey);
        }

        return binder.getRoleTO(role);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.lib.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public RoleTO readSelf(final Long roleKey) {
        // Explicit search instead of using binder.getRoleFromId() in order to bypass auth checks - will do here
        Role role = roleDAO.find(roleKey);
        if (role == null) {
            throw new NotFoundException("Role " + roleKey);
        }

        Set<Long> ownedRoleIds;
        User authUser = userDAO.find(AuthContextUtil.getAuthenticatedUsername());
        if (authUser == null) {
            ownedRoleIds = Collections.<Long>emptySet();
        } else {
            ownedRoleIds = authUser.getRoleKeys();
        }

        Set<Long> allowedRoleIds = RoleEntitlementUtil.getRoleKeys(AuthContextUtil.getOwnedEntitlementNames());
        allowedRoleIds.addAll(ownedRoleIds);
        if (!allowedRoleIds.contains(role.getKey())) {
            throw new UnauthorizedRoleException(role.getKey());
        }

        return binder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public RoleTO parent(final Long roleKey) {
        Role role = roleDAO.authFetch(roleKey);

        Set<Long> allowedRoleIds = RoleEntitlementUtil.getRoleKeys(AuthContextUtil.getOwnedEntitlementNames());
        if (role.getParent() != null && !allowedRoleIds.contains(role.getParent().getKey())) {
            throw new UnauthorizedRoleException(role.getParent().getKey());
        }

        RoleTO result = role.getParent() == null
                ? null
                : binder.getRoleTO(role.getParent());

        return result;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public List<RoleTO> children(final Long roleKey) {
        Role role = roleDAO.authFetch(roleKey);

        Set<Long> allowedRoleIds = RoleEntitlementUtil.getRoleKeys(AuthContextUtil.getOwnedEntitlementNames());

        List<Role> children = roleDAO.findChildren(role);
        List<RoleTO> childrenTOs = new ArrayList<>(children.size());
        for (Role child : children) {
            if (allowedRoleIds.contains(child.getKey())) {
                childrenTOs.add(binder.getRoleTO(child));
            }
        }

        return childrenTOs;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int count() {
        return roleDAO.count();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Override
    public List<RoleTO> list(final int page, final int size, final List<OrderByClause> orderBy) {
        List<Role> roles = roleDAO.findAll(page, size, orderBy);

        List<RoleTO> roleTOs = new ArrayList<>(roles.size());
        for (Role role : roles) {
            roleTOs.add(binder.getRoleTO(role));
        }

        return roleTOs;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public int searchCount(final SearchCond searchCondition) {
        final Set<Long> adminRoleIds = RoleEntitlementUtil.getRoleKeys(AuthContextUtil.getOwnedEntitlementNames());
        return searchDAO.count(adminRoleIds, searchCondition, SubjectType.ROLE);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    @Override
    public List<RoleTO> search(final SearchCond searchCondition, final int page, final int size,
            final List<OrderByClause> orderBy) {

        final List<Role> matchingRoles = searchDAO.search(
                RoleEntitlementUtil.getRoleKeys(AuthContextUtil.getOwnedEntitlementNames()),
                searchCondition, page, size, orderBy, SubjectType.ROLE);

        final List<RoleTO> result = new ArrayList<>(matchingRoles.size());
        for (Role role : matchingRoles) {
            result.add(binder.getRoleTO(role));
        }

        return result;
    }

    @PreAuthorize("hasRole('ROLE_CREATE')")
    public RoleTO create(final RoleTO roleTO) {
        // Check that this operation is allowed to be performed by caller
        Set<Long> allowedRoleIds = RoleEntitlementUtil.getRoleKeys(AuthContextUtil.getOwnedEntitlementNames());
        if (roleTO.getParent() != 0 && !allowedRoleIds.contains(roleTO.getParent())) {
            throw new UnauthorizedRoleException(roleTO.getParent());
        }

        // Attributable transformation (if configured)
        RoleTO actual = attrTransformer.transform(roleTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation
         */
        Map.Entry<Long, List<PropagationStatus>> created = provisioningManager.create(roleTO);
        final RoleTO savedTO = binder.getRoleTO(created.getKey());
        savedTO.getPropagationStatusTOs().addAll(created.getValue());
        return savedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Override
    public RoleTO update(final RoleMod roleMod) {
        // Check that this operation is allowed to be performed by caller
        roleDAO.authFetch(roleMod.getKey());

        // Attribute value transformation (if configured)
        RoleMod actual = attrTransformer.transform(roleMod);
        LOG.debug("Transformed: {}", actual);

        Map.Entry<Long, List<PropagationStatus>> updated = provisioningManager.update(roleMod);

        final RoleTO updatedTO = binder.getRoleTO(updated.getKey());
        updatedTO.getPropagationStatusTOs().addAll(updated.getValue());
        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_DELETE')")
    @Override
    public RoleTO delete(final Long roleKey) {
        List<Role> ownedRoles = roleDAO.findOwnedByRole(roleKey);
        if (!ownedRoles.isEmpty()) {
            List<String> owned = new ArrayList<String>(ownedRoles.size());
            for (Role role : ownedRoles) {
                owned.add(role.getKey() + " " + role.getName());
            }

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RoleOwnership);
            sce.getElements().addAll(owned);
            throw sce;
        }

        List<PropagationStatus> statuses = provisioningManager.delete(roleKey);

        RoleTO roleTO = new RoleTO();
        roleTO.setKey(roleKey);

        roleTO.getPropagationStatusTOs().addAll(statuses);

        return roleTO;
    }

    @PreAuthorize("(hasRole('ROLE_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE)")
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult res = new BulkActionResult();

        if (bulkAction.getOperation() == BulkAction.Type.DELETE) {
            for (String roleKey : bulkAction.getTargets()) {
                try {
                    res.add(delete(Long.valueOf(roleKey)).getKey(), BulkActionResult.Status.SUCCESS);
                } catch (Exception e) {
                    LOG.error("Error performing delete for role {}", roleKey, e);
                    res.add(roleKey, BulkActionResult.Status.FAILURE);
                }
            }
        } else {
            LOG.warn("Unsupported bulk action: {}", bulkAction.getOperation());
        }

        return res;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO unlink(final Long roleKey, final Collection<String> resources) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleKey);
        roleMod.getResourcesToRemove().addAll(resources);
        final Long updatedResult = provisioningManager.unlink(roleMod);

        return binder.getRoleTO(updatedResult);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO link(final Long roleKey, final Collection<String> resources) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleKey);
        roleMod.getResourcesToAdd().addAll(resources);
        return binder.getRoleTO(provisioningManager.link(roleMod));
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO unassign(final Long roleKey, final Collection<String> resources) {
        final RoleMod roleMod = new RoleMod();
        roleMod.setKey(roleKey);
        roleMod.getResourcesToRemove().addAll(resources);
        return update(roleMod);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO assign(
            final Long roleKey, final Collection<String> resources, final boolean changePwd, final String password) {

        final RoleMod userMod = new RoleMod();
        userMod.setKey(roleKey);
        userMod.getResourcesToAdd().addAll(resources);
        return update(userMod);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO deprovision(final Long roleKey, final Collection<String> resources) {
        final Role role = roleDAO.authFetch(roleKey);

        List<PropagationStatus> statuses = provisioningManager.deprovision(roleKey, resources);

        final RoleTO updatedTO = binder.getRoleTO(role);
        updatedTO.getPropagationStatusTOs().addAll(statuses);
        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    @Override
    public RoleTO provision(
            final Long roleKey, final Collection<String> resources, final boolean changePwd, final String password) {
        final RoleTO original = binder.getRoleTO(roleKey);

        //trick: assign and retrieve propagation statuses ...
        original.getPropagationStatusTOs().addAll(
                assign(roleKey, resources, changePwd, password).getPropagationStatusTOs());

        // .... rollback.
        TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        return original;
    }

    @Override
    protected RoleTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Long key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof RoleTO) {
                    key = ((RoleTO) args[i]).getKey();
                } else if (args[i] instanceof RoleMod) {
                    key = ((RoleMod) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0l)) {
            try {
                return binder.getRoleTO(key);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
