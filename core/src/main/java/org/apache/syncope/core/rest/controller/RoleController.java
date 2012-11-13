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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.syncope.client.mod.RoleMod;
import org.apache.syncope.client.to.PropagationTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.DefaultPropagationHandler;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationManager;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.RoleSubCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/role")
public class RoleController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDataBinder dataBinder;

    @Autowired
    private RoleWorkflowAdapter rwfAdapter;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{roleId}")
    @Transactional(readOnly = true)
    public RoleTO read(@PathVariable("roleId") final Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = dataBinder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        auditManager.audit(Category.role, RoleSubCategory.read, Result.success,
                "Successfully read role: " + role.getId());

        return dataBinder.getRoleTO(role);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, value = "/selfRead/{roleId}")
    @Transactional(readOnly = true)
    public RoleTO selfRead(@PathVariable("roleId") final Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        // Explicit search instead of using dataBinder.getRoleFromId() in order to bypass auth checks - will do here
        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role " + roleId);
        }

        Set<Long> ownedRoleIds;
        SyncopeUser authUser = userDAO.find(SecurityContextHolder.getContext().getAuthentication().getName());
        if (authUser == null) {
            ownedRoleIds = Collections.EMPTY_SET;
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

        return dataBinder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/parent/{roleId}")
    @Transactional(readOnly = true)
    public RoleTO parent(@PathVariable("roleId") final Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = dataBinder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (role.getParent() != null && !allowedRoleIds.contains(role.getParent().getId())) {
            throw new UnauthorizedRoleException(role.getParent().getId());
        }

        RoleTO result = role.getParent() == null
                ? null
                : dataBinder.getRoleTO(role.getParent());

        auditManager.audit(Category.role, RoleSubCategory.parent, Result.success,
                result == null
                ? "Role " + role.getId() + " is a root role"
                : "Found parent for role " + role.getId() + ": " + result.getId());

        return result;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/children/{roleId}")
    @Transactional(readOnly = true)
    public List<RoleTO> children(@PathVariable("roleId") final Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = dataBinder.getRoleFromId(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeRole> children = roleDAO.findChildren(role);
        List<RoleTO> childrenTOs = new ArrayList<RoleTO>(children.size());
        for (SyncopeRole child : children) {
            if (allowedRoleIds.contains(child.getId())) {
                childrenTOs.add(dataBinder.getRoleTO(child));
            }
        }

        auditManager.audit(Category.role, RoleSubCategory.children, Result.success,
                "Found " + childrenTOs.size() + " children of role " + roleId);

        return childrenTOs;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/list")
    @Transactional(readOnly = true)
    public List<RoleTO> list() {
        List<SyncopeRole> roles = roleDAO.findAll();

        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(dataBinder.getRoleTO(role));
        }

        auditManager.audit(Category.role, RoleSubCategory.list, Result.success,
                "Successfully listed all roles: " + roleTOs.size());

        return roleTOs;
    }

    @PreAuthorize("hasRole('ROLE_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public RoleTO create(final HttpServletResponse response, @RequestBody final RoleTO roleTO)
            throws UnauthorizedRoleException, WorkflowException, NotFoundException, PropagationException {

        LOG.debug("Role create called with parameters {}", roleTO);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (roleTO.getParent() != 0 && !allowedRoleIds.contains(roleTO.getParent())) {
            throw new UnauthorizedRoleException(roleTO.getParent());
        }

        WorkflowResult<Long> created = rwfAdapter.create(roleTO);

        // Extend the current authentication context to include the role just created
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(auth.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(EntitlementUtil.getEntitlementNameFromRoleId(created.getResult())));
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                auth.getPrincipal(), auth.getCredentials(), authorities);
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created, roleTO.getVirtualAttributes());

        final List<PropagationTO> propagations = new ArrayList<PropagationTO>();
        taskExecutor.execute(tasks, new DefaultPropagationHandler(connObjectUtil, propagations));

        final RoleTO savedTO = dataBinder.getRoleTO(created.getResult());
        savedTO.setPropagationTOs(propagations);

        LOG.debug("About to return created role\n{}", savedTO);

        auditManager.audit(Category.role, RoleSubCategory.create, Result.success,
                "Successfully created role: " + savedTO.getId());

        response.setStatus(HttpServletResponse.SC_CREATED);
        return savedTO;
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public RoleTO update(@RequestBody final RoleMod roleMod)
            throws NotFoundException, UnauthorizedRoleException, WorkflowException, PropagationException {

        LOG.debug("Role update called with {}", roleMod);

        SyncopeRole role = dataBinder.getRoleFromId(roleMod.getId());

        WorkflowResult<Long> updated = rwfAdapter.update(roleMod);

        List<PropagationTask> tasks = propagationManager.getRoleUpdateTaskIds(updated,
                roleMod.getVirtualAttributesToBeRemoved(), roleMod.getVirtualAttributesToBeUpdated());

        final List<PropagationTO> propagations = new ArrayList<PropagationTO>();
        taskExecutor.execute(tasks, new DefaultPropagationHandler(connObjectUtil, propagations));

        final RoleTO updatedTO = dataBinder.getRoleTO(updated.getResult());
        updatedTO.setPropagationTOs(propagations);

        auditManager.audit(Category.role, RoleSubCategory.update, Result.success,
                "Successfully updated role: " + role.getId());

        LOG.debug("About to return updated role\n{}", updatedTO);

        return updatedTO;
    }

    @PreAuthorize("hasRole('ROLE_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{roleId}")
    public RoleTO delete(@PathVariable("roleId") final Long roleId)
            throws NotFoundException, UnauthorizedRoleException, WorkflowException, PropagationException {

        SyncopeRole role = dataBinder.getRoleFromId(roleId);

        RoleTO roleTO = dataBinder.getRoleTO(role);

        List<PropagationTask> tasks = propagationManager.getRoleDeleteTaskIds(roleId);

        final List<PropagationTO> propagations = new ArrayList<PropagationTO>();
        taskExecutor.execute(tasks, new DefaultPropagationHandler(connObjectUtil, propagations));

        rwfAdapter.delete(roleId);

        auditManager.audit(Category.role, RoleSubCategory.delete, Result.success,
                "Successfully deleted role: " + roleId);

        LOG.debug("Role successfully deleted: {}", roleId);

        return roleTO;
    }
}
