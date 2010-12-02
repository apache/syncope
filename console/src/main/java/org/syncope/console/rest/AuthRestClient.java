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

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.console.SyncopeUser;

/**
 * Console client for invoking authentication services.
 */
public class AuthRestClient {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(AuthRestClient.class);
    protected RestClient restClient;

    /**
     * Authenticate the user.
     * @param username
     * @param password
     * @return SyncopeUser valued object
     */
    public SyncopeUser authenticate(String username, String password) {

        SyncopeUser user = null;
        String roles = "";

        if ("admin".equals(username) && "password".equals(password)) {
           

            List<String> rolesList = getAdminRoles();

            for(int i = 0; i< rolesList.size(); i++) {
                String role = rolesList.get(i);
                roles +=role;

                if(i != rolesList.size())
                    roles += ",";
            }

            user = new SyncopeUser(username, roles);

            return user;
        }
        else  if ("manager".equals(username) && "password".equals(password)) {

            List<String> rolesList = getManagerRoles();

            for (int i = 0; i < rolesList.size(); i++) {
                String role = rolesList.get(i);
                roles += role;

                if (i != rolesList.size())
                    roles += ",";

            }

            user = new SyncopeUser(username, roles);

            return user;
        }
        else
            return null;
    }

    public List<String> getAdminRoles() {
        List<String> roles = new ArrayList<String>();
        
        roles.add("USER_CREATE");
        roles.add("USER_LIST");
        roles.add("USER_READ");
        roles.add("USER_DELETE");
        roles.add("USER_UPDATE");
        roles.add("USER_VIEW");

        roles.add("SCHEMA_CREATE");
        roles.add("SCHEMA_LIST");
        roles.add("SCHEMA_READ");
        roles.add("SCHEMA_DELETE");
        roles.add("SCHEMA_UPDATE");

        roles.add("ROLES_CREATE");
        roles.add("ROLES_LIST");
        roles.add("ROLES_READ");
        roles.add("ROLES_DELETE");
        roles.add("ROLES_UPDATE");

        roles.add("RESOURCES_CREATE");
        roles.add("RESOURCES_LIST");
        roles.add("RESOURCES_READ");
        roles.add("RESOURCES_DELETE");
        roles.add("RESOURCES_UPDATE");

        roles.add("CONNECTORS_CREATE");
        roles.add("CONNECTORS_LIST");
        roles.add("CONNECTORS_READ");
        roles.add("CONNECTORS_DELETE");
        roles.add("CONNECTORS_UPDATE");

        roles.add("REPORT_LIST");

        roles.add("CONFIGURATION_CREATE");
        roles.add("CONFIGURATION_LIST");
        roles.add("CONFIGURATION_READ");
        roles.add("CONFIGURATION_DELETE");
        roles.add("CONFIGURATION_UPDATE");

        roles.add("TASKS_CREATE");
        roles.add("TASKS_LIST");
        roles.add("TASKS_READ");
        roles.add("TASKS_DELETE");
        roles.add("TASKS_UPDATE");
        roles.add("TASKS_EXECUTE");

        return roles;
    }

    public List<String> getManagerRoles() {
        List<String> roles = new ArrayList<String>();

        //roles.add("USER_CREATE");
        roles.add("USER_LIST");
        roles.add("USER_READ");
        roles.add("USER_DELETE");
//        roles.add("USER_UPDATE");

//        roles.add("SCHEMA_CREATE");
        roles.add("SCHEMA_LIST");
//        roles.add("SCHEMA_READ");
//        roles.add("SCHEMA_DELETE");
//        roles.add("SCHEMA_UPDATE");

         roles.add("CONNECTORS_LIST");
         roles.add("REPORT_LIST");

//        roles.add("ROLES_CREATE");
        roles.add("ROLES_LIST");
        roles.add("ROLES_READ");
//        roles.add("ROLES_DELETE");
//        roles.add("ROLES_UPDATE");
        roles.add("TASKS_LIST");

        return roles;
    }

    /**
     * Getter for restClient attribute.
     * @return RestClient instance
     */
    public RestClient getRestClient() {
        return restClient;
    }

    /**
     * Setter for restClient attribute.
     * @param restClient instance
     */
    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
}
