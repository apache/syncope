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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.RoleTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.rest.data.RoleDataBinder;

@Controller
@RequestMapping("/role")
public class RoleController extends AbstractController {

    @Autowired
    private SyncopeRoleDAO syncopeRoleDAO;
    @Autowired
    private RoleDataBinder roleDataBinder;

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public RoleTO create(HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody RoleTO roleTO)
            throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("create called with parameters " + roleTO);
        }

        SyncopeRole syncopeRole = null;
        try {
            syncopeRole = roleDataBinder.createSyncopeRole(roleTO);
        } catch (SyncopeClientCompositeErrorException e) {
            log.error("Could not create for " + roleTO, e);

            return throwCompositeException(e, response);
        }
        syncopeRole = syncopeRoleDAO.save(syncopeRole);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return roleDataBinder.getRoleTO(syncopeRole);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{roleId}")
    public void delete(HttpServletResponse response,
            @PathVariable("roleId") Long roleId)
            throws IOException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);

        if (role == null) {
            log.error("Could not find role '" + roleId + "'");
            throwNotFoundException(String.valueOf(roleId), response);
        } else {
            syncopeRoleDAO.delete(roleId);
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public RoleTOs list(HttpServletRequest request) throws IOException {
        List<SyncopeRole> roles = syncopeRoleDAO.findAll();
        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());

        for (SyncopeRole role : roles) {
            roleTOs.add(roleDataBinder.getRoleTO(role));
        }

        RoleTOs result = new RoleTOs();
        result.setRoles(roleTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/parent/{roleId}")
    public RoleTO parent(HttpServletResponse response,
            @PathVariable("roleId") Long roleId)
            throws IOException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);

        if (role == null) {
            log.error("Could not find role '" + roleId + "'");
            return throwNotFoundException(String.valueOf(roleId), response);
        }

        return role.getParent() == null ? null
                : roleDataBinder.getRoleTO(role.getParent());
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/children/{roleId}")
    public RoleTOs children(HttpServletResponse response,
            @PathVariable("roleId") Long roleId)
            throws IOException {

        List<SyncopeRole> roles = syncopeRoleDAO.findChildren(roleId);
        List<RoleTO> roleTOs = new ArrayList<RoleTO>(roles.size());

        for (SyncopeRole role : roles) {
            roleTOs.add(roleDataBinder.getRoleTO(role));
        }

        RoleTOs result = new RoleTOs();
        result.setRoles(roleTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{roleId}")
    public RoleTO read(HttpServletResponse response,
            @PathVariable("roleId") Long roleId)
            throws IOException {

        SyncopeRole role = syncopeRoleDAO.find(roleId);

        if (role == null) {
            log.error("Could not find role '" + roleId + "'");
            return throwNotFoundException(String.valueOf(roleId), response);
        }

        return roleDataBinder.getRoleTO(role);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public RoleTO update(HttpServletResponse response,
            @RequestBody RoleTO roleTO)
            throws IOException {

        log.info("update called with parameter " + roleTO);

        return roleTO;
    }
}
