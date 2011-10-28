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
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.mod.RoleMod;
import org.syncope.client.to.RoleTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Role's services.
 */
@Component
public class RoleRestClient extends AbstractBaseRestClient {

    /**
     * Get all Roles.
     * @return SchemaTOs
     */
    public List<RoleTO> getAllRoles()
            throws SyncopeClientCompositeErrorException {

        List<RoleTO> roles = null;

        try {
            roles = Arrays.asList(restTemplate.getForObject(
                    baseURL + "role/list.json", RoleTO[].class));
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While listing all roles", e);
        }

        return roles;
    }

    /**
     * Create new role.
     * @param roleTO
     */
    public void createRole(RoleTO roleTO) {
        restTemplate.postForObject(
                baseURL + "role/create", roleTO, RoleTO.class);
    }

    /**
     * Load an already existent role by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public RoleTO readRole(Long id) {
        RoleTO roleTO = null;

        try {
            roleTO = restTemplate.getForObject(
                    baseURL + "role/read/{roleId}.json", RoleTO.class, id);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While reading a role", e);
        }
        return roleTO;
    }

    /**
     * Update an already existent role.
     * @param roleTO updated
     * @return true is the opertion ends succesfully, false otherwise
     */
    public void updateRole(RoleMod roleMod) {
        restTemplate.postForObject(
                baseURL + "role/update", roleMod, RoleTO.class);
    }

    /**
     * Delete an already existent role by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRole(Long id) {
        restTemplate.delete(baseURL + "role/delete/{roleId}.json", id);
    }
}
