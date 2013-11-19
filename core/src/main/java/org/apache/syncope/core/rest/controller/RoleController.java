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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/role")
public class RoleController extends AbstractController<RoleTO> {

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

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{roleId}")
    @Transactional(readOnly = true)
    public RoleTO read(@PathVariable("roleId") final Long roleId) {
        SyncopeRole role = binder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        return binder.getRoleTO(role);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, value = "/selfRead/{roleId}")
    @Transactional(readOnly = true)
    public RoleTO selfRead(@PathVariable("roleId") final Long roleId) {
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

        return binder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/parent/{roleId}")
    @Transactional(readOnly = true)
    public RoleTO parent(@PathVariable("roleId") final Long roleId) {
        SyncopeRole role = binder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (role.getParent() != null && !allowedRoleIds.contains(role.getParent().getId())) {
            throw new UnauthorizedRoleException(role.getParent().getId());
        }

        RoleTO result = role.getParent() == null
                ? null
                : binder.getRoleTO(role.getParent());

        return result;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/children/{roleId}")
    @Transactional(readOnly = true)
    public List<RoleTO> children(@PathVariable("roleId") final Long roleId) {
        SyncopeRole role = binder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeRole> children = roleDAO.findChildren(role);
        List<RoleTO> childrenTOs = new ArrayList<RoleTO>(children.size());
        for (SyncopeRole child : children) {
            if (allowedRoleIds.contains(child.getId())) {
                childrenTOs.add(binder.getRoleTO(child));
            }
        }

        return childrenTOs;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<RoleTO> search(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        return search(searchCondition, -1, -1);
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search/{page}/{size}")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public List<RoleTO> search(@RequestBody final NodeCond searchCondition, @PathVariable("page") final int page,
            @PathVariable("size") final int size)
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

        return result;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.POST, value = "/search/count")
    @Transactional(readOnly = true, rollbackFor = {Throwable.class})
    public ModelAndView searchCount(@RequestBody final NodeCond searchCondition)
            throws InvalidSearchConditionException {

        if (!searchCondition.isValid()) {
            LOG.error("Invalid search condition: {}", searchCondition);
            throw new InvalidSearchConditionException();
        }

        final Set<Long> adminRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        return new ModelAndView().addObject(searchDAO.count(adminRoleIds, searchCondition,
                AttributableUtil.getInstance(AttributableType.ROLE)));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list")
    @Transactional(readOnly = true)
    public List<RoleTO> list() {
        List<SyncopeRole> roles = roleDAO.findAll();

        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(binder.getRoleTO(role));
        }

        return roleTOs;
    }

    @PreAuthorize("hasRole('ROLE_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public RoleTO create(final HttpServletResponse response, @RequestBody final RoleTO roleTO) {
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

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created, actual.getVirtualAttributes());
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        final RoleTO savedTO = binder.getRoleTO(created.getResult());
        savedTO.setPropagationStatusTOs(propagationReporter.getStatuses());

        LOG.debug("About to return created role\n{}", savedTO);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public RoleTO update(@RequestBody final RoleMod roleMod) {
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
                actual.getVirtualAttributesToBeRemoved(), actual.getVirtualAttributesToBeUpdated());
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        final RoleTO updatedTO = binder.getRoleTO(updated.getResult());
        updatedTO.setPropagationStatusTOs(propagationReporter.getStatuses());

        LOG.debug("About to return updated role\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{roleId}")
    public RoleTO delete(@PathVariable("roleId") final Long roleId) {
        LOG.debug("Role delete called for {}", roleId);

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

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        roleTO.setPropagationStatusTOs(propagationReporter.getStatuses());

        rwfAdapter.delete(roleId);

        LOG.debug("Role successfully deleted: {}", roleId);

        return roleTO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RoleTO resolveReference(final Method method, final Object... args) throws UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof RoleTO) {
                    id = ((RoleTO) args[i]).getId();
                } else if (args[i] instanceof RoleMod) {
                    id = ((RoleMod) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return binder.getRoleTO(id);
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
