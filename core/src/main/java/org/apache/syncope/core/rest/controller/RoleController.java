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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.NotFoundException;
import org.apache.syncope.controller.ContextAware;
import org.apache.syncope.controller.RoleService;
import org.apache.syncope.controller.UnauthorizedRoleException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.mod.RoleMod;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.RoleSubCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class RoleController extends AbstractController implements RoleService, ContextAware {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDataBinder dataBinder;

    protected UriInfo uriInfo;

    @Override
    public RoleTO read(final Long roleId) throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = dataBinder.getSyncopeRole(roleId);
        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());

        SyncopeUser authUser = userDAO.find(SecurityContextHolder.getContext().getAuthentication().getName());
        // authUser == null for admin user
        if (authUser != null) {
            allowedRoleIds.addAll(authUser.getRoleIds());
        }

        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        auditManager.audit(Category.role, RoleSubCategory.selfRead, Result.success,
                "Successfully read role: " + role.getId());

        return dataBinder.getRoleTO(role);
    }



    @Override
    public RoleTO parent(final Long roleId) throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = dataBinder.getSyncopeRole(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (role.getParent() != null && !allowedRoleIds.contains(role.getParent().getId())) {
            throw new UnauthorizedRoleException(role.getParent().getId());
        }

        RoleTO result = role.getParent() == null
                ? null
                : dataBinder.getRoleTO(role.getParent());

        auditManager.audit(Category.role, RoleSubCategory.parent, Result.success, result == null
                ? "Role " + role.getId() + " is a root role"
                : "Found parent for role " + role.getId() + ": " + result.getId());

        return result;
    }

    @Override
    public List<RoleTO> children(final Long roleId) throws NotFoundException {
        SyncopeRole role = dataBinder.getSyncopeRole(roleId);

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

    @Override
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

    @Override
    public Response create(final RoleTO roleTO) throws UnauthorizedRoleException {

        LOG.debug("Role create called with parameters {}", roleTO);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (roleTO.getParent() != 0 && !allowedRoleIds.contains(roleTO.getParent())) {
            throw new UnauthorizedRoleException(roleTO.getParent());
        }

        SyncopeRole role = roleDAO.save(dataBinder.create(roleTO));

        auditManager.audit(Category.role, RoleSubCategory.create, Result.success,
                "Successfully created role: " + role.getId());

        URI newRoleURI = getUriInfo().getAbsolutePathBuilder().path(role.getId().toString()).build();
        return Response.created(newRoleURI).build();
    }

    @Override
    public RoleTO update(final Long roleId, final RoleMod roleMod) throws NotFoundException,
            UnauthorizedRoleException {
        LOG.debug("Role update called with parameter {}", roleMod);

        SyncopeRole role = dataBinder.getSyncopeRole(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        // Updates only elements set in roleMod
        dataBinder.update(role, roleMod);
        role = roleDAO.save(role);

        auditManager.audit(Category.role, RoleSubCategory.update, Result.success,
                "Successfully updated role: " + role.getId());

        return dataBinder.getRoleTO(role);
    }

    @Override
    public Response delete(final Long roleId) throws NotFoundException, UnauthorizedRoleException {
        SyncopeRole role = dataBinder.getSyncopeRole(roleId);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        auditManager.audit(Category.role, RoleSubCategory.delete, Result.success,
                "Successfully deleted role: " + role.getId());

        roleDAO.delete(roleId);

        return Response.ok().build();
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    @Override
    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }
}
