/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.rest.data.RoleDataBinder;
import org.syncope.core.util.EntitlementUtil;

@Controller
@RequestMapping("/role")
public class RoleController extends AbstractController {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private RoleDataBinder roleDataBinder;

    @PreAuthorize("hasRole('ROLE_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public RoleTO create(final HttpServletResponse response,
            final @RequestBody RoleTO roleTO)
            throws SyncopeClientCompositeErrorException,
            UnauthorizedRoleException {

        LOG.debug("Role create called with parameters {}", roleTO);

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        if (roleTO.getParent() != 0
                && !allowedRoleIds.contains(roleTO.getParent())) {

            throw new UnauthorizedRoleException(roleTO.getParent());
        }

        SyncopeRole role;
        try {
            role = roleDataBinder.create(roleTO);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("Could not create for " + roleTO, e);

            throw e;
        }
        role = roleDAO.save(role);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return roleDataBinder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{roleId}")
    public void delete(@PathVariable("roleId") Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role " + String.valueOf(roleId));
        }

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        roleDAO.delete(roleId);
    }

    @PreAuthorize("hasRole('ROLE_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<RoleTO> list() {
        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeRole> roles = roleDAO.findAll();
        List<RoleTO> roleTOs = new ArrayList<RoleTO>();
        for (SyncopeRole role : roles) {
            if (allowedRoleIds.contains(role.getId())) {
                roleTOs.add(roleDataBinder.getRoleTO(role));
            }
        }

        return roleTOs;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/parent/{roleId}")
    public RoleTO parent(@PathVariable("roleId") Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role " + String.valueOf(roleId));
        }

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        if (role.getParent() != null
                && !allowedRoleIds.contains(role.getParent().getId())) {

            throw new UnauthorizedRoleException(role.getParent().getId());
        }

        return role.getParent() == null ? null
                : roleDataBinder.getRoleTO(role.getParent());
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/children/{roleId}")
    public List<RoleTO> children(@PathVariable("roleId") Long roleId) {
        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());

        List<SyncopeRole> roles = roleDAO.findChildren(roleId);
        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            if (allowedRoleIds.contains(role.getId())) {
                roleTOs.add(roleDataBinder.getRoleTO(role));
            }
        }

        return roleTOs;
    }

    @PreAuthorize("hasRole('ROLE_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{roleId}")
    public RoleTO read(@PathVariable("roleId") Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException(String.valueOf(roleId));
        }

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        return roleDataBinder.getRoleTO(role);
    }

    @PreAuthorize("hasRole('ROLE_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public RoleTO update(@RequestBody RoleMod roleMod)
            throws NotFoundException, UnauthorizedRoleException {

        LOG.debug("Role update called with parameter {}", roleMod);

        SyncopeRole role = roleDAO.find(roleMod.getId());
        if (role == null) {
            throw new NotFoundException(
                    "Role " + String.valueOf(roleMod.getId()));
        }

        Set<Long> allowedRoleIds = EntitlementUtil.getRoleIds(
                EntitlementUtil.getOwnedEntitlementNames());
        if (!allowedRoleIds.contains(role.getId())) {
            throw new UnauthorizedRoleException(role.getId());
        }

        roleDataBinder.update(role, roleMod);
        role = roleDAO.save(role);

        return roleDataBinder.getRoleTO(role);
    }
}
