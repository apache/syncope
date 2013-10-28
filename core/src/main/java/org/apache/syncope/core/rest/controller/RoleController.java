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
package org.apache.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditElements.RoleSubCategory;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AttributableSearchDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.AttributableTransformer;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleController {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected AttributableSearchDAO searchDAO;

    @Autowired
    protected RoleDataBinder binder;

    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected AttributableTransformer attrTransformer;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @PreAuthorize("hasAnyRole('ROLE_READ', T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT)")
    @Transactional(readOnly = true)
    public RoleTO read(final Long roleId) {
        SyncopeRole role;
        // bypass role entitlements check
        if (anonymousUser.equals(EntitlementUtil.getAuthenticatedUsername())) {
            role = roleDAO.find(roleId);
        } else {
            role = binder.getRoleFromId(roleId);
        }

        auditManager.audit(Category.role, RoleSubCategory.read, Result.success,
                "Successfully read role: " + role.getId());

        return binder.getRoleTO(role);
    }

    @PreAuthorize("isAuthenticated() "
            + "and not(hasRole(T(org.apache.syncope.common.SyncopeConstants).ANONYMOUS_ENTITLEMENT))")
    @Transactional(readOnly = true)
    public RoleTO selfRead(final Long roleId) {
        // Explicit search instead of using binder.getRoleFromId() in order to bypass auth checks - will do here
        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role " + roleId);
        }

        Set<Long> ownedRoleIds;
        SyncopeUser authUser = userDAO.find(SecurityContextHolder.getContext().getAuthentication().getName());
        if (authUser == null) {
            ownedRoleIds = Collections.<Long>emptySet();
        } else {
            ownedRoleIds = authUser.getRoleIds();
        }

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        allowedRoleIds.addAll(ownedRoleIds);
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        auditManager.audit(Category.role, RoleSubCategory.selfRead, Result.success,
                "Successfully read own role: " + role.getId());

        return binder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public RoleTO parent(final Long roleId) {
        SyncopeRole role = binder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (role.getParent() != null && !allowedRoleIds.contains(role.getParent().getId())) {
            throw new UnauthorizedRoleException(role.getParent().getId());
        }

        RoleTO result = role.getParent() == null
                ? null
                : binder.getRoleTO(role.getParent());

        auditManager.audit(Category.role, RoleSubCategory.parent, Result.success,
                result == null
                ? "Role " + role.getId() + " is a root role"
                : "Found parent for role " + role.getId() + ": " + result.getId());

        return result;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public List<RoleTO> children(final Long roleId) {
        SyncopeRole role = binder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeRole> children = roleDAO.findChildren(role);
        List<RoleTO> childrenTOs = new ArrayList<RoleTO>(children.size());
        for (SyncopeRole child : children) {
            if (allowedRoleIds.contains(child.getId())) {
                childrenTOs.add(binder.getRoleTO(child));
            }
        }

        auditManager.audit(Category.role, RoleSubCategory.children, Result.success,
                "Found " + childrenTOs.size() + " children of role " + roleId);

        return childrenTOs;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public List<RoleTO> search(final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        return search(searchCondition, -1, -1);
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public List<RoleTO> search(final NodeCond searchCondition, final int page, final int size)
            throws InvalidSearchConditionException {

        LOG.debug("Role search called with condition {}", searchCondition);

        if (!searchCondition.isValid()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final List<SyncopeRole> matchingRoles = searchDAO.search(
                EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames()), searchCondition, page, size,
                AttributableUtil.getInstance(AttributableType.ROLE));

        final List<RoleTO> result = new ArrayList<RoleTO>(matchingRoles.size());
        for (SyncopeRole role : matchingRoles) {
            result.add(binder.getRoleTO(role));
        }

        auditManager.audit(Category.role, AuditElements.RoleSubCategory.read, Result.success,
                "Successfully searched for roles (page=" + page + ", size=" + size + "): " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public int searchCount(final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        if (!searchCondition.isValid()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        return searchDAO.count(adminRoleIds, searchCondition, AttributableUtil.getInstance(AttributableType.ROLE));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<RoleTO> list() {
        List<SyncopeRole> roles = roleDAO.findAll();

        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(binder.getRoleTO(role));
        }

        auditManager.audit(Category.role, RoleSubCategory.list, Result.success,
                "Successfully listed all roles: " + roleTOs.size());

        return roleTOs;
    }

    @PreAuthorize("hasRole('ROLE_CREATE')")
    public RoleTO create(final RoleTO roleTO) {
        LOG.debug("Role create called with parameters {}", roleTO);

        // Check that this operation is allowed to be performed by caller
        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (roleTO.getParent() != 0 && !allowedRoleIds.contains(roleTO.getParent())) {
            throw new UnauthorizedRoleException(roleTO.getParent());
        }

        // Attributable transformation (if configured)
        RoleTO actual = attrTransformer.transform(roleTO);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation
         */
        WorkflowResult<Long> created = rwfAdapter.create(actual);

        EntitlementUtil.extendAuthContext(created.getResult());

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created, actual.getVirAttrs());
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        final RoleTO savedTO = binder.getRoleTO(created.getResult());
        savedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        LOG.debug("About to return created role\n{}", savedTO);

        auditManager.audit(Category.role, RoleSubCategory.create, Result.success,
                "Successfully created role: " + savedTO.getId());

        return savedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    public RoleTO update(final RoleMod roleMod) {
        LOG.debug("Role update called with {}", roleMod);

        // Check that this operation is allowed to be performed by caller
        SyncopeRole role = binder.getRoleFromId(roleMod.getId());

        // Attribute value transformation (if configured)
        RoleMod actual = attrTransformer.transform(roleMod);
        LOG.debug("Transformed: {}", actual);

        /*
         * Actual operations: workflow, propagation
         */
        WorkflowResult<Long> updated = rwfAdapter.update(actual);

        List<PropagationTask> tasks = propagationManager.getRoleUpdateTaskIds(updated,
                actual.getVirAttrsToRemove(), actual.getVirAttrsToUpdate());
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        final RoleTO updatedTO = binder.getRoleTO(updated.getResult());
        updatedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        auditManager.audit(Category.role, RoleSubCategory.update, Result.success,
                "Successfully updated role: " + role.getId());

        LOG.debug("About to return updated role\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_DELETE')")
    public RoleTO delete(final Long roleId) {
        LOG.debug("Role delete called for {}", roleId);

        List<SyncopeRole> ownedRoles = roleDAO.findOwned(binder.getRoleFromId(roleId));
        if (!ownedRoles.isEmpty()) {
            List<String> owned = new ArrayList<String>(ownedRoles.size());
            for (SyncopeRole role : ownedRoles) {
                owned.add(role.getId() + " " + role.getName());
            }

            auditManager.audit(Category.role, AuditElements.UserSubCategory.delete, Result.failure,
                    "Could not delete role: " + roleId + " because of role(s) ownership " + owned);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RoleOwnership);
            sce.getElements().addAll(owned);
            throw sce;
        }

        // Generate propagation tasks for deleting users from role resources, if they are on those resources only
        // because of the reason being deleted (see SYNCOPE-357)
        List<PropagationTask> tasks = new ArrayList<PropagationTask>();
        for (WorkflowResult<Long> wfResult : binder.getUsersOnResourcesOnlyBecauseOfRole(roleId)) {
            tasks.addAll(propagationManager.getUserDeleteTaskIds(wfResult));
        }

        // Generate propagation tasks for deleting this role from resources
        tasks.addAll(propagationManager.getRoleDeleteTaskIds(roleId));

        RoleTO roleTO = new RoleTO();
        roleTO.setId(roleId);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        roleTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        rwfAdapter.delete(roleId);

        auditManager.audit(Category.role, RoleSubCategory.delete, Result.success,
                "Successfully deleted role: " + roleId);

        LOG.debug("Role successfully deleted: {}", roleId);

        return roleTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public RoleTO unlink(final Long roleId, final Collection<String> resources) {
        LOG.debug("About to unlink role({}) and resources {}", roleId, resources);

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleId);

        roleMod.getResourcesToRemove().addAll(resources);

        final WorkflowResult<Long> updated = rwfAdapter.update(roleMod);

        final RoleTO updatedTO = binder.getRoleTO(updated.getResult());

        auditManager.audit(Category.user, AuditElements.RoleSubCategory.update, Result.success,
                "Successfully updated role: " + updatedTO.getName());

        LOG.debug("About to return updated role\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public RoleTO unassign(final Long roleId, final Collection<String> resources) {
        LOG.debug("About to unassign role({}) and resources {}", roleId, resources);

        final RoleMod roleMod = new RoleMod();
        roleMod.setId(roleId);
        roleMod.getResourcesToRemove().addAll(resources);

        return update(roleMod);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public RoleTO deprovision(final Long roleId, final Collection<String> resources) {
        LOG.debug("About to deprovision role({}) from resources {}", roleId, resources);

        final SyncopeRole role = binder.getRoleFromId(roleId);

        final Set<String> noPropResourceName = role.getResourceNames();
        noPropResourceName.removeAll(resources);

        final List<PropagationTask> tasks = propagationManager.getRoleDeleteTaskIds(roleId, noPropResourceName);
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        final RoleTO updatedTO = binder.getRoleTO(role);
        updatedTO.getPropagationStatusTOs().addAll(propagationReporter.getStatuses());

        auditManager.audit(Category.user, AuditElements.RoleSubCategory.update, Result.success,
                "Successfully deprovisioned role: " + updatedTO.getName());

        LOG.debug("About to return updated role\n{}", updatedTO);

        return updatedTO;
    }
}
