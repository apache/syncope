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

import org.syncope.client.to.RoleTO;
import org.syncope.client.to.RoleTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;

/**
 * Console client for invoking Rest Role's services.
 */
public class RolesRestClient {

    RestClient restClient;

    /**
     * Get all Roles.
     * @return SchemaTOs
     */
    public RoleTOs getAllRoles() {
        RoleTOs roles = null;

        try{
        roles = restClient.getRestTemplate().getForObject(restClient.getBaseURL()
                + "role/list.json", RoleTOs.class);
        }
        catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }

        return roles;
    }

    /**
     * Create new role.
     * @param schemaTO
     */
    public void createRole(RoleTO RoleTO) {
        try{
        restClient.getRestTemplate().postForObject(restClient.getBaseURL() +
                "role/create", RoleTO, RoleTO.class);
        }
        catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an already existent role by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public RoleTO readRole(String name) {
        RoleTO roleTO = null;

        try {
        roleTO = restClient.getRestTemplate().getForObject
                (restClient.getBaseURL() + "role/read/" + name + ".json",
                RoleTO.class);
        }
        catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }
        return roleTO;
    }

    /**
     * Update an already existent role.
     * @param schemaTO updated
     * @return true is the opertion ends succesfully, false otherwise
     */
    public boolean updateRole(RoleTO roleTO) {
        RoleTO newRoleTO = null;

        try {
        newRoleTO = restClient.getRestTemplate().postForObject
                (restClient.getBaseURL() + "role/update.json", roleTO,
                RoleTO.class);
        }
        catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }

        return (roleTO.equals(newRoleTO))?true:false;
    }
    
    /**
     * Delete an already existent role by its name.
     * @param name (e.g.:surname)
     * @return schemaTO
     */
    public void deleteRole(String name) {
        try {
        restClient.getRestTemplate().delete(restClient.getBaseURL() +
                "role/delete/{roleName}.json",name);
        }
        catch (SyncopeClientCompositeErrorException e) {
            e.printStackTrace();
        }

    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
}