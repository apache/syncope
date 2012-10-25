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
package org.apache.syncope.core.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.util.EntitlementUtil;

@Configurable
public class SyncopeUserDetailsService implements UserDetailsService {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

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
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException, DataAccessException {
        final Set<SimpleGrantedAuthority> authorities = new HashSet<SimpleGrantedAuthority>();
        if (adminUser.equals(username)) {
            for (Entitlement entitlement : entitlementDAO.findAll()) {
                authorities.add(new SimpleGrantedAuthority(entitlement.getName()));
            }
        } else {
            final SyncopeUser user = userDAO.find(username);

            if (user == null) {
                throw new UsernameNotFoundException("Could not find any user with id " + username);
            }

            // Give entitlements based on roles assigned to user (and their ancestors)
            final Set<SyncopeRole> roles = new HashSet<SyncopeRole>(user.getRoles());
            for (SyncopeRole role : user.getRoles()) {
                roles.addAll(roleDAO.findAncestors(role));
            }
            for (SyncopeRole role : roles) {
                for (Entitlement entitlement : role.getEntitlements()) {
                    authorities.add(new SimpleGrantedAuthority(entitlement.getName()));
                }
            }
            // Give role operational entitlements for owned roles
            List<SyncopeRole> ownedRoles = roleDAO.findOwned(user);
            if (!ownedRoles.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CREATE"));
                authorities.add(new SimpleGrantedAuthority("ROLE_READ"));
                authorities.add(new SimpleGrantedAuthority("ROLE_UPDATE"));
                authorities.add(new SimpleGrantedAuthority("ROLE_DELETE"));

                for (SyncopeRole role : ownedRoles) {
                    authorities.add(new SimpleGrantedAuthority(EntitlementUtil.
                            getEntitlementNameFromRoleId(role.getId())));
                }
            }
        }

        return new User(username, "<PASSWORD_PLACEHOLDER>", true, true, true, true, authorities);
    }
}
