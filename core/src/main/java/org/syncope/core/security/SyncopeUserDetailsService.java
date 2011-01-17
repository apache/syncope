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
package org.syncope.core.security;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.UserDAO;

@Configurable
public class SyncopeUserDetailsService implements UserDetailsService {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    private String adminUser;

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    @Override
    public UserDetails loadUserByUsername(final String username)
            throws UsernameNotFoundException, DataAccessException {

        User result;
        Set<GrantedAuthorityImpl> authorities =
                new HashSet<GrantedAuthorityImpl>();
        if (adminUser.equals(username)) {
            for (Entitlement entitlement : entitlementDAO.findAll()) {
                authorities.add(
                        new GrantedAuthorityImpl(entitlement.getName()));
            }
        } else {
            Long id;
            try {
                id = Long.valueOf(username);
            } catch (NumberFormatException e) {
                throw new UsernameNotFoundException(
                        "Invalid user id: " + username, e);
            }

            SyncopeUser user = userDAO.find(id);
            if (user == null) {
                throw new UsernameNotFoundException(
                        "Could not find any user with id " + id);
            }

            for (SyncopeRole role : user.getRoles()) {
                for (Entitlement entitlement : role.getEntitlements()) {
                    authorities.add(new GrantedAuthorityImpl(
                            entitlement.getName()));
                }
            }
        }

        return new User(username, "<PASSWORD_PLACEHOLDER>",
                true, true, true, true, authorities);
    }
}
