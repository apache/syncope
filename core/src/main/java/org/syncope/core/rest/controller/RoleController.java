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
import javassist.NotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.propagation.ResourceOperations;
import org.syncope.core.rest.data.RoleDataBinder;

@Controller
@RequestMapping("/role")
public class RoleController extends AbstractController {

    @Autowired
    private SyncopeRoleDAO syncopeRoleDAO;
    @Autowired
    private RoleDataBinder roleDataBinder;

    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public RoleTO create(HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody RoleTO roleTO)
            throws SyncopeClientCompositeErrorException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("create called with parameters " + roleTO);
        }

        SyncopeRole role = null;
        try {
            role = roleDataBinder.create(roleTO);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("Could not create for " + roleTO, e);

            throw e;
        }
        role = syncopeRoleDAO.save(role);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return roleDataBinder.getRoleTO(role);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{roleId}")
    public void delete(HttpServletResponse response,
            @PathVariable("roleId") Long roleId)
            throws NotFoundException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);

        if (role == null) {
            LOG.error("Could not find role '" + roleId + "'");

            throw new NotFoundException(String.valueOf(roleId));
        } else {
            syncopeRoleDAO.delete(roleId);
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public ModelAndView list(HttpServletRequest request) {
        List<SyncopeRole> roles = syncopeRoleDAO.findAll();
        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(roleDataBinder.getRoleTO(role));
        }

        return new ModelAndView().addObject(roleTOs);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/parent/{roleId}")
    public RoleTO parent(HttpServletResponse response,
            @PathVariable("roleId") Long roleId)
            throws NotFoundException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);

        if (role == null) {
            LOG.error("Could not find role '" + roleId + "'");

            throw new NotFoundException(String.valueOf(roleId));
        }

        return role.getParent() == null ? null
                : roleDataBinder.getRoleTO(role.getParent());
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/children/{roleId}")
    public ModelAndView children(HttpServletResponse response,
            @PathVariable("roleId") Long roleId) {

        List<SyncopeRole> roles = syncopeRoleDAO.findChildren(roleId);
        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());
        for (SyncopeRole role : roles) {
            roleTOs.add(roleDataBinder.getRoleTO(role));
        }

        return new ModelAndView().addObject(roleTOs);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{roleId}")
    public RoleTO read(HttpServletResponse response,
            @PathVariable("roleId") Long roleId)
            throws NotFoundException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);

        if (role == null) {
            LOG.error("Could not find role '" + roleId + "'");

            throw new NotFoundException(String.valueOf(roleId));
        }

        return roleDataBinder.getRoleTO(role);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public RoleTO update(HttpServletResponse response,
            @RequestBody RoleMod roleMod)
            throws NotFoundException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("update called with parameter " + roleMod);
        }

        SyncopeRole role = syncopeRoleDAO.find(roleMod.getId());
        if (role == null) {
            LOG.error("Could not find user '" + roleMod.getId() + "'");

            throw new NotFoundException(String.valueOf(roleMod.getId()));
        }

        ResourceOperations resourceOperations =
                roleDataBinder.update(role, roleMod);
        role = syncopeRoleDAO.save(role);

        return roleDataBinder.getRoleTO(role);
    }
}
