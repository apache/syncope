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
package org.syncope.console;

import org.apache.wicket.authorization.strategies.role.Roles;

/**
 * SyncopeUser to store in SyncopeSession after the authentication.
 */
public class SyncopeUser implements java.io.Serializable
{

    private String username;
    
    private final Roles roles;

    /**
     * Create a new Syncope session user
     * @param username
     * @param roles a comma seperated list of roles 
     * (corresponding to Syncope's entitlements)
     */
    public SyncopeUser(String username, String roles) {

        if (username == null)
        {
            throw new IllegalArgumentException("username must be not null");
        }
        if (roles == null)
        {
            throw new IllegalArgumentException("roles must be not null");
        }
        this.username = username;
        this.roles = new Roles(roles);
    }

    public String getUsername()
    {
        return username;
    }

    /**
     * Whether this user has any of the given roles.
     *
     * @param roles
     *            set of roles
     * @return whether this user has any of the given roles
     */
    public boolean hasAnyRole(Roles roles)
    {
        return this.roles.hasAnyRole(roles);
    }

   /**
     * Whether this user has the given role.
     *
     * @param role
     * @return whether this user has the given role
     */
    public boolean hasRole(String role)
    {
        return this.roles.hasRole(role);
    }

    public Roles getRoles() {
        return roles;
    }
}